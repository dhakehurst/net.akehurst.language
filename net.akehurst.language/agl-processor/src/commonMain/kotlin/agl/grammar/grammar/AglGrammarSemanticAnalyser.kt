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

import net.akehurst.language.api.analyser.SemanticAnalyser
import net.akehurst.language.api.analyser.SemanticAnalyserException
import net.akehurst.language.api.grammar.*
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.AutomatonKind
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.LanguageIssueKind
import net.akehurst.language.api.processor.LanguageProcessorPhase


internal class AglGrammarSemanticAnalyser(
) : SemanticAnalyser<List<Grammar>, GrammarContext> {

    private val items = mutableListOf<LanguageIssue>()
    private var _locationMap: Map<*, InputLocation>? = null

    override fun clear() {
        this.items.clear()
        _locationMap = null
    }

    override fun analyse(asm: List<Grammar>, locationMap: Map<*, InputLocation>?, context: GrammarContext?): List<LanguageIssue> {
        this._locationMap = locationMap ?: emptyMap<Any, InputLocation>()
        return when (asm) {
            is List<*> -> checkGrammar(asm, AutomatonKind.LOOKAHEAD_1) //TODO: how to check using user specified AutomatonKind ?
            else -> throw SemanticAnalyserException("This SemanticAnalyser is for an ASM of type List<Grammar>", null)
        }
    }

    private fun checkGrammar(grammarList: List<Grammar>, automatonKind: AutomatonKind): List<LanguageIssue> {
        grammarList.forEach { grammar ->
            this.checkNonTerminalReferencesExist(grammar)
            if (items.isEmpty()) {
                this.checkForAmbiguities(grammar, automatonKind)
            }
        }
        return this.items
    }

    private fun checkNonTerminalReferencesExist(grammar: Grammar) {
        grammar.rule.forEach {
            val rhs = it.rhs
            this.checkRuleItem(grammar, rhs)
        }
    }

    private fun checkRuleItem(grammar: Grammar, rhs: RuleItem) {
        when (rhs) {
            is EmptyRule -> {
            }
            is Terminal -> {
            }
            is NonTerminal -> {
                try {
                    rhs.referencedRule(grammar) //will throw 'GrammarRuleNotFoundException' if rule not found
                } catch (e: GrammarRuleNotFoundException) {
                    val item = LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS, _locationMap!![rhs], e.message!!)
                    this.items.add(item)
                }
            }
            is Concatenation -> {
                rhs.items.forEach { checkRuleItem(grammar, it) }
            }
            is Choice -> {
                rhs.alternative.forEach { checkRuleItem(grammar, it) }
            }
            is Group -> {
                rhs.choice.alternative.forEach { checkRuleItem(grammar, it) }
            }
            is SimpleList -> {
                checkRuleItem(grammar, rhs.item)
            }
            is SeparatedList -> {
                checkRuleItem(grammar, rhs.item)
                checkRuleItem(grammar, rhs.separator)
            }
        }
    }

    private fun checkForAmbiguities(grammar: Grammar, automatonKind: AutomatonKind) {
        val itemsSet = mutableSetOf<LanguageIssue>()
        //TODO: find a way to reuse RuntimeRuleSet rather than re compute here
        val conv = ConverterToRuntimeRules(grammar)
        val rrs = conv.runtimeRuleSet
        //TODO: pass in goalRuleName
        val goalRuleName = grammar.rule.first { it.isSkip.not() }.name
        val automaton = rrs.automatonFor(goalRuleName, automatonKind)

        automaton.allBuiltStates.forEach { state ->
            val trans = state.outTransitions.allBuiltTransitions
            if (trans.size > 1) {
                trans.forEach { tr1 ->
                    trans.forEach { tr2 ->
                        //TODO: should we compare actions here? prob not
                        if (tr1 !== tr2 && tr1.action == tr2.action) {
                            val lhi = tr1.lookaheadGuard.content.intersect(tr2.lookaheadGuard.content)
                            if (lhi.isNotEmpty() || (tr1.lookaheadGuard.content.isEmpty() && tr2.lookaheadGuard.content.isEmpty())) {
                                val ori1 = conv.originalRuleItemFor(tr1.to.runtimeRules.first().runtimeRuleSetNumber,tr1.to.runtimeRules.first().number) //FIXME
                                val ori2 = conv.originalRuleItemFor(tr2.to.runtimeRules.first().runtimeRuleSetNumber,tr2.to.runtimeRules.first().number) //FIXME
                                val or1 = ori1.owningRule
                                val or2 = ori2.owningRule
                                val lhStr = lhi.map { it.tag }
                                val msg = "Ambiguity on $lhStr between ${or1.name} and ${or2.name}"
                                itemsSet.add(LanguageIssue(LanguageIssueKind.WARNING, LanguageProcessorPhase.SEMANTIC_ANALYSIS, _locationMap!![ori1], msg))
                                itemsSet.add(LanguageIssue(LanguageIssueKind.WARNING, LanguageProcessorPhase.SEMANTIC_ANALYSIS, _locationMap!![ori2], msg))
                            }
                        }
                    }
                }
            }
        }
        items.addAll(itemsSet)
    }
}