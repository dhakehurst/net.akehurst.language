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

class test_LR1_States {

    companion object {
        // This grammar is LR(1) but not LALR(1)

        // S = A a | b A c | B c | b B a ;
        // A = d ;
        // B = d ;
        //
        // S = S1 | S2 | S3 | S4
        // S1 = A a ;
        // S2 = b A c ;
        // S3 = B c ;
        // S4 = b B a ;
        // A = d ;
        // B = d ;

        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("S1")
                ref("S2")
                ref("S3")
                ref("S4")
            }
            concatenation("S1") { ref("A"); literal("a") }
            concatenation("S2") { literal("b"); ref("A"); literal("c") }
            concatenation("S3") { ref("B"); literal("c") }
            concatenation("S4") { literal("b"); ref("B"); literal("a") }
            concatenation("A") { literal("d") }
            concatenation("B") { literal("d") }
        }
        val S = rrs.findRuntimeRule("S")
        val S1 = rrs.findRuntimeRule("S1")
        val S2 = rrs.findRuntimeRule("S2")
        val S3 = rrs.findRuntimeRule("S3")
        val S4 = rrs.findRuntimeRule("S4")
        val rA = rrs.findRuntimeRule("A")
        val rB = rrs.findRuntimeRule("B")
        val a = rrs.findRuntimeRule("'a'")
        val b = rrs.findRuntimeRule("'b'")
        val c = rrs.findRuntimeRule("'c'")
        val d = rrs.findRuntimeRule("'d'")
    }

    @Test
    fun s0_closure() {
        val s0 = rrs.startingState(S)
        val actual = s0.createClosure(emptySet())

        val expected = setOf<RulePositionWithLookahead>(
                RulePositionWithLookahead(RulePosition(s0.runtimeRule, 0, 0), setOf()),
                RulePositionWithLookahead(RulePosition(S, 0, 0), setOf(RuntimeRuleSet.END_OF_TEXT)),
                RulePositionWithLookahead(RulePosition(S, 1, 0), setOf(RuntimeRuleSet.END_OF_TEXT)),
                RulePositionWithLookahead(RulePosition(S, 2, 0), setOf(RuntimeRuleSet.END_OF_TEXT)),
                RulePositionWithLookahead(RulePosition(S, 3, 0), setOf(RuntimeRuleSet.END_OF_TEXT)),
                RulePositionWithLookahead(RulePosition(S1, 0, 0), setOf(a)),
                RulePositionWithLookahead(RulePosition(S2, 0, 0), setOf(d)),
                RulePositionWithLookahead(RulePosition(S3, 0, 0), setOf(c)),
                RulePositionWithLookahead(RulePosition(S4, 0, 0), setOf(d)),
                RulePositionWithLookahead(RulePosition(rA, 0, 0), setOf(a)), //NOT c, because the A in S2 is not at pos 0
                RulePositionWithLookahead(RulePosition(rB, 0, 0), setOf(c)), //NOT a, because the B in S4 is not at pos 0
                RulePositionWithLookahead(RulePosition(b, 0, 0), setOf(d)),
                RulePositionWithLookahead(RulePosition(d, 0, 0), setOf(a)),
                RulePositionWithLookahead(RulePosition(d, 0, 0), setOf(c))
        )

        assertEquals(expected, actual)
    }

    @Test
    fun growsInto() {
        val s0 = rrs.startingState(S)
        val actual = s0.transitions(null)

        val s2 = s0.stateSet.fetch(RulePosition(b, 0, 0))
        val expected = listOf<Transition>(
                Transition(s0, s2, Transition.ParseAction.WIDTH, setOf(), null, { _, _ -> true })
        )

        assertEquals(expected, actual)
    }

    @Test
    fun s0_transitions() {
        val s0 = rrs.startingState(S)
        val actual = s0.transitions(null)

        val s2 = s0.stateSet.fetch(RulePosition(b, 0, RulePosition.END_OF_RULE))
        val s3 = s0.stateSet.fetch(RulePosition(d, 0, RulePosition.END_OF_RULE))
        val expected = listOf<Transition>(
                Transition(s0, s2, Transition.ParseAction.WIDTH, setOf(d), null, { _, _ -> true }),
                Transition(s0, s3, Transition.ParseAction.WIDTH, setOf(a,c), null, { _, _ -> true })
        )

        assertEquals(expected, actual)
    }

    @Test
    fun s3_transitions() {
        val s0 = rrs.startingState(S)
        s0.transitions(null)
        val s2 = s0.stateSet.fetch(RulePosition(b, 0, RulePosition.END_OF_RULE))
        val s3 = s0.stateSet.fetch(RulePosition(d, 0, RulePosition.END_OF_RULE))

        val actual = s3.transitions(s0)
        val expected = listOf<Transition>(
                Transition(s0, s2, Transition.ParseAction.WIDTH, setOf(d), null, { _, _ -> true }),
                Transition(s0, s3, Transition.ParseAction.WIDTH, setOf(a,c), null, { _, _ -> true })
        )

        assertEquals(expected, actual)
    }

}