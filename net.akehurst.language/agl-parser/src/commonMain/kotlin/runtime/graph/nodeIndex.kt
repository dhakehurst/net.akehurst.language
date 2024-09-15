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

package net.akehurst.language.agl.runtime.graph

import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsNonTerminal
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsTerminal
import net.akehurst.language.agl.util.Debug
import net.akehurst.language.automaton.leftcorner.ParserState
import net.akehurst.language.sppt.api.SpptDataNode


/*
 A node that is still growing is identified by
  - the state (and state set)
  - position in the input where it starts
  - position in the input where the node stops (i.e. its length)
    length/nextInputPosition is necessary because ?
  - size of a list ( only relevant for MULTI and SEPARATED_LIST)
 */
internal class GrowingNodeIndex(
    val runtimeState: RuntimeState,
    val startPosition: Int,
    val nextInputPositionBeforeSkip: Int,
    val nextInputPositionAfterSkip: Int,
    val numNonSkipChildren: Int, //for use with MULTI and SEPARATED_LIST
    val childrenPriorities: List<List<Int>>?
) {

    companion object {
        // used for start and leaf
        //fun index(state: ParserState, lhs: LookaheadSet, startPosition: Int, nextInputPosition: Int, listSize: Int): GrowingNodeIndex {
        //    return GrowingNodeIndex(state.runtimeRules,state.positions, lhs.number, startPosition, nextInputPosition, listSize)
        //}

        // used to augment the GrowingNodeIndex (GSS node identity) for MULTI and SEPARATED_LIST
        // needed because the 'RuleOptionPosition' does not capture the 'position' in the list
        fun listSize(runtimeRule: RuntimeRule, numNonSkipChildren: Int): Int = when (runtimeRule.rhs) {
            is RuntimeRuleRhsNonTerminal -> numNonSkipChildren
            is RuntimeRuleRhsTerminal -> 0
        }
    }

    val complete by lazy {
        CompleteNodeIndex(runtimeState.state, startPosition, nextInputPositionBeforeSkip, nextInputPositionAfterSkip)
    }

    private val _hashCode = arrayOf(runtimeState, startPosition, nextInputPositionAfterSkip, numNonSkipChildren).contentHashCode()

    val isComplete: Boolean get() = runtimeState.isAtEnd
    val isGoal: Boolean get() = runtimeState.state.isGoal

    val state: ParserState get() = this.runtimeState.state

    val isLeaf: Boolean get() = this.runtimeState.state.isLeaf
    val isEmptyMatch: Boolean get() = this.startPosition == this.nextInputPositionBeforeSkip

    override fun hashCode(): Int = _hashCode

    override fun equals(other: Any?): Boolean = when (other) {
        !is GrowingNodeIndex -> false
        else -> when {
            this.runtimeState != other.runtimeState -> false
            this.startPosition != other.startPosition -> false
            this.nextInputPositionAfterSkip != other.nextInputPositionAfterSkip -> false
            this.numNonSkipChildren != other.numNonSkipChildren -> false
            else -> true
        }
    }

    override fun toString(): String {
        val ct = runtimeState.runtimeLookaheadSet.map { it.fullContent.joinToString { it.tag } }
        val ctStr = ct.joinToString(
            prefix = "[",
            postfix = "]",
            separator = "|"
        ) { it }
        return "GNI{state=${runtimeState.state},lhs=$ctStr,sp=${startPosition},np=$nextInputPositionAfterSkip,nc=$numNonSkipChildren}"
    }

}

// Because of embedded grammars and skipnodes (as embedded)
// this must also contain the id of the TreeData...or rather an id of the grammar/ParserStateSet that it belongs to
// also the treeData object is needed when getting children of a node in the conversion to SPPT
/**
 * Identity of a node that has been completed - i.e. all children parsed/found
 * Identity based on:
 *  - runtimeRules - rather than RuleOptionPosition, as same runtimeRule can have alternatives
 *  - startPosition
 *  - nextInputPosition
 */
internal class CompleteNodeIndex(
    val state: ParserState,
    override val startPosition: Int,
    val nextInputPositionBeforeSkip: Int,
    val nextInputPositionAfterSkip: Int
) : SpptDataNode {

    init {
        if (Debug.CHECK) check(state.rulePositions.all { it.isAtEnd })
    }

    override val rule: RuntimeRule get() = this.state.firstRule
    val runtimeRulesAsSet: Set<RuntimeRule> get() = this.state.runtimeRulesAsSet
    val rulePositions get() = this.state.rulePositions

    private val _hashCode_cache = arrayOf(runtimeRulesAsSet, startPosition, nextInputPositionAfterSkip).contentHashCode()

    val highestPriorityRule get() = this.state.rulePositions.maxBy { it.option }.rule as RuntimeRule
    val firstRule: RuntimeRule by lazy { this.state.rulePositions[0].rule as RuntimeRule }
    val isLeaf: Boolean get() = firstRule.isTerminal //should only be one if true
    val isEmbedded: Boolean get() = firstRule.isEmbedded //should only be one if true
    val isEmptyMatch: Boolean get() = this.startPosition == this.nextInputPositionBeforeSkip
    val hasSkipData: Boolean get() = this.nextInputPositionBeforeSkip != nextInputPositionAfterSkip

    override val nextInputPosition: Int get() = nextInputPositionAfterSkip
    override val nextInputNoSkip get() = this.nextInputPositionBeforeSkip

    override val option: Int get() = this.state.priorityList[0]
    val priorityList: List<Int> get() = this.state.priorityList

    override fun hashCode(): Int = this._hashCode_cache
    override fun equals(other: Any?): Boolean = when {
        other !is SpptDataNode -> false
        this.startPosition != other.startPosition -> false
        this.nextInputPosition != other.nextInputPosition -> false
        this.rule != other.rule -> false
        else -> true
    }

    override fun toString(): String {
        return "CNI{$startPosition-$nextInputPositionBeforeSkip,${
            runtimeRulesAsSet.joinToString(
                prefix = "(",
                postfix = ")",
                separator = ","
            ) { it.tag }
        }|${option}}"
    }
    /*
        //useful during debug
        fun toStringTree(input: InputFromString): String {
            val runtimeRules = state.runtimeRulesAsSet
            val nodes = when {
                state.isLeaf -> runtimeRules.map { rr ->
                    SPPTLeafFromInput(input, rr, this.startPosition, this.nextInputPosition, 0)
                }

                else -> runtimeRules.map { rr ->
                    SPPTBranchFromTreeData(treeData, input, rr, 0, this.startPosition, this.nextInputPosition, 0)
                }
            }
            val v = ToStringVisitor("\n", "  ")
            return nodes.joinToString(separator = "\n") { n -> v.visitNode(n, "  ").joinToString(separator = "\n") }
        }
    */
}
