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

import net.akehurst.language.agl.collections.toSeparatedList
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.agl.processor.SyntaxAnalysisResultDefault
import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsListSeparated
import net.akehurst.language.agl.sppt.SPPTFromTreeData
import net.akehurst.language.agl.util.Debug
import net.akehurst.language.api.analyser.ScopeModel
import net.akehurst.language.api.analyser.SyntaxAnalyser
import net.akehurst.language.api.asm.AsmElementPath
import net.akehurst.language.api.asm.AsmElementSimple
import net.akehurst.language.api.asm.AsmSimple
import net.akehurst.language.api.grammar.GrammarItem
import net.akehurst.language.api.grammar.RuleItem
import net.akehurst.language.api.grammarTypeModel.GrammarTypeModel
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.api.processor.SentenceContext
import net.akehurst.language.api.processor.SyntaxAnalysisResult
import net.akehurst.language.api.sppt.*
import net.akehurst.language.collections.mutableStackOf
import net.akehurst.language.typemodel.api.*

data class NodeTypes(
    val forNode: TypeUsage,
    val forChildren: TypeUsage
) {
    constructor(t: TypeUsage) : this(t, t)
}

data class DownData(
    val path: AsmElementPath,
    val typeUse: NodeTypes,
    val compressedPT: Boolean
)

data class ChildData(
    val nodeInfo: SpptDataNodeInfo,
    val value: Any?
)

/**
 * TypeName <=> RuleName
 *
 * @param scopeDefinition TypeNameDefiningScope -> Map<TypeNameDefiningSomethingReferencable, referencableProperty>
 * @param references ReferencingTypeName, referencingPropertyName  -> ??
 */
abstract class SyntaxAnalyserSimpleAbstract2<A : AsmSimple>(
    val typeModel: GrammarTypeModel,
    val scopeModel: ScopeModel
) : SyntaxAnalyser<A> {

    companion object {
        private const val ns = "net.akehurst.language.agl.syntaxAnalyser"
        const val CONFIGURATION_KEY_AGL_SCOPE_MODEL = "$ns.scope.model"
    }

    private var _asm: AsmSimple? = null
    private val _issues = IssueHolder(LanguageProcessorPhase.SYNTAX_ANALYSIS)

    //private val _scopeMap = mutableMapOf<AsmElementPath, ScopeSimple<AsmElementPath>>()
    private lateinit var _mapToGrammar: (Int, Int) -> RuleItem

    override val locationMap = mutableMapOf<Any, InputLocation>()

    private fun findTypeForRule(ruleName: String) = this.typeModel.findTypeUsageForRule(ruleName)

    override fun clear() {
        this.locationMap.clear()
        this._asm = null
        this._issues.clear()
    }

    override fun configure(configurationContext: SentenceContext<GrammarItem>, configuration: Map<String, Any>): List<LanguageIssue> {
        return emptyList()
    }

    override fun transform(sppt: SharedPackedParseTree, mapToGrammar: (Int, Int) -> RuleItem): SyntaxAnalysisResult<A> {
        this._mapToGrammar = mapToGrammar
        _asm = AsmSimple()
        val sentence = (sppt as SPPTFromTreeData).input.text
        //val treeData = (sppt as SPPTFromTreeData).treeData
        //var path = AsmElementPath.ROOT // + (_asm!!.rootElements.size).toString()

        val downStack = mutableStackOf<DownData?>() //when null don't use branch
        val stack = mutableStackOf<ChildData>()
        val walker = object : SpptWalker {
            override fun leaf(nodeInfo: SpptDataNodeInfo) {
                val value = createValueFromLeaf(sentence, nodeInfo)
                stack.push(ChildData(nodeInfo, value))
            }

            override fun beginBranch(nodeInfo: SpptDataNodeInfo) = beginBranch2(nodeInfo)

            private fun beginBranch1(nodeInfo: SpptDataNodeInfo) {
                val dd = when {
                    downStack.isEmpty -> { // root
                        val typeUse = this@SyntaxAnalyserSimpleAbstract2.findTypeForRule(nodeInfo.node.rule.tag)
                            ?: error("Type not found for ${nodeInfo.node.rule.tag}")
                        val t = typeUse.type
                        when {
                            t is ElementType && t.property.size == 1 -> {
                                // special cases where PT is compressed for lists (and optionals)
                                when {
                                    t.property.values.first().typeUse.nullable -> {
                                        DownData(AsmElementPath.ROOT, NodeTypes(typeUse, t.property.values.first().typeUse), false)
                                        //DownData(AsmElementPath.ROOT, t.property.values.first().typeUse, true)
                                    }

                                    nodeInfo.node.rule.isOptional -> DownData(AsmElementPath.ROOT, NodeTypes(typeUse, typeUse), false)
                                    nodeInfo.node.rule.isList -> {
                                        DownData(AsmElementPath.ROOT, NodeTypes(typeUse, t.property.values.first().typeUse), false)
                                        //DownData(AsmElementPath.ROOT, t.property.values.first().typeUse, true)
                                    }

                                    else -> DownData(AsmElementPath.ROOT, NodeTypes(typeUse, typeUse), false)
                                }
                            }

                            else -> DownData(AsmElementPath.ROOT, NodeTypes(typeUse, typeUse), false)
                        }
                    }

                    else -> {
                        val parentDownData = downStack.peek()
                        when {
                            null == parentDownData -> null //error("Should not happen")
                            parentDownData.typeUse.forChildren.nullable -> {
                                DownData(parentDownData.path, NodeTypes(parentDownData.typeUse.forChildren.notNullable, parentDownData.typeUse.forChildren.notNullable), false)
                            }

                            else -> {
                                val parentType = parentDownData.typeUse.forChildren.type
                                when (parentType) {
                                    is PrimitiveType -> parentDownData
                                    is AnyType -> TODO()
                                    is NothingType -> TODO()
                                    is UnnamedSuperTypeType -> {
                                        val t = parentType.subtypes[nodeInfo.parentAlt.option]
                                        DownData(parentDownData.path, NodeTypes(t), false)
                                    }

                                    is ListSimpleType -> {
                                        val p = parentDownData.path.plus(nodeInfo.child.index.toString())
                                        val t = parentDownData.typeUse.forChildren.arguments[0]
                                        DownData(p, NodeTypes(t), false)
                                    }

                                    is ListSeparatedType -> {
                                        val p = parentDownData.path.plus(nodeInfo.child.index.toString())
                                        val t = parentDownData.typeUse.forChildren.arguments[0]
                                        DownData(p, NodeTypes(t), false)
                                    }

                                    is TupleType -> {
                                        val prop = parentType.getPropertyByIndex(nodeInfo.child.propertyIndex)
                                        if (null == prop) {
                                            null  // property not used
                                        } else {
                                            val p = parentDownData.path.plus(prop.name)
                                            val t = prop.typeUse
                                            DownData(p, NodeTypes(t), false)
                                        }
                                    }

                                    is ElementType -> {
                                        if (parentType.subtypes.isEmpty()) {
                                            val prop = parentType.getPropertyByIndex(nodeInfo.child.propertyIndex)
                                            if (null == prop) {
                                                null  // property not used
                                            } else {
                                                val p = parentDownData.path.plus(prop.name)
                                                when {
                                                    parentType is ElementType && parentType.property.size == 1 -> {
                                                        when {
                                                            nodeInfo.node.rule.isOptional -> {
                                                                DownData(p, NodeTypes(prop.typeUse), false)
                                                            }

                                                            nodeInfo.node.rule.isList -> {
                                                                //downStack.push(DownData(p, prop.typeUse, false))
                                                                DownData(p, NodeTypes(prop.typeUse), false)
                                                            }

                                                            else -> DownData(p, NodeTypes(prop.typeUse), false)
                                                        }
                                                    }

                                                    prop.typeUse.type is UnnamedSuperTypeType -> {
                                                        val t = (prop.typeUse.type as UnnamedSuperTypeType).subtypes[nodeInfo.alt.option]
                                                        when {
                                                            nodeInfo.node.rule.isOptional -> DownData(p, NodeTypes(prop.typeUse), false)
                                                            t.type is TupleType -> DownData(p, NodeTypes(TypeUsage.ofType(t.type, t.arguments, prop.typeUse.nullable)), false)
                                                            else -> DownData(p, NodeTypes(prop.typeUse), false)
                                                        }
                                                    }

                                                    else -> DownData(p, NodeTypes(prop.typeUse), false)
                                                }

                                            }
                                        } else {
                                            val t = parentType.subtypes[nodeInfo.parentAlt.option]
                                            val p = parentDownData.path
                                            when {
                                                t is ElementType && t.property.size == 1 -> {
                                                    when {
//                                                        t.property.values.first().typeUse.nullable -> {
//                                                            downStack.push(DownData(p, TypeUsage.ofType(t, emptyList(), parentDownData.typeUse.nullable), false))
//                                                            DownData(p, t.property.values.first().typeUse, true)
//                                                        }

                                                        nodeInfo.node.rule.isOptional -> {
                                                            DownData(p, NodeTypes(TypeUsage.ofType(t, emptyList(), parentDownData.typeUse.forChildren.nullable)), false)
                                                        }

                                                        nodeInfo.node.rule.isList -> {
                                                            DownData(
                                                                p,
                                                                NodeTypes(
                                                                    TypeUsage.ofType(t, emptyList(), parentDownData.typeUse.forChildren.nullable),
                                                                    t.property.values.first().typeUse
                                                                ),
                                                                false
                                                            )
                                                        }

                                                        else -> DownData(p, NodeTypes(TypeUsage.ofType(t, emptyList(), parentDownData.typeUse.forChildren.nullable)), false)
                                                    }
                                                }

                                                else -> DownData(p, NodeTypes(TypeUsage.ofType(t, emptyList(), parentDownData.typeUse.forChildren.nullable)), false)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                downStack.push(dd)
            }

            private fun beginBranch2(nodeInfo: SpptDataNodeInfo) {
                val parentDownData = downStack.peekOrNull()
                val p = when {
                    downStack.isEmpty -> AsmElementPath.ROOT
                    null == parentDownData -> AsmElementPath.ROOT.plus("<error>")  // property unused
                    else -> {
                        val parentType = parentDownData!!.typeUse.forChildren.type
                        when (parentType) {
                            is PrimitiveType -> parentDownData.path
                            is AnyType -> TODO()
                            is NothingType -> TODO()
                            is UnnamedSuperTypeType -> parentDownData.path

                            is ListSimpleType -> parentDownData.path.plus(nodeInfo.child.index.toString())

                            is ListSeparatedType -> parentDownData.path.plus(nodeInfo.child.index.toString())

                            is TupleType -> {
                                val prop = parentType.getPropertyByIndex(nodeInfo.child.propertyIndex)
                                prop?.let { parentDownData.path.plus(prop.name) } ?: parentDownData.path.plus("<error>")
                            }

                            is ElementType -> {
                                when {
                                    parentType.subtypes.isNotEmpty() -> parentDownData.path
                                    else -> {
                                        val prop = parentType.getPropertyByIndex(nodeInfo.child.propertyIndex)
                                        prop?.let { parentDownData.path.plus(prop.name) } ?: parentDownData.path.plus("<error>")
                                    }
                                }
                            }
                        }
                    }
                }
                val tu = when {
                    downStack.isEmpty -> {
                        val typeUse = this@SyntaxAnalyserSimpleAbstract2.findTypeForRule(nodeInfo.node.rule.tag)
                            ?: error("Type not found for ${nodeInfo.node.rule.tag}")
                        val tu = typeForCompressedRules(typeUse, nodeInfo)
                        resolveUnnamedSuperType(tu.forChildren, nodeInfo)
                    }

                    else -> typeForNode(parentDownData?.typeUse?.forChildren, nodeInfo)
                }
                val dd = when {
                    tu.forNode.type is NothingType -> null //could test for NothingType instead of null when used
                    else -> DownData(p, tu, false)
                }
                downStack.push(dd)
            }

            override fun endBranch(nodeInfo: SpptDataNodeInfo) {
                val opt = nodeInfo.alt.option
                val numChildren = nodeInfo.numChildrenAlternatives[opt]!!
                val children = stack.pop(numChildren)
                val adjChildren = children.reversed()
                val downData = when {
                    true == downStack.peek()?.compressedPT -> {
                        downStack.pop()
                        downStack.pop()
                    }

                    else -> downStack.pop()
                }
                val value = if (null == downData) {
                    null //branch not used for element property value, push null for correct num children on stack
                } else {
                    createValueFromBranch(sentence, downData, nodeInfo, adjChildren)
                }
                stack.push(ChildData(nodeInfo, value))
                // path = path.parent!!
            }

            override fun beginEmbedded(nodeInfo: SpptDataNodeInfo) {
                TODO("not implemented")
            }

            override fun endEmbedded(nodeInfo: SpptDataNodeInfo) {
                TODO("not implemented")
            }

            override fun skip(startPosition: Int, nextInputPosition: Int) {
                // do nothing
            }

            override fun error(msg: String, path: () -> List<SpptDataNode>) {
                TODO("not implemented")
            }

        }
        sppt.traverseTreeDepthFirst(walker, false)
        val root = stack.pop()
        _asm!!.addRoot(root.value!!)
        return SyntaxAnalysisResultDefault(_asm as A?, _issues, locationMap)
    }

    private fun typeForNode(parentTypeUsage: TypeUsage?, nodeInfo: SpptDataNodeInfo): NodeTypes {
        return when {
            null == parentTypeUsage -> NodeTypes(NothingType.use) // property unused

            else -> when {
                parentTypeUsage.nullable -> {
                    resolveUnnamedSuperType(parentTypeUsage.notNullable, nodeInfo)
                }

                else -> {
                    val parentType = parentTypeUsage.type
                    when (parentType) {
                        is PrimitiveType -> NodeTypes(parentTypeUsage)
                        is AnyType -> TODO()
                        is NothingType -> TODO()
                        is UnnamedSuperTypeType -> {
                            val t = parentType.subtypes[nodeInfo.parentAlt.option]
                            val tc = typeForCompressedRules(t, nodeInfo)
                            tc
                        }

                        is ListSimpleType -> {
                            NodeTypes(parentTypeUsage.arguments[0])
                        }

                        is ListSeparatedType -> {
                            val argIndex = nodeInfo.child.index % 2
                            NodeTypes(parentTypeUsage.arguments[argIndex])
                        }

                        is TupleType -> {
                            val prop = parentType.getPropertyByIndex(nodeInfo.child.propertyIndex)
                            val tu = prop?.typeUse ?: NothingType.use  // property not used if prop==null
                            resolveUnnamedSuperType(tu, nodeInfo)
                        }

                        is ElementType -> {
                            when {
                                parentType.subtypes.isNotEmpty() -> {
                                    val t = parentType.subtypes[nodeInfo.parentAlt.option]
                                    val tc = typeForCompressedRules(t.typeUse(), nodeInfo)
                                    tc
                                }

                                else -> {
                                    val prop = parentType.getPropertyByIndex(nodeInfo.child.propertyIndex)
                                    val tu = prop?.typeUse ?: NothingType.use  // property not used if prop==null
                                    resolveUnnamedSuperType(tu, nodeInfo)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun typeForCompressedRules(typeUsage: TypeUsage, nodeInfo: SpptDataNodeInfo): NodeTypes {
        val type = typeUsage.type
        return when {
            type is ElementType && type.property.size == 1 -> {
                // special cases where PT is compressed for lists (and optionals)
                when {
                    type.property.values.first().typeUse.nullable -> NodeTypes(typeUsage, type.property.values.first().typeUse)
                    nodeInfo.node.rule.isOptional -> NodeTypes(typeUsage)
                    nodeInfo.node.rule.isList -> NodeTypes(typeUsage, type.property.values.first().typeUse)
                    else -> NodeTypes(typeUsage)
                }
            }

            else -> NodeTypes(typeUsage)
        }
    }

    private fun resolveUnnamedSuperType(typeUsage: TypeUsage, nodeInfo: SpptDataNodeInfo): NodeTypes {
        return when {
            typeUsage.type is UnnamedSuperTypeType -> {
                val t = (typeUsage.type as UnnamedSuperTypeType).subtypes[nodeInfo.alt.option]
                when {
                    nodeInfo.node.rule.isOptional -> NodeTypes(typeUsage)
                    t.type is TupleType -> NodeTypes(TypeUsage.ofType(t.type, t.arguments, typeUsage.nullable))
                    else -> NodeTypes(typeUsage)
                }
            }

            else -> NodeTypes(typeUsage)
        }
    }

    private fun createValueFromLeaf(sentence: String, target: SpptDataNodeInfo): String? = when {
        target.node.rule.isEmptyTerminal -> null
        else -> sentence.substring(target.node.startPosition, target.node.nextInputNoSkip)
    }

    private fun createValueFromBranch(sentence: String, downData: DownData, target: SpptDataNodeInfo, children: List<ChildData>): Any? {
        val targetType = findTypeForRule(target.node.rule.tag)

        return when {
            //target.node.rule.isOptional && null == targetType -> {
            //    val child = children[0]
            //    child.value
            //}
            downData.typeUse.forNode.nullable && target.node.rule.isOptional -> {
                val child = children[0]
                when {
                    null == child.value -> null
                    else -> {
                        val nonOptChildren = listOf(ChildData(child.nodeInfo, child.value))
                        child.value
                    }
                }
            }

            else -> {
                val type = downData.typeUse.forNode.type
                when (type) {
                    is PrimitiveType -> {
                        createStringValueFromBranch(sentence, target)
                    }

                    is AnyType -> {
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

                    is NothingType -> error("Internal Error: items should not have type 'NothingType'")

                    is UnnamedSuperTypeType -> {
                        val actualType = type.subtypes[target.alt.option].type
                        when (actualType) {
                            is TupleType -> createValueFor(sentence, actualType, downData.path, ChildData(target, children))
                            else -> children[0].value
                        }

                    }

                    is ListSimpleType -> {
                        when {
                            null != targetType && targetType.type !is ListSimpleType && targetType.type is ElementType -> {
                                val propValue = when {
                                    target.node.rule.isListSeparated -> createListSimpleValueFromBranch(
                                        target,
                                        downData.path,
                                        children.map { it.value },
                                        type
                                    ).toSeparatedList<Any, Any>().items

                                    else -> createListSimpleValueFromBranch(target, downData.path, children.map { it.value }, type)
                                }
                                val propDecl = (targetType.type as ElementType).property.values.first()
                                val el = _asm!!.createElement(downData.path, targetType.type.name)
                                setPropertyOrReferenceFromDeclaration(el, propDecl, propValue)
                                el
                            }

                            else -> createListSimpleValueFromBranch(target, downData.path, children.map { it.value }, type)
                        }
                    }

                    is ListSeparatedType -> {
                        when {
                            null != targetType && targetType.type !is ListSeparatedType && targetType.type is ElementType -> {
                                val propValue = createListSeparatedValueFromBranch(target, downData.path, children.map { it.value }, type)
                                val propDecl = (targetType.type as ElementType).property.values.first()
                                val el = _asm!!.createElement(downData.path, targetType.type.name)
                                setPropertyOrReferenceFromDeclaration(el, propDecl, propValue)
                                el
                            }

                            else -> createListSeparatedValueFromBranch(target, downData.path, children.map { it.value }, type)
                        }
                    }

                    is TupleType -> {
                        createTupleFrom(sentence, type, downData.path, ChildData(target, children))
                    }

                    is ElementType -> {
                        if (type.subtypes.isNotEmpty()) {
                            if (Debug.CHECK) check(1 == children.size)
                            children[0].value
                        } else {
                            val el = _asm!!.createElement(downData.path, type.name)
                            for (propDecl in type.property.values) {
                                val propType = propDecl.typeUse.type
                                val propValue: Any? = when (propType) {
                                    is PrimitiveType -> {
                                        val childData = children[propDecl.childIndex]
                                        createStringValueFromBranch(sentence, childData.nodeInfo)
                                    }

                                    is ListSimpleType -> {
                                        when {
                                            target.node.rule.isListSimple && target.node.option == RulePosition.OPTION_MULTI_EMPTY -> emptyList<Any>()
                                            target.node.rule.isList -> createList(target, children.map { it.value })
                                            else -> {
                                                val childData = children[propDecl.childIndex]
                                                when {
                                                    childData.nodeInfo.node.rule.isList -> when {
                                                        null == childData.value -> emptyList()
                                                        childData.value is List<*> -> createList(childData.nodeInfo, childData.value as List<Any?>)
                                                        childData.value is AsmElementSimple -> childData.value.properties.values.first().value as List<Any?>
                                                        else -> listOf(childData.value)
                                                    }

                                                    else -> error("Internal Error: cannot create a ListSimple from '$childData'")
                                                }
                                            }
                                        }
                                    }

                                    is ListSeparatedType -> {
                                        val childData = children[propDecl.childIndex]
                                        when {
                                            childData.nodeInfo.node.rule.isEmptyTerminal -> emptyList<Any>()
                                            target.node.rule.isList -> children.map { it.value }.toSeparatedList<Any, Any>()
                                            childData.nodeInfo.node.rule.isList -> when {
                                                childData.value is List<*> -> childData.value
                                                childData.value is AsmElementSimple -> childData.value.properties.values.first().value as List<Any?>
                                                else -> TODO()
                                            }

                                            else -> error("Internal Error: cannot create a ListSeparated from '$childData'")
                                        }
                                    }

                                    else -> {
                                        val childData = children[propDecl.childIndex]
                                        childData.value
                                    }
                                }
                                setPropertyOrReferenceFromDeclaration(el, propDecl, propValue)
                            }
                            el
                        }
                    }
                }
            }
        }
    }

    private fun createValueFor(sentence: String, type: TypeDefinition, path: AsmElementPath, childData: ChildData): Any? = when (type) {
        is PrimitiveType -> createStringValueFromBranch(sentence, childData.nodeInfo)
        is AnyType -> TODO()
        is NothingType -> TODO()
        is UnnamedSuperTypeType -> TODO()
        is ListSimpleType -> TODO()
        is ListSeparatedType -> TODO()
        is TupleType -> createTupleFrom(sentence, type, path, childData)
        is ElementType -> createElementFrom(sentence, type, path, childData.value as List<ChildData>)
    }

    private fun createStringValueFromBranch(sentence: String, target: SpptDataNodeInfo): String? = when {
        target.node.startPosition == target.node.nextInputNoSkip -> null
        else -> sentence.substring(target.node.startPosition, target.node.nextInputNoSkip)
    }

    private fun createList(nodeData: SpptDataNodeInfo, list: List<Any?>): List<Any?> {
        return when {
            nodeData.node.rule.isListSimple -> list
            nodeData.node.rule.isListSeparated -> {
                val rhs = (nodeData.node.rule as RuntimeRule).rhs as RuntimeRuleRhsListSeparated
                when {
                    rhs.separatorRhsItem.isTerminal -> list.toSeparatedList<Any, Any>().items
                    else -> list.toSeparatedList<Any, Any>().separators
                }
            }

            else -> error("Internal error: List kind not handled")
        }
    }

    private fun createListSimpleValueFromBranch(target: SpptDataNodeInfo, path: AsmElementPath, children: List<Any?>, type: TypeDefinition): List<*> {
        if (Debug.CHECK) check(type is ListSimpleType)
        return when {
            target.node.rule.isEmptyTerminal -> emptyList<Any>()
            target.node.rule.isList -> children.filterNotNull()
            else -> error("Internal Error: cannot create a List from '$target'")
        }
    }

    private fun createListSeparatedValueFromBranch(target: SpptDataNodeInfo, path: AsmElementPath, children: List<Any?>, type: TypeDefinition): List<*> {
        if (Debug.CHECK) check(type is ListSeparatedType)
        return when {
            target.node.rule.isEmptyTerminal -> emptyList<Any>()
            target.node.rule.isList -> {
                val sList = children.toSeparatedList<Any, Any>()
                sList
            }

            else -> error("Internal Error: cannot create a List from '$target'")
        }
    }

    private fun createTupleFrom(sentence: String, type: TupleType, path: AsmElementPath, childData: ChildData): AsmElementSimple {
        val el = _asm!!.createElement(path, TupleType.INSTANCE_NAME) // TODO: should have a createTuple method
        val v = childData.value
        for (propDecl in type.property.values) {
            val propType = propDecl.typeUse
            when (v) {
                is List<*> -> {
                    val propChildData = (childData.value as List<ChildData>)[propDecl.childIndex]
                    val propValue = propChildData.value //createValueFor(sentence, propType.type, path, propChildData)
                    setPropertyOrReferenceFromDeclaration(el, propDecl, propValue)
                }

                else -> TODO()
            }


        }
        return el
    }

    private fun createElementFrom(sentence: String, type: ElementType, path: AsmElementPath, children: List<ChildData>) {
        if (type.subtypes.isNotEmpty()) {
            if (Debug.CHECK) check(1 == children.size)
            //if (Debug.CHECK) {
            //var actualType: ElementType = type
            // while (actualType.subtypes.isNotEmpty()) {
            //     actualType = actualType.subtypes[actualTarget.option]
            // }
            // check(1 == children.size)
            // }
            children[0].value
        } else {
            val el = _asm!!.createElement(path, type.name)
            for (propDecl in type.property.values) {
                val propPath = path + propDecl.name
                val propType = propDecl.typeUse.type
                val childData = children[propDecl.childIndex]
                val propValue: Any? = when (propType) {
                    is PrimitiveType -> {
                        createStringValueFromBranch(sentence, childData.nodeInfo)
                    }

                    is ListSimpleType -> {
                        when {
                            childData.nodeInfo.node.rule.isEmptyTerminal -> emptyList<Any>()
//                            target.rule.isList -> children.map { it.value }
                            childData.nodeInfo.node.rule.isList -> when {
                                childData.value is List<*> -> childData.value
                                childData.value is AsmElementSimple -> childData.value.properties.values.first().value as List<Any?>

                                else -> TODO()
                            }

                            else -> error("Internal Error: cannot create a List from '$childData'")
                        }
                    }

                    is ListSeparatedType -> {
                        TODO()
                        // val listNode = when {
                        //     actualTarget.isList -> actualTarget
                        //     else -> actualTarget.asBranch.nonSkipChildren[propDecl.childIndex]
                        // }
                        // createListSeparatedValueFromBranch(listNode, path, propType)
                    }

                    is TupleType -> createTupleFrom(sentence, propType, path, childData)

                    is UnnamedSuperTypeType -> {
                        val actualType = propType.subtypes[childData.nodeInfo.parentAlt.option].type
                        when (actualType) {
                            is TupleType -> createTupleFrom(sentence, actualType as TupleType, path, childData)
                            else -> {
                                TODO()
                            }
                        }

                    }

                    else -> children[propDecl.childIndex]
                }
                setPropertyOrReferenceFromDeclaration(el, propDecl, propValue)
            }
            el
        }
    }

    private fun isReference(el: AsmElementSimple, name: String): Boolean {
        return scopeModel?.isReference(el.typeName, name) ?: false
    }

    private fun setPropertyOrReferenceFromDeclaration(el: AsmElementSimple, declaration: PropertyDeclaration, value: Any?) {
        val isRef = this.isReference(el, declaration.name)
        when {
            isRef -> el.setPropertyFromDeclaration(declaration, value, true)
            else -> el.setPropertyFromDeclaration(declaration, value, false)
        }
    }
}
