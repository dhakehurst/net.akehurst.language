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

    val nextNotAtEndFollow: FirstFollowCache3.Companion.FollowDeferred

    val parents: Set<ClosureItem>

    /**
     * parent.next.follow
     */
    val parentNextNotAtEndFollow: FirstFollowCache3.Companion.FollowDeferred

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
    fun createChild(childRulePosition: RulePosition): ClosureItem

    /**
     * create child and link to parents
     */
    fun addChild(childRulePosition: RulePosition): ClosureItem

    fun toStringRec(done: MutableSet<ClosureItem>): String
    fun shortStringRec(done: MutableSet<ClosureItem>): List<String>

}

internal interface ClosureItemRoot : ClosureItem {

}

internal interface ClosureItemChild : ClosureItem {
    /**
     * all parents will/must have same rulePosition
     */
    val parentRulePosition: RulePosition

    /**
     * all parents will/must have same context RulePosition
     */
    val parentContext: RulePosition


    /**
     * parent.parentNextFollow
     */
    val parentFollowAtEnd: FirstFollowCache3.Companion.FollowDeferred

    val parentOfInfo: FirstFollowCache3.Companion.ParentOfInContext

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
    rootParentNextNotAtEndFollow: FirstFollowCache3.Companion.FollowDeferred
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
                    rulePosition.isAtEnd -> parentNextNotAtEndFollow
                    else -> {
                        val nexts = rulePosition.next()
                        val allNextFollow = nexts.map { next ->
                            when {
                                next.isAtEnd -> parentNextNotAtEndFollow
                                else -> FirstFollowCache3.Companion.FollowDeferredCalculation(ffc, context, next, parentNextNotAtEndFollow)
                            }
                        }
                        FirstFollowCache3.Companion.FollowDeferredComposite(allNextFollow.toSet())
                    }
                }
            }

            override val shortString: List<String> get() = this.shortStringRec(mutableSetOf())
            val longString get() = toStringRec(mutableSetOf())

            override fun createChild(childRulePosition: RulePosition): ClosureItem =
                ClosureChildGraph(this.ffc, this.graph, this, childRulePosition)

            override fun addChild(childRulePosition: RulePosition): ClosureItem {
                val child = ClosureChildGraph(this.ffc, this.graph, this, childRulePosition)
                this.graph.addParentOf(child, this)
                return child
            }

            override fun hashCode(): Int = this._id.contentDeepHashCode()
            override fun equals(other: Any?): Boolean = when (other) {
                !is ClosureItem -> false
                else -> this._id.contentDeepEquals(other._id)
            }

            override fun toString(): String = "$rulePosition($parentNextNotAtEndFollow)[$nextNotAtEndFollow]"

            override fun shortStringRec(done: MutableSet<ClosureItem>): List<String> {
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
                    done.add(this)
                    return when (this) {
                        is ClosureRootGraph -> listOf(str)
                        else -> parents.flatMap { p -> p.shortStringRec(done).map { "$it-$str" } }
                    }
                }
            }
        }

        class ClosureRootGraph(
            ffc: FirstFollowCache3,
            graph: ClosureGraph,
            override val context: RulePosition,
            override val rulePosition: RulePosition,
            override val parentNextNotAtEndFollow: FirstFollowCache3.Companion.FollowDeferred,
        ) : ClosureItemAbstract(ffc, graph), ClosureItemRoot {
            override val parents: Set<ClosureItem> = emptySet() // get() = error("ClosureItemRoot has no parent")

            override val _id = arrayOf(context, rulePosition, nextNotAtEndFollow, parentNextNotAtEndFollow)
            override fun toStringRec(done: MutableSet<ClosureItem>): String = "$rulePosition[$parentNextNotAtEndFollow]-|-$context"
        }

        class ClosureChildGraph(
            ffc: FirstFollowCache3,
            graph: ClosureGraph,
            initialParent: ClosureItem,
            override val rulePosition: RulePosition
        ) : ClosureItemAbstract(ffc, graph), ClosureItemChild {

            override val parents: Set<ClosureItem> get() = this.graph.parentsOf(this)
            override val parentRulePosition: RulePosition = initialParent.rulePosition
            override val parentContext: RulePosition = initialParent.context

            override val context: RulePosition = when {
                parentRulePosition.isAtStart -> parentContext
                else -> parentRulePosition
            }

            override val parentFollowAtEnd: FirstFollowCache3.Companion.FollowDeferred = initialParent.parentNextNotAtEndFollow
            override val parentNextNotAtEndFollow: FirstFollowCache3.Companion.FollowDeferred = initialParent.nextNotAtEndFollow
            override val parentOfInfo: FirstFollowCache3.Companion.ParentOfInContext = this.let {
                val parentContext = this.parentContext
                val parentRulePosition = this.parentRulePosition
                val parentFfc = initialParent.ffc
                val x = parentRulePosition.next().map { pn ->
                    val pnf = when {
                        pn.isAtEnd -> parentFollowAtEnd
                        else -> FirstFollowCache3.Companion.FollowDeferredCalculation(parentFfc, parentContext, pn, parentFollowAtEnd)
                    }
                    Pair(pn, pnf)
                }.toSet()
                FirstFollowCache3.Companion.ParentOfInContext(parentFollowAtEnd, x, parentRulePosition)
            }

            override fun addParent(extraParent: ClosureItem) {
                if (Debug.CHECK) {
                    check(parentRulePosition == extraParent.rulePosition)
                    check(parentContext == extraParent.context)
                }
                this.graph.addParentOf(this, extraParent)
            }

            override val _id = arrayOf(context, rulePosition, nextNotAtEndFollow, parentContext, parentRulePosition, parentFollowAtEnd, parentNextNotAtEndFollow)

            override fun toStringRec(done: MutableSet<ClosureItem>): String {
                return if (done.contains(this)) {
                    "$rulePosition[$parentNextNotAtEndFollow]..."
                } else {
                    done.add(this)
                    parents.joinToString("\n") {
                        val s = it.toStringRec(done)
                        "$rulePosition[$parentNextNotAtEndFollow]-->$s"
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
   // private val _ancestors = lazyMutableMapNonNull<ClosureItem, MutableSet<ClosureItem>> { mutableSetOf() }

    val root = ClosureRootGraph(ffc, this, rootContext, rootRulePosition, rootParentNextNotAtEndFollow)

    fun traverseUpPaths(bottom: ClosureItem, func: (item: ClosureItem, childNeedsNext: Boolean) -> Boolean) {
        val done = mutableSetOf<Pair<PathData,PathData>>()
        val bottomNeedsNext = func.invoke(bottom, false)
        val openPaths = mutableQueueOf(PathData(bottom, bottomNeedsNext))
        while (openPaths.isNotEmpty) {
            val child = openPaths.dequeue()
            when (child.cls) {
                is ClosureItemRoot -> Unit //end path
                is ClosureItemChild -> {
                    for (prt in child.cls.parents) {
                        val needsNext = func.invoke(prt, child.needsNext)
                        val pd = Pair(child,PathData(prt, needsNext))
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

    fun addParentOf(child: ClosureItem, parent: ClosureItem): Boolean {
        //return if (this._ancestors[parent].contains(child)) {
            // do not create loops
        //    false
        //} else {
           return  this._parentOf[child].add(parent)
            //this._ancestors[child].add(parent)
            //this._ancestors[child].addAll(this._ancestors[parent])
           // true
       // }
    }
}