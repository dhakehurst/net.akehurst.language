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
    val stateNumber: StateNumber,
    val rulePosition: RulePosition,
    val ancestorRPs: List<RulePosition>
) {

    val items:Set<RuntimeRule> get() { return this.rulePosition.items }
    val runtimeRule:RuntimeRule get() { return this.rulePosition.runtimeRule }
    val choice:Int get() { return this.rulePosition.choice }
    val position:Int get() { return this.rulePosition.position }

    val isAtEnd: Boolean get() { return this.rulePosition.isAtEnd }

    val directParent = ancestorRPs.lastOrNull() // assumes that sets are ordered, which if created via kotlin setOf, they should be.
    val parentAncestors:List<RulePosition> = when (ancestorRPs.size) {
        0-> emptyList<RulePosition>()
        else ->ancestorRPs - directParent!!
    }
    // --- Any ---

    override fun hashCode(): Int {
        return stateNumber.value
    }

    override fun equals(other: Any?): Boolean {
        return if (other is RulePositionState) {
            this.rulePosition == other.rulePosition && this.ancestorRPs == other.ancestorRPs
        } else {
            false
        }
    }

    override fun toString(): String {
        return "RPP(${stateNumber.value},${rulePosition},${ancestorRPs})"
    }

    fun deepEquals(other:RulePositionState) :Boolean {
        return this.rulePosition == other.rulePosition && this.ancestorRPs == other.ancestorRPs
    }

}
