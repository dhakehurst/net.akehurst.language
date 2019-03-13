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

package net.akehurst.language.parser.scannerless

import net.akehurst.language.api.parser.ParseException
import net.akehurst.language.api.sppt.SPPTNode
import net.akehurst.language.agl.runtime.graph.GrowingNode
import net.akehurst.language.agl.runtime.graph.GrowingNodeIndex
import net.akehurst.language.agl.runtime.graph.ParseGraph
import net.akehurst.language.agl.runtime.graph.PreviousInfo
import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleItemKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.parser.sppt.SPPTBranchDefault
import net.akehurst.language.parser.sppt.SPPTLeafDefault
import net.akehurst.language.parser.sppt.SPPTNodeDefault

internal class RuntimeParser(
    private val runtimeRuleSet: RuntimeRuleSet,
    private val graph: ParseGraph
) {
    // copy of graph growing head for each iteration, cached to that we can find best match in case of error
    private var toGrow: List<GrowingNode> = listOf()

    //needs to be public so that expectedAt can use it
    val lastGrown: Collection<GrowingNode>
        get() {
            return setOf<GrowingNode>().union(this.graph.growing.values).union(this.toGrow)
        }

    val longestLastGrown: SPPTNode?
        get() {
            val llg = this.lastGrown.maxWith(Comparator<GrowingNode> { a, b -> a.nextInputPosition.compareTo(b.nextInputPosition) })
            return if (null == llg) {
                return null
            } else if (llg.isLeaf) {
                this.graph.findOrTryCreateLeaf(llg.runtimeRule, llg.startPosition)
            } else {
                val cn = SPPTBranchDefault(llg.runtimeRule, llg.startPosition, llg.nextInputPosition, llg.priority)
                cn.childrenAlternatives.add(llg.children)
                cn
            }
        }

    val canGrow: Boolean
        get() {
            return this.graph.canGrow
        }


    fun start(userGoalRule: RuntimeRule) {
        val gr = RuntimeRuleSet.createGoal(userGoalRule)
        this.graph.start(gr, runtimeRuleSet)
    }

    fun grow() {
        this.toGrow = this.graph.growingHead.values.toList() //Note: this should be a copy of the list of values
        this.graph.growingHead.clear()
        for (gn in this.toGrow) {
            val previous = this.graph.pop(gn)
            this.growNode(gn, previous)
        }
    }

    private fun growNode(gn: GrowingNode,previous: Set<PreviousInfo>) {

        val didSkipNode = this.tryGrowWidthWithSkipRules(gn, previous)
        if (didSkipNode) {
            return
        } else {
            if (gn.runtimeRule.isSkip) {
                this.tryGraftBackSkipNode(gn, previous)
            } else {
                for (prev in previous) {
                    this.growWithPrev(gn, prev)
                }
            }
        }
    }

    private fun growWithPrev(gn: GrowingNode, previous: PreviousInfo) {
        this.tryGrowHeight(gn, previous)
        this.tryGraftInto(gn, previous)
        this.tryShift(gn, previous)
    }

    private fun tryGrowWidthWithSkipRules(gn: GrowingNode, previous: Set<PreviousInfo>): Boolean {
        var modified = false
  //      if (gn.currentRulePosition.runtimeRule.isTerminal) { //(gn.canGrowWidthWithSkip) { // don't grow width if its complete...cant graft back
            val rps = this.runtimeRuleSet.firstSkipRuleTerminalPositions
            for (rp in rps) {
                for (rr in rp.runtimeRule.itemsAt[rp.position]) {
                    val l = this.graph.findOrTryCreateLeaf(rr, gn.nextInputPosition)
                    if (null != l) {
                        //val newRP = runtimeRuleSet.nextRulePosition(rp, rr)
                        //newRP.forEach {
                        this.graph.pushToStackOf(true, l, gn, previous)
                        modified = true
                        //}
                    }
                }
            }
 //       }
        return modified
    }

    private fun tryGraftBackSkipNode(gn: GrowingNode, previous: Set<PreviousInfo>) {
        for (info in previous) {
            this.tryGraftInto(gn, info)
        }
    }

    /*
        private fun tryReduceFirst(gn: GrowingNode, prev: PreviousInfo) {
            //growHeight
            if (gn.currentRulePosition.isAtEnd && gn.targetRulePosition.isAtStart) { //gn.hasCompleteChildren) {
                val complete = this.graph.findCompleteNode(gn.runtimeRule.number, gn.startPosition, gn.matchedTextLength)
                    ?: throw ParseException("Internal error: Should never happen")

                val nextRps = runtimeRuleSet.nextRulePosition(gn.targetRulePosition, complete.runtimeRule)
                nextRps.forEach { nextRp ->
                    //TODO: maybe check lookahead here also?
                    this.graph.createWithFirstChild(nextRp, newTgtRP, gn.targetRulePosition.runtimeRule, complete, setOf(prev)) //maybe lookahead to wanted next token here (it would need to be part of RP)
                }
            }
        }

        private fun tryReduceMiddle(gn: GrowingNode, prev: PreviousInfo) {
            //graftBack
            if (gn.currentRulePosition.isAtEnd && gn.targetRulePosition.isAtMiddle) {
                val complete = this.graph.findCompleteNode(gn.runtimeRule.number, gn.startPosition, gn.matchedTextLength)
                    ?: throw ParseException("internal error, should never happen")
                //this.graph.growNextChild(info.node.targetRulePosition, info.node, complete, info.atPosition)
                val nextRps = runtimeRuleSet.nextRulePosition(prev.node.currentRulePosition, gn.runtimeRule)
                nextRps.forEach { nextRp ->
                    this.graph.growNextChild(nextRp, prev.node, complete, prev.atPosition)
                }
            }
        }

        private fun tryReduceLast(gn: GrowingNode, prev: PreviousInfo) {
            if (gn.currentRulePosition.isAtEnd && gn.targetRulePosition.isAtEnd) {
                val complete = this.graph.findCompleteNode(gn.runtimeRule.number, gn.startPosition, gn.matchedTextLength)
                    ?: throw ParseException("Internal error: Should never happen")

                val newTargetRPs = runtimeRuleSet.growsInto(gn.targetRulePosition.runtimeRule, graph.runtimeGoalRule).map { RulePosition(it.runtimeRule, it.choice, 0) }
                val fntp = if (gn.targetRulePosition.runtimeRule.isSkip) { //TODO: do we really need to check if we are growing a skip node
                    newTargetRPs
                } else {
                    newTargetRPs.filter {
                        //TODO: checking the graftinto rule against prev, position doesn't work!
                        (it.runtimeRule == prev.node.runtimeRule) || runtimeRuleSet.calcCanGrowInto(it.runtimeRule, prev.node.runtimeRule, prev.atPosition)
                    }
                }
                fntp.forEach { newTgtRP ->
                    //val lookaheadItems = findLookaheadItems(newTgtRP, gn.runtimeRule, null)
                    val nextRps = runtimeRuleSet.nextRulePosition(gn.targetRulePosition, complete.runtimeRule)
                    nextRps.forEach { nextRp ->
                        //TODO: maybe check lookahead here also?
                        this.graph.createWithFirstChild(nextRp, newTgtRP, gn.targetRulePosition.runtimeRule, complete, setOf(prev)) //maybe lookahead to wanted next token here (it would need to be part of RP)
                    }
                }
            }
        }
    */
    private fun tryGrowHeight(gn: GrowingNode, previous: PreviousInfo) {
        val canHeight = !previous.node.currentRulePosition.isAtEnd //should always be true
            && gn.currentRulePosition.isAtEnd
        //&& !previous.node.currentRulePosition.items.contains(gn.runtimeRule)

        if (canHeight || gn.isSkipGrowth) {
            val complete = this.graph.findCompleteNode(gn.runtimeRule, gn.startPosition, gn.matchedTextLength)
                ?: throw ParseException("Internal error: Should never happen")

            //val newTargetRPs = runtimeRuleSet.growsInto(gn.runtimeRule, graph.runtimeGoalRule).map { RulePosition(it.runtimeRule, it.choice, 0) }
            val newTargetRPs = runtimeRuleSet.growsInto(previous.node.currentRulePosition, gn.currentRulePosition).filter { it.isAtStart }
            val fntp = if (gn.runtimeRule.isSkip) { //TODO: do we really need to check if we are growing a skip node
                newTargetRPs
            } else {
                newTargetRPs.filter {
                    //TODO: checking the graftinto rule against prev, position doesn't work!
                    (!it.runtimeRule.isGoal) && (
                        (it.runtimeRule.isSkip) || (it.runtimeRule == previous.node.runtimeRule) || runtimeRuleSet.calcCanGrowInto(it.runtimeRule, previous.node.runtimeRule, previous.atPosition)
                    )
                }
            }
            for (newRP in fntp) {
                if (newRP.runtimeRule.isSkip) {
                    val nextRps = runtimeRuleSet.nextRulePosition(newRP, complete.runtimeRule)
                    for (nextRp in nextRps) {
                        this.graph.createWithFirstChild(gn.isSkipGrowth, nextRp, complete, setOf(previous), gn.skipNodes) //maybe lookahead to wanted next token here (it would need to be part of RP)
                    }
                } else {
                    val lh = runtimeRuleSet.lookahead(newRP, previous.node.currentRulePosition, previous.lookahead)
                    val hasLh = lh.any {
                        val l = this.graph.findOrTryCreateLeaf(it, gn.nextInputPosition)
                        null != l
                    }

                    if (hasLh) {
                        //val lookaheadItems = findLookaheadItems(newTgtRP, gn.runtimeRule, null)
                        val nextRps = runtimeRuleSet.nextRulePosition(newRP, complete.runtimeRule)
                        for (nextRp in nextRps) {

                            this.graph.createWithFirstChild(gn.isSkipGrowth,nextRp, complete, setOf(previous), gn.skipNodes) //maybe lookahead to wanted next token here (it would need to be part of RP)
                        }
                    }
                }
            }


        } else {

        }
    }

    /*
        private fun tryGraftBack(gn: GrowingNode, previous: Set<PreviousInfo>): Boolean {
            var graftBack = false
            for (prev in previous) {
                //TODO: canGraftBack calls expectsItemAt which calls contains, so it has a nested loop, is it worth caching here?
                //           if (gn.canGraftBack(prev)) { // if hascompleteChildren && isStacked && prevInfo is valid
                graftBack = this.tryGraftBack(gn, prev)
                //           }
            }
            return graftBack
        }

        private fun tryGraftBack(gn: GrowingNode, info: PreviousInfo): Boolean {
            var result = false
            // TODO: perhaps should return list of those who are not grafted!
            // for (final IGrowingNode.PreviousInfo info : previous) {
            if (info.node.hasNextExpectedItem) {
                result = result or this.tryGraftInto(gn, info)
            } else {
                // can't push back
                result = result or false
            }
            // }
            return result
        }
    */
    private fun tryGraftInto(gn: GrowingNode, previous: PreviousInfo) {
        val canGraft = !previous.node.currentRulePosition.isAtEnd //should always be true
            && gn.currentRulePosition.isAtEnd
            && previous.node.currentRulePosition.items.contains(gn.runtimeRule)

        if (gn.runtimeRule.isSkip) {
            val complete = this.graph.findCompleteNode(gn.runtimeRule, gn.startPosition, gn.matchedTextLength)
                ?: throw ParseException("internal error, should never happen")
            this.graph.growNextSkipChild(previous.node, complete)
        } else if (canGraft) {
            // complete will not be null because we do not graftback unless gn has complete children
            // and graph will try to 'complete' a GraphNode when it is created.
            val complete = this.graph.findCompleteNode(gn.runtimeRule, gn.startPosition, gn.matchedTextLength)
                ?: throw ParseException("internal error, should never happen")

            val nextRps = runtimeRuleSet.nextRulePosition(previous.node.currentRulePosition, gn.runtimeRule)
            for (nextRp in nextRps) {

                val lh = runtimeRuleSet.lookahead(nextRp, previous.node.currentRulePosition, previous.lookahead)
                val hasLh = lh.any {
                    val l = this.graph.findOrTryCreateLeaf(it, gn.nextInputPosition)
                    null != l
                }
                if (hasLh) {
                    this.graph.growNextChild(false, nextRp, previous.node, complete, previous.atPosition, gn.skipNodes)
                }
            }
        } else {
            // drop
        }
    }

    private fun tryShift(gn: GrowingNode, prev: PreviousInfo): Boolean {
        var modified = false
        if (gn.canGrowWidth) { // don't grow width if its complete...cant graft back
            //TODO: get firstTermnials of gn, no need for RPs here I think.
            //val rps = runtimeRuleSet.expectedTerminalRulePositions[gn.currentRulePosition] ?: arrayOf()
            //val rps2 = previous.flatMap{prev->this.runtimeRuleSet.calcGrowsInto( ??? gn.runtimeRule, prev.node.runtimeRule, prev.atPosition)}
            //val rpsf = rps.filter {
            //TODO: do we need this filter?
            //    runtimeRuleSet.calcCanGrowInto(it.runtimeRule, gn.currentRulePosition.runtimeRule, gn.currentRulePosition.position)
            //}
            //for (newTgtRp in rps) {
            //val nextRP = gn.rulePosition.next
            val items = runtimeRuleSet.firstTerminals2[gn.currentRulePosition]
                ?: emptySet()//  newTgtRp.items //  rp.runtimeRule.itemsAt[rp.position]
            for (rr in items) {
                if (rr.isTerminal) { //it might not be, e.g. if rp is in a multi,0 where it could be empty
                    val l = this.graph.findOrTryCreateLeaf(rr, gn.nextInputPosition)
                    if (null != l) {
                        //TODO: try doing lookahead in height or graft instead!
                        // should allow the skip to be consumed before looking ahead
                        //val lhok = hasLookaheadAfterSkip(gn.currentRulePosition, gn.nextInputPosition + l.matchedTextLength)
                        //if (lhok) {
                        this.graph.pushToStackOf(false, l, gn, setOf(prev))
                        //}
                    }
                }
            }
            // }
        }
        return modified
    }
/*
    private fun hasLookaheadAfterSkip(newTgtRp: RulePosition, initialPosition: Int): Boolean {
        val position = initialPosition
        val lookaheadItems = runtimeRuleSet.lookahead(newTgtRp, graph.runtimeGoalRule)
        var foundLookahead = lookaheadItems.any { lhi ->
            null != this.graph.findOrTryCreateLeaf(lhi, position) //TODO: should we do something special if we match empty rules?
        }
        if (!foundLookahead) {
            val skipTerms = runtimeRuleSet.allSkipTerminals
            val skipAheads = skipTerms.mapNotNull { lhi ->
                this.graph.findOrTryCreateLeaf(lhi, position)
            }
            if (skipAheads.isEmpty()) {
                return false
            } else {
                //TODO: this is a problem if the skip rules are a complex grammar in them selves
                // really need to parse the whole skip tree to lookahead!
                for (skipLeaf in skipAheads) {
                    val pos = position + skipLeaf.matchedTextLength
                    foundLookahead = hasLookaheadAfterSkip(newTgtRp, pos)

                }
            }
        }
        return foundLookahead
    }
*/
    /*
        private fun pushStackNewRoot(leafNode: SPPTLeafDefault, stack: GrowingNode, previous: Set<PreviousInfo>): Boolean {
            var modified = false
            // no existing parent was suitable, use newRoot
            if (this.hasStackedPotential(leafNode, stack)) {
                this.graph.pushToStackOf(rp, leafNode, stack, previous)
                modified = true
            }
            return modified
        }
    */
    private fun hasStackedPotential(completeNode: SPPTNodeDefault, stack: GrowingNode): Boolean {
        if (completeNode.isSkip) {
            return true
        } else {
            // if node is nextexpected item on stack, or could grow into nextexpected item
            if (stack.hasNextExpectedItem) {

                if (
                    this.runtimeRuleSet.calcCanGrowInto(completeNode.runtimeRule, stack.runtimeRule, stack.currentRulePosition.position)
                    || this.runtimeRuleSet.isSkipTerminal[completeNode.runtimeRule.number]
                ) {
                    return true;
                }
/*
                for (expectedRule in stack.nextExpectedItems) {
                    if (completeNode.runtimeRule.number == expectedRule.number) {
                        // node is nextexpected item on stack
 //                       return true
                    } else {
                        // node is a possible subnode of nextexpected item
                        if (completeNode.runtimeRule.isNonTerminal) {
                            //TODO: would it help to use 'at' here? rather than all subrules
                            val possibles = this.runtimeRuleSet.subNonTerminals[expectedRule.number]
                            val res = possibles.contains(completeNode.runtimeRule)
                            if (res) {
 //                               return true
                            }
                        } else {
                            //TODO: can we change this so that we don't have to test using 'contains' ? (and above) ?
                            val possibles = this.runtimeRuleSet.subTerminals[expectedRule.number]
                            val res = possibles.contains(completeNode.runtimeRule)
                            if (res) {
  //                              return true
                            }

                        }
                    }
                }
*/
                return false
//            } else return if (this.runtimeRuleSet.allSkipTerminals.contains(completeNode.runtimeRule)) {
            } else return if (this.runtimeRuleSet.isSkipTerminal[completeNode.runtimeRule.number]) { //should be faster, hard to tell!
                true
            } else {
                false
            }
        }
    }

}