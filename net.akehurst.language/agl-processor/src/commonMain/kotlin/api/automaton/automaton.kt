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

package net.akehurst.language.api.automaton

import net.akehurst.language.agl.api.runtime.RuleSet
import net.akehurst.language.agl.automaton.automaton
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.api.processor.AutomatonKind

interface Automaton {
    companion object {
        fun build(
            rrs: RuleSet,
            automatonKind: AutomatonKind,
            userGoalRule: String,
            isSkip: Boolean,
            init: AutomatonBuilder.() -> Unit
        ): Automaton = automaton(rrs as RuntimeRuleSet, automatonKind, userGoalRule, isSkip, init)
    }

    fun asString(withStates:Boolean=false):String
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
    fun state(ruleNumber:Int, option:Int, position:Int)
    fun transition(action: ParseAction, init: TransitionBuilder.() -> Unit)
}

@AglAutomatonDslMarker
interface TransitionBuilder {
    fun ctx(vararg stateNumbers: Int)
    fun source(stateNumber:Int)
    fun target(stateNumber:Int)
}