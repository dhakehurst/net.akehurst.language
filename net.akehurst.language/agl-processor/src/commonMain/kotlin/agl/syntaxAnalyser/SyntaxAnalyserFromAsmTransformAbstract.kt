/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.akehurst.language.agl.syntaxAnalyser

//import net.akehurst.language.asm.simple.*
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsEmbedded
import net.akehurst.language.agl.simple.Grammar2TransformRuleSet.Companion.toLeafAsStringTrRule
import net.akehurst.language.agl.simple.Grammar2TransformRuleSet.Companion.toSubtypeTrRule
import net.akehurst.language.agl.util.Debug
import net.akehurst.language.api.syntaxAnalyser.AsmFactory
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.asm.api.*
import net.akehurst.language.asm.simple.AsmPathSimple
import net.akehurst.language.asm.simple.AsmStructureSimple
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.collections.MutableStack
import net.akehurst.language.collections.mutableStackOf
import net.akehurst.language.expressions.asm.*
import net.akehurst.language.expressions.processor.*
import net.akehurst.language.grammar.api.GrammarRuleName
import net.akehurst.language.parser.api.Rule
import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sppt.api.*
import net.akehurst.language.sppt.treedata.locationForNode
import net.akehurst.language.sppt.treedata.matchedTextNoSkip
import net.akehurst.language.transform.api.TransformModel
import net.akehurst.language.transform.api.TransformationRule
import net.akehurst.language.transform.asm.*
import net.akehurst.language.transform.processor.AsmTransformInterpreter
import net.akehurst.language.typemodel.api.*
import net.akehurst.language.typemodel.asm.StdLibDefault
import net.akehurst.language.typemodel.asm.TypeArgumentNamedSimple
import net.akehurst.language.typemodel.asm.TypeModelSimple

data class NodeTrRules(
    val forNode: TransformationRule,
    val forChildren: TransformationRule
) {
    constructor(t: TransformationRule) : this(t, t)
}

data class DownData2(
    val path: ParsePath,
    val trRule: NodeTrRules
)

data class ChildData<AsmValueType : Any>(
    val nodeInfo: SpptDataNodeInfo,
    val value: TypedObject<AsmValueType>?
)

/**
 * TypeName <=> RuleName
 *
 * @param scopeDefinition TypeNameDefiningScope -> Map<TypeNameDefiningSomethingReferencable, referencableProperty>
 * @param references ReferencingTypeName, referencingPropertyName  -> ??
 */
abstract class SyntaxAnalyserFromAsmTransformAbstract<AsmType : Any, AsmValueType : Any>(
    _typeModel: TypeModel,
    val asmTransformModel: TransformModel,
    val relevantTrRuleSet: QualifiedName,
    val asmFactory: AsmFactory<AsmType, AsmValueType>
) : SyntaxAnalyserFromTreeDataAbstract<AsmType>() {

    companion object {
        private const val ns = "net.akehurst.language.agl.syntaxAnalyser"
        const val CONFIGURATION_KEY_AGL_SCOPE_MODEL = "$ns.scope.model"

        val PropertyDeclaration.isTheSingleProperty get() = this.owner.property.size == 1

        val Rule.hasOnyOneRhsItem get() = this.rhsItems.size == 1 && this.rhsItems[0].size == 1

    }

    val typeModel: TypeModel get() = asmFactory.typeModel

    private var _asm: AsmType? = null
    override val asm: AsmType get() = _asm as AsmType

    val relevantRuleSet = asmTransformModel.findNamespaceOrNull(relevantTrRuleSet.front)?.findDefinitionOrNull(relevantTrRuleSet.last)
        ?: error("Relevant TransformRuleSet not Found for '$relevantTrRuleSet'")
    val _trf = AsmTransformInterpreter(typeModel, asmFactory)

    init {
        asmFactory.typeModel = TypeModelSimple(SimpleName(_typeModel.name.value + "+ParseNodeNamespace")).also {
            it.addAllNamespaceAndResolveImports(_typeModel.namespace + AsmTransformInterpreter.parseNodeNamespace)
        }
    }

    private fun findTrRuleForGrammarRuleNamedOrNull(grmRuleName: String): TransformationRule? {
        return relevantRuleSet.findAllTrRuleForGrammarRuleNamedOrNull(GrammarRuleName(grmRuleName))
    }

    override fun <T : Any> clear(done: Set<SyntaxAnalyser<T>>) {
        super.clear(done)
        this._trf.clear()
        this._asm = null
    }

    override fun walkTree(sentence: Sentence, treeData: TreeData, skipDataAsTree: Boolean) {
        val syntaxAnalyserStack: MutableStack<SyntaxAnalyserFromAsmTransformAbstract<AsmType, AsmValueType>> = mutableStackOf(this)
        val downStack = mutableStackOf<DownData2>() //when null don't use branch
        val stack = mutableStackOf<ChildData<AsmValueType>>()
        val walker = object : SpptWalker {
            var isRoot = true
            override fun beginTree() {
                // use same asm for all embedded trees
                // otherwise need to combine and adjust indexes
                // faster to use same asm in first place
                syntaxAnalyserStack.peek().setAsm(_asm ?: asmFactory.constructAsm())
                isRoot = true
            }

            override fun endTree() {
                val root = stack.pop()
                syntaxAnalyserStack.peek().addAsmRoot(root.value!!.self)
                //do not pop the asm, leave it here, so it can be retrieved when wanted.
                // embedded ASMs are popped in endEmbedded
            }

            override fun leaf(nodeInfo: SpptDataNodeInfo) {
                // leaf transforms to either String or null if leaf is EmptyLeaf
                val value = syntaxAnalyserStack.peek().createValueFromLeaf(sentence, nodeInfo)
                stack.push(ChildData(nodeInfo, value))
            }

            override fun beginBranch(nodeInfo: SpptDataNodeInfo) {
                val parentDownData = downStack.peekOrNull()
//                val p = when {
//                    downStack.isEmpty -> AsmPathSimple.ROOT + (asmFactory.rootList(asm).size).toString()
//                    null == parentDownData -> AsmPathSimple.ROOT.plus("<error>")  // property unused
//                    isRoot -> parentDownData.path
//                    else -> syntaxAnalyserStack.peek().pathFor(parentDownData.path, parentDownData.trRule.forChildren.resolvedType.resolvedDeclaration, nodeInfo)
//                }
                val p = nodeInfo.path
                val tr = when {
                    isRoot -> {
                        isRoot = false
                        val trRule = syntaxAnalyserStack.peek().findTrRuleForGrammarRuleNamedOrNull(nodeInfo.node.rule.tag)
                            ?: error("Type not found for ${nodeInfo.node.rule.tag}")
                        trRule
                    }

                    else -> syntaxAnalyserStack.peek().trRuleForNode(parentDownData?.trRule?.forChildren, nodeInfo)
                }
                val ddcomp = resolveCompressed(p, tr, nodeInfo)
                downStack.push(ddcomp)
            }

            override fun endBranch(nodeInfo: SpptDataNodeInfo) {
                val opt = nodeInfo.alt.option
                val numChildren = nodeInfo.numChildrenAlternatives[opt]!!
                val children = stack.pop(numChildren)
                val adjChildren = children.reversed()
                val downData = downStack.pop()
                val value = when {
                    //NothingType -> branch not used for element property value, push null for correct num children on stack
                    typeModel.NothingType == downData.trRule.forNode.resolvedType.resolvedDeclaration -> asmFactory.nothing()
                    else -> syntaxAnalyserStack.peek().createValueFromBranch(sentence, downData, nodeInfo, adjChildren)
                }
                value?.let { setLocationFor(it.self, nodeInfo, sentence) }
                stack.push(ChildData(nodeInfo, value))
                // path = path.parent!!
            }

            override fun beginEmbedded(nodeInfo: SpptDataNodeInfo) {
                val embeddedRhs = (nodeInfo.node.rule as RuntimeRule).rhs as RuntimeRuleRhsEmbedded
                val embRuleName = embeddedRhs.embeddedStartRule.tag
                val embGrmName = embeddedRhs.embeddedRuntimeRuleSet.qualifiedName
                val embSyntaxAnalyser = embeddedSyntaxAnalyser[QualifiedName(embGrmName)] as SyntaxAnalyserFromAsmTransformAbstract<*, *>?
                    ?: error("Embedded SyntaxAnalyser not found for '$embGrmName' in SyntaxAnalyser using TrRuleSet '${relevantTrRuleSet}'")
                syntaxAnalyserStack.push(embSyntaxAnalyser as SyntaxAnalyserFromAsmTransformAbstract<AsmType, AsmValueType>)
                val parentPath = when {
                    isRoot -> AsmPathSimple.ROOT + (asmFactory.rootList(asm).size).toString()
                    else -> downStack.peek().path
                }
                val parentTr = when {
                    isRoot -> syntaxAnalyserStack.peek().findTrRuleForGrammarRuleNamedOrNull(nodeInfo.node.rule.tag) ?: error("Type not found for ${nodeInfo.node.rule.tag}")
                    else -> downStack.peek().trRule.forChildren
                }
                val p = nodeInfo.path //syntaxAnalyserStack.peek().pathFor(parentPath, parentTr.resolvedType.resolvedDeclaration, nodeInfo)
                val tu = syntaxAnalyserStack.peek().findTrRuleForGrammarRuleNamedOrNull(embRuleName)
                    ?: error("Type not found for $embRuleName")
                val dd = resolveCompressed(p, tu, nodeInfo)
                downStack.push(dd)
            }

            override fun endEmbedded(nodeInfo: SpptDataNodeInfo) {
                val embSyntaxAnalyser = syntaxAnalyserStack.pop()
                val embeddedAsm = embSyntaxAnalyser._asm!!
                downStack.pop()
                val value = asmFactory.rootList(embeddedAsm).last()
                removeAsmRoot(value)
                value?.let { setLocationFor(value, nodeInfo, sentence) }
                stack.push(ChildData(nodeInfo, asmFactory.toTypedObject(value)))
            }

            override fun skip(startPosition: Int, nextInputPosition: Int) {
                // do nothing
            }

            override fun treeError(msg: String, path: PathFunction) {
                issues.error(null, "Error 'msg' at '${path.invoke().joinToString(separator = "/")}'")
            }

        }
        treeData.traverseTreeDepthFirst(walker, false)
    }

    private fun setAsm(value: AsmType) {
        this._asm = value
    }

    private fun addAsmRoot(rootValue: AsmValueType) {
        asmFactory.addRoot(_asm!!, rootValue)
    }

    private fun removeAsmRoot(rootValue: AsmValueType) {
        asmFactory.removeRoot(_asm!!, rootValue)
    }

    // private fun createAsmStructure(path: AsmPath, qualifiedTypeName: QualifiedName): Any {
    //    return objectGraph.constructStructure(qualifiedTypeName,path)
    // }

    //private fun pathFor(parentPath: ParsePath, parentType: TypeDefinition, nodeInfo: SpptDataNodeInfo) =
//        parsePathFor(parentPath,parentType,nodeInfo)


//    private fun parsePathFor(parentPath: AsmPath, parentType: TypeDefinition, nodeInfo: SpptDataNodeInfo): AsmPath {
//        return parentPath.plus(nodeInfo.node.rule.tag)
//    }

    private fun asmPathFor(parentPath: AsmPath, parentType: TypeDefinition, nodeInfo: SpptDataNodeInfo): AsmPath {
        return when (parentType) {
            is PrimitiveType -> parentPath
            is UnionType -> parentPath
            is CollectionType -> parentPath.plus(nodeInfo.child.index.toString())
            is TupleType -> {
                val prop = parentType.getOwnedPropertyByIndexOrNull(nodeInfo.child.propertyIndex)
                prop?.let { parentPath.plus(prop.name.value) } ?: parentPath.plus("<error>")
            }

            is DataType -> {
                when {
                    parentType.subtypes.isNotEmpty() -> parentPath
                    else -> {
                        val prop = parentType.getOwnedPropertyByIndexOrNull(nodeInfo.child.propertyIndex)
                        val elem = prop?.let { prop.name.value } ?: "<error>"
                        parentPath.plus(elem)
                    }
                }
            }

            else -> when (parentType) {
                typeModel.NothingType -> parentPath.plus("<error>")
                typeModel.AnyType -> parentPath.plus("<any>") // FIXME:
                else -> error("Should not happen")
            }
        }
    }

    private fun trRuleForNode(parentTrRule: TransformationRule?, nodeInfo: SpptDataNodeInfo): TransformationRule {
        //val parentType = parentTrRule?.resolvedType
        val parentTypeDecl = parentTrRule?.resolvedType?.resolvedDeclaration
        val nodeRule = nodeInfo.node.rule
        return when {
            null == parentTypeDecl -> transformationRule(StdLibDefault.NothingType, RootExpressionDefault.NOTHING)
            /*
            nodeRule.isOptional -> when {
                nodeRule.isPseudo -> transformationRule(SimpleTypeModelStdLib.AnyType.nullable(), RootExpressionSimple.SELF)
                else -> {
                    // special case compressed rule
                    this.findTrRuleForGrammarRuleNamedOrNull(nodeInfo.node.rule.tag) ?: error("Should not happen")
                }
            }

            nodeRule.isListSimple -> when {
                nodeRule.isPseudo -> transformationRule(
                    SimpleTypeModelStdLib.List.type(listOf(SimpleTypeModelStdLib.AnyType.asTypeArgument)),
                    EXPRESSION_CHILDREN
                )

                else -> {
                    // special case compressed rule - no pseudo node for list
                    this.findTrRuleForGrammarRuleNamedOrNull(nodeInfo.node.rule.tag) ?: error("Should not happen")
                }
            }

            nodeRule.isListSeparated -> when {
                nodeRule.isPseudo -> transformationRule(
                    SimpleTypeModelStdLib.List.type(listOf(SimpleTypeModelStdLib.AnyType.asTypeArgument)),
                    EXPRESSION_CHILDREN
                )

                else -> {
                    // special case compressed rule - no pseudo node for list
                    this.findTrRuleForGrammarRuleNamedOrNull(nodeInfo.node.rule.tag) ?: error("Should not happen")
                }
            }
*/
            else -> when {
                nodeRule.isPseudo -> transformationRule(StdLibDefault.AnyType, RootExpressionDefault.SELF)
                /*
                nodeRule.isPseudo -> { //TODO: check if isPseudo maybe just need to return self..higher TR-rule handles pars-tree-nodes?
                    //must be group or choice
                    when (parentTypeDecl) {
                        is StructuredType -> {
                            val propType = parentTypeDecl.getOwnedPropertyByIndexOrNull(nodeInfo.child.propertyIndex)?.typeInstance
                            when (propType) {
                                null -> transformationRule(StdLibDefault.NothingType, RootExpressionSimple.NOTHING) // no property when non-term is a literal
                                else -> transformationRule(propType, RootExpressionSimple.SELF)
                            }
                        }

                        is TupleType -> {
                            val pt = (parentType as TupleTypeInstance).typeArguments.getOrNull(nodeInfo.child.propertyIndex)
                            when (pt) {
                                null -> transformationRule(StdLibDefault.NothingType, RootExpressionSimple.NOTHING) // no arg when non-term is a literal
                                else -> transformationRule(pt.type, RootExpressionSimple.SELF)
                            }
                        }

                        is UnionType -> {
                            val idx = when { //FIXME: why do we need this difference here ?
                                nodeRule.isList -> nodeInfo.parentAlt.option.asIndex
                                else -> nodeInfo.alt.option.asIndex
                            }
                            val subtype =parentTypeDecl.alternatives[idx]
                            transformationRule(subtype, RootExpressionSimple.SELF)
                        }

                        is SpecialTypeSimple -> {
                            transformationRule(StdLibDefault.AnyType, RootExpressionSimple.SELF)
                        }

                        is PrimitiveType -> {
                            transformationRule(StdLibDefault.AnyType, RootExpressionSimple.SELF)
                        }

                        else -> error("Unsupported type '${parentTypeDecl::class.simpleName}'")
                    }

                }
*/
                else -> this.findTrRuleForGrammarRuleNamedOrNull(nodeInfo.node.rule.tag)
                    ?: error("Transform Rule for '${nodeInfo.node.rule.tag}' not found")
            }
        }
    }

    /*
        private fun trRuleForNode(parentTrRule: TransformationRule?, nodeInfo: SpptDataNodeInfo): TransformationRule {
            return when {
            null == parentTrRule -> typeModel.NothingType.type().toNoActionTrRule() // property unused
            parentTrRule.resolvedType.isNullable -> trRuleForParentOptional(parentTrRule, nodeInfo)
            nodeInfo.node.rule.isEmbedded -> trRuleForEmbedded(parentTrRule, nodeInfo)
            else -> {
                val parentType = parentTrRule.resolvedType.declaration
                when (parentType) {
                    is PrimitiveType -> parentTrRule
                    is UnnamedSupertypeType -> trRuleForParentUnnamedSuperType(parentTypeUsage, nodeInfo)
                    is CollectionType -> when (parentType) {
                        SimpleTypeModelStdLib.List -> trRuleForParentListSimple(parentTypeUsage, nodeInfo)
                        SimpleTypeModelStdLib.ListSeparated -> trRuleForParentListSeparated(parentTypeUsage, nodeInfo)
                        else -> error("Should not happen")
                    }

                    is TupleType -> trRuleForParentTuple(parentType, nodeInfo)
                    is DataType -> trRuleForParentElement(parentType, nodeInfo)
                    else -> when (parentType) {
                        typeModel.NothingType -> typeModel.NothingType.type().toNoActionTrRule()
                        typeModel.AnyType -> TODO()
                        else -> error("Should not happen")
                    }
                }
            }
        }
    }
*/
    private fun typeForParentOptional(parentTypeUsage: TypeInstance, nodeInfo: SpptDataNodeInfo): TypeInstance {
        // nodes map to runtime-rules, not user-rules
        // if user-rule only had one optional item, then runtime-rule is 'compressed, i.e. no pseudo rule for the option
        if (Debug.CHECK) check(parentTypeUsage.isNullable)
        return parentTypeUsage.notNullable()
    }

    private fun typeForEmbedded(parentTypeUsage: TypeInstance, nodeInfo: SpptDataNodeInfo): TypeInstance {
        // need to skip over the embedded node and use type of its child
        if (Debug.CHECK) check(nodeInfo.node.rule.isEmbedded)
        val type = parentTypeUsage.resolvedDeclaration
        return when (type) {
            is DataType -> {
                val prop = type.getOwnedPropertyByIndexOrNull(nodeInfo.child.propertyIndex)
                prop?.typeInstance ?: typeModel.NothingType.type()
            }

            else -> parentTypeUsage
        }
    }

    private fun typeForParentListSimple(parentTypeUsage: TypeInstance, nodeInfo: SpptDataNodeInfo): TypeInstance {
        // nodes map to runtime-rules, not user-rules
        // if user-rule only had one list item, then runtime-rule is 'compressed, i.e. no pseudo rule for the list
        if (Debug.CHECK) check(parentTypeUsage.resolvedDeclaration == StdLibDefault.List)
        val itemTypeUse = parentTypeUsage.typeArguments[0]
        return itemTypeUse as TypeInstance
    }

    private fun typeForParentListSeparated(parentTypeUsage: TypeInstance, nodeInfo: SpptDataNodeInfo): TypeInstance {
        // nodes map to runtime-rules, not user-rules
        // if user-rule only had one slist item, then runtime-rule is 'compressed, i.e. no pseudo rule for the slist
        if (Debug.CHECK) check(parentTypeUsage.resolvedDeclaration == StdLibDefault.ListSeparated)
        val index = nodeInfo.child.index % 2
        val childTypeUse = parentTypeUsage.typeArguments[index]
        return childTypeUse as TypeInstance
    }

    private fun typeForParentUnnamedSuperType(parentTypeUsage: TypeInstance, nodeInfo: SpptDataNodeInfo): TypeInstance {
        if (Debug.CHECK) check(parentTypeUsage.isNullable)
        val tu = (parentTypeUsage.resolvedDeclaration as UnionType).alternatives[nodeInfo.alt.option.asIndex]
        return tu
    }

    private fun typeForParentTuple(parentType: TupleType, nodeInfo: SpptDataNodeInfo): TypeInstance {
        val prop = parentType.getOwnedPropertyByIndexOrNull(nodeInfo.child.propertyIndex)
        return typeForProperty(prop, nodeInfo)
    }

    private fun typeForParentElement(parentType: DataType, nodeInfo: SpptDataNodeInfo): TypeInstance {
        return when {
            parentType.subtypes.isNotEmpty() -> {
                val t = parentType.subtypes[nodeInfo.parentAlt.option.asIndex]
                return t
            }

            else -> {
                val prop = parentType.getOwnedPropertyByIndexOrNull(nodeInfo.child.propertyIndex)
                typeForProperty(prop, nodeInfo)
            }
        }
    }

    private fun resolveElementSubtype(typeUse: TypeInstance, nodeInfo: SpptDataNodeInfo): TypeInstance {
        val type = typeUse.resolvedDeclaration
        return when {
            type is DataType && type.subtypes.isNotEmpty() -> {
                val t = type.subtypes[nodeInfo.alt.option.asIndex]
                t
            }

            else -> typeUse
        }
    }

    private fun typeForProperty(prop: PropertyDeclaration?, nodeInfo: SpptDataNodeInfo): TypeInstance {
        return when {
            null == prop -> typeModel.NothingType.type() // property unused
            prop.typeInstance.isNullable -> prop.typeInstance//typeForOptional(propTypeUse, nodeInfo)
            else -> {
                val propType = prop.typeInstance.resolvedDeclaration
                when (propType) {
                    is PrimitiveType -> (prop.typeInstance)
                    is UnionType -> {
                        val tu = resolveUnnamedSuperTypeSubtype(prop.typeInstance, nodeInfo)
                        when (tu.resolvedDeclaration) {
                            is TupleType -> (tu)
                            else -> (prop.typeInstance)
                        }
                        //NodeTypes(tu)
                        //NodeTypes(prop.typeUse)//typeForUnnamedSuperType(propTypeUse, nodeInfo)
                    }

                    is CollectionType -> (prop.typeInstance)
                    is TupleType -> (prop.typeInstance)
                    is DataType -> (prop.typeInstance)
                    else -> when (propType) {
                        typeModel.NothingType -> typeModel.NothingType.type()
                        typeModel.AnyType -> TODO()
                        else -> error("Should not happen")
                    }
                }
            }
        }
    }

    private fun resolveCompressed(p: ParsePath, trRule: TransformationRule, nodeInfo: SpptDataNodeInfo): DownData2 {
        val typeDecl = trRule.resolvedType.resolvedDeclaration
        return when {
            typeDecl is StructuredType && nodeInfo.node.rule.isOptional && nodeInfo.node.rule.hasOnyOneRhsItem && nodeInfo.node.rule.rhsItems[0][0].isTerminal -> {
                DownData2(p, NodeTrRules(trRule, StdLibDefault.String.toLeafAsStringTrRule()))
            }

            typeDecl is DataType && typeDecl.property.size == 1 -> {
                // special cases where PT is compressed for lists (and optionals)
                when {
                    typeDecl.property.first().typeInstance.isNullable -> {
                        val fProp = typeDecl.property.first()
                        val pp = p.plus(fProp.name.value)
                        //DownData2(pp, NodeTrRules(trRule, fProp.typeInstance.toSelfAssignChild0TrRule()))
                        DownData2(p, NodeTrRules(trRule))
                    }

                    nodeInfo.node.rule.isOptional -> DownData2(p, NodeTrRules(trRule))
                    nodeInfo.node.rule.isList -> {
                        val fProp = typeDecl.property.first()
                        val pp = p.plus(fProp.name.value)
                        //DownData2(pp, NodeTrRules(trRule, fProp.typeInstance.toSelfAssignChild0TrRule()))
                        DownData2(p, NodeTrRules(trRule))//, fProp.typeInstance))
                    }

                    else -> DownData2(p, NodeTrRules(trRule))
                }
            }

            typeDecl is UnionType -> {
                when {
                    // special cases where PT is compressed for choice of concats
                    nodeInfo.node.rule.isChoice -> when {
                        typeDecl.alternatives[nodeInfo.alt.option.asIndex].resolvedDeclaration is TupleType -> DownData2(
                            p,
                            NodeTrRules(trRule, typeDecl.alternatives[nodeInfo.alt.option.asIndex].toSubtypeTrRule())
                        )

                        else -> DownData2(p, NodeTrRules(trRule))
                    }

                    else -> DownData2(p, NodeTrRules(trRule))
                }
            }

            else -> DownData2(p, NodeTrRules(trRule))
        }
    }

    private fun resolveUnnamedSuperTypeSubtype(typeUse: TypeInstance, nodeInfo: SpptDataNodeInfo): TypeInstance {
        val type = typeUse.resolvedDeclaration
        return when {
            type is UnionType -> when {
                nodeInfo.node.rule.isChoice && type.alternatives.isNotEmpty() -> {
                    val t = type.alternatives[nodeInfo.alt.option.asIndex]
                    t
                }

                else -> typeUse
            }

            else -> typeUse
        }
    }

    private fun createValueFromLeaf(sentence: Sentence, target: SpptDataNodeInfo): TypedObject<AsmValueType>? = when {
        target.node.rule.isEmptyTerminal -> asmFactory.nothing() // for empty or optional rules
        target.node.rule.isEmptyListTerminal -> null  // return null for empty lists

        else -> {
            val v = sentence.matchedTextNoSkip(target.node)
            asmFactory.createPrimitiveValue(StdLibDefault.String.qualifiedTypeName, v)
        }
    }

    private fun createValueFromBranch(sentence: Sentence, downData: DownData2, target: SpptDataNodeInfo, children: List<ChildData<AsmValueType>>): TypedObject<AsmValueType>? =
        createValueFromBranch2(sentence, downData, target, children)

    private fun createValueFromBranch2(sentence: Sentence, downData: DownData2, target: SpptDataNodeInfo, children: List<ChildData<AsmValueType>>): TypedObject<AsmValueType> {
        // optional children should have value Nothing, if it is an empty list then it will contain null
        val asmChildren = children.mapNotNull { it.value }
        val childrenAsmList = when {
            target.node.rule.isListSeparated -> asmFactory.createCollection(StdLibDefault.ListSeparated.qualifiedName, asmChildren)
            else -> asmFactory.createCollection(StdLibDefault.List.qualifiedName, asmChildren)
        }
        val asmMatchedText = asmFactory.createPrimitiveValue(StdLibDefault.String.qualifiedTypeName, sentence.matchedTextNoSkip(target.node))

        val parsePath = asmFactory.any(downData.path)
        val alternative = asmFactory.createPrimitiveValue(StdLibDefault.Integer.qualifiedTypeName, target.alt.option.value.toLong())
        val selfType = when {
            // target.node.rule.isTerminal -> AsmTransformInterpreter.PARSE_NODE_TYPE_LEAF
            target.node.rule.isListSeparated -> AsmTransformInterpreter.PARSE_NODE_TYPE_BRANCH_SEPARATED
            else -> AsmTransformInterpreter.PARSE_NODE_TYPE_BRANCH_SIMPLE
        }
//        val self = asmFactory.createStructureValue(selfType.qualifiedTypeName, mapOf(AsmTransformInterpreter.PATH.value to asmPath))
        val self = asmFactory.createTupleValue(
            listOf(
                TypeArgumentNamedSimple(AsmTransformInterpreter.PATH, parsePath.type),
                TypeArgumentNamedSimple(AsmTransformInterpreter.ALTERNATIVE, alternative.type),
                TypeArgumentNamedSimple(AsmTransformInterpreter.CHILDREN, childrenAsmList.type), //StdLibDefault.List.type(listOf(StdLibDefault.AnyType.asTypeArgument))),
                TypeArgumentNamedSimple(AsmTransformInterpreter.CHILD, childrenAsmList.type),
                TypeArgumentNamedSimple(AsmTransformInterpreter.MATCHED_TEXT, asmMatchedText.type),
            )
        )
        asmFactory.setProperty(self, AsmTransformInterpreter.PATH.value, parsePath)
        asmFactory.setProperty(self, AsmTransformInterpreter.ALTERNATIVE.value, alternative)
        asmFactory.setProperty(self, AsmTransformInterpreter.CHILDREN.value, childrenAsmList)
        asmFactory.setProperty(self, AsmTransformInterpreter.CHILD.value, childrenAsmList)
        asmFactory.setProperty(self, AsmTransformInterpreter.MATCHED_TEXT.value, asmMatchedText)

        //TODO: use factory, requires TransformInterpreter to be generic on SelfType
        val typedSelf = self //TypedObjectAsmValue(selfType, self as AsmValue) //asmFactory.toTypedObject(self, selfType)
        val evc = EvaluationContext.of(mapOf(AsmTransformInterpreter.SELF to typedSelf))
        val tr = downData.trRule.forNode
        _trf.clear()
        val asm = _trf.evaluate(evc, tr)
        if(asm.self is AsmStructureSimple) { //FIXME: don't like this here...hacky!
            (asm.self as AsmStructureSimple).parsePath = downData.path.toString()
        }
        _trf.issues.forEach {
            super.issues.error(null, "Error evaluating transformation rule '${it.message}':\n${tr.asString()}")
        }
        return asm
    }

    /*
        private fun createValueFromBranch1(sentence: Sentence, downData: DownData2, target: SpptDataNodeInfo, children: List<ChildData>): Any? {
            val targetType = findTrRuleForGrammarRuleNamedOrNull(target.node.rule.tag)

            return when {
                downData.trRule.forNode.resolvedType.isNullable && target.node.rule.isOptional -> {
                    val child = children[0]
                    when {
                        null == child.value -> null
                        else -> {
                            val nonOptChildren = listOf(ChildData(child.nodeInfo, child.value))
                            child.value as AsmValue
                        }
                    }
                }

                target.node.rule.isEmbedded -> children[0].value as AsmValue
                else -> {
                    val type = downData.trRule.forNode.resolvedType.resolvedDeclaration
                    when (type) {
                        is PrimitiveType -> {
                            createStringValueFromBranch(sentence, target)
                        }

                        is UnionType -> {
                            val actualType = type.alternatives[target.alt.option.asIndex].resolvedDeclaration
                            when (actualType) {
                                is TupleType -> createTupleFrom(sentence, actualType, downData.path, children)
                                else -> children[0].value as AsmValue
                            }
                        }

                        is CollectionType -> when (type) {

                            StdLibDefault.List -> {
                                when {
                                    null != targetType && targetType.resolvedType.resolvedDeclaration != StdLibDefault.List && targetType.resolvedType.resolvedDeclaration is DataType -> {
                                        val propValue = when {
                                            target.node.rule.isListSeparated -> {
                                                /*
                                                val alist = createListSimpleValueFromBranch(
                                                    target,
                                                    downData.path,
                                                    children.map { it.value as AsmValue? },
                                                    type
                                                )
                                                val els = alist.elements.toSeparatedList<AsmValue, AsmValue, AsmValue>().items
                                                 */
                                                val sepList = createListSeparatedItemsValueFromBranch(target, downData.path, children, type)
                                                sepList
                                            }

                                            else -> createListSimpleValueFromBranch(target, downData.path, children.map { it.value as AsmValue? }, type)
                                        }
                                        val propDecl = (targetType.resolvedType.resolvedDeclaration as DataType).property.first()
                                        val el = createAsmStructure(downData.path, targetType.resolvedType.resolvedDeclaration.qualifiedName)
                                        setPropertyFromDeclaration(el, propDecl, propValue)
                                        el
                                    }

                                    else -> createListSimpleValueFromBranch(target, downData.path, children.map { it.value as AsmValue? }, type)
                                }
                            }

                            StdLibDefault.ListSeparated -> {
                                when {
                                    null != targetType && targetType.resolvedType.resolvedDeclaration != StdLibDefault.ListSeparated && targetType.resolvedType.resolvedDeclaration is DataType -> {
                                        val propValue = createListSeparatedValueFromBranch(target, downData.path, children.map { it.value }, type)
                                        val propDecl = (targetType.resolvedType.resolvedDeclaration as DataType).property.first()
                                        val el = createAsmStructure(downData.path, targetType.resolvedType.resolvedDeclaration.qualifiedName)
                                        setPropertyFromDeclaration(el, propDecl, propValue)
                                        el
                                    }

                                    else -> createListSeparatedValueFromBranch(target, downData.path, children.map { it.value }, type)
                                }
                            }

                            else -> error("Should not happen")
                        }

                        is TupleType -> {
                            createTupleFrom(sentence, type, downData.path, children)
                        }

                        is DataType -> {
                            if (type.subtypes.isNotEmpty()) {
                                if (Debug.CHECK) check(1 == children.size)
                                children[0].value as AsmValue
                            } else {
                                val el = createAsmStructure(downData.path, type.qualifiedName)
                                for (propDecl in type.property) {
                                    val propType = propDecl.typeInstance.resolvedDeclaration
                                    val propValue: Any? = when (propType) {
                                        is PrimitiveType -> {
                                            val childData = children[propDecl.index]
                                            createStringValueFromBranch(sentence, childData.nodeInfo)
                                        }

                                        is CollectionType -> when (propType) {
                                            StdLibDefault.List -> {
                                                when {
                                                    target.node.rule.isListSimple && target.node.option == RulePosition.OPTION_MULTI_EMPTY -> asmFactory.listOfValues(emptyList())
                                                    target.node.rule.isList -> createList(target, children.map { it.value as AsmValue })
                                                    else -> {
                                                        val childData = children[propDecl.index]
                                                        when {
                                                            childData.nodeInfo.node.rule.isList -> when {
                                                                null == childData.value -> asmFactory.listOfValues(emptyList())
                                                                childData.value is AsmList -> createList(childData.nodeInfo, childData.value.elements)
                                                                childData.value is AsmStructure -> childData.value.property.values.first().value as AsmList
                                                                else -> asmFactory.listOfValues(listOf(childData.value as AsmValue))
                                                            }

                                                            else -> error("Internal Error: cannot create a ListSimple from '$childData'")
                                                        }
                                                    }
                                                }
                                            }

                                            StdLibDefault.ListSeparated -> {
                                                when {
                                                    //childData.nodeInfo.node.rule.isEmptyTerminal -> AsmListSeparatedSimple(emptyList())
                                                    target.node.rule.isListSeparated && target.node.option == RulePosition.OPTION_MULTI_EMPTY -> asmFactory.listOfSeparatedValues(
                                                        emptyListSeparated()
                                                    )

                                                    target.node.rule.isList -> asmFactory.listOfSeparatedValues(
                                                        children.map { it.value!! }.toSeparatedList()
                                                    )

                                                    else -> {
                                                        val childData = children[propDecl.index]
                                                        when {
                                                            childData.nodeInfo.node.rule.isList -> when {
                                                                null == childData.value -> asmFactory.listOfSeparatedValues(emptyListSeparated())
                                                                childData.value is AsmListSeparated -> childData.value
                                                                childData.value is AsmList -> asmFactory.listOfSeparatedValues(childData.value.elements.toSeparatedList())
                                                                childData.value is AsmStructure -> childData.value.property.values.first().value as AsmListSeparated
                                                                else -> asmFactory.listOfSeparatedValues(listOf(childData.value as AsmValue).toSeparatedList())
                                                            }

                                                            else -> error("Internal Error: cannot create a ListSeparated from '$childData'")
                                                        }
                                                    }
                                                }
                                            }

                                            else -> error("Should not happen")
                                        }

                                        else -> {
                                            val childData = children[propDecl.index]
                                            childData.value as AsmValue?
                                        }
                                    }
                                    setPropertyFromDeclaration(el, propDecl, propValue)
                                }
                                el
                            }
                        }

                        else -> when (type) {
                            typeModel.NothingType -> error("Internal Error: items should not have type 'NothingType'")
                            typeModel.AnyType -> {
                                TODO()
                                /*
                        val actualType = this.findTypeForRule(target.name) ?: error("Internal Error: cannot find actual type for ${target.name}")
                        when (actualType.type) {
                            is AnyType -> {// when {
                                //must be a choice in a group
                                val choice = _mapToGrammar(target.runtimeRuleSetNumber, target.runtimeRuleNumber) as Choice
                                when (choice.alternative.size) {
                                    1 -> {
                                        val ch = target.children[0]
                                        val childType = this.findTypeForRule(ch.name) ?: error("Internal Error: cannot find type for ${ch.name}")
                                        val chPath = path
                                        //val childsScope = scope
                                        createValue(ch, chPath, childType)//, childsScope)
                                    }

                                    else -> {
                                        TODO()
                                    }
                                }
                                //else -> error("Internal Error: cannot find actual type for ${target.name}")
                            }

                            else -> createValue(target, path, actualType)//, scope)
                        }
                         */
                            }

                            else -> error("Should not happen")
                        }
                    }
                }
            }
        }

        private fun createValueFor(sentence: Sentence, type: TypeDefinition, path: AsmPath, childData: ChildData): Any = when (type) {
            is PrimitiveType -> createStringValueFromBranch(sentence, childData.nodeInfo)
            is UnionType -> TODO()
            is CollectionType -> TODO()
            is TupleType -> createTupleFrom(sentence, type, path, childData.value as List<ChildData>)
            is DataType -> createElementFrom(sentence, type, path, childData.value as List<ChildData>)
            else -> when (type) {
                typeModel.NothingType -> TODO()
                typeModel.AnyType -> TODO()
                else -> error("Shold not happen")
            }
        }

        private fun createStringValueFromBranch(sentence: Sentence, target: SpptDataNodeInfo): Any = when {
            target.node.startPosition == target.node.nextInputNoSkip -> asmFactory.nothingValue()
            else -> {
                val str = sentence.matchedTextNoSkip(target.node)
                asmFactory.primitiveValue(StdLibDefault.String.qualifiedTypeName, str)
            }
        }

        private fun createList(nodeData: SpptDataNodeInfo, list: List<AsmValue>): Any {
            return when {
                nodeData.node.rule.isListSimple -> asmFactory.listOfValues(list)
                nodeData.node.rule.isListSeparated -> {
                    val rhs = (nodeData.node.rule as RuntimeRule).rhs as RuntimeRuleRhsListSeparated
                    when {
                        rhs.separatorRhsItem.isTerminal -> asmFactory.listOfValues(list.toSeparatedList<AsmValue, AsmValue, AsmValue>().items)
                        else -> asmFactory.listOfValues(list.toSeparatedList<AsmValue, AsmValue, AsmValue>().separators)
                    }
                }

                else -> error("Internal error: List kind not handled")
            }
        }

        private fun createListSimpleValueFromBranch(target: SpptDataNodeInfo, path: AsmPath, children: List<AsmValue?>, type: TypeDefinition): Any {
            if (Debug.CHECK) check(type == StdLibDefault.List)
            return when {
                target.node.rule.isEmptyTerminal -> asmFactory.listOfValues(emptyList())
                target.node.rule.isEmptyListTerminal -> asmFactory.listOfValues(emptyList())
                target.node.rule.isListSimple -> asmFactory.listOfValues(children.filterNotNull())
                else -> error("Internal Error: cannot create a List from '$target'")
            }
        }

        private fun createListSeparatedValueFromBranch(target: SpptDataNodeInfo, path: AsmPath, children: List<Any?>, type: TypeDefinition): Any {
            if (Debug.CHECK) check(type == StdLibDefault.ListSeparated)
            return when {
                target.node.rule.isEmptyTerminal -> asmFactory.listOfSeparatedValues(emptyListSeparated())
                target.node.rule.isEmptyListTerminal -> asmFactory.listOfSeparatedValues(emptyListSeparated())
                target.node.rule.isListSeparated -> {
                    val sList = (children as List<Any>).toSeparatedList<Any, Any, Any>()
                    asmFactory.listOfSeparatedValues(sList)
                }

                else -> error("Internal Error: cannot create a List(Separated) from '$target'")
            }
        }

        private fun createListSeparatedItemsValueFromBranch(target: SpptDataNodeInfo, path: AsmPath, children: List<Any?>, type: TypeDefinition): Any {
            if (Debug.CHECK) check(type == StdLibDefault.ListSeparated)
            return when {
                target.node.rule.isEmptyTerminal -> asmFactory.listOfValues(emptyList())
                target.node.rule.isEmptyListTerminal -> asmFactory.listOfValues(emptyList())
                target.node.rule.isListSeparated -> {
                    val sList = (children as List<Any>).toSeparatedList<Any, Any, Any>()
                    asmFactory.listOfValues(sList.items)
                }

                else -> error("Internal Error: cannot create a List(Separated) from '$target'")
            }
        }

        private fun createTupleFrom(sentence: Sentence, type: TupleType, path: AsmPath, children: List<ChildData>): Any {
            val el = createAsmStructure(path, type.qualifiedName) // TODO: should have a createTuple method

            for (propDecl in type.property) {
                val propChildData = children[propDecl.index]
                val propValue = propChildData.value //createValueFor(sentence, propType.type, path, propChildData)
                setPropertyFromDeclaration(el, propDecl, propValue as AsmValue?)
            }
            return el
        }

        private fun createElementFrom(sentence: Sentence, type: DataType, path: AsmPath, children: List<ChildData>): Any {
            return if (type.subtypes.isNotEmpty()) {
                if (Debug.CHECK) check(1 == children.size)
                children[0].value as AsmStructure
            } else {
                val el = createAsmStructure(path, type.qualifiedName)
                for (propDecl in type.property) {
                    val propPath = path + propDecl.name.value
                    val propType = propDecl.typeInstance.resolvedDeclaration
                    val childData = children[propDecl.index]
                    val propValue = when (propType) {
                        is PrimitiveType -> {
                            createStringValueFromBranch(sentence, childData.nodeInfo)
                        }

                        is CollectionType -> when (propType) {
                            StdLibDefault.List -> {
                                when {
                                    childData.nodeInfo.node.rule.isEmptyTerminal -> asmFactory.listOfValues(emptyList())
                                    childData.nodeInfo.node.rule.isEmptyListTerminal -> asmFactory.listOfValues(emptyList())
                                    childData.nodeInfo.node.rule.isList -> when {
                                        childData.value is AsmList -> childData.value
                                        childData.value is AsmStructure -> childData.value.property.values.first().value as AsmList

                                        else -> TODO()
                                    }

                                    else -> error("Internal Error: cannot create a List from '$childData'")
                                }
                            }

                            StdLibDefault.ListSeparated -> {
                                TODO()
                            }

                            else -> error("Should not happen")
                        }

                        is TupleType -> createTupleFrom(sentence, propType, path, childData.value as List<ChildData>)

                        is UnionType -> {
                            val opt = childData.nodeInfo.parentAlt.option
                            if (RulePosition.OPTION_NONE == opt) error("Should not happen")
                            val actualType = propType.alternatives[opt.asIndex].resolvedDeclaration
                            when (actualType) {
                                is TupleType -> createTupleFrom(sentence, actualType as TupleType, path, childData.value as List<ChildData>)
                                else -> {
                                    TODO()
                                }
                            }

                        }

                        else -> children[propDecl.index].value as AsmValue
                    }
                    setPropertyFromDeclaration(el, propDecl, propValue)
                }
                el
            }
        }
    */
    private fun setPropertyFromDeclaration(el: AsmValueType, declaration: PropertyDeclaration, value: AsmValueType?) {
        // whether it is a reference or not is handled later in Semantic Analysis
        val v = asmFactory.toTypedObject(value)
        asmFactory.setProperty(asmFactory.toTypedObject(el), declaration.name.value, v)
    }

}
