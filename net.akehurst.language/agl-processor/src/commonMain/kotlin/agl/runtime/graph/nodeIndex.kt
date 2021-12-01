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
import net.akehurst.language.agl.runtime.structure.*


/*
 A node that is still growing is identified by
  - the state (and state set)
  - position in the input where it starts
  - position in the input where the node stops (i.e. its length)
    length/nextInputPosition is necessary because ?
  - size of a list ( only relevant for MULTI and SEPARATED_LIST)
 */
internal data class GrowingNodeIndex(
    val treeData: TreeData,
    val state: ParserState,
    val runtimeLookaheadSet: LookaheadSet,
    val startPosition: Int,
    val nextInputPosition: Int,
    val listSize: Int //for use with MULTI and SEPARATED_LIST
//        val priority: Int
) {

    //TODO: don't store data twice..but also prefer not to create 2 objects!
    val complete = CompleteNodeIndex(treeData, state.runtimeRulesSet, startPosition, nextInputPosition, this.state.optionList, this)

    companion object {
        // used for start and leaf
        //fun index(state: ParserState, lhs: LookaheadSet, startPosition: Int, nextInputPosition: Int, listSize: Int): GrowingNodeIndex {
        //    return GrowingNodeIndex(state.runtimeRules,state.positions, lhs.number, startPosition, nextInputPosition, listSize)
        //}

        // used to augment the GrowingNodeIndex (GSS node identity) for MULTI and SEPARATED_LIST
        // needed because the 'RulePosition' does not capture the 'position' in the list
        fun listSize(runtimeRule: RuntimeRule, childrenSize: Int): Int = when (runtimeRule.kind) {
            RuntimeRuleKind.NON_TERMINAL -> when (runtimeRule.rhs.itemsKind) {
                RuntimeRuleRhsItemsKind.EMPTY -> -1
                RuntimeRuleRhsItemsKind.CONCATENATION -> childrenSize
                RuntimeRuleRhsItemsKind.CHOICE -> -1
                RuntimeRuleRhsItemsKind.LIST -> when (runtimeRule.rhs.listKind) {
                    RuntimeRuleListKind.MULTI -> childrenSize
                    RuntimeRuleListKind.SEPARATED_LIST -> childrenSize
                    else -> TODO()
                }
            }
            else -> -1
        }
    }

    override fun toString(): String {
        return "GNI{state=$state,lhs=${runtimeLookaheadSet.content.joinToString(prefix = "[", postfix = "]", separator = ",") { it.tag }},startPos=${startPosition}, nextPos=$nextInputPosition, listSize=$listSize}"
    }

}

// Because of embedded grammars and skipnodes (as embedded)
// this must also contain the id of the TreeData...or rather an id of the grammar/ParserStateSet that it belongs to
// also the treeData object is needed when getting children of a node in the conversion to SPPT
internal class CompleteNodeIndex(
    val treeData: TreeData,
    val runtimeRules: Set<RuntimeRule>, //TODO: maybe encode as an (StateSet,Int)!
    val startPosition: Int,
    val nextInputPosition: Int,
    val optionList: List<Int>, // need this info in order to resolve priorities, but it should not be part of identity
    val gni:GrowingNodeIndex? // the GNI used to create this, TODO: remove it...just for debug
) {
    private val hashCode_cache = arrayOf(treeData, runtimeRules, startPosition, nextInputPosition).contentHashCode()

    val firstRule: RuntimeRule get() = runtimeRules.first()
    val isLeaf: Boolean get() = firstRule.kind == RuntimeRuleKind.TERMINAL //should only be one if true

    val priorityList get() = optionList //TODO: if priority is different to option this must change

    override fun hashCode(): Int = this.hashCode_cache
    override fun equals(other: Any?): Boolean = when {
        other !is CompleteNodeIndex -> false
        other.treeData != this.treeData -> false
        other.startPosition != this.startPosition -> false
        other.nextInputPosition != this.nextInputPosition -> false
        other.runtimeRules != this.runtimeRules -> false
        else -> true
    }
    override fun toString(): String = "CNI{(SS=${this.treeData.forStateSetNumber}) sp=$startPosition,np=$nextInputPosition,R=${runtimeRules.joinToString { it.tag }}}"
}

