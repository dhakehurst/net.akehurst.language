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

import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.agl.sppt.SPPTBranchFromInputAndGrownChildren
import net.akehurst.language.api.analyser.AnalyserIssue
import net.akehurst.language.api.analyser.SyntaxAnalyser
import net.akehurst.language.api.asm.AsmElementSimple
import net.akehurst.language.api.asm.AsmSimple
import net.akehurst.language.api.asm.ScopeSimple
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.sppt.SPPTBranch
import net.akehurst.language.api.sppt.SPPTLeaf
import net.akehurst.language.api.sppt.SPPTNode
import net.akehurst.language.api.sppt.SharedPackedParseTree

/**
 * TypeName <=> RuleName
 *
 * @param scopeDefinition TypeNameDefiningScope -> Map<TypeNameDefiningSomethingReferencable, referencableProperty>
 * @param references ReferencingTypeName, referencingPropertyName  -> ??
 */
class SyntaxAnalyserSimple(
    val scopeDefinition: Map<String, Map<String, String>>? = null,
    val references: Map<Pair<String, String>, Any>?=null
) : SyntaxAnalyser<AsmSimple, ContextSimple> {

    private var _asm: AsmSimple? = null
    private var _context: ContextSimple? = null // cached value, provided on call to transform
    private val _issues = mutableListOf<AnalyserIssue>()
    private val _scopes = mutableMapOf<AsmElementSimple, ScopeSimple<AsmElementSimple>>()

    override val locationMap = mutableMapOf<Any, InputLocation>()

    override fun clear() {
        this.locationMap.clear()
        this._context = null
    }

    override fun transform(sppt: SharedPackedParseTree, context: ContextSimple?): Pair<AsmSimple, List<AnalyserIssue>> {
        this._context = context
        _asm = AsmSimple()
        val value = this.createValue(sppt.root, context?.scope)
        _asm?.addRoot(value as AsmElementSimple)

        val issues = this.resolveReferences()

        return Pair(_asm!!, issues)
    }

    private fun createValue(target: SPPTNode, scope: ScopeSimple<AsmElementSimple>?): Any? {
        val v = when (target) {
            is SPPTLeaf -> createValueFromLeaf(target)
            is SPPTBranch -> createValueFromBranch(target, scope)
            else -> error("should never happen!")
        }
        if (v is AsmElementSimple) {
            locationMap[v] = target.location
        }
        return v
    }

    private fun createValueFromLeaf(target: SPPTLeaf): Any? {
        val leaf = target //as SPPTLeafDefault
        val value = when {
            leaf.isEmptyLeaf -> null
            else -> leaf.nonSkipMatchedText
        }
        return value
    }

    private fun createValueFromBranch(target: SPPTBranch, scope: ScopeSimple<AsmElementSimple>?): Any? {
        val br = target as SPPTBranchFromInputAndGrownChildren //SPPTBranchDefault //TODO: make write thing available on interface
        return when (br.runtimeRule.kind) {
            RuntimeRuleKind.TERMINAL -> error("should never happen!")
            RuntimeRuleKind.NON_TERMINAL -> when (br.runtimeRule.rhs.itemsKind) {
                RuntimeRuleRhsItemsKind.EMPTY -> TODO()
                RuntimeRuleRhsItemsKind.CHOICE -> {
                    val v = this.createValue(br.children[0], scope)
                    v
                }
                RuntimeRuleRhsItemsKind.CONCATENATION -> {
                    val count = mutableMapOf<String, Int>()
                    val el = _asm!!.createNonRootElement(br.name)// AsmElementSimple(br.name)
                    br.runtimeRule.rhs.items.forEachIndexed { index, rr ->
                        //TODO: leave out unnamed literals
                        if (rr.tag == "'${rr.value}'") {
                            // is unnamed literal don't use it as a property
                        } else {
                            val name = createPropertyName(rr)
                            val nname = if (el.hasProperty(name)) {
                                val i = count[name] ?: 2
                                count[name] = i + 1
                                name + i
                            } else {
                                name
                            }
                            val childsScope = createScope(scope, el) // could be same scope or a new one
                            val value = this.createValue(br.nonSkipChildren[index], childsScope)
                            this.setPropertyOrReference(el, nname, value)
                        }
                    }
                    if (br.runtimeRule.rhs.items.size == 1) {
                        if (br != br.tree?.root &&
                            br.runtimeRule.rhs.items[0].kind == RuntimeRuleKind.NON_TERMINAL
                            && br.runtimeRule.rhs.items[0].rhs.itemsKind == RuntimeRuleRhsItemsKind.LIST
                        ) {
                            el.properties[0].value
                        } else {
                            el
                        }
                    } else {
                        el
                    }
                }
                RuntimeRuleRhsItemsKind.LIST -> when (br.runtimeRule.rhs.listKind) {
                    RuntimeRuleListKind.MULTI -> {
                        val name = br.runtimeRule.rhs.items[RuntimeRuleItem.MULTI__ITEM].tag
                        val list = br.nonSkipChildren.mapNotNull { this.createValue(it, scope) } //TODO: childsScope
                        val value = if (br.runtimeRule.rhs.multiMax == 1) {
                            if (list.isEmpty()) null else list[0]
                        } else {
                            list
                        }
                        if (br == br.tree?.root) {
                            val el = _asm!!.createNonRootElement(br.name)
                            el.setProperty(name, value, false)
                            el
                        } else {
                            value
                        }
                    }
                    RuntimeRuleListKind.SEPARATED_LIST -> {
                        val name = br.runtimeRule.rhs.items[RuntimeRuleItem.SLIST__ITEM].tag
                        val list = br.nonSkipChildren.mapNotNull { this.createValue(it, scope) }//TODO: childsScope
                        if (br.runtimeRule.rhs.multiMax == 1) {
                            val value = if (list.isEmpty()) null else list[0]
                            value
                        } else {
                            list
                        }
                    }
                    else -> TODO()
                }
            }
            RuntimeRuleKind.GOAL -> this.createValue(br.children[0], scope)
            RuntimeRuleKind.EMBEDDED -> TODO()
        }
    }

    private fun createPropertyName(runtimeRule: RuntimeRule): String {
        //TODO: think we have to determine if rr is a pseudo rule or not here!
        return when (runtimeRule.kind) {
            RuntimeRuleKind.TERMINAL -> runtimeRule.tag
            RuntimeRuleKind.NON_TERMINAL -> when (runtimeRule.rhs.itemsKind) {
                RuntimeRuleRhsItemsKind.LIST -> when (runtimeRule.rhs.listKind) {
                    RuntimeRuleListKind.MULTI -> createPropertyName(runtimeRule.rhs.items[RuntimeRuleItem.MULTI__ITEM])
                    RuntimeRuleListKind.SEPARATED_LIST -> runtimeRule.tag
                    else -> TODO()
                }
                else -> runtimeRule.tag
            }
            RuntimeRuleKind.EMBEDDED -> runtimeRule.tag
            RuntimeRuleKind.GOAL -> runtimeRule.tag
        }
    }

    private fun isReferable(contextTypeName: String, el: AsmElementSimple): Boolean {
        return if (null == this.scopeDefinition) {
            false
        } else {
            val referables = this.scopeDefinition[contextTypeName]
            if (null == referables) {
                false
            } else {
                val prop = referables[el.typeName]
                if (null == prop) {
                    false
                } else {
                    true
                }
            }
        }
    }

    private fun getReferable(contextTypeName: String, el: AsmElementSimple): String? {
        return if (null == this.scopeDefinition) {
            null
        } else {
            val referables = this.scopeDefinition[contextTypeName]
            if (null == referables) {
                null
            } else {
                val prop = referables[el.typeName]
                if (null == prop) {
                    null
                } else {
                    el.getPropertyAsString(prop)
                }
            }
        }
    }

    private fun createScope(scope: ScopeSimple<AsmElementSimple>?, el: AsmElementSimple): ScopeSimple<AsmElementSimple>? {
        return if (null == scope) {
            null
        } else {
            val scopeName = scope.belongingTo?.typeName
            if (null != scopeName) {
                val referableName = getReferable(scopeName, el)
                if (null!=referableName) {
                    scope.addToScope(referableName, el)
                }
            }
            if (scopeDefinition?.containsKey(el.typeName) ?: false) {
                val newScope = scope.childScope(el)
                _scopes[el] = newScope
                newScope
            } else {
                scope
            }

        }
    }

    fun resolveReferences(): List<AnalyserIssue> {
        _issues.clear()
        _asm!!.rootElements.forEach {
            resolveReferencesElement(it)
        }
        return _issues
    }

    private fun resolveReferencesElement(el: AsmElementSimple) {
        el.properties.forEach {
            if (it.isReference) {

            } else {
                // no need to resolve
            }
        }
    }

    private fun isReference(el: AsmElementSimple, name: String): Boolean {
        return references?.containsKey(Pair(el.typeName, name)) ?: false
    }

    private fun setPropertyOrReference(el: AsmElementSimple, name: String, value: Any?) {
        val isRef = this.isReference(el, name) ?: false
        when {
            isRef -> el.setProperty(name, value, true)
            else -> el.setProperty(name, value, false)
        }

    }
}