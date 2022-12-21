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
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class test_FirstFollowCache : test_AutomatonUtilsAbstract() {

    val sut = FirstFollowCache3()

    private fun check_calcFirstTermClosure(context: RulePosition, rulePosition: RulePosition, nextContextFollow: LookaheadSetPart, expectedShortStr: Set<String>) {
        val graph = ClosureGraph(context, rulePosition, nextContextFollow)
        sut.calcFirstTermClosure(graph)

        val actualShortStr = graph.nonRootClosures.flatMap { cls -> cls.shortString }.toSet()
        val comp = Comparator<String> { e1, e2 ->
            when {
                e1.length > e2.length -> 1
                e1.length < e2.length -> -1
                else -> e1.compareTo(e2)
            }
        }
        val expSorted = expectedShortStr.sortedWith(comp)
        val actSorted = actualShortStr.sortedWith(comp)
        assertEquals(expSorted, actSorted)
    }

    private fun check_calcAllClosure(G: RuntimeRule, expectedShortStr: Set<String>) {
        val graph = ClosureGraph(RP(G, o0, SR), RP(G, o0, SR), LookaheadSetPart.EOT)
        sut.calcAllClosure(graph)

        val actualShortStr = graph.nonRootClosures.flatMap { cls -> cls.shortString }.toSet()
        val comp = Comparator<String> { e1, e2 ->
            when {
                e1.length > e2.length -> 1
                e1.length < e2.length -> -1
                else -> e1.compareTo(e2)
            }
        }
        val expSorted = expectedShortStr.sortedWith(comp)
        val actSorted = actualShortStr.sortedWith(comp)
        assertEquals(expSorted, actSorted)
    }

    private fun check_firstTerminalInContext(context: RulePosition, rulePosition: RulePosition, nextContextFollow: LookaheadSetPart, expected: Set<FirstTerminalInfo>) {
        sut.clear()
        val actual = sut.firstTerminalInContext(context, rulePosition, nextContextFollow)
        assertEquals(expected, actual)
    }

    private fun check_firstTerminalInContext_all(
        G: RuntimeRule,
        context: RulePosition,
        rulePosition: RulePosition,
        nextContextFollow: LookaheadSetPart,
        expected: Set<FirstTerminalInfo>
    ) {
        val graph = ClosureGraph(RP(G, o0, SR), RP(G, o0, SR), LookaheadSetPart.EOT)
        sut.calcAllClosure(graph)

        val actual = sut.firstTerminalInContext(context, rulePosition, nextContextFollow)
        assertEquals(expected, actual)
    }

    private fun check_parentInContext(
        contextContext: RulePosition,
        context: RulePosition,
        contextNextContextFirstOf: LookaheadSetPart,
        rule: RuntimeRule,
        expected: Set<ParentNext>
    ) {
        sut.firstTerminalInContext(contextContext, context, contextNextContextFirstOf)
        val actual = sut.parentInContext(contextContext, context, rule)
        assertEquals(expected, actual)
    }

    private fun check_parentInContext_all(G: RuntimeRule, contextContext: RulePosition, context: RulePosition, rule: RuntimeRule, expected: Set<ParentNext>) {
        val graph = ClosureGraph(RP(G, o0, SR), RP(G, o0, SR), LookaheadSetPart.EOT)
        sut.calcAllClosure(graph)

        val actual = sut.parentInContext(contextContext, context, rule)
        assertEquals(expected, actual)
    }

    @Test
    fun concatenation_abc() {
        // S =  'a' 'b' 'c' ;
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("a"); literal("b"); literal("c") }
        }
        val S = rrs.findRuntimeRule("S")
        val G = rrs.goalRuleFor[S]
        val a = rrs.findRuntimeRule("'a'")
        val b = rrs.findRuntimeRule("'b'")
        val c = rrs.findRuntimeRule("'c'")

        check_calcFirstTermClosure(
            RP(G, o0, SOR), RP(G, o0, SOR), LookaheadSetPart.EOT,
            setOf(
                "G",
                "G-S[0]",
                "G-S[0]-'a'",
            )
        )

        check_calcAllClosure(
            G, setOf(
                "G",
                "G-S[0]",
                "G-S[1]",
                "G-S[2]",
                "G-S[ER]",
                "G-S[0]-'a'",
                "G-S[1]-'b'",
                "G-S[2]-'c'",
            )
        )

        // G=.S -- W[b] --> 'a'
        check_firstTerminalInContext(
            RP(G, o0, SOR), RP(G, o0, SOR), LookaheadSetPart.EOT,
            setOf(
                FirstTerminalInfo(a, LHS(b))
            )
        )

        // G=.S -- H[b](EOT) --> S=a.bc
        check_parentInContext(
            RP(G, o0, SOR), RP(G, o0, SOR), LookaheadSetPart.EOT, a,
            setOf(
                ParentNext(firstPosition = true, rulePosition = RP(S, o0, p1), firstOf = LHS(b), parentFollow = LHS(EOT))
            )
        )

        // S=a.bc -- W[c] --> 'b'
        check_firstTerminalInContext(
            RP(G, o0, SOR), RP(S, o0, p1), LookaheadSetPart.RT,
            setOf(
                FirstTerminalInfo(terminalRule = b, follow = LHS(c))
            )
        )

        // G=.S -- W --> 'a'
        check_firstTerminalInContext_all(
            G,
            RP(G, o0, SOR), RP(G, o0, SOR), LookaheadSetPart.EOT,
            setOf(
                FirstTerminalInfo(a, LHS(b))
            )
        )
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
/*
        check_calcFirstTermClosure(
            RP(G, o0, SOR), RP(G, o0, SOR), LookaheadSetPart.EOT,
            setOf(
                "G",
                "G-S.0",
                "G-S.1",
                "G-S.0-'a'",
                "G-S.1-S1[0]",
                "G-S.1-S1[0]-S.0",
                "G-S.1-S1[0]-S.1",
                "G-S.1-S1[0]-S.0-'a'",
                "G-S.1-S1[0]-S.1-S1[0]",
                "G-S.1-S1[0]-S.1-S1[0]-S.0",
                "G-S.1-S1[0]-S.1-S1[0]-S.1",
                "G-S.1-S1[0]-S.1-S1[0]-S.0-'a'",
            )
        )

        check_calcAllClosure(
            G, setOf(
                "G",
                "G-S.0",
                "G-S.1",
                "G-S.0-'a'",
                "G-S.1-S1[0]",
                "G-S.1-S1[1]",
                "G-S.1-S1[ER]",
                "G-S.1-S1[0]-S.0",
                "G-S.1-S1[0]-S.1",
                "G-S.1-S1[1]-'a'",
                "G-S.1-S1[0]-S.0-'a'",
                "G-S.1-S1[0]-S.1-S1[0]",
                "G-S.1-S1[0]-S.1-S1[1]",
                "G-S.1-S1[0]-S.1-S1[ER]",
                "G-S.1-S1[0]-S.1-S1[0]-S.0",
                "G-S.1-S1[0]-S.1-S1[0]-S.1",
                "G-S.1-S1[0]-S.1-S1[1]-'a'",
                "G-S.1-S1[0]-S.1-S1[0]-S.0-'a'",
                "G-S.1-S1[0]-S.1-S1[0]-S.1-S1[1]",
                "G-S.1-S1[0]-S.1-S1[0]-S.1-S1[ER]",
                "G-S.1-S1[0]-S.1-S1[0]-S.1-S1[1]-'a'",
            )
        )

        // G=.S -- W --> 'a'
        check_firstTerminalInContext(
            RP(G, o0, SOR), RP(G, o0, SOR), LookaheadSetPart.EOT,
            setOf(
                FirstTerminalInfo(a, LHS(EOT)),
                FirstTerminalInfo(a, LHS(a))
            )
        )

        // a -- H[<EOT>](<EOT>) --> S=a.    | G-S.0-'a'
        // a -- H[a](a, <EOT>) --> S=a.     | G-S.1-S1[0]-S.0-'a', G-S.1-S1[0]-S.1-S1[0]-S.0-'a'
        check_parentInContext(
            RP(G, o0, SOR), RP(G, o0, SOR), LookaheadSetPart.EOT, a,
            setOf(
                ParentNext(true, RP(S, o0, ER), LHS(EOT), LHS(EOT)),
                ParentNext(true, RP(S, o0, ER), LHS(a), LHS(a, EOT))
            )
        )

        // S1=S.a -- W[RT] --> 'a'
        check_firstTerminalInContext(
            RP(G, o0, SOR), RP(S1, o0, p1), LookaheadSetPart.RT,
            setOf(
                FirstTerminalInfo(a, LHS(RT))
            )
        )

        // G=.S -- W --> 'a'
        check_firstTerminalInContext_all(
            G,
            RP(G, o0, SOR), RP(G, o0, SOR), LookaheadSetPart.EOT,
            setOf(
                FirstTerminalInfo(a, LHS(EOT)),
                FirstTerminalInfo(a, LHS(a))
            )
        )

        // a -- H[<EOT>](<EOT>) --> S=a.    | G-S.0-'a'
        // a -- H[a](a, <EOT>) --> S=a.     | G-S.1-S1[0]-S.0-'a', G-S.1-S1[0]-S.1-S1[0]-S.0-'a'
        check_parentInContext_all(
            G,
            RP(G, o0, SR), RP(G, o0, SR), a,
            setOf(
                ParentNext(true, RP(S, o0, ER), LHS(EOT), LHS(EOT)),
                ParentNext(true, RP(S, o0, ER), LHS(a), LHS(a, EOT))
            )
        )
*/
        // S -- H[<EOT>]() --> G=S.            | G-S.0-'a'
        // S -- H[a](a,<EOT>) --> S1=S.a       | G-S.1-S1[0]-S.0-'a', G-S.1-S1[0]-S.1-S1[0]-S.0-'a'
        check_parentInContext_all(
            G,
            RP(G, o0, SR), RP(G, o0, SR), S,
            setOf(
                ParentNext(true, RP(G, o0, ER), LHS(EOT), LHS()),
                ParentNext(true, RP(S1, o0, p1), LHS(a), LHS(EOT)),
                ParentNext(true, RP(S1, o0, p1), LHS(a), LHS(a))
            )
        )
//ParentNext(firstPosition=true, rulePosition=0.RP(<GOAL>,0,-1), firstOf=LHS(<EOT>), parentFollow=LHS()),
// ParentNext(firstPosition=true, rulePosition=0.RP(S1,0,1), firstOf=LHS('a'), parentFollow=LHS(<EOT>)),
// ParentNext(firstPosition=true, rulePosition=0.RP(S1,0,1), firstOf=LHS('a'), parentFollow=LHS('a'))]


        // S1=S.a -- W[<EOT>,a] --> 'a'
        check_firstTerminalInContext_all(
            G,
            RP(G, o0, SOR), RP(S1, o0, p1), LookaheadSetPart.EOT,
            setOf(
                FirstTerminalInfo(a, LHS(EOT)),
                FirstTerminalInfo(a, LHS(a))
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


    @Test
    fun sList_0_n_literal() {
        val rrs = runtimeRuleSet {
            sList("S", 0, -1, "'a'", "'b'")
            literal("'a'", "a")
            literal("'b'", "b")
        }
        val S = rrs.findRuntimeRule("S")
        val G = rrs.goalRuleFor[S]
        val a = rrs.findRuntimeRule("'a'")
        val b = rrs.findRuntimeRule("'b'")

        check_calcFirstTermClosure(
            RP(G, o0, SOR), RP(G, o0, SOR), LookaheadSetPart.EOT,
            setOf(
                "G",
                "G-S.b",
                "G-S.b-'a'",
                "G-S.b-<E>"
            )
        )

        check_calcAllClosure(
            G, setOf(
                "G",
                "G-S.b",
                "G-S.i",
                "G-S.s",
                "G-S.e",
                "G-S.b-'a'",
                "G-S.b-<E>",
                "G-S.i-'a'",
                "G-S.s-'b'"
            )
        )

        // G=.S -- W[b,<EOT>] --> 'a'
        // G=.S -- W[<EOT>] --> <empty>
        check_firstTerminalInContext(
            RP(G, o0, SOR), RP(G, o0, SOR), LookaheadSetPart.EOT,
            setOf(
                FirstTerminalInfo(a, LHS(b, EOT)),
                FirstTerminalInfo(EMPTY, LHS(EOT))
            )
        )

        check_parentInContext(
            RP(G, o0, SOR), RP(G, o0, SOR), LookaheadSetPart.EOT, a,
            setOf(
                ParentNext(true, RP(S, OLI, PLS), LHS(b), LHS(EOT)),
                ParentNext(true, RP(S, OLI, ER), LHS(EOT), LHS(EOT))
            )
        )

    }


    @Test
    fun java_NavigableExpression() {
        // NavigableExpression = MethodReference | GenericMethodInvocation ;
        // MethodReference = MethodInvocation '::' IDENTIFIER ;
        // GenericMethodInvocation = TypeArguments? MethodInvocation ;
        // MethodInvocation = IDENTIFIER '(' Expression ')' ;
        // TypeArguments = '<>' ;
        // Expression = Postfix | IDENTIFIER ;
        // Postfix = Expression '++' ;
        // leaf IDENTIFIER = "[A-Za-z]+" ;
        val rrs = runtimeRuleSet {
            choice("NE", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("MR")
                ref("GMI")
            }
            concatenation("MR") { ref("MI"); literal("::"); ref("id") }
            concatenation("GMI") { ref("oTA"); ref("MI"); }
            concatenation("MI") { ref("id"); literal("("); ref("E"); literal(")") }
            multi("oTA", 0, 1, "TA")
            concatenation("TA") { literal("<>") }
            choice("E", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("P")
                ref("id")
            }
            concatenation("P") { ref("E"); literal("++"); }
            pattern("id", "[a-zA-Z]+")
        }
        val NE = rrs.findRuntimeRule("NE")
        val G = rrs.goalRuleFor[NE]

        check_calcFirstTermClosure(
            RP(G, o0, SOR), RP(G, o0, SOR), LookaheadSetPart.EOT,
            setOf(
                "G-NE.0",
                "G-NE.1",
                "G-NE.0-MR[0]",
                "G-NE.1-GMI[0]",
                "G-NE.0-MR[0]-MI[0]",
                "G-NE.1-GMI[0]-oTA.b",
                "G-NE.0-MR[0]-MI[0]-id",
                "G-NE.1-GMI[0]-oTA.b-TA[0]",
                "G-NE.1-GMI[0]-oTA.b-<E>",
                "G-NE.1-GMI[0]-oTA.b-TA[0]-'<>'"
            )
        )

        check_calcAllClosure(
            G, setOf(
                "G-NE.0",
                "G-NE.1",
                "G-NE.0-MR[0]",
                "G-NE.0-MR[1]",
                "G-NE.0-MR[2]",
                "G-NE.0-MR[ER]",
                "G-NE.1-GMI[0]",
                "G-NE.1-GMI[1]",
                "G-NE.1-GMI[ER]",
                "G-NE.0-MR[0]-MI[0]",
                "G-NE.0-MR[0]-MI[1]",
                "G-NE.0-MR[0]-MI[2]",
                "G-NE.0-MR[0]-MI[3]",
                "G-NE.0-MR[0]-MI[ER]",
                "G-NE.0-MR[1]-'::'",
                "G-NE.0-MR[2]-id",
                "G-NE.1-GMI[0]-oTA.b",
                "G-NE.1-GMI[0]-oTA.e",
                "G-NE.1-GMI[1]-MI[0]",
                "G-NE.1-GMI[1]-MI[1]",
                "G-NE.1-GMI[1]-MI[2]",
                "G-NE.1-GMI[1]-MI[3]",
                "G-NE.1-GMI[1]-MI[ER]",
                "G-NE.0-MR[0]-MI[0]-id",
                "G-NE.0-MR[0]-MI[1]='('",
            )
        )
    }
}