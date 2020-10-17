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

    internal fun createLookaheadSet(content: Set<RuntimeRule>): LookaheadSet {
        return this.stateSet.runtimeRuleSet.createLookaheadSet(content)
    }

    fun widthInto4(): Set<RulePosition> {
        val fsts = this.stateSet.firstTerminals[this.rulePosition]
        return fsts.flatMap { it.rulePositionsAt[0] }.toSet()
    }

    fun heightOrGraftInto3(): Set<RulePosition> {
        val pp = this.stateSet.parentPosition[this.runtimeRule]
        //val ppn = pp.flatMap { it.next() }
        //val cls = ppn.flatMap { rp -> calcClosureLR0(rp) }
        //val f1 = cls.filter { it.rulePosition.runtimeRule.kind === RuntimeRuleKind.GOAL || it.rulePosition.runtimeRule.kind === RuntimeRuleKind.NON_TERMINAL }
        //val f2 = f1.filter { it.rulePosition.items.contains(this.rulePosition.runtimeRule) }
        return pp
    }

    fun growsInto(ancestor: ParserState): Boolean {
       return this.stateSet.growsInto(ancestor.rulePosition,this.rulePosition)
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
                        val heightOrGraftInto = this.heightOrGraftInto3()
                        for (pp in heightOrGraftInto) {
                            if (pp.runtimeRule.kind == RuntimeRuleKind.GOAL) {
                                when {
                                    this.runtimeRule == this.stateSet.runtimeRuleSet.END_OF_TEXT -> {
                                        pp.next().forEach { nrp ->
                                            val ts = this.createGraftTransition3(nrp, pp)
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
                                            val ts = this.createGraftTransition3(nrp, pp)
                                            __graftTransitions.addAll(ts)//, addLh, parentLh))
                                        }
                                    }
                                }
                            } else {
                                when (pp.isAtStart) {
                                    true -> {
                                        pp.next().forEach { nrp ->
                                            val ts = this.createHeightTransition3(nrp, pp)
                                            __heightTransitions.addAll(ts)
                                        }
                                    }
                                    false -> {
                                        pp.next().forEach { nrp ->
                                            val ts = this.createGraftTransition3(nrp, pp)
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
                        val widthInto = this.widthInto4()
                        for (p in widthInto) {
                            val rp = p//.rulePosition
                            val lhsl = listOf(createLookaheadSet(stateSet.fetchOrCreateLookahead(p.atEnd())))// p.lookaheadSetList
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
                                val widthInto = this.widthInto4()
                                for (p in widthInto) {
                                    val rp = p//.rulePosition
                                    val lhsl = listOf(createLookaheadSet(stateSet.fetchOrCreateLookahead(p)))//p.lookaheadSetList
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
        val toRP = RulePosition(rp.runtimeRule, rp.option, RulePosition.END_OF_RULE) //assumes rp is a terminal
        val to = this.stateSet.fetchOrCreateParseState(toRP) //TODO: state also depends on lh (or parentRels!)
        val addLh = lookaheadSetList
        val lh = addLh.lastOrNull { it != LookaheadSet.EMPTY } ?: LookaheadSet.EMPTY
        return Transition(this, to, action, addLh, lh, null) { _, _ -> true }
    }

    private fun createHeightTransition3(toRp: RulePosition, prevGuard: RulePosition): Set<Transition> {
        //val prevGuard = toRp//rulePosition //for height, previous must not match prevGuard
        val action = Transition.ParseAction.HEIGHT
        val to = this.stateSet.fetchOrCreateParseState(toRp)
        val lhcs = this.stateSet.fetchOrCreateFirstAt(toRp)
        val addLh = listOf(to.createLookaheadSet(lhcs))
        val parentLh = LookaheadSet.EMPTY //TODO: remove
        val trs = setOf(Transition(this, to, action, addLh, parentLh, prevGuard) { _, _ -> true })
        return trs
    }

    private fun createGraftTransition3(toRp: RulePosition, prevGuard: RulePosition): Set<Transition> {
        //val prevGuard = toRp//rulePosition //TODO: Parent RP //for graft, previous must match prevGuard
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
        val lhcs = this.stateSet.fetchOrCreateFirstAt(toRp)
        val addLh = listOf(to.createLookaheadSet(lhcs))
        val parentLh = LookaheadSet.EMPTY //TODO: remove
        val trs = setOf(Transition(this, to, action, addLh, parentLh, prevGuard, runtimeGuard))
        return trs
    }

    private fun createEmbeddedTransition(rp: RulePosition, lookaheadSetList: List<LookaheadSet>): Transition {
        val action = Transition.ParseAction.EMBED
        val toRP = RulePosition(rp.runtimeRule, rp.option, RulePosition.END_OF_RULE)
        val to = this.stateSet.fetchOrCreateParseState(toRP)
        val addLh = lookaheadSetList
        val lh = addLh.last { it != LookaheadSet.EMPTY }
        return Transition(this, to, action, addLh, lh, null) { _, _ -> true }
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
