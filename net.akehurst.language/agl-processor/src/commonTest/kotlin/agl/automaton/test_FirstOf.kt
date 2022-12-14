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

internal class test_FirstOf : test_AutomatonUtilsAbstract() {

    val sut = FirstOf()

    private fun assert(rulePosition: RulePosition, ifReachedEnd: LookaheadSetPart, expected: Set<RuntimeRule>) {
        val actual = sut.expectedAt(rulePosition, ifReachedEnd)
        assertEquals(LookaheadSetPart.createFromRuntimeRules(expected), actual)
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

        assert(RP(G, o0, SOR), LookaheadSetPart.EOT, expected = setOf(a))
        assert(RP(G, o0, ER), LookaheadSetPart.EMPTY, expected = emptySet())

        assert(RP(S, o0, SOR), LookaheadSetPart.EOT, expected = setOf(a))
        assert(RP(S, o0, ER), LookaheadSetPart.EOT, expected = setOf(EOT))

        assert(RP(S, o1, SOR), LookaheadSetPart.EOT, expected = setOf(a))
        assert(RP(S, o1, ER), LookaheadSetPart.EOT, expected = setOf(EOT))

        assert(RP(S1, o0, SOR), LookaheadSetPart.EOT, expected = setOf(a))
        assert(RP(S1, o0, p1), LookaheadSetPart.EOT, expected = setOf(a))
        assert(RP(S1, o0, EOR), LookaheadSetPart.EOT, expected = setOf(EOT))

        assert(RP(a, o0, EOR), LookaheadSetPart.EOT, expected = setOf(EOT))
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

        assert(RP(G, o0, SOR), LookaheadSetPart.EOT, expected = setOf(a))
        assert(RP(G, o0, EOR), LookaheadSetPart.EMPTY, expected = emptySet())

        assert(RP(S, o0, SOR), LookaheadSetPart.EOT, expected = setOf(a))
        assert(RP(S, o0, EOR), LookaheadSetPart.EOT, expected = setOf(EOT))

        assert(RP(S, o1, SOR), LookaheadSetPart.EOT, expected = setOf(a))
        assert(RP(S, o1, p1), LookaheadSetPart.EOT, expected = setOf(a))
        assert(RP(S, o1, EOR), LookaheadSetPart.EOT, expected = setOf(EOT))

        assert(RP(a, o0, EOR), LookaheadSetPart.EOT, expected = setOf(EOT))
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


        assert(RP(G, o0, SOR), LookaheadSetPart.EOT, expected = setOf(a))
        assert(RP(G, o0, ER), LookaheadSetPart.EMPTY, expected = emptySet())

        assert(RP(S, o0, SOR), LookaheadSetPart.EOT, expected = setOf(a))
        assert(RP(S, o0, ER), LookaheadSetPart.EOT, expected = setOf(EOT))

        assert(RP(S, o1, SOR), LookaheadSetPart.EOT, expected = setOf(a))
        assert(RP(S, o1, ER), LookaheadSetPart.EOT, expected = setOf(EOT))

        assert(RP(S1, o0, SOR), LookaheadSetPart.EOT, expected = setOf(a))
        assert(RP(S1, o0, p1), LookaheadSetPart.EOT, expected = setOf(c))

        assert(RP(S1, o0, p2), LookaheadSetPart.EOT, expected = setOf(EOT, b))
        assert(RP(S1, o0, EOR), LookaheadSetPart.EOT, expected = setOf(EOT))

        assert(RP(a, o0, EOR), LookaheadSetPart.EOT, expected = setOf(EOT))
        assert(RP(b, o0, EOR), LookaheadSetPart.EOT, expected = setOf(EOT))
        assert(RP(c, o0, EOR), LookaheadSetPart.EOT, expected = setOf(EOT))
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
            choice("NavigableExpression",RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("MethodReference")
                ref("GenericMethodInvocation")
            }
            concatenation("MethodReference") { ref("MethodInvocation"); literal("::"); ref("IDENTIFIER") }
            concatenation("GenericMethodInvocation") { ref("optTypeArguments"); ref("MethodInvocation"); }
            concatenation("MethodInvocation") { ref("IDENTIFIER"); literal("("); ref("Expression"); literal(")")}
            multi("optTypeArguments",0,1,"TypeArguments")
            concatenation("TypeArguments") { literal("<>") }
            choice("Expression",RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
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

        assert(RP(G, o0, SOR), LookaheadSetPart.EOT, expected = setOf(IDENTIFIER,oc))
        assert(RP(G, o0, EOR), LookaheadSetPart.EMPTY, expected = emptySet())

        assert(RP(NavigableExpression, o0, SOR), LookaheadSetPart.EOT, expected = setOf(IDENTIFIER,oc))
        assert(RP(NavigableExpression, o0, EOR), LookaheadSetPart.EOT, expected = setOf(EOT))


    }

}