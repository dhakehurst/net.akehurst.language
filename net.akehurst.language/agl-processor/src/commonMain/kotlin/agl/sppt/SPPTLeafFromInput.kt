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
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsLiteral
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsPattern
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.sppt.*

//TODO: currently this has to be public, because otherwise kotlin does not
// use the non-mangled names for properties
/*internal */ class SPPTLeafFromInput internal constructor(
    input: InputFromString,
    runtimeRule: RuntimeRule,
    startPosition: Int,
    nextInputPosition: Int,
    priority: Int
) : SPPTNodeFromInputAbstract(input, runtimeRule, 0, startPosition, nextInputPosition, priority), SPPTLeaf {

    companion object {
        val NONE = object : SPPTLeaf {
            override val isPattern: Boolean get() = TODO("not implemented")
            override val isLiteral: Boolean get() = TODO("not implemented")
            override val tagList: List<String> get() = TODO("not implemented")
            override val eolPositions: List<Int> get() = TODO("not implemented")
            override val metaTags: List<String> get() = TODO("not implemented")
            override val identity: SPPTNodeIdentity get() = TODO("not implemented")
            override val name: String get() = TODO("not implemented")
            override val runtimeRuleSetNumber: Int get() = TODO("not implemented")
            override val runtimeRuleNumber: Int get() = TODO("not implemented")
            override val option: Int get() = TODO("not implemented")
            override val location: InputLocation get() = TODO("not implemented")
            override val lastLeaf: SPPTLeaf get() = TODO("not implemented")
            override val startPosition: Int get() = TODO("not implemented")
            override val matchedTextLength: Int get() = TODO("not implemented")
            override val nextInputPosition: Int get() = TODO("not implemented")
            override val priority: Int get() = TODO("not implemented")
            override val matchedText: String get() = TODO("not implemented")
            override val nonSkipMatchedText: String get() = TODO("not implemented")
            override val numberOfLines: Int get() = TODO("not implemented")
            override val isEmptyLeaf: Boolean get() = TODO("not implemented")
            override val isEmptyMatch: Boolean get() = TODO("not implemented")
            override val isLeaf: Boolean get() = TODO("not implemented")
            override val isList: Boolean get() = TODO("not implemented")
            override val isOptional: Boolean get() = TODO("not implemented")
            override val isExplicitlyNamed: Boolean get() = TODO("not implemented")
            override val isBranch: Boolean get() = TODO("not implemented")
            override val isSkip: Boolean get() = TODO("not implemented")
            override val asLeaf: SPPTLeaf get() = TODO("not implemented")
            override val asBranch: SPPTBranch get() = TODO("not implemented")
            override var parent: SPPTBranch?
                get() = TODO("not implemented")
                set(value) {
                    TODO("not implemented")
                }
            override var tree: SharedPackedParseTree?
                get() = TODO("not implemented")
                set(value) {
                    TODO("not implemented")
                }

            override fun setTags(arg: List<String>) {
                TODO("not implemented")
            }

            override fun contains(other: SPPTNode): Boolean {
                TODO("not implemented")
            }

        }
    }

    // --- SPPTLeaf ---
    override val isPattern: Boolean get() = runtimeRule.isPattern
    override val isLiteral: Boolean get() = runtimeRule.isPattern.not()
    override val isLeaf: Boolean get() = true
    override val isExplicitlyNamed: Boolean get() = this.runtimeRule.isExplicitlyNamed
    override val isBranch: Boolean get() = false
    override val asBranch: SPPTBranch get() = throw SPPTException("Not a Branch", null)
    override val location: InputLocation get() = input.locationFor(startPosition, matchedTextLength)
    override val lastLeaf: SPPTLeaf get() = this
    override val asLeaf: SPPTLeaf get() = this
    override var tagList: List<String> = mutableListOf<String>() //TODO: initialise late

    override val matchedText: String get() = input[startPosition, nextInputPosition]
    override val isEmptyLeaf: Boolean get() = this.runtimeRule.isEmptyTerminal
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

    override fun setTags(tags: List<String>) {
        this.tagList = tags
    }

    override val eolPositions: List<Int> by lazy { input.eolPositions(matchedText) }

    // --- SPPTNode ---
    override val nonSkipMatchedText: String get() = if (isSkip) "" else this.matchedText

    override fun contains(other: SPPTNode): Boolean {
        return this.identity == other.identity
    }

    override fun toString(): String {
        val name = when {
            this.runtimeRule == RuntimeRuleSet.END_OF_TEXT -> RuntimeRuleSet.END_OF_TEXT_TAG
            this.runtimeRule.isEmptyTerminal -> "§empty"
            this.isLiteral -> "'${(this.runtimeRule.rhs as RuntimeRuleRhsLiteral).value}'"
            this.isPattern -> "\"${(this.runtimeRule.rhs as RuntimeRuleRhsPattern).pattern}\""
            else -> this.name //shouldn't happen!
        }

        return "${this.startPosition},${this.nextInputPosition},C:${name}(${this.runtimeRule.ruleNumber})"
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