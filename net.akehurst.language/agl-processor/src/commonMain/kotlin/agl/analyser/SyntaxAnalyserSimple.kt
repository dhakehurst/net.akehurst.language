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

package net.akehurst.language.agl.analyser

import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleItem
import net.akehurst.language.agl.runtime.structure.RuntimeRuleItemKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleKind
import net.akehurst.language.api.analyser.AsmElementSimple
import net.akehurst.language.api.analyser.SyntaxAnalyser
import net.akehurst.language.api.analyser.SyntaxAnalyserException
import net.akehurst.language.api.sppt.SPPTBranch
import net.akehurst.language.api.sppt.SPPTLeaf
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.api.sppt.SharedPackedParseTreeVisitor
import net.akehurst.language.parser.sppt.SPPTBranchDefault
import net.akehurst.language.parser.sppt.SPPTLeafDefault


class SyntaxAnalyserSimple : SyntaxAnalyser, SharedPackedParseTreeVisitor<AsmElementSimple, Any?> {


    override fun clear() {
        TODO("not implemented")
    }

    override fun <T> transform(sppt: SharedPackedParseTree): T {
        return sppt.accept(this, null) as T
    }

    override fun visit(target: SharedPackedParseTree, arg: Any?): AsmElementSimple {
        val root = target.root
        return root.accept(this, arg)
    }

    override fun visit(target: SPPTLeaf, arg: Any?): AsmElementSimple {
        val leaf = target as SPPTLeafDefault
        val value = when {
            leaf.isEmptyLeaf -> null
            else -> leaf.nonSkipMatchedText
        }
        val el = AsmElementSimple(leaf.name)
        el.setProperty(leaf.name, value)
        return el

    }

    override fun visit(target: SPPTBranch, arg: Any?): AsmElementSimple {
        val br = target as SPPTBranchDefault //TODO: make write thing available on interface
        var el = AsmElementSimple(br.name)
        when (br.runtimeRule.kind) {
            RuntimeRuleKind.TERMINAL -> throw SyntaxAnalyserException("Should not happen", null)
            RuntimeRuleKind.NON_TERMINAL -> when (br.runtimeRule.rhs.kind) {
                RuntimeRuleItemKind.MULTI -> {
                    val name = br.runtimeRule.rhs.items[RuntimeRuleItem.MULTI__ITEM].tag
                    val list = br.nonSkipChildren.map { it.accept(this, arg) }
                    val value = list.mapNotNull { it.properties[0].value } // there should be only one property
                    if (br.runtimeRule.rhs.multiMax == 1) {
                        val v = if (value.isEmpty()) null else value[0]
                        el.setProperty(name, v)
                    } else {
                        el.setProperty(name, value)
                    }
                }
                RuntimeRuleItemKind.SEPARATED_LIST -> {
                    val name = br.runtimeRule.rhs.items[RuntimeRuleItem.SLIST__ITEM].tag
                    val list = br.nonSkipChildren.map { it.accept(this, arg) }
                    val value = list.map {
                        if (it.properties.size == 1)
                            it.properties[0].value
                        else
                            it
                    } // there should be only one property
                    el.setProperty(name, value)
                }
                RuntimeRuleItemKind.CHOICE -> {
                    val v = br.children[0].accept(this, arg)
                    val name = createPropertyName(br.runtimeRule)
                    val allChoicesPrimitive = br.runtimeRule.rhs.items.all { it.kind==RuntimeRuleKind.TERMINAL }
                   if (allChoicesPrimitive) {
                       el = v
                   } else {
                       el.setProperty(name, v)
                   }

                }
                RuntimeRuleItemKind.CONCATENATION -> {
                    val count = mutableMapOf<String, Int>()
                    br.runtimeRule.rhs.items.forEachIndexed { index, rr ->
                        val name = createPropertyName(rr)
                        val value = br.nonSkipChildren[index].accept(this, arg)
                        val vvalue = if (1 == value.properties.size) {
                            value.properties[0].value
                        } else {
                            value
                        }
                        if (el.hasProperty(name)) {
                            val i = count[name] ?: 2
                            count[name] = i + 1
                            val nname = name + i
                            el.setProperty(nname, vvalue)
                        } else {
                            el.setProperty(name, vvalue)
                        }
                    }
                }
                else -> throw SyntaxAnalyserException("Unsupported rhs type", null)
            }
            RuntimeRuleKind.GOAL -> br.children[0].accept(this, arg)
            RuntimeRuleKind.EMBEDDED -> TODO()
        }
        return el
    }

    fun createPropertyName(runtimeRule: RuntimeRule): String {
        return when (runtimeRule.kind) {
            RuntimeRuleKind.TERMINAL -> runtimeRule.tag
            RuntimeRuleKind.NON_TERMINAL -> when (runtimeRule.rhs.kind) {
                RuntimeRuleItemKind.MULTI -> createPropertyName(runtimeRule.rhs.items[RuntimeRuleItem.MULTI__ITEM])
                RuntimeRuleItemKind.SEPARATED_LIST -> createPropertyName(runtimeRule.rhs.items[RuntimeRuleItem.SLIST__ITEM])
                else -> runtimeRule.tag
            }
            RuntimeRuleKind.EMBEDDED -> runtimeRule.tag
            RuntimeRuleKind.GOAL -> runtimeRule.tag
        }
    }

    fun createAstElement(target: SPPTBranch, arg: Any?): AsmElementSimple {
        val asm = AsmElementSimple(target.name)
        target.nonSkipChildren.forEach {
            val name = when {
                it is SPPTBranchDefault -> {
                    when (it.runtimeRule.kind) {
                        RuntimeRuleKind.NON_TERMINAL -> when (it.runtimeRule.rhs.kind) {
                            RuntimeRuleItemKind.MULTI -> it.runtimeRule.rhs.items[RuntimeRuleItem.MULTI__ITEM].tag
                            else -> it.name
                        }
                        else -> it.name
                    }
                }
                else -> it.name
            }
            val value = it.accept(this, arg)
            if (null != value) {

                asm.setProperty(name, value)
            }
        }
        return asm
    }

    fun multi(target: SPPTBranch, arg: Any?): List<Any> {
        return target.nonSkipChildren.mapNotNull { it.accept(this, arg) }
    }
}