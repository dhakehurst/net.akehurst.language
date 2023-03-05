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

import net.akehurst.language.agl.automaton.ParserState
import net.akehurst.language.agl.parser.InputFromString
import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.agl.sppt.SPPTBranchFromTreeData
import net.akehurst.language.agl.sppt.SPPTLeafFromInput
import net.akehurst.language.agl.sppt.ToStringVisitor


/*
 A node that is still growing is identified by
  - the state (and state set)
  - position in the input where it starts
  - position in the input where the node stops (i.e. its length)
    length/nextInputPosition is necessary because ?
  - size of a list ( only relevant for MULTI and SEPARATED_LIST)
 */
internal class GrowingNodeIndex(
    treeData: TreeDataComplete,
    val runtimeState: RuntimeState,
     startPosition: Int,
     nextInputPosition: Int,
     nextInputPositionAfterSkip: Int,
    val numNonSkipChildren: Int //for use with MULTI and SEPARATED_LIST
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

    val complete = treeData.createCompleteNodeIndex(runtimeState.state, startPosition, nextInputPosition, nextInputPositionAfterSkip, this)

    private val _hashCode = arrayOf(runtimeState, startPosition, nextInputPosition, numNonSkipChildren).contentHashCode()

    val startPosition: Int get() = complete.startPosition
    val nextInputPosition: Int get() = complete.nextInputPosition
    val nextInputPositionAfterSkip: Int get() = complete.nextInputPositionAfterSkip

    val isComplete: Boolean get() = runtimeState.isAtEnd
    val isGoal:Boolean get() = runtimeState.state.isGoal

    //TODO: don't store data twice..also prefer not to create 2 objects!
    //val complete = CompleteNodeIndex(treeData.forStateSetNumber, runtimeState.state, startPosition, nextInputPosition, nextInputPositionAfterSkip, this)

    val state: ParserState get() = this.runtimeState.state

    val isLeaf: Boolean get() = this.runtimeState.state.isLeaf
    val isEmptyMatch: Boolean get() = this.runtimeState.state.isAtEnd && this.startPosition == this.nextInputPosition

    override fun hashCode(): Int = _hashCode

    override fun equals(other: Any?): Boolean = when (other) {
        !is GrowingNodeIndex -> false
        else -> when {
            this.runtimeState != other.runtimeState -> false
            this.startPosition != other.startPosition -> false
            this.nextInputPosition != other.nextInputPosition -> false
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
        return "GNI{state=${runtimeState.state},lhs=$ctStr,sp=${startPosition},np=$nextInputPosition,len=$numNonSkipChildren}"
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
    val treeData: TreeDataComplete,
    // only RuntimeRules are needed for comparisons, but priority needed in order to resolve priorities, but it should not be part of identity
    val state: ParserState,
    val startPosition: Int,
    val nextInputPosition: Int,
    val nextInputPositionAfterSkip: Int,
    val gni: GrowingNodeIndex? // the GNI used to create this, used when dropping
) {

    val runtimeRulesSet: Set<RuntimeRule> get() = this.state.runtimeRulesSet
    val rulePositions get() = this.state.rulePositions

    private val _hashCode_cache = arrayOf(treeData, runtimeRulesSet, startPosition, nextInputPosition).contentHashCode()

    //TODO: don't store data twice..also prefer not to create 2 objects!
    val preferred by lazy { PreferredChildIndex(runtimeRulesSet, startPosition) }

    val highestPriorityRule get() = this.state.rulePositions.maxBy { it.option }.rule as RuntimeRule
    val firstRule: RuntimeRule by lazy { this.state.rulePositions[0].rule as RuntimeRule }
    val isLeaf: Boolean get() = firstRule.isTerminal //should only be one if true
    val isEmbedded: Boolean get() = firstRule.isEmbedded //should only be one if true
    val hasSkipData: Boolean get() = this.nextInputPosition != nextInputPositionAfterSkip

    val optionList: List<Int> get() = this.state.priorityList
    val priorityList: List<Int> get() = this.state.priorityList

    override fun hashCode(): Int = this._hashCode_cache
    override fun equals(other: Any?): Boolean = when {
        other !is CompleteNodeIndex -> false
        other.treeData != this.treeData -> false
        other.startPosition != this.startPosition -> false
        other.nextInputPosition != this.nextInputPosition -> false
        //other.rulePositions != this.rulePositions -> false
        other.runtimeRulesSet != this.runtimeRulesSet -> false
        else -> true
    }

    override fun toString(): String {
        return "CNI{(${this.treeData.forStateSetNumber}),$startPosition-$nextInputPosition,R=${runtimeRulesSet.joinToString(prefix = "[", postfix = "]", separator = ",") { it.tag }}}"
    }

    //useful during debug
    fun toStringTree(input: InputFromString): String {
        val runtimeRules = state.runtimeRulesSet
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
}

internal data class PreferredChildIndex(
    val runtimeRulesSet: Set<RuntimeRule>,
    val startPosition: Int,
) {
    override fun toString(): String = "PI{R=${runtimeRulesSet.joinToString { it.tag }},sp=$startPosition"
}