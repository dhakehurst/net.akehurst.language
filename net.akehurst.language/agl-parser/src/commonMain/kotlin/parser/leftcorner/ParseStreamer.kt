/*
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.akehurst.language.parser.leftcorner

import net.akehurst.language.agl.runtime.graph.GrowingNodeIndex
import net.akehurst.language.agl.runtime.graph.ParseGraph
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.parser.api.ParseStreamListener
import net.akehurst.language.parser.api.Rule
import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sppt.api.SpptDataNode
import net.akehurst.language.sppt.api.TreeData
import net.akehurst.language.sppt.treedata.ChildChain
import net.akehurst.language.sppt.treedata.TreeDataComplete2
import net.akehurst.language.sppt.treedata.TreeDataGrowing

/**
 * Sends the parse to a [ParseStreamListener] while parsing.
 *
 * After each season, the committed frontier is worked out from the graph-structured stack (GSS):
 * it is the left-most start position of any GSS node that is not on every path from the heads to the bottom.
 * A GSS node that is on every path has one, fixed, list of children so far.
 * The completed children of those nodes that end before the frontier can no longer change.
 * They are sent bottom-up, left to right, and then dropped from the tree data.
 *
 * Which children have been sent is remembered per cell of the children chains, not per node,
 * because the same (zero-length) node can be a child in more than one place.
 */
internal class ParseStreamer(
    private val listener: ParseStreamListener,
    private val sentence: Sentence,
    private val issues: IssueHolder,
    private val lagWarningLimit: Int
) {

    private companion object {
        // how far (in characters) the heads must move before cached parser data is dropped again
        const val PRUNE_STEP = 1000
    }

    private data class Ambiguity(val firstPosition: Int, var count: Int)

    // the cells of children chains whose child has been sent
    private val sentCells = hashSetOf<ChildChain<SpptDataNode>>()

    // the furthest sentence position reached by a sent node
    private var sentUpTo = 0

    private var initialSkipSent = false
    private var lagWarned = false
    private val ambiguities = linkedMapOf<Rule, Ambiguity>()
    private val ambiguousNodes = hashSetOf<SpptDataNode>()

    val committedUpTo: Int get() = sentUpTo

    // position up to which cached parser data was last dropped
    private var prunedUpTo = 0

    private var graph: ParseGraph? = null

    fun start(graph: ParseGraph) {
        this.graph = graph
        graph.treeData.recordCompleteChains = true
    }

    /**
     * Called by the parse graph when a second parse of the same text is found and the merge
     * cannot pick one. Only one parse can be sent, so this is an error and streaming stops (see [stopped]).
     * Each node is counted once.
     * When the new parse has the same children as the existing one (e.g. the same empty node found again
     * through hidden left recursion), it is the same tree, so not an ambiguity.
     */
    fun ambiguity(existing: SpptDataNode, newChildren: () -> List<SpptDataNode>) {
        if (ambiguousNodes.contains(existing)) return // already counted
        val existingAlternatives = graph?.treeData?.complete?.childrenFor(existing) ?: emptyList()
        if (existingAlternatives.isNotEmpty()) {
            val nc = newChildren()
            if (existingAlternatives.any { it.second == nc }) return
        }
        ambiguity(existing)
    }

    private fun ambiguity(existing: SpptDataNode) {
        if (ambiguousNodes.add(existing).not()) return // already counted
        val a = ambiguities[existing.rule]
        if (null == a) {
            ambiguities[existing.rule] = Ambiguity(existing.startPosition, 1)
        } else {
            a.count++
        }
    }

    /** true once an ambiguity was found: nothing more is sent, the parse should stop and call [fail] */
    val stopped: Boolean get() = ambiguities.isNotEmpty()

    fun seasonEnd(rp: RuntimeParserAgl) {
        if (stopped) return
        val graph = rp.graph
        val growing = graph.treeData
        if (initialSkipSent.not()) {
            initialSkipSent = true
            growing.complete.initialSkip?.let { sendSkip(it) }
        }
        val heads = graph._gss.heads
        if (heads.isEmpty()) return
        val (dominatorsBottomFirst, nonDominators) = dominators(graph, heads)
        val frontier = when {
            nonDominators.isEmpty() -> heads.minOf { it.startPosition }
            else -> nonDominators.minOf { it.startPosition }
        }
        for (d in dominatorsBottomFirst) {
            if (d.isComplete) continue // a complete head, what it becomes part of is not known yet
            val cells = unsentCells(growing.growingChainOf(d))
            for (cell in cells) {
                // a node ending at the frontier is held back: a zero-length node there (e.g. an empty optional)
                // can still be re-parsed by a head starting at the frontier
                if (cell.head.nextInputPosition >= frontier) break
                sendSubtree(growing, growing.complete, cell.head)
                if (stopped) return
                sentCells.add(cell)
            }
        }
        // forget cached data that can no longer be used, once the heads have moved on far enough
        val lowestHead = heads.minOf { it.nextInputPositionAfterSkip }
        if (lowestHead - prunedUpTo >= PRUNE_STEP) {
            prunedUpTo = lowestHead
            rp.dropCachesBefore(lowestHead)
            graph.dropIncompleteIndexBefore(lowestHead)
        }
        if (lagWarned.not()) {
            val headPos = heads.maxOf { it.nextInputPositionAfterSkip }
            val lag = headPos - sentUpTo
            if (lag > lagWarningLimit) {
                lagWarned = true
                val competing = nonDominators
                    .flatMap { it.state.runtimeRules }
                    .filter { it.isGoal.not() && it.isTerminal.not() }
                    .map { it.tag }
                    .distinct()
                val loc = sentence.locationFor(sentUpTo, 0)
                issues.warn(
                    loc,
                    "Streaming held back: $lag characters parsed since line ${loc.line} column ${loc.column} cannot be sent yet, " +
                            "because more than one parse is still possible. Rules still competing: ${competing.joinToString()}",
                    competing
                )
            }
        }
    }

    fun finish(growing: TreeDataGrowing<GrowingNodeIndex, SpptDataNode>) {
        val treeData = growing.complete
        if (initialSkipSent.not()) {
            initialSkipSent = true
            treeData.initialSkip?.let { sendSkip(it) }
        }
        if (stopped.not()) sendSubtree(growing, treeData, treeData.userRoot)
        if (stopped) {
            fail()
        } else {
            sentCells.clear()
            listener.finished(issues)
        }
    }

    fun fail() {
        raiseAmbiguityErrors()
        sentCells.clear()
        listener.failed(issues, sentUpTo)
    }

    private fun raiseAmbiguityErrors() {
        for ((rule, a) in ambiguities) {
            val loc = sentence.locationFor(a.firstPosition, 0)
            val times = if (a.count == 1) "" else " (${a.count} places found before stopping)"
            issues.error(
                loc,
                "Ambiguity: text matching rule '${rule.tag}' at line ${loc.line} column ${loc.column} can be parsed in more than one way$times. " +
                        "Streaming stopped, because only one parse can be sent. A preference rule or a priority choice in the grammar would remove the ambiguity.",
                rule.tag
            )
        }
        ambiguities.clear()
        ambiguousNodes.clear()
    }

    /** the cells after the last sent cell, oldest first */
    private fun unsentCells(chain: ChildChain<SpptDataNode>?): List<ChildChain<SpptDataNode>> {
        val out = ArrayList<ChildChain<SpptDataNode>>()
        var c = chain
        while (null != c && sentCells.contains(c).not()) {
            out.add(c)
            c = c.prev
        }
        out.reverse()
        return out
    }

    /**
     * Returns the GSS nodes that are on every path from the heads to the bottom (bottom of the stack first),
     * and the nodes that are not.
     * The GSS can have cycles (e.g. hidden left recursion through an empty match), so this uses reachability:
     * a node is on every path when the bottom cannot be reached from the heads without going through it.
     */
    private fun dominators(graph: ParseGraph, heads: List<GrowingNodeIndex>): Pair<List<GrowingNodeIndex>, List<GrowingNodeIndex>> {
        // fast path: one head with one path all the way down
        if (heads.size == 1) {
            val chain = ArrayList<GrowingNodeIndex>()
            val inChain = hashSetOf<GrowingNodeIndex>()
            var n = heads[0]
            while (inChain.add(n)) {
                chain.add(n)
                val prev = graph.previousOf(n)
                when (prev.size) {
                    0 -> {
                        chain.reverse()
                        return Pair(chain, emptyList())
                    }

                    1 -> n = prev.first()
                    else -> break
                }
            }
        }
        // general case
        val previous = hashMapOf<GrowingNodeIndex, Set<GrowingNodeIndex>>()
        val todo = ArrayList<GrowingNodeIndex>(heads)
        while (todo.isNotEmpty()) {
            val n = todo.removeAt(todo.lastIndex)
            if (previous.containsKey(n).not()) {
                val ps = graph.previousOf(n) - n
                previous[n] = ps
                for (p in ps) if (previous.containsKey(p).not()) todo.add(p)
            }
        }
        val all = previous.keys
        val bottoms = all.filter { previous[it]!!.isEmpty() }.toSet()
        val headSet = heads.toSet()
        fun reachesBottomAvoiding(avoid: GrowingNodeIndex): Boolean {
            val seen = hashSetOf<GrowingNodeIndex>()
            val stack = ArrayList<GrowingNodeIndex>()
            for (h in headSet) if (h != avoid) stack.add(h)
            while (stack.isNotEmpty()) {
                val n = stack.removeAt(stack.lastIndex)
                if (seen.add(n)) {
                    if (bottoms.contains(n)) return true
                    for (p in previous[n]!!) if (p != avoid && seen.contains(p).not()) stack.add(p)
                }
            }
            return false
        }
        val domSet = hashSetOf<GrowingNodeIndex>()
        val nonDom = ArrayList<GrowingNodeIndex>()
        for (n in all) {
            if (reachesBottomAvoiding(n)) nonDom.add(n) else domSet.add(n)
        }
        // order the dominators along one path from a head down to the bottom, then reverse
        val path = ArrayList<GrowingNodeIndex>()
        val onPath = hashSetOf<GrowingNodeIndex>()
        fun findPath(n: GrowingNodeIndex): Boolean {
            if (onPath.add(n).not()) return false
            path.add(n)
            if (bottoms.contains(n)) return true
            for (p in previous[n]!!) if (findPath(p)) return true
            path.removeAt(path.lastIndex)
            onPath.remove(n)
            return false
        }
        for (h in headSet) if (findPath(h)) break
        val domBottomFirst = path.filter { domSet.contains(it) }.reversed()
        return Pair(domBottomFirst, nonDom)
    }

    private fun markSent(node: SpptDataNode) {
        if (node.nextInputPosition > sentUpTo) sentUpTo = node.nextInputPosition
    }

    private fun sendSkip(skipData: TreeData) {
        val r = skipData.root ?: return
        listener.skip(r.startPosition, r.nextInputPosition)
    }

    private class Item(
        val node: SpptDataNode,
        val treeData: TreeData,
        val growing: TreeDataGrowing<GrowingNodeIndex, SpptDataNode>?,
        val expanded: Boolean,
        val numChildren: Int
    )

    /**
     * Send [top] and every node below it that has not been sent yet, in post-order.
     * A child that was already sent (its chain cell is in [sentCells]) is not sent again;
     * the listener already has it as the most recent unused node(s).
     */
    private fun sendSubtree(growing: TreeDataGrowing<GrowingNodeIndex, SpptDataNode>?, treeData: TreeData, top: SpptDataNode) {
        val stack = ArrayList<Item>()
        stack.add(Item(top, treeData, growing, false, 0))
        while (stack.isNotEmpty()) {
            val item = stack.removeAt(stack.lastIndex)
            val node = item.node
            val td = item.treeData
            when {
                item.expanded -> {
                    listener.branch(node, item.numChildren)
                    markSent(node)
                    td.skipDataAfter(node)?.let { sendSkip(it) }
                    // a zero-length node can be a child in more than one place, so keep it
                    if (node.startPosition != node.nextInputPosition) (td as? TreeDataComplete2)?.removeSent(node)
                }

                node.rule.isEmbedded -> {
                    val emb = td.embeddedFor(node)
                    if (null == emb) {
                        listener.leaf(node)
                        markSent(node)
                        td.skipDataAfter(node)?.let { sendSkip(it) }
                    } else {
                        // the embedded tree is sent whole, after its initial skip
                        stack.add(Item(node, td, item.growing, true, 1))
                        stack.add(Item(emb.userRoot, emb, null, false, 0))
                        emb.initialSkip?.let { sendSkip(it) }
                    }
                }

                node.rule.isTerminal -> {
                    listener.leaf(node)
                    markSent(node)
                    td.skipDataAfter(node)?.let { sendSkip(it) }
                    (td as? TreeDataComplete2)?.removeSent(node)
                }

                else -> {
                    val alternatives = td.childrenFor(node)
                    if (alternatives.size > 1) ambiguity(node)
                    if (stopped) return
                    val chain = item.growing?.completeChainOf(node)
                    item.growing?.removeCompleteChain(node)
                    // when some children were already sent, use the alternative they belong to
                    val children = when {
                        null != chain && alternatives.size > 1 -> chain.toList()
                        else -> alternatives.firstOrNull()?.second ?: emptyList()
                    }
                    val unsent = unsentChildrenOf(chain, children)
                    stack.add(Item(node, td, item.growing, true, children.size))
                    for (i in unsent.indices.reversed()) {
                        stack.add(Item(unsent[i], td, item.growing, false, 0))
                    }
                }
            }
        }
    }

    /**
     * The children not sent yet: a suffix of [children].
     * Only a node completed from a growing parent can have sent children; [chain] is its recorded chain of children.
     */
    private fun unsentChildrenOf(chain: ChildChain<SpptDataNode>?, children: List<SpptDataNode>): List<SpptDataNode> {
        if (null == chain || sentCells.isEmpty()) return children
        // the recorded chain must be the children chosen (it may belong to another alternative)
        if (chain.size != children.size) return children
        var numUnsent = 0
        var c: ChildChain<SpptDataNode>? = chain
        var i = children.size - 1
        while (null != c && sentCells.contains(c).not()) {
            if (c.head !== children[i] && c.head != children[i]) return children
            numUnsent++
            c = c.prev
            i--
        }
        // remove the sent cells of this chain, they are not needed any more
        while (null != c) {
            sentCells.remove(c)
            c = c.prev
        }
        return children.subList(children.size - numUnsent, children.size)
    }
}
