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

import net.akehurst.language.agl.agl.grammar.scopes.ScopeModel
import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.agl.sppt.SPPTBranchFromInputAndGrownChildren
import net.akehurst.language.api.analyser.SyntaxAnalyser
import net.akehurst.language.api.asm.*
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.LanguageIssueKind
import net.akehurst.language.api.processor.LanguageProcessorPhase
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
    val scopeModel: ScopeModel = ScopeModel()
) : SyntaxAnalyser<AsmSimple, ContextSimple> {

    private var _asm: AsmSimple? = null
    private var _context: ContextSimple? = null // cached value, provided on call to transform
    private val _issues = mutableListOf<LanguageIssue>()

    override val locationMap = mutableMapOf<Any, InputLocation>()

    override fun clear() {
        this.locationMap.clear()
        this._asm = null
        this._context = null
        this._issues.clear()
    }

    override fun transform(sppt: SharedPackedParseTree, context: ContextSimple?): Pair<AsmSimple, List<LanguageIssue>> {
        this._context = context
        _asm = AsmSimple()
        val value = this.createValue(sppt.root, context?.scope)
        _asm?.addRoot(value as AsmElementSimple)

        _asm!!.rootElements.forEach {
            val iss = scopeModel.resolveReferencesElement(it, locationMap, context?.scope)
            this._issues.addAll(iss)
        }

        return Pair(_asm!!, _issues)
    }

    private fun createValue(target: SPPTNode, scope: ScopeSimple<AsmElementSimple>?): Any? {
        val v = when (target) {
            is SPPTLeaf -> createValueFromLeaf(target)
            is SPPTBranch -> createValueFromBranch(target, scope)
            else -> error("should never happen!")
        }
        if (v is AsmElementSimple) {
            locationMap[v] = target.location
            this.addToScope(scope, v)
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

    private fun getReferable(scopeFor: String, el: AsmElementSimple): String? {
        val prop = scopeModel.getReferablePropertyNameFor(scopeFor, el.typeName)
        return if (null == prop) {
            null
        } else {
            el.getPropertyAsString(prop)
        }
    }

    private fun createScope(scope: ScopeSimple<AsmElementSimple>?, el: AsmElementSimple): ScopeSimple<AsmElementSimple>? {
        return if (null == scope) {
            null
        } else {
            if (scopeModel.isScope(el.typeName)) {
                val newScope = scope.childScope(el.typeName)
                el.ownScope = newScope
                newScope
            } else {
                scope
            }
        }
    }

    private fun addToScope(scope: ScopeSimple<AsmElementSimple>?, el: AsmElementSimple) {
        if (null == scope) {
            //do nothing
        } else {
            val scopeName = scope.forTypeName
            val referableName = getReferable(scopeName, el)
            if (null != referableName) {
                scope.addToScope(referableName, el.typeName, el)
            }
        }
    }

    private fun isReference(el: AsmElementSimple, name: String): Boolean {
        return scopeModel.isReference(el.typeName, name)
    }

    private fun setPropertyOrReference(el: AsmElementSimple, name: String, value: Any?) {
        val isRef = this.isReference(el, name)
        when {
            isRef -> el.setProperty(name, value, true)
            else -> el.setProperty(name, value, false)
        }

    }
}

internal fun ScopeModel.resolveReferencesElement(el: AsmElementSimple, locationMap:Map<Any,InputLocation>, scope: ScopeSimple<AsmElementSimple>?) : List<LanguageIssue> {
    val scopeModel = this
    val issues = mutableListOf<LanguageIssue>()
    val elScope = el.ownScope ?: scope
    if (null != elScope) {
        el.properties.forEach { prop ->
            if (prop.isReference) {
                val v = prop.value
                if (null == v) {
                    //can't set reference, but no issue
                } else if (v is AsmElementReference) {
                    val typeNames = scopeModel.getReferredToTypeNameFor(el.typeName, prop.name)
                    val referreds = typeNames.mapNotNull { elScope.findOrNull(v.reference, it) }
                    if (1 < referreds.size) {
                        val location = locationMap[el] //TODO: should be property location
                        issues.add(
                            LanguageIssue(
                                LanguageIssueKind.WARNING,
                                LanguageProcessorPhase.SYNTAX_ANALYSIS,
                                location,
                                "Multiple options for '${v.reference}' as reference for '${el.typeName}.${prop.name}'"
                            )
                        )
                    }
                    val referred = referreds.firstOrNull()
                    if (null == referred) {
                        val location = locationMap[el] //TODO: should be property location
                        issues.add(
                            LanguageIssue(
                                LanguageIssueKind.ERROR,
                                LanguageProcessorPhase.SYNTAX_ANALYSIS,
                                location,
                                "Cannot find '${v.reference}' as reference for '${el.typeName}.${prop.name}'"
                            )
                        )
                    } else {
                        el.getPropertyAsReference(prop.name)?.value = referred
                    }

                } else {
                    val location = locationMap[el] //TODO: should be property location
                    issues.add(
                        LanguageIssue(
                            LanguageIssueKind.ERROR,
                            LanguageProcessorPhase.SYNTAX_ANALYSIS,
                            location,
                            "Cannot resolve reference property '${el.typeName}.${prop.name}' because it is not defined as a reference"
                        )
                    )
                }
            } else {
                // no need to resolve
            }
        }
        el.children.forEach {
            val chIss = resolveReferencesElement(it, locationMap, elScope)
            issues.addAll(chIss)
        }
    }
    return issues
}