/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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
import net.akehurst.language.agl.runtime.structure.RuleOptionId
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsItemsKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleListKind
import net.akehurst.language.api.sppt.SPPTNode

internal class GrowingChildNode(
        val state: ParserState?, //if null then its skip children
        val children: List<SPPTNode>
) {

    companion object {
        private fun ParserState.isDuplicateOf(other: ParserState?): Boolean = when {
            null == other -> false
            this == other -> true
            else -> {
                this.rulePositions.all { sRp -> other.rulePositions.any { lRp -> sRp.identity == lRp.identity } }
            }
        }
    }

    var nextChild: GrowingChildNode? = null
    var nextChildAlternatives: MutableMap<List<RuleOptionId>, MutableList<GrowingChildNode>>? = null

    val isLast: Boolean get() = null == nextChild && null == nextChildAlternatives

    val nextInputPosition: Int
        get() = when {
            null == state -> children.last().nextInputPosition
            1==children.size -> children.first().nextInputPosition
            else -> {
                //check(1==children.map { it.nextInputPosition }.toSet().size)
                children.first().nextInputPosition
            }
        }

    private fun appendRealLast(node:GrowingChildNode): GrowingChildNode {
        this.nextChild = node
        return node
    }

    private fun appendAlternativeLast(growingChildren: GrowingChildren, childIndex: Int, node:GrowingChildNode): GrowingChildNode? {
        val existingNextChild = this.nextChild
        return when {
            null != existingNextChild -> { // first alternative
                when {
                    node.state!!.isDuplicateOf(existingNextChild.state) -> {
                        val alternativeNextChild = node
                        when {
                            alternativeNextChild.nextInputPosition != existingNextChild.nextInputPosition -> {
                                val nextChildAlternatives = mutableMapOf<List<RuleOptionId>, MutableList<GrowingChildNode>>()
                                val alts = mutableListOf(existingNextChild)
                                nextChildAlternatives[state!!.rulePositionIdentity] = alts
                                this.nextChild = null
                                alts.add(alternativeNextChild)
                                growingChildren.incNextChildAlt(childIndex, state!!.rulePositionIdentity)
                                this.nextChildAlternatives = nextChildAlternatives
                                alternativeNextChild
                            }
                            else -> {
                                //TODO: do we replace or drop?"
                                existingNextChild
                                //null
                            }
                        }
                    }
                    else -> {
                        val nextChildAlternatives = mutableMapOf<List<RuleOptionId>, MutableList<GrowingChildNode>>()
                        nextChildAlternatives[existingNextChild.state!!.rulePositionIdentity] = mutableListOf(existingNextChild)
                        this.nextChild = null
                        val alternativeNextChild = node
                        nextChildAlternatives[node.state!!.rulePositionIdentity] = mutableListOf(alternativeNextChild)
                        this.nextChildAlternatives = nextChildAlternatives
                        alternativeNextChild
                    }
                }
            }
            else -> { // other alternatives
                // if null==nextChild then must be that null!=nextChildAlternatives
                val nextChildAlternatives = this.nextChildAlternatives!!
                val alts = nextChildAlternatives[node.state!!.rulePositionIdentity]
                when {
                    null == alts -> { // first alt with this rulePositionIdentity
                        val alternativeNextChild = node
                        nextChildAlternatives[node.state!!.rulePositionIdentity] = mutableListOf(alternativeNextChild)
                        alternativeNextChild
                    }
                    else -> {
                        val alternativeNextChild = node
                        // check if there is a duplicate with greater length
                        val existing = alts.firstOrNull { it.nextInputPosition == alternativeNextChild.nextInputPosition }
                        when {
                            null == existing -> {
                                alts.add(alternativeNextChild)
                                growingChildren.setNextChildAlt(childIndex, node.state!!.rulePositionIdentity,alts.size-1)
                                alternativeNextChild
                            }
                            else -> {
                                //error("TODO: do we replace or drop?")
                                growingChildren.setNextChildAlt(childIndex, node.state!!.rulePositionIdentity,alts.indexOf(existing))
                                existing
                                //null
                            }
                        }
                    }
                }
            }
        }
    }

    fun appendLast(growingChildren: GrowingChildren, childIndex: Int, nextChild:GrowingChildNode): GrowingChildNode? = when {
        this.isLast -> appendRealLast(nextChild)
        else -> appendAlternativeLast(growingChildren, childIndex, nextChild)
    }

    fun next(altNext: Int, ruleOption: RuleOptionId): GrowingChildNode? = when {
        null != nextChildAlternatives -> nextChildAlternatives!!.entries.firstOrNull {
            val rpIds = it.key
            rpIds.any { it == ruleOption }
        }?.value?.get(altNext)
        null == nextChild -> null
        else -> nextChild
    }

    operator fun get(ruleOption: RuleOptionId): List<SPPTNode> {
        return when {
            null == this.state -> children //skip nodes
            else -> when {
                1 == children.size -> children
                else -> {
                    val rr = ruleOption.runtimeRule
                    val i = when (rr.kind) {
                        RuntimeRuleKind.TERMINAL -> 0
                        RuntimeRuleKind.EMBEDDED -> 0
                        RuntimeRuleKind.GOAL,
                        RuntimeRuleKind.NON_TERMINAL -> when (rr.rhs.itemsKind) {
                            RuntimeRuleRhsItemsKind.EMPTY -> TODO()
                            RuntimeRuleRhsItemsKind.CONCATENATION -> state.rulePositions.indexOfFirst { it.runtimeRule == ruleOption.runtimeRule }
                            RuntimeRuleRhsItemsKind.CHOICE -> state.rulePositions.indexOfFirst { it.runtimeRule == ruleOption.runtimeRule && it.option == ruleOption.option }
                            RuntimeRuleRhsItemsKind.LIST -> when(rr.rhs.listKind) {
                                RuntimeRuleListKind.MULTI -> state.rulePositions.indexOfFirst { it.runtimeRule == ruleOption.runtimeRule && it.option == ruleOption.option }
                                RuntimeRuleListKind.SEPARATED_LIST -> state.rulePositions.indexOfFirst { it.runtimeRule == ruleOption.runtimeRule && it.option == ruleOption.option }
                                else -> TODO()
                            }
                        }
                    }
                    if (-1==i) emptyList() else listOf(children[i])
                }
            }
        }
    }

    override fun toString(): String = when {
        null == this.state -> children.joinToString(separator = " ") {
            "${it.startPosition},${it.nextInputPosition},${it.name}[${it.option}]"
        }
        else -> children.joinToString(separator = "|") {
            "${it.startPosition},${it.nextInputPosition},${it.name}[${it.option}]"
        }
    }

}