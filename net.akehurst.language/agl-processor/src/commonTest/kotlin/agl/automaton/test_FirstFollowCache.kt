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

package net.akehurst.language.agl.automaton

import net.akehurst.language.agl.agl.automaton.FirstOf
import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class test_FirstFollowCache : test_AutomatonUtilsAbstract() {

    val sut = FirstFollowCache3()

    private fun check_calcFirstTermClosure(context: RulePosition, rulePosition: RulePosition, nextContextFollow: LookaheadSetPart, expectedShortStr: Set<String>) {
        val graph = ClosureGraph(context, rulePosition, nextContextFollow)
        sut.calcFirstTermClosure(graph)

        assertEquals(expectedShortStr.size, graph.nonRootClosures.size)
        val actualShortStr = graph.nonRootClosures.flatMap { cls -> cls.shortString }.toSet()
        assertEquals(expectedShortStr, actualShortStr)
    }

    private fun check_calcAllClosure(G: RuntimeRule, expectedShortStr: Set<String>) {
        val graph = ClosureGraph(RP(G, o0, SOR), RP(G, o0, SOR), LookaheadSetPart.EOT)
        sut.calcAllClosure(graph)

        assertEquals(expectedShortStr.size, graph.nonRootClosures.size)
        val actualShortStr = graph.nonRootClosures.flatMap { cls -> cls.shortString }.toSet()
        assertEquals(expectedShortStr, actualShortStr)
    }

    @Test
    fun leftRecursive() {
        // S =  'a' | S1
        // S1 = S 'a'
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("a")
                ref("S1")
            }
            concatenation("S1") { ref("S"); literal("a") }
        }
        val S = rrs.findRuntimeRule("S")
        val G = rrs.goalRuleFor[S]
        val S1 = rrs.findRuntimeRule("S1")
        val a = rrs.findRuntimeRule("'a'")

        check_calcFirstTermClosure(
            RP(G, o0, SOR), RP(G, o0, SOR), LookaheadSetPart.EOT,
            setOf(
                "G-S",
                "G-S-a",
                "G-S-S1-S-a"
            )
        )

        check_calcAllClosure(
            G, setOf(
                "G-S-a",
                "G-S-S1-S-a",
                "G-S-S1-S-S-a",
                "G-S-S1-a",
                "G",
                "G-S",
                "G-S-S1",
                "G-S-S1-S",
                "G-S-s1-S-S",
            )
        )
    }

    @Test
    fun leftRecursive2() {
        // S =  'a' | S 'a'
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("a")
                concatenation { ref("S"); literal("a") }
            }
        }
        val S = rrs.findRuntimeRule("S")
        val G = rrs.goalRuleFor[S]
        val a = rrs.findRuntimeRule("'a'")


    }

}