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
        this.graph.start(gr)
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
                val grownHeight = this.tryGrowHeight(gn, previous)

                // try reduce rest
                var graftBack = this.tryGraftBack(gn, previous)

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
                        val newRP = runtimeRuleSet.nextRulePosition(rp, rr)
                        newRP.forEach {
                            this.graph.pushToStackOf(it, l, gn, previous)
                            modified = true
                        }
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
        if (gn.hasCompleteChildren) {
            val thisRP = gn.targetRulePosition
            when (thisRP.position) {
                /*
                0 -> { // start
                    val thisRP: RulePosition = gn.targetRulePosition
                    val newParentRule = thisRP.runtimeRule
                    //if (gn.runtimeRule == prevRP.runtimeRule) { // && ?? == prevRP.position) {
                    val complete = this.graph.findCompleteNode(gn.runtimeRule.number, gn.startPosition, gn.matchedTextLength)
                        ?: throw ParseException("Internal error: Should never happen")
                    val newTargetRPs = runtimeRuleSet.nextRulePosition(thisRP, complete.runtimeRule)
                    newTargetRPs.forEach { tgtRP ->
                        this.graph.createWithFirstChild(tgtRP, newParentRule, complete, previous) //maybe lookahead to wanted next token here (it would need to be part of RP)
                    }
                }
                */
                RulePosition.END_OF_RULE -> {
                    //TODO: maybe we need to grow height of a skip non-terminal!
                    if (this.runtimeRuleSet.isSkipTerminal[gn.runtimeRule.number]) {
                        val rps = runtimeRuleSet.expectedSkipItemRulePositionsTransitive
                        val rpsf = rps.filter { //TODO: do we need this filter?
                            it.runtimeRule.couldHaveChild(gn.runtimeRule, 0)
                        }
                        rpsf.forEach { rp ->
                            val complete = this.graph.findCompleteNode(gn.runtimeRule.number, gn.startPosition, gn.matchedTextLength)
                                ?: throw ParseException("Internal error: Should never happen")
                            val tgtRPs = runtimeRuleSet.nextRulePosition(rp, complete.runtimeRule)
                            tgtRPs.forEach {
                                this.graph.createWithFirstChild(it, it.runtimeRule, complete, previous)
                            }
                        }
                    } else {
                        for (prev in previous) {
                            //val goalRp = this.graph.goalNode.targetRulePosition
                            val goalRp = RulePosition(prev.node.runtimeRule, prev.atPosition, setOf())
                            val rps = runtimeRuleSet.expectedItemRulePositionsTransitive[goalRp]
                                ?: setOf()// nextRulePosition(rp, gn.runtimeRule) //FIXME: not the right last param
                            val rpsf = rps.filter { //TODO: do we need this filter?
                                 it.position==0 && it.runtimeRule.couldHaveChild(gn.runtimeRule, 0)
                            }
                            rpsf.forEach { rp ->
                                val complete = this.graph.findCompleteNode(gn.runtimeRule.number, gn.startPosition, gn.matchedTextLength)
                                    ?: throw ParseException("Internal error: Should never happen")
                                val tgtRPs = runtimeRuleSet.nextRulePosition(rp, complete.runtimeRule)
                                val newPrev = PreviousInfo(prev.node, prev.atPosition)
                                tgtRPs.forEach {
                                    this.graph.createWithFirstChild(it, it.runtimeRule, complete, setOf(newPrev))
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

    private fun tryGraftInto(gn: GrowingNode, info: PreviousInfo): Boolean {
        var result = false

        if (gn.isSkip) {
            // complete will not be null because we do not graftback unless gn has complete children
            // and graph will try to 'complete' a GraphNode when it is created.
            val complete = this.graph.findCompleteNode(gn.runtimeRule.number, gn.startPosition, gn.matchedTextLength)
                ?: throw ParseException("internal error, should never happen")
            this.graph.growNextSkipChild(info.node, complete)
            // info.node.duplicateWithNextSkipChild(gn);
            // this.graftInto(gn, info);
            result = result or true
        } else if (info.node.expectsItemAt(gn.runtimeRule, info.atPosition)) {
            // complete will not be null because we do not graftback unless gn has complete children
            // and graph will try to 'complete' a GraphNode when it is created.
                val complete = this.graph.findCompleteNode(gn.runtimeRule.number, gn.startPosition, gn.matchedTextLength)
                    ?: throw ParseException("internal error, should never happen")
                //this.graph.growNextChild(info.node.targetRulePosition, info.node, complete, info.atPosition)
                val prp = info.node.targetRulePosition//RulePosition(info.node.runtimeRule, info.atPosition, setOf())
                val rps = runtimeRuleSet.nextRulePosition(prp, complete.runtimeRule)
                rps.forEach { rp ->
                    this.graph.growNextChild(rp, info.node, complete, info.atPosition)
                }
                result = result or true

        } else {
            // drop
            result = result or false
        }
        return result
    }

    /*
    private fun tryGrowWidth1(gn: GrowingNode, previous: Set<PreviousInfo>): Boolean {
        var modified = false
        if (gn.canGrowWidth) { // don't grow width if its complete...cant graft back
            val expectedNextTerminal = runtimeRuleSet.findNextExpectedTerminals(gn.runtimeRule, gn.nextItemIndex)
            for (rr in expectedNextTerminal) {
                val l = this.graph.findOrTryCreateLeaf(rr, gn.nextInputPosition)
                if (null != l) {
                    modified = this.pushStackNewRoot(l, gn, previous)
                }
            }
        }
        return modified
    }
*/
    private fun tryGrowWidth(gn: GrowingNode, previous: Set<PreviousInfo>): Boolean {
        var modified = false
        if (gn.canGrowWidth) { // don't grow width if its complete...cant graft back
            val thisRP = RulePosition(gn.runtimeRule, gn.nextItemIndex, setOf())// gn.targetRulePosition
            val rps = runtimeRuleSet.expectedTerminalRulePositions[thisRP] ?: arrayOf()
            for (rp in rps) {
                //val nextRP = gn.rulePosition.next
                for (rr in rp.runtimeRule.itemsAt[rp.position]) {
                    if (rr.isTerminal) { //it might not be, e.g. if rp is in a multi,0 where it could be empty
                        val l = this.graph.findOrTryCreateLeaf(rr, gn.nextInputPosition)
                        if (null != l) {
                            val newRP = runtimeRuleSet.nextRulePosition(rp, rr)
                            newRP.forEach {
                                this.graph.pushToStackOf(it, l, gn, previous)
                            }
                            //modified = this.pushStackNewRoot(l, rp, gn, previous) //TODO: leaf + RulePosition that terminal fits into
                        }
                    }
                }
            }
        }
        return modified
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
                    this.runtimeRuleSet.calcCanGrowInto(completeNode.runtimeRule, stack.runtimeRule, stack.nextItemIndex)
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