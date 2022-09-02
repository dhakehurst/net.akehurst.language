/**
 * Copyright (C) 2022 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

import net.akehurst.language.agl.agl.automaton.FirstOf
import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleListKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsItemsKind
import net.akehurst.language.agl.util.Debug
import net.akehurst.language.collections.lazyMutableMapNonNull

internal data class RulePositionUpInfo(
    val context: RulePosition,

    /**
     * when {
     *  next.isAtEnd -> nextContextFollow
     *  else -> next.follow
     * }
     */
    val nextNotAtEndFollow: LookaheadSetPart,

    /**
     * parent.nextNotAtEndFollow
     */
    val nextContextFollow: LookaheadSetPart
)

internal data class FirstTerminalInfo(
    val terminalRule: RuntimeRule,
    val nextContextFollow: LookaheadSetPart
) {
    override fun toString(): String = "${terminalRule.tag}[$nextContextFollow]"
}

/**
 * Identified by :
 *  - context, used for prev guard for transition
 *  - rulePosition, target (WIDTH) if terminal
 *  - parentNextFollow, used for lookahead guard (WIDTH)
 *  - parentOfInfo (HEIGHT/GRAFT)
 *  -- parentFollowAtEnd, used for lookahead up (HEIGHT)
 *  -- parentNextInfo, target (HEIGHT/GRAFT)
 *  --- parentNext, target (HEIGHT/GRAFT)
 *  --- parentNextFollow guard (HEIGHT/GRAFT)
 *  -- parentRulePosition, used for HEIGHT/GRAFT decision
 */
internal interface ClosureItem {
    val isRoot: Boolean

    val ffc: FirstFollowCache3
    val graph: ClosureGraph
    val rulePosition: RulePosition

    val children: Set<ClosureItem>

    val upInfo: RulePositionUpInfo
    val downInfo: Set<FirstTerminalInfo>

    // there could be multiple different parents although the upInfo is the same
    // parent and thus parentNext can be different
    val parentNext: Set<ParentNext>

    val shortString: List<String>

    /**
     * create child and link to parents
     * return child if it was new
     */
    fun createAndAddChild(childRulePosition: RulePosition): ClosureItemChild?

    //fun resolveUp()
    fun resolveDown()

    fun toStringRec(done: MutableSet<ClosureItem>): String
    fun shortStringRec(done: Set<ClosureItem>): List<String>

}

internal interface ClosureItemRoot : ClosureItem

internal interface ClosureItemChild : ClosureItem

internal data class ParentNext(
    /**
     * parent.rulePosition.isAtStart
     */
    val firstPosition: Boolean,
    /**
     * parent.next
     */
    val rulePosition: RulePosition,
    /**
     * parent.next.isAtEnd -> nextContextFollow
     * else -> parent.next.follow
     */
    val follow: LookaheadSetPart,
    /**
     * parent.isAtStart -> parent.nextContextFollow
     * else -> EMPTY
     */
    val parentNextContextFollow: LookaheadSetPart
) {
    //override fun hashCode(): Int = rulePosition.hashCode()
    //override fun equals(other: Any?): Boolean =when(other) {
    //    !is ParentNext -> false
    //    else -> other.rulePosition==this.rulePosition
    //}
}

internal class ClosureGraph(
    ffc: FirstFollowCache3,
    rootContext: RulePosition,
    rootRulePosition: RulePosition,
    rootNextContextFollow: LookaheadSetPart
) {

    companion object {

        fun nextNotAtEndFollow(
            ffc: FirstFollowCache3,
            rulePosition: RulePosition,
            nextContextFollow: LookaheadSetPart
        ): LookaheadSetPart = when {
            rulePosition.isTerminal -> nextContextFollow
            else -> {
                if (Debug.CHECK) check(rulePosition.isAtEnd.not()) { "Internal Error: rulePosition of ClosureItem should never be at end" }
                val nexts = rulePosition.next()
                val allNextFollow = nexts.map { next ->
                    when {
                        next.isAtEnd -> nextContextFollow
                        else -> FirstOf(ffc.stateSet).expectedAt(next, nextContextFollow)
                    }
                }
                allNextFollow.fold(LookaheadSetPart.EMPTY) { acc, it -> acc.union(it) }
            }
        }

        fun parentNext(graph: ClosureGraph, parents: Set<ClosureItem>): Set<ParentNext> {
            val result = mutableSetOf<ParentNext>()
            for (parent in parents) {
                val gpInfo = when (parent) {
                    is ClosureItemRoot -> graph.rootUpInfo
                    is ClosureItemChild -> parent.upInfo
                    else -> error("Internal Error: subtype ${parent::class.simpleName} not handled")
                }
                val atStart = parent.rulePosition.isAtStart
                val prntNextContextFollow = when (atStart) {
                    true -> gpInfo.nextContextFollow
                    false -> LookaheadSetPart.EMPTY
                }
                //TODO: can we just use parent.nextNotAtEnd here ?
                for (prntNext in parent.rulePosition.next()) {
                    val prntNextFollow = when (prntNext.isAtEnd) {
                        true -> gpInfo.nextContextFollow
                        else -> FirstOf(parent.ffc.stateSet).expectedAt(prntNext, gpInfo.nextContextFollow)
                    }
                    val pn = ParentNext(atStart, prntNext, prntNextFollow, prntNextContextFollow)
                    result.add(pn)
                }
            }
            return result
        }

        abstract class ClosureItemAbstractGraph(
            final override val ffc: FirstFollowCache3,
            final override val graph: ClosureGraph,
            final override val rulePosition: RulePosition,
            final override val upInfo: RulePositionUpInfo
        ) : ClosureItem {

            private var _resolveDownCalled = false
            protected var _resolveUpCalled = false

            override val children: Set<ClosureItem> get() = this.graph.childrenOf(this)

            override val shortString: List<String> get() = this.shortStringRec(mutableSetOf())

            override fun createAndAddChild(childRulePosition: RulePosition): ClosureItemChild? {
                val child = ClosureItemChildGraph(this.ffc, this.graph, childRulePosition, this)
                val added = this.graph.addParentOf(child, this)
                return if (added) child else null
            }

            override lateinit var downInfo: Set<FirstTerminalInfo>

            override val parentNext: Set<ParentNext> by lazy {
                parentNext(this.graph, this.graph.parentsOf(this))
            }

            override fun resolveDown() {
                if (Debug.CHECK) check(_resolveUpCalled) { "resolveUp() must be called first" }
                if (_resolveDownCalled) {
                    // do nothing, terminate recursion
                    // this.downInfo already set to empty
                } else {
                    this.downInfo = mutableSetOf()
                    this._resolveDownCalled = true
                    val children = graph.childrenOf(this)
                    if (children.isEmpty()) {
                        //assume this is a terminal
                        val firstTerminalInfo = when {
                            this.rulePosition.isTerminal -> FirstTerminalInfo(this.rulePosition.runtimeRule, this.upInfo.nextContextFollow)
                            this.rulePosition.isEmbedded -> FirstTerminalInfo(this.rulePosition.runtimeRule, this.upInfo.nextContextFollow)
                            else -> error("Internal Error: should be terminal or embedded")
                        }
                        (this.downInfo as MutableSet).add(firstTerminalInfo)

                    } else {
                        for (child in children) {
                            child.resolveDown()
                            (this.downInfo as MutableSet).addAll(child.downInfo)
                        }
                    }
                }
            }

            override fun shortStringRec(done: Set<ClosureItem>): List<String> {
                val rp = this.rulePosition
                val rr = rp.runtimeRule
                val str = when {
                    rr.isNonTerminal && rr.rhs.listKind == RuntimeRuleListKind.SEPARATED_LIST -> when {
                        rp.isAtStart -> rr.tag + 'b'
                        rp.position == RulePosition.POSITION_SLIST_SEPARATOR -> rr.tag + 's'
                        rp.position == RulePosition.POSITION_SLIST_ITEM -> rr.tag + 'i'
                        rp.isAtEnd -> rr.tag + 'e'
                        else -> TODO()
                    }

                    rr.isNonTerminal && rr.rhs.itemsKind == RuntimeRuleRhsItemsKind.CHOICE -> "${rr.tag}${rp.option}"
                    else -> rr.tag
                }
                return if (done.contains(this)) {
                    listOf("...${str}")
                } else {
                    val newDone = done + this
                    when (this) {
                        is ClosureItemRootGraph -> listOf(str)
                        is ClosureItemChildGraph -> setOf(parent).flatMap { p ->
                            //if (done.contains(p)) {
                            //    listOf("...${str}")
                            //} else {
                            p.shortStringRec(newDone).map { "$it-$str" }
                            //}
                        }

                        else -> error("Internal Error: subtype of ClosureItemAbstractGraph not handled - ${this::class.simpleName}")
                    }
                }
            }

            val _id = arrayOf(rulePosition, upInfo.context, upInfo.nextContextFollow, upInfo.nextNotAtEndFollow)
            override fun hashCode(): Int = _id.contentHashCode()
            override fun equals(other: Any?): Boolean = when {
                other !is ClosureItem -> false
                this.rulePosition != other.rulePosition -> false
                this.upInfo.context != other.upInfo.context -> false
                this.upInfo.nextContextFollow != other.upInfo.nextContextFollow -> false
                this.upInfo.nextNotAtEndFollow != other.upInfo.nextNotAtEndFollow -> false
                else -> true
            }

            override fun toString(): String = "$rulePosition[${upInfo.nextNotAtEndFollow}]{${upInfo.nextContextFollow}}"

        }

        class ClosureItemRootGraph(
            ffc: FirstFollowCache3,
            graph: ClosureGraph,
            rulePosition: RulePosition
        ) : ClosureItemAbstractGraph(ffc, graph, rulePosition, graph.rootUpInfo), ClosureItemRoot {

            override val isRoot: Boolean = true

            override fun toStringRec(done: MutableSet<ClosureItem>): String = "$rulePosition"

        }

        class ClosureItemChildGraph(
            ffc: FirstFollowCache3,
            graph: ClosureGraph,
            rulePosition: RulePosition,
            val parent: ClosureItem
        ) : ClosureItemAbstractGraph(
            ffc, graph, rulePosition, resolveUpInfo(ffc, rulePosition, parent)
        ), ClosureItemChild {

            companion object {
                fun resolveUpInfo(ffc: FirstFollowCache3, rulePosition: RulePosition, parent: ClosureItem): RulePositionUpInfo {
                    val gpInfo = parent.upInfo
                    val atStart = parent.rulePosition.isAtStart
                    val childContext = when {
                        atStart -> gpInfo.context
                        else -> parent.rulePosition
                    }
                    val childNextContextFollow = gpInfo.nextNotAtEndFollow
                    val childNextNotAtEndFollow = nextNotAtEndFollow(ffc, rulePosition, childNextContextFollow)
                    return RulePositionUpInfo(childContext, childNextNotAtEndFollow, childNextContextFollow)
                }
            }

            override val isRoot: Boolean = false

            override fun toStringRec(done: MutableSet<ClosureItem>): String {
                return if (done.contains(this)) {
                    "$rulePosition..."
                } else {
                    done.add(this)
                    setOf(parent).joinToString("\n") {
                        val s = it.toStringRec(done)
                        "$rulePosition-->$s"
                    }
                }

            }

        }

    }

    private val _childrenOf = lazyMutableMapNonNull<ClosureItem, MutableSet<ClosureItem>> { mutableSetOf() }
    private val _parentsOf = lazyMutableMapNonNull<ClosureItem, MutableSet<ClosureItem>> { mutableSetOf() }

    val rootUpInfo = RulePositionUpInfo(
        context = rootContext,
        nextNotAtEndFollow = nextNotAtEndFollow(ffc, rootRulePosition, rootNextContextFollow), //nextContext, nextContextFollow),
        nextContextFollow = rootNextContextFollow
    )

    val root = ClosureItemRootGraph(ffc, this, rootRulePosition) //rootNextContext, rootNextContextFollow)

    val nonRootClosures: Set<ClosureItem> get() = _parentsOf.keys

    fun resolveAllChildParentInfo() {
        this.root.resolveDown()
        for (cls in this.nonRootClosures) {
            cls.resolveDown()
        }
    }

    fun childrenOf(child: ClosureItem): Set<ClosureItem> = this._childrenOf[child]
    fun parentsOf(child: ClosureItem): Set<ClosureItem> = this._parentsOf[child]

    fun addParentOf(child: ClosureItemChild, extraParent: ClosureItem): Boolean {
        this._childrenOf[extraParent].add(child)
        return _parentsOf[child].add(extraParent)
    }


}