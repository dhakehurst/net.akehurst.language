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

import net.akehurst.language.automaton.leftcorner.LookaheadSetPart
import net.akehurst.language.parser.api.Assoc
import net.akehurst.language.parser.api.OptionNum
import net.akehurst.language.parser.api.PrefOption
import net.akehurst.language.parser.api.PrefRule

class RuntimePreferenceRule(
    override val contextRule: RuntimeRule,
    override val options: List<RuntimePreferenceOption>
) : PrefRule {

    data class RuntimePreferenceOption(
        override val precedence: Int,
        override  val spine: List<RuntimeRule>,
        override  val option: OptionNum,
        override val operators: Set<RuntimeRule>,
        override  val associativity: Assoc
    ) : PrefOption
}