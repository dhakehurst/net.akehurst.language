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

package net.akehurst.language.grammar.processor

import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.agl.processor.SemanticAnalysisResultDefault
import net.akehurst.language.automaton.api.ParseAction
import net.akehurst.language.grammar.api.*
import net.akehurst.language.automaton.api.AutomatonKind
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.api.processor.SemanticAnalysisOptions
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.automaton.leftcorner.ParserStateSet
import net.akehurst.language.parser.api.InputLocation


class AglGrammarSemanticAnalyser() : SemanticAnalyser<GrammarModel, ContextFromGrammarRegistry> {

    companion object {
        private const val ns = "net.akehurst.language.agl.grammar.grammar"
        const val OPTIONS_KEY_AMBIGUITY_ANALYSIS = "$ns.ambiguity.analysis"
    }

    private val issues = IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS)
    private var _locationMap: Map<*, InputLocation>? = null
    private var _analyseAmbiguities = true

    // Grammar -> Set<Rules>, used rules have an entry in the set
    private val _unusedRules = mutableMapOf<Grammar, MutableSet<GrammarRule>>()

    private fun issueWarn(item: Any?, message: String, data: Any?) {
        val location = this._locationMap?.get(item)
        issues.warn(location, message, data)
    }

    private fun issueError(item: Any, message: String, data: Any?) {
        val location = this._locationMap?.get(item)
        issues.error(location, message, data)
    }

    override fun clear() {
        _unusedRules.clear()
        this.issues.clear()
        _locationMap = null
    }

    override fun analyse(
        asm: GrammarModel,
        locationMap: Map<Any, InputLocation>?,
        context: ContextFromGrammarRegistry?,
        options: SemanticAnalysisOptions<GrammarModel, ContextFromGrammarRegistry>
    ): SemanticAnalysisResult {
        this._locationMap = locationMap ?: emptyMap<Any, InputLocation>()
        this._analyseAmbiguities = options.other[OPTIONS_KEY_AMBIGUITY_ANALYSIS] as Boolean? ?: false

        if (null == context) {
            issueWarn(null, "No ContextFromGrammarRegistry supplied, grammar references cannot be resolved", null)
        }

        asm.namespace.forEach { ns ->
            ns.definition.forEach {
                context?.grammarRegistry?.registerGrammar(it)
            }
        }

        checkGrammar(context, asm, AutomatonKind.LOOKAHEAD_1) //TODO: how to check using user specified AutomatonKind ?
        return SemanticAnalysisResultDefault(issues)
    }

    private fun checkGrammar(context: ContextFromGrammarRegistry?, grammarList: GrammarModel, automatonKind: AutomatonKind) {
        grammarList.namespace.forEach { ns ->
            ns.definition.forEach { grammar ->
                this.resolveGrammarRefs(context, grammar)
                this.analyseGrammar(context, grammar)
                this.checkRuleUsage(grammar)
                this.checkForDuplicates(grammar)
                if (issues.errors.isEmpty() && _analyseAmbiguities) {
                    this.checkForAmbiguities(grammar, automatonKind)
                }
            }
        }
    }

    private fun resolveGrammarRefs(context: ContextFromGrammarRegistry?, grammar: Grammar) {
        checkGrammarExistsAndResolve(context, grammar.extends)
        checkGrammarExistsAndResolve(context, grammar.allGrammarReferencesInRules)
    }

    private fun checkGrammarExistsAndResolve(context: ContextFromGrammarRegistry?, refs: List<GrammarReference>) {
        for (it in refs) {
            checkGrammarExistsAndResolve(context, it)
        }
    }

    private fun checkGrammarExistsAndResolve(context: ContextFromGrammarRegistry?, ref: GrammarReference) {
        val g = context?.grammarRegistry?.findGrammarOrNull(ref.localNamespace, ref.nameOrQName)
        if (null == g) {
            this.issueError(ref, "Grammar '${ref.nameOrQName}' not found", null)
        } else {
            ref.resolveAs(g)
        }
    }

    private fun checkRuleUsage(grammar: Grammar) {
        _unusedRules[grammar]!!.forEach {
            when {
                it is OverrideRule -> Unit // don't report it may be overriding something used in a base rule, TODO: check this
                else -> issueWarn(it, "Rule '${it.name}' is not used in grammar ${grammar.name}.", null)
            }
        }
    }

    private fun recordUnusedRule(grammar: Grammar, rule: GrammarRule) {
        val set = this._unusedRules[grammar]!!
        set.add(rule)
    }

    private fun analyseGrammar(context: ContextFromGrammarRegistry?, grammar: Grammar) {
        _unusedRules[grammar] = mutableSetOf()
        // default usage is unused for all rules in this grammar
        grammar.grammarRule.forEach {
            when {
                it.isSkip -> Unit // skip skip rules
                else -> recordUnusedRule(grammar, it)
            }
        }
        // first rule is default goal rule //TODO: adjust for 'option defaultGoalRule'
        this._unusedRules[grammar]!!.remove(grammar.grammarRule.first { it.isSkip.not() })

        grammar.grammarRule.forEach {
            when (it) {
                is NormalRule -> this.analyseRuleItem(context, grammar, it.rhs)
                is OverrideRule -> {
                    //need to check what is overridden exists, before analysing the rule
                    val overridden = grammar.findAllSuperGrammarRule(it.name)
                    when {
                        overridden.isEmpty() -> issueError(
                            it,
                            "Rule '${it.name}' is defined as override, but no rule with same name is found in extended grammars of '${grammar.name}'",
                            null
                        )

                        else -> this.analyseRuleItem(context, grammar, it.rhs)
                    }
                }
            }
        }
    }

    private fun checkForDuplicates(grammar: Grammar) {
        val rules1 = mutableMapOf<GrammarRuleName, MutableList<GrammarRule>>() // must be a list for cheking in same grammar because rules would have the same id
        grammar.grammarRule.forEach {
            val list = rules1[it.name]
            if (null == list) {
                rules1[it.name] = mutableListOf(it)
            } else {
                list.add(it)
            }
        }
        rules1.values.forEach {
            when {
                1 < it.size -> {
                    val r = it.last() //can't get location because id is the same
                    issueError(r, "More than one rule named '${r.name}' found in grammar '${grammar.name}'", null)
                }

                else -> Unit //OK
            }
        }

        val rules2 = mutableMapOf<GrammarRuleName, MutableSet<GrammarRule>>()
        grammar.allInheritedResolvedGrammarRule.forEach {
            val set = rules2[it.name]
            if (null == set) {
                rules2[it.name] = mutableSetOf(it)
            } else {
                set.add(it)
            }
        }

        grammar.grammarRule.forEach {
            val set = rules2[it.name]
            if (null == set) {
                rules2[it.name] = mutableSetOf(it)
            } else {
                set.add(it)
            }
        }
        rules2.values.forEach {
            when {
                1 < it.size -> {
                    val or = it.firstOrNull { it is OverrideRule && grammar === it.grammar }
                    when (or) {
                        null -> {
                            it.forEach {
                                issueError(it, "More than one rule named '${it.name}' found in grammar '${grammar.name}'", null)
                            }
                        }

                        else -> Unit // local override
                    }
                }

                else -> Unit // OK
            }
        }
    }

    private fun analyseRuleItem(context: ContextFromGrammarRegistry?, grammar: Grammar, rhs: RuleItem) {
        when (rhs) {
            is EmptyRule -> Unit
            is Terminal -> Unit
            is Embedded -> checkGrammarExistsAndResolve(context, rhs.embeddedGrammarReference)
            is Concatenation -> rhs.items.forEach { analyseRuleItem(context, grammar, it) }
            is Choice -> rhs.alternative.forEach { analyseRuleItem(context, grammar, it) }
            is Group -> analyseRuleItem(context, grammar, rhs.groupedContent)
            is OptionalItem -> analyseRuleItem(context, grammar, rhs.item)
            is SimpleList -> analyseRuleItem(context, grammar, rhs.item)
            is SeparatedList -> {
                analyseRuleItem(context, grammar, rhs.item)
                analyseRuleItem(context, grammar, rhs.separator)
            }

            is NonTerminal -> {
                rhs.targetGrammar?.let { checkGrammarExistsAndResolve(context, it) }
                val rule = when {
                    null == rhs.targetGrammar -> grammar.findAllResolvedGrammarRule(rhs.ruleReference)
                    else -> rhs.targetGrammar?.resolved?.findAllResolvedGrammarRule(rhs.ruleReference)
                }
                //    .toSet() //convert result to set so that same rule from same grammar is not repeated
                when {
                    null == rule -> {
                        issueError(rhs, "GrammarRule '${rhs.ruleReference}' not found in grammar '${grammar.name}'", null)
                    }

                    else -> {
                        this._unusedRules[grammar]!!.remove(rule)
                    }
                }
            }
        }
    }

    private fun checkForAmbiguities(grammar: Grammar, automatonKind: AutomatonKind) {
        //val itemsSet = mutableSetOf<LanguageIssue>()
        //TODO: find a way to reuse RuntimeRuleSet rather than re compute here
        val conv = ConverterToRuntimeRules(grammar)
        val rrs = conv.runtimeRuleSet
        //TODO: pass in goalRuleName
        val goalRuleName = grammar.defaultGoalRule.name.value
        //TODO: optionally do this...as it builds the automaton..we don't always want to build it!
        // and if built want to  reuse the build
        val automaton = rrs.automatonFor(goalRuleName, automatonKind)

        (automaton as ParserStateSet).allBuiltStates.forEach { state ->
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
                                    val lhg1 = tr1.lookahead.map { it.guard.part }.reduce { acc, it -> acc.intersect(it) }
                                    val lhg2 = tr2.lookahead.map { it.guard.part }.reduce { acc, it -> acc.intersect(it) }
                                    val lhi = lhg1.intersect(lhg2)
                                    if (lhi.isNotEmpty) {
                                        val frOr = conv.originalRuleItemFor(tr1.from.runtimeRules.first().runtimeRuleSetNumber, tr1.from.runtimeRules.first().ruleNumber)
                                        val ori1 = conv.originalRuleItemFor(tr1.to.runtimeRules.first().runtimeRuleSetNumber, tr1.to.runtimeRules.first().ruleNumber) //FIXME
                                        val ori2 = conv.originalRuleItemFor(tr2.to.runtimeRules.first().runtimeRuleSetNumber, tr2.to.runtimeRules.first().ruleNumber) //FIXME
                                        val orF = frOr?.owningRule
                                        val or1 = ori1?.owningRule
                                        val or2 = ori2?.owningRule
                                        val lhStr = lhi.fullContent.map { it.tag }
                                        val msg = "Ambiguity: [${tr1.action}/${tr2.action}] conflict from '${orF?.name}' into '${or1?.name}/${or2?.name}' on $lhStr"
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