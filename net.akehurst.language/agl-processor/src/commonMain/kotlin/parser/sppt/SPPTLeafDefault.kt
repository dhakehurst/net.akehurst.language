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

package net.akehurst.language.parser.sppt

import net.akehurst.language.api.sppt.*
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.api.parser.InputLocation

class SPPTLeafDefault(
        terminalRule: RuntimeRule,
        location: InputLocation,
        override val isEmptyLeaf: Boolean,
        override val matchedText: String,
        priority: Int
) : SPPTNodeAbstract(
        terminalRule,
        location,
        location.position+matchedText.length,
        priority
), SPPTLeaf
{

    // --- SPPTLeaf ---

    override val isPattern: Boolean = terminalRule.isPattern
    override val isLiteral: Boolean = !terminalRule.isPattern

    override val tagList = mutableListOf<String>()

    override var eolPositions: List<Int> = emptyList()

    override val metaTags: List<String> by lazy { //TODO: make this configurable on the LanguageProcessor
        val map = mutableMapOf<String, String>(
                "\$keyword" to "'[a-zA-Z][a-zA-Z0-9]*'"
        )
        map.mapNotNull {
            when {
                this.name.matches(Regex(it.value)) -> it.key
                else -> null
            }
        }
    }
    // --- SPPTNode ---

    override val nonSkipMatchedText: String = if (isSkip) "" else this.matchedText

    override fun contains(other: SPPTNode): Boolean {
        return this.identity == other.identity
    }

    override val isLeaf: Boolean = true
    override val isBranch: Boolean = false

    override val asLeaf: SPPTLeaf = this

    override val asBranch: SPPTBranch get() { throw SPPTException("Not a Branch", null) }

    override val lastLocation get() = this.location

    override fun <T, A> accept(visitor: SharedPackedParseTreeVisitor<T, A>, arg: A): T {
        return visitor.visit(this,  arg)
    }

    override fun toString(): String {
        val name = when {
            this.runtimeRule == RuntimeRuleSet.END_OF_TEXT -> "EOT"
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