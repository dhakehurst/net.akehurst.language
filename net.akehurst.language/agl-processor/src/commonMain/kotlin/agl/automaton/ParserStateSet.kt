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

package net.akehurst.language.agl.automaton

import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.collections.MutableQueue
import net.akehurst.language.collections.Stack
import net.akehurst.language.collections.lazyMapNonNull
import kotlin.reflect.KProperty

// TODO: remove this pre release
// This a workaround for the debugger
// see [https://youtrack.jetbrains.com/issue/KTIJ-1170#focus=Comments-27-4433190.0-0]
operator fun <T> Lazy<T>.getValue(thisRef: Any?, property: KProperty<*>) = value

enum class AutomatonKind {
    LOOKAHEAD_NONE,     // LC(O) like LR(0)
    LOOKAHEAD_SIMPLE,   // SLC like SLR
    LOOKAHEAD_1         // LC(1) like LR(1)
}

class ParserStateSet(
    val number: Int,
    val runtimeRuleSet: RuntimeRuleSet,
    val userGoalRule: RuntimeRule,
    val isSkip: Boolean,
    val automatonKind: AutomatonKind
) {

    private var nextStateNumber = 0
    var preBuilt = false; private set
    internal val buildCache: BuildCache = when (automatonKind) {
        AutomatonKind.LOOKAHEAD_NONE -> BuildCacheLC0(this)
        AutomatonKind.LOOKAHEAD_SIMPLE -> TODO()
        AutomatonKind.LOOKAHEAD_1 -> BuildCacheLC1(this)
    }
    //internal val buildCache:BuildCache = BuildCacheLC1(this)

    val usedRules: Set<RuntimeRule> by lazy {
        calcUsedRules(this.startState.runtimeRules.first())
    }
    val usedTerminalRules: Set<RuntimeRule> by lazy {
        this.usedRules.filter { it.kind === RuntimeRuleKind.TERMINAL }.toSet()
    }
    val usedNonTerminalRules: Set<RuntimeRule> by lazy {
        this.usedRules.filter { it.kind !== RuntimeRuleKind.TERMINAL }.toSet()
    }

    /*
     * A collection of RulePositions identifies a Parser state.
     * similar to LR(0) states
     * Lookahead on the transitions allows for equivalence to LR(1)
     */
    val states = lazyMapNonNull<List<RulePosition>, ParserState> {
        ParserState(StateNumber(this.nextStateNumber++), it, this)
    }

    val allBuiltTransitions: Set<Transition> get() = states.values.flatMap { it.outTransitions.allBuiltTransitions }.toSet()

    val startState: ParserState by lazy {
        val goalRule = RuntimeRuleSet.createGoalRule(userGoalRule)
        val goalRP = listOf(RulePosition(goalRule, 0, 0))
        val state = this.states[goalRP]
        state
    }
    val endState: ParserState by lazy {
        val goalRule = this.startState.runtimeRules.first()
        val goalRP = listOf(RulePosition(goalRule, 0, RulePosition.END_OF_RULE))
        val state = this.states[goalRP]
        state
    }

    /*
        // runtimeRule -> set of rulePositions where the rule is used
        internal val parentPosition = lazyMapNonNull<RuntimeRule, Set<RulePosition>> { childRR ->
            //TODO: possibly faster to pre cache this! and goal rules currently not included!
            when {
                (childRR === RuntimeRuleSet.END_OF_TEXT) -> { //TODO: should this check for contains in possibleEndOfText, and what if something in endOfText is also valid mid text!
                    setOf(RulePosition(this.startState.runtimeRule, 0, 1))
                }
                childRR.isSkip -> when {
                    //this must be the skipParserStateSet, could test this.isSkip to be sure!
                    childRR.number == RuntimeRuleSet.SKIP_RULE_NUMBER -> {
                        setOf(this.startState.rulePosition)
                    }
                    childRR.number == RuntimeRuleSet.SKIP_CHOICE_RULE_NUMBER -> {
                        val option = this.runtimeRuleSet.skipRules.indexOf(childRR)
                        setOf(RulePosition(this.userGoalRule, 0, 0), RulePosition(this.userGoalRule, 0, RulePosition.MULIT_ITEM_POSITION))
                    }
                    else -> {
                        val option = this.runtimeRuleSet.skipRules.indexOf(childRR)
                        val chRule = this.userGoalRule.rhs.items[RuntimeRuleItem.MULTI__ITEM]
                        setOf(RulePosition(chRule, option, 0))
                    }
                }
                else -> {
                    val s = this.runtimeRuleSet.parentPosition[childRR].filter { this.usedNonTerminalRules.contains(it.runtimeRule) }.toSet()
                    if (childRR == this.userGoalRule) {
                        s + this.startState.rulePosition
                    } else {
                        s
                    }
                }
            }
        }

        internal val _parentRelations = mutableMapOf<RuntimeRule, Set<ParentRelation>>()
        internal fun parentRelation(runtimeRule: RuntimeRule): Set<ParentRelation> {
            var set = _parentRelations[runtimeRule]
            return if (null == set) {
                val t = if (runtimeRule == this.userGoalRule) {
                    setOf(ParentRelation(this.startState.rulePosition, LookaheadSet.UP))
                } else {
                    emptySet()
                }
                set = t + this.calcParentRelation(runtimeRule)
                _parentRelations[runtimeRule] = set
                set
            } else {
                set
            }
        }

        private fun calcParentRelation(childRR: RuntimeRule): Set<ParentRelation> {
            val x = this.parentPosition[childRR].map { rp ->
                val lhc = calcLookaheadUp2(rp)
                val lhs = createLookaheadSet(lhc)
                ParentRelation(rp, lhs)
            }.toSet()
            return x
        }
    */
    internal val firstTerminals = lazyMapNonNull<RulePosition, Set<RuntimeRule>> { rp ->
        when (rp.runtimeRule.kind) {
            RuntimeRuleKind.TERMINAL -> setOf(rp.runtimeRule)
            RuntimeRuleKind.EMBEDDED -> {
                TODO()
            }
            RuntimeRuleKind.GOAL -> when {
                rp.isAtStart -> when {
                    this.isSkip -> {
                        this.runtimeRuleSet.skipRules.flatMap {
                            this.runtimeRuleSet.firstTerminals[it.number].filter { this.usedTerminalRules.contains(it) }
                        }.toSet()
                    }
                    else -> this.runtimeRuleSet.firstTerminals[this.userGoalRule.number].filter { this.usedTerminalRules.contains(it) }.toSet()
                }
                rp.isAtEnd -> emptySet()
                else -> emptySet()//this.possibleEndOfText
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

    internal fun createLookaheadSet(content: Set<RuntimeRule>): LookaheadSet = this.runtimeRuleSet.createLookaheadSet(content) //TODO: Maybe cache here rather than in rrs
    fun createWithParent(upLhs: LookaheadSet, parentLookahead: LookaheadSet): LookaheadSet = this.runtimeRuleSet.createWithParent(upLhs, parentLookahead)

    fun build(): ParserStateSet {
        println("Build not yet implemented")
        //this.buildCache.on()
        //TODO: buildAndTraverse()
        //preBuilt = true
        //this.buildCache.clearAndOff()
        return this
    }

    private fun buildAndTraverse1() {
        data class StateNode(val prev: StateNode?, val state: ParserState) {
            override fun toString(): String = when {
                null == prev -> "$state"
                else -> "$state --> $prev"
            }
        }

        val done = mutableSetOf<Pair<ParserState?, Transition>>()
        val transitions = MutableQueue<Pair<StateNode, Transition>>()
        var curNode = StateNode(null, this.startState)
        val s0_trans = this.startState.transitions(curNode.prev?.state)
        s0_trans.forEach { transitions.enqueue(Pair(curNode, it)) }
        while (transitions.isEmpty.not()) {
            val pair = transitions.dequeue()
            curNode = pair.first
            val prevState = curNode.prev?.state
            val tr = pair.second
            val dp = Pair(prevState, tr)
            if (done.contains(dp)) {
                //do nothing
            } else {
                done.add(dp)
                // assume we take the transition
                val curState = curNode.state
                val nextState = tr.to
                check(tr.from == curState) { "Error building Automaton" }
                when (tr.action) {
                    Transition.ParseAction.WIDTH -> {
                        val newNode = StateNode(prev = curNode, state = nextState)
                        val newTrans = newNode.state.transitions(newNode.prev?.state)
                        newTrans.forEach { transitions.enqueue(Pair(newNode, it)) }
                        //done.add(dp)
                    }
                    Transition.ParseAction.EMBED -> {
                        val newNode = StateNode(prev = curNode, state = nextState)
                        val newTrans = newNode.state.transitions(newNode.prev?.state)
                        newTrans.forEach { transitions.enqueue(Pair(newNode, it)) }
                        //done.add(dp)
                    }
                    Transition.ParseAction.HEIGHT -> {
                        val newNode = StateNode(prev = curNode.prev, state = nextState)
                        val newTrans = newNode.state.transitions(newNode.prev?.state)
                        newTrans.forEach { transitions.enqueue(Pair(newNode, it)) }
                    }
                    Transition.ParseAction.GRAFT -> {
                        val prevNode = curNode.prev!!
                        val newNode = StateNode(prev = prevNode.prev, state = nextState)
                        val newTrans = newNode.state.transitions(newNode.prev?.state)
                        newTrans.forEach { transitions.enqueue(Pair(newNode, it)) }
                    }
                    Transition.ParseAction.GRAFT_OR_HEIGHT -> TODO()
                    Transition.ParseAction.GOAL -> null;//buildAndTraverse(nextState, prevStack, done)
                }
            }
        }
    }

    private fun buildAndTraverse() {
        this.buildCache.buildCaches()
        // key = Pair<listOf(<rhs-items>)>
        val stateInfos = this.buildCache.stateInfo()

        for (si in stateInfos) {
            val state = this.states[si.rulePositions]
            when {
                state.isGoal -> state.transitions(null)
                else -> {
                    for (prevRps in si.possiblePrev) {
                        val prev = this.states[prevRps]
                        state.transitions(prev)
                    }
                }
            }
        }

    }

    internal fun fetch(rulePosition: List<RulePosition>): ParserState {
        return this.states[rulePosition]
    }
    /*
    private val _growsInto = mutableMapOf<Pair<RulePosition, RulePosition>, Boolean>()
    fun growsInto(ancestor: RulePosition, descendant: RulePosition): Boolean {
        //TODO: can we do this faster somehow? the closure is potentially slow!
        val p = Pair(ancestor, descendant)
        var r = _growsInto[p]
        if (null == r) {
            val thisStart = descendant.runtimeRule.rulePositionsAt[0]
            r = calcClosureLR0(ancestor).any {
                thisStart.contains(it)
            }
            _growsInto[p] = r
        }
        return r
    }*/


    private fun calcUsedRules(
        rule: RuntimeRule,
        used: MutableSet<RuntimeRule> = mutableSetOf(),
        done: BooleanArray = BooleanArray(this.runtimeRuleSet.runtimeRules.size)
    ): Set<RuntimeRule> {
        return when {
            0 > rule.number -> {
                used.add(rule)
                for (sr in rule.rhs.items) {
                    calcUsedRules(sr, used, done)
                }
                used
            }
            done[rule.number] -> used
            else -> when {
                rule.kind === RuntimeRuleKind.NON_TERMINAL -> {
                    used.add(rule)
                    done[rule.number] = true
                    for (sr in rule.rhs.items) {
                        calcUsedRules(sr, used, done)
                    }
                    used
                }
                else -> {
                    used.add(rule)
                    done[rule.number] = true
                    used
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

    override fun toString(): String = "ParserStateSet{$number}"
}