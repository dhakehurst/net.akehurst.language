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
        /*
        val gnindex = GrowingNodeIndex(goalRule.number, 0, 0, 0)
        val gn = GrowingNode(goalRule, 0, 0, 0, 0, emptyList<SPPTNodeDefault>(), 0)
        this.toGrow = listOf(gn)
        this.graph.addGrowingHead(gnindex, gn)
        if (gn.hasCompleteChildren) {
            val cn = this.graph.createBranchNoChildren(goalRule, 0, 0, 0)
            if (gn.isLeaf) {
                // dont try and add children...can't for a leaf
            } else {
                cn.childrenAlternatives.add(gn.children)
            }
        }
        */
    }

    fun grow() {
        this.toGrow = this.graph.growingHead.values.toList() //Note: this should be a copy of the list of values
        this.graph.growingHead.clear()
        for (gn in this.toGrow) {
            this.growNode(gn)
        }
    }

    private fun growNode(gn: GrowingNode) {
        val previous = this.graph.pop(gn)

        val didSkipNode = this.tryGrowWidthWithSkipRules(gn, previous)
        if (didSkipNode) {
            return
        } else {
            if (gn.isSkip) {
                this.tryGraftBackSkipNode(gn, previous)
            } else {
                // TODO: need to find a way to do either height or graft..not both, maybe!
                // problem is deciding which
                // try reduce first
                //TODO: use growFirst/Mid/Last ? maybe
                val grownHeight = this.tryGrowHeight(gn, previous) // reduce first item & it may be the last (only) item

                // try reduce rest
                var graftBack = this.tryGraftBack(gn, previous) // reduce middle or last items

                // maybe only shift if not done either of above!
                // tomitas original does that!
                // shift
                val grownWidth = this.tryGrowWidth(gn, previous)
            }
        }
    }

    private fun tryGrowWidthWithSkipRules(gn: GrowingNode, previous: Set<PreviousInfo>): Boolean {
        var modified = false
        if (gn.canGrowWidthWithSkip) { // don't grow width if its complete...cant graft back
            val rps = this.runtimeRuleSet.firstSkipRuleTerminalPositions
            for (rp in rps) {
                for (rr in rp.runtimeRule.itemsAt[rp.position]) {
                    val l = this.graph.findOrTryCreateLeaf(rr, gn.nextInputPosition)
                    if (null != l) {
                        //val newRP = runtimeRuleSet.nextRulePosition(rp, rr)
                        //newRP.forEach {
                            this.graph.pushToStackOf(rp, l, gn, previous)
                            modified = true
                        //}
                    }
                }
            }
        }
        return modified
    }

    private fun tryGraftBackSkipNode(gn: GrowingNode, previous: Set<PreviousInfo>) {
        for (info in previous) {
            this.tryGraftInto(gn, info)
        }
    }

    /*
        private fun tryGrowHeight1(gn: GrowingNode, previous: PreviousInfo): Boolean {
            var result = false
            if (gn.hasCompleteChildren) {
                val childRule = gn.runtimeRule
                if (this.runtimeRuleSet.isSkipTerminal[childRule.number]) {
                    val possibleSuperRules = this.runtimeRuleSet.firstSuperNonTerminal[childRule.number]
                    for (sr in possibleSuperRules) {
                        val complete = this.graph.findCompleteNode(gn.runtimeRule.number, gn.startPosition, gn.matchedTextLength)
                            ?: throw ParseException("Internal error: Should never happen")
                        val rp = RulePosition(gn.runtimeRule, 1, setOf())
                        this.graph.createWithFirstChild(rp, sr, complete, setOf(previous))
                        //this.growHeightByType(complete, sr, previous)
                        result = result or true // TODO: this should depend on if the growHeight does something
                    }
                } else {
                    val toGrow = mutableSetOf<RuntimeRule>()
                    for ((node) in previous) {
                        val prevItemIndex = node.nextItemIndex
                        val prevRule = node.runtimeRule
                        //TODO: find a way to cache the calcGrowsInto
                        toGrow.addAll(this.runtimeRuleSet.calcGrowsInto(childRule, prevRule, prevItemIndex))
                    }
                    for (rr in toGrow) {
                        val complete = this.graph.findCompleteNode(gn.runtimeRule.number, gn.startPosition, gn.matchedTextLength)
                            ?: throw ParseException("Internal error: Should never happen")
                        val rp = RulePosition(rr, 1, setOf())
                        this.graph.createWithFirstChild(rp, rr, complete, previous)
                        //this.growHeightByType(complete, info, previous)
                        result = result or true // TODO: this should depend on if the growHeight does something
                    }
                }
            } else {
                // do nothing
            }
            return result
        }
    */
    private fun tryGrowHeight(gn: GrowingNode, previous: Set<PreviousInfo>): Boolean {
//        if (gn.currentRulePosition.isAtEnd && !gn.currentRulePosition.runtimeRule.isGoal && gn.targetRulePosition.items.contains(gn.currentRulePosition.runtimeRule)) { //gn.hasCompleteChildren) {
        if (gn.currentRulePosition.isAtEnd && !(gn.targetRulePosition.position==-1) && gn.targetRulePosition.items.contains(gn.currentRulePosition.runtimeRule)) { //gn.hasCompleteChildren) {
            val tgtRP = gn.targetRulePosition
            when (tgtRP.position) {
                0 -> { // start
                    val newParentRule = tgtRP.runtimeRule
                    //if (gn.runtimeRule == prevRP.runtimeRule) { // && ?? == prevRP.position) {
                    val complete = this.graph.findCompleteNode(gn.runtimeRule.number, gn.startPosition, gn.matchedTextLength)
                        ?: throw ParseException("Internal error: Should never happen")
                    for (prev in previous) {
                        //val goalRp = RulePosition(prev.node.runtimeRule, 0,prev.atPosition)
                        //val rps = runtimeRuleSet.expectedItemRulePositionsTransitive[goalRp]
                        //    ?: setOf()// nextRulePosition(rp, gn.runtimeRule) //FIXME: not the right last param
                        //val rpsf = rps.filter { //TODO: do we need this filter?
                        //    //     rps2.contains(it.runtimeRule) &&
                        //    it.position==0 && it.runtimeRule.couldHaveChild(gn.runtimeRule, 0)
                        //}

                        val newTargetRPs = runtimeRuleSet.growsInto(tgtRP.runtimeRule, graph.runtimeGoalRule).map { RulePosition(it.runtimeRule, it.choice, 0) }
                        val fntp = if (tgtRP.runtimeRule.isSkip) { //TODO: do we really need to check if we are growing a skip node
                            newTargetRPs
                        } else {
                            newTargetRPs.filter {
                                //TODO: checking the graftinto rule against prev, position doesn't work!
                                (it.runtimeRule==prev.node.runtimeRule ) || runtimeRuleSet.calcCanGrowInto(it.runtimeRule, prev.node.runtimeRule, prev.atPosition)
                            }
                        }
                        fntp.forEach { newTgtRP ->
                            //val lookaheadItems = findLookaheadItems(newTgtRP, gn.runtimeRule, null)
                            val nextRps = runtimeRuleSet.nextRulePosition(tgtRP, complete.runtimeRule)
                            nextRps.forEach { nextRp -> //TODO: maybe check lookahead here also?
                                this.graph.createWithFirstChild(nextRp, newTgtRP, newParentRule, complete, previous) //maybe lookahead to wanted next token here (it would need to be part of RP)
                            }
                        }
                    }
                }
                RulePosition.END_OF_RULE -> { //This never happens!
                    //TODO: maybe we need to grow height of a skip non-terminal!
                    if (this.runtimeRuleSet.isSkipTerminal[gn.runtimeRule.number]) {
                        val rps = runtimeRuleSet.expectedSkipItemRulePositionsTransitive
                        val rpsf = rps.filter {
                            //TODO: do we need this filter?
                            it.runtimeRule.couldHaveChild(gn.runtimeRule, 0)
                        }
                        rpsf.forEach { rp ->
                            val complete = this.graph.findCompleteNode(gn.runtimeRule.number, gn.startPosition, gn.matchedTextLength)
                                ?: throw ParseException("Internal error: Should never happen")
                            val tgtRPs = runtimeRuleSet.nextRulePosition(rp, complete.runtimeRule)
                            tgtRPs.forEach {
                                //val lookaheadItems = findLookaheadItems(it, gn.runtimeRule, null)
                                this.graph.createWithFirstChild(it, it, it.runtimeRule, complete, previous) //FIXME: this is temp, find correct nextRp
                            }
                        }
                    } else {
                        for (prev in previous) {
                            //val goalRp = this.graph.goalNode.targetRulePosition
                            val goalRp = RulePosition(prev.node.runtimeRule, 0, prev.atPosition)
                            val rps = runtimeRuleSet.expectedItemRulePositionsTransitive[goalRp]
                                ?: setOf()// nextRulePosition(rp, gn.runtimeRule) //FIXME: not the right last param
                            val rps2 = runtimeRuleSet.expectedItemRulePositionsTransitive[prev.node.targetRulePosition]
                                ?: setOf()// nextRulePosition(rp, gn.runtimeRule) //FIXME: not the right last param
                            //val rps2 = this.runtimeRuleSet.calcGrowsInto(gn.runtimeRule, prev.node.runtimeRule, prev.atPosition)
                            val rpsf = rps.filter {
                                //TODO: do we need this filter?
                                //     rps2.contains(it.runtimeRule) &&
                                it.position == 0 && it.runtimeRule.couldHaveChild(gn.runtimeRule, 0)
                            }
                            val rpsf2 = rpsf.filter {
                                val nextRP = runtimeRuleSet.nextRulePosition(prev.node.targetRulePosition, it.runtimeRule)
                                nextRP.any { nrp ->
                                    val lh: Array<RulePosition> = runtimeRuleSet.expectedTerminalRulePositions[nrp]
                                        ?: arrayOf<RulePosition>()
                                    lh.any { trp ->
                                        trp.runtimeRule.itemsAt[trp.position].any {
                                            null != graph.findOrTryCreateLeaf(it, gn.nextInputPosition)
                                        }
                                    }
                                }
                            }
                            rpsf.forEach { rp ->
                                val complete = this.graph.findCompleteNode(gn.runtimeRule.number, gn.startPosition, gn.matchedTextLength)
                                    ?: throw ParseException("Internal error: Should never happen")
                                val tgtRules = runtimeRuleSet.firstSuperNonTerminal[rp.runtimeRule.number]

                                val newPrev = PreviousInfo(prev.node, prev.atPosition)
                                tgtRules.forEach {
                                    val newTgtRp = RulePosition(it, 0, 0)
                                    //val lookaheadItems = findLookaheadItems(newTgtRp, gn.runtimeRule, prev)
                                    this.graph.createWithFirstChild(newTgtRp, newTgtRp, rp.runtimeRule, complete, setOf(newPrev))  //FIXME: this is temp, find correct nextRp
                                }
                            }
                        }
                    }
                }
                else -> return false //throw ParseException("Should never happen")
            }
            return true
        } else {
            return false
        }
    }

    private fun tryGraftBack(gn: GrowingNode, previous: Set<PreviousInfo>): Boolean {
        var graftBack = false
        for (prev in previous) {
            //TODO: canGraftBack calls expectsItemAt which calls contains, so it has a nested loop, is it worth caching here?
            if (gn.canGraftBack(prev)) { // if hascompleteChildren && isStacked && prevInfo is valid
                graftBack = this.tryGraftBack(gn, prev)
            }
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

    private fun tryGraftInto(gn: GrowingNode, prev: PreviousInfo): Boolean {
        var result = false

        //if (gn.targetRulePosition.position == 0) return false

        val correctTgt = gn.currentRulePosition.isAtEnd && gn.targetRulePosition.runtimeRule.number == prev.node.runtimeRule.number //TODO: can we get the proper next target if we grow height to completion immediately!
        if (gn.isSkip) {
            // complete will not be null because we do not graftback unless gn has complete children
            // and graph will try to 'complete' a GraphNode when it is created.
            val complete = this.graph.findCompleteNode(gn.runtimeRule.number, gn.startPosition, gn.matchedTextLength)
                ?: throw ParseException("internal error, should never happen")
            //val lookaheadItems = findLookaheadItems(prev.node.targetRulePosition, gn.runtimeRule, prev)
            this.graph.growNextSkipChild(prev.node, complete)
            // info.node.duplicateWithNextSkipChild(gn);
            // this.graftInto(gn, info);
            result = result or true
        } else if (correctTgt && prev.node.expectsItemAt(gn.runtimeRule, prev.atPosition)) { //TODO: could we use gn.targetRulePosition here ?
            // complete will not be null because we do not graftback unless gn has complete children
            // and graph will try to 'complete' a GraphNode when it is created.
            val complete = this.graph.findCompleteNode(gn.runtimeRule.number, gn.startPosition, gn.matchedTextLength)
                ?: throw ParseException("internal error, should never happen")
            //this.graph.growNextChild(info.node.targetRulePosition, info.node, complete, info.atPosition)
            val nextRps = runtimeRuleSet.nextRulePosition(prev.node.currentRulePosition, gn.runtimeRule)
            nextRps.forEach { nextRp ->
                this.graph.growNextChild(nextRp, prev.node, complete, prev.atPosition)
            }

            result = result or true

        } else {
            // drop
            result = result or false
        }
        return result
    }

    private fun tryGrowWidth(gn: GrowingNode, previous: Set<PreviousInfo>): Boolean {
        var modified = false
        if (gn.canGrowWidth) { // don't grow width if its complete...cant graft back
            //val thisRP = RulePosition(gn.runtimeRule, gn.nextItemIndex, setOf())// gn.targetRulePosition

            val rps = runtimeRuleSet.expectedTerminalRulePositions[gn.currentRulePosition] ?: arrayOf()
            //val rps2 = previous.flatMap{prev->this.runtimeRuleSet.calcGrowsInto( ??? gn.runtimeRule, prev.node.runtimeRule, prev.atPosition)}
            val rpsf = rps.filter { //TODO: do we need this filter?
                runtimeRuleSet.calcCanGrowInto(it.runtimeRule, gn.currentRulePosition.runtimeRule, gn.currentRulePosition.position)
            }
            for (newTgtRp in rps) {
                //val nextRP = gn.rulePosition.next
                val items = newTgtRp.items //  rp.runtimeRule.itemsAt[rp.position]
                for (rr in items) {
                    if (rr.isTerminal) { //it might not be, e.g. if rp is in a multi,0 where it could be empty
                        val l = this.graph.findOrTryCreateLeaf(rr, gn.nextInputPosition)
                        if (null != l) {
                            val lhok = hasLookaheadAfterSkip(newTgtRp, gn.nextInputPosition + l.matchedTextLength)
                            if (lhok) {
                                this.graph.pushToStackOf(newTgtRp, l, gn, previous)
                            }
                        }
                    }
                }
            }
        }
        return modified
    }

    private fun hasLookaheadAfterSkip(newTgtRp:RulePosition, initialPosition: Int) : Boolean {
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
                for(skipLeaf in skipAheads) {
                    val pos = position + skipLeaf.matchedTextLength
                    foundLookahead = hasLookaheadAfterSkip(newTgtRp, pos)

                }
            }
        }
        return foundLookahead
    }

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