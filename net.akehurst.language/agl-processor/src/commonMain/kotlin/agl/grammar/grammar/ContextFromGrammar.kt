/**
 * Copyright (C) 2021 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.grammar.grammar

import net.akehurst.language.agl.syntaxAnalyser.ContextSimple
import net.akehurst.language.agl.syntaxAnalyser.ScopeSimple
import net.akehurst.language.agl.syntaxAnalyser.TypeModelFromGrammar
import net.akehurst.language.agl.syntaxAnalyser.TypeModelFromGrammarConfiguration
import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.grammar.GrammarItem
import net.akehurst.language.api.processor.SentenceContext

// used by other languages that reference rules  in a grammar

class ContextFromGrammar(
) : SentenceContext<String> {
    constructor(grammar: Grammar) : this(listOf(grammar))
    constructor(grammars: List<Grammar>) : this() { createScopeFrom(grammars) }

    companion object {
        const val GRAMMAR_RULE_CONTEXT_TYPE_NAME = "GrammarRule"
        const val GRAMMAR_TERMINAL_CONTEXT_TYPE_NAME = "Terminal"
    }

    override var rootScope = ScopeSimple<String>(null, "", "")

    fun clear() {
        this.rootScope = ScopeSimple<String>(null, "", "")
    }

    fun createScopeFrom(grammars: List<Grammar>) {
        val scope = ScopeSimple<String>(null, "", grammars.last().name)
        grammars.forEach { g ->
            g.allResolvedGrammarRule.forEach {
                scope.addToScope(it.name, GRAMMAR_RULE_CONTEXT_TYPE_NAME, it.name)
            }
            g.allResolvedTerminal.forEach {
                scope.addToScope(it.name, GRAMMAR_TERMINAL_CONTEXT_TYPE_NAME, it.name)
            }
        }
        this.rootScope = scope
    }

    override fun hashCode(): Int = rootScope.hashCode()

    override fun equals(other: Any?): Boolean = when {
        other !is ContextFromGrammar -> false
        this.rootScope != other.rootScope -> false
        else -> true
    }
    override fun toString(): String = "ContextFromGrammar"
}