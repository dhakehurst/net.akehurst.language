/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.aMinimalVersion

import net.akehurst.language.agl.automaton.*
import net.akehurst.language.agl.collections.GraphStructuredStack
import net.akehurst.language.agl.collections.binaryHeap
import net.akehurst.language.agl.parser.InputFromString
import net.akehurst.language.agl.runtime.graph.TreeData
import net.akehurst.language.agl.runtime.graph.TreeDataComplete
import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.api.automaton.ParseAction
import net.akehurst.language.api.sppt.SpptDataNode
import net.akehurst.language.collections.LazyMutableMapNonNull
import net.akehurst.language.collections.lazyMutableMapNonNull
import net.akehurst.language.collections.mutableQueueOf

internal class Automaton(
    val runtimeRuleSet: RuntimeRuleSet,
    val userGoalRule: RuntimeRule
) {
    companion object {
        var nextNumber: Int = 0

        fun merge(trans: Set<Transition>): Set<Transition> {
            val g = trans.groupBy { Triple(it.source, it.target, it.action) }
            val m = g.map { me ->
                val from = me.key.first
                val action = me.key.third
                val to = me.key.second
                val lh = me.value.map { it.lh }.reduce { acc, it -> acc.union(it) }
                val up = me.value.map { it.up }.reduce { acc, it -> acc.union(it) }
                val ctx = me.value.flatMap { it.context }.toSet()
                Transition(from, to, action, lh, up, ctx)
            }.toSet()
            return m
        }
    }

    val number = nextNumber++
    private val _states = mutableMapOf<RulePosition, State>()
    val states get() = _states.values

    val goalRule by lazy { runtimeRuleSet.goalRuleFor[userGoalRule] }
    val startState = createState(RulePosition(goalRule, 0, 0))

    fun createState(rp: RulePosition): State {
        var s = _states[rp]
        return if (null == s) {
            s = State(this, this._states.size, rp)
            _states[rp] = s
            s
        } else {
            s
        }
    }

    fun usedAutomatonToString(withStates: Boolean = false): String {
        val b = StringBuilder()
        val states = this.states
        val transitions = states.flatMap { it.allOutTransition }.toSet()

        //b.append("UsedRules: ${this.usedRules.size}  States: ${states.size}  Transitions: ${transitions.size} ")
        b.append(" States: ${states.size}  Transitions: ${transitions.size} ")
        b.append("\n")

        if (withStates) {
            states.forEach {
                val str = "$it {${it.allOutTransition.flatMap { it.context }.map { it.number }}}"
                b.append(str).append("\n")
            }
        }

        transitions.sortedBy { it.source.rp.toString() }.sortedBy { it.target.rp.toString() }
            .forEach { tr ->
                val prev = tr.source.allOutTransition.flatMap { it.context }.toSet()
                    .map { it.number } //transitionsByPrevious.entries.filter { it.value?.contains(tr) ?: false }.map { it.key?.number?.value }
                    .sorted()
                val frStr = "${tr.source.number}:(${tr.source.rp})"
                val toStr = "${tr.target.number}:${tr.target.rp}"
                val trStr = "$frStr --> $toStr"
                val lh = "[${tr.lh.fullContent.joinToString { it.tag }}]"
                val up = "(${tr.up.fullContent.joinToString { it.tag }})"
                b.append(" ${tr.action} ")
                b.append(trStr)
                b.append(lh)
                b.append(up)
                b.append(" {${prev.joinToString()}} ")
                b.append("\n")
            }

        return b.toString()
    }

    override fun hashCode(): Int = this.number
    override fun equals(other: Any?): Boolean = when {
        other !is Automaton -> false
        this.number != other.number -> false
        else -> true
    }

    override fun toString(): String = "$number"
}

internal class State(
    val automaton: Automaton,
    val number: Int,
    val rp: RulePosition
) {
    val outTransitionsByKey = mutableMapOf<State, Set<Transition>>()
    val allOutTransition get() = outTransitionsByKey.values.flatten().toSet()

    fun outTransFor(key: State): Set<Transition>? {
        return outTransitionsByKey[key]
    }

    fun addOutTrans(key: State, trans: Set<Transition>) {
        val existing = outTransFor(key) ?: emptySet()
        val t = Automaton.merge(trans + existing)
        outTransitionsByKey[key] = t
    }

    override fun hashCode(): Int = arrayOf(automaton.number, this.number).contentHashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is State -> false
        this.automaton.number != other.automaton.number -> false
        this.number != other.number -> false
        else -> true
    }

    override fun toString(): String = "$number/${automaton.number} $rp"
}

internal data class Transition(
    val source: State,
    val target: State,
    val action: ParseAction,
    val lh: LookaheadSetPart,
    val up: LookaheadSetPart,
    val context: Set<State>
) {

    override fun toString(): String = "${source.rp} -$action[${lh}]($up)-> ${target.rp} {${context.joinToString { it.rp.toString() }}}"
}

internal data class GSSNode(
    val state: State,
    val rlh: LookaheadSetPart,
    val sp: Int,
    val nip: Int,
//    val numNonSkipChildren: Int
) {
    val isGoal get() = this.state.rp.isGoal
    val isComplete get() = this.state.rp.isAtEnd
    val isEmptyMatch get() = this.sp == this.nip
}

internal class CompleteNode(
    override val rule: RuntimeRule,
    override val startPosition: Int,
    override val nextInputPosition: Int,
    override val option: Int
) : SpptDataNode {

    private val _hashCode_cache = arrayOf(rule, /*option,*/ startPosition, nextInputPosition).contentHashCode()
    override fun hashCode(): Int = _hashCode_cache
    override fun equals(other: Any?): Boolean = when {
        other !is CompleteNode -> false
        this.startPosition != other.startPosition -> false
        this.nextInputPosition != other.nextInputPosition -> false
        //this.option != other.option -> false
        this.rule != other.rule -> false
        else -> true
    }

    override fun toString(): String = "CN(${rule.tag}|${option},$startPosition-$nextInputPosition)"
}

internal class MinimalParser private constructor(
    val automaton: Automaton,
    val isSkip: Boolean,
    val skipAutomaton: Automaton?
) {

    companion object {
        const val TRACE = false

        fun parser(goalRuleName: String, runtimeRuleSet: RuntimeRuleSet): MinimalParser {
            val userGoalRule = runtimeRuleSet.findRuntimeRule(goalRuleName)
            val automaton = Automaton(runtimeRuleSet, userGoalRule)
            val skipAutomaton = skipAutomaton(runtimeRuleSet)
            val goalRule = runtimeRuleSet.findRuntimeRule(goalRuleName)
            return MinimalParser(automaton, false, skipAutomaton)
        }

        private fun skipAutomaton(runtimeRuleSet: RuntimeRuleSet): Automaton? {
            val skipRules = runtimeRuleSet.runtimeRules.filter { it.isSkip }
            return if (skipRules.isEmpty()) {
                null
            } else {
                val skipChoiceRule = RuntimeRule(runtimeRuleSet.number, RuntimeRuleSet.SKIP_CHOICE_RULE_NUMBER, RuntimeRuleSet.SKIP_CHOICE_RULE_TAG, false).also {
                    val options = skipRules.mapIndexed { index, skpRl ->
                        RuntimeRuleRhsConcatenation(it, listOf(skpRl))
                    }
                    val rhs = RuntimeRuleRhsChoice(it, RuntimeRuleChoiceKind.LONGEST_PRIORITY, options)
                    it.setRhs(rhs)
                }
                val skipMultiRule = RuntimeRule(runtimeRuleSet.number, RuntimeRuleSet.SKIP_RULE_NUMBER, RuntimeRuleSet.SKIP_RULE_TAG, false)
                    .also { it.setRhs(RuntimeRuleRhsListSimple(it, 1, -1, skipChoiceRule)) }
                Automaton(runtimeRuleSet, skipMultiRule)
            }
        }

        private val GSSNode.complete get() = CompleteNode(this.state.rp.rule, this.sp, this.nip, this.state.rp.option)

        private fun GraphStructuredStack<GSSNode>.setRoot(
            state: State,
            rlh: LookaheadSetPart,
            sp: Int,
            nip: Int,
//            nc: Int
        ): GSSNode {
            val nn = GSSNode(
                state = state,
                rlh = rlh,
                sp = sp,
                nip = nip,
//                numNonSkipChildren = nc, //used in TreeData to get index of next child
            )
            this.root(nn)
            return nn
        }

        private fun GraphStructuredStack<GSSNode>.pushNode(
            prev: GSSNode,
            state: State,
            rlh: LookaheadSetPart,
            sp: Int,
            nip: Int,
//            nc: Int
        ): GSSNode {
            val nn = GSSNode(
                state = state,
                rlh = rlh,
                sp = sp,
                nip = nip,
//                numNonSkipChildren = nc, //used in TreeData to get index of next child
            )
            this.push(prev, nn)
            return nn
        }

        val CompleteNode.length get() = this.nextInputPosition - this.startPosition

        fun TreeData<GSSNode, CompleteNode>.setFirstChildForParent(parent: GSSNode, child: CompleteNode) {
            if (parent.isComplete) {
                this.setFirstChildForComplete(parent.complete, child, parent.state.rp.rule.isChoiceAmbiguous)
            } else {
                this.setFirstChildForGrowing(parent, child)
            }
        }

        private fun TreeData<GSSNode, CompleteNode>.setNextChildInParent(oldParent: GSSNode, newParent: GSSNode, nextChild: CompleteNode) {
            if (newParent.isComplete) {
                this.setNextChildForCompleteParent(oldParent, newParent.complete, nextChild, newParent.state.rp.rule.isChoiceAmbiguous)
            } else {
                this.setNextChildForGrowingParent(oldParent, newParent, nextChild)
            }
        }

    }

    private fun tracePeekHead(curPos: Int, hd: GSSNode) {
        if (isSkip) {
            //parsing skip, don't log
        } else {
            println()
            println("At $curPos, Head: (${hd.state.rp}/${hd.rlh})[${hd.sp}-${hd.nip}]")
        }
    }

    private fun traceTrans(hd: GSSNode, pv: State, tr: Transition, b: Boolean) {
        if (isSkip) {
            //parsing skip, don't log
        } else {
            println("  (${hd.state.rp}/${hd.rlh})-->${pv.rp}")
            val m = if (b) "taken" else "ignored"
            println("  $m $tr")
        }
    }

    private fun traceTrans(hd: GSSNode, pv: GSSNode, pp: GSSNode?, tr: Transition, b: Boolean) {
        if (isSkip) {
            //parsing skip, don't log
        } else {
            println("  (${hd.state.rp}/${hd.rlh})-->(${pv.state.rp}/${pv.rlh})[${pv.sp}-${pv.nip}]-->(${pp?.state?.rp}/${pp?.rlh})[${pp?.sp}-${pp?.nip}]")
            val m = if (b) "taken" else "ignored"
            println("  $m $tr")
        }
    }

    private fun traceDrop(hd: GSSNode) {
        if (isSkip) {
            //parsing skip, don't log
        } else {
            println("  Dropped Stack: (${hd.state.rp}/${hd.rlh})[${hd.sp}-${hd.nip}]")
        }
    }

    val ss = automaton.startState
    var sppf = TreeData<GSSNode, CompleteNode>(automaton.number)
    val gss = GraphStructuredStack<GSSNode>(binaryHeap { parent, child ->
        //val _growingHeadHeap: BinaryHeapFifo<GrowingNodeIndex, GrowingNode> = binaryHeapFifo { parent, child ->
        // Ordering rules:
        // 1) nextInputPosition lower number first
        // 2) shift before reduce (reduce happens if state.isAtEnd)
        // 3) startPosition lower number first
        when {
            // 1) nextInputPosition lower number first
            parent.nip < child.nip -> 1
            parent.nip > child.nip -> -1
            else -> when {
                // 2) shift before reduce (reduce happens if state.isAtEnd)
                parent.state.rp.isAtEnd && child.state.rp.isAtEnd -> 0
                parent.state.rp.isAtEnd -> -1 // shift child first
                child.state.rp.isAtEnd -> 1 // shift parent first
                else -> when {
                    // 3) startPosition higher number first
                    parent.sp < child.sp -> -1
                    parent.sp > child.sp -> 1
                    else -> 0
                }
            }
        }
    })

    val skipTerms by lazy { automaton.runtimeRuleSet.skipTerminals }
    val skipParser: MinimalParser? by lazy { skipAutomaton?.let { MinimalParser(skipAutomaton, true, null) }?.also { it.input = this.input } }
    val embedded = mutableMapOf<Pair<RuntimeRuleSet, RuntimeRule>, MinimalParser>()

    var input: InputFromString? = null

    fun parse(sentence: String): TreeDataComplete<CompleteNode> {
        //this.reset()
        this.input = InputFromString(-1, sentence)
        this.skipParser?.input = this.input
        val td = this.parseAt(0, LookaheadSetPart.EOT)
        val sppt = td ?: error("Parse Failed")
        return sppt
    }

    fun reset() {
        this.input = null
        this.skipParser?.input = null
        this.gss.clear()
        this.sppf = TreeData(automaton.number)
    }

    private fun parseAt(position: Int, eot: LookaheadSetPart): TreeDataComplete<CompleteNode>? {
        val initialSkipData = if (null == skipParser) {
            null
        } else {
            val slh = firstTerminals(ss.rp, ss.rp, eot)
                .filterNot { it.terminalRule.isEmptyTerminal }
                .map { it.terminalRule }
                .let { LookaheadSetPart.createFromRuntimeRules(it.toSet()) }
                .union(eot)
            tryParseSkip(position, slh)
        }
        val nip = initialSkipData?.root?.nextInputPosition ?: position
        val stNd = gss.setRoot(ss, eot, position, nip)//, 0)
        sppf.initialise(stNd, initialSkipData)

        var currentNextInputPosition = nip
        val doneEmpties = mutableSetOf<Pair<State, Set<GSSNode>>>()
        while (gss.hasNextHead) {
            val hd = gss.peekFirstHead!!
            if (TRACE) tracePeekHead(currentNextInputPosition, hd)
            if (hd.nip > currentNextInputPosition) {
                doneEmpties.clear()
                currentNextInputPosition = hd.nip
            }
            if (hd.isEmptyMatch && doneEmpties.contains(Pair(hd.state, gss.peekPrevious(hd)))) {
                //don't do it again
                gss.dropStack(hd)
                if (TRACE) traceDrop(hd)
            } else {
                if (hd.isEmptyMatch) {
                    doneEmpties.add(Pair(hd.state, gss.peekPrevious(hd)))
                }
                when {
                    hd.isGoal && hd.isComplete -> recordGoal(sppf, hd)
                    hd.isGoal && hd.isComplete.not() -> growIncomplete2(hd, ss, eot)
                    hd.isComplete.not() -> growIncomplete(hd, eot)
                    hd.isComplete -> growComplete(hd, eot)
                }
            }
        }
        return if (sppf.complete.root == null) {
            null
        } else {
            sppf.complete
        }
    }

    private fun recordGoal(sppf: TreeData<GSSNode, CompleteNode>, hd: GSSNode) {
        sppf.complete.setRoot(hd.complete)
        gss.dropStack(hd)
    }

    private fun growIncomplete(hd: GSSNode, peot: LookaheadSetPart) {
        for (pv in gss.peekPrevious(hd)) {
            growIncomplete2(hd, pv.state, peot)
        }
    }

    private fun growIncomplete2(hd: GSSNode, pv: State, peot: LookaheadSetPart) {
        var grown = false
        val trans = transitionsIncomplete(hd.state, pv).filter { it.context.contains(pv) }
        for (tr in trans) {
            val b = when (tr.action) {
                ParseAction.WIDTH -> doWidth(hd, tr, peot)
                ParseAction.EMBED -> doEmbed(hd, tr, peot)
                else -> error("Error")
            }
            grown = grown || b
            if (TRACE) traceTrans(hd, pv, tr, b)
        }
        if (grown.not()) gss.dropStack(hd)
    }

    private fun growComplete(hd: GSSNode, peot: LookaheadSetPart) {
        var headGrownHeight = false
        var headGrownGraft = false
        val dropPrevs = mutableMapOf<GSSNode, Boolean>()
        for (pv in gss.peekPrevious(hd)) {
            var prevGrownHeight = false
            var prevGrownGraft = false
            val pps = gss.peekPrevious(pv)
            if (pps.isEmpty()) {
                val (h, g) = growComplete2(hd, pv, null, peot)
                prevGrownHeight = prevGrownHeight || h
                prevGrownGraft = prevGrownGraft || g
            } else {
                for (pp in pps) {
                    val (h, g) = growComplete2(hd, pv, pp, peot)
                    prevGrownHeight = prevGrownHeight || h
                    prevGrownGraft = prevGrownGraft || g
                }
            }
            if (prevGrownHeight.not()) dropPrevs[pv] = prevGrownGraft
            headGrownHeight = headGrownHeight || prevGrownHeight
            headGrownGraft = headGrownGraft || prevGrownGraft
        }
        cleanUpGss(hd, headGrownHeight, headGrownGraft, dropPrevs)
    }

    private fun cleanUpGss(hd: GSSNode, headGrownHeight: Boolean, headGrownGraft: Boolean, dropPrevs: Map<GSSNode, Boolean>) {
        when {
            headGrownHeight.not() && headGrownGraft.not() -> gss.dropStack(hd)
            headGrownHeight && headGrownGraft.not() -> gss.dropStack(hd)
            headGrownHeight.not() && headGrownGraft -> gss.dropStack(hd)
            headGrownHeight && headGrownGraft -> gss.dropStack(hd)
        }
        dropPrevs.forEach {
            if (it.value) {
                gss.dropStack(it.key)
            } else {
                gss.dropStack(it.key)
            }
        }
    }

    private fun growComplete2(hd: GSSNode, pv: GSSNode, pp: GSSNode?, peot: LookaheadSetPart): Pair<Boolean, Boolean> {
        var grownHeight = false
        var grownGraft = false
        val pps = pp?.state ?: ss
        val trans = transitionsComplete(hd.state, pv.state, pps).filter { it.context.contains(pv.state) }
        for (tr in trans) {
            when (tr.action) {
                ParseAction.GOAL -> {
                    val b = doGoal(hd, pv, tr, peot)
                    grownGraft = grownGraft || b
                    if (TRACE) traceTrans(hd, pv, pp, tr, b)
                }

                ParseAction.HEIGHT -> {
                    val b = doHeight(hd, pv, tr, peot)
                    grownHeight = grownHeight || b
                    if (TRACE) traceTrans(hd, pv, pp, tr, b)
                }

                ParseAction.GRAFT -> {
                    val b = doGraft(hd, pv, pp!!, tr, peot)
                    grownGraft = grownGraft || b
                    if (TRACE) traceTrans(hd, pv, pp, tr, b)
                }

                else -> error("Error")
            }
        }
        return Pair(grownHeight, grownGraft)
    }

    private fun doGoal(hd: GSSNode, pv: GSSNode, tr: Transition, peot: LookaheadSetPart): Boolean {
        val lh = tr.lh.resolve(peot, pv.rlh)
        return if (input!!.isLookingAtAnyOf(lh, hd.nip)) {
            val nn = gss.setRoot(tr.target, pv.rlh, hd.sp, hd.nip)//, nc)
            sppf.setNextChildInParent(pv, nn, hd.complete)
            true
        } else {
            false
        }
    }

    private fun doWidth(hd: GSSNode, tr: Transition, peot: LookaheadSetPart): Boolean {
        val lf = input!!.findOrTryCreateLeaf(tr.target.rp.rule, hd.nip)
        return if (null != lf) {
            val slh = tr.lh.resolve(peot, hd.rlh)
            val skipData = tryParseSkip(lf.nextInputPosition, slh)
            val nip = if (null != skipData) skipData.root!!.nextInputPosition!! else lf.nextInputPosition
            if (input!!.isLookingAtAnyOf(slh, nip)) {
                val rlh = LookaheadSetPart.EMPTY
                val nn = gss.pushNode(hd, tr.target, rlh, lf.startPosition, nip)//, 0)
                if (null != skipData) sppf.setSkipDataAfter(nn.complete, skipData)
                true
            } else {
                false
            }
        } else {
            false
        }
    }

    private fun doHeight(hd: GSSNode, pv: GSSNode, tr: Transition, peot: LookaheadSetPart): Boolean {
        val lh = tr.lh.resolve(peot, pv.rlh)
        val nip = hd.nip
        return if (input!!.isLookingAtAnyOf(lh, nip)) {
            val rlh = tr.up.resolve(peot, pv.rlh)
            val nn = gss.pushNode(pv, tr.target, rlh, hd.sp, hd.nip)//, 1)
            if (nn.isComplete) {
                val existing = sppf.preferred(nn.complete)
                if (null == existing) {
                    sppf.setFirstChildForParent(nn, hd.complete)
                    true
                } else {
                    when {
                        existing.length > nn.complete.length -> {
                            false
                        }

                        else -> {
                            sppf.setFirstChildForParent(nn, hd.complete)
                            true
                        }
                    }
                }
            } else {
                sppf.setFirstChildForParent(nn, hd.complete)
                true
            }
        } else {
            false
        }
    }

    private fun doGraft(hd: GSSNode, pv: GSSNode, pp: GSSNode, tr: Transition, peot: LookaheadSetPart): Boolean {
        val lh = tr.lh.resolve(peot, pv.rlh)
        return if (input!!.isLookingAtAnyOf(lh, hd.nip)) {
            val rlh = pv.rlh
//            val nc = pv.numNonSkipChildren + 1
            val nn = gss.pushNode(pp!!, tr.target, rlh, pv.sp, hd.nip)//, nc)
            if (nn.isComplete) {
                val existing = sppf.preferred(nn.complete)
                if (null == existing) {
                    sppf.setNextChildInParent(pv, nn, hd.complete)
                    true
                } else {
                    when {
                        existing.length > nn.complete.length -> {
                            false
                        }

                        else -> {
                            sppf.setNextChildInParent(pv, nn, hd.complete)
                            true
                        }
                    }
                }
            } else {
                sppf.setNextChildInParent(pv, nn, hd.complete)
                true
            }
        } else {
            false
        }
    }

    private fun doEmbed(hd: GSSNode, tr: Transition, peot: LookaheadSetPart): Boolean {
        val embeddedRhs = tr.target.rp.rule.rhs as RuntimeRuleRhsEmbedded
        val embeddedRRS = embeddedRhs.embeddedRuntimeRuleSet
        val embeddedGoal = embeddedRhs.embeddedStartRule
        val key = Pair(embeddedRRS, embeddedGoal)
        val embeddedParser = if (embedded.containsKey(key)) {
            embedded[key]!!
        } else {
            val p = MinimalParser.parser(embeddedGoal.tag, embeddedRRS)
            embedded[key] = p
            p
        }
        embeddedParser.input = this.input
        val embeddedEOT = tr.lh.unionContent(this.skipTerms).resolve(peot, hd.rlh)
        val embed = embeddedParser.parseAt(hd.nip, embeddedEOT)
        return if (null != embed) {
            val slh = tr.lh.resolve(peot, hd.rlh)
            val skipData = tryParseSkip(embed.root!!.nextInputPosition, slh)
            val nip = if (null != skipData) skipData.root!!.nextInputPosition!! else embed.root!!.nextInputPosition
            val rlh = LookaheadSetPart.EMPTY
            val nn = gss.pushNode(hd, tr.target, rlh, embed.root!!.startPosition, nip)//, 0)
            sppf.complete.setEmbeddedTreeFor(nn.complete, embed)
            if (null != skipData) sppf.setSkipDataAfter(nn.complete, skipData)
            true
        } else {
            false
        }
    }

    private fun tryParseSkip(position: Int, slh: LookaheadSetPart): TreeDataComplete<CompleteNode>? {
        return if (null == skipParser) {
            null
        } else {
            skipParser!!.reset()
            skipParser!!.input = this.input
            skipParser!!.parseAt(position, slh)
        }
    }

    // Automaton
    private fun transitionsIncomplete(hd: State, pv: State): Set<Transition> {
        val key = pv
        val trans = hd.outTransFor(key)
        return if (null == trans) {
            clearCache()
            val pe = when {
                hd.rp.isGoal -> LookaheadSetPart.EOT
                else -> LookaheadSetPart.RT
            }
            val ts = firstTerminals(pv.rp, hd.rp, pe).map {
                val action = when {
                    it.terminalRule.isEmbedded -> ParseAction.EMBED
                    else -> ParseAction.WIDTH
                }
                Transition(
                    hd,
                    automaton.createState(it.terminalRule.asTerminalRulePosition),
                    action,
                    it.parentExpectedAt,
                    LookaheadSetPart.EMPTY,
                    setOf(pv)
                )
            }.toSet()
            val trs = Automaton.merge(ts)
            hd.addOutTrans(key, trs)
            hd.outTransitionsByKey[key]!!
        } else {
            trans
        }
    }

    private fun transitionsComplete(hd: State, pv: State, pp: State): Set<Transition> {
        val key = pv//Pair(pv, pp)
        val trans = hd.outTransFor(key)
        return if (null == trans) {
            val ts = parentsInContext(pp.rp, pv.rp, hd.rp.rule).map { pn ->
                val action = when {
                    pn.rulePosition.isGoal -> ParseAction.GOAL
                    pn.firstPosition -> ParseAction.HEIGHT
                    else -> ParseAction.GRAFT
                }
                val tgt = pn.rulePosition
                val lh = pn.expectedAt
                val up = pn.parentExpectedAt
                Transition(
                    hd,
                    automaton.createState(tgt),
                    action,
                    lh,
                    up,
                    setOf(pv)
                )
            }.toSet()
            val trs = Automaton.merge(ts)
            hd.addOutTrans(key, trs)
            hd.outTransitionsByKey[key]!!
        } else {
            trans
        }
    }

    /**
     * for an incomplete rule compute
     * the next terminal in the given context
     * and the lookahead set of terminals expected after it.
     */
    private fun firstTerminals(pv: RulePosition, rp: RulePosition, parentFollow: LookaheadSetPart): Set<FirstTerminalInfo> {
        return if (rp.isGoal && rp.isAtEnd) {
            emptySet()
        } else {
            processClosure(ClosureGraph(pv, rp, parentFollow))
            this._firstTerminal[pv][rp]
        }
    }

    /**
     * for a complete rule compute
     * the next rule-position in possible parents for the given context
     * whether it is a first position in the parent
     * the lookahead (set of terminals expected after the next rule position in parent
     * the set of terminals expected at the end of the parent rule
     */
    private fun parentsInContext(pp: RulePosition, pv: RulePosition, cr: RuntimeRule): Set<ParentNext> {
        processClosure(ClosureGraph(pp, pv, LookaheadSetPart.RT))
        val ctx = pv
        return this._parentInContext[ctx][cr]
    }

    private fun processClosure(graph: ClosureGraph) {
        val todoList = mutableQueueOf<ClosureItem>()
        todoList.enqueue(graph.root)
        while (todoList.isNotEmpty) {
            val cls = todoList.dequeue()
            for (item in cls.rulePosition.items) {
                when {
                    item.isTerminal -> graph.addChild(cls, item.asTerminalRulePosition)
                    item.isNonTerminal -> {
                        val childRps = item.rulePositionsAtStart
                        for (childRp in childRps) {
                            val child = graph.addChild(cls, childRp)
                            if (null != child) {
                                todoList.enqueue(child)
                            }
                        }
                    }

                    else -> error("Internal Error: should never happen")
                }
            }
        }
        graph.resolveAllChildParentInfo()

        // cache stuff
        for (dwn in graph.root.downInfo) {
            cacheFirstTerminalInContext(graph.root.context, graph.root.rulePosition, dwn)
            cachePossibleContext(graph.root.rulePosition, graph.root.context)
        }
        for (cls in graph.nonRootClosures) {
            if (cls.downInfo.isEmpty()) {
                cachePossibleContext(cls.rulePosition, cls.context)
            } else {
                when {
                    cls.rulePosition.isAtStart ||
                            cls.rulePosition.isTerminal -> {
                        cacheParentInContext(cls.context, cls.rulePosition.rule, cls.parentNext)
                        cachePossibleContext(cls.rulePosition, cls.context)
                    }

                    else -> cls.downInfo.forEach { dwn ->
                        cacheFirstTerminalInContext(cls.context, cls.rulePosition, dwn)
                        cachePossibleContext(cls.rulePosition, cls.context)
                    }
                }
            }
        }
    }

    // ----- cache -----
    private val _firstTerminal = lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RulePosition, MutableSet<FirstTerminalInfo>>> { lazyMutableMapNonNull { hashSetOf() } }
    private val _parentInContext = lazyMutableMapNonNull<RulePosition, LazyMutableMapNonNull<RuntimeRule, MutableSet<ParentNext>>> { lazyMutableMapNonNull { hashSetOf() } }
    private val _possibleContexts = lazyMutableMapNonNull<RulePosition, MutableSet<RulePosition>> { hashSetOf() }

    fun clearCache() {
        _firstTerminal.clear()
        _parentInContext.clear()
        _possibleContexts.clear()
    }

    fun cachePossibleContext(rp: RulePosition, ctx: RulePosition) {
        this._possibleContexts[rp].add(ctx)
    }

    fun cacheFirstTerminalInContext(ctx: RulePosition, rp: RulePosition, fti: FirstTerminalInfo) {
        this._firstTerminal[ctx][rp].add(fti)
    }

    fun cacheParentInContext(ctx: RulePosition, cr: RuntimeRule, pn: Set<ParentNext>) {
        this._parentInContext[ctx][cr].addAll(pn)
    }
}