/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.akehurst.language.parser.aMinimalVersion

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.sentence.common.SentenceDefault
import net.akehurst.language.sppt.treedata.SpptWalkerToString
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

@ExperimentalTime
class test_MinimalVersionForPaper {

    private fun test(goal: String, rrs: RuntimeRuleSet, sentences: List<String>, maxOut: Int = 1000) {
        val sut = MinimalParser.parser(goal, rrs)
        for (s in sentences) {
            sut.reset()
            println("---- '${s.substring(0, min(20, s.length))}' ----")

            val (actual, duration1) = measureTimedValue { sut.parse(s) }
            val (_, duration2) = measureTimedValue { sut.parse(s) }
            println(sut.automaton.usedAutomatonToString(true))
            assertNotNull(actual)
            println("Duration: $duration1  --  $duration2")
            val walker = SpptWalkerToString(SentenceDefault(s), "  ")
            actual.traverseTreeDepthFirst(walker, true)
            val out = walker.output
            println(out.substring(0, min(maxOut, out.length)))
        }
    }

    @Test
    fun concatenation_literal_abc() {
        // S = a b c
        test(
            "S",
            runtimeRuleSet {
                concatenation("S") { literal("a"); literal("b"); literal("c") }
            },
            listOf(
                "abc"
            )
        )
    }

    @Test
    fun bodmas_exprOpExprRules_root_choiceEqual() {
        // S = E
        // E = R | M | A
        // R = v
        // M = E * E
        // A = E + E
        test(
            "S",
            runtimeRuleSet {
                concatenation("S") { ref("E") }
                choice("E", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                    ref("R")
                    ref("M")
                    ref("A")
                }
                concatenation("R") { literal("v") }
                concatenation("M") { ref("E"); literal("*"); ref("E") }
                concatenation("A") { ref("E"); literal("+"); ref("E") }
            },
            listOf("v", "v+v", "v*v", "v+v*v", "v*v+v", "v+v*v*v+v+v")
        )
    }

    @Test
    fun hiddenLeft() {
        // S = B S 'c' | 'a'
        // B = 'b' | <empty>
        test(
            "S",
            runtimeRuleSet {
                choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                    concatenation { ref("B"); ref("S"); literal("c") }
                    concatenation { literal("a") }
                }
                choice("B", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                    literal("b")
                    ref("Be")
                }
                concatenation("Be") { empty() }
            },
            listOf(
                "a",
                "bac",
                "ac",
                "acc",
                "accc",
                "accccc",
                "bacc"
            )
        )
    }

    @Test
    fun hiddenRight() {
        // S = 'c' S B | 'a'
        // B = 'b' | <empty>
        test(
            "S",
            runtimeRuleSet {
                choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                    concatenation { literal("c"); ref("S"); ref("B") }
                    concatenation { literal("a") }
                }
                choice("B", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                    literal("b")
                    ref("Be")
                }
                concatenation("Be") { empty() }
            },
            listOf(
                "a", "cab", "ca", "cca", "ccab"
            )
        )
    }

    @Test
    fun whitespace_leftRecursive_a_aWS500() {
        test(
            "S",
            runtimeRuleSet {
                concatenation("WS", true) { pattern("\\s+") }
                choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                    literal("a")
                    concatenation { ref("S"); literal("a") }
                }
            },
            listOf(
                "a",
                " a",
                "a ",
                "a a",
                "a ".repeat(10),
                "a ".repeat(500)
            )
        )
    }

    @Test
    fun ScottJohnstone_RightNulled_1() {
        // S = abAa | aBAa | aba
        // abAa = a b A a
        // aBAa = a B A a
        // aba = a b a
        // A = a | a A
        // B = b
        test(
            "S",
            runtimeRuleSet {
                choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                    ref("abAa")
                    ref("aBAa")
                    ref("aba")
                }
                concatenation("abAa") { literal("a"); literal("b"); ref("A"); literal("a") }
                concatenation("aBAa") { literal("a"); ref("B"); ref("A"); literal("a") }
                concatenation("aba") { literal("a"); literal("b"); literal("a") }
                choice("A", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                    literal("a")
                    ref("A1")
                }
                concatenation("A1") { literal("a"); ref("A") }
                concatenation("B") { literal("b") }
            },
            listOf(
                "aba",
                "abaa",
                "abaaa",
            )
        )
    }

    @Test
    fun GTB() {
        /*
         * from [https://www.researchgate.net/publication/222194445_The_Grammar_Tool_Box_A_Case_Study_Comparing_GLR_Parsing_Algorithms]
         * The Grammar Tool Box: A Case Study Comparing GLR Parsing Algorithms, Adrian Johnstone, Elizabeth Scott, Giorgios Economopoulos
         *
         * S = 'a' | A B | A 'z' ;
         * A = 'a' ;
         * B = 'b' | <empty> ;
         *
         */
        test(
            "S",
            runtimeRuleSet {
                choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                    literal("a")
                    concatenation { ref("A"); ref("B") }
                    concatenation { ref("A"); literal("z") }
                }
                concatenation("A") { literal("a") }
                choice("B", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                    literal("b")
                    ref("be")
                }
                concatenation("be") { empty() }
            },
            listOf(
                "a",
                "az",
                "ab"
            )
        )
    }

    @Test
    fun Generalized_Bottom_Up_Parsers_With_Reduced_Stack_Activity__G2() {
        /*
         * from [https://www.researchgate.net/publication/220458273_Generalized_Bottom_Up_Parsers_With_Reduced_Stack_Activity]
         * The Generalized Bottom Up Parsers WithReduced Stack Activity, Elizabeth Scott, Adrian Johnstone
         *
         * S = S a | A ;
         * A = b A | <empty> ;
         *
         */
        test(
            "S",
            runtimeRuleSet {
                choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                    concatenation { ref("S"); literal("a") }
                    concatenation { ref("A"); }
                }
                choice("A", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                    concatenation { literal("b"); ref("A") }
                    concatenation { empty() }
                }
            },
            listOf(
                "a",
                "aa",
                "aaa",
                "ba",
                "bba",
                "bbba",
                "bbaa",
                "bbbaaa"
            )
        )
    }

    @Test
    fun Generalized_Bottom_Up_Parsers_With_Reduced_Stack_Activity__G3() {
        /*
         * from [https://www.researchgate.net/publication/220458273_Generalized_Bottom_Up_Parsers_With_Reduced_Stack_Activity]
         * The Generalized Bottom Up Parsers WithReduced Stack Activity, Elizabeth Scott, Adrian Johnstone
         *
         * S = BSb | b ;
         * B = b | <empty> ;
         *
         */
        test(
            "S",
            runtimeRuleSet {
                choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                    concatenation { ref("B"); ref("S"); literal("b") }
                    concatenation { literal("b") }
                }
                choice("B", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                    concatenation { literal("b"); }
                    concatenation { empty() }
                }
            },
            listOf(
                "b",
                "bb",
                "bbb",
                "bbbb",
                "b".repeat(20)
            )
        )
    }

    @Test
    fun RecursiveIssue() {
        //  S = A | S
        //  A = a | a A
        test(
            "S",
            runtimeRuleSet {
                choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                    concatenation { ref("A") }
                    concatenation { ref("S"); }
                }
                choice("A", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                    literal("a")
                    concatenation { literal("a"); ref("A") }
                }
            },
            listOf(
                "a"
            )
        )
    }

    @Test
    fun Johnson_SSS() {
        // S = S S S | S S | a
        test(
            "S",
            runtimeRuleSet {
                choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                    concatenation { ref("S"); ref("S"); ref("S") }
                    concatenation { ref("S"); ref("S") }
                    literal("a")
                }
            },
            listOf(
                "a",
                "aa",
                "aaa",
                "aaaa",
                "aaaaa",
                "a".repeat(10)
            )
        )
    }

    @Test
    fun Johnson_SSS_Ambiguous() {
        // S = S S S || S S || a
        test(
            "S",
            runtimeRuleSet {
                choice("S", RuntimeRuleChoiceKind.AMBIGUOUS) {
                    concatenation { ref("S"); ref("S"); ref("S") }
                    concatenation { ref("S"); ref("S") }
                    literal("a")
                }
            },
            listOf(
                "a",
                "aa",
                "aaa",
                "aaaa",
                "aaaaa",
                "a".repeat(10)
            )
        )
    }

    @Test
    fun G1() {
        //  S = A B | E S B
        //  A = a | a A
        //  B = b | E
        //  E = e | <e>
        test(
            "S",
            runtimeRuleSet {
                choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                    concatenation { ref("A"); ref("B") }
                    concatenation { ref("E"); ref("S"); ref("B") }
                }
                choice("A", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                    literal("a")
                    concatenation { literal("a"); ref("A") }
                }
                choice("B", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                    concatenation { literal("b"); }
                    concatenation { ref("E") }
                }
                choice("E", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                    concatenation { literal("e"); }
                    concatenation { empty() }
                }
            },
            listOf(
                "a",
                "aa",
                "aaa",
                "ab",
                "aab",
                "aaab",
                "ae",
                "aae",
                "aaae"
            )
        )
    }

    @Test
    fun G2_right_recursive() {
        //  S = a | a S
        test(
            "S",
            runtimeRuleSet {
                choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                    literal("a")
                    concatenation { literal("a"); ref("S") }
                }
            },
            listOf(
                "a",
                "aa",
                "aaa",
                "aaaa",
            )
        )
    }

    @Test
    fun G3_right_recursive_with_empty() {
        //  S = <e> | S a
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                concatenation { empty() }
                concatenation { literal("a"); ref("S"); }
            }
        }
        //println(rrs.fullAutomatonToString("S", AutomatonKind.LOOKAHEAD_1))

        test(
            "S",
            rrs,
            listOf(
                "",
                "a",
                "aa",
                "aaa",
                "aaaa",
            )
        )


    }

    @Test
    fun G4_LR1() {
        //  S = <e> | S a | S b
        test(
            "S",
            runtimeRuleSet {
                choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                    concatenation { empty() }
                    concatenation { ref("S"); literal("a") }
                    concatenation { ref("S"); literal("b") }
                }
            },
            listOf(
                "b",
                "a",
                "aa",
                "ab",
                "ba",
                "bb",
                "aaa",
                "aab",
                "aba",
                "abb",
                "baa",
                "bab",
                "bba",
                "bbb",
                "aaaa",
                "aaab",
                "aaba",
                "aabb",
                "abaa",
                "abab",
                "abba",
                "abbb",
                "baaa",
                "baab",
                "baba",
                "babb",
                "bbaa",
                "bbab",
                "bbba",
                "bbbb",
            )
        )
    }

    @Test
    fun Emebdded_Rrec() {
        //  S = <e> | S a
        val emb = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                concatenation { empty() }
                concatenation { literal("a"); ref("S"); }
            }
        }
        // S = d | B S
        // B = b I::S b | c I::S c
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                concatenation { literal("d") }
                concatenation { ref("B"); ref("S") }
            }
            choice("B", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                concatenation { literal("b"); ref("E"); literal("b") }
                concatenation { literal("c"); ref("E"); literal("c") }
            }
            embedded("E", emb, "S")
        }

        val sentences = listOf(
            "d",
            "babd",
            "cacd",
            "baaaabd",
            "caaaacd",
            "babcacd",
            "baaaabcaaaacd",
            "caaaacbaaaabd",
        )

        test(
            "S",
            rrs,
            sentences,
            1000
        )
    }

    @Test
    fun Emebdded_Lrec() {
        //  S = <e> | a S
        val emb = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                concatenation { empty() }
                concatenation { literal("a"); ref("S"); }
            }
        }
        // S = B | S B
        // B = b I::S b | c I::S c
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                concatenation { ref("B") }
                concatenation { ref("S"); ref("B") }
            }
            choice("B", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                concatenation { literal("b"); ref("E"); literal("b") }
                concatenation { literal("c"); ref("E"); literal("c") }
            }
            embedded("E", emb, "S")
        }

        val sentences = listOf(
            //"bab",
            //"cac",
            "babbabbab",
            "baaaab",
            "caaaac",
            "babcac",
            "babbabbab",
            "caccaccac",
            "baaaabcaaaac",
            "caaaacbaaaab",
        )

        test(
            "S",
            rrs,
            sentences,
            1000
        )
    }

}