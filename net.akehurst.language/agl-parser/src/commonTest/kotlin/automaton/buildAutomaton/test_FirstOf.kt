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
import kotlin.test.Ignore
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
        val rG = rrs.goalRuleFor[S]
        val a = rrs.findRuntimeRule("'a'")
        val b = rrs.findRuntimeRule("'b'")

        check_expectedAt(RP(rG, oN, SR), LookaheadSetPart.EOT, expected = setOf(EOT, a))
        check_expectedAt(RP(S, oSI, SR), LookaheadSetPart.EOT, expected = setOf(a))
        check_expectedAt(RP(S, oSE, SR), LookaheadSetPart.EOT, expected = setOf(EOT))
        check_expectedAt(RP(S, oSS, pSS), LookaheadSetPart.EOT, expected = setOf(b))
        check_expectedAt(RP(S, oSI, pSI), LookaheadSetPart.EOT, expected = setOf(a))
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
        val rG = rrs.goalRuleFor[S]
        val S1 = rrs.findRuntimeRule("S1")
        val a = rrs.findRuntimeRule("'a'")

        check_expectedAt(RP(rG, oN, SOR), LookaheadSetPart.EOT, expected = setOf(a))
        check_expectedAt(RP(rG, oN, ER), LookaheadSetPart.EMPTY, expected = emptySet())

        check_expectedAt(RP(S, o0, SOR), LookaheadSetPart.EOT, expected = setOf(a))
        check_expectedAt(RP(S, o0, ER), LookaheadSetPart.EOT, expected = setOf(EOT))

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
        val rG = rrs.goalRuleFor[S]
        val a = rrs.findRuntimeRule("'a'")

        check_expectedAt(RP(rG, oN, SOR), LookaheadSetPart.EOT, expected = setOf(a))
        check_expectedAt(RP(rG, oN, EOR), LookaheadSetPart.EMPTY, expected = emptySet())

        check_expectedAt(RP(S, o0, SOR), LookaheadSetPart.EOT, expected = setOf(a))
        check_expectedAt(RP(S, o0, EOR), LookaheadSetPart.EOT, expected = setOf(EOT))

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
        val rG = rrs.goalRuleFor[S]
        val S1 = rrs.findRuntimeRule("S1")
        val B = rrs.findRuntimeRule("B")
        val Be = rrs.findRuntimeRule("Be")
        val a = rrs.findRuntimeRule("'a'")
        val b = rrs.findRuntimeRule("'b'")
        val c = rrs.findRuntimeRule("'c'")


        check_expectedAt(RP(rG, oN, SOR), LookaheadSetPart.EOT, expected = setOf(a))
        check_expectedAt(RP(rG, oN, ER), LookaheadSetPart.EMPTY, expected = emptySet())

        check_expectedAt(RP(S, o0, SOR), LookaheadSetPart.EOT, expected = setOf(a))
        check_expectedAt(RP(S, o0, ER), LookaheadSetPart.EOT, expected = setOf(EOT))

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
        val rG = rrs.goalRuleFor[NavigableExpression]
        val IDENTIFIER = rrs.findTerminalRule("IDENTIFIER")
        val oc = rrs.findTerminalRule("'<>'")

        check_expectedAt(RP(rG, oN, SOR), LookaheadSetPart.EOT, expected = setOf(IDENTIFIER, oc))
        check_expectedAt(RP(rG, oN, EOR), LookaheadSetPart.EMPTY, expected = emptySet())

        check_expectedAt(RP(NavigableExpression, o0, SOR), LookaheadSetPart.EOT, expected = setOf(IDENTIFIER))
        check_expectedAt(RP(NavigableExpression, o1, SOR), LookaheadSetPart.EOT, expected = setOf(IDENTIFIER, oc))
        check_expectedAt(RP(NavigableExpression, o0, EOR), LookaheadSetPart.EOT, expected = setOf(EOT))
        check_expectedAt(RP(NavigableExpression, o1, EOR), LookaheadSetPart.EOT, expected = setOf(EOT))


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
        val rG = rrs.goalRuleFor[S]
        val B = rrs.findRuntimeRule("B")
        val a = rrs.findRuntimeRule("'a'")

        // G = . S
        check_expectedAt(RP(rG, oN, SR), LookaheadSetPart.EOT, expected = setOf(a, EOT))
        // G = S .
        check_expectedAt(RP(rG, oN, ER), LookaheadSetPart.EOT, expected = setOf(EOT))

        // S = . 'a'
        check_expectedAt(RP(S, o0, SR), LookaheadSetPart.EOT, expected = setOf(a))
        // S = 'a' .
        check_expectedAt(RP(S, o0, ER), LookaheadSetPart.EOT, expected = setOf(EOT))

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
        // PM = MP PMc         -- nullable (MP and PMc can both be empty)
        // PMc = DE | UE
        // DE = S
        // MP = 'v'?
        // UE = ;
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("PD"); ref("PB") }
            concatenation("PD") { literal("p"); ref("optQ") }
            optional("optQ", "'q'")
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
            choice("PMc", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("DE")
                ref("UE")
            }
            concatenation("DE") { ref("S") }
            optional("MP", "'v'")
            concatenation("UE") { empty() }
            literal("'q'", "q")
            literal("'v'", "v")
        }
        val S = rrs.findRuntimeRule("S")
        val rG = rrs.goalRuleFor[S]
        val PD = rrs.findRuntimeRule("PD")
        val optQ = rrs.findRuntimeRule("optQ")
        val PB = rrs.findRuntimeRule("PB")
        val PBC = rrs.findRuntimeRule("PBC")
        val PBE = rrs.findRuntimeRule("PBE")
        val IM = rrs.findRuntimeRule("IM")
        val PM = rrs.findRuntimeRule("PM")
        val PMc = rrs.findRuntimeRule("PMc")
        val DE = rrs.findRuntimeRule("DE")
        val MP = rrs.findRuntimeRule("MP")
        val UE = rrs.findRuntimeRule("UE")
        val p = rrs.findRuntimeRule("'p'")
        val q = rrs.findRuntimeRule("'q'")
        val f = rrs.findRuntimeRule("'f'")
        val b = rrs.findRuntimeRule("'b'")
        val e = rrs.findRuntimeRule("'e'")
        val i = rrs.findRuntimeRule("'i'")
        val v = rrs.findRuntimeRule("'v'")

        // G = . S
        check_expectedAt(RP(rG, oN, SR), LookaheadSetPart.EOT, expected = setOf(p))
        // G = S .
        check_expectedAt(RP(rG, oN, ER), LookaheadSetPart.EOT, expected = setOf(EOT))

        // S = . PD PB
        check_expectedAt(RP(S, oN, SR), LookaheadSetPart.EOT, expected = setOf(p))
        // S = PD . PB
        check_expectedAt(RP(S, oN, p1), LookaheadSetPart.EOT, expected = setOf(f, b))
        // S = PD PB .
        check_expectedAt(RP(S, oN, ER), LookaheadSetPart.EOT, expected = setOf(EOT))

        // PD = . 'p' 'q'?
        check_expectedAt(RP(PD, oN, SR), LookaheadSetPart.EOT, expected = setOf(p))
        // PD = 'p' . 'q'?      -- 'q'? can be empty, so also what follows PD
        check_expectedAt(RP(PD, oN, p1), LookaheadSetPart.EOT, expected = setOf(q, EOT))
        // optQ = . 'q' | . <empty>
        check_expectedAt(RP(optQ, oOI, SR), LookaheadSetPart.EOT, expected = setOf(q))
        check_expectedAt(RP(optQ, oOE, SR), LookaheadSetPart.EOT, expected = setOf(EOT))

        // PB = . 'f'
        check_expectedAt(RP(PB, o0, SR), LookaheadSetPart.EOT, expected = setOf(f))
        // PB = . 'b' PBC 'e'
        check_expectedAt(RP(PB, o1, SR), LookaheadSetPart.EOT, expected = setOf(b))
        // PB = 'b' . PBC 'e'   -- PBC can be empty, so also 'e'; PBE starts with 'v' (MP), 'p' (DE = S) or 'i' (IM)
        check_expectedAt(RP(PB, o1, p1), LookaheadSetPart.EOT, expected = setOf(v, p, i, e))
        // PB = 'b' PBC . 'e'
        check_expectedAt(RP(PB, o1, p2), LookaheadSetPart.EOT, expected = setOf(e))

        // PBC = . PBE*  (item option) -- PBE can be empty
        check_expectedAt(RP(PBC, oLI, SR), LookaheadSetPart.EOT, expected = setOf(v, p, i, EOT))
        // PBC = . <empty list>
        check_expectedAt(RP(PBC, oLE, SR), LookaheadSetPart.EOT, expected = setOf(EOT))

        // PBE = . PM            -- PM can be empty
        check_expectedAt(RP(PBE, o0, SR), LookaheadSetPart.EOT, expected = setOf(v, p, EOT))
        // PBE = . IM
        check_expectedAt(RP(PBE, o1, SR), LookaheadSetPart.EOT, expected = setOf(i))

        // IM = . 'i'
        check_expectedAt(RP(IM, oN, SR), LookaheadSetPart.EOT, expected = setOf(i))

        // PM = . MP PMc
        check_expectedAt(RP(PM, oN, SR), LookaheadSetPart.EOT, expected = setOf(v, p, EOT))
        // PM = MP . PMc
        check_expectedAt(RP(PM, oN, p1), LookaheadSetPart.EOT, expected = setOf(p, EOT))

        // PMc = . DE
        check_expectedAt(RP(PMc, o0, SR), LookaheadSetPart.EOT, expected = setOf(p))
        // PMc = . UE
        check_expectedAt(RP(PMc, o1, SR), LookaheadSetPart.EOT, expected = setOf(EOT))

        // DE = . S
        check_expectedAt(RP(DE, oN, SR), LookaheadSetPart.EOT, expected = setOf(p))

        // MP = . 'v' | . <empty>
        check_expectedAt(RP(MP, oOI, SR), LookaheadSetPart.EOT, expected = setOf(v))
        check_expectedAt(RP(MP, oOE, SR), LookaheadSetPart.EOT, expected = setOf(EOT))

        // UE = . <empty>
        check_expectedAt(RP(UE, oN, SR), LookaheadSetPart.EOT, expected = setOf(EOT))
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
        val rG = rrs.goalRuleFor[S]

        // S = . E*
        check_expectedAt(RP(S, oLI, SR), LookaheadSetPart.EOT, expected = setOf(EOT))

    }

    @Test
    fun nullableRecursion() {
        // S = 'a' N3
        // N3 = N1 N3 | <empty>
        // N1 = N3 'b'
        // the terminal 'b' is behind a nullable recursion (needs the least fixpoint)
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("a"); ref("N3") }
            choice("N3", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                concatenation { ref("N1"); ref("N3") }
                concatenation { empty() }
            }
            concatenation("N1") { ref("N3"); literal("b") }
        }
        val S = rrs.findRuntimeRule("S")
        val N3 = rrs.findRuntimeRule("N3")
        val N1 = rrs.findRuntimeRule("N1")
        val a = rrs.findRuntimeRule("'a'")
        val b = rrs.findRuntimeRule("'b'")

        check_expectedAt(RP(S, oN, SR), LookaheadSetPart.EOT, expected = setOf(a))
        check_expectedAt(RP(S, oN, p1), LookaheadSetPart.EOT, expected = setOf(b, EOT))
        check_expectedAt(RP(N3, o0, SR), LookaheadSetPart.EOT, expected = setOf(b))
        check_expectedAt(RP(N3, o0, p1), LookaheadSetPart.EOT, expected = setOf(b, EOT))
        check_expectedAt(RP(N3, o1, SR), LookaheadSetPart.EOT, expected = setOf(EOT))
        check_expectedAt(RP(N1, oN, SR), LookaheadSetPart.EOT, expected = setOf(b))
        check_expectedAt(RP(N1, oN, p1), LookaheadSetPart.EOT, expected = setOf(b))
    }

    @Test
    fun nullableSequence() {
        // S = A B C 'd'
        // A = 'a'?
        // B = 'b'?
        // C = 'c'?
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("A"); ref("B"); ref("C"); literal("d") }
            optional("A", "'a'")
            optional("B", "'b'")
            optional("C", "'c'")
            literal("'a'", "a")
            literal("'b'", "b")
            literal("'c'", "c")
        }
        val S = rrs.findRuntimeRule("S")
        val a = rrs.findRuntimeRule("'a'")
        val b = rrs.findRuntimeRule("'b'")
        val c = rrs.findRuntimeRule("'c'")
        val d = rrs.findRuntimeRule("'d'")

        check_expectedAt(RP(S, oN, SR), LookaheadSetPart.EOT, expected = setOf(a, b, c, d))
        check_expectedAt(RP(S, oN, p1), LookaheadSetPart.EOT, expected = setOf(b, c, d))
        check_expectedAt(RP(S, oN, p2), LookaheadSetPart.EOT, expected = setOf(c, d))
        check_expectedAt(RP(S, oN, p3), LookaheadSetPart.EOT, expected = setOf(d))
        check_expectedAt(RP(S, oN, ER), LookaheadSetPart.EOT, expected = setOf(EOT))
    }

    @Test
    fun allOptionsNullable() {
        // S = X 'z'
        // X = A | B
        // A = <empty>
        // B = 'b'?
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("X"); literal("z") }
            choice("X", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("A")
                ref("B")
            }
            concatenation("A") { empty() }
            optional("B", "'b'")
            literal("'b'", "b")
        }
        val S = rrs.findRuntimeRule("S")
        val X = rrs.findRuntimeRule("X")
        val b = rrs.findRuntimeRule("'b'")
        val z = rrs.findRuntimeRule("'z'")

        check_expectedAt(RP(S, oN, SR), LookaheadSetPart.EOT, expected = setOf(b, z))
        check_expectedAt(RP(X, o0, SR), LookaheadSetPart.EOT, expected = setOf(EOT))
        check_expectedAt(RP(X, o1, SR), LookaheadSetPart.EOT, expected = setOf(b, EOT))
    }

    @Test
    fun ifReachedEnd_is_used_as_given() {
        // S = 'a' B
        // B = 'b'?
        // what follows the end of the rule is the given ifReachedEnd, whatever it is
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("a"); ref("B") }
            optional("B", "'b'")
            literal("'b'", "b")
            literal("'x'", "x")
        }
        val S = rrs.findRuntimeRule("S")
        val a = rrs.findRuntimeRule("'a'")
        val b = rrs.findRuntimeRule("'b'")
        val x = rrs.findRuntimeRule("'x'")
        val lhX = LookaheadSetPart.createFromRuntimeRules(setOf(x))
        val lhRT = LookaheadSetPart.createFromRuntimeRules(setOf(RT))

        // not nullable: ifReachedEnd not used
        check_expectedAt(RP(S, oN, SR), lhX, expected = setOf(a))
        // nullable rest: ifReachedEnd added
        check_expectedAt(RP(S, oN, p1), lhX, expected = setOf(b, x))
        check_expectedAt(RP(S, oN, p1), lhRT, expected = setOf(b, RT))
        check_expectedAt(RP(S, oN, p1), LookaheadSetPart.EMPTY, expected = setOf(b))
        // at end: exactly ifReachedEnd
        check_expectedAt(RP(S, oN, ER), lhX, expected = setOf(x))
        check_expectedAt(RP(S, oN, ER), lhRT, expected = setOf(RT))
        check_expectedAt(RP(S, oN, ER), LookaheadSetPart.EMPTY, expected = emptySet())
    }

    @Test
    fun multi_1_n() {
        // S = As 'c'
        // As = 'a'+
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("As"); literal("c") }
            multi("As", 1, -1, "'a'")
            literal("'a'", "a")
        }
        val S = rrs.findRuntimeRule("S")
        val As = rrs.findRuntimeRule("As")
        val a = rrs.findRuntimeRule("'a'")
        val c = rrs.findRuntimeRule("'c'")

        check_expectedAt(RP(S, oN, SR), LookaheadSetPart.EOT, expected = setOf(a))
        check_expectedAt(RP(S, oN, p1), LookaheadSetPart.EOT, expected = setOf(c))
        check_expectedAt(RP(As, oLI, SR), LookaheadSetPart.EOT, expected = setOf(a))
        check_expectedAt(RP(As, oLI, PMI), LookaheadSetPart.EOT, expected = setOf(a))
        check_expectedAt(RP(As, oLI, ER), LookaheadSetPart.EOT, expected = setOf(EOT))
    }

    @Test
    fun multi_0_n() {
        // S = As 'c'
        // As = 'a'*
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("As"); literal("c") }
            multi("As", 0, -1, "'a'")
            literal("'a'", "a")
        }
        val S = rrs.findRuntimeRule("S")
        val As = rrs.findRuntimeRule("As")
        val a = rrs.findRuntimeRule("'a'")
        val c = rrs.findRuntimeRule("'c'")

        check_expectedAt(RP(S, oN, SR), LookaheadSetPart.EOT, expected = setOf(a, c))
        check_expectedAt(RP(S, oN, p1), LookaheadSetPart.EOT, expected = setOf(c))
        check_expectedAt(RP(As, oLI, SR), LookaheadSetPart.EOT, expected = setOf(a))
        check_expectedAt(RP(As, oLI, PMI), LookaheadSetPart.EOT, expected = setOf(a))
        check_expectedAt(RP(As, oLE, SR), LookaheadSetPart.EOT, expected = setOf(EOT))
    }

    @Test
    fun sList_1_n_in_concatenation() {
        // S = L 'c'
        // L = [ 'a' / ',' ]+
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("L"); literal("c") }
            sList("L", 1, -1, "'a'", "','")
            literal("'a'", "a")
            literal("','", ",")
        }
        val S = rrs.findRuntimeRule("S")
        val L = rrs.findRuntimeRule("L")
        val a = rrs.findRuntimeRule("'a'")
        val comma = rrs.findRuntimeRule("','")
        val c = rrs.findRuntimeRule("'c'")

        check_expectedAt(RP(S, oN, SR), LookaheadSetPart.EOT, expected = setOf(a))
        check_expectedAt(RP(S, oN, p1), LookaheadSetPart.EOT, expected = setOf(c))
        check_expectedAt(RP(L, oSI, SR), LookaheadSetPart.EOT, expected = setOf(a))
        check_expectedAt(RP(L, oSS, pSS), LookaheadSetPart.EOT, expected = setOf(comma))
        check_expectedAt(RP(L, oSI, pSI), LookaheadSetPart.EOT, expected = setOf(a))
    }

    @Test
    fun indirectLeftRecursion() {
        // S = A 'x' | 'y'
        // A = S 'z'
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                concatenation { ref("A"); literal("x") }
                literal("y")
            }
            concatenation("A") { ref("S"); literal("z") }
        }
        val S = rrs.findRuntimeRule("S")
        val rG = rrs.goalRuleFor[S]
        val A = rrs.findRuntimeRule("A")
        val x = rrs.findRuntimeRule("'x'")
        val y = rrs.findRuntimeRule("'y'")
        val z = rrs.findRuntimeRule("'z'")

        check_expectedAt(RP(rG, oN, SR), LookaheadSetPart.EOT, expected = setOf(y))
        check_expectedAt(RP(S, o0, SR), LookaheadSetPart.EOT, expected = setOf(y))
        check_expectedAt(RP(S, o0, p1), LookaheadSetPart.EOT, expected = setOf(x))
        check_expectedAt(RP(S, o1, SR), LookaheadSetPart.EOT, expected = setOf(y))
        check_expectedAt(RP(A, oN, SR), LookaheadSetPart.EOT, expected = setOf(y))
        check_expectedAt(RP(A, oN, p1), LookaheadSetPart.EOT, expected = setOf(z))
    }

    @Test
    fun hiddenLeftRecursion() {
        // S = E S 'a' | 'b'
        // E = <empty>
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                concatenation { ref("E"); ref("S"); literal("a") }
                literal("b")
            }
            concatenation("E") { empty() }
        }
        val S = rrs.findRuntimeRule("S")
        val a = rrs.findRuntimeRule("'a'")
        val b = rrs.findRuntimeRule("'b'")

        check_expectedAt(RP(S, o0, SR), LookaheadSetPart.EOT, expected = setOf(b))
        check_expectedAt(RP(S, o0, p1), LookaheadSetPart.EOT, expected = setOf(b))
        check_expectedAt(RP(S, o0, p2), LookaheadSetPart.EOT, expected = setOf(a))
    }

    @Test
    fun embedded() {
        // Inner: skip WS = '_' ; Bs = B+ ; B = 'b'
        // S = 'a' gB 'c' ; gB = Inner::Bs
        // the first of an embedded rule includes the skip terminals of the embedded grammar
        val inner = runtimeRuleSet("test.Inner") {
            concatenation("WS", true) { literal("_") }
            multi("Bs", 1, -1, "B")
            concatenation("B") { literal("b") }
        }
        val rrs = runtimeRuleSet("test.S") {
            concatenation("S") { literal("a"); ref("gB"); literal("c") }
            embedded("gB", inner, "Bs")
        }
        val S = rrs.findRuntimeRule("S")
        val a = rrs.findRuntimeRule("'a'")
        val c = rrs.findRuntimeRule("'c'")
        val ib = inner.findRuntimeRule("'b'")
        val iWS = inner.findRuntimeRule("'_'")

        check_expectedAt(RP(S, oN, SR), LookaheadSetPart.EOT, expected = setOf(a))
        check_expectedAt(RP(S, oN, p1), LookaheadSetPart.EOT, expected = setOf(ib, iWS))
        check_expectedAt(RP(S, oN, p2), LookaheadSetPart.EOT, expected = setOf(c))
    }

    @Test
    fun embedded_nullable() {
        // Inner: skip WS = '_' ; Bs = B* ; B = 'b'
        // S = 'a' gB 'c' ; gB = Inner::Bs
        val inner = runtimeRuleSet("test.Inner") {
            concatenation("WS", true) { literal("_") }
            multi("Bs", 0, -1, "B")
            concatenation("B") { literal("b") }
        }
        val rrs = runtimeRuleSet("test.S") {
            concatenation("S") { literal("a"); ref("gB"); literal("c") }
            embedded("gB", inner, "Bs")
        }
        val S = rrs.findRuntimeRule("S")
        val c = rrs.findRuntimeRule("'c'")
        val ib = inner.findRuntimeRule("'b'")
        val iWS = inner.findRuntimeRule("'_'")

        check_expectedAt(RP(S, oN, p1), LookaheadSetPart.EOT, expected = setOf(ib, iWS, c))
    }

    // TODO(David): FirstOf treats a nullable embedded rule as making the whole enclosing rule nullable
    //  (firstOfNotEmptySafe and firstOfRpNotEmpty set needsNext instead of going on to the next item),
    //  so S = X 'd' ; X = gB 'c' gives {b, _, d} at the start of S instead of {b, _, c}.
    @Ignore
    @Test
    fun embedded_nullable_inside_a_rule() {
        // Inner: skip WS = '_' ; Bs = B* ; B = 'b'
        // S = X 'd' ; X = gB 'c' ; gB = Inner::Bs
        // X is not nullable ('c' is required), so 'd' can not follow the start of S
        val inner = runtimeRuleSet("test.Inner") {
            concatenation("WS", true) { literal("_") }
            multi("Bs", 0, -1, "B")
            concatenation("B") { literal("b") }
        }
        val rrs = runtimeRuleSet("test.S") {
            concatenation("S") { ref("X"); literal("d") }
            concatenation("X") { ref("gB"); literal("c") }
            embedded("gB", inner, "Bs")
        }
        val S = rrs.findRuntimeRule("S")
        val X = rrs.findRuntimeRule("X")
        val c = rrs.findRuntimeRule("'c'")
        val ib = inner.findRuntimeRule("'b'")
        val iWS = inner.findRuntimeRule("'_'")

        check_expectedAt(RP(X, oN, SR), LookaheadSetPart.EOT, expected = setOf(ib, iWS, c))
        check_expectedAt(RP(S, oN, SR), LookaheadSetPart.EOT, expected = setOf(ib, iWS, c))
    }
}
