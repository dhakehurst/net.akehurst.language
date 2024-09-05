package net.akehurst.language.agl.generators

import net.akehurst.language.agl.generators.GenerateTypeModelViaReflection.Companion.asPossiblyQualifiedName
import net.akehurst.language.api.language.base.Import
import net.akehurst.language.api.language.base.PossiblyQualifiedName
import net.akehurst.language.api.language.base.PossiblyQualifiedName.Companion.asPossiblyQualifiedName
import net.akehurst.language.api.language.base.QualifiedName
import net.akehurst.language.api.language.base.SimpleName
import net.akehurst.language.typemodel.api.*
import net.akehurst.language.typemodel.simple.*
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.name
import kotlin.reflect.*
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.javaField


class GenerateTypeModelViaReflection(
    val typeModelName: SimpleName,
    val resolveImports: Boolean = false,
    val additionalNamespaces:List<TypeNamespace> = emptyList(),
    val substituteNamespaces:Map<String,String>
) {
    companion object {
        const val STD_LIB = "net.akehurst.language.typemodel.simple.SimpleTypeModelStdLib"

        val KClass<*>.isInterface get() = java.isInterface
        val KClass<*>.isEnum get()= this.isSubclassOf(Enum::class)
        val <T : Enum<T>> KClass<T>.enumValues: Array<T> get()= java.enumConstants
        val KClass<*>.isCollection get()= this.isSubclassOf(Collection::class)
        val KClassifier.asPossiblyQualifiedName get() = when(this) {
            is KClass<*> -> this.qualifiedName!!.asPossiblyQualifiedName
            is KTypeParameter -> this.name.asPossiblyQualifiedName
            else -> error("Unsupported")
        }
        val KType.asPossiblyQualifiedName get() = this.classifier!!.asPossiblyQualifiedName
        fun KType.asTypeInstance(ns:TypeNamespace,context:TypeDeclaration, substituteNamespaces:Map<String,String>):TypeInstance {
            val pqtn = this.asPossiblyQualifiedName
            val subName = when(pqtn) {
                is SimpleName -> pqtn
                is QualifiedName -> {
                    val qn = substituteNamespaces[pqtn.front.value]?.let { QualifiedName(it) } ?: pqtn.front
                    qn.append(pqtn.last)
                }
                else -> error("Unsupported")
            }

            val targs = this.arguments.map { pj ->
                pj.type!!.asTypeInstance(ns,context, substituteNamespaces)
            }
            val isNullable = this.isMarkedNullable
            return ns.createTypeInstance(context, subName, targs, isNullable)
        }
    }

    private val _typeModel = TypeModelSimple(typeModelName)

    fun addPackage(packageName: String, initialImports:List<Import> = emptyList()) {
        val ns = TypeNamespaceSimple(QualifiedName(packageName), initialImports)

        val kclasses = findClasses(packageName)

        for (kclass in kclasses) {
            when {
                KVisibility.PUBLIC == kclass.visibility -> {
                    when {
                        kclass.isValue -> addValueType(ns, kclass)
                        kclass.isEnum -> addEnumType(ns,kclass as KClass<out Enum<*>>)
                        kclass.isInterface -> addInterfaceType(ns, kclass)
                        kclass.isCollection-> addCollectionType(ns, kclass)
                        else -> addDataType(ns, kclass)
                    }
                }
                else -> {
                    println("Not generating '${kclass.simpleName}' as it is not public")
                }
            }
        }

        _typeModel.addNamespace(ns)
    }

    fun generate():TypeModel {
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
                    propertyOf(typeName = "t", characteristics = setOf(), propertyName = "n", isNullable = true) {
                        typeArgument(qualifiedTypeName = "")
                    }
                }
            }
        }

        _typeModel.addAllNamespace(additionalNamespaces)
        if (resolveImports) {
            _typeModel.resolveImports()
        }
        return _typeModel
    }


    private fun findSuperTypesAndImports(targetNamespaceName:QualifiedName, kclass: KClass<*>) : Pair<List<PossiblyQualifiedName>,List<Import>> {
        val imports = mutableListOf<Import>()
        val superTypes = kclass.supertypes.map {
            val qn = QualifiedName((it.classifier as KClass<*>).qualifiedName!!)
            val pn = qn.front
            val sn = qn.last
            if(pn==targetNamespaceName) {
                //same package, not import
            } else {
                val imp = substituteNamespaces[pn.value]?.let { Import(it) }
                    ?: pn.asImport
                imports.add(imp)
            }
            sn
        }
        return Pair(superTypes, imports)
    }


    private fun addPrimitiveType(ns:TypeNamespaceSimple, kclass: KClass<*>) {
        ns.addDefinition(PrimitiveTypeSimple(ns, SimpleName(kclass.simpleName!!)))
    }

    private fun <T: Enum<T>> addEnumType(ns:TypeNamespaceSimple, kclass: KClass<T>) {
        val lits =kclass.enumValues.map { it.name }
        ns.addDefinition(EnumTypeSimple(ns, SimpleName(kclass.simpleName!!),lits))
    }

    private fun addCollectionType(ns:TypeNamespaceSimple, kclass: KClass<*>) {
        ns.addDefinition(CollectionTypeSimple(ns, SimpleName(kclass.simpleName!!)))
    }

    private fun addValueType(ns:TypeNamespaceSimple, kclass: KClass<*>) {
        val type = ValueTypeSimple(ns, SimpleName(kclass.simpleName!!))
        ns.addDefinition(type)
        addTypeParameters(ns,type, kclass)
        val (st, imp) = findSuperTypesAndImports(ns.qualifiedName, kclass)
        st.forEach{ type.addSupertype(it) }
        imp.forEach { ns.addImport(it) }

        addPropertiesAndImports(ns,type, kclass)
    }

    private fun addInterfaceType(ns:TypeNamespaceSimple, kclass: KClass<*>) {
        val type = InterfaceTypeSimple(ns, SimpleName(kclass.simpleName!!))
        ns.addDefinition(type)
        addTypeParameters(ns,type, kclass)

        val (st, imp) = findSuperTypesAndImports(ns.qualifiedName, kclass)
        st.forEach{ type.addSupertype(it) }
        imp.forEach { ns.addImport(it) }

        addPropertiesAndImports(ns,type, kclass)
    }

    private fun addDataType(ns:TypeNamespaceSimple, kclass: KClass<*>) {
        val type = DataTypeSimple(ns, SimpleName(kclass.simpleName!!))
        ns.addDefinition(type)
        addTypeParameters(ns,type, kclass)
        val (st, imp) = findSuperTypesAndImports(ns.qualifiedName, kclass)
        st.forEach{ type.addSupertype(it) }
        imp.forEach { ns.addImport(it) }

        addPropertiesAndImports(ns,type, kclass)
    }

    private fun addTypeParameters(ns: TypeNamespaceSimple, type:TypeDeclaration, kclass: KClass<*>) {
        when {
            kclass.typeParameters.isEmpty() -> Unit
            else -> {
                kclass.typeParameters.forEach {
                    type.addTypeParameter(SimpleName(it.name))
                }
            }
        }
    }

    private fun addPropertiesAndImports(ns: TypeNamespaceSimple, type:TypeDeclaration, kclass: KClass<*>) {
        //TODO: what about extension properties !
        kclass.declaredMemberProperties.forEach { mp ->
            when {
                mp.javaField == null -> { //must be derived
                    val pn = PropertyName( mp.name)
                    val ty = mp.returnType.asTypeInstance(ns,type,substituteNamespaces)
                    type.appendPropertyDerived(pn, ty, "from JVM reflection", "")
                }
                else -> {
                    // assume stored
                    when(type) {
                        is StructuredType -> {
                            val pn = PropertyName( mp.name)
                            val ty = mp.returnType.asTypeInstance(ns,type,substituteNamespaces)
                            val chrs = setOf(PropertyCharacteristic.MEMBER)
                            type.appendPropertyStored(pn, ty, chrs)
                        }
                        else -> error("Cannot add a STORED Property to non StructuredType '$type'")
                    }
                }
            }

        }
    }

    private fun findClasses(packageName: String):List<KClass<*>> {
        val result = mutableListOf<KClass<*>>()
        val path = "/"+packageName.replace('.', '/')
        val uri = this::class.java.getResource(path)!!.toURI()
        val filePath = if (uri.scheme == "jar") {
            val fileSystem = FileSystems.newFileSystem(uri, emptyMap<String, Any>())
            fileSystem.getPath(path)
        } else {
           Paths.get(uri)
        }
        val stream = Files.walk(filePath, 1)
            .filter{f-> !f.name.contains('$') && f.name.endsWith(".class") }
        for (file in stream) {
            val cn = file.name.dropLast(6) // remove .class
            val qn = "${packageName}.$cn"
            val kclass = Class.forName(qn).kotlin
            result.add(kclass)
        }
        return result
    }
}