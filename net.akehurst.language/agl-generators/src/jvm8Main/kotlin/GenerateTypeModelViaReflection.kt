package net.akehurst.language.agl.generators

import net.akehurst.language.agl.generators.GenerateTypeModelViaReflection.Companion.isCollection
import net.akehurst.language.agl.generators.GenerateTypeModelViaReflection.Companion.isEnum
import net.akehurst.language.agl.generators.GenerateTypeModelViaReflection.Companion.isInterface
import net.akehurst.language.agl.language.typemodel.typeModel
import net.akehurst.language.api.language.base.Import
import net.akehurst.language.api.language.base.PossiblyQualifiedName
import net.akehurst.language.api.language.base.PossiblyQualifiedName.Companion.asPossiblyQualifiedName
import net.akehurst.language.api.language.base.QualifiedName
import net.akehurst.language.api.language.base.SimpleName
import net.akehurst.language.collections.lazyMap
import net.akehurst.language.collections.lazyMutableMapNonNull
import net.akehurst.language.typemodel.api.*
import net.akehurst.language.typemodel.simple.*
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.name
import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.javaField


class GenerateTypeModelViaReflection(
    val typeModelName: SimpleName,
    val additionalNamespaces: List<TypeNamespace> = emptyList(),
    val substituteTypes: Map<String, String>
) {
    companion object {
        const val STD_LIB = "net.akehurst.language.typemodel.simple.SimpleTypeModelStdLib"

        val KOTLIN_TO_AGL = mapOf(
            "kotlin.Any" to SimpleTypeModelStdLib.AnyType.qualifiedTypeName.value,
            "kotlin.Boolean" to SimpleTypeModelStdLib.Boolean.qualifiedTypeName.value,
            "kotlin.String" to SimpleTypeModelStdLib.String.qualifiedTypeName.value,
            "kotlin.Int" to SimpleTypeModelStdLib.Integer.qualifiedTypeName.value,
            "kotlin.Double" to SimpleTypeModelStdLib.Real.qualifiedTypeName.value,
            "kotlin.Float" to SimpleTypeModelStdLib.Real.qualifiedTypeName.value,
            "kotlin.Pair" to SimpleTypeModelStdLib.Pair.qualifiedName.value,
            "kotlin.collections.Collection" to SimpleTypeModelStdLib.Collection.qualifiedName.value,
            "kotlin.collections.List" to SimpleTypeModelStdLib.List.qualifiedName.value,
            "kotlin.collections.Set" to SimpleTypeModelStdLib.Set.qualifiedName.value,
            "net.akehurst.language.collections.OrderedSet"  to SimpleTypeModelStdLib.OrderedSet.qualifiedName.value,
            "kotlin.collections.Map" to SimpleTypeModelStdLib.Map.qualifiedName.value,
            "java.lang.Exception" to SimpleTypeModelStdLib.Exception.qualifiedTypeName.value,
            "java.lang.RuntimeException" to SimpleTypeModelStdLib.Exception.qualifiedTypeName.value,
            "kotlin.Throwable" to SimpleTypeModelStdLib.Exception.qualifiedTypeName.value,
        )

        val KClass<*>.isInterface get() = java.isInterface
        val KClass<*>.isEnum get() = this.isSubclassOf(Enum::class)
        val <T : Enum<T>> KClass<T>.enumValues: Array<T> get() = java.enumConstants
        val KClass<*>.isCollection get() = this.isSubclassOf(Collection::class)
        val KClassifier.asPossiblyQualifiedName
            get() = when (this) {
                is KClass<*> -> this.qualifiedName!!.asPossiblyQualifiedName
                is KTypeParameter -> this.name.asPossiblyQualifiedName
                else -> error("Unsupported")
            }
        val KType.asPossiblyQualifiedName get() = this.classifier!!.asPossiblyQualifiedName

        /**
         * returns the resolve TypeInstance, plus list of qualified names of other required types
         */
        fun KType.asTypeInstance(ns: TypeNamespace, context: TypeDeclaration, substituteTypes: Map<String, String>): Pair<TypeInstance, List<QualifiedName>> {
            val requires = mutableListOf<QualifiedName>()
            val pqtn = this.asPossiblyQualifiedName
            val subName = when (pqtn) {
                is SimpleName -> pqtn
                is QualifiedName -> {
                    val qn = substituteTypes[pqtn.value]?.let { QualifiedName(it) } ?: pqtn
                    if(ns.qualifiedName!=qn.front) {
                        logInfo("'$context' requires import '$qn'")
                        requires.add(qn)
                        ns.addImport(qn.front.asImport)
                    }
                    qn.last
                }

                else -> error("Unsupported")
            }

            val targsp = this.arguments.map { pj ->
                when {
                    null==pj.type -> Pair(SimpleTypeModelStdLib.AnyType, emptyList()) // '*' projection
                    else -> pj.type!!.asTypeInstance(ns, context, substituteTypes)
                }
            }
            targsp.forEach { requires.addAll(it.second) }
            val targs = targsp.map { it.first }
            val isNullable = this.isMarkedNullable
            val ty =  ns.createTypeInstance(context, subName, targs, isNullable)
            return Pair(ty, requires)
        }

        fun logInfo(message: String) {
            //println(message)
        }
    }

    private val _typeModel = TypeModelSimple(typeModelName)

    private val _include = mutableListOf<QualifiedName>()
    private val _exclude = mutableListOf<QualifiedName>()
    private val _requires = lazyMutableMapNonNull<QualifiedName, MutableSet<QualifiedName>> { mutableSetOf() }

    fun include(qualifiedName:String) {
        _include.add(QualifiedName(qualifiedName))
    }

    fun exclude(qualifiedName:String) {
        _exclude.add(QualifiedName(qualifiedName))
    }

    fun addPackage(packageName: String, initialImports: List<Import> = emptyList()) {
        val ns = TypeNamespaceSimple(QualifiedName(packageName), initialImports)

        val kclasses = findClasses(packageName)

        for (kclass in kclasses) {
            addKClass(ns, kclass)
        }

        _typeModel.addNamespace(ns)
    }

    fun addKClass(ns:TypeNamespaceSimple, kclass: KClass<*>) {
        when {
            _exclude.contains(QualifiedName(kclass.qualifiedName!!)) -> Unit
            kclass.supertypes.any { it.classifier == Annotation::class } -> Unit // do not add annotations
            // canot detect DslBuilder annotations at runtime!
            //kclass.annotations.any { it.annotationClass.annotations.any { an -> an.annotationClass.isSubclassOf(DslMarker::class) } } -> Unit // do not add DSL builders
            KVisibility.PUBLIC == kclass.visibility -> {
                when {
                    kclass.isValue -> addValueType(ns, kclass)
                    kclass.isEnum -> addEnumType(ns, kclass as KClass<out Enum<*>>)
                    kclass.isInterface -> addInterfaceType(ns, kclass)
                    kclass.isCollection -> addCollectionType(ns, kclass)
                    else -> addDataType(ns, kclass)
                }
            }

            else -> {
                logInfo("Not generating '${kclass.simpleName}' as it is not public")
            }
        }
    }

    fun generate(): TypeModel {
        typeModel(
            name = "",
            resolveImports = true,
            namespaces = additionalNamespaces,
        ) {
            namespace(
                qualifiedName = "",
                imports = listOf(""),
            ) {
                primitiveType("PT")
                enumType("ET", listOf())
                collectionType("CT", listOf())
                valueType("VT") {}
                interfaceType("IT") {}
                dataType("DT") {
                    typeParameters()
                    constructor_ {
                        parameter("n", "t")
                    }
                    propertyOf(typeName = "t", characteristics = setOf(), propertyName = "n", isNullable = true) {
                        typeArgument(qualifiedTypeName = "")
                    }
                }
            }
        }

        _include.forEach {
            val ns = _typeModel.findOrCreateNamespace(it.front, emptyList()) as TypeNamespaceSimple
            val kclass = Class.forName(it.value).kotlin
            addKClass(ns, kclass)
        }
        additionalNamespaces.forEach { _typeModel.addNamespace(it) }

        _requires.forEach { (t, reqs) ->
           val notFound =  reqs.mapNotNull { qn ->
                val foundReq = _typeModel.findByQualifiedNameOrNull(qn)
                if (null==foundReq) {
                    qn
                } else {
                    null
                }
            }
            if(notFound.isNotEmpty()) {
                println("Type '$t' requires the following:")
                notFound.forEach {
                    println("  '$it'")
                }
            }
        }

        _typeModel.resolveImports()
        return _typeModel
    }

    private fun findSuperTypesAndImports(targetNamespaceName: QualifiedName, kclass: KClass<*>): Pair<List<PossiblyQualifiedName>, List<Import>> {
        val imports = mutableListOf<Import>()
        val superTypes = kclass.supertypes.map {
            val qn = QualifiedName((it.classifier as KClass<*>).qualifiedName!!)
            if (qn.front == targetNamespaceName) {
                //same package, not import
                qn.last
            } else {
                val sbQn = substituteTypes[qn.value]?.let { QualifiedName(it) } ?: qn
                imports.add(sbQn.front.asImport)
                sbQn.last
            }
        }
        return Pair(superTypes, imports)
    }

    private fun addPrimitiveType(ns: TypeNamespaceSimple, kclass: KClass<*>) {
        ns.addDefinition(PrimitiveTypeSimple(ns, SimpleName(kclass.simpleName!!)))
    }

    private fun <T : Enum<T>> addEnumType(ns: TypeNamespaceSimple, kclass: KClass<T>) {
        val lits = kclass.enumValues.map { it.name }
        ns.addDefinition(EnumTypeSimple(ns, SimpleName(kclass.simpleName!!), lits))
    }

    private fun addCollectionType(ns: TypeNamespaceSimple, kclass: KClass<*>) {
        ns.addDefinition(CollectionTypeSimple(ns, SimpleName(kclass.simpleName!!)))
    }

    private fun addValueType(ns: TypeNamespaceSimple, kclass: KClass<*>) {
        val type = ValueTypeSimple(ns, SimpleName(kclass.simpleName!!))
        ns.addDefinition(type)
        addTypeParameters(ns, type, kclass)
        val (st, imp) = findSuperTypesAndImports(ns.qualifiedName, kclass)
        st.forEach { type.addSupertype(it) }
        imp.forEach {
            logInfo("'$type' requires import '$it' due to supertype")
            ns.addImport(it)
        }
        addConstructors(ns, type, kclass)
        addPropertiesAndImports(ns, type, kclass)
    }

    private fun addInterfaceType(ns: TypeNamespaceSimple, kclass: KClass<*>) {
        val type = InterfaceTypeSimple(ns, SimpleName(kclass.simpleName!!))
        ns.addDefinition(type)
        addTypeParameters(ns, type, kclass)

        val (st, imp) = findSuperTypesAndImports(ns.qualifiedName, kclass)
        st.forEach { type.addSupertype(it) }
        imp.forEach {
            logInfo("'$type' requires import '$it' due to supertype")
            ns.addImport(it)
        }

        addPropertiesAndImports(ns, type, kclass)
    }

    private fun addDataType(ns: TypeNamespaceSimple, kclass: KClass<*>) {
        val type = DataTypeSimple(ns, SimpleName(kclass.simpleName!!))
        ns.addDefinition(type)
        addTypeParameters(ns, type, kclass)
        val (st, imp) = findSuperTypesAndImports(ns.qualifiedName, kclass)
        st.forEach { type.addSupertype(it) }
        imp.forEach {
            logInfo("'$type' requires import '$it' due to supertype")
            ns.addImport(it)
        }
        addConstructors(ns, type, kclass)
        addPropertiesAndImports(ns, type, kclass)
    }

    private fun addTypeParameters(ns: TypeNamespaceSimple, type: TypeDeclaration, kclass: KClass<*>) {
        when {
            kclass.typeParameters.isEmpty() -> Unit
            else -> {
                kclass.typeParameters.forEach {
                    type.addTypeParameter(SimpleName(it.name))
                }
            }
        }
    }

    private fun addConstructors(ns: TypeNamespaceSimple, type: TypeDeclaration, kclass: KClass<*>) {
        // must be declared in correct order
        kclass.primaryConstructor?.let { c ->
            c.name
            val prms = c.valueParameters.map { cp ->
                val pn = net.akehurst.language.typemodel.api.ParameterName(cp.name!!)
                val (ty, req) = cp.type.asTypeInstance(ns, type, substituteTypes)
                _requires[type.qualifiedName].addAll(req)
                ParameterDefinitionSimple(pn, ty, null)
            }
            when (type) {
                is ValueTypeSimple -> type.addConstructor(prms)
                is DataTypeSimple -> type.addConstructor(prms)
                else -> error("Cannot add a STORED Property to non StructuredType '$type'")
            }
        }
        //TODO: other constructors
    }

    private fun addPropertiesAndImports(ns: TypeNamespaceSimple, type: TypeDeclaration, kclass: KClass<*>) {
        //TODO: what about extension properties !
        kclass.declaredMemberProperties.forEach { mp ->
            if(mp.visibility == KVisibility.PUBLIC) {
                when {
                    mp.javaField == null -> { //must be derived
                        val pn = PropertyName(mp.name)
                        val (ty, req) = mp.returnType.asTypeInstance(ns, type, substituteTypes)
                        _requires[type.qualifiedName].addAll(req)
                        type.appendPropertyDerived(pn, ty, "from JVM reflection", "")
                    }

                    Lazy::class.java.isAssignableFrom(mp.javaField!!.type) -> { // also derived
                        val pn = PropertyName(mp.name)
                        val (ty, req) = mp.returnType.asTypeInstance(ns, type, substituteTypes)
                        _requires[type.qualifiedName].addAll(req)
                        type.appendPropertyDerived(pn, ty, "from JVM reflection", "")
                    }

                    else -> {
                        // assume stored
                        when (type) {
                            is StructuredType -> {
                                val pn = PropertyName(mp.name)
                                val (ty, req) = mp.returnType.asTypeInstance(ns, type, substituteTypes)
                                _requires[type.qualifiedName].addAll(req)
                                val vv = when (mp) {
                                    is KMutableProperty1<*, *> -> PropertyCharacteristic.READ_WRITE
                                    else -> PropertyCharacteristic.READ_ONLY // if stored and read only, must be identity
                                }
                                // Can't do this because typemodel not resolved yet!
                                val comp_ref = when(typeModelKindFor(mp.returnType.asPossiblyQualifiedName)) {
                                    is PrimitiveType -> PropertyCharacteristic.REFERENCE
                                    is ValueType -> PropertyCharacteristic.COMPOSITE
                                    is EnumType -> PropertyCharacteristic.REFERENCE
                                    is TupleType -> PropertyCharacteristic.COMPOSITE // Think this can't happen anyhow
                                    is UnnamedSupertypeType -> PropertyCharacteristic.COMPOSITE // Think this can't happen anyhow
                                    else -> {
                                        // no way to determine for Data/Interface/Collection ?
                                        when {
                                            mp.annotations.any { it.annotationClass == Komposite::class } -> PropertyCharacteristic.COMPOSITE
                                            else -> PropertyCharacteristic.REFERENCE
                                        }
                                    }
                                }
                                //val comp_ref2 = PropertyCharacteristic.REFERENCE
                                val chrs = setOf(vv,comp_ref )
                                type.appendPropertyStored(pn, ty, chrs)
                            }

                            else -> error("Cannot add a STORED Property to non StructuredType '$type'")
                        }
                    }
                }
            }
        }
    }

    private fun typeModelKindFor(typeQualifiedName: PossiblyQualifiedName): KClass<*> {
        val kclass = Class.forName(typeQualifiedName.value).kotlin
        return when {
            kclass.isValue -> ValueType::class
            kclass.isEnum -> EnumType::class
            kclass.isInterface -> InterfaceType::class
            kclass.isCollection -> CollectionType::class
            kclass.isData -> DataType::class
            else -> DataType::class //currently DataType used for other class types
        }
    }

    private fun findClasses(packageName: String): List<KClass<*>> {
        val result = mutableListOf<KClass<*>>()
        val path = "/" + packageName.replace('.', '/')
        val uri = this::class.java.getResource(path)!!.toURI()
        var fileSystem: FileSystem? = null
        val filePath = if (uri.scheme == "jar") {
            fileSystem = FileSystems.newFileSystem(uri, emptyMap<String, Any>())
            fileSystem.getPath(path)
        } else {
            Paths.get(uri)
        }
        val stream = Files.walk(filePath, 1)
            .filter { f -> !f.name.contains('$') && f.name.endsWith(".class") }
        for (file in stream) {
            val cn = file.name.dropLast(6) // remove .class
            val qn = "${packageName}.$cn"
            val kclass = Class.forName(qn).kotlin
            result.add(kclass)
        }
        fileSystem?.close()
        return result
    }
}