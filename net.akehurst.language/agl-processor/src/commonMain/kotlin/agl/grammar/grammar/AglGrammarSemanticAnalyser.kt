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

package net.akehurst.language.agl.grammar.grammar

import net.akehurst.language.agl.runtime.structure.LookaheadSet
import net.akehurst.language.api.grammar.*
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyserException
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyserItem
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyserItemKind


class AglGrammarSemanticAnalyser(
) : SemanticAnalyser {

    private val items = mutableListOf<SemanticAnalyserItem>()

    override fun clear() {
        this.items.clear()
    }

    override fun <T> analyse(asm: T, locationMap: Map<Any, InputLocation>): List<SemanticAnalyserItem> {
        return when (asm) {
            is List<*> -> checkGrammar(asm as List<Grammar>, locationMap)
            else -> throw SemanticAnalyserException("This SemanticAnalyser is for an ASM of type List<Grammar>", null)
        }
    }

    fun checkGrammar(grammarList: List<Grammar>, locationMap: Map<Any, InputLocation>): List<SemanticAnalyserItem> {
        grammarList.forEach { grammar ->
            this.checkNonTerminalReferencesExist(grammar, locationMap)
            if (items.isEmpty()) {
                this.checkForAmbiguities(grammar, locationMap)
            }
        }
        return this.items
    }

    fun checkNonTerminalReferencesExist(grammar: Grammar, locationMap: Map<Any, InputLocation>) {
        grammar.rule.forEach {
            val rhs = it.rhs
            this.checkRuleItem(grammar, locationMap, rhs)
        }
    }

    fun checkRuleItem(grammar: Grammar, locationMap: Map<Any, InputLocation>, rhs: RuleItem) {
        when (rhs) {
            is EmptyRule -> {
            }
            is Terminal -> {
            }
            is NonTerminal -> {
                try {
                    rhs.referencedRule //will throw 'GrammarRuleNotFoundException' if rule not found
                } catch (e: GrammarRuleNotFoundException) {
                    val item = SemanticAnalyserItem(SemanticAnalyserItemKind.ERROR, locationMap[rhs], e.message!!)
                    this.items.add(item)
                }
            }
            is Concatenation -> {
                rhs.items.forEach { checkRuleItem(grammar, locationMap, it) }
            }
            is Choice -> {
                rhs.alternative.forEach { checkRuleItem(grammar, locationMap, it) }
            }
            is Group -> {
                rhs.choice.alternative.forEach { checkRuleItem(grammar, locationMap, it) }
            }
            is Multi -> {
                checkRuleItem(grammar, locationMap, rhs.item)
            }
            is SeparatedList -> {
                checkRuleItem(grammar, locationMap, rhs.item)
                checkRuleItem(grammar, locationMap, rhs.separator)
            }
        }
    }

    fun checkForAmbiguities(grammar: Grammar, locationMap: Map<Any, InputLocation>) {
        val itemsSet = mutableSetOf<SemanticAnalyserItem>()
        //TODO: find a way to reuse RuntimeRuleSet rather than re compute here
        val conv = ConverterToRuntimeRules(grammar)
        val rrs = conv.transform()
        //TODO: pass in goalRuleName
        val goalRuleName = grammar.rule.first { it.isSkip.not() }.name
        val automaton = rrs.automatonFor(goalRuleName)

        automaton.states.values.forEach {state ->
            val trans = state.allBuiltTransitions
            if (trans.size > 1) {
                trans.forEach { tr1 ->
                    trans.forEach {tr2 ->
                        //TODO: should we compare actions here? prob not
                        if (tr1 !== tr2 && tr1.action==tr2.action) {
                            val lhi = tr1.lookaheadGuard.content.intersect(tr2.lookaheadGuard.content)
                            if (lhi.isNotEmpty() || (tr1.lookaheadGuard.content.isEmpty() && tr2.lookaheadGuard.content.isEmpty())) {
                                val ori1 = conv.originalRuleItemFor(tr1.to.runtimeRule)
                                val ori2 = conv.originalRuleItemFor(tr2.to.runtimeRule)
                                val or1 = ori1.owningRule
                                val or2 = ori2.owningRule
                                val lhStr = lhi.map { it.tag }
                                val msg = "Ambiguity on $lhStr between ${or1.name} and ${or2.name}"
                                itemsSet.add(SemanticAnalyserItem(SemanticAnalyserItemKind.WARNING, locationMap[ori1], msg))
                                itemsSet.add(SemanticAnalyserItem(SemanticAnalyserItemKind.WARNING, locationMap[ori2], msg))
                            }
                        }
                    }
                }
            }
        }
        items.addAll(itemsSet)
    }
}