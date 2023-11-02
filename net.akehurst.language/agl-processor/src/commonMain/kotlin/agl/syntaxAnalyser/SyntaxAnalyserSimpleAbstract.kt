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
import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsEmbedded
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsListSeparated
import net.akehurst.language.agl.sppt.TreeDataComplete
import net.akehurst.language.agl.util.Debug
import net.akehurst.language.api.asm.AsmElementPath
import net.akehurst.language.api.asm.AsmElementSimple
import net.akehurst.language.api.asm.AsmSimple
import net.akehurst.language.api.grammarTypeModel.GrammarTypeNamespace
import net.akehurst.language.api.language.reference.CrossReferenceModel
import net.akehurst.language.api.sppt.*
import net.akehurst.language.collections.MutableStack
import net.akehurst.language.collections.mutableStackOf
import net.akehurst.language.collections.toSeparatedList
import net.akehurst.language.typemodel.api.*
import net.akehurst.language.typemodel.simple.SimpleTypeModelStdLib

data class NodeTypes(
    val forNode: TypeInstance,
    val forChildren: TypeInstance
) {
    constructor(t: TypeInstance) : this(t, t)
}

data class DownData(
    val path: AsmElementPath,
    val typeUse: NodeTypes
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
    val grammarNamespaceQualifiedName: String,
    val typeModel: TypeModel,
    val scopeModel: CrossReferenceModel
) : SyntaxAnalyserFromTreeDataAbstract<A>() {

    companion object {
        private const val ns = "net.akehurst.language.agl.syntaxAnalyser"
        const val CONFIGURATION_KEY_AGL_SCOPE_MODEL = "$ns.scope.model"

        val PropertyDeclaration.isTheSingleProperty get() = this.owner.property.size == 1

        val Rule.hasOnyOneRhsItem get() = this.rhsItems.size == 1 && this.rhsItems[0].size == 1
    }

    private var _asm: AsmSimple? = null
    override val asm: A get() = _asm as A

    private fun findTypeUsageForRule(ruleName: String): TypeInstance? {
        val ns = this.typeModel.namespace[grammarNamespaceQualifiedName] as GrammarTypeNamespace?
        return ns?.findTypeUsageForRule(ruleName)
    }

    override fun clear() {
        super.clear()
        this._asm = null
    }

//    override fun configure(configurationContext: SentenceContext<GrammarItem>, configuration: Map<String, Any>): List<LanguageIssue> {
//        return emptyList()
//    }

    override fun walkTree(sentence: Sentence, treeData: TreeDataComplete<out SpptDataNode>, skipDataAsTree: Boolean) {
        val syntaxAnalyserStack: MutableStack<SyntaxAnalyserSimpleAbstract<A>> = mutableStackOf(this)
        val downStack = mutableStackOf<DownData>() //when null don't use branch
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
                syntaxAnalyserStack.peek().addAsmRoot(root.value!!)
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
                    downStack.isEmpty -> AsmElementPath.ROOT + (asm.rootElements.size).toString()
                    null == parentDownData -> AsmElementPath.ROOT.plus("<error>")  // property unused
                    isRoot -> parentDownData.path
                    else -> syntaxAnalyserStack.peek().pathFor(parentDownData.path, parentDownData.typeUse.forChildren.type, nodeInfo)
                }
                val tu = when {
                    isRoot -> {
                        isRoot = false
                        val typeUse = syntaxAnalyserStack.peek().findTypeUsageForRule(nodeInfo.node.rule.tag)
                            ?: error("Type not found for ${nodeInfo.node.rule.tag}")
                        typeUse
                    }

                    else -> syntaxAnalyserStack.peek().typeForNode(parentDownData?.typeUse?.forChildren, nodeInfo)
                }
                val ddcomp = resolveCompressed(p, tu, nodeInfo)
                downStack.push(ddcomp)
            }

            override fun endBranch(nodeInfo: SpptDataNodeInfo) {
                val opt = nodeInfo.alt.option
                val numChildren = nodeInfo.numChildrenAlternatives[opt]!!
                val children = stack.pop(numChildren)
                val adjChildren = children.reversed()
                val downData = downStack.pop()
                val value = when {
                    typeModel.NothingType == downData.typeUse.forNode.type -> null //branch not used for element property value, push null for correct num children on stack
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
                val embSyntaxAnalyser = embeddedSyntaxAnalyser[embGrmName] as SyntaxAnalyserSimpleAbstract?
                    ?: error("Embedded SyntaxAnalyser not found for '$embGrmName' in SyntaxAnalyser for '${grammarNamespaceQualifiedName}'")
                syntaxAnalyserStack.push(embSyntaxAnalyser as SyntaxAnalyserSimpleAbstract<A>)
                val parentDownData = downStack.peek()!!
                val p = syntaxAnalyserStack.peek().pathFor(parentDownData.path, parentDownData.typeUse.forChildren.type, nodeInfo)
                val tu = syntaxAnalyserStack.peek().findTypeUsageForRule(embRuleName)
                    ?: error("Type not found for $embRuleName")
                val dd = resolveCompressed(p, tu, nodeInfo)
                downStack.push(dd)
            }

            override fun endEmbedded(nodeInfo: SpptDataNodeInfo) {
                val embSyntaxAnalyser = syntaxAnalyserStack.pop()
                val embeddedAsm = embSyntaxAnalyser._asm!!
                downStack.pop()
                val value = embeddedAsm.rootElements.last()
                removeAsmRoot(value)
                value?.let { locationMap[it] = sentence.locationForNode(nodeInfo.node) }
                stack.push(ChildData(nodeInfo, value))
            }

            override fun skip(startPosition: Int, nextInputPosition: Int) {
                // do nothing
            }

            override fun error(msg: String, path: () -> List<SpptDataNode>) {
                issues.error(null, "Error 'msg' at '${path().joinToString(separator = "/")}'")
            }

        }
        treeData.traverseTreeDepthFirst(walker, false)
    }

    private fun setAsm(value: AsmSimple) {
        this._asm = value
    }

    private fun addAsmRoot(value: Any) {
        this._asm!!.addRoot(value)
    }

    private fun removeAsmRoot(value: Any) {
        this._asm!!.removeRoot(value)
    }

    private fun createAsmElement(path: AsmElementPath, name: String): AsmElementSimple =
        this._asm!!.createElement(path, name)

    private fun pathFor(parentPath: AsmElementPath, parentType: TypeDeclaration, nodeInfo: SpptDataNodeInfo): AsmElementPath {
        return when (parentType) {
            is PrimitiveType -> parentPath
            is UnnamedSupertypeType -> parentPath
            is CollectionType -> parentPath.plus(nodeInfo.child.index.toString())
            is TupleType -> {
                val prop = parentType.getPropertyByIndexOrNull(nodeInfo.child.propertyIndex)
                prop?.let { parentPath.plus(prop.name) } ?: parentPath.plus("<error>")
            }

            is DataType -> {
                when {
                    parentType.subtypes.isNotEmpty() -> parentPath
                    else -> {
                        val prop = parentType.getPropertyByIndexOrNull(nodeInfo.child.propertyIndex)
                        prop?.let { parentPath.plus(prop.name) } ?: parentPath.plus("<error>")
                    }
                }
            }

            else -> when (parentType) {

                typeModel.NothingType -> parentPath.plus("<error>")
                typeModel.AnyType -> TODO()
                else -> error("Should not happen")
            }
        }
    }

    private fun typeForNode(parentTypeUsage: TypeInstance?, nodeInfo: SpptDataNodeInfo): TypeInstance {
        return when {
            null == parentTypeUsage -> typeModel.NothingType.instance() // property unused
            parentTypeUsage.isNullable -> typeForParentOptional(parentTypeUsage, nodeInfo)
            nodeInfo.node.rule.isEmbedded -> typeForEmbedded(parentTypeUsage, nodeInfo)
            else -> {
                val parentType = parentTypeUsage.type
                when (parentType) {
                    is PrimitiveType -> parentTypeUsage
                    is UnnamedSupertypeType -> typeForParentUnnamedSuperType(parentTypeUsage, nodeInfo)
                    is CollectionType -> when (parentType) {
                        SimpleTypeModelStdLib.List -> typeForParentListSimple(parentTypeUsage, nodeInfo)
                        SimpleTypeModelStdLib.ListSeparated -> typeForParentListSeparated(parentTypeUsage, nodeInfo)
                        else -> error("Should not happen")
                    }

                    is TupleType -> typeForParentTuple(parentType, nodeInfo)
                    is DataType -> typeForParentElement(parentType, nodeInfo)
                    else -> when (parentType) {
                        typeModel.NothingType -> typeModel.NothingType.instance()
                        typeModel.AnyType -> TODO()
                        else -> error("Shold not happen")
                    }
                }
            }
        }
    }

    private fun typeForParentOptional(parentTypeUsage: TypeInstance, nodeInfo: SpptDataNodeInfo): TypeInstance {
        // nodes map to runtime-rules, not user-rules
        // if user-rule only had one optional item, then runtime-rule is 'compressed, i.e. no pseudo rule for the option
        if (Debug.CHECK) check(parentTypeUsage.isNullable)
        return parentTypeUsage.notNullable()
    }

    private fun typeForEmbedded(parentTypeUsage: TypeInstance, nodeInfo: SpptDataNodeInfo): TypeInstance {
        // need to skip over the embedded node and use type of its child
        if (Debug.CHECK) check(nodeInfo.node.rule.isEmbedded)
        val type = parentTypeUsage.type
        return when (type) {
            is DataType -> {
                val prop = type.getPropertyByIndexOrNull(nodeInfo.child.propertyIndex)
                prop?.typeInstance ?: typeModel.NothingType.instance()
            }

            else -> parentTypeUsage
        }
    }

    private fun typeForParentListSimple(parentTypeUsage: TypeInstance, nodeInfo: SpptDataNodeInfo): TypeInstance {
        // nodes map to runtime-rules, not user-rules
        // if user-rule only had one list item, then runtime-rule is 'compressed, i.e. no pseudo rule for the list
        if (Debug.CHECK) check(parentTypeUsage.type == SimpleTypeModelStdLib.List)
        val itemTypeUse = parentTypeUsage.typeArguments[0]
        return itemTypeUse
    }

    private fun typeForParentListSeparated(parentTypeUsage: TypeInstance, nodeInfo: SpptDataNodeInfo): TypeInstance {
        // nodes map to runtime-rules, not user-rules
        // if user-rule only had one slist item, then runtime-rule is 'compressed, i.e. no pseudo rule for the slist
        if (Debug.CHECK) check(parentTypeUsage.type == SimpleTypeModelStdLib.ListSeparated)
        val index = nodeInfo.child.index % 2
        val childTypeUse = parentTypeUsage.typeArguments[index]
        return childTypeUse
    }

    private fun typeForParentUnnamedSuperType(parentTypeUsage: TypeInstance, nodeInfo: SpptDataNodeInfo): TypeInstance {
        if (Debug.CHECK) check(parentTypeUsage.isNullable)
        val tu = (parentTypeUsage.type as UnnamedSupertypeType).subtypes[nodeInfo.parentAlt.option]
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
        val type = typeUse.type
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
            null == prop -> typeModel.NothingType.instance() // property unused
            prop.typeInstance.isNullable -> prop.typeInstance//typeForOptional(propTypeUse, nodeInfo)
            else -> {
                val propType = prop.typeInstance.type
                when (propType) {
                    is PrimitiveType -> (prop.typeInstance)
                    is UnnamedSupertypeType -> {
                        val tu = resolveUnnamedSuperTypeSubtype(prop.typeInstance, nodeInfo)
                        when (tu.type) {
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
                        typeModel.NothingType -> typeModel.NothingType.instance()
                        typeModel.AnyType -> TODO()
                        else -> error("Should not happen")
                    }
                }
            }
        }
    }

    private fun resolveCompressed(p: AsmElementPath, typeUsage: TypeInstance, nodeInfo: SpptDataNodeInfo): DownData {
        val type = typeUsage.type
        return when {
            type is StructuredType && nodeInfo.node.rule.isOptional && nodeInfo.node.rule.hasOnyOneRhsItem && nodeInfo.node.rule.rhsItems[0][0].isTerminal -> {
                DownData(p, NodeTypes(typeUsage, SimpleTypeModelStdLib.String))
            }

            type is DataType && type.property.size == 1 -> {
                // special cases where PT is compressed for lists (and optionals)
                when {
                    type.property.first().typeInstance.isNullable -> {
                        val fProp = type.property.first()
                        val pp = p.plus(fProp.name)
                        DownData(pp, NodeTypes(typeUsage, fProp.typeInstance))
                    }

                    nodeInfo.node.rule.isOptional -> DownData(p, NodeTypes(typeUsage))
                    nodeInfo.node.rule.isList -> {
                        val fProp = type.property.first()
                        val pp = p.plus(fProp.name)
                        DownData(pp, NodeTypes(typeUsage, fProp.typeInstance))
                    }

                    else -> DownData(p, NodeTypes(typeUsage))
                }
            }

            type is UnnamedSupertypeType -> when {
                // special cases where PT is compressed for choice of concats
                nodeInfo.node.rule.isChoice -> when {
                    type.subtypes[nodeInfo.alt.option].type is TupleType -> DownData(p, NodeTypes(typeUsage, type.subtypes[nodeInfo.alt.option]))
                    else -> DownData(p, NodeTypes(typeUsage))
                }

                else -> DownData(p, NodeTypes(typeUsage))
            }

            else -> DownData(p, NodeTypes(typeUsage))
        }
    }

    private fun resolveUnnamedSuperTypeSubtype(typeUse: TypeInstance, nodeInfo: SpptDataNodeInfo): TypeInstance {
        val type = typeUse.type
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

    private fun createValueFromLeaf(sentence: Sentence, target: SpptDataNodeInfo): String? = when {
        target.node.rule.isEmptyTerminal -> null
        else -> sentence.matchedTextNoSkip(target.node)
    }

    private fun createValueFromBranch(sentence: Sentence, downData: DownData, target: SpptDataNodeInfo, children: List<ChildData>): Any? {
        val targetType = findTypeUsageForRule(target.node.rule.tag)

        return when {
            //target.node.rule.isOptional && null == targetType -> {
            //    val child = children[0]
            //    child.value
            //}
            downData.typeUse.forNode.isNullable && target.node.rule.isOptional -> {
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

                    is UnnamedSupertypeType -> {
                        val actualType = type.subtypes[target.alt.option].type
                        when (actualType) {
                            is TupleType -> createValueFor(sentence, actualType, downData.path, ChildData(target, children))
                            else -> children[0].value
                        }

                    }

                    is CollectionType -> when (type) {

                        SimpleTypeModelStdLib.List -> {
                            when {
                                null != targetType && targetType.type != SimpleTypeModelStdLib.List && targetType.type is DataType -> {
                                    val propValue = when {
                                        target.node.rule.isListSeparated -> createListSimpleValueFromBranch(
                                            target,
                                            downData.path,
                                            children.map { it.value },
                                            type
                                        ).toSeparatedList<Any, Any>().items

                                        else -> createListSimpleValueFromBranch(target, downData.path, children.map { it.value }, type)
                                    }
                                    val propDecl = (targetType.type as DataType).property.first()
                                    val el = createAsmElement(downData.path, targetType.type.name)
                                    setPropertyOrReferenceFromDeclaration(el, propDecl, propValue)
                                    el
                                }

                                else -> createListSimpleValueFromBranch(target, downData.path, children.map { it.value }, type)
                            }
                        }

                        SimpleTypeModelStdLib.ListSeparated -> {
                            when {
                                null != targetType && targetType.type != SimpleTypeModelStdLib.ListSeparated && targetType.type is DataType -> {
                                    val propValue = createListSeparatedValueFromBranch(target, downData.path, children.map { it.value }, type)
                                    val propDecl = (targetType.type as DataType).property.first()
                                    val el = createAsmElement(downData.path, targetType.type.name)
                                    setPropertyOrReferenceFromDeclaration(el, propDecl, propValue)
                                    el
                                }

                                else -> createListSeparatedValueFromBranch(target, downData.path, children.map { it.value }, type)
                            }
                        }

                        else -> error("Should not happen")
                    }

                    is TupleType -> {
                        createTupleFrom(sentence, type, downData.path, ChildData(target, children))
                    }

                    is DataType -> {
                        if (type.subtypes.isNotEmpty()) {
                            if (Debug.CHECK) check(1 == children.size)
                            children[0].value
                        } else {
                            val el = createAsmElement(downData.path, type.name)
                            for (propDecl in type.property) {
                                val propType = propDecl.typeInstance.type
                                val propValue: Any? = when (propType) {
                                    is PrimitiveType -> {
                                        val childData = children[propDecl.index]
                                        createStringValueFromBranch(sentence, childData.nodeInfo)
                                    }

                                    is CollectionType -> when (propType) {
                                        SimpleTypeModelStdLib.List -> {
                                            when {
                                                target.node.rule.isListSimple && target.node.option == RulePosition.OPTION_MULTI_EMPTY -> emptyList<Any>()
                                                target.node.rule.isList -> createList(target, children.map { it.value })
                                                else -> {
                                                    val childData = children[propDecl.index]
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

                                        SimpleTypeModelStdLib.ListSeparated -> {
                                            val childData = children[propDecl.index]
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

                                        else -> error("Should not happen")
                                    }

                                    else -> {
                                        val childData = children[propDecl.index]
                                        childData.value
                                    }
                                }
                                setPropertyOrReferenceFromDeclaration(el, propDecl, propValue)
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

                        else -> error("Shold not happen")
                    }
                }
            }
        }
    }

    private fun createValueFor(sentence: Sentence, type: TypeDeclaration, path: AsmElementPath, childData: ChildData): Any? = when (type) {
        is PrimitiveType -> createStringValueFromBranch(sentence, childData.nodeInfo)
        is UnnamedSupertypeType -> TODO()
        is CollectionType -> TODO()
        is TupleType -> createTupleFrom(sentence, type, path, childData)
        is DataType -> createElementFrom(sentence, type, path, childData.value as List<ChildData>)
        else -> when (type) {
            typeModel.NothingType -> TODO()
            typeModel.AnyType -> TODO()
            else -> error("Shold not happen")
        }
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

    private fun createListSimpleValueFromBranch(target: SpptDataNodeInfo, path: AsmElementPath, children: List<Any?>, type: TypeDeclaration): List<*> {
        if (Debug.CHECK) check(type == SimpleTypeModelStdLib.List)
        return when {
            target.node.rule.isEmptyTerminal -> emptyList<Any>()
            target.node.rule.isList -> children.filterNotNull()
            else -> error("Internal Error: cannot create a List from '$target'")
        }
    }

    private fun createListSeparatedValueFromBranch(target: SpptDataNodeInfo, path: AsmElementPath, children: List<Any?>, type: TypeDeclaration): List<*> {
        if (Debug.CHECK) check(type == SimpleTypeModelStdLib.ListSeparated)
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
        val el = createAsmElement(path, type.name) // TODO: should have a createTuple method
        val v = childData.value
        for (propDecl in type.property) {
            val propType = propDecl.typeInstance
            when (v) {
                is List<*> -> {
                    val propChildData = (childData.value as List<ChildData>)[propDecl.index]
                    val propValue = propChildData.value //createValueFor(sentence, propType.type, path, propChildData)
                    setPropertyOrReferenceFromDeclaration(el, propDecl, propValue)
                }

                else -> TODO()
            }


        }
        return el
    }

    private fun createElementFrom(sentence: Sentence, type: DataType, path: AsmElementPath, children: List<ChildData>) {
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
            for (propDecl in type.property) {
                val propPath = path + propDecl.name
                val propType = propDecl.typeInstance.type
                val childData = children[propDecl.index]
                val propValue: Any? = when (propType) {
                    is PrimitiveType -> {
                        createStringValueFromBranch(sentence, childData.nodeInfo)
                    }

                    is CollectionType -> when (propType) {
                        SimpleTypeModelStdLib.List -> {
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

                        SimpleTypeModelStdLib.ListSeparated -> {
                            TODO()
                            // val listNode = when {
                            //     actualTarget.isList -> actualTarget
                            //     else -> actualTarget.asBranch.nonSkipChildren[propDecl.childIndex]
                            // }
                            // createListSeparatedValueFromBranch(listNode, path, propType)
                        }

                        else -> error("Should not happen")
                    }

                    is TupleType -> createTupleFrom(sentence, propType, path, childData)

                    is UnnamedSupertypeType -> {
                        val actualType = propType.subtypes[childData.nodeInfo.parentAlt.option].type
                        when (actualType) {
                            is TupleType -> createTupleFrom(sentence, actualType as TupleType, path, childData)
                            else -> {
                                TODO()
                            }
                        }

                    }

                    else -> children[propDecl.index]
                }
                setPropertyOrReferenceFromDeclaration(el, propDecl, propValue)
            }
            el
        }
    }

//    private fun isReference(el: AsmElementSimple, name: String): Boolean {
//        return scopeModel.isReference(el.typeName, name) ?: false
//    }

    private fun setPropertyOrReferenceFromDeclaration(el: AsmElementSimple, declaration: PropertyDeclaration, value: Any?) {
//        val isRef = this.isReference(el, declaration.name)
//        when {
//            isRef -> el.setPropertyFromDeclaration(declaration, value, true)
//            else -> el.setPropertyFromDeclaration(declaration, value, false)
//        }
        // whether it is a reference or not is handled later in Semantic Analysis
        el.setPropertyFromDeclaration(declaration, value)
    }
}
