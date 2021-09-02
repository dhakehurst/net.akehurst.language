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

import net.akehurst.language.agl.sppt.SPPTBranchFromInputAndGrownChildren
import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.sppt.*
import net.akehurst.language.api.syntaxAnalyser.AsmElementSimple
import net.akehurst.language.api.syntaxAnalyser.AsmSimple
import net.akehurst.language.api.syntaxAnalyser.AsmSimpleBuilder
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser


class SyntaxAnalyserSimple : SyntaxAnalyser {

    private val _asm = AsmSimple()

    override val locationMap = mutableMapOf<Any, InputLocation>()

    override fun clear() {
        locationMap.clear()
    }

    override fun <T> transform(sppt: SharedPackedParseTree): T {
        val value = this.createValue(sppt.root)
        _asm.addRoot(value as AsmElementSimple)
        return _asm as T
    }

    private fun createValue(target: SPPTNode): Any? {
        return when (target) {
            is SPPTLeaf -> createValueFromLeaf(target)
            is SPPTBranch -> createValueFromBranch(target)
            else -> error("should never happen!")
        }
    }

    private fun createValueFromLeaf(target: SPPTLeaf): Any? {
        val leaf = target //as SPPTLeafDefault
        val value = when {
            leaf.isEmptyLeaf -> null
            else -> leaf.nonSkipMatchedText
        }
        return value
    }

    private fun createValueFromBranch(target: SPPTBranch): Any? {
        val br = target as SPPTBranchFromInputAndGrownChildren //SPPTBranchDefault //TODO: make write thing available on interface
        return when (br.runtimeRule.kind) {
            RuntimeRuleKind.TERMINAL -> error("should never happen!")
            RuntimeRuleKind.NON_TERMINAL -> when (br.runtimeRule.rhs.itemsKind) {
                RuntimeRuleRhsItemsKind.EMPTY -> TODO()
                RuntimeRuleRhsItemsKind.CHOICE -> {
                    val v = this.createValue(br.children[0])
                    v
                }
                RuntimeRuleRhsItemsKind.CONCATENATION -> {
                    val count = mutableMapOf<String, Int>()
                    var el = _asm.createNonRootElement(br.name)// AsmElementSimple(br.name)
                    br.runtimeRule.rhs.items.forEachIndexed { index, rr ->
                        val name = createPropertyName(rr)
                        val value = this.createValue(br.nonSkipChildren[index])
                        if (el.hasProperty(name)) {
                            val i = count[name] ?: 2
                            count[name] = i + 1
                            val nname = name + i
                            el.setProperty(nname, value)
                        } else {
                            el.setProperty(name, value)
                        }
                    }
                    if (br.runtimeRule.rhs.items.size == 1) {
                        if (br.runtimeRule.rhs.items[0].kind == RuntimeRuleKind.NON_TERMINAL
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
                        val list = br.nonSkipChildren.mapNotNull { this.createValue(it) }
                        if (br.runtimeRule.rhs.multiMax == 1) {
                            val value = if (list.isEmpty()) null else list[0]
                            value
                        } else {
                            list
                        }
                    }
                    RuntimeRuleListKind.SEPARATED_LIST -> {
                        val name = br.runtimeRule.rhs.items[RuntimeRuleItem.SLIST__ITEM].tag
                        val list = br.nonSkipChildren.mapNotNull { this.createValue(it) }
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
            RuntimeRuleKind.GOAL -> this.createValue(br.children[0])
            RuntimeRuleKind.EMBEDDED -> TODO()
        }
    }

    private fun createPropertyName(runtimeRule: RuntimeRule): String {
        return when (runtimeRule.kind) {
            RuntimeRuleKind.TERMINAL -> runtimeRule.tag
            RuntimeRuleKind.NON_TERMINAL -> when (runtimeRule.rhs.itemsKind) {
                RuntimeRuleRhsItemsKind.LIST -> when (runtimeRule.rhs.listKind) {
                    RuntimeRuleListKind.MULTI -> createPropertyName(runtimeRule.rhs.items[RuntimeRuleItem.MULTI__ITEM])
                    RuntimeRuleListKind.SEPARATED_LIST -> createPropertyName(runtimeRule.rhs.items[RuntimeRuleItem.SLIST__ITEM])
                    else -> TODO()
                }
                else -> runtimeRule.tag
            }
            RuntimeRuleKind.EMBEDDED -> runtimeRule.tag
            RuntimeRuleKind.GOAL -> runtimeRule.tag
        }
    }

}