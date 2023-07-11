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
        val treeData = (sppt as SPPTFromTreeData).treeData
        var path = AsmElementPath.ROOT // + (_asm!!.rootElements.size).toString()

        val stack = mutableStackOf<Any?>()
        val walker = object : SpptWalker {
            override fun leaf(nodeInfo: SpptDataNodeInfo) {
                val value = createValueFromLeaf(sentence, nodeInfo.node)
                stack.push(value)
            }

            override fun beginBranch(nodeInfo: SpptDataNodeInfo) {
                val typeUse = this@SyntaxAnalyserSimpleAbstract2.findTypeForRule(nodeInfo.node.rule.tag) ?: error("No Element Type for ${nodeInfo.node.rule.tag}")
                val type = typeUse.type
                when (type) {
                    is PrimitiveType -> Unit
                    is AnyType -> Unit
                    is NothingType -> Unit
                    is UnnamedSuperTypeType -> Unit
                    is ListSimpleType -> Unit
                    is ListSeparatedType -> Unit
                    is TupleType -> Unit
                    is ElementType -> Unit
                }
            }

            override fun endBranch(nodeInfo: SpptDataNodeInfo) {
                val opt = nodeInfo.alt.option
                val numChildren = nodeInfo.numChildrenAlternatives[opt]!!
                val children = stack.pop(numChildren)
                val adjChildren = children.reversed()
                val value = createValueFromBranch(nodeInfo.node, path, adjChildren)
                stack.push(value)
                // path = path.parent!!
            }

            override fun beginEmbedded(nodeInfo: SpptDataNodeInfo) {
                TODO("not implemented")
            }

            override fun endEmbedded(nodeInfo: SpptDataNodeInfo) {
                TODO("not implemented")
            }

            override fun skip(startPosition: Int, nextInputPosition: Int) {
                TODO("not implemented")
            }

            override fun error(msg: String, path: () -> List<SpptDataNode>) {
                TODO("not implemented")
            }

        }
        sppt.traverseTreeDepthFirst(walker, false)
        val root = stack.pop()
        _asm!!.addRoot(root)
        return SyntaxAnalysisResultDefault(_asm as A?, _issues, locationMap)
    }

    /*
        private fun createValue(target: SpptDataNode, path: AsmElementPath): Any? {
            val elType = this.findTypeForRule(target.name)
            return when {
                null == elType -> {
                    "No Element Type for ${target.name}" //TODO
                }

                else -> createValue(target, path, elType)//, scope)
            }
        }
    */
    /*
        private fun createValue(target: SpptDataNode, path: AsmElementPath, type: TypeUsage): Any? {
            val nonOptTarget = when {
                type.nullable -> when (target) {
                    is SPPTBranch -> target.asBranch.nonSkipChildren[0]
                    else -> target
                }

                else -> target
            }
            val v = when {
                nonOptTarget.isEmptyLeaf -> null
                else -> when (nonOptTarget) {
                    is SPPTLeaf -> createValueFromLeaf(nonOptTarget)
                    is SPPTBranch -> createValueFromBranch(nonOptTarget, path, type.notNullable)
                    else -> error("should never happen!")
                }
            }
            if (v is AsmElementSimple) {
                locationMap[v] = target.location
            }
            return v
        }
    */
    private fun createValueFromLeaf(sentence: String, target: SpptDataNode): String? = when {
        target.rule.isEmptyTerminal -> null
        else -> sentence.substring(target.startPosition, target.nextInputPosition)
    }

    private fun createValueFromBranch(target: SpptDataNode, path: AsmElementPath, children: List<Any?>): Any? {//, scope: ScopeSimple<AsmElementPath>?): Any? {
        val typeUse = this.findTypeForRule(target.rule.tag) ?: error("No Element Type for ${target.rule.tag}")
        val type = typeUse.type//typeModel.findTypeUsageForRule(target.name) ?: argType
        return when (type) {
            is PrimitiveType -> {
                TODO()
//                createStringValueFromBranch(target)
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
                TODO()
                // var actualType = type.subtypes[target.option]
                //  var actualTarget: SPPTNode = when {
                //     actualType.type is TupleType -> target
                //     else -> target.nonSkipChildren[0]
                // }
                // this.createValue(actualTarget, path, actualType)//, scope)
            }

            is ListSimpleType -> createListSimpleValueFromBranch(target, path, children, typeUse)

            is ListSeparatedType -> createListSeparatedValueFromBranch(target, path, children, typeUse)

            is TupleType -> {
                TODO()
                /*
                val el = _asm!!.createElement(path, type.name)
                //val childsScope = createScope(scope, el)
                for (propDecl in type.property.values) {
                    val propTgt = target.asBranch.nonSkipChildren[propDecl.childIndex]
                    val propType = propDecl.typeUse
                    val propPath = path + propDecl.name
                    val propValue = when {
                        //propTgt.isOptional -> when {
                        //    propDecl.typeUse.nullable -> createValue(propTgt.asBranch.nonSkipChildren[0], propPath, propType)
                        //    else -> error("Internal Error: '$propDecl' is not nullable !")
                        //}

                        propTgt.isEmbedded -> {
                            val embTgt = propTgt.asBranch.nonSkipChildren[0]
                            createValue(embTgt, propPath, propType)
                        }

                        else -> createValue(propTgt, propPath, propType)
                    }
                    setPropertyOrReferenceFromDeclaration(el, propDecl, propValue)
                }
                el
                 */
            }

            is ElementType -> {
                //var actualTarget: SPPTNode = target
                var actualType: ElementType = type
                // while (actualType.subtypes.isNotEmpty()) {
                //    actualType = actualType.subtypes[actualTarget.option]
                //    actualTarget = actualTarget.asBranch.nonSkipChildren[0]
                //}
                val el = _asm!!.createElement(path, actualType.name)
                for (propDecl in actualType.property.values) {
                    val propPath = path + propDecl.name
                    val propType = propDecl.typeUse
                    val propValue = when {
                        propType.type is ListSimpleType -> {
                            TODO()
                            // val listNode = when {
                            //     actualTarget.isList -> actualTarget
                            //     else -> actualTarget.asBranch.nonSkipChildren[propDecl.childIndex]
                            //}
                            //createListSimpleValueFromBranch(listNode, path, propType)
                        }

                        propType.type is ListSeparatedType -> {
                            TODO()
                            // val listNode = when {
                            //     actualTarget.isList -> actualTarget
                            //     else -> actualTarget.asBranch.nonSkipChildren[propDecl.childIndex]
                            // }
                            // createListSeparatedValueFromBranch(listNode, path, propType)
                        }

                        else -> children[propDecl.childIndex]
                    }
                    setPropertyOrReferenceFromDeclaration(el, propDecl, propValue)
                }
                el
            }
        }
    }

    private fun createStringValueFromBranch(sentence: String, target: SpptDataNode): String? = when {
        target.startPosition == target.nextInputPosition -> null
        else -> sentence.substring(target.startPosition, target.nextInputPosition)
    }

    private fun createListSimpleValueFromBranch(target: SpptDataNode, path: AsmElementPath, children: List<Any?>, type: TypeUsage): List<*> {
        if (Debug.CHECK) check(type.type is ListSimpleType)
        return when {
            target.rule.isEmptyTerminal -> emptyList<Any>()
            target.rule.isList -> children
            // a concatenation rule that contains a list maps the list direct to a property
            //target.isBranch && target.asBranch.nonSkipChildren[0].isList -> createListSimpleValueFromBranch(target.asBranch.nonSkipChildren[0], path, type)
            else -> error("Internal Error: cannot create a List from '$target'")
        }
    }

    private fun createListSeparatedValueFromBranch(target: SpptDataNode, path: AsmElementPath, children: List<Any?>, type: TypeUsage): List<*> {
        if (Debug.CHECK) check(type.type is ListSeparatedType)
        return when {
            target.rule.isEmptyTerminal -> emptyList<Any>()
            target.rule.isList -> {
                val sList = children.toSeparatedList<Any, Any>()
                sList
            }
            // a concatenation rule that contains a list maps the list direct to a property
            //target.isBranch && target.asBranch.nonSkipChildren[0].isList -> createListSeparatedValueFromBranch(target.asBranch.nonSkipChildren[0], path, type)
            else -> error("Internal Error: cannot create a List from '$target'")
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
