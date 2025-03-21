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

package net.akehurst.language.agl.runtime.structure

import net.akehurst.language.automaton.api.Automaton
import net.akehurst.language.automaton.api.AutomatonKind
import net.akehurst.language.automaton.leftcorner.ParserStateSet
import net.akehurst.language.collections.lazyMutableMapNonNull
import net.akehurst.language.parser.api.PrefRule
import net.akehurst.language.parser.api.RuleSet
import kotlin.collections.component1
import kotlin.collections.component2

class RuntimeRuleSet(
    val number: Int,
    val qualifiedName: String, //QualifiedName,
    val runtimeRules: List<RuntimeRule>,
    val precedenceRules: List<PrefRule>
) : RuleSet {

    companion object {
        var nextRuntimeRuleSetNumber = 0

        // val numberForGrammar = lazyMutableMapNonNull<Grammar, Int> { nextRuntimeRuleSetNumber++ }
        /** Grammar.qualifiedName -> number **/
        val numberForGrammar = lazyMutableMapNonNull<String, Int> { nextRuntimeRuleSetNumber++ }

        const val NO_RRS = -1

        const val GOAL_RULE_NUMBER = -1
        const val EOT_RULE_NUMBER = -2
        const val SKIP_RULE_NUMBER = -3
        const val SKIP_CHOICE_RULE_NUMBER = -4
        const val RUNTIME_LOOKAHEAD_RULE_NUMBER = -6
        const val ANY_LOOKAHEAD_RULE_NUMBER = -7
        const val UNDEFINED_LOOKAHEAD_RULE_NUMBER = -8
        const val EMPTY_RULE_NUMBER = -9
        const val EMPTY_LIST_RULE_NUMBER = -10

        const val END_OF_TEXT_TAG = "<EOT>"
        const val GOAL_TAG = "<GOAL>"
        const val SKIP_MULTI_TAG = "<SKIP-MULTI>"
        const val SKIP_CHOICE_RULE_TAG = "<SKIP-CHOICE>"
        const val RUNTIME_LOOKAHEAD_RULE_TAG = "<RT>"
        const val ANY_LOOKAHEAD_RULE_TAG = "<ANY>"
        const val UNDEFINED_LOOKAHEAD_RULE_TAG = "<UNDEFINED>"
        const val EMPTY_RULE_TAG = "<EMPTY>"
        const val EMPTY_LIST_RULE_TAG = "<EMPTY_LIST>"

        val END_OF_TEXT = RuntimeRule(NO_RRS, EOT_RULE_NUMBER, END_OF_TEXT_TAG, false, false)
            .also { it.setRhs(RuntimeRuleRhsCommonTerminal(it)) }
        val USE_RUNTIME_LOOKAHEAD = RuntimeRule(NO_RRS, RUNTIME_LOOKAHEAD_RULE_NUMBER, RUNTIME_LOOKAHEAD_RULE_TAG, false, false)
            .also { it.setRhs(RuntimeRuleRhsCommonTerminal(it)) }
        val ANY_LOOKAHEAD = RuntimeRule(NO_RRS, ANY_LOOKAHEAD_RULE_NUMBER, ANY_LOOKAHEAD_RULE_TAG, false, false)
            .also { it.setRhs(RuntimeRuleRhsCommonTerminal(it)) }
        val UNDEFINED_RULE = RuntimeRule(NO_RRS, UNDEFINED_LOOKAHEAD_RULE_NUMBER, UNDEFINED_LOOKAHEAD_RULE_TAG, false, false)
            .also { it.setRhs(RuntimeRuleRhsCommonTerminal(it)) }
        val EMPTY = RuntimeRule(NO_RRS, EMPTY_RULE_NUMBER, EMPTY_RULE_TAG, false, false)
            .also { it.setRhs(RuntimeRuleRhsEmpty(it)) }
        val EMPTY_LIST = RuntimeRule(NO_RRS, EMPTY_LIST_RULE_NUMBER, EMPTY_LIST_RULE_TAG, false, false)
            .also { it.setRhs(RuntimeRuleRhsEmptyList(it)) }
    }

    val goalRuleFor = lazyMutableMapNonNull<RuntimeRule, RuntimeRule> {
        val ug = it //this.findRuntimeRule(it)
        val gr = RuntimeRule(this.number, GOAL_RULE_NUMBER, GOAL_TAG, false, false)
        gr.setRhs(RuntimeRuleRhsGoal(gr, ug))
        gr
    }

    // Mutable list used, so that 'setRules' can set it
    //val runtimeRules: List<RuntimeRule> = mutableListOf()

    val skipRules: List<RuntimeRule> by lazy { this.runtimeRules.filter { it.isSkip } }

    val skipTerminals: Set<RuntimeRule> by lazy { this.skipParserStateSet?.usedTerminalRules ?: emptySet() }

    val nonSkipRules: Array<RuntimeRule> by lazy { this.runtimeRules.filter { it.isSkip.not() }.toTypedArray() }

    // used if scanning (excluding skip)
    override val nonSkipTerminals: List<RuntimeRule> by lazy {
        this.runtimeRules.flatMap {
            when {
                it.isEmbedded -> (it.rhs as RuntimeRuleRhsEmbedded).embeddedRuntimeRuleSet.nonSkipTerminals.toList()
                it.isTerminal && it.isSkip.not() -> listOf(it)
                else -> emptyList()
            }
        }
    }

    // used if scanning (including skip)
    override val terminals: List<RuntimeRule> by lazy {
        this.terminalByTag.values.toList()
        //this.runtimeRules.flatMap {
        //    when {
        //        it.isEmbedded -> (it.rhs as RuntimeRuleRhsEmbedded).embeddedRuntimeRuleSet.terminals.toList()
        //        it.isTerminal -> listOf(it)
         //       else -> emptyList()
        //    }
        //}
    }

    override val embeddedTerminals: List<RuntimeRule> by lazy {
        //TODO("What about recursive !")
        findEmbeddedRuleSets().flatMap {
            it.terminals
        }.toSet().toList()
    }

    /*
        val firstTerminals: Array<Set<RuntimeRule>> by lazy {
            this.runtimeRules.map { this.calcFirstTerminals(it) }
                .toTypedArray()
        }

        // used when calculating lookahead
        val expectedTerminalRulePositions = lazyMap<RulePosition, Array<RulePosition>> {
            calcExpectedTerminalRulePositions(it).toTypedArray()
        }
    */
    /*
    // used when calculating lookahead
    val firstTerminals2 = lazyMutableMapNonNull<RulePosition, List<RuntimeRule>> {
        val trps = expectedTerminalRulePositions[it] ?: arrayOf()
        trps.flatMap { it.items }.toSet().toList()
    }
*/

    private val rulesByTag by lazy {
        runtimeRules.associate { Pair(it.tag, it) }
    }
    private val nonTerminalRuleNumber by lazy {
        rulesByTag.filter { (k, v) -> v.isNonTerminal }.entries.associate {(k, v) ->  Pair(k, v.ruleNumber) }
    }
    private val terminalByTag:Map<String, RuntimeRule> by lazy {
        rulesByTag.filter { (k, v) -> v.isTerminal }
    }
    private val embeddedRuleNumber by lazy {
        rulesByTag.filter { (k, v) -> v.isEmbedded }.entries.associate {(k, v) ->  Pair(k, v.ruleNumber) }
    }

    // userGoalRule -> ParserStateSet
    private val states_cache = mutableMapOf<String, ParserStateSet>()
    private val skipStateSet = mutableMapOf<RuntimeRule, ParserStateSet>()

    /**
     * <SKIP-MULTI> = <SKIP-CHOICE>+
     * <SKIP-CHOICE> = SR-0 | ... | SR-n
     */
    internal val skipParserStateSet: ParserStateSet? by lazy {
        if (skipRules.isEmpty()) {
            null
        } else {
            val skipChoiceRule = RuntimeRule(this.number, SKIP_CHOICE_RULE_NUMBER, SKIP_CHOICE_RULE_TAG, false, false).also {
                val options = skipRules.map { skpRl ->
                    RuntimeRuleRhsConcatenation(it, listOf(skpRl))
                }
                val rhs = RuntimeRuleRhsChoice(it, RuntimeRuleChoiceKind.LONGEST_PRIORITY, options)
                it.setRhs(rhs)
            }
            val skipMultiRule = RuntimeRule(this.number, SKIP_RULE_NUMBER, SKIP_MULTI_TAG, false, false)
                .also { it.setRhs(RuntimeRuleRhsListSimple(it, 1, -1, skipChoiceRule)) }

            //TODO: how to set AutomatonKind here!
            val ss = ParserStateSet(nextStateSetNumber++, this, skipMultiRule, true, AutomatonKind.LOOKAHEAD_1, false)
            ss
        }
    }

    /*
    //called from ParserStateSet, which adds the Goal GrammarRule bits
    internal val parentPosition = lazyMutableMapNonNull<RuntimeRule, Set<RulePosition>> { childRR ->
        //TODO: this is slow, is there a better way?
        this.runtimeRules.flatMap { rr ->
            val rps = rr.rulePositions
            val f = rps.filter { rp ->
                rp.items.contains(childRR)
            }
            f
        }.toSet()
    }
*/
    internal var nextStateSetNumber = 0

    override fun automatonFor(goalRuleName: String, automatonKind: AutomatonKind): Automaton {
        this.buildFor(goalRuleName, automatonKind)
        return this.states_cache[goalRuleName]!! //findRuntimeRule would throw exception if not exist
    }

    override fun usedAutomatonFor(goalRuleName: String): Automaton {
        return this.states_cache[goalRuleName]!!
    }

    /*
        internal fun createAllSkipStates() {
            this.skipRules.forEach { skipRule ->
                val stateSet = ParserStateSet(nextStateSetNumber++, this, skipRule, emptySet(), true)
                this.skipStateSet[skipRule] = stateSet
                val startSet = skipRule.rulePositions.map { rp ->
                    stateSet.states[rp]
                    RulePositionWithLookahead(rp, emptySet())
                }.toSet()
                startSet.transitiveClosure { parent ->
                    val parentRP = parent.rulePosition
                    parentRP.items.flatMap { rr ->
                        rr.rulePositions.mapNotNull { childRP ->
                            val childRPEnd = RulePosition(childRP.runtimeRule, childRP.option, RulePosition.END_OF_RULE)
                            //val elh = this.calcLookahead(parent, childRPEnd, parent.lookahead)
                            val childEndState = stateSet.states[childRPEnd] // create state!
                            val lh = this.calcLookahead(parent, childRP, parent.lookahead)
                            //childEndState.addParentRelation(ParentRelation(parentRP, elh))
                            RulePositionWithLookahead(childRP, lh)
                            //TODO: this seems different to the other closures!
                        }
                    }.toSet()
                }
            }
        }

        fun fetchSkipStates(rulePosition: RulePosition): ParserState {
            return this.skipStateSet.values.mapNotNull { it.fetchOrNull(rulePosition) }.first() //TODO: maybe more than 1 !
        }
    */
    /*
    internal fun calcLookahead(parent: RulePositionWithLookahead?, childRP: RulePosition, ifEmpty: Set<RuntimeRule>): Set<RuntimeRule> {
        return when (childRP.runtimeRule.kind) {
            RuntimeRuleKind.TERMINAL -> useParentLH(parent, ifEmpty)
            RuntimeRuleKind.EMBEDDED -> useParentLH(parent, ifEmpty)
            //val rr = childRP.runtimeRule
            //rr.embeddedRuntimeRuleSet!!.firstTerminals[rr.embeddedStartRule!!.number]
            //}
            RuntimeRuleKind.GOAL -> when (childRP.position) {
                0 -> childRP.runtimeRule.rhs.items.drop(1).toSet()
                else -> emptySet()
            }

            RuntimeRuleKind.NON_TERMINAL -> {
                when {
                    childRP.isAtEnd -> useParentLH(parent, ifEmpty)
                    else -> {
                        //this childRP will not itself be applied to Height or GRAFT,
                        // however it should carry the FIRST of next in the child,
                        // so that this childs children can use it if needed
                        childRP.items.flatMap { fstChildItem ->
                            val nextRPs = childRP.next() //nextRulePosition(childRP, fstChildItem)
                            nextRPs.flatMap { nextChildRP ->
                                if (nextChildRP.isAtEnd) {
                                    if (null == parent) {
                                        ifEmpty
                                    } else {
                                        calcLookahead(null, parent.rulePosition, parent.lookahead)
                                    }
                                } else {
                                    val lh: List<RuntimeRule> = this.firstTerminals2[nextChildRP]
                                    if (lh.isEmpty()) {
                                        error("should never happen")
                                    } else {
                                        lh
                                    }
                                }
                            }
                        }.toSet()
                    }
                }
            }
        }
    }

    private fun useParentLH(parent: RulePositionWithLookahead?, ifEmpty: Set<RuntimeRule>): Set<RuntimeRule> {
        return if (null == parent) {
            ifEmpty
        } else {
            if (parent.isAtEnd) {
                parent.lookahead
            } else {
                val nextRPs = parent.rulePosition.next()//nextRulePosition(parent.rulePosition, childRP.runtimeRule)
                nextRPs.flatMap { nextRP ->
                    if (nextRP.isAtEnd) {
                        calcLookahead(null, parent.rulePosition, parent.lookahead)
                    } else {
                        val lh: List<RuntimeRule> = this.firstTerminals2[nextRP]
                        if (lh.isEmpty()) {
                            error("should never happen")
                        } else {
                            lh
                        }
                    }
                }.toSet()
            }
        }
    }
*/
    fun buildFor(userGoalRuleName: String, automatonKind: AutomatonKind): ParserStateSet {
        val ss = this.fetchStateSetFor(userGoalRuleName, automatonKind)
        return ss.build()
    }

    override fun addPreBuiltFor(userGoalRuleName: String, automaton: Automaton) {
        this.states_cache[userGoalRuleName] = automaton as ParserStateSet
    }

    fun fetchStateSetFor(userGoalRule: RuntimeRule, automatonKind: AutomatonKind): ParserStateSet =
        fetchStateSetFor(userGoalRule.tag, automatonKind)

    internal fun fetchStateSetFor(userGoalRuleName: String, automatonKind: AutomatonKind): ParserStateSet {
        //TODO: need to cache by possibleEndOfText also
        var stateSet = this.states_cache[userGoalRuleName]
        if (null == stateSet) {
            stateSet = ParserStateSet(nextStateSetNumber++, this, this.findRuntimeRule(userGoalRuleName), false, automatonKind, false)
            this.states_cache[userGoalRuleName] = stateSet
        }
        return stateSet
    }

    fun findEmbeddedRuleSets(found:Set<RuntimeRuleSet> = emptySet()) : Set<RuntimeRuleSet> {
        return when {
            found.contains(this) -> found
            else -> {
                val res = runtimeRules.filter{ it.isEmbedded }.flatMap {
                    val es = (it.rhs as RuntimeRuleRhsEmbedded).embeddedRuntimeRuleSet
                    val r = es.findEmbeddedRuleSets(found+this)
                    r + es
                }.toSet()
                res
            }
        }
    }

    fun findRuntimeRule(tag: String): RuntimeRule {
        return this.rulesByTag[tag] ?: error("Internal Error: RuntimeRule '${tag}' not found")
    }

    fun findTerminalRule(tag: String): RuntimeRule {
        return this.terminalByTag[tag]
            ?: error("Terminal RuntimeRule ${tag} not found")
    }

    internal fun precedenceRulesFor(precedenceContext: RuntimeRule): RuntimePreferenceRule? =
        this.precedenceRules.firstOrNull {
            (it as RuntimePreferenceRule).contextRule == precedenceContext
        } as RuntimePreferenceRule?

    /*
    // used when calculating lookahead ?
    private fun calcExpectedItemRulePositionTransitive(rp: RulePosition): Set<RulePosition> {
        val s = setOf(rp)//rp.runtimeRule.calcExpectedRulePositions(rp.position)

        return s.transitiveClosure { rp ->
            if (RulePosition.END_OF_RULE == rp.position) {
                emptySet()
            } else {
                when (rp.runtimeRule.kind) {
                    RuntimeRuleKind.TERMINAL -> emptySet<RulePosition>()
                    RuntimeRuleKind.GOAL,
                    RuntimeRuleKind.NON_TERMINAL -> {
                        val item = rp.runtimeRule.item(rp.option, rp.position) ?: TODO()
                        when (item.kind) {
                            RuntimeRuleKind.GOAL -> TODO()
                            RuntimeRuleKind.TERMINAL -> setOf(rp)
                            RuntimeRuleKind.NON_TERMINAL -> item.calcExpectedRulePositions(0)
                            RuntimeRuleKind.EMBEDDED -> {
                                val embeddedStartRp = RulePosition(item.embeddedStartRule!!, 0, RulePosition.START_OF_RULE)
                                item.embeddedRuntimeRuleSet!!.expectedTerminalRulePositions[embeddedStartRp]!!.toSet()
                            }
                        }
                    }

                    RuntimeRuleKind.EMBEDDED -> TODO()
                }
            }.toSet()
        }
    }

    private fun calcExpectedTerminalRulePositions(rp: RulePosition): Set<RulePosition> {
        val nextItems = this.calcExpectedItemRulePositionTransitive(rp)
        return nextItems.filter {
            when (it.runtimeRule.kind) {
                RuntimeRuleKind.TERMINAL -> false
                else -> {
                    if (RulePosition.END_OF_RULE == it.position) {
                        false
                    } else {
                        it.items.any { it.kind == RuntimeRuleKind.TERMINAL }
                    }
                }
            }
        }.toSet() //TODO: cache ?
    }


      private fun calcFirstSubRules(runtimeRule: RuntimeRule): Set<RuntimeRule> {
          return runtimeRule.findSubRulesAt(0)
      }

      private fun calcFirstTerminals(runtimeRule: RuntimeRule): Set<RuntimeRule> {
          var rr = runtimeRule.findTerminalAt(0)
          for (r in this.calcFirstSubRules(runtimeRule)) {
              rr += r.findTerminalAt(0)
          }
          return rr
      }
  */
    fun usedAutomatonToString(userGoalRuleName: String, withStates: Boolean = false) =
        this.states_cache[userGoalRuleName]!!.usedAutomatonToString(withStates)

    internal fun fullAutomatonToString(goalRuleName: String, automatonKind: AutomatonKind): String {
        this.buildFor(goalRuleName, automatonKind)
        return this.usedAutomatonToString("S")
    }

    fun calcUsedRules(
        rule: RuntimeRule,
        used: MutableSet<RuntimeRule> = mutableSetOf(),
        done: BooleanArray = BooleanArray(this.runtimeRules.size)
    ): Set<RuntimeRule> {
        return when {
            rule.isGoal -> {
                used.add(rule)
                for (sr in rule.rhsItems.flatten()) {
                    calcUsedRules(sr as RuntimeRule, used, done)
                }
                used
            }

            rule.ruleNumber >= 0 && done[rule.ruleNumber] -> used
            else -> when {
                rule.isNonTerminal -> {
                    used.add(rule)
                    if (rule.ruleNumber >= 0) done[rule.ruleNumber] = true
                    for (sr in rule.rhsItems.flatten()) {
                        calcUsedRules(sr as RuntimeRule, used, done)
                    }
                    used
                }

                else -> {
                    used.add(rule)
                    if (rule.ruleNumber > 0) done[rule.ruleNumber] = true
                    used
                }
            }
        }
    }

    // only used in test
    fun clone(): RuntimeRuleSet {
        val cloneNumber = nextRuntimeRuleSetNumber++
        val clonedRules = this.runtimeRules.associate { rr ->
            val cr = RuntimeRule(cloneNumber, rr.ruleNumber, rr.name, rr.isSkip, rr.isPseudo)
            Pair(rr.tag, cr)
        }
        this.runtimeRules.forEach {
            val cr = clonedRules[it.tag] ?: error("Internal Error: cannot find cloned rule with tag '${it.tag}' ")
            cr.setRhs(it.rhs.clone(clonedRules))
        }
        val rules = clonedRules.values.toList()
        val clonedPrecedenceRules = this.precedenceRules.map {
            val clonedCtx = clonedRules[it.contextRule.tag]!!
            val clonedPrecRules = (it as RuntimePreferenceRule).options.map { pr ->
                val cTgt = pr.spine.map { clonedRules[it.tag]!! }
                val cOp = pr.operators.map { clonedRules[it.tag]!! }.toSet()
                RuntimePreferenceRule.RuntimePreferenceOption(pr.precedence, cTgt, pr.option, cOp, pr.associativity)
            }
            RuntimePreferenceRule(clonedCtx, clonedPrecRules)
        }
        val clone = RuntimeRuleSet(cloneNumber, qualifiedName, rules, clonedPrecedenceRules)
        return clone
    }

    override fun toString(): String {
        val rulesStr = this.runtimeRules
            .sortedBy { it.tag }
            .map {
                "  " + it.asString
            }.joinToString("\n")
        val precRules = this.precedenceRules
            .sortedBy { it.contextRule.tag }
            .joinToString(separator = "\n") {
                val opts = it.options.joinToString(separator = "\n") {
                    "    ${it.precedence} ${it.associativity} ${it.spine.joinToString(separator = "<-") { it.tag }} ${it.option} ${it.operators}"
                }
                "  PREC ${it.contextRule.tag} {\n${opts}\n  }"
            }
        val s = """
RuntimeRuleSet '$qualifiedName' {
${rulesStr}
${precRules}
}
""".trimIndent()
        return s
    }

    override fun hashCode(): Int = number
    override fun equals(other: Any?): Boolean = when {
        other is RuntimeRuleSet -> this.number == other.number
        else -> false
    }
}