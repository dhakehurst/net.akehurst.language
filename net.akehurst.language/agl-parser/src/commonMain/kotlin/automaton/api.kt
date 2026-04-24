/**
 * Copyright (C) 2022 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.automaton.api

import net.akehurst.language.parser.api.OptionNum
import net.akehurst.language.parser.api.Rule
import net.akehurst.language.parser.api.RulePosition
import net.akehurst.language.parser.api.RuleSet

enum class AutomatonKind {
    LOOKAHEAD_NONE,     // LC(O) like LR(0)
    LOOKAHEAD_SIMPLE,   // SLC like SLR
    LOOKAHEAD_1         // LC(1) like LR(1)
}

// @JvmInline
// TODO: value classes don't work (fully) in js and wasm
data class StateNumber(val value:Int) //: PublicValueType

interface Automaton {
    val ruleSet : RuleSet
    val state: Set<AutomatonState>
    val transition: Set<AutomatonTransition>

    fun asString(withStates:Boolean=false):String
}

interface AutomatonState {
    val number: StateNumber
    val rulePosition: List<RulePosition>
}

interface AutomatonTransition {
    val action: ParseAction
    val source: AutomatonState
    val target: AutomatonState
    val lookahead: Set<LookaheadGuard>
    val prev:Set<AutomatonState>
    val prevPrev:Set<AutomatonState>
}

interface LookaheadGuard {
    val guard : Set<Rule>
    val up : Set<Rule>
}

enum class ParseAction {
    HEIGHT, // reduce first
    GRAFT,  // reduce other
    WIDTH,  // shift
    GOAL,    // goal
    EMBED,
}

@DslMarker
internal annotation class AglAutomatonDslMarker

@AglAutomatonDslMarker
interface AutomatonBuilder {
    fun state(ruleNumber:Int, option: OptionNum, position:Int)
    fun transition(action: ParseAction, init: TransitionBuilder.() -> Unit)
}

@AglAutomatonDslMarker
interface TransitionBuilder {
    fun pctx(vararg stateNumbers: Int)
    fun ctx(vararg stateNumbers: Int)
    fun src(stateNumber:Int)
    fun tgt(stateNumber:Int)
}