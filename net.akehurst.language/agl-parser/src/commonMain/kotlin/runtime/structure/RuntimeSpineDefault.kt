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

package net.akehurst.language.agl.runtime.structure

import net.akehurst.language.agl.runtime.graph.GrowingNodeIndex
import net.akehurst.language.parser.api.RuntimeSpine

internal class RuntimeSpineDefault(
    val head: GrowingNodeIndex,
    val gssSnapshot: Map<GrowingNodeIndex, Set<GrowingNodeIndex>>,
    override val expectedNextTerminals: Set<RuntimeRule>,
    override val nextChildNumber: Int
) : RuntimeSpine {
    override val usedPosition: Int get() = head.nextInputPositionAfterSkip

    override val elements: List<RulePositionRuntime> by lazy {
        val list = mutableListOf(head.state.rulePosition.first())
        var next = gssSnapshot[head]
        while (null != next && next.isNotEmpty()) {
            val nh = next.first() //TODO: could be many
            list.add(nh.state.rulePosition.first())
            next = gssSnapshot[nh]
        }
        list
    }

    override fun toString(): String = "RTSpine ${elements.joinToString(separator = "->") { it.toString() }}"
}

/**
 * a spine with a given [usedPosition] and filtered [expectedNextTerminals], used by expectedAt
 */
internal class RuntimeSpineAt(
    val spine: RuntimeSpine,
    override val usedPosition: Int,
    override val expectedNextTerminals: Set<RuntimeRule>
) : RuntimeSpine by spine {
    override fun hashCode(): Int = arrayOf(spine, usedPosition, expectedNextTerminals).contentHashCode()
    override fun equals(other: Any?): Boolean = when (other) {
        !is RuntimeSpineAt -> false
        else -> this.spine == other.spine && this.usedPosition == other.usedPosition && this.expectedNextTerminals == other.expectedNextTerminals
    }

    override fun toString(): String = "RTSpine@$usedPosition ${elements.joinToString(separator = "->") { it.toString() }}"
}

/**
 * spine for a complete sentence, i.e. the goal rule is complete and only end-of-text is expected next
 */
internal class RuntimeSpineEndOfText(
    finishRulePosition: RulePositionRuntime,
    override val usedPosition: Int
) : RuntimeSpine {
    override val expectedNextTerminals: Set<RuntimeRule> = setOf(RuntimeRuleSet.END_OF_TEXT)
    override val elements: List<RulePositionRuntime> = listOf(finishRulePosition)
    override val nextChildNumber: Int = 1

    override fun hashCode(): Int = arrayOf(elements, usedPosition).contentHashCode()
    override fun equals(other: Any?): Boolean = when (other) {
        !is RuntimeSpineEndOfText -> false
        else -> this.elements == other.elements && this.usedPosition == other.usedPosition
    }

    override fun toString(): String = "RTSpine@$usedPosition <EOT> ${elements.joinToString(separator = "->") { it.toString() }}"
}
