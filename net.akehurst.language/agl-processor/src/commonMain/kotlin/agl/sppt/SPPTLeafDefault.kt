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
        0, //no option for terminal rules
        location,
        location.position+matchedText.length,
        priority
), SPPTLeaf
{
    companion object {
        val NONE = object : SPPTLeaf {
            override val isPattern: Boolean
                get() = TODO("not implemented")
            override val isLiteral: Boolean
                get() = TODO("not implemented")
            override val tagList: List<String>
                get() = TODO("not implemented")
            override val eolPositions: List<Int>
                get() = TODO("not implemented")
            override val metaTags: List<String>
                get() = TODO("not implemented")
            override val identity: SPPTNodeIdentity
                get() = TODO("not implemented")
            override val name: String
                get() = TODO("not implemented")
            override val runtimeRuleNumber: Int
                get() = TODO("not implemented")
            override val option: Int
                get() = TODO("not implemented")
            override val location: InputLocation
                get() = TODO("not implemented")
            override val lastLeaf: SPPTLeaf
                get() = TODO("not implemented")
            override val startPosition: Int
                get() = TODO("not implemented")
            override val matchedTextLength: Int
                get() = TODO("not implemented")
            override val nextInputPosition: Int
                get() = TODO("not implemented")
            override val priority: Int
                get() = TODO("not implemented")
            override val matchedText: String
                get() = TODO("not implemented")
            override val nonSkipMatchedText: String
                get() = TODO("not implemented")
            override val numberOfLines: Int
                get() = TODO("not implemented")
            override val isEmptyLeaf: Boolean
                get() = TODO("not implemented")
            override val isEmptyMatch: Boolean
                get() = TODO("not implemented")
            override val isLeaf: Boolean
                get() = true
            override val isBranch: Boolean
                get() = false
            override val isSkip: Boolean
                get() = TODO("not implemented")
            override val asLeaf: SPPTLeaf
                get() = TODO("not implemented")
            override val asBranch: SPPTBranch
                get() = TODO("not implemented")
            override var parent: SPPTBranch?
                get() = TODO("not implemented")
                set(value) {TODO("not implemented")}

            override fun contains(other: SPPTNode): Boolean {
                TODO("not implemented")
            }

        }
    }


    // --- SPPTLeaf ---

    override val isPattern: Boolean = terminalRule.isPattern
    override val isLiteral: Boolean = !terminalRule.isPattern

    override val tagList = mutableListOf<String>()

    override lateinit var eolPositions: List<Int> // = emptyList()

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

    override val isLeaf: Boolean = true
    override val isBranch: Boolean = false

    override val asLeaf: SPPTLeaf = this

    override val asBranch: SPPTBranch get() { throw SPPTException("Not a Branch", null) }

    override val lastLeaf get() = this

    override fun toString(): String {
        val name = when {
            this.runtimeRule == RuntimeRuleSet.END_OF_TEXT -> RuntimeRuleSet.END_OF_TEXT_TAG
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