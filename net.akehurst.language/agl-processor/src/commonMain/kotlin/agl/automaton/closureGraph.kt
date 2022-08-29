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

internal data class ChildParentInfo(
    val child: RulePosition,
    val parent: RulePosition?,
    val childContext: RulePosition,
//    val childNextContext: Set<RulePosition>,
    /**
     * when {
     *  next.isAtEnd -> nextContextFollow
     *  else -> next.follow
     * }
     */
    val childNextNotAtEndFollow: FirstFollowCache3.Companion.FollowDeferred,
    /**
     * parent.nextNotAtEndFollow
     */
    val childNextContextFollow: FirstFollowCache3.Companion.FollowDeferred,

    val parentNext: Set<ParentNext>
)

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
    val rulePosition: RulePosition

    val childParentInfo: Set<ChildParentInfo>

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
    val firstPosition: Boolean,
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
    val parents: Set<ClosureItem>
    fun addParent(extraParent: ClosureItem)
}

internal class ClosureGraph(
    ffc: FirstFollowCache3,
    rootContext: RulePosition,
    rootRulePosition: RulePosition,
//    rootNextContext: Set<RulePosition>,
    rootNextContextFollow: FirstFollowCache3.Companion.FollowDeferred
) {

    companion object {
        fun nextNotAtEndFollow(
            ffc: FirstFollowCache3,
            rulePosition: RulePosition,
            context: RulePosition,
//            nextContext: Set<RulePosition>,
            nextContextFollow: FirstFollowCache3.Companion.FollowDeferred
        ): FirstFollowCache3.Companion.FollowDeferred = when {
            rulePosition.isTerminal -> nextContextFollow
            else -> {
                if (Debug.CHECK) check(rulePosition.isAtEnd.not()) { "Internal Error: rulePosition of ClosureItem should never be at end" }
                val nexts = rulePosition.next()
                val allNextFollow = nexts.map { next ->
                    when {
                        next.isAtEnd -> nextContextFollow
                        else -> FirstFollowCache3.Companion.FollowDeferredCalculation(ffc, context, next, nextContextFollow) //nextContext, nextContextFollow)
                    }
                }
                FirstFollowCache3.Companion.FollowDeferredComposite.constructOrDelegate(allNextFollow.toSet())
            }
        }

        abstract class ClosureItemAbstractGraph(
            override val ffc: FirstFollowCache3,
            override val graph: ClosureGraph,
            override val rulePosition: RulePosition,
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
                childNeedsNext -> {
                    if (Debug.CHECK) check(rulePosition.isAtEnd.not()) { "Internal Error: rulePosition of ClosureItem should never be at end" }
                    val nexts = rulePosition.next()
                    nexts.any { nxt ->
                        when {
                            nxt.isAtEnd -> true
                            else -> ffc.needsNext(nxt)
                        }
                    }
                }

                else -> false
            }

            override val shortString: List<String> get() = this.shortStringRec(mutableSetOf())
            val longString get() = toStringRec(mutableSetOf())

            override fun createChild(childRulePosition: RulePosition): ClosureItemChild =
                ClosureItemChildGraph(this.ffc, this.graph, childRulePosition)

            override fun addChild(child: ClosureItemChild): Boolean = this.graph.addParentOf(child, this)


            override fun hashCode(): Int = this.rulePosition.hashCode()
            override fun equals(other: Any?): Boolean = when (other) {
                !is ClosureItem -> false
                else -> this.rulePosition == other.rulePosition
            }

            override fun toString(): String = "$rulePosition"

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
                        is ClosureItemChildGraph -> parents.flatMap { p ->
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
        }

        class ClosureItemRootGraph(
            ffc: FirstFollowCache3,
            graph: ClosureGraph,
            context: RulePosition,
            rulePosition: RulePosition,
//            nextContext: Set<RulePosition>,
            nextContextFollow: FirstFollowCache3.Companion.FollowDeferred,
        ) : ClosureItemAbstractGraph(ffc, graph, rulePosition), ClosureItemRoot {

            override val childParentInfo: Set<ChildParentInfo> = setOf(
                ChildParentInfo(
                    child = this.rulePosition,
                    parent = null,
                    childContext = context,
//                    childNextContext = nextContext,
                    childNextNotAtEndFollow = nextNotAtEndFollow(ffc, rulePosition, context, nextContextFollow), //nextContext, nextContextFollow),
                    childNextContextFollow = nextContextFollow,
                    parentNext = emptySet(), //root has no parents
                )
            )

            override fun toStringRec(done: MutableSet<ClosureItem>): String = "$rulePosition"

        }

        class ClosureItemChildGraph(
            ffc: FirstFollowCache3,
            graph: ClosureGraph,
            rulePosition: RulePosition
        ) : ClosureItemAbstractGraph(ffc, graph, rulePosition), ClosureItemChild {

            override val parents: Set<ClosureItem> = this.graph.parentsOf(this)

            override val childParentInfo: Set<ChildParentInfo> by lazy { graph.childParentInfo(this.rulePosition) }

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

        data class PathData(
            val cls: ClosureItem,
            val needsNext: Boolean
        )

    }

    private val _parentOf = lazyMutableMapNonNull<ClosureItem, MutableSet<ClosureItem>> { mutableSetOf() }
    private val _childParentInfo = lazyMutableMapNonNull<RulePosition, MutableSet<ChildParentInfo>> { mutableSetOf() }

    val root = ClosureItemRootGraph(ffc, this, rootContext, rootRulePosition, rootNextContextFollow) //rootNextContext, rootNextContextFollow)

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

    fun addParentOf(child: ClosureItemChild, extraParent: ClosureItem): Boolean {
        val added = this._parentOf[child].add(extraParent)
        if (added) {
            for (parentGrandParentInfo in extraParent.childParentInfo) {
                val info = createChildParentInfo(child, extraParent, parentGrandParentInfo)
                this._childParentInfo[child.rulePosition].add(info)
            }
        }
        return added
    }

    private fun createChildParentInfo(child: ClosureItem, parent: ClosureItem, parentGrandParentInfo: ChildParentInfo): ChildParentInfo {
        val context: RulePosition = when {
            parent.rulePosition.isAtStart -> parentGrandParentInfo.childContext
            else -> parent.rulePosition
        }
        /*
        val nextContext: Set<RulePosition> = let {
            parent.rulePosition.next().flatMap { pn ->
                when (pn.isAtEnd) {
                    true -> when {
                        parentGrandParentInfo.childNextContextFollow.containsEmptyRules -> parentGrandParentInfo.childNextContext + extraParent.parentNextContext
                        else -> parentGrandParentInfo.childNextContext
                    }

                    else -> setOf(pn)
                }
            }.toSet()
        }
         */
        val nextContextFollow = parentGrandParentInfo.childNextNotAtEndFollow

        val nextNotAtEndFollow = nextNotAtEndFollow(child.ffc, child.rulePosition, context, nextContextFollow)

        val prnt = parent.rulePosition
        val atStart = prnt.isAtStart
        val prntNextContextFollow = when (atStart) {
            true -> parentGrandParentInfo.childNextContextFollow
            false -> FirstFollowCache3.Companion.FollowDeferredLiteral.EMPTY
        }
        val parentNext = prnt.next().map { prntNext ->
            val prntNextFollow = when (prntNext.isAtEnd) {
                true -> parentGrandParentInfo.childNextContextFollow
                else -> FirstFollowCache3.Companion.FollowDeferredCalculation(
                    child.ffc,
                    parentGrandParentInfo.childContext,
                    prntNext,
                    //                    parentGrandParentInfo.childNextContext,
                    parentGrandParentInfo.childNextContextFollow
                )
            }
            ParentNext(atStart, prntNext, prntNextFollow, prntNextContextFollow)
        }.toSet()

        return ChildParentInfo(
            child = child.rulePosition,
            parent = parent.rulePosition,
            childContext = context,
//            childNextContext = nextContext,
            childNextNotAtEndFollow = nextNotAtEndFollow,
            childNextContextFollow = nextContextFollow,
            parentNext = parentNext
        )
    }

    fun childParentInfo(child: RulePosition): Set<ChildParentInfo> = this._childParentInfo[child]
}