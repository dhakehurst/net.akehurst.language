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

package net.akehurst.language.agl.sppt

import net.akehurst.language.agl.parser.InputFromString
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.sppt.*

class SPPTLeafFromInput(
        input: InputFromString,
        runtimeRule: RuntimeRule,
        startPosition: Int,
        nextInputPosition: Int,
        priority: Int
) : SPPTNodeFromInputAbstract(input, runtimeRule, 0, startPosition, nextInputPosition, priority), SPPTLeaf {

    // --- SPPTLeaf ---
    override val isPattern: Boolean get() = runtimeRule.isPattern
    override val isLiteral: Boolean get() = runtimeRule.isPattern.not()
    override val isLeaf: Boolean get() = true
    override val isBranch: Boolean get() = false
    override val asBranch: SPPTBranch get() = throw SPPTException("Not a Branch", null)
    override val location: InputLocation get() = input.locationFor(startPosition,nextInputPosition)
    override val lastLeaf: SPPTLeaf get() = this
    override val asLeaf: SPPTLeaf get() = this
    override lateinit var tagList: List<String>// = mutableListOf<String>()
    override lateinit var eolPositions: List<Int> // = emptyList()

    override val matchedText: String get() = input[startPosition, nextInputPosition]
    override val isEmptyLeaf: Boolean get() = this.runtimeRule.isEmptyRule
    override val metaTags: List<String> by lazy { //TODO: make this configurable on the LanguageProcessor
        val map = mutableMapOf<String, String>(
                "\$keyword" to "'[a-zA-Z_][a-zA-Z0-9_-]*'"
        )
        map.mapNotNull {
            when {
                this.name.matches(Regex(it.value)) -> it.key
                else -> null
            }
        }
    }

    // --- SPPTNode ---
    override val nonSkipMatchedText: String get() = if (isSkip) "" else this.matchedText

    override fun contains(other: SPPTNode): Boolean {
        return this.identity == other.identity
    }

    override fun toString(): String {
        val name = when {
            this.runtimeRule == RuntimeRuleSet.END_OF_TEXT -> RuntimeRuleSet.END_OF_TEXT_TAG
            this.runtimeRule.isEmptyRule -> "§empty"
            this.isLiteral -> "'${this.runtimeRule.value}'"
            this.isPattern -> "\"${this.runtimeRule.value}\""
            else -> this.name //shouldn't happen!
        }

        return "${this.startPosition},${this.nextInputPosition},C:${name}(${this.runtimeRule.number})"
    }

    override fun hashCode(): Int {
        return this.identity.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return if (other is SPPTLeaf) {
            this.identity == other.identity
        } else {
            false
        }
    }

}