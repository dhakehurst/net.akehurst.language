package net.akehurst.language.agl.generators

import net.akehurst.kotlinx.komposite.processor.Komposite
import net.akehurst.language.base.api.*
import net.akehurst.language.collections.lazyMutableMapNonNull
import net.akehurst.language.typemodel.api.*
import net.akehurst.language.typemodel.asm.*
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
    val substituteTypes: Map<String, String>,
    val kompositeStr: List<String>
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
            "net.akehurst.language.collections.OrderedSet" to SimpleTypeModelStdLib.OrderedSet.qualifiedName.value,
            "kotlin.collections.Map" to SimpleTypeModelStdLib.Map.qualifiedName.value,
            "java.util.LinkedHashMap" to SimpleTypeModelStdLib.Map.qualifiedName.value,
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

        fun KType.substitutedName(substituteTypes: Map<String, String>): PossiblyQualifiedName {
            val pqtn = this.asPossiblyQualifiedName
            return substituteTypes[pqtn.value]?.let { QualifiedName(it) } ?: pqtn
        }

        val KType.isMutableCollection:Boolean get() {
            val clz = this.classifier
            return when(clz) {
                is KClass<*> -> clz.isSubclassOf(MutableCollection::class) || clz.isSubclassOf(MutableMap::class)
                is KTypeParameter -> TODO()
                else -> error("Unsupported")
            }
        }

        /**
         * returns the resolve TypeInstance, plus list of qualified names of other required types
        fun KType.asTypeInstance(ns: TypeNamespace, context: TypeDeclaration, substituteTypes: Map<String, String>): Pair<TypeInstance, List<QualifiedName>> {
            val requires = mutableListOf<QualifiedName>()
            val pqtn = this.asPossiblyQualifiedName
            val subName = when (pqtn) {
                is SimpleName -> pqtn
                is QualifiedName -> {
                    val qn = substituteTypes[pqtn.value]?.let { QualifiedName(it) } ?: pqtn
                    if (ns.qualifiedName != qn.front) {
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
                    null == pj.type -> Pair(SimpleTypeModelStdLib.AnyType, emptyList()) // '*' projection
                    else -> pj.type!!.asTypeInstance(ns, context, substituteTypes)
                }
            }
            targsp.forEach { requires.addAll(it.second) }
            val targs = targsp.map { it.first.asTypeArgument }
            val isNullable = this.isMarkedNullable
            val ty = ns.createTypeInstance(context, subName, targs, isNullable)
            return Pair(ty, requires)
        }
*/
        fun KClass<*>.allPropertiesNamed(propertyName: String) =
            this.declaredMemberProperties.filter { it.name == propertyName } +
                    this.allSupertypes.flatMap {
                        val cl = it.classifier
                        when (cl) {
                            is KTypeParameter -> emptyList()
                            is KClass<*> -> cl.memberProperties.filter { it.name == propertyName }
                            else -> error("Unsupported")
                        }
                    }

        fun KClass<*>.isKomposite(propertyName: String, komposite: TypeModel): Boolean =
            komposite.findByQualifiedNameOrNull(this.qualifiedName!!.asQualifiedName)
                ?.findPropertyOrNull(PropertyName(propertyName))
                ?.isComposite
                ?: false


        fun logInfo(message: String) {
            //println(message)
        }
    }

    private val _typeModel = TypeModelSimple(typeModelName)
    private val _komposite = kompositeStr.map {
        Komposite.process(it).let {
            assert(it.issues.errors.isEmpty()) { it.issues.errors.toString() }
            it.asm!!
        }
    }.reduce { acc, km ->
        acc.addAllNamespaceAndResolveImports(km.namespace)
        acc
    }

    private val _include = mutableListOf<QualifiedName>()
    private val _exclude = mutableListOf<QualifiedName>()
    private val _requires = lazyMutableMapNonNull<QualifiedName, MutableSet<QualifiedName>> { mutableSetOf() }

    fun include(qualifiedName: String) {
        _include.add(QualifiedName(qualifiedName))
    }

    fun exclude(qualifiedName: String) {
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

    fun addKClass(ns: TypeNamespaceSimple, kclass: KClass<*>) {
        when {
            _exclude.contains(QualifiedName(kclass.qualifiedName!!)) -> Unit
            kclass.supertypes.any { it.classifier == Annotation::class } -> Unit // do not add annotations
            // cannot detect DslBuilder annotations at runtime!
            //kclass.annotations.any { it.annotationClass.annotations.any { an -> an.annotationClass.isSubclassOf(DslMarker::class) } } -> Unit // do not add DSL builders
            KVisibility.PUBLIC == kclass.visibility -> {
                when {
                    null != kclass.objectInstance -> addSingleton(ns, kclass)
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
        _include.forEach {
            val ns = _typeModel.findOrCreateNamespace(it.front, emptyList()) as TypeNamespaceSimple
            val kclass = Class.forName(it.value).kotlin
            addKClass(ns, kclass)
        }
        additionalNamespaces.forEach { _typeModel.addNamespace(it) }

        _requires.forEach { (t, reqs) ->
            val notFound = reqs.mapNotNull { qn ->
                val foundReq = _typeModel.findByQualifiedNameOrNull(qn)
                if (null == foundReq) {
                    qn
                } else {
                    null
                }
            }
            if (notFound.isNotEmpty()) {
                println("Type '$t' requires the following:")
                notFound.forEach {
                    println("  '$it'")
                }
            }
        }

        _typeModel.resolveImports()
        return _typeModel
    }

    private fun addSuperTypes(type: TypeDeclaration, kclass: KClass<*>) {
        val imports = mutableListOf<Import>()
        val superTypes = kclass.supertypes.map {
            toTypeInstance(type, it)
        }
        superTypes.forEach { type.addSupertype(it) }
        imports.forEach {
            logInfo("'$type' requires import '$it' due to supertype")
            type.namespace.addImport(it)
        }
    }

    fun toTypeInstance(context:TypeDeclaration,kType: KType): TypeInstance {
        val targetNamespace = context.namespace
        val targetNamespaceName = targetNamespace.qualifiedName
        val imports = mutableListOf<Import>()
        val targs = kType.arguments.map {
            val tp = it.type
            when(tp) {
                null -> error("Unsupported")
                else -> toTypeInstance(context,tp).asTypeArgument
            }
        }
        val clz = kType.classifier
        val subName = kType.substitutedName(substituteTypes)
        val ti = when(clz) {
            is KClass<*> -> {
                when(subName) {
                    is QualifiedName -> {
                        if (subName.front == targetNamespaceName) {
                            //same package, not import
                            subName.last
                        } else {
                            val sbQn = substituteTypes[subName.value]?.let { QualifiedName(it) } ?: subName
                            imports.add(sbQn.front.asImport)
                            sbQn.last
                        }
                    }
                }
                targetNamespace.createTypeInstance(context, subName, targs, false)
            }
            is KTypeParameter -> TypeParameterReference(context, subName.simpleName)
            else -> error("Unsupported")
        }

        _requires[context.qualifiedName].addAll(imports.map {
            it.asQualifiedName
        })
        imports.forEach { context.namespace.addImport(it) }
        return ti
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

    private fun addSingleton(ns: TypeNamespaceSimple, kclass: KClass<*>) {
        val type = SingletonTypeSimple(ns, SimpleName(kclass.simpleName!!))
        ns.addDefinition(type)
        //       addTypeParameters(ns, type, kclass)
//        addSuperTypes(type, kclass)
//        addConstructors(ns, type, kclass)
//        addPropertiesAndImports(ns, type, kclass)
    }

    private fun addValueType(ns: TypeNamespaceSimple, kclass: KClass<*>) {
        val type = ValueTypeSimple(ns, SimpleName(kclass.simpleName!!))
        ns.addDefinition(type)
        addTypeParameters(ns, type, kclass)
        addSuperTypes(type, kclass)
        addConstructors(ns, type, kclass)
        addPropertiesAndImports(ns, type, kclass)
    }

    private fun addInterfaceType(ns: TypeNamespaceSimple, kclass: KClass<*>) {
        val type = InterfaceTypeSimple(ns, SimpleName(kclass.simpleName!!))
        ns.addDefinition(type)
        addTypeParameters(ns, type, kclass)
        addSuperTypes(type, kclass)
        addPropertiesAndImports(ns, type, kclass)
    }

    private fun addDataType(ns: TypeNamespaceSimple, kclass: KClass<*>) {
        val type = DataTypeSimple(ns, SimpleName(kclass.simpleName!!))
        ns.addDefinition(type)
        addTypeParameters(ns, type, kclass)
        addSuperTypes(type, kclass)
        addConstructors(ns, type, kclass)
        addPropertiesAndImports(ns, type, kclass)
    }

    private fun addTypeParameters(ns: TypeNamespaceSimple, type: TypeDeclaration, kclass: KClass<*>) {
        when {
            kclass.typeParameters.isEmpty() -> Unit
            else -> {
                kclass.typeParameters.forEach {
                    type.addTypeParameter(TypeParameterSimple(SimpleName(it.name)))
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
                //val (ty, req) = cp.type.asTypeInstance(ns, type, substituteTypes)
                val ti = toTypeInstance(type, cp.type)
                ParameterDefinitionSimple(pn, ti, null)
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
        val props = when {
            kclass.isInterface -> kclass.declaredMemberProperties
            else -> kclass.memberProperties
        }

        kclass.declaredMemberProperties.forEach { mp ->
            val comp_ref = kompOrRefFor(kclass, mp)
            if (mp.visibility == KVisibility.PUBLIC || comp_ref.first == PropertyCharacteristic.COMPOSITE) {
                when {
                    mp.javaField == null -> { //must be derived
                        val pn = PropertyName(mp.name)
                        //val (ty, req) = mp.returnType.asTypeInstance(ns, type, substituteTypes)
                        val ti = toTypeInstance(type, mp.returnType)
                        when {
                            comp_ref.second -> addStoredProperty(ns, type, mp, comp_ref.first) // explicit komposite must be stored
                            else -> type.appendPropertyDerived(pn, ti, "from JVM reflection", "")
                        }
                    }

                    Lazy::class.java.isAssignableFrom(mp.javaField!!.type) -> { // also derived
                        val pn = PropertyName(mp.name)
                        //val (ty, req) = mp.returnType.asTypeInstance(ns, type, substituteTypes)
                        val ti = toTypeInstance(type, mp.returnType)
                        type.appendPropertyDerived(pn, ti, "from JVM reflection", "")
                    }

                    else -> {
                        addStoredProperty(ns, type, mp, comp_ref.first) // assume stored
                    }
                }
            }
        }
    }

    private fun addStoredProperty(ns: TypeNamespaceSimple, type: TypeDeclaration, mp: KProperty1<*, *>, comp_ref: PropertyCharacteristic) {
        when (type) {
            is StructuredType -> {
                val pn = PropertyName(mp.name)
                //val (ty, req) = mp.returnType.asTypeInstance(ns, type, substituteTypes)
                val ti = toTypeInstance(type, mp.returnType)
                val vv = when {
                    mp is KMutableProperty1<*, *> -> PropertyCharacteristic.READ_WRITE
                    mp.returnType.isMutableCollection -> PropertyCharacteristic.READ_WRITE
                    else -> PropertyCharacteristic.READ_ONLY // if stored and read only, must be identity
                }

                val chrs = setOf(vv, comp_ref)
                type.appendPropertyStored(pn, ti, chrs)
            }

            else -> error("Cannot add a STORED Property to non StructuredType '$type'")
        }
    }

    /**
     * returns COMPOSITE or REFERENCE and whether the property is explicitly marked as composite
     */
    private fun kompOrRefFor(kclass: KClass<*>, mp: KProperty1<*, *>): Pair<PropertyCharacteristic, Boolean> {
        val overrideKomposite = kclass.allSupertypes.any {
            val clz = it.classifier
            when (clz) {
                null -> false
                is KClass<*> -> clz.isKomposite(mp.name, _komposite)
                else -> false
            }
        }
        return when {
            overrideKomposite -> Pair(PropertyCharacteristic.COMPOSITE, true)
            kclass.isKomposite(mp.name, _komposite) -> Pair(PropertyCharacteristic.COMPOSITE, true)
            else -> Pair(autoDetectKompOrRefFor(mp.returnType),false)
        }
    }

    private fun autoDetectKompOrRefFor(type: KType): PropertyCharacteristic {
        val kind = typeModelKindFor(type)
        return when {
            null == kind -> PropertyCharacteristic.REFERENCE
            PrimitiveType::class.isSuperclassOf(kind) -> PropertyCharacteristic.REFERENCE
            ValueType::class.isSuperclassOf(kind) -> PropertyCharacteristic.COMPOSITE
            EnumType::class.isSuperclassOf(kind) -> PropertyCharacteristic.REFERENCE
            TupleType::class.isSuperclassOf(kind) -> PropertyCharacteristic.COMPOSITE // Think this can't happen anyhow
            UnnamedSupertypeType::class.isSuperclassOf(kind) -> PropertyCharacteristic.COMPOSITE // Think this can't happen anyhow
            CollectionType::class.isSuperclassOf(kind) -> {
                val arg = type.arguments.last()
                arg.type?.let { autoDetectKompOrRefFor(it) }
                    ?: return PropertyCharacteristic.REFERENCE
            }

            else -> PropertyCharacteristic.REFERENCE // no way to determine for Data/Interface/Collection ?
        }
    }

    private fun typeModelKindFor(ktype: KType): KClass<*>? {
        val subName = ktype.substitutedName(substituteTypes)
        return when (subName) {
            is SimpleName -> null
            is QualifiedName -> {
                val foundType = SimpleTypeModelStdLib.findOwnedTypeNamed(subName.last)
                when (foundType) {
                    null -> {
                        val kclass = Class.forName(subName.value).kotlin
                        when {
                            kclass.isValue -> ValueType::class
                            kclass.isEnum -> EnumType::class
                            kclass.isInterface -> InterfaceType::class
                            kclass.isCollection -> CollectionType::class
                            kclass.isData -> DataType::class
                            else -> DataType::class //currently DataType used for other class types
                        }
                    }

                    else -> foundType::class
                }
            }

            else -> error("Unsupported")
        }

    }

    private fun findClasses(packageName: String): List<KClass<*>> {
        val result = mutableListOf<KClass<*>>()
        val path = "/" + packageName.replace('.', '/')
        val uri = this::class.java.getResource(path)?.toURI() ?: error("Cannot find resource with path '$path'")
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