/**
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

import net.akehurst.language.agl.automaton.LookaheadSetPart

internal class PrecedenceRules(
    val contextRule: RuntimeRule,
    val rules: List<PrecedenceRule>
) {

    enum class Associativity { NONE, LEFT, RIGHT }

    data class PrecedenceRule(
        val precedence: Int,
        val target: RuntimeRule,
        val operators: Set<RuntimeRule>,
        val associativity: Associativity
    )

    fun precedenceFor(to: Set<RuntimeRule>, lh: LookaheadSetPart): List<PrecedenceRule> {
        val r = rules.filter { pr ->
            to.contains(pr.target) && pr.operators.any { lh.fullContent.contains(it) }
        }
        return r
    }
}