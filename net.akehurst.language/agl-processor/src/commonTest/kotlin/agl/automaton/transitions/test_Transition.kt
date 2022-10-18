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

package net.akehurst.language.agl.automaton

import agl.automaton.automaton
import net.akehurst.language.agl.automaton.ParserState.Companion.lhs
import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.StateNumber
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

internal class test_Transition : test_AutomatonUtilsAbstract() {

    @Test
    fun t() {

        val rrs = runtimeRuleSet {
            concatenation("S") { ref("expr") }
            choice("expr", RuntimeRuleChoiceKind.PRIORITY_LONGEST) {
                ref("root")
                ref("div")
                ref("add")
            }
            choice("root", RuntimeRuleChoiceKind.PRIORITY_LONGEST) {
                ref("var")
            }
            sList("div", 2, -1, "expr", "'/'")
            sList("add", 2, -1, "expr", "'+'")
            concatenation("var") { literal("v") }
            literal("'/'", "/")
            literal("'+'", "+")
        }
        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val G = SM.startState.runtimeRules.first()
        val v = rrs.findRuntimeRule("'v'")
        val d = rrs.findRuntimeRule("'/'")
        val a = rrs.findRuntimeRule("'+'")
        val vr = rrs.findRuntimeRule("var")

        val stateSet = ParserStateSet(0, rrs, S, false, AutomatonKind.LOOKAHEAD_1)
        val s1 = ParserState(StateNumber(1), listOf(RP(v, o0, EOR)), stateSet)
        val s2 = ParserState(StateNumber(2), listOf(RP(vr, o0, EOR)), stateSet)
        val tr1 = Transition(
            from = s1,
            to = s2,
            action = Transition.ParseAction.HEIGHT,
            lookahead = setOf(Lookahead(LHS(EOT, d).lhs(stateSet), LHS(EOT, d).lhs(stateSet)), Lookahead(LHS(a).lhs(stateSet), LHS(a).lhs(stateSet)))
        )
        val tr2 = Transition(
            from = s1,
            to = s2,
            action = Transition.ParseAction.HEIGHT,
            lookahead = setOf(
                Lookahead(LHS(EOT).lhs(stateSet), LHS(EOT).lhs(stateSet)),
                Lookahead(LHS(d).lhs(stateSet), LHS(d).lhs(stateSet)),
                Lookahead(LHS(a).lhs(stateSet), LHS(a).lhs(stateSet))
            )
        )

        val group = mutableSetOf(tr1, tr2).groupBy {
            val l = listOf(tr1.from, tr1.action, tr1.to, tr1.runtimeGuard)
            println("$l")
            l
        }

        val list1 = listOf(tr1.from, tr1.action, tr1.to, tr1.runtimeGuard)
        val list2 = listOf(tr2.from, tr2.action, tr2.to, tr2.runtimeGuard)

        assertEquals(list1, list2)
        assertEquals(1, group.size)
    }

    @Test
    fun lambda_function_equals_is_different_on_JS() {
        // in TransitionCacheLC1.merge, transitions are grouped using trans.runtimeGuard as one of the keys
        // trans.runtimeGuard is a lambda function - defaults to 'true'
        // diff result on JVM and JS
        fun lambd() = { true }

        val l1 = lambd()
        val l2 = lambd()

        val m1 = { true }
        val m2 = { true }

        assertNotEquals(m1.hashCode(), m2.hashCode()) //pass JVM and JS
        assertEquals(l1.hashCode(), l2.hashCode()) // pass JVM, fail JS
    }

}