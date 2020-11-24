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
import net.akehurst.language.collections.Stack
import net.akehurst.language.collections.lazyMapNonNull

data class ParentRelation(
        val rulePosition: RulePosition,
        val lookaheadSet: LookaheadSet
)

data class ClosureItem(
        val parentItem: ClosureItem?, //needed for height/graft
        val rulePosition: RulePosition,
        val next: RulePosition?,
        val lookaheadSet: LookaheadSet
) {

    val prev: Set<RulePosition>
        get() = when {
            null == parentItem -> emptySet()
            else -> parentItem.rulePosition.next().filter { it.isAtEnd.not() }.toSet()
        }

    private fun chain(): String {
        val p = if (null == parentItem) {
            ""
        } else {
            "${parentItem.chain()}->"
        }
        return "$p$rulePosition"
    }

    override fun toString(): String {
        return "${chain()}$lookaheadSet"
    }
}

class ParserStateSet(
        val number: Int,
        val runtimeRuleSet: RuntimeRuleSet,
        val userGoalRule: RuntimeRule,
        val isSkip: Boolean
) {

    private var nextState = 0
    private var nextParentRelation = 0
    var preBuilt = false; private set

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
     * A RulePosition identifies a Parser state.
     * LR(0) states
     * The parentRelation can be used to determine the LR(1) related lookahead
     */
    val states = lazyMapNonNull<List<RulePosition>, ParserState> {
        ParserState(StateNumber(this.nextState++), it, this)
    }

    val allBuiltTransitions: Set<Transition> get() = states.values.flatMap { it.allBuiltTransitions }.toSet()

    val startState: ParserState by lazy {
        val goalRule = RuntimeRuleSet.createGoalRule(userGoalRule)
        val goalRP = listOf(RulePosition(goalRule, 0, 0))
        val startState = this.states[goalRP]
        startState
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

    internal fun createLookaheadSet(content: Set<RuntimeRule>): LookaheadSet {
        return this.runtimeRuleSet.createLookaheadSet(content) //TODO: Maybe cache here rather than in rrs
    }

    fun build(): ParserStateSet {
        val s0 = this.startState
        //val sG = this.states[s0.rulePosition.atEnd()]
        //val trans = s0.transitions(null) //+ sG.transitions(null)
        val done = mutableSetOf<Pair<ParserState, ParserState?>>()//Pair(s0, null))
        val prevStack = Stack<Pair<ParserState, LookaheadSet>>()//.push(Pair(null, LookaheadSet.EOT))
        buildAndTraverse(s0, prevStack, done)
        preBuilt = true
        return s0.stateSet
    }

    private fun buildAndTraverse(curState: ParserState, prevStack: Stack<Pair<ParserState, LookaheadSet>>, done: MutableSet<Pair<ParserState, ParserState?>>) {
        //TODO: using done here does not work, as the pair (state,prev) does not identify the path to each state, could have alternative 'prev-->prev...'
        // prob need to take the closure of each state first or something, like traditional LR SM build
        // see grammar in test_Java8_Singles.Expressions_Type__int
        //for(prev in prevStack.elements) {
        val prevPair = prevStack.peekOrNull()
        val prevSt = prevPair?.first
        val prevLh = prevPair?.second ?: LookaheadSet.EOT
        val dp = Pair(curState, prevSt) //TODO: maybe need LH here also !, maybe done just needs to be the items on the stack!
        if (done.contains(dp)) {
            //do nothing more
        } else {
                val state = curState
                done.add(dp)
                val trans = state.transitions(prevSt)
                for (nt in trans) {
                    val nextState = nt.to
                    when (nt.action) {
                        Transition.ParseAction.WIDTH -> {
                            val newLh = LookaheadSet.EMPTY
                            val np = Pair(state, newLh)
                            buildAndTraverse(nextState, prevStack.push(np), done)
                        }
                        Transition.ParseAction.EMBED -> {
                            val newLh = LookaheadSet.EMPTY
                            val np = Pair(state, newLh)
                            buildAndTraverse(nextState, prevStack.push(np), done)
                        }
                        Transition.ParseAction.HEIGHT -> {
                            val newLh = LookaheadSet.EMPTY
                            val np = Pair(state, newLh)
                            buildAndTraverse(nextState, prevStack, done)
                        }
                        Transition.ParseAction.GRAFT -> {
                            val popped = prevStack.pop()
                            val newLh = popped.item.second
                            val np = Pair(state, newLh)
                            buildAndTraverse(nextState, prevStack.pop().stack, done)
                        }
                        Transition.ParseAction.GOAL -> null;//buildAndTraverse(nextState, prevStack, done)
                    }
                }

        }
        // }
    }

    private fun buildAndTraverse1(curState: ParserState, prevStack: Stack<Pair<ParserState, LookaheadSet>>, done: MutableSet<Pair<ParserState, ParserState?>>) {
        //TODO: using done here does not work, as the pair (state,prev) does not identify the path to each state, could have alternative 'prev-->prev...'
        // prob need to take the closure of each state first or something, like traditional LR SM build
        // see grammar in test_Java8_Singles.Expressions_Type__int
        //for(prev in prevStack.elements) {
        val prevPair = prevStack.peekOrNull()
        val prevSt = prevPair?.first
        val prevLh = prevPair?.second ?: LookaheadSet.EOT
        val dp = Pair(curState, prevSt) //TODO: maybe need LH here also !, maybe done just needs to be the items on the stack!
        if (done.contains(dp)) {
            //do nothing more
        } else {
            val rps = curState.runtimeRules.flatMap { it.rulePositions }.toSet()
            for (rp in rps) {
                val state = this.states[listOf(rp)] //FIXME: this is wrong
                done.add(dp)
                val trans = state.transitions(prevSt)
                for (nt in trans) {
                    val nextState = nt.to
                    when (nt.action) {
                        Transition.ParseAction.WIDTH -> {
                            val newLh = LookaheadSet.EMPTY
                            val np = Pair(state, newLh)
                            buildAndTraverse(nextState, prevStack.push(np), done)
                        }
                        Transition.ParseAction.EMBED -> {
                            val newLh = LookaheadSet.EMPTY
                            val np = Pair(state, newLh)
                            buildAndTraverse(nextState, prevStack.push(np), done)
                        }
                        Transition.ParseAction.HEIGHT -> {
                            val newLh = LookaheadSet.EMPTY
                            val np = Pair(state, newLh)
                            buildAndTraverse(nextState, prevStack, done)
                        }
                        Transition.ParseAction.GRAFT -> {
                            val popped = prevStack.pop()
                            val newLh = popped.item.second
                            val np = Pair(state, newLh)
                            buildAndTraverse(nextState, prevStack.pop().stack, done)
                        }
                        Transition.ParseAction.GOAL -> null;//buildAndTraverse(nextState, prevStack, done)
                    }
                }
            }
        }
        // }
    }

    /*
    internal fun fetchOrCreateParseState(rulePosition: RulePosition): ParserState { //
        val existing = this.states[rulePosition]
        return if (null == existing) {
            val v = ParserState(StateNumber(this.nextState++), rulePosition, this)
            this.states[rulePosition] = v
            v
        } else {
            existing
        }
    }
*/

    internal fun fetch(rulePosition: List<RulePosition>): ParserState {
        return this.states[rulePosition]
    }

    internal fun fetchOrNull(rulePosition: List<RulePosition>): ParserState? {
        return this.states[rulePosition]
    }


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
    }


    internal fun calcClosure(rp: RulePosition, upLhs: LookaheadSet): Set<ClosureItem> {
        val lhsc = calcLookaheadDown(rp, upLhs.content)
        val lhs = createLookaheadSet(lhsc)
        return calcClosure(ClosureItem(null, rp, null, lhs))
    }

    // does not go all the way down to terminals,
    // stops just above,
    // when item.rulePosition.items is a TERMINAL/EMBEDDED
    internal fun calcClosure(item: ClosureItem, items: MutableSet<ClosureItem> = mutableSetOf()): Set<ClosureItem> {
        return when {
            item.rulePosition.isAtEnd -> items
            items.any {
                it.rulePosition == item.rulePosition &&
                        it.lookaheadSet == item.lookaheadSet &&
                        it.parentItem?.lookaheadSet == item.parentItem?.lookaheadSet
            } -> items
            else -> {
                items.add(item)
                for (rr in item.rulePosition.items) {
                    when (rr.kind) {
                        RuntimeRuleKind.TERMINAL,
                        RuntimeRuleKind.EMBEDDED -> {
                            //val chItem = ClosureItem(item, RulePosition(rr, 0, RulePosition.END_OF_RULE), item.lookaheadSet)
                            //items.add(chItem)
                        }
                        RuntimeRuleKind.GOAL,
                        RuntimeRuleKind.NON_TERMINAL -> {
                            val chRps = rr.rulePositionsAt[0]
                            for (chRp in chRps) {
                                val chNext = chRp.next()
                                for (chNx in chNext) {
                                    val lh = firstOf(chNx, item.lookaheadSet.content)
                                    val lhs = this.runtimeRuleSet.createLookaheadSet(lh)
                                    val ci = ClosureItem(item, chRp, chNx, lhs)
                                    calcClosure(ci, items)
                                }
                            }
                        }
                    }
                }
                items
            }
        }
    }

    private val _calcClosureLR0 = mutableMapOf<RulePosition, Set<RulePosition>>()
    private fun calcClosureLR0(rp: RulePosition): Set<RulePosition> {
        var cl = _calcClosureLR0[rp]
        if (null == cl) {
            cl = calcClosureLR0(rp, mutableSetOf())
            _calcClosureLR0[rp] = cl
        }
        return cl
    }

    private fun calcClosureLR0(rp: RulePosition, items: MutableSet<RulePosition>): Set<RulePosition> {
        return when {
            items.contains(rp) -> {
                items
            }
            else -> {
                items.add(rp)
                val itemRps = rp.items.flatMap {
                    it.rulePositionsAt[0]
                }.toSet()
                itemRps.forEach { childRp ->
                    calcClosureLR0(childRp, items)
                }
                items
            }
        }
    }

    private fun calcUsedRules(rule: RuntimeRule, used: MutableSet<RuntimeRule> = mutableSetOf(), done: BooleanArray = BooleanArray(this.runtimeRuleSet.runtimeRules.size)): Set<RuntimeRule> {
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

    fun calcLookaheadDown(rulePosition: RulePosition, ifReachEnd: Set<RuntimeRule>): Set<RuntimeRule> {
        return when {
            rulePosition.isAtEnd -> ifReachEnd
            else -> {
                val next = rulePosition.next()
                next.flatMap {
                    firstOf(it, ifReachEnd)
                }.toSet()
            }
        }
    }

    fun firstOf(rulePosition: RulePosition, ifReachEnd: Set<RuntimeRule>): Set<RuntimeRule> {
        return when {
            rulePosition.isAtEnd -> ifReachEnd
            else -> {
                // this will iterate .next() until end of rule so no need to do it here
                val res = firstOfRpNotEmpty(rulePosition, mutableMapOf(), BooleanArray(this.runtimeRuleSet.runtimeRules.size))
                when (res.needsNext) {
                    false -> res.result
                    else -> res.result + ifReachEnd
                }
            }
        }
    }

    data class FirstOfResult(
            val needsNext: Boolean,
            val result: Set<RuntimeRule>
    )

    fun firstOfRpNotEmpty(rulePosition: RulePosition, doneRp: MutableMap<RulePosition, FirstOfResult>, done: BooleanArray): FirstOfResult {
        var existing = doneRp[rulePosition]
        if (null == existing) {
            /*DEBUG*/ if (rulePosition.isAtEnd) error("Internal Error")
            var needsNext = false
            val result = mutableSetOf<RuntimeRule>()

            var rps = setOf(rulePosition)
            while (rps.isNotEmpty()) {
                val nrps = mutableSetOf<RulePosition>()
                for (rp in rps) {
                    //TODO: handle self recursion, i.e. multi/slist perhaps filter out rp from rp.next or need a 'done' map to results
                    val item = rp.item
                    when {
                        //item is null only when rp.isAtEnd
                        null == item /*rp.isAtEnd*/ -> needsNext = true
                        item.isEmptyRule -> nrps.addAll(rp.next())
                        else -> when (item.kind) {
                            RuntimeRuleKind.GOAL -> TODO()
                            RuntimeRuleKind.TERMINAL -> result.add(item)
                            RuntimeRuleKind.EMBEDDED -> {
                                val embSS = item.embeddedRuntimeRuleSet!!.fetchStateSetFor(item.embeddedStartRule!!)
                                val f = embSS.firstOfNotEmpty(item.embeddedStartRule, doneRp, BooleanArray(item.embeddedRuntimeRuleSet.runtimeRules.size))
                                result.addAll(f.result)
                                if (f.needsNext) {
                                    needsNext = true
                                }
                            }
                            RuntimeRuleKind.NON_TERMINAL -> {
                                val f = firstOfNotEmpty(item, doneRp, done)
                                result.addAll(f.result)
                                if (f.needsNext) nrps.addAll(rp.next())
                            }
                        }
                    }
                }
                rps = nrps
            }
            existing = FirstOfResult(needsNext, result)
            doneRp[rulePosition] = existing
        }
        return existing
    }

    //TODO: use smaller array for done, but would to map rule number!
    private val _firstOfNotEmpty = Array<FirstOfResult?>(this.runtimeRuleSet.runtimeRules.size, { null })
    fun firstOfNotEmpty(rule: RuntimeRule, doneRp: MutableMap<RulePosition, FirstOfResult>, done: BooleanArray): FirstOfResult {
        //fun firstOfNotEmpty(rule: RuntimeRule): FirstOfResult {
        return when {
            0 > rule.number -> when {
                RuntimeRuleSet.GOAL_RULE_NUMBER == rule.number -> TODO()
                RuntimeRuleSet.EOT_RULE_NUMBER == rule.number -> TODO()
                RuntimeRuleSet.SKIP_RULE_NUMBER == rule.number -> TODO()
                RuntimeRuleSet.SKIP_CHOICE_RULE_NUMBER == rule.number -> firstOfNotEmptySafe(rule, doneRp, done)
                RuntimeRuleSet.USE_PARENT_LOOKAHEAD_RULE_NUMBER == rule.number -> TODO()
                else -> error("unsupported rule number $rule")
            }
            done[rule.number] -> _firstOfNotEmpty[rule.number] ?: FirstOfResult(false, emptySet())
            else -> {
                var result = _firstOfNotEmpty[rule.number]
                if (null == result) {
                    done[rule.number] = true
                    result = firstOfNotEmptySafe(rule, doneRp, done)
                    _firstOfNotEmpty[rule.number] = result
                }
                result
            }
        }
    }

    fun firstOfNotEmptySafe(rule: RuntimeRule, doneRp: MutableMap<RulePosition, FirstOfResult>, done: BooleanArray): FirstOfResult {
        var needsNext = false
        val result = mutableSetOf<RuntimeRule>()
        val pos = rule.rulePositionsAt[0]
        for (rp in pos) {
            val item = rp.item
            when {
                null == item -> error("should never happen")
                item.isEmptyRule -> needsNext = true //should not happen
                else -> when (item.kind) {
                    RuntimeRuleKind.GOAL -> error("should never happen")
                    RuntimeRuleKind.EMBEDDED -> {
                        val embSS = item.embeddedRuntimeRuleSet!!.fetchStateSetFor(item.embeddedStartRule!!)
                        val f = embSS.firstOfNotEmpty(item.embeddedStartRule, doneRp, BooleanArray(item.embeddedRuntimeRuleSet.runtimeRules.size))
                        result.addAll(f.result)
                        if (f.needsNext) {
                            needsNext = true
                        }
                    }
                    RuntimeRuleKind.TERMINAL -> result.add(item)
                    RuntimeRuleKind.NON_TERMINAL -> {
                        val f = firstOfRpNotEmpty(rp, doneRp, done)
                        result.addAll(f.result)
                        needsNext = needsNext || f.needsNext
                    }
                }
            }
        }
        return FirstOfResult(needsNext, result)
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