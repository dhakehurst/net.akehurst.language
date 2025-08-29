package net.akehurst.language.agl.generators

import net.akehurst.language.base.api.Indent
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.types.api.*
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.asm.TypeParameterReference

data class TypeModelFormatConfiguration(
    val exludedNamespaces: List<QualifiedName> = emptyList(),
    val includeInterfaces: Boolean = true,
    val properties: PropertiesTypeModelFormatConfiguration
)

data class PropertiesTypeModelFormatConfiguration(
    val includeDerived: Boolean = true
)

class FormatTypeModelAsKotlinTypeModelBuilder(
    val configuration: TypeModelFormatConfiguration
) {

    companion object {
        fun <T : Any> List<T>.joinAsCommerSeparatedStrings(func: (el: T) -> CharSequence = { it.toString() }): String {
            return if (this.isNotEmpty()) {
                this.joinToString(separator = ", ") { "\"${func.invoke(it)}\"" }
            } else {
                ""
            }
        }

        fun <T : Any> StringBuilder.appendWithEol(collection: Collection<T>, func: (el: T) -> CharSequence) {
            if (collection.isNotEmpty()) {
                this.append(collection.joinToString(separator = "\n", transform = func))
                this.append("\n")
            }
        }
    }

    fun formatTypeModel(indent: Indent, typesDomain: TypesDomain, resolveImports: Boolean, additionalNamespaces: List<String>): String {
        val sb = StringBuilder()
        val ans = additionalNamespaces.joinToString(separator = ",")
        sb.append("typeModel(\"${typesDomain.name.value}\", $resolveImports, listOf($ans)) {\n")
        val ns = typesDomain.namespace.filterNot { configuration.exludedNamespaces.contains(it.qualifiedName) }
        sb.append(ns.joinToString(separator = "\n") { formatNamespace(indent.inc, it) })
        sb.append("}")
        return sb.toString()
    }

    fun formatNamespace(indent: Indent, namespace: TypesNamespace): String {
        val sb = StringBuilder()
        val qn = namespace.qualifiedName.value
        val imports = namespace.import.joinAsCommerSeparatedStrings { it.value }
        sb.append("  namespace(\"$qn\", listOf($imports)) {\n")
        sb.appendWithEol(namespace.singletonType) { formatSingletonType(indent.inc, namespace, it) }
        sb.appendWithEol(namespace.primitiveType) { formatPrimitiveType(indent.inc, namespace, it) }
        sb.appendWithEol(namespace.enumType) { formatEnumType(indent.inc, namespace, it) }
        sb.appendWithEol(namespace.collectionType) { formatCollectionType(indent.inc, namespace, it) }
        sb.appendWithEol(namespace.valueType) { formatValueType(indent.inc, namespace, it) }
        sb.appendWithEol(namespace.interfaceType) { formatInterfaceType(indent.inc, namespace, it) }
        sb.appendWithEol(namespace.dataType) { formatDataType(indent.inc, namespace, it) }
        sb.append("  }")
        return sb.toString()
    }

    fun formatTypeMembers(indent: Indent, context: TypesNamespace, type: TypeDefinition): String {
        val sb = StringBuilder()
        sb.appendWithEol(type.property.filter { it.isStored }) { formatProperty(indent, context, it) }
        if (configuration.properties.includeDerived) {
            sb.appendWithEol(type.property.filter { it.isDerived }) { formatProperty(indent, context, it) }
        }
        sb.appendWithEol(type.method) { "// fun ${it.name}" }
        return sb.toString()
    }

    fun formatSingletonType(indent: Indent, context: TypesNamespace, type: SingletonType): String {
        val sb = StringBuilder()
        val tn = type.name.value
        sb.append("${indent}singleton(\"$tn\")")
        return sb.toString()
    }

    fun formatPrimitiveType(indent: Indent, context: TypesNamespace, type: PrimitiveType): String {
        val sb = StringBuilder()
        val tn = type.name.value
        sb.append("${indent}primitive($tn)")
        formatTypeMembers(indent.inc, context, type)
        return sb.toString()
    }

    fun formatEnumType(indent: Indent, context: TypesNamespace, type: EnumType): String {
        val sb = StringBuilder()
        val tn = type.name.value
        val lits = type.literals.joinToString(separator = ", ") { "\"$it\"" }
        sb.append("    enum(\"$tn\", listOf($lits))")
        return sb.toString()
    }

    fun formatCollectionType(indent: Indent, context: TypesNamespace, type: CollectionType): String {
        val sb = StringBuilder()
        val tn = type.name
        sb.append("${indent}collection(\"$tn\")")
        return sb.toString()
    }

    fun formatValueType(indent: Indent, context: TypesNamespace, type: ValueType): String {
        val sb = StringBuilder()
        val tn = type.name
        sb.append("${indent} value(\"$tn\") {\n")
        sb.append(formatSupertypes(indent.inc, context, type.supertypes))
        sb.append(formatConstructors(indent.inc, context, type.constructors))
        sb.append(formatTypeMembers(indent.inc, context, type))
        sb.append("${indent}}")
        return sb.toString()
    }

    fun formatInterfaceType(indent: Indent, context: TypesNamespace, type: InterfaceType): String {
        val sb = StringBuilder()
        val tn = type.name
        sb.append("${indent}interface_(\"$tn\") {\n")
        sb.append(formatTypeParameters(indent.inc, context, type.typeParameters.map { it.name }))
        sb.append(formatSupertypes(indent.inc, context, type.supertypes))
        sb.append(formatTypeMembers(indent.inc, context, type))
        sb.append("${indent}}")
        return sb.toString()
    }

    fun formatDataType(indent: Indent, context: TypesNamespace, type: DataType): String {
        val sb = StringBuilder()
        val tn = type.name
        sb.append("${indent}data(\"$tn\") {\n")
        sb.append(formatTypeParameters(indent.inc, context, type.typeParameters.map { it.name }))
        sb.append(formatSupertypes(indent.inc, context, type.supertypes))
        sb.append(formatConstructors(indent.inc, context, type.constructors))
        sb.append(formatTypeMembers(indent.inc, context, type))
        sb.append("${indent}}")
        return sb.toString()
    }

    fun formatTypeParameters(indent: Indent, context: TypesNamespace, typeParameters: List<SimpleName>): String {
        return when {
            typeParameters.isEmpty() -> ""
            else -> {
                val tps = typeParameters.joinAsCommerSeparatedStrings { it.value }
                "${indent}typeParameters($tps)\n"
            }
        }
    }

    fun formatSupertypes(indent: Indent, context: TypesNamespace, supertypes: List<TypeInstance>): String {
        return when {
            supertypes.isEmpty() -> ""
            else -> {
                supertypes
                    .filterNot { it == StdLibDefault.AnyType }
                    .joinToString(separator = "\n", postfix = "\n") { st ->
                        val pqn = when {
                            st.resolvedDeclaration.namespace == context -> st.typeName.value
                            context.import.contains(st.qualifiedTypeName.front.asImport) -> st.typeName.value
                            else -> st.qualifiedTypeName.value // TODO: add import if not name clash
                        }
                        val targs = when {
                            st.typeArguments.isEmpty() -> ""
                            else -> st.typeArguments.joinToString(prefix = "{ ", separator = "; ", postfix = " }") { ta ->
                                val tn = ta.type.possiblyQualifiedNameInContext(context)
                                "ref(\"$tn\")"
                            }
                        }
                        "${indent}supertype(\"$pqn\")$targs"
                    }
            }
        }
    }

    fun formatConstructors(indent: Indent, context: TypesNamespace, constructors: List<ConstructorDeclaration>): String {
        return when {
            constructors.isEmpty() -> ""
            else -> {
                constructors.joinToString(separator = "\n") { c ->
                    when {
                        c.parameters.isEmpty() -> "${indent}constructor_ {}\n"
                        else -> {
                            val params = c.parameters.joinToString(separator = "\n") { p ->
                                val t = when {
                                    p.typeInstance.namespace == context -> p.typeInstance.typeName.value
                                    else -> p.typeInstance.qualifiedTypeName.value // TODO: add import if not name clash
                                }
                                val nullable = p.typeInstance.isNullable
                                "${indent.inc}parameter(\"${p.name.value}\", \"$t\", $nullable)"
                            }
                            "${indent}constructor_ {\n${params}\n${indent}}\n"
                        }
                    }
                }
            }
        }
    }

    fun formatProperty(indent: Indent, context: TypesNamespace, pd: PropertyDeclaration): String {
        val characteristics = pd.characteristics.joinToString(separator = ", ") {
            when(it) {
                PropertyCharacteristic.REFERENCE -> "REF"
                PropertyCharacteristic.COMPOSITE -> "CMP"
                PropertyCharacteristic.DERIVED -> "DER"
                PropertyCharacteristic.IDENTITY -> "IDY"
                PropertyCharacteristic.PRIMITIVE -> "PRM"
                PropertyCharacteristic.READ_ONLY -> "VAL"
                PropertyCharacteristic.READ_WRITE -> "VAR"
                PropertyCharacteristic.CONSTRUCTOR -> "CON"
                PropertyCharacteristic.STORED -> "STR"
            }
        }
        val propertyName = pd.name
        val typeName = when {
            pd.typeInstance.namespace == context -> pd.typeInstance.typeName
            else -> pd.typeInstance.qualifiedTypeName // TODO: add import if not name clash
        }
        val isNullable = pd.typeInstance.isNullable
        val typeArgs = when {
            pd.typeInstance.typeArguments.isEmpty() -> ""
            else -> {
                val sb = StringBuilder()
                sb.append("{\n")
                sb.appendWithEol(pd.typeInstance.typeArguments) {
                    val ty = it.type
                    val tan = when{
                        ty is TypeParameterReference -> ty.typeParameterName.value
                        ty.namespace == context -> ty.typeName.value
                        else -> ty.qualifiedTypeName.value
                    }
                    "${indent.inc}typeArgument(\"$tan\")" //TODO: nested args
                }
                sb.append("${indent}}")
                sb.toString()
            }
        }
        return "${indent}propertyOf(setOf($characteristics), \"$propertyName\", \"$typeName\", $isNullable)$typeArgs"
    }

}