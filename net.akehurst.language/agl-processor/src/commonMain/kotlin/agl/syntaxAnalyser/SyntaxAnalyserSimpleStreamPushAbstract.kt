/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.akehurst.language.agl.syntaxAnalyser

import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsEmbedded
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsListSeparated
import net.akehurst.language.agl.util.Debug
import net.akehurst.language.api.grammarTypeModel.GrammarTypeNamespace
import net.akehurst.language.asm.api.AsmPath
import net.akehurst.language.asm.api.AsmStructure
import net.akehurst.language.asm.simple.AsmPathSimple
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.collections.mutableStackOf
import net.akehurst.language.collections.toSeparatedList
import net.akehurst.language.grammar.api.GrammarRuleName
import net.akehurst.language.parser.api.Rule
import net.akehurst.language.parser.api.RulePosition
import net.akehurst.language.reference.api.CrossReferenceModel
import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sppt.api.*
import net.akehurst.language.sppt.treedata.locationForNode
import net.akehurst.language.sppt.treedata.matchedTextNoSkip
import net.akehurst.language.typemodel.api.*
import net.akehurst.language.typemodel.asm.SimpleTypeModelStdLib

data class ChildDataAny(
    val nodeInfo: SpptDataNodeInfo,
    val value: Any?
)

abstract class SyntaxAnalyserSimpleStreamPushAbstract<out AsmType : Any>(
    val grammarNamespaceQualifiedName: QualifiedName,
    val typeModel: TypeModel,
    val scopeModel: CrossReferenceModel
) : SyntaxAnalyserFromTreeDataAbstract<AsmType>() {

    companion object {
        private const val ns = "net.akehurst.language.agl.syntaxAnalyser"
        const val CONFIGURATION_KEY_AGL_SCOPE_MODEL = "$ns.scope.model"

        val PropertyDeclaration.isTheSingleProperty get() = this.owner.property.size == 1

        val Rule.hasOnyOneRhsItem get() = this.rhsItems.size == 1 && this.rhsItems[0].size == 1
    }

    abstract fun startAsm()
    abstract fun finishAsm()

    abstract fun primitive(type: PrimitiveType, value: String?)

    abstract fun startList()
    abstract fun finishList()

    abstract fun startListSeparated()
    abstract fun finishListSeparated()

    abstract fun listElement()

    abstract fun startAsmElement(path: AsmPath, type: DataType)
    abstract fun finishAsmElement(path: AsmPath, type: DataType)

    abstract fun startTuple()
    abstract fun finishTuple(path: AsmPath)

    abstract fun startProperty(declaration: PropertyDeclaration, isRef: Boolean)
    abstract fun finishProperty(declaration: PropertyDeclaration, isRef: Boolean)

    private fun findTypeUsageForRule(ruleName: String): TypeInstance? {
        val ns = this.typeModel.findNamespaceOrNull(grammarNamespaceQualifiedName) as GrammarTypeNamespace?
        return ns?.findTypeForRule(GrammarRuleName(ruleName))
    }

//    override fun configure(configurationContext: SentenceContext<GrammarItem>, configuration: Map<String, Any>): List<LanguageIssue> {
//        return emptyList()
//    }

    override fun walkTree(sentence: Sentence, treeData: TreeData, skipDataAsTree: Boolean) {
        val syntaxAnalyserStack = mutableStackOf(this)
        val downStack = mutableStackOf<DownData?>() //when null don't use branch
        val stack = mutableStackOf<ChildDataAny>()
        val walker = object : SpptWalker {
            override fun beginTree() {
                syntaxAnalyserStack.peek().startAsm()
            }

            override fun endTree() {
                syntaxAnalyserStack.peek().finishAsm()
            }

            override fun leaf(nodeInfo: SpptDataNodeInfo) {
                val type = SimpleTypeModelStdLib.String.declaration as PrimitiveType
                syntaxAnalyserStack.peek().createPrimitiveValue(sentence, type, nodeInfo)
                stack.push(ChildDataAny(nodeInfo, null))
            }

            override fun beginBranch(nodeInfo: SpptDataNodeInfo) {
                val parentDownData = downStack.peekOrNull()
                val p = when {
                    downStack.isEmpty -> AsmPathSimple.ROOT
                    null == parentDownData -> AsmPathSimple.ROOT.plus("<error>")  // property unused
                    else -> syntaxAnalyserStack.peek().pathFor(parentDownData.path, parentDownData.typeUse.forChildren.declaration, nodeInfo)
                }
                val tu = when {
                    downStack.isEmpty -> {
                        val typeUse = syntaxAnalyserStack.peek().findTypeUsageForRule(nodeInfo.node.rule.tag)
                            ?: error("Type not found for ${nodeInfo.node.rule.tag}")
                        typeUse
                    }

                    else -> syntaxAnalyserStack.peek().typeForNode(parentDownData?.typeUse?.forChildren, nodeInfo)
                }
                val tuc = resolveCompressed(tu, nodeInfo)
                val dd = when {
                    tuc.forNode.declaration == typeModel.NothingType -> null //could test for NothingType instead of null when used
                    else -> DownData(p, tuc)
                }
                downStack.push(dd)
                when (tuc.forNode.declaration) {
                    is PrimitiveType -> Unit
                    is UnnamedSupertypeType -> Unit
                    is CollectionType -> when (tuc.forNode.declaration) {
                        SimpleTypeModelStdLib.List -> startList()
                        SimpleTypeModelStdLib.ListSeparated -> startListSeparated()
                        else -> error("Should not happen")
                    }

                    is TupleType -> startTuple()
                    is DataType -> startAsmElement(p, tuc.forNode.declaration as DataType)
                    else -> when (tuc.forNode.declaration) {
                        typeModel.NothingType -> Unit
                        typeModel.AnyType -> Unit
                        else -> error("Shold not happen")
                    }
                }
            }

            override fun endBranch(nodeInfo: SpptDataNodeInfo) {
                val opt = nodeInfo.alt.option
                val numChildren = nodeInfo.numChildrenAlternatives[opt]!!
                val children = stack.pop(numChildren)
                val adjChildren = children.reversed()
                val downData = downStack.pop()
                val value = when {
                    null == downData -> null //branch not used for element property value, push null for correct num children on stack
                    //nodeInfo.node.rule.isOptional -> {
                    //TODO("this currently causes issues")
                    //    (children[0] as ChildData).value
                    //}

                    else -> syntaxAnalyserStack.peek().createValueFromBranch(sentence, downData, nodeInfo, adjChildren)
                }
                value?.let { locationMap[it] = sentence.locationForNode(nodeInfo.node) }
                stack.push(ChildDataAny(nodeInfo, value))
                // path = path.parent!!
            }

            override fun beginEmbedded(nodeInfo: SpptDataNodeInfo) {
                val embeddedRhs = (nodeInfo.node.rule as RuntimeRule).rhs as RuntimeRuleRhsEmbedded
                val embRuleName = embeddedRhs.embeddedStartRule.tag
                val embGrmName = embeddedRhs.embeddedRuntimeRuleSet.qualifiedName
                val embSyntaxAnalyser =
                    embeddedSyntaxAnalyser[QualifiedName(embGrmName)] as SyntaxAnalyserSimpleStreamPushAbstract?
                        ?: error("Embedded SyntaxAnalyser not found for '$embGrmName' in SyntaxAnalyser for '${grammarNamespaceQualifiedName}'")
                syntaxAnalyserStack.push(embSyntaxAnalyser as SyntaxAnalyserSimpleStreamPushAbstract<AsmType>)
                val parentDownData = downStack.peek()!!
                val p = syntaxAnalyserStack.peek().pathFor(parentDownData.path, parentDownData.typeUse.forChildren.declaration, nodeInfo)
                val tu = syntaxAnalyserStack.peek().findTypeUsageForRule(embRuleName)
                    ?: error("Type not found for $embRuleName")
                val tuc = resolveCompressed(tu, nodeInfo)
                val dd = when {
                    tuc.forNode.declaration == typeModel.NothingType -> null //could test for NothingType instead of null when used
                    else -> DownData(p, tuc)
                }
                downStack.push(dd)
            }

            override fun endEmbedded(nodeInfo: SpptDataNodeInfo) {
                val embSyntaxAnalyser = syntaxAnalyserStack.pop()
                //value?.let { locationMap[it] = nodeInfo.node.locationIn(sentence) }
                stack.push(ChildDataAny(nodeInfo, null))
            }

            override fun skip(startPosition: Int, nextInputPosition: Int) {
                // do nothing
            }

            override fun error(msg: String, path: PathFunction) {
                TODO("not implemented")
            }

        }
        treeData.traverseTreeDepthFirst(walker, false)
    }

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
                typeModel.AnyType -> TODO()
                else -> error("Shold not happen")
            }
        }
    }

    private fun typeForNode(parentTypeUsage: TypeInstance?, nodeInfo: SpptDataNodeInfo): TypeInstance {
        return when {
            null == parentTypeUsage -> typeModel.NothingType.type() // property unused
            parentTypeUsage.isNullable -> typeForParentOptional(parentTypeUsage, nodeInfo)
            nodeInfo.node.rule.isEmbedded -> typeForEmbedded(parentTypeUsage, nodeInfo)
            else -> {
                val parentType = parentTypeUsage.declaration
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
                        typeModel.NothingType -> typeModel.NothingType.type()
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
                        else -> error("Shold not happen")
                    }
                }
            }
        }
    }

    private fun resolveCompressed(typeUsage: TypeInstance, nodeInfo: SpptDataNodeInfo): NodeTypes {
        val type = typeUsage.declaration
        return when {
            type is StructuredType && nodeInfo.node.rule.isOptional && nodeInfo.node.rule.hasOnyOneRhsItem && nodeInfo.node.rule.rhsItems[0][0].isTerminal -> {
                NodeTypes(typeUsage, SimpleTypeModelStdLib.String)
            }

            type is DataType && type.property.size == 1 -> {
                // special cases where PT is compressed for lists (and optionals)
                when {
                    type.property.first().typeInstance.isNullable -> NodeTypes(typeUsage, type.property.first().typeInstance)
                    nodeInfo.node.rule.isOptional -> NodeTypes(typeUsage)
                    nodeInfo.node.rule.isList -> NodeTypes(typeUsage, type.property.first().typeInstance)
                    else -> NodeTypes(typeUsage)
                }
            }

            type is UnnamedSupertypeType -> when {
                // special cases where PT is compressed for choice of concats
                nodeInfo.node.rule.isChoice -> when {
                    type.subtypes[nodeInfo.alt.option].declaration is TupleType -> NodeTypes(typeUsage, type.subtypes[nodeInfo.alt.option])
                    else -> NodeTypes(typeUsage)
                }

                else -> NodeTypes(typeUsage)
            }

            else -> NodeTypes(typeUsage)
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

    private fun createValueFromBranch(sentence: Sentence, downData: DownData, target: SpptDataNodeInfo, children: List<ChildDataAny>) {
        val targetType = findTypeUsageForRule(target.node.rule.tag)

        when {
            downData.typeUse.forNode.isNullable && target.node.rule.isOptional -> {
                val child = children[0]
                when {
                    null == child.value -> null
                    else -> {
                        val nonOptChildren = listOf(ChildDataAny(child.nodeInfo, child.value))
                        child.value
                    }
                }
            }

            target.node.rule.isEmbedded -> children[0].value
            else -> {
                val type = downData.typeUse.forNode.declaration
                when (type) {
                    is PrimitiveType -> {
                        createPrimitiveValue(sentence, SimpleTypeModelStdLib.String.declaration as PrimitiveType, target)
                    }

                    is UnnamedSupertypeType -> {
                        val actualType = type.subtypes[target.alt.option].declaration
                        when (actualType) {
                            is TupleType -> createTupleFrom(sentence, actualType, downData.path, children)
                            else -> children[0].value
                        }

                    }

                    is CollectionType -> when (type) {
                        SimpleTypeModelStdLib.List -> {
                            when {
                                null != targetType && targetType.declaration != SimpleTypeModelStdLib.List && targetType.declaration is DataType -> {
                                    finishList()
                                    val elType = targetType.declaration as DataType
                                    val propDecl = elType.property.first()
                                    setPropertyOrReferenceFromDeclaration(elType, propDecl)
                                    finishAsmElement(downData.path, elType)
                                }

                                else -> finishList()
                            }
                        }

                        SimpleTypeModelStdLib.ListSeparated -> {
                            when {
                                null != targetType && targetType.declaration != SimpleTypeModelStdLib.ListSeparated && targetType.declaration is DataType -> {
                                    finishListSeparated()
                                    val elType = targetType.declaration as DataType
                                    val propDecl = elType.property.first()
                                    setPropertyOrReferenceFromDeclaration(elType, propDecl)
                                    finishAsmElement(downData.path, elType)
                                }

                                else -> finishListSeparated()
                            }
                        }

                        else -> error("Should not happen")
                    }

                    is TupleType -> {
                        createTupleFrom(sentence, type, downData.path, children)
                    }

                    is DataType -> {
                        if (type.subtypes.isNotEmpty()) {
                            // ???
                        } else {
                            for (propDecl in type.property) {
                                val propType = propDecl.typeInstance.declaration
                                when (propType) {
                                    is PrimitiveType -> {
                                        val childData = children[propDecl.index]
                                        createPrimitiveValue(sentence, SimpleTypeModelStdLib.String.declaration as PrimitiveType, childData.nodeInfo)
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
                                                            childData.value is AsmStructure -> childData.value.property.values.first().value as List<Any?>
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
                                                childData.nodeInfo.node.rule.isEmptyListTerminal -> emptyList<Any>()
                                                target.node.rule.isList -> children.map { it.value }.toSeparatedList<Any?, Any, Any>()
                                                childData.nodeInfo.node.rule.isList -> when {
                                                    childData.value is List<*> -> childData.value
                                                    childData.value is AsmStructure -> childData.value.property.values.first().value as List<Any?>
                                                    else -> TODO()
                                                }

                                                else -> error("Internal Error: cannot create a ListSeparated from '$childData'")
                                            }
                                        }

                                        else -> error("Should not happen")
                                    }

                                    else -> {
                                        //nothing to do
                                    }
                                }
                                setPropertyOrReferenceFromDeclaration(type, propDecl)
                            }
                            finishAsmElement(downData.path, type)
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

    private fun createValueFor(sentence: Sentence, type: TypeDeclaration, path: AsmPath, childData: ChildDataAny): Any? = when (type) {
        is PrimitiveType -> createPrimitiveValue(sentence, SimpleTypeModelStdLib.String.declaration as PrimitiveType, childData.nodeInfo)
        is UnnamedSupertypeType -> TODO()
        is CollectionType -> TODO()
        is TupleType -> createTupleFrom(sentence, type, path, childData.value as List<ChildDataAny>)
        is DataType -> createElementFrom(sentence, type, path, childData.value as List<ChildDataAny>)
        else -> when (type) {
            typeModel.NothingType -> TODO()
            typeModel.AnyType -> TODO()
            else -> error("Shold not happen")
        }
    }

    private fun createPrimitiveValue(sentence: Sentence, type: PrimitiveType, nodeInfo: SpptDataNodeInfo) {
        val text = when {
            nodeInfo.node.rule.isEmptyTerminal -> null
            nodeInfo.node.rule.isEmptyListTerminal -> null
            else -> sentence.matchedTextNoSkip(nodeInfo.node)
        }
        primitive(type, text)
    }

    private fun createList(nodeData: SpptDataNodeInfo, list: List<Any?>): List<Any?> {
        return when {
            nodeData.node.rule.isListSimple -> list
            nodeData.node.rule.isListSeparated -> {
                val rhs = (nodeData.node.rule as RuntimeRule).rhs as RuntimeRuleRhsListSeparated
                when {
                    rhs.separatorRhsItem.isTerminal -> list.toSeparatedList<Any?, Any, Any>().items
                    else -> list.toSeparatedList<Any?, Any, Any>().separators
                }
            }

            else -> error("Internal error: List kind not handled")
        }
    }

    private fun createTupleFrom(sentence: Sentence, type: TupleType, path: AsmPath, children: List<ChildDataAny>) {
        for (propDecl in type.property) {
            TODO()
        //    setPropertyOrReferenceFromDeclaration(type, propDecl)
        }
        finishTuple(path)
    }

    private fun createElementFrom(sentence: Sentence, type: DataType, path: AsmPath, children: List<ChildDataAny>) {
        if (type.subtypes.isNotEmpty()) {
            if (Debug.CHECK) check(1 == children.size)
            children[0].value
        } else {
            for (propDecl in type.property) {
                val propPath = path + propDecl.name.value
                val propType = propDecl.typeInstance.declaration
                val childData = children[propDecl.index]
                when (propType) {
                    is PrimitiveType -> {
                        val text = when {
                            childData.nodeInfo.node.rule.isEmptyTerminal -> null
                            childData.nodeInfo.node.rule.isEmptyListTerminal -> null
                            else -> sentence.matchedTextNoSkip(childData.nodeInfo.node)
                        }
                        primitive(propType, text)
                    }

                    is CollectionType -> finishList()

                    is TupleType -> createTupleFrom(sentence, propType, path, childData.value as List<ChildDataAny>)

                    is UnnamedSupertypeType -> {
                        val actualType = propType.subtypes[childData.nodeInfo.parentAlt.option].declaration
                        when (actualType) {
                            is TupleType -> createTupleFrom(sentence, actualType, path, childData.value as List<ChildDataAny>)
                            else -> {
                                TODO()
                            }
                        }

                    }

                    else -> children[propDecl.index]
                }
                setPropertyOrReferenceFromDeclaration(type, propDecl)
            }
            finishAsmElement(path, type)
        }
    }

//    private fun isReference(elType: StructuredType, name: String): Boolean {
//        return scopeModel?.isReference(elType.name, name) ?: false
//    }

    private fun setPropertyOrReferenceFromDeclaration(elType: StructuredType, declaration: PropertyDeclaration) {
        //val isRef = this.isReference(elType, declaration.name)
        val isRef = false
        // hande refs in semantic analysis
        finishProperty(declaration, isRef)
    }
}