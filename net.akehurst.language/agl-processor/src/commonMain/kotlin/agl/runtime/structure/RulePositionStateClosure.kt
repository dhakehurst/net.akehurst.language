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

inline class ClosureNumber(val value: Int)

class RulePositionClosure(
    val number: ClosureNumber,
    val root: RulePosition,
    val content : Set<RulePositionPath> //TODO: enable this to be calculated on demand
) {


    override fun equals(other: Any?): Boolean {
        if (other is RulePositionClosure) {
            return this.number == other.number
        } else {
            return false
        }
    }

    override fun hashCode(): Int {
        return this.number.value
    }

    override fun toString(): String {
        return "RPC(${this.number},${this.root})"
    }

}