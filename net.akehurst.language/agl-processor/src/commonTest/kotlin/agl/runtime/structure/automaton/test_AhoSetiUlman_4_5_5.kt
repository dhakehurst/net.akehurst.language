/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

import kotlin.test.Test
import kotlin.test.assertEquals

class test_AhoSetiUlman_4_5_5 {

    companion object {
        // S = C C ;
        // C = c C | d ;
        //
        // S = C C ;
        // C = C1 | d ;
        // C1 = c C ;
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("C"); ref("C") }
            choice("C", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("C1")
                literal("d")
            }
            concatenation("C1") { literal("c"); ref("C") }
        }
        val S = rrs.findRuntimeRule("S")
        val C = rrs.findRuntimeRule("C")
        val C1 = rrs.findRuntimeRule("C1")
        val cT = rrs.findRuntimeRule("'c'")
        val dT = rrs.findRuntimeRule("'d'")
        val G = rrs.startingState(S, emptySet()).runtimeRule

        val s0 = rrs.startingState(S, emptySet())

        val lhs0 = LookaheadSet(0, setOf(rrs.END_OF_TEXT))
        val lhs1 = LookaheadSet(1, setOf(dT, cT))
    }

    @Test
    fun s0_widthInto() {
TODO()
    }
}