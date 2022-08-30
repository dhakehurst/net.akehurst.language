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

internal interface ChildParentRel {
    val child: RulePosition

    val info: Set<ChildParentInfo>

    fun resolve(graph: ClosureGraph)
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
    val ffc: FirstFollowCache3
    val graph: ClosureGraph
    val rulePosition: RulePosition

    val childParentRels: Set<ChildParentRel>

    val shortString: List<String>

    //fun needsNext(childNeedsNext: Boolean): Boolean

    /**
     * create child but do not link to parents
     */
    fun createChild(childRulePosition: RulePosition): ClosureItemChild

    /**
     * link child to this as parent
     */
    fun addChild(child: ClosureItemChild): Boolean

    //fun parentInfo(parent: RulePosition?) : ChildParentRel

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
            rulePosition: RulePosition,
            rootRel: ChildParentRel
        ) : ClosureItemAbstractGraph(ffc, graph, rulePosition), ClosureItemRoot {

            override val childParentRels: Set<ChildParentRel> = setOf(rootRel)

            //override fun parentInfo(parent: RulePosition?): ChildParentRel = when(parent) {
            //    null -> childParentInfo.first()
            //    else -> error("Internal Error: $parent is not a parent of ${this.rulePosition}")
            //}

            override fun toStringRec(done: MutableSet<ClosureItem>): String = "$rulePosition"

        }

        class ClosureItemChildGraph(
            ffc: FirstFollowCache3,
            graph: ClosureGraph,
            rulePosition: RulePosition
        ) : ClosureItemAbstractGraph(ffc, graph, rulePosition), ClosureItemChild {

            override val parents: Set<ClosureItem> = this.graph.parentsOf(this.rulePosition)

            override val childParentRels: Set<ChildParentRel> by lazy { graph.childParentRels(this.rulePosition) }

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

            //override fun parentInfo(parent: RulePosition?): ChildParentRel = graph.childParentInfo(this.rulePosition)

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
            val child: RulePosition,
            val isClosureRoot: Boolean,
            val needsNext: Boolean
        )

        data class ChildParentRelRoot(
            val ffc: FirstFollowCache3,
            override val child: RulePosition,
            override val info: Set<ChildParentInfo>
        ) : ChildParentRel {
            override fun resolve(graph: ClosureGraph) {}
        }


        data class ChildParentRelChild(
            val ffc: FirstFollowCache3,
            override val child: RulePosition,
            val parent: RulePosition
        ) : ChildParentRel {
            private var resolveCalled = false

            override lateinit var info: Set<ChildParentInfo>

            override fun resolve(graph: ClosureGraph) {
                if (resolveCalled) {

                } else {
                    this.info = mutableSetOf()
                    this.resolveCalled = true
                    val gps = graph.parentsOf(parent)
                    if (gps.isEmpty()) {
                        //assume parent is root
                        val gpInfo = graph.rootChildParentInfo
                        val childContext = when {
                            parent.isAtStart -> gpInfo.childContext
                            else -> parent
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
                        val childNextContextFollow = gpInfo.childNextNotAtEndFollow
                        val childNextNotAtEndFollow = nextNotAtEndFollow(ffc, child, childContext, childNextContextFollow)
                        val atStart = parent.isAtStart
                        val prntNextContextFollow = when (atStart) {
                            true -> gpInfo.childNextContextFollow
                            false -> FirstFollowCache3.Companion.FollowDeferredLiteral.EMPTY
                        }
                        val parentNext = parent.next().map { prntNext ->
                            val prntNextFollow = when (prntNext.isAtEnd) {
                                true -> gpInfo.childNextContextFollow
                                else -> FirstFollowCache3.Companion.FollowDeferredCalculation(
                                    ffc,
                                    gpInfo.childContext,
                                    prntNext,
                                    //                    parentGrandParentInfo.childNextContext,
                                    gpInfo.childNextContextFollow
                                )
                            }
                            ParentNext(atStart, prntNext, prntNextFollow, prntNextContextFollow)
                        }.toSet()
                        val i = ChildParentInfo(childContext, childNextNotAtEndFollow, childNextContextFollow, parentNext)
                        (this.info as MutableSet).add(i)
                    } else {
                        for (gp in gps) {
                            val gpRels = graph.childParentRels(parent, gp.rulePosition)
                            for (gpRel in gpRels) {
                                gpRel.resolve(graph)
                                for (gpInfo in gpRel.info) {
                                    val childContext = when {
                                        parent.isAtStart -> gpInfo.childContext
                                        else -> parent
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
                                    val childNextContextFollow = gpInfo.childNextNotAtEndFollow
                                    val childNextNotAtEndFollow = nextNotAtEndFollow(ffc, child, childContext, childNextContextFollow)
                                    val atStart = parent.isAtStart
                                    val prntNextContextFollow = when (atStart) {
                                        true -> gpInfo.childNextContextFollow
                                        false -> FirstFollowCache3.Companion.FollowDeferredLiteral.EMPTY
                                    }
                                    val parentNext = parent.next().map { prntNext ->
                                        val prntNextFollow = when (prntNext.isAtEnd) {
                                            true -> gpInfo.childNextContextFollow
                                            else -> FirstFollowCache3.Companion.FollowDeferredCalculation(
                                                ffc,
                                                gpInfo.childContext,
                                                prntNext,
                                                //                    parentGrandParentInfo.childNextContext,
                                                gpInfo.childNextContextFollow
                                            )
                                        }
                                        ParentNext(atStart, prntNext, prntNextFollow, prntNextContextFollow)
                                    }.toSet()
                                    val i = ChildParentInfo(childContext, childNextNotAtEndFollow, childNextContextFollow, parentNext)
                                    (this.info as MutableSet).add(i)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private val _parentOf = lazyMutableMapNonNull<RulePosition, MutableSet<ClosureItem>> { mutableSetOf() }
    private val _childParentRels = lazyMutableMapNonNull<RulePosition, MutableSet<ChildParentRel>> { mutableSetOf() }

    val rootChildParentInfo = ChildParentInfo(
        childContext = rootContext,
//                    childNextContext = nextContext,
        childNextNotAtEndFollow = nextNotAtEndFollow(ffc, rootRulePosition, rootContext, rootNextContextFollow), //nextContext, nextContextFollow),
        childNextContextFollow = rootNextContextFollow,
        parentNext = emptySet(), //root has no parents
    )
    val rootRel = ChildParentRelRoot(
        ffc = ffc,
        child = rootRulePosition,
        info = setOf(rootChildParentInfo)
    )
    val root = ClosureItemRootGraph(ffc, this, rootRulePosition,rootRel) //rootNextContext, rootNextContextFollow)

    fun traverseUpPaths(bottom: ClosureItem, func: (isClosureRoot: Boolean, rp: RulePosition, cpInfo: ChildParentInfo, childNeedsNext: Boolean) -> Boolean) {
        val done = mutableSetOf<RulePosition>()
        val openPaths = mutableQueueOf<PathData>()

        val isBottomClosureRoot = when (bottom) {
            is ClosureItemRoot -> true
            is ClosureItemChild -> false
            else -> error("Internal Error: subtype ${bottom::class.simpleName} not handled")
        }
        val bottomCpRels = this.childParentRels(bottom.rulePosition)
        for (cpRel in bottomCpRels) {
            for (cpInfo in cpRel.info) {
                val bottomNeedsNext = func.invoke(isBottomClosureRoot, bottom.rulePosition, cpInfo, false)
                openPaths.enqueue(PathData(bottom.rulePosition, isBottomClosureRoot, bottomNeedsNext))
            }
        }
        while (openPaths.isNotEmpty) {
            val pathElement = openPaths.dequeue()
            val child = pathElement.child
            when (pathElement.isClosureRoot) {
                true -> {
                    val rootInfo = this.rootChildParentInfo
                    func.invoke(true, this.root.rulePosition, rootInfo, pathElement.needsNext)
                }

                else -> {
                    val parents = this.parentsOf(child)
                    for (parent in parents) {
                        if (done.contains(parent.rulePosition)) {
                            //done it already
                        } else {
                            done.add(child)
                            val cpRels = this.childParentRels(child, parent.rulePosition)
                            for (cpRel in cpRels) {
                                for (cpInfo in cpRel.info) {
                                    val needsNext = func.invoke(false, child, cpInfo, pathElement.needsNext)
                                    val pd = PathData(child, false, needsNext)
                                    openPaths.enqueue(pd)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun resolveAllChildParentInfo() {
        for ((child, infos) in this._childParentRels) {
            for (info in infos) {
                info.resolve(this)
            }
        }
    }

    fun parentsOf(child: RulePosition): Set<ClosureItem> = this._parentOf[child]

    fun addParentOf(child: ClosureItemChild, extraParent: ClosureItem): Boolean {
        val added = this._parentOf[child.rulePosition].add(extraParent)
        if (added) {
            val info = createChildParentInfo(child.ffc, child.rulePosition, extraParent.rulePosition)
            this._childParentRels[child.rulePosition].add(info)
        } else {
            val i = 0
        }
        return added
    }

    private fun createChildParentInfo(ffc: FirstFollowCache3, child: RulePosition, parent: RulePosition): ChildParentRel =
        ChildParentRelChild(
            ffc = ffc,
            child = child,
            parent = parent
        )

    fun childParentRels(child: RulePosition): Set<ChildParentRel> = this._childParentRels[child]
    fun childParentRels(child: RulePosition, parent: RulePosition): Set<ChildParentRel> = this._childParentRels[child]
}