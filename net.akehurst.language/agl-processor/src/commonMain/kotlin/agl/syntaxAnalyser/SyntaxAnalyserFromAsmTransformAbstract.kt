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

import net.akehurst.language.asm.simple.*
import net.akehurst.language.agl.simple.Grammar2TransformRuleSet.Companion.toLeafAsStringTrRule
import net.akehurst.language.agl.simple.Grammar2TransformRuleSet.Companion.toSubtypeTrRule
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsEmbedded
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsListSeparated
import net.akehurst.language.agl.simple.Grammar2TransformRuleSet.Companion.EXPRESSION_CHILDREN
import net.akehurst.language.agl.util.Debug
import net.akehurst.language.asm.api.*
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.collections.MutableStack
import net.akehurst.language.collections.emptyListSeparated
import net.akehurst.language.collections.mutableStackOf
import net.akehurst.language.collections.toSeparatedList
import net.akehurst.language.expressions.asm.*
import net.akehurst.language.expressions.processor.*
import net.akehurst.language.grammar.api.GrammarRuleName
import net.akehurst.language.parser.api.Rule
import net.akehurst.language.parser.api.RulePosition
import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sppt.api.*
import net.akehurst.language.sppt.treedata.locationForNode
import net.akehurst.language.sppt.treedata.matchedTextNoSkip
import net.akehurst.language.transform.api.TransformModel
import net.akehurst.language.transform.api.TransformationRule
import net.akehurst.language.transform.asm.*
import net.akehurst.language.transform.processor.AsmTransformInterpreter
import net.akehurst.language.typemodel.api.*
import net.akehurst.language.typemodel.asm.SimpleTypeModelStdLib
import net.akehurst.language.typemodel.asm.SpecialTypeSimple
import net.akehurst.language.typemodel.asm.TypeModelSimple

data class NodeTrRules(
    val forNode: TransformationRule,
    val forChildren: TransformationRule
) {
    constructor(t: TransformationRule) : this(t, t)
}

data class DownData2(
    val path: AsmPath,
    val trRule: NodeTrRules
)

/**
 * TypeName <=> RuleName
 *
 * @param scopeDefinition TypeNameDefiningScope -> Map<TypeNameDefiningSomethingReferencable, referencableProperty>
 * @param references ReferencingTypeName, referencingPropertyName  -> ??
 */
abstract class SyntaxAnalyserFromAsmTransformAbstract<A : Asm>(
//    val grammarNamespaceQualifiedName: QualifiedName,
    _typeModel: TypeModel,
    val asmTransformModel: TransformModel,
    val relevantTrRuleSet: QualifiedName
    //val scopeModel: CrossReferenceModel
) : SyntaxAnalyserFromTreeDataAbstract<A>() {

    companion object {
        private const val ns = "net.akehurst.language.agl.syntaxAnalyser"
        const val CONFIGURATION_KEY_AGL_SCOPE_MODEL = "$ns.scope.model"

        val PropertyDeclaration.isTheSingleProperty get() = this.owner.property.size == 1

        val Rule.hasOnyOneRhsItem get() = this.rhsItems.size == 1 && this.rhsItems[0].size == 1

    }

    val typeModel: TypeModel = TypeModelSimple(SimpleName(_typeModel.name.value + "+ParseNodeNamespace")).also {
        it.addAllNamespaceAndResolveImports(_typeModel.namespace + AsmTransformInterpreter.parseNodeNamespace)
    }

    private var _asm: AsmSimple? = null
    override val asm: A get() = _asm as A

    val relevantRuleSet = asmTransformModel.findNamespaceOrNull(relevantTrRuleSet.front)?.findDefinitionOrNull(relevantTrRuleSet.last)
        ?: error("Relevant TransformRuleSet not Found for '$relevantTrRuleSet'")
    val _trf = AsmTransformInterpreter(typeModel)

    private fun findTrRuleForGrammarRuleNamedOrNull(grmRuleName: String): TransformationRule? {
        return relevantRuleSet.findTrRuleForGrammarRuleNamedOrNull(GrammarRuleName(grmRuleName))
    }

    override fun clear() {
        super.clear()
        this._asm = null
    }

    override fun walkTree(sentence: Sentence, treeData: TreeData, skipDataAsTree: Boolean) {
        val syntaxAnalyserStack: MutableStack<SyntaxAnalyserFromAsmTransformAbstract<A>> = mutableStackOf(this)
        val downStack = mutableStackOf<DownData2>() //when null don't use branch
        val stack = mutableStackOf<ChildData>()
        val walker = object : SpptWalker {
            var isRoot = true
            override fun beginTree() {
                // use same asm for all embedded trees
                // otherwise need to combine and adjust indexes
                // faster to use same asm in first place
                syntaxAnalyserStack.peek().setAsm(_asm ?: AsmSimple())
                isRoot = true
            }

            override fun endTree() {
                val root = stack.pop()
                syntaxAnalyserStack.peek().addAsmRoot(root.value!! as AsmValue)
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
                val p = when {
                    downStack.isEmpty -> AsmPathSimple.ROOT + (asm.root.size).toString()
                    null == parentDownData -> AsmPathSimple.ROOT.plus("<error>")  // property unused
                    isRoot -> parentDownData.path
                    else -> syntaxAnalyserStack.peek().pathFor(parentDownData.path, parentDownData.trRule.forChildren.resolvedType.declaration, nodeInfo)
                }
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
                val value: AsmValue? = when {
                    //NothingType -> branch not used for element property value, push null for correct num children on stack
                    typeModel.NothingType == downData.trRule.forNode.resolvedType.declaration -> AsmNothingSimple
                    else -> syntaxAnalyserStack.peek().createValueFromBranch(sentence, downData, nodeInfo, adjChildren)
                }
                value?.let { locationMap[it] = sentence.locationForNode(nodeInfo.node) }
                stack.push(ChildData(nodeInfo, value))
                // path = path.parent!!
            }

            override fun beginEmbedded(nodeInfo: SpptDataNodeInfo) {
                val embeddedRhs = (nodeInfo.node.rule as RuntimeRule).rhs as RuntimeRuleRhsEmbedded
                val embRuleName = embeddedRhs.embeddedStartRule.tag
                val embGrmName = embeddedRhs.embeddedRuntimeRuleSet.qualifiedName
                val embSyntaxAnalyser = embeddedSyntaxAnalyser[QualifiedName(embGrmName)] as SyntaxAnalyserFromAsmTransformAbstract?
                    ?: error("Embedded SyntaxAnalyser not found for '$embGrmName' in SyntaxAnalyser using TrRuleSet '${relevantTrRuleSet}'")
                syntaxAnalyserStack.push(embSyntaxAnalyser as SyntaxAnalyserFromAsmTransformAbstract<A>)
                val parentDownData = downStack.peek()!!
                val p = syntaxAnalyserStack.peek().pathFor(parentDownData.path, parentDownData.trRule.forChildren.resolvedType.declaration, nodeInfo)
                val tu = syntaxAnalyserStack.peek().findTrRuleForGrammarRuleNamedOrNull(embRuleName)
                    ?: error("Type not found for $embRuleName")
                val dd = resolveCompressed(p, tu, nodeInfo)
                downStack.push(dd)
            }

            override fun endEmbedded(nodeInfo: SpptDataNodeInfo) {
                val embSyntaxAnalyser = syntaxAnalyserStack.pop()
                val embeddedAsm = embSyntaxAnalyser._asm!!
                downStack.pop()
                val value = embeddedAsm.root.last()
                removeAsmRoot(value)
                value?.let { locationMap[it] = sentence.locationForNode(nodeInfo.node) }
                stack.push(ChildData(nodeInfo, value))
            }

            override fun skip(startPosition: Int, nextInputPosition: Int) {
                // do nothing
            }

            override fun error(msg: String, path: PathFunction) {
                issues.error(null, "Error 'msg' at '${path.invoke().joinToString(separator = "/")}'")
            }

        }
        treeData.traverseTreeDepthFirst(walker, false)
    }

    private fun setAsm(value: AsmSimple) {
        this._asm = value
    }

    private fun addAsmRoot(value: AsmValue) {
        this._asm!!.addRoot(value)
    }

    private fun removeAsmRoot(value: Any) {
        this._asm!!.removeRoot(value)
    }

    private fun createAsmElement(path: AsmPath, name: QualifiedName): AsmStructure =
        this._asm!!.createStructure(path, name)

    private fun pathFor(parentPath: AsmPath, parentType: TypeDeclaration, nodeInfo: SpptDataNodeInfo): AsmPath {
        return when (parentType) {
            is PrimitiveType -> parentPath
            is UnnamedSupertypeType -> parentPath
            is CollectionType -> parentPath.plus(nodeInfo.child.index.toString())
            is TupleType -> {
                val prop = parentType.getPropertyByIndexOrNull(nodeInfo.child.propertyIndex)
                prop?.let { parentPath.plus(prop.name.value) } ?: parentPath.plus("<error>")
            }

            is DataType -> {
                when {
                    parentType.subtypes.isNotEmpty() -> parentPath
                    else -> {
                        val prop = parentType.getPropertyByIndexOrNull(nodeInfo.child.propertyIndex)
                        prop?.let { parentPath.plus(prop.name.value) } ?: parentPath.plus("<error>")
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
        val parentType = parentTrRule?.resolvedType
        val parentTypeDecl = parentTrRule?.resolvedType?.declaration
        val nodeRule = nodeInfo.node.rule
        return when {
            null == parentTypeDecl -> transformationRule(SimpleTypeModelStdLib.NothingType, RootExpressionSimple.NOTHING)
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

            else -> when {
                nodeRule.isPseudo -> { //TODO: check if isPseudo maybe just need to return self..higher TR-rule handles pars-tree-nodes?
                    //must be group or choice
                    when (parentTypeDecl) {
                        is StructuredType -> {
                            val propType = parentTypeDecl.getPropertyByIndexOrNull(nodeInfo.child.propertyIndex)?.typeInstance
                            when (propType) {
                                null -> transformationRule(SimpleTypeModelStdLib.NothingType, RootExpressionSimple.NOTHING) // no property when non-term is a literal
                                else -> transformationRule(propType, RootExpressionSimple.SELF)
                            }
                        }

                        is TupleType -> {
                            val pt = (parentType as TupleTypeInstance).typeArguments.getOrNull(nodeInfo.child.propertyIndex)
                            when (pt) {
                                null -> transformationRule(SimpleTypeModelStdLib.NothingType, RootExpressionSimple.NOTHING) // no arg when non-term is a literal
                                else -> transformationRule(pt.type, RootExpressionSimple.SELF)
                            }
                        }

                        is UnnamedSupertypeType -> {
                            val subtype = parentTypeDecl.subtypes[nodeInfo.alt.option]
                            transformationRule(subtype, RootExpressionSimple.SELF)
                        }

                        is SpecialTypeSimple -> {
                            transformationRule(SimpleTypeModelStdLib.AnyType, RootExpressionSimple.SELF)
                        }

                        is PrimitiveType -> {
                            transformationRule(SimpleTypeModelStdLib.AnyType, RootExpressionSimple.SELF)
                        }

                        else -> error("Unsupported type '${parentTypeDecl::class.simpleName}'")
                    }

                }

                else -> this.findTrRuleForGrammarRuleNamedOrNull(nodeInfo.node.rule.tag)
                    ?: error("Should not happen")
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
        val type = parentTypeUsage.declaration
        return when (type) {
            is DataType -> {
                val prop = type.getPropertyByIndexOrNull(nodeInfo.child.propertyIndex)
                prop?.typeInstance ?: typeModel.NothingType.type()
            }

            else -> parentTypeUsage
        }
    }

    private fun typeForParentListSimple(parentTypeUsage: TypeInstance, nodeInfo: SpptDataNodeInfo): TypeInstance {
        // nodes map to runtime-rules, not user-rules
        // if user-rule only had one list item, then runtime-rule is 'compressed, i.e. no pseudo rule for the list
        if (Debug.CHECK) check(parentTypeUsage.declaration == SimpleTypeModelStdLib.List)
        val itemTypeUse = parentTypeUsage.typeArguments[0]
        return itemTypeUse as TypeInstance
    }

    private fun typeForParentListSeparated(parentTypeUsage: TypeInstance, nodeInfo: SpptDataNodeInfo): TypeInstance {
        // nodes map to runtime-rules, not user-rules
        // if user-rule only had one slist item, then runtime-rule is 'compressed, i.e. no pseudo rule for the slist
        if (Debug.CHECK) check(parentTypeUsage.declaration == SimpleTypeModelStdLib.ListSeparated)
        val index = nodeInfo.child.index % 2
        val childTypeUse = parentTypeUsage.typeArguments[index]
        return childTypeUse as TypeInstance
    }

    private fun typeForParentUnnamedSuperType(parentTypeUsage: TypeInstance, nodeInfo: SpptDataNodeInfo): TypeInstance {
        if (Debug.CHECK) check(parentTypeUsage.isNullable)
        val tu = (parentTypeUsage.declaration as UnnamedSupertypeType).subtypes[nodeInfo.parentAlt.option]
        return tu
    }

    private fun typeForParentTuple(parentType: TupleType, nodeInfo: SpptDataNodeInfo): TypeInstance {
        val prop = parentType.getPropertyByIndexOrNull(nodeInfo.child.propertyIndex)
        return typeForProperty(prop, nodeInfo)
    }

    private fun typeForParentElement(parentType: DataType, nodeInfo: SpptDataNodeInfo): TypeInstance {
        return when {
            parentType.subtypes.isNotEmpty() -> {
                val t = parentType.subtypes[nodeInfo.parentAlt.option]
                return t
            }

            else -> {
                val prop = parentType.getPropertyByIndexOrNull(nodeInfo.child.propertyIndex)
                typeForProperty(prop, nodeInfo)
            }
        }
    }

    private fun resolveElementSubtype(typeUse: TypeInstance, nodeInfo: SpptDataNodeInfo): TypeInstance {
        val type = typeUse.declaration
        return when {
            type is DataType && type.subtypes.isNotEmpty() -> {
                val t = type.subtypes[nodeInfo.alt.option]
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
                val propType = prop.typeInstance.declaration
                when (propType) {
                    is PrimitiveType -> (prop.typeInstance)
                    is UnnamedSupertypeType -> {
                        val tu = resolveUnnamedSuperTypeSubtype(prop.typeInstance, nodeInfo)
                        when (tu.declaration) {
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

    private fun resolveCompressed(p: AsmPath, trRule: TransformationRule, nodeInfo: SpptDataNodeInfo): DownData2 {
        val typeDecl = trRule.resolvedType.declaration
        return when {
            typeDecl is StructuredType && nodeInfo.node.rule.isOptional && nodeInfo.node.rule.hasOnyOneRhsItem && nodeInfo.node.rule.rhsItems[0][0].isTerminal -> {
                DownData2(p, NodeTrRules(trRule, SimpleTypeModelStdLib.String.toLeafAsStringTrRule()))
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

            typeDecl is UnnamedSupertypeType -> when {
                // special cases where PT is compressed for choice of concats
                nodeInfo.node.rule.isChoice -> when {
                    typeDecl.subtypes[nodeInfo.alt.option].declaration is TupleType -> DownData2(
                        p,
                        NodeTrRules(trRule, typeDecl.subtypes[nodeInfo.alt.option].toSubtypeTrRule())
                    )

                    else -> DownData2(p, NodeTrRules(trRule))
                }

                else -> DownData2(p, NodeTrRules(trRule))
            }

            else -> DownData2(p, NodeTrRules(trRule))
        }
    }

    private fun resolveUnnamedSuperTypeSubtype(typeUse: TypeInstance, nodeInfo: SpptDataNodeInfo): TypeInstance {
        val type = typeUse.declaration
        return when {
            type is UnnamedSupertypeType -> when {
                nodeInfo.node.rule.isChoice && type.subtypes.isNotEmpty() -> {
                    val t = type.subtypes[nodeInfo.alt.option]
                    t
                }

                else -> typeUse
            }

            else -> typeUse
        }
    }

    private fun createValueFromLeaf(sentence: Sentence, target: SpptDataNodeInfo): AsmValue? = when {
        target.node.rule.isEmptyTerminal -> AsmNothingSimple // for empty or optional rules
        target.node.rule.isEmptyListTerminal -> null  // return null for empty lists

        else -> {
            val v = sentence.matchedTextNoSkip(target.node)
            AsmPrimitiveSimple.stdString(v)
        }
    }

    private fun createValueFromBranch(sentence: Sentence, downData: DownData2, target: SpptDataNodeInfo, children: List<ChildData>): AsmValue? =
        createValueFromBranch2(downData, target, children)

    private fun createValueFromBranch2(downData: DownData2, target: SpptDataNodeInfo, children: List<ChildData>): AsmValue {
        // optional children should have value Nothing, if it is an empty list then it will contain null
        val asmChildren = children.mapNotNull { it.value }
        val childrenAsmList = when {
            target.node.rule.isListSeparated -> AsmListSeparatedSimple(asmChildren.toSeparatedList())
            else -> AsmListSimple(asmChildren)
        }

        val asmPath = AsmAnySimple.stdAny(downData.path)
        val alternative = AsmPrimitiveSimple.stdInteger(target.alt.option)
        val leaf = when { //FIXME: only sometimes need this!
            children.isNotEmpty() && null != children[0].value && children[0].value!!.isStdString -> children[0].value!!
            else -> AsmNothingSimple
        }
        val selfType = when {
            // target.node.rule.isTerminal -> AsmTransformInterpreter.PARSE_NODE_TYPE_LEAF
            target.node.rule.isListSeparated -> AsmTransformInterpreter.PARSE_NODE_TYPE_BRANCH_SEPARATED
            else -> AsmTransformInterpreter.PARSE_NODE_TYPE_BRANCH_SIMPLE
        }.type()
        val self = AsmStructureSimple(AsmPathSimple(""), selfType.qualifiedTypeName)
        self.setProperty(AsmTransformInterpreter.PATH.asValueName, asmPath, 0)
        self.setProperty(AsmTransformInterpreter.ALTERNATIVE.asValueName, alternative, 1)
        self.setProperty(AsmTransformInterpreter.CHILDREN.asValueName, childrenAsmList, 3)
        self.setProperty(AsmTransformInterpreter.CHILD.asValueName, childrenAsmList, 4)

        val evc = EvaluationContext.of(
            mapOf(
                AsmTransformInterpreter.SELF to self.toTypedObject(selfType)
            )
        )

        val asm = _trf.evaluate(evc, downData.path, downData.trRule.forNode)
        _trf.issues.forEach {
            super.issues.error(null, "Error evaluating transformation rule: ${it.message}")
        }
        return asm
    }

    private fun createValueFromBranch1(sentence: Sentence, downData: DownData2, target: SpptDataNodeInfo, children: List<ChildData>): AsmValue? {
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
                val type = downData.trRule.forNode.resolvedType.declaration
                when (type) {
                    is PrimitiveType -> {
                        createStringValueFromBranch(sentence, target)
                    }

                    is UnnamedSupertypeType -> {
                        val actualType = type.subtypes[target.alt.option].declaration
                        when (actualType) {
                            is TupleType -> createTupleFrom(sentence, actualType, downData.path, children)
                            else -> children[0].value as AsmValue
                        }
                    }

                    is CollectionType -> when (type) {

                        SimpleTypeModelStdLib.List -> {
                            when {
                                null != targetType && targetType.resolvedType.declaration != SimpleTypeModelStdLib.List && targetType.resolvedType.declaration is DataType -> {
                                    val propValue = when {
                                        target.node.rule.isListSeparated -> {
                                            val alist = createListSimpleValueFromBranch(
                                                target,
                                                downData.path,
                                                children.map { it.value as AsmValue? },
                                                type
                                            )
                                            val els = alist.elements.toSeparatedList<AsmValue, AsmValue, AsmValue>().items
                                            AsmListSimple(els)
                                        }

                                        else -> createListSimpleValueFromBranch(target, downData.path, children.map { it.value as AsmValue? }, type)
                                    }
                                    val propDecl = (targetType.resolvedType.declaration as DataType).property.first()
                                    val el = createAsmElement(downData.path, targetType.resolvedType.declaration.qualifiedName)
                                    setPropertyFromDeclaration(el, propDecl, propValue)
                                    el
                                }

                                else -> createListSimpleValueFromBranch(target, downData.path, children.map { it.value as AsmValue? }, type)
                            }
                        }

                        SimpleTypeModelStdLib.ListSeparated -> {
                            when {
                                null != targetType && targetType.resolvedType.declaration != SimpleTypeModelStdLib.ListSeparated && targetType.resolvedType.declaration is DataType -> {
                                    val propValue = createListSeparatedValueFromBranch(target, downData.path, children.map { it.value }, type)
                                    val propDecl = (targetType.resolvedType.declaration as DataType).property.first()
                                    val el = createAsmElement(downData.path, targetType.resolvedType.declaration.qualifiedName)
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
                            val el = createAsmElement(downData.path, type.qualifiedName)
                            for (propDecl in type.property) {
                                val propType = propDecl.typeInstance.declaration
                                val propValue: AsmValue? = when (propType) {
                                    is PrimitiveType -> {
                                        val childData = children[propDecl.index]
                                        createStringValueFromBranch(sentence, childData.nodeInfo)
                                    }

                                    is CollectionType -> when (propType) {
                                        SimpleTypeModelStdLib.List -> {
                                            when {
                                                target.node.rule.isListSimple && target.node.option == RulePosition.OPTION_MULTI_EMPTY -> AsmListSimple(emptyList())
                                                target.node.rule.isList -> createList(target, children.map { it.value as AsmValue })
                                                else -> {
                                                    val childData = children[propDecl.index]
                                                    when {
                                                        childData.nodeInfo.node.rule.isList -> when {
                                                            null == childData.value -> AsmListSimple(emptyList())
                                                            childData.value is AsmList -> createList(childData.nodeInfo, childData.value.elements)
                                                            childData.value is AsmStructure -> childData.value.property.values.first().value as AsmList
                                                            else -> AsmListSimple(listOf(childData.value as AsmValue))
                                                        }

                                                        else -> error("Internal Error: cannot create a ListSimple from '$childData'")
                                                    }
                                                }
                                            }
                                        }

                                        SimpleTypeModelStdLib.ListSeparated -> {
                                            when {
                                                //childData.nodeInfo.node.rule.isEmptyTerminal -> AsmListSeparatedSimple(emptyList())
                                                target.node.rule.isListSeparated && target.node.option == RulePosition.OPTION_MULTI_EMPTY -> AsmListSeparatedSimple(
                                                    emptyListSeparated()
                                                )

                                                target.node.rule.isList -> AsmListSeparatedSimple(children.map { it.value as AsmValue }
                                                    .toSeparatedList<AsmValue, AsmValue, AsmValue>())

                                                else -> {
                                                    val childData = children[propDecl.index]
                                                    when {
                                                        childData.nodeInfo.node.rule.isList -> when {
                                                            null == childData.value -> AsmListSeparatedSimple(emptyListSeparated())
                                                            childData.value is AsmListSeparated -> childData.value
                                                            childData.value is AsmList -> AsmListSeparatedSimple(childData.value.elements.toSeparatedList<AsmValue, AsmValue, AsmValue>())
                                                            childData.value is AsmStructure -> childData.value.property.values.first().value as AsmListSeparated
                                                            else -> AsmListSeparatedSimple(listOf(childData.value as AsmValue).toSeparatedList())
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

    private fun createValueFor(sentence: Sentence, type: TypeDeclaration, path: AsmPath, childData: ChildData): AsmValue = when (type) {
        is PrimitiveType -> createStringValueFromBranch(sentence, childData.nodeInfo)
        is UnnamedSupertypeType -> TODO()
        is CollectionType -> TODO()
        is TupleType -> createTupleFrom(sentence, type, path, childData.value as List<ChildData>)
        is DataType -> createElementFrom(sentence, type, path, childData.value as List<ChildData>)
        else -> when (type) {
            typeModel.NothingType -> TODO()
            typeModel.AnyType -> TODO()
            else -> error("Shold not happen")
        }
    }

    private fun createStringValueFromBranch(sentence: Sentence, target: SpptDataNodeInfo): AsmValue = when {
        target.node.startPosition == target.node.nextInputNoSkip -> AsmNothingSimple
        else -> {
            val str = sentence.matchedTextNoSkip(target.node)
            AsmPrimitiveSimple.stdString(str)
        }
    }

    private fun createList(nodeData: SpptDataNodeInfo, list: List<AsmValue>): AsmList {
        return when {
            nodeData.node.rule.isListSimple -> AsmListSimple(list)
            nodeData.node.rule.isListSeparated -> {
                val rhs = (nodeData.node.rule as RuntimeRule).rhs as RuntimeRuleRhsListSeparated
                when {
                    rhs.separatorRhsItem.isTerminal -> AsmListSimple(list.toSeparatedList<AsmValue, AsmValue, AsmValue>().items)
                    else -> AsmListSimple(list.toSeparatedList<AsmValue, AsmValue, AsmValue>().separators)
                }
            }

            else -> error("Internal error: List kind not handled")
        }
    }

    private fun createListSimpleValueFromBranch(target: SpptDataNodeInfo, path: AsmPath, children: List<AsmValue?>, type: TypeDeclaration): AsmList {
        if (Debug.CHECK) check(type == SimpleTypeModelStdLib.List)
        return when {
            target.node.rule.isEmptyTerminal -> AsmListSimple(emptyList())
            target.node.rule.isEmptyListTerminal -> AsmListSimple(emptyList())
            target.node.rule.isList -> AsmListSimple(children.filterNotNull())
            else -> error("Internal Error: cannot create a List from '$target'")
        }
    }

    private fun createListSeparatedValueFromBranch(target: SpptDataNodeInfo, path: AsmPath, children: List<Any?>, type: TypeDeclaration): AsmListSeparated {
        if (Debug.CHECK) check(type == SimpleTypeModelStdLib.ListSeparated)
        return when {
            target.node.rule.isEmptyTerminal -> AsmListSeparatedSimple(emptyListSeparated())
            target.node.rule.isEmptyListTerminal -> AsmListSeparatedSimple(emptyListSeparated())
            target.node.rule.isList -> {
                val sList = (children as List<AsmValue>).toSeparatedList<AsmValue, AsmValue, AsmValue>()
                AsmListSeparatedSimple(sList)
            }

            else -> error("Internal Error: cannot create a List from '$target'")
        }
    }

    private fun createTupleFrom(sentence: Sentence, type: TupleType, path: AsmPath, children: List<ChildData>): AsmStructure {
        val el = createAsmElement(path, type.qualifiedName) // TODO: should have a createTuple method

        for (propDecl in type.property) {
            val propChildData = children[propDecl.index]
            val propValue = propChildData.value //createValueFor(sentence, propType.type, path, propChildData)
            setPropertyFromDeclaration(el, propDecl, propValue as AsmValue?)
        }
        return el
    }

    private fun createElementFrom(sentence: Sentence, type: DataType, path: AsmPath, children: List<ChildData>): AsmStructure {
        return if (type.subtypes.isNotEmpty()) {
            if (Debug.CHECK) check(1 == children.size)
            children[0].value as AsmStructure
        } else {
            val el = createAsmElement(path, type.qualifiedName)
            for (propDecl in type.property) {
                val propPath = path + propDecl.name.value
                val propType = propDecl.typeInstance.declaration
                val childData = children[propDecl.index]
                val propValue: AsmValue? = when (propType) {
                    is PrimitiveType -> {
                        createStringValueFromBranch(sentence, childData.nodeInfo)
                    }

                    is CollectionType -> when (propType) {
                        SimpleTypeModelStdLib.List -> {
                            when {
                                childData.nodeInfo.node.rule.isEmptyTerminal -> AsmListSimple(emptyList())
                                childData.nodeInfo.node.rule.isEmptyListTerminal -> AsmListSimple(emptyList())
                                childData.nodeInfo.node.rule.isList -> when {
                                    childData.value is AsmList -> childData.value
                                    childData.value is AsmStructure -> childData.value.property.values.first().value as AsmList

                                    else -> TODO()
                                }

                                else -> error("Internal Error: cannot create a List from '$childData'")
                            }
                        }

                        SimpleTypeModelStdLib.ListSeparated -> {
                            TODO()
                        }

                        else -> error("Should not happen")
                    }

                    is TupleType -> createTupleFrom(sentence, propType, path, childData.value as List<ChildData>)

                    is UnnamedSupertypeType -> {
                        val actualType = propType.subtypes[childData.nodeInfo.parentAlt.option].declaration
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

    private fun setPropertyFromDeclaration(el: AsmStructure, declaration: PropertyDeclaration, value: AsmValue?) {
        // whether it is a reference or not is handled later in Semantic Analysis
        val v = value ?: AsmNothingSimple
        el.setProperty(declaration.name.asValueName, v, declaration.index)
    }

}
