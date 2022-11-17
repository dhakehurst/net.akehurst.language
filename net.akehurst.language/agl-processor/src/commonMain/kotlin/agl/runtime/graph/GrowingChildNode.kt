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

import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.agl.runtime.structure.RuleOptionId
import net.akehurst.language.agl.runtime.structure.RuntimeRuleKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleListKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsItemsKind
import net.akehurst.language.api.sppt.SPPTNode

internal class GrowingChildNode(
        val ruleOptionList: Set<RuleOptionId>,
        val children: List<SPPTNode>,
        val isSkip:Boolean
) {

    companion object {
        private fun Set<RuleOptionId>.isDuplicateOf(other: Set<RuleOptionId>?): Boolean = when {
            null == other -> false
            this == other -> true
            else -> {
                this.all { sRp -> other.any { lRp -> sRp == lRp } }
            }
        }
    }

    var nextChild: GrowingChildNode? = null
    var nextChildAlternatives: MutableMap<Set<RuleOptionId>?, MutableList<GrowingChildNode>>? = null

    val isLast: Boolean get() = null == nextChild && null == nextChildAlternatives

    val nextInputPosition: Int
        get() = when {
//            null == state -> children.last().nextInputPosition
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

    private fun appendAlternativeLast(growingChildren: GrowingChildren, childIndex: Int, node:GrowingChildNode): GrowingChildNode {
        val existingNextChild = this.nextChild
        return when {
            null != existingNextChild -> { // first alternative
                when {
                    node.ruleOptionList.isDuplicateOf(existingNextChild.ruleOptionList) -> {
                        val alternativeNextChild = node
                        when {
                            alternativeNextChild.nextInputPosition != existingNextChild.nextInputPosition -> {
                                val nextChildAlternatives = mutableMapOf<Set<RuleOptionId>?, MutableList<GrowingChildNode>>()
                                val alts = mutableListOf(existingNextChild)
                                nextChildAlternatives[ruleOptionList] = alts
                                this.nextChild = null
                                alts.add(alternativeNextChild)
                                growingChildren.incNextChildAlt(childIndex, ruleOptionList)
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
                        val nextChildAlternatives = mutableMapOf<Set<RuleOptionId>?, MutableList<GrowingChildNode>>()
                        nextChildAlternatives[existingNextChild.ruleOptionList] = mutableListOf(existingNextChild)
                        this.nextChild = null
                        val alternativeNextChild = node
                        nextChildAlternatives[node.ruleOptionList] = mutableListOf(alternativeNextChild)
                        this.nextChildAlternatives = nextChildAlternatives
                        alternativeNextChild
                    }
                }
            }
            else -> { // other alternatives
                // if null==nextChild then must be that null!=nextChildAlternatives
                val nextChildAlternatives = this.nextChildAlternatives!!
                val alts = nextChildAlternatives[node.ruleOptionList]
                when {
                    null == alts -> { // first alt with this rulePositionIdentity
                        val alternativeNextChild = node
                        nextChildAlternatives[node.ruleOptionList] = mutableListOf(alternativeNextChild)
                        alternativeNextChild
                    }
                    else -> {
                        val alternativeNextChild = node
                        // check if there is a duplicate with greater length
                        val existing = alts.firstOrNull { it.nextInputPosition == alternativeNextChild.nextInputPosition }
                        when {
                            null == existing -> {
                                alts.add(alternativeNextChild)
                                growingChildren.setNextChildAlt(childIndex, node.ruleOptionList,alts.size-1)
                                alternativeNextChild
                            }
                            else -> {
                                //error("TODO: do we replace or drop?")
                                growingChildren.setNextChildAlt(childIndex, node.ruleOptionList,alts.indexOf(existing))
                                existing
                                //null
                            }
                        }
                    }
                }
            }
        }
    }

    fun appendLast(growingChildren: GrowingChildren, childIndex: Int, nextChild:GrowingChildNode): GrowingChildNode = when {
        this.isLast -> appendRealLast(nextChild)
        else -> appendAlternativeLast(growingChildren, childIndex, nextChild)
    }

    // only used by GrowingChildren
    internal fun next(altNext: Int, ruleOption: RuleOptionId): GrowingChildNode? = when {
        null != nextChildAlternatives -> nextChildAlternatives!!.entries.firstOrNull {
            val rpIds = it.key
            rpIds?.any { it == ruleOption } ?: false
        }?.value?.get(altNext)
        null == nextChild -> null
        else -> nextChild
    }
    internal fun next(altNext: Int, ruleOptionList: Set<RuleOptionId>): GrowingChildNode? = when {
        null != nextChildAlternatives -> nextChildAlternatives?.get(ruleOptionList)?.get(altNext)
        null == nextChild -> null
        else -> nextChild
    }

    operator fun get(ruleOption: RuleOptionId?): List<SPPTNode> {
        return when {
 //           null == this.state -> children //skip nodes
            else -> when {
                null==ruleOption-> children
                1 == children.size -> children
                else -> {
                    val rr = ruleOption.runtimeRule
                    val rhs = rr.rhs
                    val i = when (rhs) {
                        is RuntimeRuleRhsTerminal -> 0
                        is RuntimeRuleRhsNonTerminal -> when (rhs) {
                            is RuntimeRuleRhsGoal -> ruleOptionList.indexOfFirst { it.runtimeRule == ruleOption.runtimeRule }
                            is RuntimeRuleRhsConcatenation -> ruleOptionList.indexOfFirst { it.runtimeRule == ruleOption.runtimeRule }
                            is RuntimeRuleRhsChoice -> ruleOptionList.indexOfFirst { it.runtimeRule == ruleOption.runtimeRule && it.option == ruleOption.option }
                            is RuntimeRuleRhsList -> ruleOptionList.indexOfFirst { it.runtimeRule == ruleOption.runtimeRule && it.option == ruleOption.option }
                        }
                    }
                    if (-1==i) emptyList() else listOf(children[i])
                }
            }
        }
    }

    override fun toString(): String = when {
//        null == this.state -> children.joinToString(separator = " ") {
 //           "${it.startPosition},${it.nextInputPosition},${it.name}[${it.option}]"
 //       }
        else -> children.joinToString(separator = "|") {
            "${it.startPosition},${it.nextInputPosition},${it.name}[${it.option}]"
        }
    }

}