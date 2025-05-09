package net.akehurst.language.agl.typemodel.processor

import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserByMethodRegistrationAbstract
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.base.api.Import
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.asm.OptionHolderDefault
import net.akehurst.language.base.processor.BaseSyntaxAnalyser
import net.akehurst.language.collections.ListSeparated
import net.akehurst.language.collections.toSeparatedList
import net.akehurst.language.grammar.api.SeparatedList
import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sppt.api.SpptDataNodeInfo
import net.akehurst.language.typemodel.api.*
import net.akehurst.language.typemodel.asm.*

data class TypeRefInfo(
    val name: PossiblyQualifiedName,
    val args: List<TypeRefInfo>,
    val isNullable: Boolean
) {
    fun toTypeInstance(contextType: TypeDefinition): TypeInstance {
        val targs = args.map { it.toTypeInstance(contextType).asTypeArgument }
        return contextType.namespace.createTypeInstance(contextType.qualifiedName, name, targs, isNullable)
    }
}

class TypesSyntaxAnalyser : SyntaxAnalyserByMethodRegistrationAbstract<TypeModel>() {

    override val extendsSyntaxAnalyser: Map<QualifiedName, SyntaxAnalyser<*>> = mapOf(
        QualifiedName("Base") to BaseSyntaxAnalyser()
    )

    override fun registerHandlers() {

        super.register(this::unit)
        super.register(this::namespace)
        super.register(this::definition)
        super.register(this::singletonDefinition)
        super.register(this::primitiveDefinition)
        super.register(this::enumDefinition)
        super.register(this::enumLiterals)
        super.register(this::valueDefinition)
        super.register(this::collectionDefinition)
        super.register(this::unionDefinition)
        super.register(this::interfaceDefinition)
        super.register(this::interfaceBody)
        super.register(this::dataDefinition)
        super.register(this::supertypes)
        super.register(this::constructor)
        super.register(this::constructorParameter)
        super.register(this::property)
        super.register(this::cmp_ref)
        super.register(this::val_var)
        super.register(this::typeReference)
        super.register(this::typeArgumentList)
    }

    // --- Typemodel ---

    // override unit = option* namespace* ;
    private fun unit(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): TypeModel {
        val options = children[0] as List<Pair<String, String>>
        val namespaces = children[1] as List<TypeNamespace>
        val name = SimpleName("ParsedTypesUnit") //TODO: how to specify name, does it matter?

        val optHolder = OptionHolderDefault(null, options.associate { it })
        namespaces.forEach { (it.options as OptionHolderDefault).parent = optHolder }
        return TypeModelSimple(
            name = name,
            optHolder
        ).also {
            namespaces.forEach { ns -> it.addNamespace(ns) }
        }
    }

    // namespace = 'namespace' possiblyQualifiedName option* import* definition* ;
    private fun namespace(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): TypeNamespace {
        val pqn = children[1] as PossiblyQualifiedName
        val options = children[2] as List<Pair<String, String>>
        val imports = children[3] as List<Import>
        val defs = children[4] as List<(namespace: TypeNamespace) -> TypeDefinition>
        val optHolder = OptionHolderDefault(null, options.associate { it })

        val nsName = pqn.asQualifiedName(null)
        val namespace = TypeNamespaceSimple(nsName, optHolder, imports)
        defs.forEach { def -> def.invoke(namespace) }
        return namespace
    }

    /*     override definition
              = singletonDefinition
              | primitiveDefinition
              | enumDefinition
              | valueDefinition
              | collectionDefinition
              | dataDefinition
              | interfaceDefinition
              | unionDefinition
              ;*/
    private fun definition(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (namespace: TypeNamespace) -> TypeDefinition =
        children[0] as (namespace: TypeNamespace) -> TypeDefinition

    // singletonDefinition = 'singleton' IDENTIFIER ;
    private fun singletonDefinition(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (namespace: TypeNamespace) -> SingletonType {
        val name = SimpleName(children[1] as String)
        return { namespace: TypeNamespace ->
            SingletonTypeSimple(namespace, name).also { setLocationFor(it, nodeInfo, sentence) }
        }
    }

    // primitiveDefinition = 'primitive' IDENTIFIER ;
    private fun primitiveDefinition(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (namespace: TypeNamespace) -> PrimitiveType {
        val name = SimpleName(children[1] as String)
        return { namespace: TypeNamespace ->
            PrimitiveTypeSimple(namespace, name).also { setLocationFor(it, nodeInfo, sentence) }
        }
    }

    // enum = 'enum' NAME literals?;
    private fun enumDefinition(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (namespace: TypeNamespace) -> EnumType {
        val name = SimpleName(children[1] as String)
        val literals = children[2] as List<String>? ?: emptyList()
        return { namespace: TypeNamespace ->
            EnumTypeSimple(namespace, name, literals).also { setLocationFor(it, nodeInfo, sentence) }
        }
    }

    // enumLiterals = '{' [IDENTIFIER / ',']+ '}' ;
    private fun enumLiterals(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<String> {
        val list = children[1] as List<String>
        return list.toSeparatedList().items
    }

    // valueDefinition = 'value' IDENTIFIER ;
    private fun valueDefinition(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (namespace: TypeNamespace) -> ValueType {
        val name = SimpleName(children[1] as String)
        return { namespace: TypeNamespace ->
            ValueTypeSimple(namespace, name).also { setLocationFor(it, nodeInfo, sentence) }
        }
    }

    // collectionDefinition = 'collection' IDENTIFIER '<' typeParameterList '>' ;
    private fun collectionDefinition(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (namespace: TypeNamespace) -> CollectionType {
        val name = SimpleName(children[1] as String)
        val params = (children[3] as List<String>).map { TypeParameterSimple(SimpleName((it))) }
        return { namespace: TypeNamespace ->
            CollectionTypeSimple(namespace, name, params).also { setLocationFor(it, nodeInfo, sentence) }
        }
    }

    // unionDefinition = 'union' IDENTIFIER '{' alternatives '}' ;
    private fun unionDefinition(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (namespace: TypeNamespace) -> UnionType {
        val name = SimpleName(children[1] as String)
        val alternatives = (children[3] as List<Any>).toSeparatedList<Any, TypeRefInfo, String>().items
        return { namespace: TypeNamespace ->
            UnionTypeSimple(namespace, name).also { ut ->
                alternatives.forEach { tr ->
                    val ti = tr.toTypeInstance(ut)
                    ut.addAlternative(ti)
                }
            }.also { setLocationFor(it, nodeInfo, sentence) }
        }
    }

    // interfaceDefinition = 'interface' IDENTIFIER supertypes? interfaceBody? ;
    private fun interfaceDefinition(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (namespace: TypeNamespace) -> InterfaceType {
        val name = SimpleName(children[1] as String)
        val supertypes = children[2] as List<TypeRefInfo>? ?: emptyList()
        val property = children[3] as List<((InterfaceType) -> PropertyDeclaration)>? ?: emptyList()
        return { namespace: TypeNamespace ->
            InterfaceTypeSimple(namespace, name).also { ift ->
                supertypes.forEach { ift.addSupertype_dep(it.name) }
                property.forEach {
                    it.invoke(ift).also { p -> setResolvers(p.typeInstance as TypeInstanceSimple, ift) }
                }
            }.also { setLocationFor(it, nodeInfo, sentence) }
        }
    }

    // interfaceBody = '{' property* '}' ;
    private fun interfaceBody(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<((InterfaceType) -> PropertyDeclaration)> =
        children[1] as List<((InterfaceType) -> PropertyDeclaration)>


    /*
     dataDefinition =
       'data' IDENTIFIER supertypes? '{'
          constructor*
          property*
       '}'
    ;*/
    private fun dataDefinition(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (namespace: TypeNamespace) -> DataType {
        val name = SimpleName(children[1] as String)
        val supertypes = children[2] as List<TypeRefInfo>? ?: emptyList()
        val constructors = children[4] as List<List<(DataType) -> ParameterDeclaration>>
        val property = children[5] as List<((DataType) -> PropertyDeclaration)>

        return { ns: TypeNamespace ->
            val dt = DataTypeSimple(ns, name)
            supertypes.forEach {
                dt.addSupertype_dep(it.name)
                //(it.type as DataType).addSubtype(dt.name)
            }
            constructors.forEach { cons ->
                val consParams = cons.map { it.invoke(dt) }
                dt.addConstructor(consParams)
            }
            property.forEach {
                val p = it.invoke(dt)
                setResolvers(p.typeInstance as TypeInstanceSimple, dt)
            }
            dt.also { setLocationFor(it, nodeInfo, sentence) }
        }
    }

    private fun setResolvers(ti: TypeInstanceSimple, dt: TypeDefinition) {
        //ti.namespace = dt.namespace
        ti.typeArguments.forEach { setResolvers(it as TypeInstanceSimple, dt) }
    }

    // supertypes = ':' [ typeReference / ',']+ ;
    private fun supertypes(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<TypeRefInfo> {
        return (children[1] as List<Any>).toSeparatedList<Any, TypeRefInfo, String>().items
    }

    // constructor = 'constructor' '(' constructorParameter* ')' ;
    private fun constructor(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<(DataType) -> ParameterDeclaration> =
        children[2] as List<(DataType) -> ParameterDeclaration>

    // constructorParameter = cmp_ref? val_var? IDENTIFIER ':' typeReference ;
    private fun constructorParameter(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (DataType) -> ParameterDeclaration {
        val cmpRef = children[0] as PropertyCharacteristic?
        val valVar = children[1] as PropertyCharacteristic?
        val name = net.akehurst.language.typemodel.api.ParameterName(children[2] as String)
        val typeRef = children[4] as TypeRefInfo

        val characteristics = (cmpRef?.let { setOf(it) } ?: emptySet()) + (valVar?.let { setOf(it) } ?: emptySet())
        return { owner: DataType ->
            val tp = typeRef.toTypeInstance(owner)
            if (characteristics.isNotEmpty()) {
                owner.appendPropertyStored(PropertyName(name.value), tp, characteristics)
            }
            ParameterDefinitionSimple(name, tp, null) //TODO: default value
                .also { setLocationFor(it, nodeInfo, sentence) }
        }
    }

    // property = cmp_ref? val_var NAME : typeReference ; //TODO: grammarIndex !
    private fun property(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (StructuredType) -> PropertyDeclaration {
        val cmpRef = children[0] as PropertyCharacteristic?
        val valVar = children[1] as PropertyCharacteristic
        val name = PropertyName(children[2] as String)
        val typeRef = children[4] as TypeRefInfo

        val characteristics = (cmpRef?.let { setOf(it) } ?: emptySet()) + setOf(valVar)
       return { owner: StructuredType ->
            val typeInstance = typeRef.toTypeInstance(owner)
            owner.appendPropertyStored(name, typeInstance, characteristics)
                .also { setLocationFor(it, nodeInfo, sentence) }
        }
    }

    // cmp_ref = 'cmp' | 'ref' ;
    private fun cmp_ref(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): PropertyCharacteristic =
        when (children[0] as String) {
            "cmp" -> PropertyCharacteristic.COMPOSITE
            "ref" -> PropertyCharacteristic.REFERENCE
            else -> error("Value not allowed '${children[0]}'")
        }

    // val_var = 'val' | 'var' ;
    private fun val_var(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): PropertyCharacteristic =
        when (children[0] as String) {
            "val" -> PropertyCharacteristic.READ_ONLY
            "var" -> PropertyCharacteristic.READ_WRITE
            else -> error("Value not allowed '${children[0]}'")
        }

    // typeReference = possiblyQualifiedName typeArgumentList? '?'?;
    private fun typeReference(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): TypeRefInfo {
        val pqn = children[0] as PossiblyQualifiedName
        val typeArgumentList = children[1] as List<TypeRefInfo>? ?: emptyList()
        val isNullable = (children[2] as String?) != null
        val tr = TypeRefInfo(pqn, typeArgumentList, isNullable)//.also { locationMap[it] = nodeInfo.node.locationIn(sentence) }
        return tr
    }

    //typeArgumentList = '<' [ typeReference / ',']+ '>' ;
    private fun typeArgumentList(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<TypeRefInfo> {
        val list = (children[1] as List<Any>).toSeparatedList<Any, TypeRefInfo, String>().items
        return list
    }

}