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

package net.akehurst.language.automaton.leftcorner

import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.agl.util.Debug
import net.akehurst.language.collections.MapNotNull
import net.akehurst.language.collections.lazyMutableMapNonNull
import net.akehurst.language.parser.api.RulePosition

internal data class RulePositionUpInfo(
    val context: RulePositionRuntime,

    /**
     * when {
     *  next.isAtEnd -> parentNextNotAtEndFollow
     *  else -> next.firstOf
     * }
     */
    val nextContextFirstOf: LookaheadSetPart,

    /**
     * parent.nextContextFirstOf
     */
    val parentNextContextFirstOf: LookaheadSetPart,

    /**
     * parent.parent.nextNotAtEndFollow
     */
    val parentParentNextNotAtEndExpectedAt: LookaheadSetPart
)

data class FirstTerminalInfo(
    val terminalRule: RuntimeRule,
    val parentExpectedAt: LookaheadSetPart
) {
    private val _hashCode = arrayOf(terminalRule, parentExpectedAt).contentHashCode()
    override fun hashCode(): Int = _hashCode
    override fun equals(other: Any?): Boolean = when {
        other !is FirstTerminalInfo -> false
        other.terminalRule != this.terminalRule -> false
        other.parentExpectedAt != this.parentExpectedAt -> false
        else -> true
    }

    override fun toString(): String = "${terminalRule.tag} $parentExpectedAt"
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

    val graph: ClosureGraph
    val rulePosition: RulePositionRuntime
    val context: RulePositionRuntime
    val expectedAt: LookaheadSetPart
    val parentExpectedAt: LookaheadSetPart

    val children: Set<ClosureItem>
    val parents: Set<ClosureItem>

    //val upInfo: RulePositionUpInfo
    val downInfo: Set<FirstTerminalInfo>

    // there could be multiple different parents although the upInfo is the same
    // parent and thus parentNext can be different
    val parentNext: Set<ParentNext>

    val shortString: List<String>

    fun resolveDown()

    fun toStringRec(done: MutableSet<ClosureItem>): String
    fun shortStringRec(done: Set<ClosureItem>): List<String>

}

internal interface ClosureItemRoot : ClosureItem

internal interface ClosureItemChild : ClosureItem

/**
 * for a RulePosition rp
 * follow == nextNotAtEnd
 */
data class ParentNext(
    /**
     * rp.parent.rulePosition.isAtStart
     */
    val firstPosition: Boolean,
    /**
     * rp.parent.next
     */
    val rulePosition: RulePositionRuntime,
    /**
     * firstOf(rp.parent.next)
     */
    val expectedAt: LookaheadSetPart,
    /**
     * follow(rp.parent.next.parent)
     * == rp.parent.next.parentFollow
     * == follow(rp.parent.rule)
     * (rp.parent.parent == rp.parent.next.parent therefore)
     * == rp.parent.parentFollow
     */
    val parentExpectedAt: LookaheadSetPart
) {
    //override fun hashCode(): Int = rulePosition.hashCode()
    //override fun equals(other: Any?): Boolean =when(other) {
    //    !is ParentNext -> false
    //    else -> other.rulePosition==this.rulePosition
    //}
}

internal class ClosureGraph(
    rootContext: RulePositionRuntime,
    rootRulePosition: RulePositionRuntime,
    rootParentFollow: LookaheadSetPart
) {

    companion object {

        private val firstOf = FirstOf()

        fun expectedAt(rp: RulePositionRuntime, parentExpectedAt: LookaheadSetPart): LookaheadSetPart = when {
            rp.isAtEnd -> parentExpectedAt
            else -> {
                val nexts = rp.next()
                if (Debug.CHECK) check(nexts.isNotEmpty()) { "Internal Error: if not a terminal and not atEnd rulePosition.next() should never be empty" }
                val allNextExpectedAt = nexts.map { firstOf.expectedAt(it, parentExpectedAt) }
                allNextExpectedAt.fold(LookaheadSetPart.EMPTY) { acc, it -> acc.union(it) }
            }
        }

        fun parentNext(parents: Set<ClosureItem>): Set<ParentNext> {
            val result = mutableSetOf<ParentNext>()
            for (parent in parents) {
                val atStart = parent.rulePosition.isAtStart
                val prntNextContextExpectedAt = when (atStart) {
                    true -> parent.parentExpectedAt //parent.parents.map { it.parentFollow}.fold(LookaheadSetPart.EMPTY) { acc, it -> acc.union(it) }
                    false -> LookaheadSetPart.EMPTY
                }
                //TODO: can we just use parent.nextNotAtEnd here ?
                for (prntNext in parent.rulePosition.next()) {
                    val prntNextExpectedAt = firstOf.expectedAt(prntNext, parent.parentExpectedAt)
                    val pn = ParentNext(atStart, prntNext, prntNextExpectedAt, prntNextContextExpectedAt)
                    result.add(pn)
                }
            }
            return result
        }

        abstract class ClosureItemAbstractGraph(
            final override val graph: ClosureGraph,
            final override val rulePosition: RulePositionRuntime,
            final override val context: RulePositionRuntime,
            final override val parentExpectedAt: LookaheadSetPart,
        ) : ClosureItem {

            private var _resolveDownCalled = false

            final override val expectedAt: LookaheadSetPart by lazy { expectedAt(rulePosition, parentExpectedAt) }

            override val children: Set<ClosureItem> by lazy { this.graph.childrenOf(this) }
            override val parents: Set<ClosureItem> by lazy { graph.parentsOf(this) }

            override val shortString: List<String> get() = this.shortStringRec(mutableSetOf())

            override lateinit var downInfo: Set<FirstTerminalInfo>

            override val parentNext: Set<ParentNext> by lazy {
                parentNext(this.graph.parentsOf(this))
            }

            override fun resolveDown() {
                if (_resolveDownCalled) {
                    // do nothing, terminate recursion
                    // this.downInfo already set to empty
                } else {
                    this.downInfo = linkedSetOf()
                    this._resolveDownCalled = true
                    val children = this.children //graph.startChildrenOf(this)
                    if (children.isEmpty()) {
                        when {
                            this.rulePosition.isTerminal -> {
                                val firstTerminalInfo = FirstTerminalInfo(this.rulePosition.rule, this.parentExpectedAt)
                                (this.downInfo as MutableSet).add(firstTerminalInfo)
                            }

                            else -> Unit //error("Internal Error: should be terminal or embedded")
                        }
                    } else {
                        for (child in children) {
                            child.resolveDown()
                            if (child.rulePosition.isAtStart || child.rulePosition.isTerminal) {
                                (this.downInfo as MutableSet).addAll(child.downInfo)
                            }
                        }
                    }
                }
            }

            override fun shortStringRec(done: Set<ClosureItem>): List<String> {
                val rp = this.rulePosition
                val rr = rp.rule as RuntimeRule
                val str = when {
                    rr.isGoal -> "G"
                    rr.isEmptyTerminal -> "<E>"
                    rr.isEmptyListTerminal -> "<EL>"
                    rr.isNonTerminal -> when (rr.rhs) {
                        is RuntimeRuleRhsGoal -> rr.tag
                        is RuntimeRuleRhsConcatenation -> when {
                            rp.isAtEnd -> "${rr.tag}[ER]"
                            else -> "${rr.tag}[${rp.position}]"
                        }

                        is RuntimeRuleRhsChoice -> "${rr.tag}.${rp.option}"
                        is RuntimeRuleRhsListSimple -> when {
                            rp.isAtStart -> rr.tag + ".b"
                            rp.position == RulePosition.POSITION_MULIT_ITEM -> rr.tag + ".i"
                            rp.isAtEnd -> rr.tag + ".e"
                            else -> TODO()
                        }

                        is RuntimeRuleRhsListSeparated -> when {
                            rp.isAtStart -> rr.tag + ".b"
                            rp.position == RulePosition.POSITION_SLIST_SEPARATOR -> rr.tag + ".s"
                            rp.position == RulePosition.POSITION_SLIST_ITEM -> rr.tag + ".i"
                            rp.isAtEnd -> rr.tag + ".e"
                            else -> TODO()
                        }

                        else -> TODO()
                    }

                    else -> rr.tag
                }
                return if (done.contains(this)) {
                    val fp = parents.filter { done.contains(it).not() }
                    when (this) {
                        is ClosureItemRootGraph -> listOf(str)
                        is ClosureItemChildGraph -> fp.flatMap { p ->
                            p.shortStringRec(done).map { "$it-$str" }
                        }

                        else -> error("Internal Error: subtype of ClosureItemAbstractGraph not handled - ${this::class.simpleName}")
                    }
                } else {
                    val newDone = done + this

                    when (this) {
                        is ClosureItemRootGraph -> listOf(str)
                        is ClosureItemChildGraph -> parents.flatMap { p ->
                            p.shortStringRec(newDone).map { "$it-$str" }
                        }

                        else -> error("Internal Error: subtype of ClosureItemAbstractGraph not handled - ${this::class.simpleName}")
                    }
                }
            }

            val _id = arrayOf(rulePosition, context, parentExpectedAt)
            override fun hashCode(): Int = _id.contentHashCode()
            override fun equals(other: Any?): Boolean = when {
                other !is ClosureItem -> false
                this.rulePosition != other.rulePosition -> false
                this.context != other.context -> false
                this.parentExpectedAt != other.parentExpectedAt -> false
                else -> true
            }

            override fun toString(): String = "$rulePosition[${parentExpectedAt}]"//{${upInfo.parentNextContextFirstOf}}"

        }

        class ClosureItemRootGraph(
            graph: ClosureGraph,
            context: RulePositionRuntime,
            rulePosition: RulePositionRuntime,
            parentExpectedAt: LookaheadSetPart
        ) : ClosureItemAbstractGraph(graph, rulePosition, context, parentExpectedAt), ClosureItemRoot {

            override val isRoot: Boolean = true

            override fun toStringRec(done: MutableSet<ClosureItem>): String = "$rulePosition"

        }

        class ClosureItemChildGraph(
            graph: ClosureGraph,
            rulePosition: RulePositionRuntime,
            context: RulePositionRuntime,
            parentExpectedAt: LookaheadSetPart
        ) : ClosureItemAbstractGraph(graph, rulePosition, context, parentExpectedAt), ClosureItemChild {

            override val isRoot: Boolean = false

            override fun toStringRec(done: MutableSet<ClosureItem>): String {
                return if (done.contains(this)) {
                    "$rulePosition..."
                } else {
                    done.add(this)
                    parents.joinToString("\n") {
                        val s = it.toStringRec(done)
                        "$rulePosition-->$s"
                    }
                }

            }

        }

    }

    private val _childrenOf = lazyMutableMapNonNull<ClosureItem, MapNotNull<Int, MutableSet<ClosureItem>>> { lazyMutableMapNonNull { linkedSetOf() } }
    private val _parentsOf = lazyMutableMapNonNull<ClosureItem, MutableSet<ClosureItem>> { linkedSetOf() }

    // so we can test ClosureGraph
    internal val parentsOf: Map<ClosureItem, Set<ClosureItem>> get() = _parentsOf

    //val rootUpInfo = RulePositionUpInfo(
    //    context = rootContext,
    //    nextContextFirstOf = nextNotAtEndFollow(rootRulePosition, rootNextContextFirstOf),
    //    parentNextContextFirstOf = rootParentNextContextFirstOf,
    //    parentParentNextNotAtEndFollow = rootParentParentNextContextFirstOf
    //)

    val root = ClosureItemRootGraph(this, rootContext, rootRulePosition, rootParentFollow)

    val nonRootClosures: Set<ClosureItem>
        get() {
            //create copy, because queries to _parentsOf might create additional entries
            // and thus a ConcurrentModificationException whilst iterating over nonRootClosures
            return _parentsOf.keys.toMutableSet()
        }

    fun resolveAllChildParentInfo() {
        this.root.resolveDown()
        for (cls in this.nonRootClosures) {
            cls.resolveDown()
        }
    }

    fun childrenOf(parent: ClosureItem): Set<ClosureItem> = this._childrenOf[parent][parent.rulePosition.position]
    fun parentsOf(child: ClosureItem): Set<ClosureItem> = this._parentsOf[child]

    fun addParentOf(child: ClosureItemChild, parent: ClosureItem): Boolean {
        this._childrenOf[parent][parent.rulePosition.position].add(child)
        return _parentsOf[child].add(parent)
    }


    /**
     * create child and link to parents
     * return child if it was new
     * return null if a new closure was not created
     */
    fun addChild(parent: ClosureItem, childRulePosition: RulePositionRuntime): ClosureItemChild? {
        val childContext = when {
            parent.rulePosition.isAtStart -> parent.context
            else -> parent.rulePosition
        }
        val childParentExpectedAt = parent.expectedAt //nextContextFirstOf(childRulePosition, parent.follow)
        val child = ClosureItemChildGraph(this, childRulePosition, childContext, childParentExpectedAt)
        val added = this.addParentOf(child, parent)
        return if (added) child else null
    }

}