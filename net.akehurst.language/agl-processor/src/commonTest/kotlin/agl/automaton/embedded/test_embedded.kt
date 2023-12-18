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

package net.akehurst.language.agl.automaton.embedded

import net.akehurst.language.agl.automaton.AutomatonTest
import net.akehurst.language.agl.automaton.automaton
import net.akehurst.language.agl.automaton.test_AutomatonAbstract
import net.akehurst.language.agl.parser.LeftCornerParser
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.agl.scanner.ScannerOnDemand
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test

internal class test_embedded : test_AutomatonAbstract() {

    /*
    B = b ;

    S = a gB a ;
    gB = grammar B ;
 */
    private companion object {

        val rrsB = runtimeRuleSet {
            concatenation("B") { literal("b") }
        }
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("a"); ref("gB"); literal("a"); }
            embedded("gB", rrsB, "B")
        }

        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val gB = rrs.findRuntimeRule("gB")
        val a = rrs.findRuntimeRule("'a'")
        val G = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1).startState.runtimeRules.first()

        val B = rrsB.findRuntimeRule("B")
        val b_ = rrsB.findRuntimeRule("'b'")

        val S_SM = rrsB.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val s0 = S_SM.startState

        val B_SM = rrsB.fetchStateSetFor(B, AutomatonKind.LOOKAHEAD_1)
    }

    @Test
    fun parse_aba() {
        val parser = LeftCornerParser(ScannerOnDemand(rrs.nonSkipTerminals), rrs)
        parser.parseForGoal("S", "aba")
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {


        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {

        }

        AutomatonTest.assertEquals(expected, actual)
    }
}