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

import net.akehurst.language.agl.api.runtime.Rule
import net.akehurst.language.agl.collections.toSeparatedList
import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsEmbedded
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsListSeparated
import net.akehurst.language.agl.sppt.TreeDataComplete
import net.akehurst.language.agl.util.Debug
import net.akehurst.language.api.analyser.ScopeModel
import net.akehurst.language.api.analyser.SyntaxAnalyser
import net.akehurst.language.api.asm.AsmElementPath
import net.akehurst.language.api.asm.AsmElementSimple
import net.akehurst.language.api.asm.AsmSimple
import net.akehurst.language.api.grammar.GrammarItem
import net.akehurst.language.api.grammarTypeModel.GrammarTypeModel
import net.akehurst.language.api.grammarTypeModel.StringType
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.SentenceContext
import net.akehurst.language.api.sppt.*
import net.akehurst.language.collections.lazyMap
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
abstract class SyntaxAnalyserSimpleAbstract<A : AsmSimple>(
    val typeModel: GrammarTypeModel,
    val scopeModel: ScopeModel
) : SyntaxAnalyserFromTreeDataAbstract<A>() {

    companion object {
        private const val ns = "net.akehurst.language.agl.syntaxAnalyser"
        const val CONFIGURATION_KEY_AGL_SCOPE_MODEL = "$ns.scope.model"

        val PropertyDeclaration.isTheSingleProperty get() = this.owner.property.size == 1

        val Rule.hasOnyOneRhsItem get() = this.rhsItems.size == 1 && this.rhsItems[0].size == 1
    }

    private var _asm: AsmSimple? = null
    override val asm: A get() = _asm as A

    override val embeddedSyntaxAnalyser: Map<String, SyntaxAnalyser<A>> = lazyMap { embGramName ->
        val emTm = this.typeModel.imports.firstOrNull { it.name == embGramName } ?: error("TypeModel for '$embGramName' not found")
        when (emTm) {
            !is GrammarTypeModel -> error("TypeModel for '$embGramName' is not a GrammarTypeModel")
            else -> SyntaxAnalyserSimple(emTm, this.scopeModel) as SyntaxAnalyser<A>
        }
    }

    private fun findTypeForRule(ruleName: String) = this.typeModel.findTypeUsageForRule(ruleName)

    override fun clear() {
        super.clear()
        this._asm = null
    }

    override fun configure(configurationContext: SentenceContext<GrammarItem>, configuration: Map<String, Any>): List<LanguageIssue> {
        return emptyList()
    }

    override fun walkTree(sentence: Sentence, treeData: TreeDataComplete<out SpptDataNode>, skipDataAsTree: Boolean) {
        val syntaxAnalyserStack = mutableStackOf(this)
        val downStack = mutableStackOf<DownData?>() //when null don't use branch
        val stack = mutableStackOf<ChildData>()
        val walker = object : SpptWalker {
            override fun beginTree() {
                syntaxAnalyserStack.peek().createAsm()
            }

            override fun endTree() {
                val root = stack.pop()
                syntaxAnalyserStack.peek().setAsmRoot(root.value!!)
                //do not pop the asm, leave it here, so it can be retrieved when wanted.
                // embedded ASMs are popped in endEmbedded
            }

            override fun leaf(nodeInfo: SpptDataNodeInfo) {
                val value = syntaxAnalyserStack.peek().createValueFromLeaf(sentence, nodeInfo)
                stack.push(ChildData(nodeInfo, value))
            }

            override fun beginBranch(nodeInfo: SpptDataNodeInfo) {
                val parentDownData = downStack.peekOrNull()
                val p = when {
                    downStack.isEmpty -> AsmElementPath.ROOT
                    null == parentDownData -> AsmElementPath.ROOT.plus("<error>")  // property unused
                    else -> syntaxAnalyserStack.peek().pathFor(parentDownData.path, parentDownData.typeUse.forChildren.type, nodeInfo)
                }
                val tu = when {
                    downStack.isEmpty -> {
                        val typeUse = syntaxAnalyserStack.peek().findTypeForRule(nodeInfo.node.rule.tag)
                            ?: error("Type not found for ${nodeInfo.node.rule.tag}")
                        typeUse
                    }

                    else -> syntaxAnalyserStack.peek().typeForNode(parentDownData?.typeUse?.forChildren, nodeInfo)
                }
                val tuc = resolveCompressed(tu, nodeInfo)
                val dd = when {
                    tuc.forNode.type is NothingType -> null //could test for NothingType instead of null when used
                    else -> DownData(p, tuc, false)
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
                val value = when {
                    null == downData -> null //branch not used for element property value, push null for correct num children on stack
                    //nodeInfo.node.rule.isOptional -> {
                    //TODO("this currently causes issues")
                    //    (children[0] as ChildData).value
                    //}

                    else -> syntaxAnalyserStack.peek().createValueFromBranch(sentence, downData, nodeInfo, adjChildren)
                }
                value?.let { locationMap[it] = sentence.locationFor(nodeInfo.node) }
                stack.push(ChildData(nodeInfo, value))
                // path = path.parent!!
            }

            override fun beginEmbedded(nodeInfo: SpptDataNodeInfo) {
                val embeddedRhs = (nodeInfo.node.rule as RuntimeRule).rhs as RuntimeRuleRhsEmbedded
                val embRuleName = embeddedRhs.embeddedStartRule.tag
                val embGrmName = embeddedRhs.embeddedRuntimeRuleSet.name
                val embSyntaxAnalyser = embeddedSyntaxAnalyser[embGrmName] as SyntaxAnalyserSimpleAbstract? ?: error("Embedded SyntaxAnalyser not found for '$embGrmName'")
                syntaxAnalyserStack.push(embSyntaxAnalyser)
                val parentDownData = downStack.peek()!!
                val p = syntaxAnalyserStack.peek().pathFor(parentDownData.path, parentDownData.typeUse.forChildren.type, nodeInfo)
                val tu = syntaxAnalyserStack.peek().findTypeForRule(embRuleName)
                    ?: error("Type not found for $embRuleName")
                val tuc = resolveCompressed(tu, nodeInfo)
                val dd = when {
                    tuc.forNode.type is NothingType -> null //could test for NothingType instead of null when used
                    else -> DownData(p, tuc, false)
                }
                downStack.push(dd)
            }

            override fun endEmbedded(nodeInfo: SpptDataNodeInfo) {
                val embSyntaxAnalyser = syntaxAnalyserStack.pop()
                val embeddedAsm = embSyntaxAnalyser._asm!!

                val value = embeddedAsm.rootElements[0]
                value?.let { locationMap[it] = sentence.locationFor(nodeInfo.node) }
                stack.push(ChildData(nodeInfo, value))
            }

            override fun skip(startPosition: Int, nextInputPosition: Int) {
                // do nothing
            }

            override fun error(msg: String, path: () -> List<SpptDataNode>) {
                TODO("not implemented")
            }

        }
        treeData.traverseTreeDepthFirst(walker, false)
    }

    private fun createAsm() {
        this._asm = AsmSimple()
    }

    private fun setAsmRoot(value: Any) {
        this._asm!!.addRoot(value)
    }

    private fun createAsmElement(path: AsmElementPath, name: String): AsmElementSimple =
        this._asm!!.createElement(path, name)

    private fun pathFor(parentPath: AsmElementPath, parentType: TypeDefinition, nodeInfo: SpptDataNodeInfo): AsmElementPath {
        return when (parentType) {
            is PrimitiveType -> parentPath
            is AnyType -> TODO()
            is NothingType -> parentPath.plus("<error>")
            is UnnamedSuperTypeType -> parentPath
            is ListSimpleType -> parentPath.plus(nodeInfo.child.index.toString())
            is ListSeparatedType -> parentPath.plus(nodeInfo.child.index.toString())
            is TupleType -> {
                val prop = parentType.getPropertyByIndex(nodeInfo.child.propertyIndex)
                prop?.let { parentPath.plus(prop.name) } ?: parentPath.plus("<error>")
            }

            is ElementType -> {
                when {
                    parentType.subtypes.isNotEmpty() -> parentPath
                    else -> {
                        val prop = parentType.getPropertyByIndex(nodeInfo.child.propertyIndex)
                        prop?.let { parentPath.plus(prop.name) } ?: parentPath.plus("<error>")
                    }
                }
            }
        }
    }

    private fun typeForNode(parentTypeUsage: TypeUsage?, nodeInfo: SpptDataNodeInfo): TypeUsage {
        return when {
            null == parentTypeUsage -> NothingType.use // property unused
            parentTypeUsage.nullable -> typeForParentOptional(parentTypeUsage, nodeInfo)
            nodeInfo.node.rule.isEmbedded -> typeForEmbedded(parentTypeUsage, nodeInfo)
            else -> {
                val parentType = parentTypeUsage.type
                when (parentType) {
                    is PrimitiveType -> parentTypeUsage
                    is AnyType -> TODO()
                    is NothingType -> NothingType.use // property unused
                    is UnnamedSuperTypeType -> typeForParentUnnamedSuperType(parentTypeUsage, nodeInfo)
                    is ListSimpleType -> typeForParentListSimple(parentTypeUsage, nodeInfo)
                    is ListSeparatedType -> typeForParentListSeparated(parentTypeUsage, nodeInfo)
                    is TupleType -> typeForParentTuple(parentType, nodeInfo)
                    is ElementType -> typeForParentElement(parentType, nodeInfo)
                }
            }
        }
    }

    private fun typeForParentOptional(parentTypeUsage: TypeUsage, nodeInfo: SpptDataNodeInfo): TypeUsage {
        // nodes map to runtime-rules, not user-rules
        // if user-rule only had one optional item, then runtime-rule is 'compressed, i.e. no pseudo rule for the option
        if (Debug.CHECK) check(parentTypeUsage.nullable)
        return parentTypeUsage.notNullable
    }

    private fun typeForEmbedded(parentTypeUsage: TypeUsage, nodeInfo: SpptDataNodeInfo): TypeUsage {
        // need to skip over the embedded node and use type of its child
        if (Debug.CHECK) check(nodeInfo.node.rule.isEmbedded)
        val type = parentTypeUsage.type
        return when (type) {
            is ElementType -> {
                val prop = type.getPropertyByIndex(nodeInfo.child.propertyIndex)
                prop?.typeUse ?: NothingType.use
            }

            else -> parentTypeUsage
        }
    }

    private fun typeForParentListSimple(parentTypeUsage: TypeUsage, nodeInfo: SpptDataNodeInfo): TypeUsage {
        // nodes map to runtime-rules, not user-rules
        // if user-rule only had one list item, then runtime-rule is 'compressed, i.e. no pseudo rule for the list
        if (Debug.CHECK) check(parentTypeUsage.type is ListSimpleType)
        val itemTypeUse = parentTypeUsage.arguments[0]
        return itemTypeUse
    }

    private fun typeForParentListSeparated(parentTypeUsage: TypeUsage, nodeInfo: SpptDataNodeInfo): TypeUsage {
        // nodes map to runtime-rules, not user-rules
        // if user-rule only had one slist item, then runtime-rule is 'compressed, i.e. no pseudo rule for the slist
        if (Debug.CHECK) check(parentTypeUsage.type is ListSeparatedType)
        val index = nodeInfo.child.index % 2
        val childTypeUse = parentTypeUsage.arguments[index]
        return childTypeUse
    }

    private fun typeForParentUnnamedSuperType(parentTypeUsage: TypeUsage, nodeInfo: SpptDataNodeInfo): TypeUsage {
        if (Debug.CHECK) check(parentTypeUsage.nullable)
        val tu = (parentTypeUsage.type as UnnamedSuperTypeType).subtypes[nodeInfo.parentAlt.option]
        return tu
    }

    private fun typeForParentTuple(parentType: TupleType, nodeInfo: SpptDataNodeInfo): TypeUsage {
        val prop = parentType.getPropertyByIndex(nodeInfo.child.propertyIndex)
        return typeForProperty(prop, nodeInfo)
    }

    private fun typeForParentElement(parentType: ElementType, nodeInfo: SpptDataNodeInfo): TypeUsage {
        return when {
            parentType.subtypes.isNotEmpty() -> {
                val t = parentType.subtypes[nodeInfo.parentAlt.option]
                return t.typeUse()
            }

            else -> {
                val prop = parentType.getPropertyByIndex(nodeInfo.child.propertyIndex)
                typeForProperty(prop, nodeInfo)
            }
        }
    }

    private fun resolveElementSubtype(typeUse: TypeUsage, nodeInfo: SpptDataNodeInfo): TypeUsage {
        val type = typeUse.type
        return when {
            type is ElementType && type.subtypes.isNotEmpty() -> {
                val t = type.subtypes[nodeInfo.alt.option]
                t.typeUse()
            }

            else -> typeUse
        }
    }

    private fun typeForProperty(prop: PropertyDeclaration?, nodeInfo: SpptDataNodeInfo): TypeUsage {
        return when {
            null == prop -> NothingType.use // property unused
            prop.typeUse.nullable -> prop.typeUse//typeForOptional(propTypeUse, nodeInfo)
            else -> {
                val propType = prop.typeUse.type
                when (propType) {
                    is PrimitiveType -> (prop.typeUse)
                    is AnyType -> TODO()
                    is NothingType -> (NothingType.use)
                    is UnnamedSuperTypeType -> {
                        val tu = resolveUnnamedSuperTypeSubtype(prop.typeUse, nodeInfo)
                        when (tu.type) {
                            is TupleType -> (tu)
                            else -> (prop.typeUse)
                        }
                        //NodeTypes(tu)
                        //NodeTypes(prop.typeUse)//typeForUnnamedSuperType(propTypeUse, nodeInfo)
                    }

                    is ListSimpleType -> (prop.typeUse)
                    is ListSeparatedType -> (prop.typeUse)
                    is TupleType -> (prop.typeUse)
                    is ElementType -> (prop.typeUse)
                }
            }
        }
    }

    private fun resolveCompressed(typeUsage: TypeUsage, nodeInfo: SpptDataNodeInfo): NodeTypes {
        val type = typeUsage.type
        return when {
            type is StructuredRuleType && nodeInfo.node.rule.isOptional && nodeInfo.node.rule.hasOnyOneRhsItem && nodeInfo.node.rule.rhsItems[0][0].isTerminal -> {
                NodeTypes(typeUsage, typeModel.StringType.use)
            }

            type is ElementType && type.property.size == 1 -> {
                // special cases where PT is compressed for lists (and optionals)
                when {
                    type.property.values.first().typeUse.nullable -> NodeTypes(typeUsage, type.property.values.first().typeUse)
                    nodeInfo.node.rule.isOptional -> NodeTypes(typeUsage)
                    nodeInfo.node.rule.isList -> NodeTypes(typeUsage, type.property.values.first().typeUse)
                    else -> NodeTypes(typeUsage)
                }
            }

            type is UnnamedSuperTypeType -> when {
                // special cases where PT is compressed for choice of concats
                nodeInfo.node.rule.isChoice -> when {
                    type.subtypes[nodeInfo.alt.option].type is TupleType -> NodeTypes(typeUsage, type.subtypes[nodeInfo.alt.option])
                    else -> NodeTypes(typeUsage)
                }

                else -> NodeTypes(typeUsage)
            }

            else -> NodeTypes(typeUsage)
        }
    }

    private fun resolveUnnamedSuperTypeSubtype(typeUse: TypeUsage, nodeInfo: SpptDataNodeInfo): TypeUsage {
        val type = typeUse.type
        return when {
            type is UnnamedSuperTypeType -> when {
                nodeInfo.node.rule.isChoice && type.subtypes.isNotEmpty() -> {
                    val t = type.subtypes[nodeInfo.alt.option]
                    t
                }

                else -> typeUse
            }

            else -> typeUse
        }
    }

    private fun createValueFromLeaf(sentence: Sentence, target: SpptDataNodeInfo): String? = when {
        target.node.rule.isEmptyTerminal -> null
        else -> sentence.matchedTextNoSkip(target.node)
    }

    private fun createValueFromBranch(sentence: Sentence, downData: DownData, target: SpptDataNodeInfo, children: List<ChildData>): Any? {
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

            target.node.rule.isEmbedded -> children[0].value
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
                                val el = createAsmElement(downData.path, targetType.type.name)
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
                                val el = createAsmElement(downData.path, targetType.type.name)
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
                            val el = createAsmElement(downData.path, type.name)
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

    private fun createValueFor(sentence: Sentence, type: TypeDefinition, path: AsmElementPath, childData: ChildData): Any? = when (type) {
        is PrimitiveType -> createStringValueFromBranch(sentence, childData.nodeInfo)
        is AnyType -> TODO()
        is NothingType -> TODO()
        is UnnamedSuperTypeType -> TODO()
        is ListSimpleType -> TODO()
        is ListSeparatedType -> TODO()
        is TupleType -> createTupleFrom(sentence, type, path, childData)
        is ElementType -> createElementFrom(sentence, type, path, childData.value as List<ChildData>)
    }

    private fun createStringValueFromBranch(sentence: Sentence, target: SpptDataNodeInfo): String? = when {
        target.node.startPosition == target.node.nextInputNoSkip -> null
        else -> sentence.matchedTextNoSkip(target.node)
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

    private fun createTupleFrom(sentence: Sentence, type: TupleType, path: AsmElementPath, childData: ChildData): AsmElementSimple {
        val el = createAsmElement(path, TupleType.INSTANCE_NAME) // TODO: should have a createTuple method
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

    private fun createElementFrom(sentence: Sentence, type: ElementType, path: AsmElementPath, children: List<ChildData>) {
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
            val el = createAsmElement(path, type.name)
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
