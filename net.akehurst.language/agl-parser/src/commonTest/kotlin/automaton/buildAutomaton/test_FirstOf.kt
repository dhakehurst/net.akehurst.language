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

package net.akehurst.language.automaton.leftcorner

import net.akehurst.language.agl.runtime.structure.RulePositionRuntime
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import kotlin.test.Test
import kotlin.test.assertEquals

class test_FirstOf : test_AutomatonUtilsAbstract() {

    internal val sut = FirstOf()

    private fun check_expectedAt(rulePosition: RulePositionRuntime, ifReachedEnd: LookaheadSetPart, expected: Set<RuntimeRule>) {
        val actual = sut.expectedAt(rulePosition, ifReachedEnd)
        assertEquals(LookaheadSetPart.createFromRuntimeRules(expected), actual)
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

        check_expectedAt(RP(G, oN, SR), LookaheadSetPart.EOT, expected = setOf(EOT, a))
        check_expectedAt(RP(S, oSI, SR), LookaheadSetPart.EOT, expected = setOf(a))
        check_expectedAt(RP(S, oSE, SR), LookaheadSetPart.EOT, expected = setOf(EOT))
        check_expectedAt(RP(S, oSS, PLS), LookaheadSetPart.EOT, expected = setOf(b))
        check_expectedAt(RP(S, oSI, PLI), LookaheadSetPart.EOT, expected = setOf(a))
        check_expectedAt(RP(S, oSI, ER), LookaheadSetPart.EOT, expected = setOf(EOT))
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

        check_expectedAt(RP(G, oN, SOR), LookaheadSetPart.EOT, expected = setOf(a))
        check_expectedAt(RP(G, oN, ER), LookaheadSetPart.EMPTY, expected = emptySet())

        check_expectedAt(RP(S, oN, SOR), LookaheadSetPart.EOT, expected = setOf(a))
        check_expectedAt(RP(S, oN, ER), LookaheadSetPart.EOT, expected = setOf(EOT))

        check_expectedAt(RP(S, o1, SOR), LookaheadSetPart.EOT, expected = setOf(a))
        check_expectedAt(RP(S, o1, ER), LookaheadSetPart.EOT, expected = setOf(EOT))

        check_expectedAt(RP(S1, oN, SOR), LookaheadSetPart.EOT, expected = setOf(a))
        check_expectedAt(RP(S1, oN, p1), LookaheadSetPart.EOT, expected = setOf(a))
        check_expectedAt(RP(S1, oN, EOR), LookaheadSetPart.EOT, expected = setOf(EOT))

        check_expectedAt(RP(a, oN, EOR), LookaheadSetPart.EOT, expected = setOf(EOT))
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

        check_expectedAt(RP(G, oN, SOR), LookaheadSetPart.EOT, expected = setOf(a))
        check_expectedAt(RP(G, oN, EOR), LookaheadSetPart.EMPTY, expected = emptySet())

        check_expectedAt(RP(S, oN, SOR), LookaheadSetPart.EOT, expected = setOf(a))
        check_expectedAt(RP(S, oN, EOR), LookaheadSetPart.EOT, expected = setOf(EOT))

        check_expectedAt(RP(S, o1, SOR), LookaheadSetPart.EOT, expected = setOf(a))
        check_expectedAt(RP(S, o1, p1), LookaheadSetPart.EOT, expected = setOf(a))
        check_expectedAt(RP(S, o1, EOR), LookaheadSetPart.EOT, expected = setOf(EOT))

        check_expectedAt(RP(a, oN, EOR), LookaheadSetPart.EOT, expected = setOf(EOT))
    }

    @Test
    fun hiddenRight2() {
        // S = S1 | 'a'
        // S1 = S C B
        // B = 'b' | Be
        // Be = <empty>
        // C = 'c'
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("S1")
                literal("a")
            }
            concatenation("S1") { ref("S"); ref("C"); ref("B") }
            choice("B", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("b")
                ref("Be")
            }
            concatenation("Be") { empty() }
            concatenation("C") { literal("c") }
        }
        val S = rrs.findRuntimeRule("S")
        val G = rrs.goalRuleFor[S]
        val S1 = rrs.findRuntimeRule("S1")
        val B = rrs.findRuntimeRule("B")
        val Be = rrs.findRuntimeRule("Be")
        val a = rrs.findRuntimeRule("'a'")
        val b = rrs.findRuntimeRule("'b'")
        val c = rrs.findRuntimeRule("'c'")


        check_expectedAt(RP(G, oN, SOR), LookaheadSetPart.EOT, expected = setOf(a))
        check_expectedAt(RP(G, oN, ER), LookaheadSetPart.EMPTY, expected = emptySet())

        check_expectedAt(RP(S, oN, SOR), LookaheadSetPart.EOT, expected = setOf(a))
        check_expectedAt(RP(S, oN, ER), LookaheadSetPart.EOT, expected = setOf(EOT))

        check_expectedAt(RP(S, o1, SOR), LookaheadSetPart.EOT, expected = setOf(a))
        check_expectedAt(RP(S, o1, ER), LookaheadSetPart.EOT, expected = setOf(EOT))

        check_expectedAt(RP(S1, oN, SOR), LookaheadSetPart.EOT, expected = setOf(a))
        check_expectedAt(RP(S1, oN, p1), LookaheadSetPart.EOT, expected = setOf(c))

        check_expectedAt(RP(S1, oN, p2), LookaheadSetPart.EOT, expected = setOf(EOT, b))
        check_expectedAt(RP(S1, oN, EOR), LookaheadSetPart.EOT, expected = setOf(EOT))

        check_expectedAt(RP(a, oN, EOR), LookaheadSetPart.EOT, expected = setOf(EOT))
        check_expectedAt(RP(b, oN, EOR), LookaheadSetPart.EOT, expected = setOf(EOT))
        check_expectedAt(RP(c, oN, EOR), LookaheadSetPart.EOT, expected = setOf(EOT))
    }

    @Test
    fun java8_NavigableExpression() {
        // NavigableExpression
        //   = MethodReference
        //   | GenericMethodInvocation
        //   ;
        // MethodReference = MethodInvocation '::' IDENTIFIER ;
        // GenericMethodInvocation = TypeArguments? MethodInvocation ;
        // MethodInvocation = IDENTIFIER '(' Expression ')' ;
        // TypeArguments = '<>' ;
        //
        // Expression = Postfix | IDENTIFIER ;
        // Postfix = Expression '++' ;
        //
        // leaf IDENTIFIER = "[A-Za-z]+" ;
        val rrs = runtimeRuleSet {
            choice("NavigableExpression", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("MethodReference")
                ref("GenericMethodInvocation")
            }
            concatenation("MethodReference") { ref("MethodInvocation"); literal("::"); ref("IDENTIFIER") }
            concatenation("GenericMethodInvocation") { ref("optTypeArguments"); ref("MethodInvocation"); }
            concatenation("MethodInvocation") { ref("IDENTIFIER"); literal("("); ref("Expression"); literal(")") }
            multi("optTypeArguments", 0, 1, "TypeArguments")
            concatenation("TypeArguments") { literal("<>") }
            choice("Expression", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("Postfix")
                ref("IDENTIFIER")
            }
            concatenation("Postfix") { ref("Expression"); literal("++"); }
            pattern("IDENTIFIER", "[a-zA-Z]+")
        }
        val NavigableExpression = rrs.findRuntimeRule("NavigableExpression")
        val G = rrs.goalRuleFor[NavigableExpression]
        val IDENTIFIER = rrs.findTerminalRule("IDENTIFIER")
        val oc = rrs.findTerminalRule("'<>'")

        check_expectedAt(RP(G, oN, SOR), LookaheadSetPart.EOT, expected = setOf(IDENTIFIER, oc))
        check_expectedAt(RP(G, oN, EOR), LookaheadSetPart.EMPTY, expected = emptySet())

        check_expectedAt(RP(NavigableExpression, oN, SOR), LookaheadSetPart.EOT, expected = setOf(IDENTIFIER, oc))
        check_expectedAt(RP(NavigableExpression, oN, EOR), LookaheadSetPart.EOT, expected = setOf(EOT))


    }

    @Test
    fun empty() {
        // S =  'a' | E
        // E =  ;
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("a")
                ref("B")
            }
            concatenation("B") { empty() }
        }
        val S = rrs.findRuntimeRule("S")
        val G = rrs.goalRuleFor[S]
        val B = rrs.findRuntimeRule("B")
        val a = rrs.findRuntimeRule("'a'")

        // G = . S
        check_expectedAt(RP(G, oN, SR), LookaheadSetPart.EOT, expected = setOf(a, EOT))
        // G = S .
        check_expectedAt(RP(G, oN, ER), LookaheadSetPart.EOT, expected = setOf(EOT))

        // S = . 'a'
        check_expectedAt(RP(S, oN, SR), LookaheadSetPart.EOT, expected = setOf(a))
        // S = 'a' .
        check_expectedAt(RP(S, oN, ER), LookaheadSetPart.EOT, expected = setOf(EOT))

        // S = . E
        check_expectedAt(RP(S, o1, SR), LookaheadSetPart.EOT, expected = setOf(EOT))

        // S = E .
        check_expectedAt(RP(S, o1, ER), LookaheadSetPart.EOT, expected = setOf(EOT))
    }

    @Test
    fun empty2() {
        // S = PD PB
        // PD = 'p' 'q'?
        // PB = 'f' | 'b' PBC 'e'
        // PBC = PBE*
        // PBE = PM | IM
        // IM = 'i'
        // PM = MP PMc
        // PMc = DE | UE
        // DE = S
        // MP = 'v'?
        // PM = UE
        // UE = ;
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("PD"); ref("PB") }
            concatenation("PD") { literal("p"); literal("q") }
            choice("PB", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("f")
                concatenation { literal("b"); ref("PBC");literal("e") }
            }
            multi("PBC", 0, -1, "PBE")
            choice("PBE", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("PM")
                ref("IM")
            }
            concatenation("IM") { literal("i") }
            concatenation("PM") { ref("MP"); ref("PMc") }
            choice("PBE", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("DE")
                ref("UE")
            }
            concatenation("DE") { ref("S") }
            optional("MP", "'v'")
            concatenation("PM") { ref("UE") }
            concatenation("UE") { empty() }
            literal("'v'", "v")
        }
        val S = rrs.findRuntimeRule("S")
        val G = rrs.goalRuleFor[S]
        val PD = rrs.findRuntimeRule("PD")
        val PB = rrs.findRuntimeRule("PB")
        val p = rrs.findRuntimeRule("'p'")
        val f = rrs.findRuntimeRule("'f'")
        val b = rrs.findRuntimeRule("'b'")

        /*        // G = . S
                check_expectedAt(RP(G, o0, SR), LookaheadSetPart.EOT, expected = setOf(p))
                // G = S .
                check_expectedAt(RP(G, o0, ER), LookaheadSetPart.EOT, expected = setOf(EOT))

                // S = . PD PB
                check_expectedAt(RP(S, o0, SR), LookaheadSetPart.EOT, expected = setOf(p))
                // S = PD . PB
                check_expectedAt(RP(S, o0, p1), LookaheadSetPart.EOT, expected = setOf(f, b))
        */

        // PB = 'b' . PBC 'e'
        check_expectedAt(RP(PB, o1, p1), LookaheadSetPart.EOT, expected = setOf(f, b))

    }

    @Test
    fun empty3() {
        // S = E*
        // E = ;
        val rrs = runtimeRuleSet {
            multi("S", 0, -1, "E")
            concatenation("E") { empty() }
        }
        val S = rrs.findRuntimeRule("S")
        val G = rrs.goalRuleFor[S]

        // S = . E*
        check_expectedAt(RP(S, oN, SR), LookaheadSetPart.EOT, expected = setOf(EOT))

    }

}