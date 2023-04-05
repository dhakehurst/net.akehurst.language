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

import net.akehurst.language.api.automaton.ParseAction
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.agl.processor.SemanticAnalysisResultDefault
import net.akehurst.language.api.analyser.SemanticAnalyser
import net.akehurst.language.api.grammar.*
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.*
import net.akehurst.language.api.processor.GrammarRegistry


class AglGrammarSemanticAnalyser(
    val languageRegistry: GrammarRegistry
) : SemanticAnalyser<List<Grammar>, GrammarContext> {

    companion object {
        private const val ns = "net.akehurst.language.agl.grammar.grammar"
        const val OPTIONS_KEY_AMBIGUITY_ANALYSIS = "$ns.ambiguity.analysis"
    }

    private val issues = IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS)
    private var _locationMap: Map<*, InputLocation>? = null
    private var _analyseAmbiguities = true

    private fun issueWarn(item: Any, message: String, data: Any?) {
        val location = this._locationMap?.get(item)
        issues.warn(location, message, data)
    }

    private fun issueError(item: Any, message: String, data: Any?) {
        val location = this._locationMap?.get(item)
        issues.error(location, message, data)
    }

    override fun clear() {
        this.issues.clear()
        _locationMap = null
    }

    override fun configure(configurationContext: SentenceContext<GrammarItem>, configuration: Map<String, Any>): List<LanguageIssue> {
        //TODO
        return emptyList()
    }

    override fun analyse(asm: List<Grammar>, locationMap: Map<Any, InputLocation>?, context: GrammarContext?, options:Map<String,Any>): SemanticAnalysisResult {
        this._locationMap = locationMap ?: emptyMap<Any, InputLocation>()
        this._analyseAmbiguities = options[OPTIONS_KEY_AMBIGUITY_ANALYSIS] as Boolean? ?: true

        asm.forEach { languageRegistry.register(it) }

        checkGrammar(asm, AutomatonKind.LOOKAHEAD_1) //TODO: how to check using user specified AutomatonKind ?
        return SemanticAnalysisResultDefault(issues)
    }

    private fun checkGrammar(grammarList: List<Grammar>, automatonKind: AutomatonKind) {
        grammarList.forEach { grammar ->
            this.checkExtendsExist(grammar.extends)
            this.checkNonTerminalReferencesExist(grammar)
            if (issues.errors.isEmpty() && _analyseAmbiguities) {
                this.checkForAmbiguities(grammar, automatonKind)
            }
        }
    }

    private fun checkExtendsExist(refs: List<GrammarReference>) {
        for (it in refs) {
            checkGrammarExists(it)
        }
    }

    private fun checkGrammarExists(ref: GrammarReference) {
        val g = languageRegistry.findGrammarOrNull(ref.localNamespace, ref.nameOrQName)
        if (null == g) {
            this.issueError(ref, "Grammar '${ref.nameOrQName}' not found", null)
        } else {
            ref.resolveAs(g)
        }
    }

    private fun checkNonTerminalReferencesExist(grammar: Grammar) {
        grammar.grammarRule.forEach {
            val rhs = it.rhs
            this.checkRuleItem(grammar, rhs)
        }
    }

    private fun checkRuleItem(grammar: Grammar, rhs: RuleItem) {
        when (rhs) {
            is EmptyRule -> Unit
            is Terminal -> Unit
            is Embedded -> checkGrammarExists(rhs.embeddedGrammarReference)
            is NonTerminal -> {
                val all = grammar.findAllNonTerminalRule(rhs.name)
                when {
                    all.isEmpty() -> issueError(rhs, "GrammarRule '${rhs.name}' not found in grammar '${grammar.name}'", null)
                    all.size > 1 -> issueError(rhs, "More than one rule named '${rhs.name}' in grammar '${grammar.name}', have you remembered the 'override' modifier", null)
                    else -> Unit //resolved
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
        //val itemsSet = mutableSetOf<LanguageIssue>()
        //TODO: find a way to reuse RuntimeRuleSet rather than re compute here
        val conv = ConverterToRuntimeRules(grammar)
        val rrs = conv.runtimeRuleSet
        //TODO: pass in goalRuleName
        val goalRuleName = grammar.grammarRule.first { it.isSkip.not() }.name
        //TODO: optionally do this...as it builds the automaton..we don't always want to build it!
        // and if built want to  reuse the build
        val automaton = rrs.automatonFor(goalRuleName, automatonKind)

        automaton.allBuiltStates.forEach { state ->
            val trans = state.outTransitions.allBuiltTransitions
            if (trans.size > 1) {
                trans.forEach { tr1 ->
                    val same = trans.filter { tr2 ->
                        when (tr2.action) {
                            ParseAction.WIDTH,
                            ParseAction.EMBED -> tr1.lookahead == tr2.lookahead && tr1.to == tr2.to && tr1.context.intersect(tr2.context).isNotEmpty()

                            ParseAction.GOAL,
                            ParseAction.HEIGHT,
                            ParseAction.GRAFT -> tr1.lookahead == tr2.lookahead && tr1.context.intersect(tr2.context).isNotEmpty()
                        }
                    }
                    same.forEach { tr2 ->
                        //TODO: should we compare actions here? prob not
                        if (tr1 !== tr2) {
                            when {
                                //(tr1.action == Transition.ParseAction.WIDTH && tr2.action == Transition.ParseAction.WIDTH && tr1.to != tr2.to) -> Unit // no error
                                else -> {
                                    val lhg1 = tr1.lookahead.map { it.guard.part }.reduce{ acc,it -> acc.intersect(it) }
                                    val lhg2 = tr2.lookahead.map { it.guard.part }.reduce{ acc,it -> acc.intersect(it) }
                                    val lhi = lhg1.intersect(lhg2)
                                    if (lhi.isNotEmpty) {
                                        val ori1 = conv.originalRuleItemFor(tr1.to.runtimeRules.first().runtimeRuleSetNumber, tr1.to.runtimeRules.first().ruleNumber) //FIXME
                                        val ori2 = conv.originalRuleItemFor(tr2.to.runtimeRules.first().runtimeRuleSetNumber, tr2.to.runtimeRules.first().ruleNumber) //FIXME
                                        //val or1 = ori1.owningRule
                                        val or2 = ori2.owningRule
                                        val lhStr = lhi.fullContent.map { it.tag }
                                        val msg = "Ambiguity on $lhStr with ${or2.name}"
                                        issueWarn(ori1, msg, null)
                                        //issueWarn(ori2, msg, null)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                //nothing to check
            }
        }
    }
}