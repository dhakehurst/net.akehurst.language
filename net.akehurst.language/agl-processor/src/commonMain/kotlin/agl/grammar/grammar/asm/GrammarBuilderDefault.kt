/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.grammar.grammar.asm

import net.akehurst.language.api.grammar.*

class GrammarBuilderDefault(val namespace: Namespace, val name: String) {

    val grammar = GrammarDefault(namespace, name)

    private val _terminals = mutableMapOf<String, Terminal>()
    private fun terminal(value: String, isPattern: Boolean): Terminal {
        val t = _terminals[value]
        return if (null == t) {
            val tt = TerminalDefault(value, isPattern)
            _terminals[value] = tt
            tt.grammar = this.grammar
            tt
        } else {
            if (isPattern == t.isPattern) {
                t
            } else {
                error("Error terminal defined as both pattern and literal!")
            }
        }
    }

    fun rule(name: String): RuleBuilder {
        return RuleBuilder(GrammarRuleDefault(name, false, false, false).also {
            it.grammar = this.grammar
            this.grammar.grammarRule.add(it)
        })
    }

    fun skip(name: String, isLeaf: Boolean = false): RuleBuilder {
        return RuleBuilder(GrammarRuleDefault(name, false, true, isLeaf).also {
            it.grammar = this.grammar
            this.grammar.grammarRule.add(it)
        })
    }

    fun leaf(name: String): RuleBuilder {
        return RuleBuilder(GrammarRuleDefault(name, false, false, true).also {
            it.grammar = this.grammar
            this.grammar.grammarRule.add(it)
        })
    }

    fun terminalLiteral(value: String): Terminal {
        return terminal(value, false)
    }

    fun terminalPattern(value: String): Terminal {
        return terminal(value, true)
    }

    fun embed(embeddedGrammarName: String, embeddedGoalName: String): Embedded {
        val qn = embeddedGrammarName
        val embeddedGrammarRef = GrammarReferenceDefault(namespace, embeddedGrammarName)
        return EmbeddedDefault(embeddedGoalName, embeddedGrammarRef)
    }

    fun nonTerminal(name: String): NonTerminal {
        if (name.contains(".")) {
            TODO("Not supported")
        } else {
            return NonTerminalDefault(name)
        }
    }

    fun concatenation(vararg sequence: ConcatenationItem): Concatenation {
        return ConcatenationDefault(sequence.toList())
    }

    class RuleBuilder(val rule: GrammarRule) {

        fun empty() {
            this.rule.rhs = EmptyRuleDefault()
        }

        fun concatenation(vararg sequence: ConcatenationItem) {
            this.rule.rhs = ConcatenationDefault(sequence.toList())
        }

        fun choiceLongestFromConcatenation(vararg alternative: Concatenation) {
            this.rule.rhs = ChoiceLongestDefault(alternative.asList());
        }

        fun choiceLongestFromConcatenationItem(vararg alternative: ConcatenationItem) {
            val alternativeConcats = alternative.map { ConcatenationDefault(listOf(it)) }
//            this.rule.rhs = ChoiceLongestDefault(alternativeConcats);
            this.rule.rhs = ChoiceLongestDefault(alternative.asList());
        }

        fun choicePriority(vararg alternative: Concatenation) {
            //val alternativeConcats = alternative.map { ChoicePriorityDefault(listOf(it)) }
            this.rule.rhs = ChoicePriorityDefault(alternative.asList());
        }

        fun optional(item: SimpleItem) {
            this.rule.rhs = OptionalItemDefault(item)
        }

        fun multi(min: Int, max: Int, item: SimpleItem) {
            this.rule.rhs = SimpleListDefault(min, max, item)
        }

        fun separatedList(min: Int, max: Int, separator: SimpleItem, item: SimpleItem) {
            this.rule.rhs = SeparatedListDefault(min, max, item, separator)
        }
    }
}
