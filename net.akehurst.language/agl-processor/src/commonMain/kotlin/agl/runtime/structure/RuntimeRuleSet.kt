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

import net.akehurst.language.agl.automaton.ParserState
import net.akehurst.language.agl.automaton.ParserStateSet
import net.akehurst.language.agl.parser.InputFromString
import net.akehurst.language.api.parser.ParserException
import net.akehurst.language.collections.lazyMap
import net.akehurst.language.collections.lazyMapNonNull
import net.akehurst.language.collections.transitiveClosure

class LookaheadSet(
        val number: Int,
        val content: Set<RuntimeRule>
) {
    companion object {
        val EMPTY = LookaheadSet(-1, emptySet())
        val EOT = LookaheadSet(-2, setOf(RuntimeRuleSet.END_OF_TEXT))
        val UP = LookaheadSet(-3, setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))
    }

    override fun hashCode(): Int = number * 31
    override fun equals(other: Any?): Boolean = when {
        other is LookaheadSet -> this.number == other.number
        else -> false
    }

    override fun toString(): String = "LookaheadSet{$number,${content}}"
    fun createWithParent(parentLookahead: LookaheadSet): LookaheadSet {
        val newContent = mutableSetOf<RuntimeRule>()
        for (rr in this.content) {
            if (RuntimeRuleSet.USE_PARENT_LOOKAHEAD == rr) {
                newContent.addAll(parentLookahead.content)
            } else {
                newContent.add(rr)
            }
        }
        return LookaheadSet(-1, newContent) //TODO: create this from runtimeRuleset, maybe!
    }
}

class RuntimeRuleSet(
        // rules: List<RuntimeRule>
) {

    companion object {
        var nextRuntimeRuleSetNumber = 0

        val GOAL_RULE_NUMBER = -1;
        val EOT_RULE_NUMBER = -2;
        val SKIP_RULE_NUMBER = -3;
        val SKIP_CHOICE_RULE_NUMBER = -4;
        val USE_PARENT_LOOKAHEAD_RULE_NUMBER = -5;
        val END_OF_TEXT_TAG = "<EOT>"
        val GOAL_TAG = "<GOAL>"
        val SKIP_RULE_TAG = "<SKIP-MULTI>"
        val SKIP_CHOICE_RULE_TAG = "<SKIP-CHOICE>"
        val USE_PARENT_LOOKAHEAD_RULE_TAG = "<USE-PARENT-LOOKAHEAD>";

        val END_OF_TEXT = RuntimeRule(-1, EOT_RULE_NUMBER, END_OF_TEXT_TAG, InputFromString.END_OF_TEXT, RuntimeRuleKind.TERMINAL, false, false)
        val USE_PARENT_LOOKAHEAD = RuntimeRule(-1, USE_PARENT_LOOKAHEAD_RULE_NUMBER, USE_PARENT_LOOKAHEAD_RULE_TAG, 0.toChar().toString(), RuntimeRuleKind.TERMINAL, false, false)

        fun createGoalRule(userGoalRule: RuntimeRule): RuntimeRule {
            val gr = RuntimeRule(userGoalRule.runtimeRuleSetNumber, GOAL_RULE_NUMBER, GOAL_TAG, GOAL_TAG, RuntimeRuleKind.GOAL, false, false)
            val items = listOf(userGoalRule) //+ possibleEndOfText
            gr.rhsOpt = RuntimeRuleItem(RuntimeRuleItemKind.CONCATENATION, RuntimeRuleChoiceKind.NONE, -1, 0, items.toTypedArray())
            return gr
        }

    }

    val number: Int = nextRuntimeRuleSetNumber++

    private val nonTerminalRuleNumber: MutableMap<String, Int> = mutableMapOf()
    private val terminalRuleNumber: MutableMap<String, Int> = mutableMapOf()
    private val embeddedRuleNumber: MutableMap<String, Int> = mutableMapOf()

    private var nextLookaheadSetId = 0
    private val lookaheadSets = mutableListOf<LookaheadSet>()

    //TODO: are Arrays faster than Lists?
    var runtimeRules: List<RuntimeRule> = emptyList()

    val skipRules: Array<RuntimeRule> by lazy {
        this.runtimeRules.filter { it.isSkip }.toTypedArray()
    }

    // used to add to lookahead
    val firstSkipTerminals: Array<RuntimeRule> by lazy {
        this.skipRules.flatMap {
            firstTerminals[it.number]
        }.toTypedArray()
    }

    val nonSkipRules: Array<RuntimeRule> by lazy {
        this.runtimeRules.filter { it.isSkip.not() }.toTypedArray()
    }

    // used if scanning (excluding skip)
    val nonSkipTerminals: Array<RuntimeRule> by lazy {
        this.nonSkipRules.flatMap {
            when (it.kind) {
                RuntimeRuleKind.GOAL -> emptyList()
                RuntimeRuleKind.TERMINAL -> listOf(it)
                RuntimeRuleKind.NON_TERMINAL -> emptyList()//it.rhs.items.filter { it.kind == RuntimeRuleKind.TERMINAL }
                RuntimeRuleKind.EMBEDDED -> it.embeddedRuntimeRuleSet!!.nonSkipTerminals.toList()
            }
        }.toTypedArray()
    }

    // used if scanning (including skip)
    val terminalRules: Array<RuntimeRule> by lazy {
        this.runtimeRules.flatMap {
            when (it.kind) {
                RuntimeRuleKind.GOAL -> emptyList()
                RuntimeRuleKind.TERMINAL -> listOf(it)
                RuntimeRuleKind.NON_TERMINAL -> emptyList()
                RuntimeRuleKind.EMBEDDED -> it.embeddedRuntimeRuleSet!!.terminalRules.toList()
            }
        }.toTypedArray()
    }

    val firstTerminals: Array<Set<RuntimeRule>> by lazy {
        this.runtimeRules.map { this.calcFirstTerminals(it) }
                .toTypedArray()
    }

    val firstSkipRuleTerminalPositions: Set<RuntimeRule> by lazy {
        this.runtimeRules.filter { it.isSkip }.flatMap { this.calcFirstTerminals(it) }.toSet()
//        this.calcFirstTerminalSkipRulePositions()
    }

    // used when calculating lookahead
    val expectedTerminalRulePositions = lazyMap<RulePosition, Array<RulePosition>> {
        calcExpectedTerminalRulePositions(it).toTypedArray()
    }

    // used when calculating lookahead
    val firstTerminals2 = lazyMapNonNull<RulePosition, Set<RuntimeRule>> {
        val trps = expectedTerminalRulePositions[it] ?: arrayOf()
        trps.flatMap { it.items }.toSet()
    }

    // userGoalRule -> ParserStateSet
    private val states_cache = mutableMapOf<RuntimeRule, ParserStateSet>()
    private val skipStateSet = mutableMapOf<RuntimeRule, ParserStateSet>()

    val skipParserStateSet: ParserStateSet? by lazy {
        if (skipRules.isEmpty()) {
            null
        } else {

            val skipChoiceRule = RuntimeRule(this.number, SKIP_CHOICE_RULE_NUMBER, SKIP_CHOICE_RULE_TAG, SKIP_CHOICE_RULE_TAG, RuntimeRuleKind.NON_TERMINAL, false, true, null, null)
            skipChoiceRule.rhsOpt = RuntimeRuleItem(RuntimeRuleItemKind.CHOICE, RuntimeRuleChoiceKind.LONGEST_PRIORITY, -1, 0, skipRules)
            val skipMultiRule = RuntimeRule(this.number, SKIP_RULE_NUMBER, SKIP_RULE_TAG, SKIP_RULE_TAG, RuntimeRuleKind.NON_TERMINAL, false, true, null, null)
            skipMultiRule.rhsOpt = RuntimeRuleItem(RuntimeRuleItemKind.MULTI, RuntimeRuleChoiceKind.NONE, 1, -1, arrayOf(skipChoiceRule))

            //val skipGoalRule = RuntimeRule(this.number, SKIP_RULE_NUMBER, SKIP_RULE_TAG, "", RuntimeRuleKind.NON_TERMINAL, false, true, null, null)
            //skipGoalRule.rhsOpt = RuntimeRuleItem(RuntimeRuleItemKind.CHOICE, RuntimeRuleChoiceKind.LONGEST_PRIORITY, -1, 0, skipRules)

            val ss = ParserStateSet(nextStateSetNumber++, this, skipMultiRule, true)
            //this.states_cache[skipGoalRule] = ss
            ss
        }
    }

    //called from ParserStateSet, which adds the Goal Rule bits
    internal val parentPosition = lazyMapNonNull<RuntimeRule, Set<RulePosition>> { childRR ->
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
                RuntimeRuleKind.TERMINAL -> this.terminalRuleNumber[rr.tag] = rr.number
                RuntimeRuleKind.NON_TERMINAL -> this.nonTerminalRuleNumber[rr.tag] = rr.number
                RuntimeRuleKind.EMBEDDED -> this.embeddedRuleNumber[rr.tag] = rr.number
            }
        }
        this.runtimeRules = rules.sortedBy { it.number }
    }

    fun automatonFor(goalRuleName: String): ParserStateSet {
        this.buildFor(goalRuleName)
        val gr = this.findRuntimeRule(goalRuleName)
        return this.states_cache[gr]!! //findRuntimeRule would throw exception if not exist
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
                                    val lh: Set<RuntimeRule> = this.firstTerminals2[nextChildRP]
                                            ?: error("should never happen")
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
                        val lh: Set<RuntimeRule> = this.firstTerminals2[nextRP] ?: error("should never happen")
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

    fun buildFor(goalRuleName: String): ParserStateSet {
        val gr = this.findRuntimeRule(goalRuleName)
        val s0 = this.startingState(gr)
        return s0.stateSet.build()
    }

    fun fetchStateSetFor(userGoalRule: RuntimeRule): ParserStateSet {
        //TODO: need to cache by possibleEndOfText also
        var stateSet = this.states_cache[userGoalRule]
        if (null == stateSet) {
            stateSet = ParserStateSet(nextStateSetNumber++, this, userGoalRule, false)
            this.states_cache[userGoalRule] = stateSet
        }
        return stateSet
    }

    fun startingState(userGoalRule: RuntimeRule): ParserState {
        var stateSet = fetchStateSetFor(userGoalRule)
        return stateSet.startState
    }

    // ---

    fun findRuntimeRule(ruleName: String): RuntimeRule {
        val number = this.nonTerminalRuleNumber[ruleName]
                ?: this.terminalRuleNumber[ruleName]
                ?: this.embeddedRuleNumber[ruleName]
                ?: throw ParserException("RuntimeRule '${ruleName}' not found")
        return this.runtimeRules[number]
    }

    fun findTerminalRule(pattern: String): RuntimeRule {
        val number = this.terminalRuleNumber[pattern]
                ?: throw ParserException("Terminal RuntimeRule ${pattern} not found")
        return this.runtimeRules[number]
    }

    // used when calculating lookahead ?
    private fun calcExpectedItemRulePositionTransitive(rp: RulePosition): Set<RulePosition> {
        var s = setOf(rp)//rp.runtimeRule.calcExpectedRulePositions(rp.position)

        return s.transitiveClosure { rp ->
            if (RulePosition.END_OF_RULE == rp.position) {
                emptySet()
            } else {
                when (rp.runtimeRule.kind) {
                    RuntimeRuleKind.TERMINAL -> emptySet<RulePosition>()
                    RuntimeRuleKind.GOAL,
                    RuntimeRuleKind.NON_TERMINAL -> rp.runtimeRule.items(rp.option, rp.position).flatMap {
                        when (it.kind) {
                            RuntimeRuleKind.GOAL -> TODO()
                            RuntimeRuleKind.TERMINAL -> setOf(rp)
                            RuntimeRuleKind.NON_TERMINAL -> it.calcExpectedRulePositions(0)
                            RuntimeRuleKind.EMBEDDED -> {
                                val embeddedStartRp = RulePosition(it.embeddedStartRule!!, 0, RulePosition.START_OF_RULE)
                                it.embeddedRuntimeRuleSet!!.expectedTerminalRulePositions[embeddedStartRp]!!.toSet()
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

    internal fun createLookaheadSet(content: Set<RuntimeRule>): LookaheadSet {
        return when {
            content.isEmpty() -> LookaheadSet.EMPTY
            LookaheadSet.EOT.content == content -> LookaheadSet.EOT
            LookaheadSet.UP.content == content -> LookaheadSet.UP
            else -> {
                val existing = this.lookaheadSets.firstOrNull { it.content == content }
                if (null == existing) {
                    val num = this.nextLookaheadSetId++
                    val lhs = LookaheadSet(num, content)
                    this.lookaheadSets.add(lhs)
                    lhs
                } else {
                    existing
                }
            }
        }
    }

    internal fun printUsedAutomaton(goalRuleName: String): String {
        val b = StringBuilder()
        val gr = this.findRuntimeRule(goalRuleName)

        val states = this.states_cache[gr]!!.states.values
        val transitions = states.flatMap { it.allBuiltTransitions.toSet() }.toSet()

        states.forEach {
            b.append(it).append("\n")
        }
        transitions.forEach {
            b.append(it).append("\n")
        }


        return b.toString()
    }

    internal fun printFullAutomaton(goalRuleName: String, withClosure: Boolean = false): String {
        this.buildFor(goalRuleName)
        return this.printUsedAutomaton("S")
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