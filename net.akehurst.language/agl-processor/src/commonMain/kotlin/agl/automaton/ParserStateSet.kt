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

import net.akehurst.language.agl.automaton.ParserState.Companion.lhs
import net.akehurst.language.agl.runtime.graph.GrowingNodeIndex
import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.api.processor.AutomatonKind
import net.akehurst.language.collections.*

internal class ParserStateSet(
    val number: Int,
    val runtimeRuleSet: RuntimeRuleSet,
    val userGoalRule: RuntimeRule,
    val isSkip: Boolean,
    val automatonKind: AutomatonKind
) {

    private var nextLookaheadSetId = 0
    private val lookaheadSets = mutableListOf<LookaheadSet>()
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
    private val states = mutableMapOf<List<RulePosition>, ParserState>()

    val allBuiltStates: List<ParserState> get() = this.states.values.toList()
    val allBuiltTransitions: Set<Transition> get() = this.allBuiltStates.flatMap { it.outTransitions.allBuiltTransitions }.toSet()

    val goalRule by lazy { RuntimeRuleSet.createGoalRule(userGoalRule) }
    val startRulePosition by lazy { RulePosition(goalRule, 0, 0) }
    val finishRulePosition by lazy { RulePosition(goalRule, 0, RulePosition.END_OF_RULE) }
    val startState: ParserState by lazy { this.createState(listOf(startRulePosition)) }
    val endState: ParserState by lazy { this.createState(listOf(finishRulePosition)) }

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

    //internal fun createLookaheadSet(content: Set<RuntimeRule>): LookaheadSet = this.runtimeRuleSet.createLookaheadSet(content) //TODO: Maybe cache here rather than in rrs
    //fun createWithParent(upLhs: LookaheadSet, parentLookahead: LookaheadSet): LookaheadSet = this.runtimeRuleSet.createWithParent(upLhs, parentLookahead)

    internal fun createState(rulePositions: List<RulePosition>): ParserState {
        check(this.states.contains(rulePositions).not()) { "State already created for $rulePositions" }
        val state = ParserState(StateNumber(this.nextStateNumber++), rulePositions, this)
        this.states[rulePositions] = state
        return state
    }

    internal fun fetchState(rulePositions: List<RulePosition>): ParserState? =
        this.allBuiltStates.firstOrNull { it.rulePositions == rulePositions }

    internal fun fetchCompatibleState(rulePositions: List<RulePosition>): ParserState? {
        val existing = this.allBuiltStates.firstOrNull {
            it.rulePositions.containsAll(rulePositions)
        }
        return existing
    }

    internal fun fetchCompatibleOrCreateState(rulePositions: List<RulePosition>): ParserState =
        fetchCompatibleState(rulePositions) ?: this.createState(rulePositions)

    internal fun createLookaheadSet(includeUP: Boolean, includeEOT: Boolean, matchAny: Boolean, content: Set<RuntimeRule>): LookaheadSet {
        return when {
            content.isEmpty() -> when {
                includeUP && includeEOT.not() && matchAny.not() -> LookaheadSet.UP
                includeUP.not() && includeEOT && matchAny.not() -> LookaheadSet.EOT
                includeUP.not() && includeEOT.not() && matchAny -> LookaheadSet.ANY
                else -> LookaheadSet.EMPTY
            }
            else -> {
                val existing = this.lookaheadSets.firstOrNull {
                    it.includesUP == includeUP &&
                            it.includesEOT == includeEOT &&
                            it.matchANY == matchAny &&
                            it.content == content  //TODO: slow
                }
                if (null == existing) {
                    val num = this.nextLookaheadSetId++
                    val lhs = LookaheadSet(num, includeUP, includeEOT, matchAny, content)
                    this.lookaheadSets.add(lhs)
                    lhs
                } else {
                    existing
                }
            }
        }
    }

    private val createWithParent_cache = mutableMapOf<Pair<Int, Int>, LookaheadSet>()
    fun createWithParent(upLhs: LookaheadSet, runtimeLookahead: LookaheadSet): LookaheadSet {
        return if (upLhs.includesUP) {
            val res = createWithParent_cache[Pair(upLhs.number, runtimeLookahead.number)]
            if (null == res) {
                val lhs = when {
                    LookaheadSet.UP == upLhs -> runtimeLookahead
                    else -> {
                        val content = if (upLhs.includesUP) upLhs.content.union(runtimeLookahead.content) else upLhs.content
                        val eol = upLhs.includesEOT || (upLhs.includesUP && runtimeLookahead.includesEOT)
                        val ma = upLhs.matchANY || (upLhs.includesUP && runtimeLookahead.matchANY)
                        this.createLookaheadSet(false, eol, ma, content)
                    }
                }
                this.createWithParent_cache[Pair(upLhs.number, runtimeLookahead.number)] = lhs
                lhs
            } else {
                res
            }
        } else {
            upLhs
        }
    }

    fun build(): ParserStateSet {
        if (this.preBuilt.not()) {
            this.buildCache.switchCacheOn()
            buildAndTraverse()
            preBuilt = true
            this.buildCache.clearAndOff()
        } else {
            // already built
        }
        return this
    }

    private fun buildAndTraverse2() {
        // prev of startState = null
        // closureDown(startState)
        // FOR each RP in closure
        //   stateX = RP.next
        //   stateX.prev.add( startState )
        //   IF RP.isNotAtEnd THEN
        //     closureDown(stateX)
        //     ...

        //what about merged states!

        val possiblePrev = LazyMutableMapNonNull<ParserState, MutableSet<ParserState?>>() { mutableSetOf() }

        // Enumerate all rule-positions used in state definitions
        val allStateRPs = this.usedRules.flatMap { rr ->
            rr.rulePositions.filter { rp -> rp.isAtStart.not() }
        }
        // compute RPs merged into one state - i.e. same ?
        val allMergedStateRps = allStateRPs.groupBy { rp -> this.buildCache.firstOf(rp, LookaheadSetPart.UP) }.values //TODO: fix parameter to firstOf
        val allMergedStates = allMergedStateRps.map { rps -> this.createState(rps) }

        val stateInfos = this.buildCache.stateInfo()

        // previous of start state is null
        possiblePrev[this.startState].add(null)
        val todo = mutableQueueOf(this.startState)
        while (todo.isNotEmpty) {
            val state = todo.dequeue()
            val stateNexts = state.rulePositions.map { rp -> rp.next().map { rpn -> this.fetchCompatibleState(listOf(rpn)) } }
            //val cls = this.buildCache.closureDownFrom(state.rulePositions)

        }

    }

    private fun buildAndTraverse1() {
        class StatesStack(prevStack: Stack<ParserState>, val state: ParserState) {
            private val hashCode_cache = arrayOf(this.state, *(prevStack.elements.toTypedArray())).contentHashCode()

            val stack: Stack<ParserState> = prevStack
            val prev = stack.peekOrNull() ?: state.stateSet.startState

            fun push(nextState: ParserState): StatesStack = StatesStack(this.stack.push(this.state), nextState)
            fun pushPrev(nextState: ParserState): StatesStack = StatesStack(this.stack.clone(), nextState)
            fun pushPrevPrev(nextState: ParserState): StatesStack = StatesStack(this.stack.pop().stack, nextState)

            val isLooped: Boolean
                get() {
                    return when {
                        stack.size < 1 -> false
                        else -> {
                            val p = Pair(this.state, this.prev)
                            for (i in this.stack.elements.size - 1 downTo 0) {
                                val os = this.stack.elements[i]
                                val op = if (i > 1) this.stack.elements[i - 1] else null
                                val opr = Pair(os, op)
                                if (p == opr) {
                                    return true
                                }
                            }
                            false
                        }
                    }
                }

            override fun hashCode(): Int = hashCode_cache

            override fun equals(other: Any?): Boolean = when {
                this === other -> true
                other !is StatesStack -> false
                this.state != other.state -> false
                else -> this.stack == other.stack
            }

            override fun toString(): String = when {
                this.stack.isEmpty -> "$state-->null"
                else -> "$state-->${stack.elements.reversed().joinToString(separator = "-->") { it.toString() }}-->null"
            }
        }
//TODO: create valid states first ? and/or enable mering of states
        val done = mutableSetOf<StatesStack>()
        val transitions = MutableQueue<Pair<StatesStack, Transition>>()
        var curStack = StatesStack(Stack(), this.startState)
        val s0_trans = curStack.state.transitions(curStack.prev)
        s0_trans.forEach { transitions.enqueue(Pair(curStack, it)) }
        while (transitions.isEmpty.not()) {
            val pair = transitions.dequeue()
            curStack = pair.first
            val tr = pair.second
            // assume we take the transition
            val curState = curStack.state
            val nextState = tr.to
            check(tr.from == curState) { "Error building Automaton" }
            val newStack = when (tr.action) {
                Transition.ParseAction.WIDTH -> curStack.push(nextState)
                Transition.ParseAction.EMBED -> curStack.push(nextState)
                Transition.ParseAction.HEIGHT -> curStack.pushPrev(nextState)
                Transition.ParseAction.GRAFT -> curStack.pushPrevPrev(nextState)
                Transition.ParseAction.GOAL -> curStack
            }
            val newTrans = newStack.state.transitions(newStack.prev)
            if (newStack.isLooped) {
                // do nothing
                val i = 0
            } else {
                if (done.add(newStack)) {
                    newTrans.forEach { transitions.enqueue(Pair(newStack, it)) }
                }
            }
        }
    }

    private fun buildAndTraverse() {
        // this.buildCache.buildCaches()
        val stateInfos = this.buildCache.stateInfo()

        for (si in stateInfos) {
            if (si.rulePositions != this.startState.rulePositions) {
                val state = this.createState(si.rulePositions)
            }
        }
        for (si in stateInfos) {
            val state = this.fetchState(si.rulePositions)!!
            for (ti in si.possibleTrans) {
                val previousStates = ti.prev.map { p -> this.fetchState(p) ?: error("Internal error, state not created for $p") }
                val action = ti.action
                val to = this.fetchState(ti.to) ?: error("Internal error, state not created for ${ti.to}")
                val lookahead = when (action) {
                    Transition.ParseAction.GOAL-> LookaheadSet.EMPTY
                    Transition.ParseAction.WIDTH,
                    Transition.ParseAction.EMBED,
                    Transition.ParseAction.HEIGHT,
                    Transition.ParseAction.GRAFT -> ti.lookaheadSet.lhs(this)
                }
                val upLookahead = when (action) {
                    Transition.ParseAction.GOAL,
                    Transition.ParseAction.WIDTH,
                    Transition.ParseAction.EMBED-> setOf(LookaheadSet.EMPTY)
                    Transition.ParseAction.HEIGHT,
                    Transition.ParseAction.GRAFT -> ti.parentFirstOfNext.map { it.lhs(this) }.toSet() // why a Set ?
                }
                val prevGuard = when (action) {
                    Transition.ParseAction.GOAL,
                    Transition.ParseAction.WIDTH,
                    Transition.ParseAction.EMBED -> null
                    Transition.ParseAction.HEIGHT,
                    Transition.ParseAction.GRAFT -> ti.parent
                }
                val runtimeGuard: Transition.(GrowingNodeIndex, List<RulePosition>?) -> Boolean = { gn, previous -> true } //FIXME
                state.createTransition(previousStates, action, to, lookahead, upLookahead, prevGuard, runtimeGuard)
            }
        }

    }

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