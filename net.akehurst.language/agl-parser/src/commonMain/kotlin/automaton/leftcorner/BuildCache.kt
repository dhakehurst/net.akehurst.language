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

package net.akehurst.language.automaton.leftcorner

import net.akehurst.language.agl.runtime.structure.RulePositionRuntime

internal interface BuildCache {
    fun switchCacheOn()
    fun clearAndOff()

    fun stateInfo(): Set<StateInfo>
    fun mergedStateInfoFor(rulePositions: List<RulePositionRuntime>): StateInfo
    fun widthInto(prevState:ParserState, fromState: ParserState): Set<WidthInfo>
    fun heightOrGraftInto(prevPrev: ParserState, prevState: ParserState, fromState: ParserState) : Set<TransInfo>

    //fun expectedAt(rulePosition: RulePosition, ifReachedEnd: LookaheadSetPart): LookaheadSetPart

    // exposed on interface so we can test them
    //fun firstTerminal(prev: RuntimeState, fromState: RuntimeState): List<RuntimeRule>
    //fun followAtEndInContext(prev: RuntimeState, runtimeRule: RuntimeRule): List<RuntimeRule>
}