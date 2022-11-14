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

import net.akehurst.language.agl.agl.automaton.FirstOf
import net.akehurst.language.agl.api.runtime.Rule
import net.akehurst.language.agl.api.runtime.RuleSet
import net.akehurst.language.agl.automaton.ParserStateSet
import net.akehurst.language.agl.parser.InputFromString
import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.parser.ParserException
import net.akehurst.language.api.processor.AutomatonKind
import net.akehurst.language.collections.lazyMap
import net.akehurst.language.collections.lazyMutableMapNonNull
import net.akehurst.language.collections.transitiveClosure

internal class RuntimeRuleSet(
    val number: Int
) : RuleSet {

    companion object {
        var nextRuntimeRuleSetNumber = 0

        val numberForGrammar = lazyMutableMapNonNull<Grammar, Int> { nextRuntimeRuleSetNumber++ }

        val GOAL_RULE_NUMBER = -1
        val EOT_RULE_NUMBER = -2
        val SKIP_RULE_NUMBER = -3
        val SKIP_CHOICE_RULE_NUMBER = -4

        //val USE_PARENT_LOOKAHEAD_RULE_NUMBER = -5
        val USE_RUNTIME_LOOKAHEAD_RULE_NUMBER = -6
        val ANY_LOOKAHEAD_RULE_NUMBER = -7
        val UNKNOWN_LOOKAHEAD_RULE_NUMBER = -8
        val END_OF_TEXT_TAG = "<EOT>"
        val GOAL_TAG = "<GOAL>"
        val SKIP_RULE_TAG = "<SKIP-MULTI>"
        val SKIP_CHOICE_RULE_TAG = "<SKIP-CHOICE>"
        val USE_PARENT_LOOKAHEAD_RULE_TAG = "<UP>"
        val USE_RUNTIME_LOOKAHEAD_RULE_TAG = "<RT>"
        val ANY_LOOKAHEAD_RULE_TAG = "<ANY>"
        val UNKNOWN_LOOKAHEAD_RULE_TAG = "<UNKNOWN>"

        val END_OF_TEXT = RuntimeRule(EOT_RULE_NUMBER, 0, END_OF_TEXT_TAG, false, RuntimeRuleRhsCommonTerminal())
        val USE_RUNTIME_LOOKAHEAD = RuntimeRule(USE_RUNTIME_LOOKAHEAD_RULE_NUMBER, 0, USE_RUNTIME_LOOKAHEAD_RULE_TAG, false, RuntimeRuleRhsCommonTerminal())
        val ANY_LOOKAHEAD = RuntimeRule(ANY_LOOKAHEAD_RULE_NUMBER, 0, ANY_LOOKAHEAD_RULE_TAG, false, RuntimeRuleRhsCommonTerminal())
        val UNKNOWN_RULE = RuntimeRule(UNKNOWN_LOOKAHEAD_RULE_NUMBER, 0, UNKNOWN_LOOKAHEAD_RULE_TAG, false, RuntimeRuleRhsCommonTerminal())

        private fun createGoalRule(userGoalRule: RuntimeRule): RuntimeRule {
            val gr = RuntimeRule(GOAL_RULE_NUMBER, 0, GOAL_TAG, false, RuntimeRuleRhsGoal(userGoalRule.runtimeRuleSet, userGoalRule.ruleNumber))
            return gr
        }

    }

    private val nonTerminalRuleNumber: MutableMap<String, Int> = mutableMapOf()
    private val terminalRuleNumber: MutableMap<String, Int> = mutableMapOf()
    private val embeddedRuleNumber: MutableMap<String, Int> = mutableMapOf()

    override val goalRuleFor = lazyMutableMapNonNull<String, Rule> {
        val ug = this.findRuntimeRule(it)
        createGoalRule(ug)
    }

    var runtimeRules: List<RuntimeRule> = emptyList()

    val skipRules: List<RuntimeRule> by lazy { this.runtimeRules.filter { it.isSkip } }

    val nonSkipRules: Array<RuntimeRule> by lazy { this.runtimeRules.filter { it.isSkip.not() }.toTypedArray() }
    // used if scanning (excluding skip)
    val nonSkipTerminals: List<RuntimeRule> by lazy { this.runtimeRules.filter {it.isTerminal && it.isSkip.not() } }

    // used to add to lookahead
    val firstSkipTerminals: List<RuntimeRule> by lazy {
        FirstOf(skipParserStateSet).
        this.skipRules.flatMap {
            firstTerminals[it.ruleNumber]
        }
    }

    // used if scanning (including skip)
    val terminalRules: List<RuntimeRule> by lazy {
        this.runtimeRules.flatMap {
            when {
                it.isEmbedded -> (it.rhs as RuntimeRuleRhsEmbedded).embeddedRuntimeRuleSet.terminalRules.toList()
                it.isTerminal -> listOf(it)
                else -> emptyList()
            }
        }
    }

    val firstTerminals: Array<Set<RuntimeRule>> by lazy {
        this.runtimeRules.map { this.calcFirstTerminals(it) }
            .toTypedArray()
    }

    // used when calculating lookahead
    val expectedTerminalRulePositions = lazyMap<RuleOptionPosition, Array<RuleOptionPosition>> {
        calcExpectedTerminalRulePositions(it).toTypedArray()
    }

    // used when calculating lookahead
    val firstTerminals2 = lazyMutableMapNonNull<RuleOptionPosition, List<RuntimeRule>> {
        val trps = expectedTerminalRulePositions[it] ?: arrayOf()
        trps.flatMap { it.items }.toSet().toList()
    }

    // userGoalRule -> ParserStateSet
    private val states_cache = mutableMapOf<String, ParserStateSet>()
    private val skipStateSet = mutableMapOf<RuntimeRule, ParserStateSet>()

    internal val skipParserStateSet: ParserStateSet? by lazy {
        if (skipRules.isEmpty()) {
            null
        } else {
            val skipChoiceRule =
                RuntimeRule(this.number, SKIP_CHOICE_RULE_NUMBER, SKIP_CHOICE_RULE_TAG, SKIP_CHOICE_RULE_TAG, RuntimeRuleKind.NON_TERMINAL, false, true, null, null)
            skipChoiceRule.rhsOpt = RuntimeRuleRhs(RuntimeRuleRhsItemsKind.CHOICE, RuntimeRuleChoiceKind.LONGEST_PRIORITY, RuntimeRuleListKind.NONE, -1, 0, skipRules)
            val skipMultiRule = RuntimeRule(this.number, SKIP_RULE_NUMBER, SKIP_RULE_TAG, SKIP_RULE_TAG, RuntimeRuleKind.NON_TERMINAL, false, true, null, null)
            skipMultiRule.rhsOpt = RuntimeRuleRhs(RuntimeRuleRhsItemsKind.LIST, RuntimeRuleChoiceKind.NONE, RuntimeRuleListKind.MULTI, 1, -1, arrayOf(skipChoiceRule))

            //val skipGoalRule = RuntimeRule(this.number, SKIP_RULE_NUMBER, SKIP_RULE_TAG, "", RuntimeRuleKind.NON_TERMINAL, false, true, null, null)
            //skipGoalRule.rhsOpt = RuntimeRuleRhs(RuntimeRuleRhsItemsKind.CHOICE, RuntimeRuleChoiceKind.LONGEST_PRIORITY, -1, 0, skipRules)

            //TODO: how to set AutomatonKind here!
            val ss = ParserStateSet(nextStateSetNumber++, this, skipMultiRule, true, AutomatonKind.LOOKAHEAD_1)
            //this.states_cache[skipGoalRule] = ss
            ss
        }
    }

    //called from ParserStateSet, which adds the Goal GrammarRule bits
    internal val parentPosition = lazyMutableMapNonNull<RuntimeRule, Set<RuleOptionPosition>> { childRR ->
        //TODO: this is slow, is there a better way?
        this.runtimeRules.flatMap { rr ->
            val rps = rr.rulePositions
            val f = rps.filter { rp ->
                rp.items.contains(childRR)
            }
            f
        }.toSet()
    }

    private var nextStateSetNumber = 0

    fun setRules(rules: List<RuntimeRule>) {
        for (rr in rules) {
            when (rr.kind) {
                RuntimeRuleKind.GOAL -> { /*do nothing*/
                }

                RuntimeRuleKind.TERMINAL -> this.terminalRuleNumber[rr.tag] = rr.ruleNumber
                RuntimeRuleKind.NON_TERMINAL -> this.nonTerminalRuleNumber[rr.tag] = rr.ruleNumber
                RuntimeRuleKind.EMBEDDED -> this.embeddedRuleNumber[rr.tag] = rr.ruleNumber
            }
        }
        this.runtimeRules = rules.sortedBy { it.ruleNumber }
    }

    internal fun automatonFor(goalRuleName: String, automatonKind: AutomatonKind): ParserStateSet {
        this.buildFor(goalRuleName, automatonKind)
        return this.states_cache[goalRuleName]!! //findRuntimeRule would throw exception if not exist
    }

    internal fun usedAutomatonFor(goalRuleName: String): ParserStateSet {
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
                            val childRPEnd = RuleOptionPosition(childRP.runtimeRule, childRP.option, RuleOptionPosition.END_OF_RULE)
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

        fun fetchSkipStates(rulePosition: RuleOptionPosition): ParserState {
            return this.skipStateSet.values.mapNotNull { it.fetchOrNull(rulePosition) }.first() //TODO: maybe more than 1 !
        }
    */
    internal fun calcLookahead(parent: RulePositionWithLookahead?, childRP: RuleOptionPosition, ifEmpty: Set<RuntimeRule>): Set<RuntimeRule> {
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

    internal fun buildFor(userGoalRuleName: String, automatonKind: AutomatonKind): ParserStateSet {
        val s0 = this.fetchStateSetFor(userGoalRuleName, automatonKind).startState
        return s0.stateSet.build()
    }

    override fun fetchStateSetFor(userGoalRuleName: String, automatonKind: AutomatonKind): ParserStateSet {
        //TODO: need to cache by possibleEndOfText also
        var stateSet = this.states_cache[userGoalRuleName]
        if (null == stateSet) {
            stateSet = ParserStateSet(nextStateSetNumber++, this, this.goalRuleFor[userGoalRuleName] as RuntimeRule, false, automatonKind)
            this.states_cache[userGoalRuleName] = stateSet
        }
        return stateSet
    }

    // ---

    override fun findRuntimeRule(tag: String): RuntimeRule {
        val number = this.nonTerminalRuleNumber[tag]
            ?: this.terminalRuleNumber[tag]
            ?: this.embeddedRuleNumber[tag]
            ?: throw ParserException("RuntimeRule '${tag}' not found")
        return this.runtimeRules[number]
    }

    fun findTerminalRule(tag: String): RuntimeRule {
        val number = this.terminalRuleNumber[tag]
            ?: throw ParserException("Terminal RuntimeRule ${tag} not found")
        return this.runtimeRules[number]
    }

    // used when calculating lookahead ?
    private fun calcExpectedItemRulePositionTransitive(rp: RuleOptionPosition): Set<RuleOptionPosition> {
        val s = setOf(rp)//rp.runtimeRule.calcExpectedRulePositions(rp.position)

        return s.transitiveClosure { rp ->
            if (RuleOptionPosition.END_OF_RULE == rp.position) {
                emptySet()
            } else {
                when (rp.runtimeRule.kind) {
                    RuntimeRuleKind.TERMINAL -> emptySet<RuleOptionPosition>()
                    RuntimeRuleKind.GOAL,
                    RuntimeRuleKind.NON_TERMINAL -> {
                        val item = rp.runtimeRule.item(rp.option, rp.position) ?: TODO()
                        when (item.kind) {
                            RuntimeRuleKind.GOAL -> TODO()
                            RuntimeRuleKind.TERMINAL -> setOf(rp)
                            RuntimeRuleKind.NON_TERMINAL -> item.calcExpectedRulePositions(0)
                            RuntimeRuleKind.EMBEDDED -> {
                                val embeddedStartRp = RuleOptionPosition(item.embeddedStartRule!!, 0, RuleOptionPosition.START_OF_RULE)
                                item.embeddedRuntimeRuleSet!!.expectedTerminalRulePositions[embeddedStartRp]!!.toSet()
                            }
                        }
                    }

                    RuntimeRuleKind.EMBEDDED -> TODO()
                }
            }.toSet()
        }
    }

    private fun calcExpectedTerminalRulePositions(rp: RuleOptionPosition): Set<RuleOptionPosition> {
        val nextItems = this.calcExpectedItemRulePositionTransitive(rp)
        return nextItems.filter {
            when (it.runtimeRule.kind) {
                RuntimeRuleKind.TERMINAL -> false
                else -> {
                    if (RuleOptionPosition.END_OF_RULE == it.position) {
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

    override fun usedAutomatonToString(userGoalRuleName: String, withStates: Boolean): String {
        val b = StringBuilder()
        val states = this.states_cache[userGoalRuleName]!!.allBuiltStates
        val transitions = states.flatMap { it.outTransitions.allBuiltTransitions }

        b.append("States: ${states.size}  Transitions: ${transitions.size} ")
        b.append("\n")

        if (withStates) {
            states.forEach {
                val str = "$it {${it.outTransitions.allPrevious.map { it.number.value }}}"
                b.append(str).append("\n")
            }
        }

        transitions.sortedBy { it.from.rulePositions.toString() }.sortedBy { it.to.rulePositions.toString() }
            .forEach { tr ->
                val prev = tr.from.outTransitions.previousFor(tr)
                    .map { it.number.value } //transitionsByPrevious.entries.filter { it.value?.contains(tr) ?: false }.map { it.key?.number?.value }
                    .sorted()
                val frStr = "${tr.from.number.value}:${tr.from.rulePositions}"
                val toStr = "${tr.to.number.value}:${tr.to.rulePositions}"
                val trStr = "$frStr --> $toStr"
                val lh = tr.lookahead.joinToString(separator = "|") { "[${it.guard.fullContent.joinToString { it.tag }}](${it.up.fullContent.joinToString { it.tag }})" }
                b.append(trStr)
                b.append(" ${tr.action} ")
                b.append(lh)
                b.append(" {${prev.joinToString()}} ")
                b.append("\n")
            }

        return b.toString()
    }

    internal fun fullAutomatonToString(goalRuleName: String, automatonKind: AutomatonKind): String {
        this.buildFor(goalRuleName, automatonKind)
        return this.usedAutomatonToString("S")
    }

    // only used in test
    internal fun clone(): RuntimeRuleSet {
        val clone = RuntimeRuleSet(nextRuntimeRuleSetNumber++)
        val clonedRules = this.runtimeRules.map { rr ->
            val clonedEmbeddedRuntimeRuleSet = rr.embeddedRuntimeRuleSet?.clone()
            val clonedEmbeddedStartRule = rr.embeddedStartRule?.let { clonedEmbeddedRuntimeRuleSet?.runtimeRules?.get(it.ruleNumber) }
            RuntimeRule(
                clone.number,
                rr.ruleNumber,
                rr.name,
                rr.value,
                rr.kind,
                rr.isPattern,
                rr.isSkip,
                clonedEmbeddedRuntimeRuleSet,
                clonedEmbeddedStartRule
            )
        }
        clone.setRules(clonedRules)
        this.runtimeRules.forEach { rr ->
            val clonedRuntimeRule = clonedRules.get(rr.ruleNumber)
            when (rr.kind) {
                RuntimeRuleKind.NON_TERMINAL -> {
                    val clonedItems = rr.rhs.items.map { clonedRules[it.ruleNumber] }.toTypedArray()
                    clonedRuntimeRule.rhsOpt = rr.rhsOpt?.let { RuntimeRuleRhs(it.itemsKind, it.choiceKind, it.listKind, it.multiMin, it.multiMax, clonedItems) }
                }

                RuntimeRuleKind.TERMINAL -> {
                    if (rr.isEmptyRule) {
                        val clonedRuleThatIsEmpty = clonedRules.get(rr.rhs.EMPTY__ruleThatIsEmpty.ruleNumber)
                        val clonedRhs = RuntimeRuleRhs(RuntimeRuleRhsItemsKind.EMPTY, RuntimeRuleChoiceKind.NONE, RuntimeRuleListKind.NONE, -1, 0, arrayOf(clonedRuleThatIsEmpty))
                        clonedRuntimeRule.rhsOpt = clonedRhs
                    }
                }

                else -> Unit
            }
        }
        return clone
    }

    override fun toString(): String {
        val rulesStr = this.runtimeRules.map {
            "  " + it.toString()
        }.joinToString("\n")
        return """
            RuntimeRuleSet {
                ${rulesStr}
            }
        """.trimIndent()
    }

    override fun hashCode(): Int = number
    override fun equals(other: Any?): Boolean = when {
        other is RuntimeRuleSet -> this.number == other.number
        else -> false
    }
}