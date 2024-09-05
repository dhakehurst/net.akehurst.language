package net.akehurst.language.agl.generators

import net.akehurst.language.api.language.base.Import
import net.akehurst.language.api.language.base.QualifiedName
import net.akehurst.language.api.language.base.SimpleName
import net.akehurst.language.typemodel.api.*
import net.akehurst.language.typemodel.simple.*
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.name
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.isSubclassOf


class GenerateTypeModelViaReflection(
    val typeModelName: SimpleName,
    val resolveImports: Boolean = false,
    val additionalNamespaces:List<String> = emptyList(),
) {
    companion object {
        const val STD_LIB = "net.akehurst.language.typemodel.simple.SimpleTypeModelStdLib"

        val KClass<*>.isInterface get() = java.isInterface
        val KClass<*>.isEnum get()= this.isSubclassOf(Enum::class)
        val KClass<*>.isCollection get()= this.isSubclassOf(Collection::class)

    }

    private val _sb = StringBuilder()
    private val _typeModel = TypeModelSimple(typeModelName)

    fun namespace(packageName: String) {
        val imports = mutableListOf<Import>()
        val ns = TypeNamespaceSimple(QualifiedName(packageName), imports)
        val kclasses = findClasses(packageName)

        for (kclass in kclasses) {
            when {
                KVisibility.PUBLIC == kclass.visibility -> {
                    when {
                        kclass.isValue -> ns.addDefinition(ValueTypeSimple(ns, SimpleName(kclass.simpleName!!))) //TODO: ValueType ?
                        kclass.isInterface -> ns.addDefinition(InterfaceTypeSimple(ns, SimpleName(kclass.simpleName!!))) //TODO: InterfaceType ?
                        kclass.isEnum -> {
                            val lits = emptyList<String>() //TODO
                            ns.addDefinition(EnumTypeSimple(ns, SimpleName(kclass.simpleName!!),lits))
                        }
                        kclass.isCollection-> ns.addDefinition(CollectionTypeSimple(ns, SimpleName(kclass.simpleName!!))) //TODO: InterfaceType ?

                        else -> ns.addDefinition(DataTypeSimple(ns, SimpleName(kclass.simpleName!!)))
                    }
                }
                else -> {
                    println("Not generating '${kclass.simpleName}' as it is not public")
                }
            }
        }

        _typeModel.addNamespace(ns)
    }


    fun generate():String {
        typeModel(
            name = "",
            resolveImports = true,
            namespaces = emptyList(),
        ) {}
        if (resolveImports) {
            _typeModel.resolveImports()
        }
        startTypeModel()
        for (ns in _typeModel.namespace) {
            generateNamespace(ns)
        }
        endTypeModel()
        return _sb.toString()
    }

    private fun startTypeModel() {
        val ans = additionalNamespaces.joinToString(separator = ",")
        _sb.append("typeModel($typeModelName, $resolveImports, listOf($ans)) {\n")
    }
    private fun endTypeModel() {
        _sb.append("}")
    }

    private fun generateNamespace(ns: TypeNamespace) {
        startNamespace()
        for (tp in ns.primitiveType) {
            generatePrimitiveType(tp)
        }
        for (tp in ns.enumType) {
            generateEnumType(tp)
        }
        for (tp in ns.collectionType) {
            generateCollectionType(tp)
        }
        for (tp in ns.valueType) {
            generateValueType(tp)
        }
        for (tp in ns.interfaceType) {
            generateInterfaceType(tp)
        }
        for (tp in ns.dataType) {
            generateDataType(tp)
        }
        endNamespace()
    }

    private fun startNamespace() {
        _sb.append("namespace() {\n")
    }

    private fun endNamespace() {
        _sb.append("}\n")
    }

    private fun generatePrimitiveType(tp: PrimitiveType) {
        _sb.append("primitiveType(${tp.name.value})\n")
    }

    private fun generateEnumType(tp: EnumType) {
        _sb.append("enumType(${tp.name.value})\n")
    }

    private fun generateCollectionType(tp: CollectionType) {
        _sb.append("collectionType(${tp.name.value})\n")
    }

    private fun generateValueType(tp: ValueType) {
        _sb.append("valueType(${tp.name.value})\n")
    }

    private fun generateInterfaceType(tp: InterfaceType) {
        _sb.append("interfaceType(${tp.name.value})\n")
    }

    private fun generateDataType(tp:DataType) {
        _sb.append("dataType(${tp.name.value})\n")
    }

    fun findClasses(packageName: String):List<KClass<*>> {
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