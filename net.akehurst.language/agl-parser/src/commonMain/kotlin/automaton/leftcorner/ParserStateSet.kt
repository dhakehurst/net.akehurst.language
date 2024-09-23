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

package net.akehurst.language.automaton.leftcorner

import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.agl.util.Debug
import net.akehurst.language.automaton.api.Automaton
import net.akehurst.language.automaton.api.AutomatonKind
import net.akehurst.language.automaton.leftcorner.ParserState.Companion.lhs

class ParserStateSet(
    val number: Int,
    val runtimeRuleSet: RuntimeRuleSet,
    val userGoalRule: RuntimeRule,
    val isSkip: Boolean,
    val automatonKind: AutomatonKind,
    preBuilt: Boolean
) : Automaton {

    private var nextLookaheadSetId = 0
    private val lookaheadSets = mutableListOf<LookaheadSet>()
    private var nextStateNumber = 0

    internal val runtimeTransitionCalculator = RuntimeTransitionCalculator(this)

    var preBuilt = preBuilt; private set
    internal val buildCache: BuildCache by lazy {
        when (automatonKind) {
            AutomatonKind.LOOKAHEAD_NONE -> TODO() //BuildCacheLC0(this)
            AutomatonKind.LOOKAHEAD_SIMPLE -> TODO()
            AutomatonKind.LOOKAHEAD_1 -> BuildCacheLC1(this)
        }
    }

    val usedRules: Set<RuntimeRule> by lazy { this.runtimeRuleSet.calcUsedRules(this.startState.runtimeRules.first()) }
    val usedTerminalRules: Set<RuntimeRule> by lazy { this.usedRules.filter { it.isTerminal }.toSet() }
    val usedNonTerminalRules: Set<RuntimeRule> by lazy { this.usedRules.filter { it.isNonTerminal }.toSet() }
    val firstTerminals: Set<RuntimeRule> by lazy { this.startState.transitionsGoal(this.startState).map { it.to.firstRule }.toSet() }
    val embeddedRuntimeRuleSet: Set<RuntimeRuleSet> by lazy {
        val embRules = this.usedRules.filter { it.isEmbedded }.toSet()
        embRules.map {
            val rhs = it.rhs as RuntimeRuleRhsEmbedded
            rhs.embeddedRuntimeRuleSet
        }.toSet()
    }

    /*
     * A collection of RulePositions identifies a Parser state.
     * similar to LR(0) states
     * Lookahead on the transitions allows for equivalence to LR(1)
     */
    private val _statesByRulePosition = mutableMapOf<List<RulePositionRuntime>, ParserState>()
    private val _states = mutableListOf<ParserState>()

    val allBuiltStates: List<ParserState> get() = this._states.toList()
    val allBuiltTransitions: Set<Transition> get() = this.allBuiltStates.flatMap { it.outTransitions.allBuiltTransitions }.toSet()

    val goalRule by lazy { runtimeRuleSet.goalRuleFor[userGoalRule] }
    val startRulePosition by lazy { RulePositionRuntime(goalRule, 0, RulePositionRuntime.START_OF_RULE) }
    val finishRulePosition by lazy { RulePositionRuntime(goalRule, 0, RulePositionRuntime.END_OF_RULE) }
    val startState: ParserState by lazy { this.createState(listOf(startRulePosition)) }
    val finishState: ParserState by lazy { this.createState(listOf(finishRulePosition)) }

    internal val firstOf = FirstOf()

    /*
    internal val firstTerminals = lazyMutableMapNonNull<RulePosition, List<RuntimeRule>> { rp ->
        when (rp.runtimeRule.kind) {
            RuntimeRuleKind.TERMINAL -> listOf(rp.runtimeRule)
            RuntimeRuleKind.EMBEDDED -> {
                TODO()
            }
            RuntimeRuleKind.GOAL -> when {
                rp.isAtStart -> when {
                    this.isSkip -> {
                        this.runtimeRuleSet.skipRules.flatMap {
                            this.runtimeRuleSet.firstTerminals[it.number].filter { this.usedTerminalRules.contains(it) }
                        }.toSet().toList()
                    }
                    else -> this.runtimeRuleSet.firstTerminals[this.userGoalRule.number].filter { this.usedTerminalRules.contains(it) }.toSet().toList()
                }
                rp.isAtEnd -> emptyList()
                else -> emptyList()//this.possibleEndOfText
            }
            RuntimeRuleKind.NON_TERMINAL -> {
                this.runtimeRuleSet.firstTerminals2[rp]
                /*
                rp.items.flatMap {
                    when {
                        it.number >= 0 -> this.runtimeRuleSet.firstTerminals[it.number].filter { this.usedTerminalRules.contains(it) }
                        else -> when (it.number) {
                            RuntimeRuleSet.GOAL_RULE_NUMBER -> TODO()
                            RuntimeRuleSet.SKIP_RULE_NUMBER -> this.runtimeRuleSet.skipRules.flatMap {
                                this.runtimeRuleSet.firstTerminals[it.number].filter { this.usedTerminalRules.contains(it) }
                            }.toSet()
                            // RuntimeRuleSet.SKIP_CHOICE_RULE_NUMBER -> it.rhs.items.flatMap {
                            //     this.runtimeRuleSet.firstTerminals[it.number]
                            // }.toSet()
                            else -> error("should not happen")
                        }
                    }
                }.toSet()
                 */
            }
        }
    }
*/
    //internal fun createLookaheadSet(content: Set<RuntimeRule>): LookaheadSet = this.runtimeRuleSet.createLookaheadSet(content) //TODO: Maybe cache here rather than in rrs
    //fun createWithParent(upLhs: LookaheadSet, parentLookahead: LookaheadSet): LookaheadSet = this.runtimeRuleSet.createWithParent(upLhs, parentLookahead)

    internal fun createState(rulePositions: List<RulePositionRuntime>): ParserState {
        if (Debug.CHECK) check(this._statesByRulePosition.contains(rulePositions).not()) { "State already created for $rulePositions" }
        val existing = this._statesByRulePosition[rulePositions]
        return if (null != existing) {
            existing
        } else {
            val state = ParserState(StateNumber(this.nextStateNumber++), rulePositions.toList(), this)
            this._statesByRulePosition[rulePositions] = state
            this._states.add(state.number.value, state)
            state
        }
    }

    internal fun createMergedState(rulePositions: List<RulePositionRuntime>): ParserState {
        if (Debug.CHECK) check(this._statesByRulePosition.contains(rulePositions).not()) { "State already created for $rulePositions" }
        val existing = this._statesByRulePosition[rulePositions]
        return if (null != existing) {
            existing
        } else {
            val si = this.buildCache.mergedStateInfoFor(rulePositions)
            val state = createState(si.rulePositions.toList())
            state
        }
    }

    internal fun fetchState(rulePositions: List<RulePositionRuntime>): ParserState? =
        this.allBuiltStates.firstOrNull { it.rulePositions.toSet() == rulePositions.toSet() }

    internal fun fetchOrCreateMergedState(rulePositions: List<RulePositionRuntime>): ParserState =
        fetchState(rulePositions) ?: this.createMergedState(rulePositions)

    internal fun fetchCompatibleState(rulePositions: List<RulePositionRuntime>): ParserState? {
        val existing = this.allBuiltStates.firstOrNull {
            it.rulePositions.containsAll(rulePositions)
        }
        return existing
    }

    internal fun fetchCompatibleOrCreateState(rulePositions: List<RulePositionRuntime>): ParserState =
        fetchCompatibleState(rulePositions) ?: this.createMergedState(rulePositions)

    internal fun createLookaheadSet(includeRT: Boolean, includeEOT: Boolean, matchAny: Boolean, content: Set<RuntimeRule>): LookaheadSet {
        return when {
            content.isEmpty() -> when {
                matchAny -> LookaheadSet.ANY
                includeRT && includeEOT.not() && matchAny.not() -> LookaheadSet.RT
                includeRT.not() && includeEOT && matchAny.not() -> LookaheadSet.EOT
                includeRT.not() && includeEOT.not() && matchAny.not() -> LookaheadSet.EMPTY
                includeRT && includeEOT.not() && matchAny.not() -> LookaheadSet.RT
                includeRT && includeEOT && matchAny.not() -> LookaheadSet.RT_EOT
                else -> error("Internal Error: situation not handled")
            }

            else -> {
                val existing = this.lookaheadSets.firstOrNull {
                    it.includesRT == includeRT &&
                            it.includesEOT == includeEOT &&
                            it.matchANY == matchAny &&
                            it.content == content  //TODO: slow
                }
                if (null == existing) {
                    val num = this.nextLookaheadSetId++
                    val lhs = LookaheadSet(num, includeRT, includeEOT, matchAny, content)
                    this.lookaheadSets.add(lhs)
                    lhs
                } else {
                    existing
                }
            }
        }
    }

    internal fun createLookaheadSet(part: LookaheadSetPart): LookaheadSet = createLookaheadSet(part.includesRT, part.includesEOT, part.matchANY, part.content)

    fun precedenceRulesFor(sourceState: ParserState): RuntimePreferenceRule? =
        sourceState.runtimeRules.map { src -> this.runtimeRuleSet.precedenceRulesFor(src) }.firstOrNull() //FIXME: if more than one rule ?

    fun build(): ParserStateSet {
        if (this.preBuilt.not()) {
            this.buildCache.switchCacheOn()
            buildAndTraverse()
            preBuilt = true
            this.buildCache.clearAndOff()
        } else {
            // already built
            //TODO: would like to throw error here! error("Automaton already built")
        }
        return this
    }

    private fun buildAndTraverse() {
        // this.buildCache.buildCaches()
        val stateInfos = this.buildCache.stateInfo()
        for (si in stateInfos) {
            if (si.rulePositions != this.startState.rulePositions) {
                if (Debug.CHECK) check(this._statesByRulePosition.any { it.key.toSet() == si.rulePositions }.not()) { "State already created for $si.rulePositions" }
                val state = this.fetchCompatibleOrCreateState(si.rulePositions.toList())
            }
        }
        for (si in stateInfos) {
            val state = this.fetchState(si.rulePositions.toList())!!
            for (ti in si.possibleTrans) {
                val from = state
                val action = ti.action
                val to = this.fetchCompatibleState(ti.to.toList()) ?: error("Internal error, state not created for ${ti.to}")
                val lhs = ti.lookahead.map { Lookahead(it.guard.lhs(this), it.up.lhs(this)) }.toSet()
                for (ps in ti.prev) {
                    val prev = this.fetchCompatibleState(ps.toList()) ?: error("Internal error, state not created for $ps")
                    when {
                        state.isNotAtEnd -> state.outTransitions.createTransitionForIncomplete(prev, from, action, to, lhs)
                        else -> {
                            for (pps in ti.prevPrev) {
                                val prevPrev = this.fetchCompatibleState(pps.toList()) ?: error("Internal error, state not created for $pps")
                                state.outTransitions.createTransitionForComplete(prev, prevPrev, from, action, to, lhs)
                            }
                        }
                    }
                }
            }
        }
    }

    /*
        fun expectedAfter(rulePosition: RulePosition, doneDn: MutableMap<RulePosition, Set<RuntimeRule>> = mutableMapOf(), doneUp: MutableMap<RulePosition, Set<RuntimeRule>> = mutableMapOf()): Set<RuntimeRule> {
            return when {
                doneDn.containsKey(rulePosition) -> doneDn[rulePosition]!!
                rulePosition.runtimeRule == this.startState.runtimeRule -> when {
                    rulePosition.isAtStart -> this.expectedAfter(RulePosition(rulePosition.item!!, 0, 0), doneDn, doneUp)
                    rulePosition.isAtEnd -> LookaheadSet.UP.content
                    else -> error("should never happen")
                }
                rulePosition.isAtEnd -> {
                    val pps = this.parentPosition[rulePosition.runtimeRule]
                    val ppsn = pps.flatMap { it.next() }.toSet()
                    ppsn.flatMap { parentRp ->
                        doneUp[rulePosition] = emptySet()
                        val lh = this.expectedAfter(parentRp, doneDn, doneUp)
                        doneUp[rulePosition] = lh
                        lh
                    }.toSet()
                }
                else -> {
                    doneDn[rulePosition] = emptySet()
                    val fst = rulePosition.item
                    val nextRp = rulePosition.next()
                    val next = nextRp.flatMap {
                        expectedAfter(it, doneDn, doneUp)
                    }.toSet()
                    val lh = when {
                        null == fst -> error("Internal Error: should never happen")
                        fst.isEmptyRule -> next
                        else -> when (fst.kind) {
                            RuntimeRuleKind.GOAL -> TODO()
                            RuntimeRuleKind.TERMINAL -> setOf(fst)
                            RuntimeRuleKind.NON_TERMINAL -> {
                                val fs = fst.rulePositionsAt[0]
                                fs.flatMap {
                                    firstOf(it, next)
                                }.toSet()
                            }
                            RuntimeRuleKind.EMBEDDED -> TODO()
                        }
                    }
                    doneDn[rulePosition] = lh
                    lh
                }
            }
        }
    */
    override fun hashCode(): Int = this.number
    override fun equals(other: Any?): Boolean = when (other) {
        is ParserStateSet -> this.number == other.number
        else -> false
    }

    override fun asString(withStates: Boolean) = usedAutomatonToString(withStates)

    fun usedAutomatonToString(withStates: Boolean = false): String {
        val b = StringBuilder()
        val states = this.allBuiltStates
        val transitions = states.flatMap { it.outTransitions.allBuiltTransitions }

        b.append("UsedRules: ${this.usedRules.size}  States: ${states.size}  Transitions: ${transitions.size} ")
        b.append("\n")

        if (withStates) {
            states.forEach {
                val str = "$it {${it.outTransitions.allPrevious.joinToString { it.toString() }}}"
                b.append(str).append("\n")
            }
        }

        transitions.sortedBy { it.from.rulePositions.toString() }.sortedBy { it.to.rulePositions.toString() }
            .forEach { tr ->
                val prev = tr.from.outTransitions.previousFor(tr)
                    .joinToString { it.toString() }
                val frStr = "${tr.from.number.value}:(${tr.from.rulePositions.joinToString { "$it" }})"
                val toStr = "${tr.to.number.value}:${tr.to.rulePositions}"
                val trStr = "$frStr --> $toStr"
                val lh = tr.lookahead.joinToString(separator = "|") { "[${it.guard.fullContent.joinToString { it.tag }}](${it.up.fullContent.joinToString { it.tag }})" }
                b.append(" ${tr.action} ")
                b.append(trStr)
                b.append(lh)
                b.append(" {${prev}} ")
                b.append("\n")
            }

        return b.toString()
    }

    override fun toString(): String = "ParserStateSet{$number (${this.userGoalRule.tag}) }"
}