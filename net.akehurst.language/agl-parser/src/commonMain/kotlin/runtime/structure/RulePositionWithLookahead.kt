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

// @JvmInline
// TODO: value classes don't work (fully) in js and wasm
data class StateNumber(val value:Int) //: PublicValueType

//TODO: how is this different to ParentRelation ?
internal data class RulePositionWithLookahead(
    val rulePosition: RulePositionRuntime,
    val lookahead: Set<RuntimeRule>
) {

    //val items:Set<RuntimeRule> get() { return this.rulePosition.items }
    //val runtimeRule:RuntimeRule get() { return this.rulePosition.runtimeRule }
    //val choice:Int get() { return this.rulePosition.option }
    //val position:Int get() { return this.rulePosition.position }

    val isAtStart: Boolean get() { return this.rulePosition.isAtStart }
    val isAtEnd: Boolean get() { return this.rulePosition.isAtEnd }

}