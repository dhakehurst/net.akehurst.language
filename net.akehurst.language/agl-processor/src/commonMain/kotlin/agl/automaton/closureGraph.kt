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

import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.agl.runtime.structure.RuntimeRuleListKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsItemsKind
import net.akehurst.language.agl.util.Debug
import net.akehurst.language.collections.lazyMutableMapNonNull
import net.akehurst.language.collections.mutableQueueOf

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
    val ffc: FirstFollowCache3
    val graph: ClosureGraph
    val context: RulePosition
    val rulePosition: RulePosition

    val nextContext: Set<RulePosition>

    /**
     * when {
     *  next.isAtEnd -> nextContextFollow
     *  else -> next.follow
     * }
     */
    val nextNotAtEndFollow: FirstFollowCache3.Companion.FollowDeferred

    val parents: Set<ClosureItem>

    /**
     * parent.nextNotAtEndFollow
     */
    val nextContextFollow: FirstFollowCache3.Companion.FollowDeferred

//            val followAtEnd:FollowDeferred

    /**
     * context, rulePosition, nextNotAtEndFollow, parentFollowAtEnd, parentNextNotAtEndFollow
     */
    val _id: Array<*>

    val shortString: List<String>

    fun needsNext(childNeedsNext: Boolean): Boolean

    /**
     * create child but do not link to parents
     */
    fun createChild(childRulePosition: RulePosition): ClosureItemChild

    /**
     * link child to this as parent
     */
    fun addChild(child: ClosureItemChild): Boolean

    fun toStringRec(done: MutableSet<ClosureItem>): String
    fun shortStringRec(done: Set<ClosureItem>): List<String>

}

internal interface ClosureItemRoot : ClosureItem {

}

internal data class ParentNext(
    /**
     * parent.rulePosition.isAtStart
     */
    val firstPosition:Boolean,
    /**
     * parent.next
     */
    val rulePosition: RulePosition,
    /**
     * parent.next.isAtEnd -> nextContextFollow
     * else -> parent.next.follow
     */
    val follow: FirstFollowCache3.Companion.FollowDeferred,
    /**
     * parent.isAtStart -> parent.nextContextFollow
     * else -> EMPTY
     */
    val parentNextContextFollow: FirstFollowCache3.Companion.FollowDeferred
    ) {
    //override fun hashCode(): Int = rulePosition.hashCode()
    //override fun equals(other: Any?): Boolean =when(other) {
    //    !is ParentNext -> false
    //    else -> other.rulePosition==this.rulePosition
    //}
}

internal interface ClosureItemChild : ClosureItem {
    /**
     * all parents will/must have same rulePosition
     */
    //val parentRulePosition: RulePosition

    /**
     * all parents will/must have same context RulePosition
     */
    //val parentContext: RulePosition
    //val parentNextContext: Set<RulePosition>

    /**
     * parent.parentNextFollow
     */
    //val parentNextContextFollow: FirstFollowCache3.Companion.FollowDeferred

    val parentNext: Set<ParentNext>

    //val parentOfInfo: FirstFollowCache3.Companion.ParentOfInContext

    fun addParent(extraParent: ClosureItem)
}

internal interface ClosurePath {
    val bottom: ClosureItem
    val top: ClosureItem
    val topNeedsNext: Boolean
}

internal class ClosureGraph(
    ffc: FirstFollowCache3,
    rootContext: RulePosition,
    rootRulePosition: RulePosition,
    rootNextContext: Set<RulePosition>,
    rootNextContextFollow: FirstFollowCache3.Companion.FollowDeferred
) {

    companion object {
        abstract class ClosureItemAbstract(
            override val ffc: FirstFollowCache3,
            override val graph: ClosureGraph,
        ) : ClosureItem {

            /**
             * when {
             *   rulePosition.isEmpty -> true
             *   childNeedsNext -> when {
             *      rulePosition.isAtEnd -> true
             *      rulePosition.next().any { it.needsNext }
             *   }
             *   else -> false
             * }
             */
            override fun needsNext(childNeedsNext: Boolean): Boolean = when {
                rulePosition.isEmptyRule -> true
                childNeedsNext -> when {
                    rulePosition.isAtEnd -> true
                    else -> {
                        val nexts = rulePosition.next()
                        nexts.any { nxt ->
                            when {
                                nxt.isAtEnd -> true
                                else -> ffc.needsNext(context, nxt)
                            }
                        }
                    }
                }

                else -> false
            }

            override val nextNotAtEndFollow: FirstFollowCache3.Companion.FollowDeferred by lazy {
                when {
                    rulePosition.isTerminal -> nextContextFollow
                    else -> {
                        if (Debug.CHECK) check(rulePosition.isAtEnd.not()) { "Internal Error: rulePosition of ClosureItem should never be at end" }
                        val nexts = rulePosition.next()
                        val allNextFollow = nexts.map { next ->
                            when {
                                next.isAtEnd -> nextContextFollow
                                else -> FirstFollowCache3.Companion.FollowDeferredCalculation(ffc, context, next, nextContext, nextContextFollow)
                            }
                        }
                        FirstFollowCache3.Companion.FollowDeferredComposite.constructOrDelegate(allNextFollow.toSet())
                    }
                }
            }

            override val shortString: List<String> get() = this.shortStringRec(mutableSetOf())
            val longString get() = toStringRec(mutableSetOf())

            override fun createChild(childRulePosition: RulePosition): ClosureItemChild =
                ClosureChildGraph(this.ffc, this.graph, this, childRulePosition)

            override fun addChild(child: ClosureItemChild): Boolean = this.graph.addParentOf(child, this)

            override fun hashCode(): Int = this._id.contentDeepHashCode()
            override fun equals(other: Any?): Boolean = when (other) {
                !is ClosureItem -> false
                else -> this._id.contentDeepEquals(other._id)
            }

            abstract override fun toString(): String

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
                        is ClosureRootGraph -> listOf(str)
                        else -> parents.flatMap { p ->
                            //if (done.contains(p)) {
                            //    listOf("...${str}")
                            //} else {
                            p.shortStringRec(newDone).map { "$it-$str" }
                            //}
                        }
                    }
                }
            }
        }

        class ClosureRootGraph(
            ffc: FirstFollowCache3,
            graph: ClosureGraph,
            override val context: RulePosition,
            override val rulePosition: RulePosition,
            override val nextContext: Set<RulePosition>,
            override val nextContextFollow: FirstFollowCache3.Companion.FollowDeferred,
        ) : ClosureItemAbstract(ffc, graph), ClosureItemRoot {
            override val parents: Set<ClosureItem> = emptySet() // get() = error("ClosureItemRoot has no parent")

            override val _id = arrayOf(rulePosition, nextContextFollow)
            override fun toStringRec(done: MutableSet<ClosureItem>): String = "$rulePosition[$nextContextFollow]-|-$context"

            override fun toString(): String = "$rulePosition($nextContextFollow)[$nextNotAtEndFollow]"
        }

        class ClosureChildGraph(
            ffc: FirstFollowCache3,
            graph: ClosureGraph,
            initialParent: ClosureItem,
            override val rulePosition: RulePosition
        ) : ClosureItemAbstract(ffc, graph), ClosureItemChild {

            override val parents: Set<ClosureItem> get() = this.graph.parentsOf(this)
            //override val parentRulePosition: RulePosition = initialParent.rulePosition
            //override val parentContext: RulePosition = initialParent.context
            // override val parentNextContext: Set<RulePosition> = initialParent.nextContext

            override val context: RulePosition = when {
                initialParent.rulePosition.isAtStart -> initialParent.context
                else -> initialParent.rulePosition
            }

            override val nextContext: Set<RulePosition> = let {
                if (Debug.CHECK) check(initialParent.rulePosition.isAtEnd.not()) { "Internal Error: ClosureItem parent should never by at end" }
                initialParent.rulePosition.next().flatMap { pn ->
                    when (pn.isAtEnd) {
                        true -> initialParent.nextContext
                        else -> setOf(pn)
                    }
                }.toSet()
            }

            //override val parentNextContextFollow: FirstFollowCache3.Companion.FollowDeferred = initialParent.nextContextFollow
            override val nextContextFollow: FirstFollowCache3.Companion.FollowDeferred = initialParent.nextNotAtEndFollow
            override val parentNext: Set<ParentNext>
                get() = parents.flatMap { prnt ->
                    val atStart = prnt.rulePosition.isAtStart
                    val prntNextContextFollow = when(atStart) {
                        true -> prnt.nextContextFollow
                        false -> FirstFollowCache3.Companion.FollowDeferredLiteral.EMPTY
                    }
                    prnt.rulePosition.next().map { prntNext ->
                        val prntNextFollow = when (prntNext.isAtEnd) {
                            true -> prnt.nextContextFollow
                            else -> FirstFollowCache3.Companion.FollowDeferredCalculation(this.ffc, prnt.context, prntNext, prnt.nextContext, prnt.nextContextFollow)
                        }
                        ParentNext(atStart, prntNext, prntNextFollow, prntNextContextFollow)
                    }
                }.toSet()

            /*
            override val parentOfInfo: FirstFollowCache3.Companion.ParentOfInContext = let {
                val parentContext = this.parentContext
                val parentRulePosition = this.parentRulePosition
                val parentFfc = initialParent.ffc
                val x = parentRulePosition.next().map { pn ->
                    val pnf = when {
                        pn.isAtEnd -> parentNextContextFollow
                        else -> FirstFollowCache3.Companion.FollowDeferredCalculation(parentFfc, parentContext, pn, initialParent.nextContext, parentNextContextFollow)
                    }
                    Pair(pn, pnf)
                }.toSet()
                FirstFollowCache3.Companion.ParentOfInContext(parentNextContextFollow, x, parentRulePosition)
            }
*/
            override fun addParent(extraParent: ClosureItem) {
                this.graph.addParentOf(this, extraParent)
            }

            //override val _id = arrayOf(context, rulePosition, nextNotAtEndFollow, parentContext, parentRulePosition, parentNextContextFollow, nextContextFollow)
            override val _id = arrayOf(rulePosition, nextContext)//, parentNext)

            override fun toStringRec(done: MutableSet<ClosureItem>): String {
                return if (done.contains(this)) {
                    "$rulePosition[$nextContextFollow]..."
                } else {
                    done.add(this)
                    parents.joinToString("\n") {
                        val s = it.toStringRec(done)
                        "$rulePosition[$nextContextFollow]-->$s"
                    }
                }

            }

            override fun toString(): String = "$rulePosition[$nextContext]"
        }

        data class PathData(
            val cls: ClosureItem,
            val needsNext: Boolean
        )

    }

    private val _parentOf = lazyMutableMapNonNull<ClosureItem, MutableSet<ClosureItem>> { mutableSetOf() }
    // private val _ancestors = lazyMutableMapNonNull<ClosureItem, MutableSet<ClosureItem>> { mutableSetOf() }

    val root = ClosureRootGraph(ffc, this, rootContext, rootRulePosition, rootNextContext, rootNextContextFollow)

    fun traverseUpPaths(bottom: ClosureItem, func: (item: ClosureItem, childNeedsNext: Boolean) -> Boolean) {
        val done = mutableSetOf<Pair<PathData, PathData>>()
        val bottomNeedsNext = func.invoke(bottom, false)
        val openPaths = mutableQueueOf(PathData(bottom, bottomNeedsNext))
        while (openPaths.isNotEmpty) {
            val child = openPaths.dequeue()
            when (child.cls) {
                is ClosureItemRoot -> Unit //end path
                is ClosureItemChild -> {
                    for (prt in child.cls.parents) {
                        val needsNext = func.invoke(prt, child.needsNext)
                        val pd = Pair(child, PathData(prt, needsNext))
                        if (done.contains(pd)) {
                            //don't do it again
                        } else {
                            done.add(pd)
                            openPaths.enqueue(pd.second)
                        }
                    }
                }
            }
        }
    }

    fun parentsOf(child: ClosureItem): Set<ClosureItem> = this._parentOf[child]

    fun addParentOf(child: ClosureItem, extraParent: ClosureItem): Boolean {
        return this._parentOf[child].add(extraParent)
    }
}