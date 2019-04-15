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

package net.akehurst.language.agl.runtime.structure

class RulePositionPath(
        val ancestorRPs: List<RulePositionState>,
        val rulePosition: RulePositionState
) {

    val items: Set<RuntimeRule>
        get() {
            return this.rulePosition.items
        }
    val runtimeRule: RuntimeRule
        get() {
            return this.rulePosition.runtimeRule
        }
    val choice: Int
        get() {
            return this.rulePosition.choice
        }
    val position: Int
        get() {
            return this.rulePosition.position
        }

    val isAtEnd: Boolean
        get() {
            return this.rulePosition.isAtEnd
        }

    val directParent = ancestorRPs.lastOrNull() // assumes that sets are ordered, which if created via kotlin setOf, they should be.
    val parentAncestors: List<RulePositionState> = when (ancestorRPs.size) {
        0 -> emptyList<RulePositionState>()
        else -> ancestorRPs - directParent!!
    }
    // --- Any ---

    override fun hashCode(): Int {
        return rulePosition.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return if (other is RulePositionPath) {
            this.rulePosition == other.rulePosition && this.ancestorRPs == other.ancestorRPs
        } else {
            false
        }
    }

    override fun toString(): String {
        return "RPP(${ancestorRPs},${rulePosition})"
    }

}

class RulePositionWithGlhPath(
        val ancestorRPs: List<Pair<RulePosition, Set<RuntimeRule>>>,
        val rulePosition: Pair<RulePosition, Set<RuntimeRule>>
) {
    // --- Any ---

    override fun hashCode(): Int {
        return rulePosition.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return if (other is RulePositionWithGlhPath) {
            this.rulePosition == other.rulePosition //
                    && this.ancestorRPs == other.ancestorRPs
        } else {
            false
        }
    }

    override fun toString(): String {
        return "RPP(${ancestorRPs},${rulePosition})"
    }

}