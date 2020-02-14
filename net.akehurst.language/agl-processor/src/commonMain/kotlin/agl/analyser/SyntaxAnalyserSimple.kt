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


class SyntaxAnalyserSimple : SyntaxAnalyser, SharedPackedParseTreeVisitor<Any?, Any?> {


    override fun clear() {
        TODO("not implemented")
    }

    override fun <T> transform(sppt: SharedPackedParseTree): T {
        return sppt.accept(this, null) as T
    }

    override fun visit(target: SharedPackedParseTree, arg: Any?): Any? {
        val root = target.root
        return root.accept(this, arg)
    }

    override fun visit(target: SPPTLeaf, arg: Any?): Any? {
        val leaf = target as SPPTLeafDefault
        return when {
            leaf.runtimeRule.isPattern -> target.matchedText
            else -> null
        }
    }

    override fun visit(target: SPPTBranch, arg: Any?): Any? {
        val br = target as SPPTBranchDefault //TODO: make write thing available on interface
        return when (br.runtimeRule.kind) {
            RuntimeRuleKind.TERMINAL -> throw SyntaxAnalyserException("Should not happen",null)
            RuntimeRuleKind.NON_TERMINAL -> when (br.runtimeRule.rhs.kind) {
                RuntimeRuleItemKind.MULTI -> this.multi(target,arg)
                RuntimeRuleItemKind.SEPARATED_LIST -> target.children.map { it.accept(this, arg) }
                RuntimeRuleItemKind.CHOICE -> br.children[0].accept(this, arg)
                RuntimeRuleItemKind.CONCATENATION -> createAstElement(target, arg)
                else -> throw SyntaxAnalyserException("Unsupported rhs type",null)
            }
            RuntimeRuleKind.GOAL -> br.children[0].accept(this, arg)
        }
    }

    fun createAstElement(target: SPPTBranch, arg: Any?) : AsmElementSimple {
        val asm = AsmElementSimple(target.name)
        target.nonSkipChildren.forEach {
            val name = when {
                it is SPPTBranchDefault -> {
                    when (it.runtimeRule.kind) {
                        RuntimeRuleKind.NON_TERMINAL -> when(it.runtimeRule.rhs.kind) {
                            RuntimeRuleItemKind.MULTI -> it.runtimeRule.rhs.items[RuntimeRuleItem.MULTI__ITEM].tag
                            else -> it.name
                        }
                        else -> it.name
                    }
                }
                else -> it.name
            }
            val value = it.accept(this,arg)
            if (null!=value) {

                asm.setProperty(name,value)
            }
        }
        return asm
    }

    fun multi(target: SPPTBranch, arg: Any?) : List<Any> {
        return target.nonSkipChildren.mapNotNull { it.accept(this, arg) }
    }
}