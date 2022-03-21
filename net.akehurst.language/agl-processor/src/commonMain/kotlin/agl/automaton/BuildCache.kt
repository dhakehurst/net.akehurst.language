/**
 * Copyright (C) 2021 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.automaton

import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.agl.runtime.structure.RuntimeRule

internal interface BuildCache {
    fun on()
    fun clearAndOff()

    fun stateInfo(): Set<StateInfo>
    fun widthInto(prevState:ParserState, fromState: ParserState): Set<WidthInfo>
    fun heightGraftInto(prevState:ParserState, fromState: ParserState) : Set<HeightGraftInfo>

    fun firstOf(rulePosition: RulePosition, ifReachedEnd: LookaheadSetPart): LookaheadSetPart

}