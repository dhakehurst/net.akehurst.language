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

import net.akehurst.language.agl.runtime.graph.GrowingNode
import net.akehurst.language.collections.transitiveClosure

class ParentRelation(
        val stateSet: ParserStateSet,
        val number: Int,
        val rulePosition: RulePosition,
        val lookahead: Set<RuntimeRule>
) {
    /*
        fun growsInto(possibleAncestor:RulePosition, done:Set<ParentRelation> = emptySet()) : Boolean {
            return when {
                done.contains(this) -> false
                this.rulePosition == possibleAncestor -> true
                else -> {
                    val pp = stateSet.parentRelation(this.rulePosition.runtimeRule)
                    pp.any {
                        it.growsInto(possibleAncestor, done+this)
                    }
                }
            }
        }
    */
    override fun hashCode(): Int = stateSet.number + (31 * number)
    override fun equals(other: Any?): Boolean = when (other) {
        is ParentRelation -> this.stateSet.number == other.stateSet.number && this.number == other.number
        else -> false
    }

    override fun toString(): String = "ParentRelation{ $number/${stateSet.number} $rulePosition | $lookahead }"
}

data class ClosureItemWithLookaheadList(
        val parentItem: ClosureItemWithLookaheadList?, //needed for height/graft
        val rulePosition: RulePosition,
        val lookaheadSetList: List<LookaheadSet>
) {
    override fun toString(): String {
        val p = if (null == parentItem) {
            ""
        } else {
            "${parentItem.rulePosition}->"
        }
        return "$p$rulePosition$lookaheadSetList"
    }
}

data class ClosureItemWithLookaheadList2(
        val rulePosition: RulePosition,
        val lookaheadSetList: List<LookaheadSet>
) {
    override fun toString(): String {
        return "$rulePosition$lookaheadSetList"
    }
}

data class ClosureItemWithLookahead(
        val parentItem: ClosureItemWithLookahead?,
        val rulePosition: RulePosition,
        val lookahead: Set<RuntimeRule>
) {
    override fun toString(): String {
        val p = if (null == parentItem) {
            ""
        } else {
            "${parentItem.rulePosition}->"
        }
        return "$p$rulePosition${lookahead}"
    }
}

class ParserState(
        val number: StateNumber,
        val rulePosition: RulePosition,
        val stateSet: ParserStateSet
) {

    companion object {
        val multiRuntimeGuard: Transition.(GrowingNode) -> Boolean = { gn: GrowingNode ->
            val previousRp = gn.currentState.rulePosition
            when {
                previousRp.isAtEnd -> gn.children.size + 1 >= gn.runtimeRule.rhs.multiMin
                previousRp.position == RulePosition.MULIT_ITEM_POSITION -> {
                    //if the to state creates a complete node then min must be >= multiMin
                    if (this.to.isAtEnd) {
                        val minSatisfied = gn.children.size + 1 >= gn.runtimeRule.rhs.multiMin
                        val maxSatisfied = -1 == gn.runtimeRule.rhs.multiMax || gn.children.size + 1 <= gn.runtimeRule.rhs.multiMax
                        minSatisfied && maxSatisfied
                    } else {
                        -1 == gn.runtimeRule.rhs.multiMax || gn.children.size + 1 <= gn.runtimeRule.rhs.multiMax
                    }

                }
                else -> true
            }
        }
        val sListRuntimeGuard: Transition.(GrowingNode) -> Boolean = { gn: GrowingNode ->
            val previousRp = gn.currentState.rulePosition
            when {
                previousRp.isAtEnd -> (gn.children.size / 2) + 1 >= gn.runtimeRule.rhs.multiMin
                previousRp.position == RulePosition.SLIST_ITEM_POSITION -> {
                    //if the to state creates a complete node then min must be >= multiMin
                    if (this.to.isAtEnd) {
                        val minSatisfied = (gn.children.size / 2) + 1 >= gn.runtimeRule.rhs.multiMin
                        val maxSatisfied = -1 == gn.runtimeRule.rhs.multiMax || (gn.children.size / 2) + 1 <= gn.runtimeRule.rhs.multiMax
                        minSatisfied && maxSatisfied
                    } else {
                        -1 == gn.runtimeRule.rhs.multiMax || (gn.children.size / 2) + 1 <= gn.runtimeRule.rhs.multiMax
                    }
                }
                previousRp.position == RulePosition.SLIST_SEPARATOR_POSITION -> {
                    //if the to state creates a complete node then min must be >= multiMin
                    if (this.to.isAtEnd) {
                        val minSatisfied = (gn.children.size / 2) + 1 >= gn.runtimeRule.rhs.multiMin
                        val maxSatisfied = -1 == gn.runtimeRule.rhs.multiMax || (gn.children.size / 2) + 1 < gn.runtimeRule.rhs.multiMax
                        minSatisfied && maxSatisfied
                    } else {
                        -1 == gn.runtimeRule.rhs.multiMax || (gn.children.size / 2) + 1 < gn.runtimeRule.rhs.multiMax
                    }
                }
                else -> true
            }
        }
    }

    //FIXME: this could be memory hungry! maybe should not cache them
    //private var closureCache:MutableMap<ParentRelation,RulePositionClosure> = mutableMapOf()

    internal var transitions_cache: MutableMap<ParserState?, List<Transition>?> = mutableMapOf()

    val parentRelations: Set<ParentRelation> by lazy {
        if (rulePosition.runtimeRule.kind == RuntimeRuleKind.GOAL) {
            emptySet()
        } else {
            this.stateSet.parentRelation(this.rulePosition.runtimeRule)
        }
    }

    val items: Set<RuntimeRule>
        inline get() {
            return this.rulePosition.items
        }
    val runtimeRule: RuntimeRule
        inline get() {
            return this.rulePosition.runtimeRule
        }
    val choice: Int
        inline get() {
            return this.rulePosition.option
        }
    val position: Int
        inline get() {
            return this.rulePosition.position
        }

    val isAtEnd: Boolean
        inline get() {
            return this.rulePosition.isAtEnd
        }

    // rp -> prevLh -> lh
    val lookaheadSetForRulePosition = mutableMapOf<ClosureItemWithLookahead, MutableMap<LookaheadSet, LookaheadSet>>()

    internal fun calcClosure(prevLookaheadSet: LookaheadSet): Set<ClosureItemWithLookaheadList> {
        val lhcs = when {
            this.rulePosition.isAtEnd -> setOf(prevLookaheadSet.content)
            else -> {
                val lhc = this.calcLookaheadSetContent2(this.rulePosition, prevLookaheadSet.content)
                lhc
            }
        }
        val cl = lhcs.flatMap { lhc ->
            val lhs = createLookaheadSet(lhc)
            val clRoot = ClosureItemWithLookaheadList(null, this.rulePosition, listOf(lhs))
            val cl = this.calcClosure(clRoot)
            cl
        }
        return cl.toSet()
    }

    private fun calcClosure(item: ClosureItemWithLookaheadList, path: Set<RulePosition> = mutableSetOf(), items: MutableSet<ClosureItemWithLookaheadList> = mutableSetOf()): Set<ClosureItemWithLookaheadList> {
        return when {
            path.contains(item.rulePosition) -> items
            else -> {
                items.add(item)
                val np = path + item.rulePosition
                val itemRps = item.rulePosition.items.flatMap {
                    it.calcExpectedRulePositions(0)
                }.toSet()
                val new = itemRps.flatMap { rp ->
                    val lhcs = this.calcLookaheadSetContent2(rp, item.lookaheadSetList.last().content)
                    lhcs.flatMap { lhc ->
                        val lhs = createLookaheadSet(lhc)
                        val lhSetList = when {
                            // rp.runtimeRule.kind===RuntimeRuleKind.TERMINAL -> item.lookaheadSetList
                            lhs === LookaheadSet.EMPTY -> item.lookaheadSetList
                            else -> item.lookaheadSetList + lhs
                        }
                        val child = ClosureItemWithLookaheadList(item, rp, lhSetList)
                        calcClosure(child, np, items)
                    }
                }
                items
            }
        }
    }

    internal fun calcClosureLR0(): Set<ClosureItemWithLookaheadList> {
        val lhcs = when {
            this.rulePosition.isAtEnd -> emptySet<Set<RuntimeRule>>()
            else -> {
                val lhc = setOf(this.calcLookaheadSetContent(this.rulePosition, emptySet()))
                lhc
            }
        }
        val cl = lhcs.flatMap { lhc ->
            val lhs = createLookaheadSet(lhc)
            val clRoot = ClosureItemWithLookaheadList(null, this.rulePosition, listOf(lhs))
            val cl = this.calcClosureLR0(clRoot)
            cl
        }
        return cl.toSet()
    }

    private fun calcClosureLR0(item: ClosureItemWithLookaheadList, path: Set<RulePosition> = mutableSetOf(), items: MutableSet<ClosureItemWithLookaheadList> = mutableSetOf()): Set<ClosureItemWithLookaheadList> {
        return when {
            path.contains(item.rulePosition) -> items
            else -> {
                items.add(item)
                val np = path + item.rulePosition
                val itemRps = item.rulePosition.items.flatMap {
                    it.calcExpectedRulePositions(0)
                }.toSet()
                val new = itemRps.flatMap { rp ->
                    val lhcs = this.calcLookaheadSetContent2(rp, emptySet())
                    lhcs.flatMap { lhc ->
                        val lhs = createLookaheadSet(lhc)
                        val lhSetList = when {
                            // rp.runtimeRule.kind===RuntimeRuleKind.TERMINAL -> item.lookaheadSetList
                            lhs === LookaheadSet.EMPTY -> item.lookaheadSetList
                            else -> item.lookaheadSetList + lhs
                        }
                        val child = ClosureItemWithLookaheadList(item, rp, lhSetList)
                        calcClosureLR0(child, np, items)
                    }
                }
                items
            }
        }
    }

    internal fun calcClosureLR1(prevLookaheadSet: LookaheadSet): Set<ClosureItemWithLookahead> {
        val lh = when {
            this.rulePosition.isAtEnd -> prevLookaheadSet.content.toSet()
            else -> {
                val lhc = this.calcLookaheadSetContent(this.rulePosition, prevLookaheadSet.content.toSet())
                lhc
            }
        }
        val root = ClosureItemWithLookahead(null, this.rulePosition, lh)
        val cl = this.calcClosureLR1(root)
        return cl
    }

    private fun calcClosureLR1(item: ClosureItemWithLookahead, path: Set<RulePosition> = mutableSetOf(), items: MutableSet<ClosureItemWithLookahead> = mutableSetOf()): Set<ClosureItemWithLookahead> {
        return when {
            path.contains(item.rulePosition) -> items
            else -> {
                items.add(item)
                val np = path + item.rulePosition
                val itemRps = item.rulePosition.items.flatMap {
                    it.calcExpectedRulePositions(0)
                }.toSet()
                val new = itemRps.flatMap {
                    val lh = this.calcLookaheadSetContent(it, item.lookahead)
                    val child = ClosureItemWithLookahead(item, it, lh)
                    calcClosureLR1(child, np, items)
                }
                items
            }
        }
    }

    internal fun lookaheadSet(cur: LookaheadSet): LookaheadSet {
        //TODO: performance
        val lhc = this.calcLookaheadSetContent(this.rulePosition, cur.content.toSet())
        return createLookaheadSet(lhc)
    }

    /*
        internal fun lookaheadSet(item: LR0ClosureItem, prevLookaheadSet: LookaheadSet): LookaheadSet {
            var lhs = lookaheadSetForRulePosition[item]
            if (null == lhs) {
                lhs = mutableMapOf()
                lookaheadSetForRulePosition[item] = lhs
                var lh = this.calcLookaheadSet(item, prevLookaheadSet)
                lhs[prevLookaheadSet] = lh
                return lh
            } else {
                var lh = lhs[prevLookaheadSet]
                if (null == lh) {
                    lh = this.calcLookaheadSet(item, prevLookaheadSet)
                    lhs[prevLookaheadSet] = lh
                }
                return lh
            }
        }
    */
    internal fun calcLookaheadSetContent(rp: RulePosition, parentLookahead: Set<RuntimeRule>): Set<RuntimeRule> {
        return if (rp.isAtEnd || rp.runtimeRule.kind == RuntimeRuleKind.TERMINAL) {
            parentLookahead
        } else {
            val nextRps = rp.next()
            val content = nextRps.flatMap { nextRp ->
                if (nextRp.isAtEnd || nextRp.runtimeRule.kind == RuntimeRuleKind.TERMINAL) {
                    parentLookahead
                } else {
                    val lh: Set<RuntimeRule> = this.stateSet.runtimeRuleSet.firstTerminals2[nextRp] ?: error("should never happen")
                    if (lh.isEmpty()) {
                        error("should never happen")
                    } else {
                        lh
                    }
                }
            }.toSet()
            content
        }
    }

    internal fun calcLookaheadSetContent2(rp: RulePosition, defaultLh: Set<RuntimeRule>): Set<Set<RuntimeRule>> {
        return if (rp.isAtEnd || rp.runtimeRule.kind == RuntimeRuleKind.TERMINAL) {
            setOf(defaultLh)
        } else {
            val nextRps = rp.next()
            val content = nextRps.map { nextRp ->
                if (nextRp.isAtEnd || nextRp.runtimeRule.kind == RuntimeRuleKind.TERMINAL) {
                    defaultLh
                } else {
                    when {
                        nextRp.runtimeRule.kind === RuntimeRuleKind.GOAL && nextRp.position == 1 -> {
                            this.stateSet.possibleEndOfText
                        }
                        else -> {
                            val lh: Set<RuntimeRule> = this.stateSet.runtimeRuleSet.firstTerminals2[nextRp] ?: error("should never happen")
                            if (lh.isEmpty()) {
                                error("should never happen")
                            } else {
                                lh
                            }
                        }
                    }
                }
            }.toSet()
            content
        }
    }

    internal fun createLookaheadSet(content: Set<RuntimeRule>): LookaheadSet {
        return this.stateSet.runtimeRuleSet.createLookaheadSet(content)
    }

    fun widthInto(previousLh: LookaheadSet): List<Pair<RulePosition, LookaheadSet>> {
        val closureLR1 = this.calcClosureLR1(previousLh)
        val terms = closureLR1.filter { it.rulePosition.runtimeRule.kind == RuntimeRuleKind.TERMINAL }
        return terms.map {
            val lhs = this.createLookaheadSet(it.lookahead)
            Pair(it.rulePosition, lhs)
        }.distinct()
    }

    fun widthInto1(): List<ClosureItemWithLookaheadList> {
        // this does not work because it created H and G transitions with no way to distinguish
        // we need the prev RulePosition to see if this.rulePosition will, ever, grow into it.
        val prs = this.parentRelations
        val cls = when {
            prs.isEmpty() -> calcClosureLR0()
            else -> prs.flatMap {
                //val lhs = createLookaheadSet(it.lookahead)
                this.calcClosureLR0()
            }
        }
        val terms = cls.filter { it.rulePosition.runtimeRule.kind == RuntimeRuleKind.TERMINAL }
        return terms.distinct()
    }

    fun widthInto2(): List<ClosureItemWithLookaheadList> {
        // this does not work because it created H and G transitions with no way to distinguish
        // we need the prev RulePosition to see if this.rulePosition will, ever, grow into it.
        val prs = this.parentRelations
        val cls = when {
            prs.isEmpty() -> calcClosure(LookaheadSet.EMPTY)
            else -> prs.flatMap {
                val lhs = createLookaheadSet(it.lookahead)
                this.calcClosure(lhs)
            }
        }
        val terms = cls.filter { it.rulePosition.runtimeRule.kind == RuntimeRuleKind.TERMINAL }
        return terms.distinct()
    }

    fun widthInto3(prevRp: ParserState): Set<ParentRelation> {
        //handles the up part of the closure.
        return when {
            null == prevRp -> this.parentRelations
            prevRp.isAtEnd -> emptySet()
            else -> this.parentRelations.filter { pr ->
                when {
                    pr.rulePosition == prevRp.rulePosition -> true
                    else -> {
                        val prevClosure = prevRp.createClosure(pr.lookahead) //this.createClosure(prevRp, null)
                        prevClosure.any {
                            it.rulePosition.runtimeRule == pr.rulePosition.runtimeRule //&& it.lookahead == pr.lookahead
                        }
                    }
                }
            }.toSet()
        }
    }

    fun heightOrGraftInto(prevState: ParserState): List<ParentRelation> {
        //handles the up part of the closure.
        return when {
            //null == prevState -> this.parentRelations
            prevState.isAtEnd -> emptyList()
            else -> this.parentRelations.filter { pr ->
                when {
                    pr.rulePosition == prevState.rulePosition -> true
                    else -> {
                        //pr.growsInto(prevState.rulePosition)
                        val prevClosure = prevState.createClosure(pr.lookahead)
                        prevClosure.any {
                            it.rulePosition.runtimeRule == pr.rulePosition.runtimeRule //&& it.lookahead == pr.lookahead
                        }
                    }
                }
            }
        }
    }

    fun heightOrGraftInto3(): Set<RulePosition> {
        val pp = this.stateSet.parentPosition[this.runtimeRule]
        //val ppn = pp.flatMap { it.next() }
        //val cls = ppn.flatMap { rp -> calcClosureLR0(rp) }
        //val f1 = cls.filter { it.rulePosition.runtimeRule.kind === RuntimeRuleKind.GOAL || it.rulePosition.runtimeRule.kind === RuntimeRuleKind.NON_TERMINAL }
        //val f2 = f1.filter { it.rulePosition.items.contains(this.rulePosition.runtimeRule) }
        return pp
    }

    fun heightOrGraftInto4(): Set<Pair<RulePosition, Set<RuntimeRule>>> {
        val pp = this.stateSet.parentPosition[this.runtimeRule]
        val ppn = pp.flatMap { rp ->
            rp.next().map { nextRp ->
                val rr = when {
                    nextRp.isAtEnd &&
                            this.stateSet.userGoalRule == nextRp.runtimeRule -> (this.stateSet.runtimeRuleSet.firstTerminals2[nextRp]
                            ?: emptySet()) + setOf(this.stateSet.runtimeRuleSet.END_OF_TEXT)
                    else -> this.stateSet.runtimeRuleSet.firstTerminals2[nextRp] ?: emptySet()
                }
                Pair(rp, rr)
            }
        }.toSet()
        //val cls = ppn.flatMap { rp -> calcClosureLR0(rp) }
        //val f1 = cls.filter { it.rulePosition.runtimeRule.kind === RuntimeRuleKind.GOAL || it.rulePosition.runtimeRule.kind === RuntimeRuleKind.NON_TERMINAL }
        //val f2 = f1.filter { it.rulePosition.items.contains(this.rulePosition.runtimeRule) }
        return ppn
    }

    /*
     * for this states runtime rule, find where it is used
     */
    fun heightOrGraftInto5(): Set<Pair<RulePosition, Set<RuntimeRule>>> {
        val pp = this.stateSet.parentPosition[this.runtimeRule]
        val ppn = pp.map { rp ->
            val next = this.stateSet.fetchOrCreateNext(rp)
            Pair(rp, next)
        }.toSet()
       return ppn
    }

    fun heightOrGraftInto(prevState: ParserState, previousLh: LookaheadSet): List<Pair<RulePosition, LookaheadSet>> {
        //handles the up part of the closure.
        val lr1 = prevState.calcClosureLR1(previousLh)
        val r = lr1.mapNotNull {
            when (it.rulePosition.runtimeRule.kind) {
                RuntimeRuleKind.EMBEDDED -> {
                    TODO()
                }
                RuntimeRuleKind.TERMINAL -> {
                    if (it.rulePosition.runtimeRule == this.runtimeRule) {
                        if (null == it.parentItem) {
                            TODO()
                        } else {
                            val lhc = it.parentItem.lookahead
                            val lh = this.createLookaheadSet(lhc)
                            Pair(it.parentItem.rulePosition, lh)
                        }
                    } else {
                        null
                    }
                }
                RuntimeRuleKind.GOAL, RuntimeRuleKind.NON_TERMINAL -> {
                    //TODO: do == && == && ==0, rather than object creation
                    val rpAtStart = RulePosition(this.rulePosition.runtimeRule, this.rulePosition.option, 0)
                    if (it.rulePosition == rpAtStart || it.rulePosition.next().contains(this.rulePosition)) {
                        if (null == it.parentItem) {
                            TODO()
                        } else {
                            val lhc = it.parentItem.lookahead
                            val lh = this.createLookaheadSet(lhc)
                            Pair(it.parentItem.rulePosition, lh)
                        }
                    } else {
                        null
                    }
                }
            }
        }
        return r
    }

    internal fun createClosure(parentLookahead: Set<RuntimeRule>): Set<RulePositionWithLookahead> {
        //FIXME: find a faster way to do this....this is a performance issue point
        // create closure from this.rulePosition (root of the state) down
        val rootWlh = RulePositionWithLookahead(this.rulePosition, parentLookahead)
        val closureSet = setOf(rootWlh).transitiveClosure { prnt ->
            val parentRP = prnt.rulePosition
            val parentLh = prnt.lookahead
            parentRP.items.flatMap { rr ->
                val childrenRP = rr.calcExpectedRulePositions(0)
                childrenRP.map { childRP ->
                    val lh = this.calcLookahead(prnt, childRP, parentLh)
                    //val lh = this.stateSet.getLookahead(childRP)
                    RulePositionWithLookahead(childRP, lh)
                }
            }.toSet()
        }
        return closureSet
    }

    fun growsInto(ancestor: ParserState): Boolean {
        val thisStart = RulePosition(this.rulePosition.runtimeRule, this.rulePosition.option, 0)
        return ancestor.calcClosureLR0().any {
            it.rulePosition == thisStart
        }
    }

    fun transitions(previousState: ParserState?): List<Transition> {
        val cache = this.transitions_cache[previousState]
        val trans = if (null == cache) {
            //TODO: remove dependency on previous when calculating transitions! ?
            //val transitions = this.calcTransitions(previousState, lookaheadSet).toList()
            val transitions = this.calcTransitions(previousState).toList()
            this.transitions_cache[previousState] = transitions
            transitions
        } else {
            cache
        }
        // val filtered = this.growsInto(previous)
        return trans
    }

    private val __heightTransitions = mutableSetOf<Transition>()
    private val __graftTransitions = mutableSetOf<Transition>()
    private val __widthTransitions = mutableSetOf<Transition>()
    private val __goalTransitions = mutableSetOf<Transition>()
    private val __embeddedTransitions = mutableSetOf<Transition>()
    private val __transitions = mutableSetOf<Transition>()
    private fun calcTransitions(previousState: ParserState?): Set<Transition> {//, lookaheadSet: LookaheadSet): Set<Transition> { //TODO: add previous in order to filter parent relations
        __heightTransitions.clear()
        __graftTransitions.clear()
        __widthTransitions.clear()
        __goalTransitions.clear()
        __embeddedTransitions.clear()
        __transitions.clear()

        val goal = this.runtimeRule.kind == RuntimeRuleKind.GOAL && this.isAtEnd //from.rulePosition.position==1//from.isAtEnd
        if (goal) {
            val action = Transition.ParseAction.GOAL
            val to = this
            __goalTransitions.add(Transition(this, to, action, emptyList(), LookaheadSet.EMPTY, null) { _, _ -> true })
        } else {
            if (this.isAtEnd) {
                when {
                    this.runtimeRule.kind == RuntimeRuleKind.GOAL -> {
                        TODO("not sure if this ever happens")
                        //end of skip
                        val action = Transition.ParseAction.GOAL
                        val to = this
                        __goalTransitions.add(Transition(this, to, action, emptyList(), LookaheadSet.EMPTY, null) { _, _ -> true })
                    }
                    previousState != null -> {
                        val pS = when {
                            this.stateSet.number != previousState.stateSet.number -> this.stateSet.startState
                            else -> previousState
                        }
                        //val heightOrGraftInto = this.heightOrGraftInto(pS, lookaheadSet)
                        //val heightOrGraftInto = this.heightOrGraftInto(pS)
                        val heightOrGraftInto = this.heightOrGraftInto3()
                        //val heightOrGraftInto = this.parentRelations
                        for (pp in heightOrGraftInto) {
                            //val toRp = p.first //p.rulePosition
                            //val addLh = p.lookaheadSetList
                            //val parentLh = p.lookaheadSetList.lastOrNull { it != LookaheadSet.EMPTY } ?: LookaheadSet.EMPTY
                            if (pp.runtimeRule.kind == RuntimeRuleKind.GOAL) {
                                when {
                                    this.runtimeRule == this.stateSet.runtimeRuleSet.END_OF_TEXT -> {
                                        pp.next().forEach { nrp ->
                                            val ts = this.createGraftTransition3(nrp)
                                            __graftTransitions.addAll(ts)//, addLh, parentLh))
                                        }
                                    }
                                    (this.runtimeRule.kind === RuntimeRuleKind.GOAL && this.stateSet.isSkip) -> {
                                        // must be end of skip. TODO: can do something better than this!
                                        val action = Transition.ParseAction.GOAL
                                        val to = this
                                        __goalTransitions.add(Transition(this, to, action, emptyList(), LookaheadSet.EMPTY, null) { _, _ -> true })
                                    }
                                    else -> {
                                        pp.next().forEach { nrp ->
                                            val ts = this.createGraftTransition3(nrp)
                                            __graftTransitions.addAll(ts)//, addLh, parentLh))
                                        }
                                    }
                                }
                            } else {
                                when (pp.isAtStart) {
                                    true -> {
                                        pp.next().forEach { nrp ->
                                            val ts = this.createHeightTransition3(nrp)
                                            __heightTransitions.addAll(ts)
                                        }
                                    }
                                    false -> {
                                        pp.next().forEach { nrp ->
                                            val ts = this.createGraftTransition3(nrp)
                                            __graftTransitions.addAll(ts)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else -> error("Internal error")
                }
            } else {
                when {
                    (this.runtimeRule.kind == RuntimeRuleKind.GOAL) -> {
                        //val widthInto = this.widthInto(LookaheadSet.EMPTY)
                        //val cl = this.calcClosureLR0().filter {it.rulePosition.runtimeRule.kind===RuntimeRuleKind.TERMINAL }
                        val widthInto = this.widthInto2()
                        for (p in widthInto) {
                            val rp = p.rulePosition
                            val lhsl = p.lookaheadSetList
                            when (rp.runtimeRule.kind) {
                                RuntimeRuleKind.TERMINAL -> {
                                    __widthTransitions.add(this.createWidthTransition(rp, lhsl))
                                }
                                RuntimeRuleKind.EMBEDDED -> {
                                    __embeddedTransitions.add(this.createEmbeddedTransition(rp, lhsl))
                                }
                            }
                        }
                    }
                    previousState != null -> {
                        when {
                            this.stateSet.number != previousState.stateSet.number -> {
                                TODO()
                            }
                            else -> {
                                //val widthInto = this.widthInto(lookaheadSet)
                                val widthInto = this.widthInto2()
                                for (p in widthInto) {
                                    val rp = p.rulePosition
                                    val lhsl = p.lookaheadSetList
                                    when (rp.runtimeRule.kind) {
                                        RuntimeRuleKind.TERMINAL -> {
                                            val ts = this.createWidthTransition(rp, lhsl)
                                            __widthTransitions.add(ts)
                                        }
                                        RuntimeRuleKind.EMBEDDED -> {
                                            val ts = this.createEmbeddedTransition(rp, lhsl)
                                            __embeddedTransitions.add(ts)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else -> error("Internal error")
                }
            }
        }

        //TODO: merge transitions with everything duplicate except lookahead (merge lookaheads)
        //not sure if this should be before or after the h/g conflict test.
/*
        val conflictHeightTransitionPairs = mutableSetOf<Pair<Transition, Transition>>()
        for (trh in __heightTransitions) {
            for (trg in __graftTransitions) {
                if (trg.lookaheadGuard.containsAll(trh.lookaheadGuard)) {
                    val newTr = Transition(trh.from, trh.to, trh.action, trh.lookaheadGuard, trg.prevGuard, trh.runtimeGuard)
                    conflictHeightTransitionPairs.add(Pair(trh, trg))
                }
            }
        }

        val conflictHeightTransitions = conflictHeightTransitionPairs.map {
            val trh = it.first
            val trg = it.second
            Transition(trh.from, trh.to, trh.action, trh.lookaheadGuard, trg.prevGuard, trh.runtimeGuard)
        }
        //Note: need to do the conflict stuff to make multi work, at present!

        //val newHeightTransitions = (heightTransitions - conflictHeightTransitionPairs.map { it.first }) + conflictHeightTransitions
*/

        val groupedWidthTransitions = __widthTransitions.groupBy { Pair(it.to, it.additionalLookaheads) }
        val mergedWidthTransitions = groupedWidthTransitions.map {
            val mLh = if (it.value.size > 1) {
                val mLhC = it.value.map { it.lookaheadGuard.content.toSet() }.reduce { acc, lhc -> acc.union(lhc) }
                this.createLookaheadSet(mLhC)
            } else {
                it.value[0].lookaheadGuard
            }
            val addLh = it.value[0].additionalLookaheads
            Transition(this, it.key.first, Transition.ParseAction.WIDTH, addLh, mLh, null) { _, _ -> true }
        }

        val groupedHeightTransitions = __heightTransitions.groupBy { Triple(it.to, it.prevGuard, it.additionalLookaheads) }
        val mergedHeightTransitions = groupedHeightTransitions.map {
            val mLh = if (it.value.size > 1) {
                val mLhC = it.value.map { it.lookaheadGuard.content.toSet() }.reduce { acc, lhc -> acc.union(lhc) }
                this.createLookaheadSet(mLhC)
            } else {
                it.value[0].lookaheadGuard
            }
            val addLh = it.value[0].additionalLookaheads
            Transition(this, it.key.first, Transition.ParseAction.HEIGHT, addLh, mLh, it.key.second) { _, _ -> true }
        }

        val groupedGraftTransitions = __graftTransitions.groupBy { Triple(it.to, it.prevGuard, it.additionalLookaheads) }
        val mergedGraftTransitions = groupedGraftTransitions.map {
            val mLh = if (it.value.size > 1) {
                val mLhC = it.value.map { it.lookaheadGuard.content.toSet() }.reduce { acc, lhc -> acc.union(lhc) }
                this.createLookaheadSet(mLhC)
            } else {
                it.value[0].lookaheadGuard
            }
            val addLh = it.value[0].additionalLookaheads
            Transition(this, it.key.first, Transition.ParseAction.GRAFT, addLh, mLh, it.key.second, it.value[0].runtimeGuard)
        }

        __transitions.addAll(mergedHeightTransitions)
        __transitions.addAll(mergedGraftTransitions)
        __transitions.addAll(mergedWidthTransitions)

        __transitions.addAll(__goalTransitions)
        __transitions.addAll(__embeddedTransitions)
        return __transitions.toSet()
    }

    private fun createWidthTransition(rp: RulePosition, lookaheadSetList: List<LookaheadSet>): Transition {
        val action = Transition.ParseAction.WIDTH
        val toRP = RulePosition(rp.runtimeRule, rp.option, RulePosition.END_OF_RULE)
        val to = this.stateSet.fetchOrCreateParseState(toRP) //TODO: state also depends on lh (or parentRels!)
        val addLh = lookaheadSetList
        val lh = addLh.lastOrNull { it != LookaheadSet.EMPTY } ?: LookaheadSet.EMPTY
        return Transition(this, to, action, addLh, lh, null) { _, _ -> true }
    }


    private fun createHeightTransition2(parentRP: RulePosition): Set<Transition> {
        val prevGuard = parentRP //for height, previous must not match prevGuard
        val action = Transition.ParseAction.HEIGHT
        val toSet = parentRP.next().map { this.stateSet.fetchOrCreateParseState(it) }
        //val filteredToSet = toSet.filter { this.canGrowInto(it, previous) }
        return toSet.flatMap { to ->
            //val lhcs = to.calcLookaheadSetContent(to.rulePosition)
            val fsts = this.stateSet.runtimeRuleSet.firstTerminals2[to.rulePosition]
            val lhcs = if (null == fsts) setOf(emptySet()) else setOf(fsts)
            lhcs.flatMap { lhc ->
                val addLh = listOf(to.createLookaheadSet(lhc))
                val parentLh = LookaheadSet.EMPTY //TODO: remove
                //if (to.parentRelations.isEmpty()) {
                //val lookaheadGuard = this.calcLookahead(null, this.rulePosition, parentLh)
                setOf(Transition(this, to, action, addLh, parentLh, prevGuard) { _, _ -> true })
                //} else {
                //    to.parentRelations.map { toParent ->
                // val lh = when {
                //     to.isAtEnd -> toParent.lookahead
                //    else -> parentLh//to.stateSet.calcLookahead(parentLh, this.rulePosition)
                // }
                //val lookaheadGuard = lh //parentRelation.lookahead //to.stateSet.calcLookahead(toParent,from.rulePosition) //parentRelation.lookahead //this.calcLookahead(RulePositionWithLookahead(to.rulePosition, parentRelation.lookahead), from.rulePosition, parentRelation.lookahead)
                //        Transition(this, to, action, addLh, parentLh, prevGuard) { _, _ -> true }
                //    }
                // }
            }
        }.toSet()
    }

    private fun createHeightTransition3(toRp: RulePosition): Set<Transition> {
        val prevGuard = toRp//rulePosition //for height, previous must not match prevGuard
        val action = Transition.ParseAction.HEIGHT
        val to = this.stateSet.fetchOrCreateParseState(toRp)
        val lhcs = this.stateSet.fetchOrCreateNext(toRp)
        val addLh = listOf(to.createLookaheadSet(lhcs))
        val parentLh = LookaheadSet.EMPTY //TODO: remove
        val trs = setOf(Transition(this, to, action, addLh, parentLh, prevGuard) { _, _ -> true })
        return trs
    }


    private fun createHeightTransition(parentRP: RulePosition, addLh: List<LookaheadSet>, parentLh: LookaheadSet): Set<Transition> {
        val prevGuard = parentRP //for height, previous must not match prevGuard
        val action = Transition.ParseAction.HEIGHT
        val toSet = parentRP.next().map { this.stateSet.fetchOrCreateParseState(it) }
        //val filteredToSet = toSet.filter { this.canGrowInto(it, previous) }
        return toSet.flatMap { to ->
            if (to.parentRelations.isEmpty()) {
                //val lookaheadGuard = this.calcLookahead(null, this.rulePosition, parentLh)
                setOf(Transition(this, to, action, addLh, parentLh, prevGuard) { _, _ -> true })
            } else {
                to.parentRelations.map { toParent ->
                    val lh = when {
                        to.isAtEnd -> toParent.lookahead
                        else -> parentLh//to.stateSet.calcLookahead(parentLh, this.rulePosition)
                    }
                    val lookaheadGuard = lh //parentRelation.lookahead //to.stateSet.calcLookahead(toParent,from.rulePosition) //parentRelation.lookahead //this.calcLookahead(RulePositionWithLookahead(to.rulePosition, parentRelation.lookahead), from.rulePosition, parentRelation.lookahead)
                    Transition(this, to, action, addLh, parentLh, prevGuard) { _, _ -> true }
                }
            }

        }.toSet()
    }

    private fun createGraftTransition2(parentRp: RulePosition): Set<Transition> {
        val prevGuard = parentRp //TODO: Parent RP //for graft, previous must match prevGuard
        val runtimeGuard: Transition.(GrowingNode, RulePosition?) -> Boolean = { gn, previous ->
            if (null == previous) {
                true
            } else {
                when (previous.runtimeRule.rhs.kind) {
                    RuntimeRuleItemKind.MULTI -> multiRuntimeGuard.invoke(this, gn)
                    RuntimeRuleItemKind.SEPARATED_LIST -> sListRuntimeGuard.invoke(this, gn)
                    else -> true
                }
            }
        }
        val action = Transition.ParseAction.GRAFT
        val toSet = parentRp.next().map { this.stateSet.fetchOrCreateParseState(it) }
        return toSet.flatMap { to ->
            //val lhcs = to.calcLookaheadSetContent(to.rulePosition)
            val fsts = this.stateSet.runtimeRuleSet.firstTerminals2[to.rulePosition]
            val lhcs = if (null == fsts) setOf(emptySet()) else setOf(fsts)
            lhcs.flatMap { lhc ->
                val addLh = listOf(to.createLookaheadSet(lhc))
                val parentLh = LookaheadSet.EMPTY //TODO: remove
                // if (to.parentRelations.isEmpty()) {
                setOf(Transition(this, to, action, addLh, parentLh, prevGuard) { _, _ -> true })
                //} else {
                //    to.parentRelations.map { toParent ->
                // val lh = when {
                //    to.isAtEnd -> toParent.lookahead
                //    else -> parentLh//to.stateSet.calcLookahead(parentLh, this.rulePosition)
                //}
                //val lookaheadGuard = lh //parentRelation.lookahead //to.stateSet.calcLookahead(toParent,from.rulePosition) //parentRelation.lookahead //this.calcLookahead(RulePositionWithLookahead(to.rulePosition, parentRelation.lookahead), from.rulePosition, parentRelation.lookahead)
                //        Transition(this, to, action, addLh, parentLh, prevGuard) { _, _ -> true }
                //    }
                //}
            }
        }.toSet()
    }

    private fun createGraftTransition3(toRp: RulePosition): Set<Transition> {
        val prevGuard = toRp//rulePosition //TODO: Parent RP //for graft, previous must match prevGuard
        val runtimeGuard: Transition.(GrowingNode, RulePosition?) -> Boolean = { gn, previous ->
            if (null == previous) {
                true
            } else {
                when (previous.runtimeRule.rhs.kind) {
                    RuntimeRuleItemKind.MULTI -> multiRuntimeGuard.invoke(this, gn)
                    RuntimeRuleItemKind.SEPARATED_LIST -> sListRuntimeGuard.invoke(this, gn)
                    else -> true
                }
            }
        }
        val action = Transition.ParseAction.GRAFT
        val to = this.stateSet.fetchOrCreateParseState(toRp)
        val lhcs = this.stateSet.fetchOrCreateNext(toRp)
        val addLh = listOf(to.createLookaheadSet(lhcs))
        val parentLh = LookaheadSet.EMPTY //TODO: remove
        val trs = setOf(Transition(this, to, action, addLh, parentLh, prevGuard,runtimeGuard))
        return trs
    }

    private fun createGraftTransition(parentRP: RulePosition, addLh: List<LookaheadSet>, lookaheadSet: LookaheadSet): Set<Transition> {
        val prevGuard = parentRP //for graft, previous must match prevGuard
        val action = Transition.ParseAction.GRAFT
        val toSet = parentRP.next().map { this.stateSet.fetchOrCreateParseState(it) }
        //val filteredToSet = toSet.filter { this.canGrowInto(it, previous) }
        val runtimeGuard: Transition.(GrowingNode, RulePosition?) -> Boolean = { gn, previous ->
            if (null == previous) {
                true
            } else {
                when (previous.runtimeRule.rhs.kind) {
                    RuntimeRuleItemKind.MULTI -> multiRuntimeGuard.invoke(this, gn)
                    RuntimeRuleItemKind.SEPARATED_LIST -> sListRuntimeGuard.invoke(this, gn)
                    else -> true
                }
            }
        }
        return toSet.flatMap { to ->
            if (to.parentRelations.isEmpty()) {
                //val lookaheadGuard = this.calcLookahead(null, this.rulePosition, parentLh)
                setOf(Transition(this, to, action, addLh, lookaheadSet, prevGuard, runtimeGuard))
            } else {
                to.parentRelations.map { toParent ->
                    val lh = when {
                        to.isAtEnd -> toParent.lookahead
                        else -> lookaheadSet//to.stateSet.calcLookahead(parentLh, this.rulePosition)
                    }
                    val lookaheadGuard = lh //toParent.lookahead //to.stateSet.calcLookahead(toParent,from.rulePosition)  //this.calcLookahead(RulePositionWithLookahead(toParent.rulePosition, toParent.lookahead), from.rulePosition, toParent.lookahead)
                    Transition(this, to, action, addLh, lookaheadSet, prevGuard, runtimeGuard)
                }
            }

        }.toSet()
    }

    private fun createEmbeddedTransition(rp: RulePosition, lookaheadSetList: List<LookaheadSet>): Transition {
        val action = Transition.ParseAction.EMBED
        val toRP = RulePosition(rp.runtimeRule, rp.option, RulePosition.END_OF_RULE)
        val to = this.stateSet.fetchOrCreateParseState(toRP)
        val addLh = lookaheadSetList
        val lh = addLh.last { it != LookaheadSet.EMPTY }
        return Transition(this, to, action, addLh, lh, null) { _, _ -> true }
    }

    private fun calcLookahead(parent: RulePositionWithLookahead?, childRP: RulePosition, ifEmpty: Set<RuntimeRule>): Set<RuntimeRule> {
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
                                    val lh: Set<RuntimeRule> = this.stateSet.runtimeRuleSet.firstTerminals2[nextChildRP]
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
                        calcLookahead(null, parent.rulePosition, parent.lookahead) //TODO: why not simply parent.lookahead ?
                    } else {
                        val lh: Set<RuntimeRule> = this.stateSet.runtimeRuleSet.firstTerminals2[nextRP] ?: error("should never happen")
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

    // --- Any ---

    override fun hashCode(): Int {
        return this.number.value + this.stateSet.number * 31
    }

    override fun equals(other: Any?): Boolean {
        return if (other is ParserState) {
            this.stateSet.number == other.stateSet.number && this.number.value == other.number.value
        } else {
            false
        }
    }

    override fun toString(): String {
        return "State(${this.number.value}/${this.stateSet.number}-${rulePosition})"
    }

}
