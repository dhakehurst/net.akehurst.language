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

package net.akehurst.language.agl.aMinimalVersion

import net.akehurst.language.agl.agl.sppt.SpptWalkerToString
import net.akehurst.language.agl.grammar.grammar.AglGrammarSemanticAnalyser
import net.akehurst.language.agl.grammar.grammar.ConverterToRuntimeRules
import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
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
            val walker = SpptWalkerToString(s, "  ")
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

    @Test
    fun embedded_dot() {
        val grammarStr = """
namespace net.akehurst.language.example.dot

grammar Xml {

	skip COMMENT = "<!-- [.]* -->" ;

	file = element? ;

	element = elementEmpty | elementContent ;
	elementEmpty = '<' WS? NAME WS? attribute* '/>' ;
    elementContent = startTag content endTag ;
	startTag = '<' WS? NAME WS? attribute* '>' ;
	endTag = '</' WS? NAME WS? '>' ;

	content = (CHARDATA | element)* ;

	attribute = NAME WS? '=' WS? string WS? ;
	string = DOUBLE_QUOTE_STRING | SINGLE_QUOTE_STRING ;
	WS = "\s+" ;
	CHARDATA = "[^<]+" ;
	NAME = "[a-zA-Z][a-zA-Z0-9]*" ;
	DOUBLE_QUOTE_STRING = "\"([^\"\\]|\.)*\"" ;
	SINGLE_QUOTE_STRING = "['][^']*[']" ;
}

grammar Dot  {

    skip leaf WHITESPACE = "\s+" ;
	skip leaf MULTI_LINE_COMMENT = "/\*[^*]*\*+([^*/][^*]*\*+)*/" ;
	skip leaf SINGLE_LINE_COMMENT = "//[^\n\r]*" ;
	skip leaf C_PREPROCESSOR = "#[^\n\r]*" ;

	graph =
	  STRICT? type ID? '{' stmt_list '}'
	;
    type = GRAPH | DIGRAPH ;

	stmt_list = stmt1 * ;
    stmt1 = stmt  ';'? ;
	stmt
	  = node_stmt
      | edge_stmt
      | attr_stmt
      | assign
      | subgraph
      ;

    assign = ID '=' ID ;

    node_stmt = node_id attr_lists? ;
    node_id = ID port? ;
    port =
        ':' ID (':' compass_pt)?
      | ':' compass_pt
      ;
    leaf compass_pt	= 'n' | 'ne' | 'e' | 'se' | 's' | 'sw' | 'w' | 'nw' | 'c' | '_' ;

    edge_stmt =	edge_list attr_lists? ;
    edge_list = [edge_end / EDGEOP ]2+ ;
    edge_end = node_id | subgraph ;
    leaf EDGEOP = '--' | '->' ;

    attr_stmt = attr_type attr_lists ;
    attr_type = GRAPH | NODE | EDGE ;
    attr_lists = attr_list+ ;
    attr_list = '[' attr_list_content ']' ;
    attr_list_content = [ attr / a_list_sep ]* ;
    attr = ID '=' ID ;
    a_list_sep = (';' | ',')? ;

    subgraph = subgraph_id? '{' stmt_list '}' ;
    subgraph_id = SUBGRAPH ID? ;


	leaf STRICT = "[Ss][Tt][Rr][Ii][Cc][Tt]";
	leaf GRAPH = "[Gg][Rr][Aa][Pp][Hh]" ;
	leaf DIGRAPH = "[Dd][Ii][Gg][Rr][Aa][Pp][Hh]" ;
	leaf SUBGRAPH = "[Ss][Uu][Bb][Gg][Rr][Aa][Pp][Hh]" ;
	leaf NODE = "[Nn][Oo][Dd][Ee]" ;
    leaf EDGE = "[Ee][Dd][Gg][Ee]" ;

	ID =
	  ALPHABETIC_ID
	| NUMERAL
	| DOUBLE_QUOTE_STRING
	| HTML
	;

	leaf ALPHABETIC_ID = "[a-zA-Z_][a-zA-Z_0-9]*" ; //"[a-zA-Z\200-\377_][a-zA-Z\200-\377_0-9]*" ;

	leaf NUMERAL = "[-+]?([0-9]+([.][0-9]+)?|([.][0-9]+))" ;
	leaf DOUBLE_QUOTE_STRING = "\"(?:[^\"\\]|\\.)*\"" ;
	HTML = '<' Xml::elementContent '>' ;
}
        """
        val grammars = Agl.registry.agl.grammar.processor!!.process(grammarStr).asm!!
        val rrs = grammars.map {
            ConverterToRuntimeRules(it).runtimeRuleSet
        }
        val sentences = listOf(
            "graph {  }",
            """
            graph {
               a
               b
            }
            """.trimIndent(),
            """
                ##"I made a program to generate dot files representing the LR(0) state graph along with computed LALR(1) lookahead for an arbitrary context-free grammar, to make the diagrams I used in this article: http://blog.lab49.com/archives/2471. The program also highlights errant nodes in red if the grammar would produce a shift/reduce or reduce/reduce conflict -- you may be able to go to http://kthielen.dnsalias.com:8082/ to produce a graph more to your liking". Contributed by Kalani Thielen.

##Command to get the layout: "dot -Gsize=10,15 -Tpng thisfile > thisfile.png"

digraph g {
  graph [fontsize=30 labelloc="t" label="" splines=true overlap=false rankdir = "LR"];
  ratio = auto;
  "state0" [ style = "filled, bold" penwidth = 5 fillcolor = "white" fontname = "Courier New" shape = "Mrecord" label =<<table border="0" cellborder="0" cellpadding="3" bgcolor="white"><tr><td bgcolor="black" align="center" colspan="2"><font color="white">State #0</font></td></tr><tr><td align="left" port="r0">&#40;0&#41; s -&gt; &bull;e ${'$'} </td></tr><tr><td align="left" port="r1">&#40;1&#41; e -&gt; &bull;l '=' r </td></tr><tr><td align="left" port="r2">&#40;2&#41; e -&gt; &bull;r </td></tr><tr><td align="left" port="r3">&#40;3&#41; l -&gt; &bull;'*' r </td></tr><tr><td align="left" port="r4">&#40;4&#41; l -&gt; &bull;'n' </td></tr><tr><td align="left" port="r5">&#40;5&#41; r -&gt; &bull;l </td></tr></table>> ];
  "state1" [ style = "filled" penwidth = 1 fillcolor = "white" fontname = "Courier New" shape = "Mrecord" label =<<table border="0" cellborder="0" cellpadding="3" bgcolor="white"><tr><td bgcolor="black" align="center" colspan="2"><font color="white">State #1</font></td></tr><tr><td align="left" port="r3">&#40;3&#41; l -&gt; &bull;'*' r </td></tr><tr><td align="left" port="r3">&#40;3&#41; l -&gt; '*' &bull;r </td></tr><tr><td align="left" port="r4">&#40;4&#41; l -&gt; &bull;'n' </td></tr><tr><td align="left" port="r5">&#40;5&#41; r -&gt; &bull;l </td></tr></table>> ];
  "state2" [ style = "filled" penwidth = 1 fillcolor = "white" fontname = "Courier New" shape = "Mrecord" label =<<table border="0" cellborder="0" cellpadding="3" bgcolor="white"><tr><td bgcolor="black" align="center" colspan="2"><font color="white">State #2</font></td></tr><tr><td align="left" port="r4">&#40;4&#41; l -&gt; 'n' &bull;</td><td bgcolor="grey" align="right">=${'$'}</td></tr></table>> ];
  "state3" [ style = "filled" penwidth = 1 fillcolor = "white" fontname = "Courier New" shape = "Mrecord" label =<<table border="0" cellborder="0" cellpadding="3" bgcolor="white"><tr><td bgcolor="black" align="center" colspan="2"><font color="white">State #3</font></td></tr><tr><td align="left" port="r5">&#40;5&#41; r -&gt; l &bull;</td><td bgcolor="grey" align="right">=${'$'}</td></tr></table>> ];
  "state4" [ style = "filled" penwidth = 1 fillcolor = "white" fontname = "Courier New" shape = "Mrecord" label =<<table border="0" cellborder="0" cellpadding="3" bgcolor="white"><tr><td bgcolor="black" align="center" colspan="2"><font color="white">State #4</font></td></tr><tr><td align="left" port="r3">&#40;3&#41; l -&gt; '*' r &bull;</td><td bgcolor="grey" align="right">=${'$'}</td></tr></table>> ];
  "state5" [ style = "filled" penwidth = 1 fillcolor = "black" fontname = "Courier New" shape = "Mrecord" label =<<table border="0" cellborder="0" cellpadding="3" bgcolor="black"><tr><td bgcolor="black" align="center" colspan="2"><font color="white">State #5</font></td></tr><tr><td align="left" port="r0"><font color="white">&#40;0&#41; s -&gt; e &bull;${'$'} </font></td></tr></table>> ];
  "state6" [ style = "filled" penwidth = 1 fillcolor = "white" fontname = "Courier New" shape = "Mrecord" label =<<table border="0" cellborder="0" cellpadding="3" bgcolor="white"><tr><td bgcolor="black" align="center" colspan="2"><font color="white">State #6</font></td></tr><tr><td align="left" port="r1">&#40;1&#41; e -&gt; l &bull;'=' r </td></tr><tr><td align="left" port="r5">&#40;5&#41; r -&gt; l &bull;</td><td bgcolor="grey" align="right">${'$'}</td></tr></table>> ];
  "state7" [ style = "filled" penwidth = 1 fillcolor = "white" fontname = "Courier New" shape = "Mrecord" label =<<table border="0" cellborder="0" cellpadding="3" bgcolor="white"><tr><td bgcolor="black" align="center" colspan="2"><font color="white">State #7</font></td></tr><tr><td align="left" port="r1">&#40;1&#41; e -&gt; l '=' &bull;r </td></tr><tr><td align="left" port="r3">&#40;3&#41; l -&gt; &bull;'*' r </td></tr><tr><td align="left" port="r4">&#40;4&#41; l -&gt; &bull;'n' </td></tr><tr><td align="left" port="r5">&#40;5&#41; r -&gt; &bull;l </td></tr></table>> ];
  "state8" [ style = "filled" penwidth = 1 fillcolor = "white" fontname = "Courier New" shape = "Mrecord" label =<<table border="0" cellborder="0" cellpadding="3" bgcolor="white"><tr><td bgcolor="black" align="center" colspan="2"><font color="white">State #8</font></td></tr><tr><td align="left" port="r1">&#40;1&#41; e -&gt; l '=' r &bull;</td><td bgcolor="grey" align="right">${'$'}</td></tr></table>> ];
  "state9" [ style = "filled" penwidth = 1 fillcolor = "white" fontname = "Courier New" shape = "Mrecord" label =<<table border="0" cellborder="0" cellpadding="3" bgcolor="white"><tr><td bgcolor="black" align="center" colspan="2"><font color="white">State #9</font></td></tr><tr><td align="left" port="r2">&#40;2&#41; e -&gt; r &bull;</td><td bgcolor="grey" align="right">${'$'}</td></tr></table>> ];
  state0 -> state5 [ penwidth = 5 fontsize = 28 fontcolor = "black" label = "e" ];
  state0 -> state6 [ penwidth = 5 fontsize = 28 fontcolor = "black" label = "l" ];
  state0 -> state9 [ penwidth = 5 fontsize = 28 fontcolor = "black" label = "r" ];
  state0 -> state1 [ penwidth = 1 fontsize = 14 fontcolor = "grey28" label = "'*'" ];
  state0 -> state2 [ penwidth = 1 fontsize = 14 fontcolor = "grey28" label = "'n'" ];
  state1 -> state1 [ penwidth = 1 fontsize = 14 fontcolor = "grey28" label = "'*'" ];
  state1 -> state4 [ penwidth = 5 fontsize = 28 fontcolor = "black" label = "r" ];
  state1 -> state2 [ penwidth = 1 fontsize = 14 fontcolor = "grey28" label = "'n'" ];
  state1 -> state3 [ penwidth = 5 fontsize = 28 fontcolor = "black" label = "l" ];
  state6 -> state7 [ penwidth = 1 fontsize = 14 fontcolor = "grey28" label = "'='" ];
  state7 -> state8 [ penwidth = 5 fontsize = 28 fontcolor = "black" label = "r" ];
  state7 -> state1 [ penwidth = 1 fontsize = 14 fontcolor = "grey28" label = "'*'" ];
  state7 -> state2 [ penwidth = 1 fontsize = 14 fontcolor = "grey28" label = "'n'" ];
  state7 -> state3 [ penwidth = 5 fontsize = 28 fontcolor = "black" label = "l" ];
}
            """

        )
        test(
            "graph",
            rrs.last(),
            sentences
        )
    }

    @Test
    fun java8() {
        val grammarStr = """
/*
 * This may be more permissive than the other ambiguous grammar
 * derived from [https://docs.oracle.com/javase/specs/jls/se8/html/jls-19.html]
 */
namespace net.akehurst.language.java8

grammar Base {
    skip leaf WHITESPACE = "\s+" ;
    skip leaf COMMENT_SINGLE_LINE = "//[^\r\n]*" ;
    skip leaf COMMENT_MULTI_LINE = "/\*[^*]*\*+([^*/][^*]*\*+)*/" ;

    leaf IDENTIFIER = JAVA_LETTER JAVA_LETTER_OR_DIGIT* ;
    leaf JAVA_LETTER_OR_DIGIT = JAVA_LETTER | "[0-9]" ;
    leaf JAVA_LETTER= UNICODE_LETTER | '${'$'}' | '_' ;
    leaf UNICODE_LETTER = "[A-Za-z]" ; //TODO: add unicode chars !

    QualifiedName = [ IDENTIFIER / '.' ]+ ;

}

grammar Literals {
    Literal
      = INTEGER_LITERAL
      | FLOATING_POINT_LITERAL
      | BOOLEAN_LITERAL
      | CHARACTER_LITERAL
      | STRING_LITERAL
      | NULL_LITERAL
      ;

    leaf INTEGER_LITERAL
      = HEX_NUMERAL
      | OCT_NUMERAL
      | BINARY_NUMERAL
      | DECIMAL_NUMERAL
      ;

    leaf DECIMAL_NUMERAL = "(0|[1-9]([0-9_]*[0-9])?)" INTEGER_TYPE_SUFFIX? ;
    leaf HEX_NUMERAL     = "0[xX][0-9a-fA-F]([0-9a-fA-F_]*[0-9a-fA-F])?" INTEGER_TYPE_SUFFIX? ;
    leaf OCT_NUMERAL     = "0_*[0-7]([0-7_]*[0-7])?" INTEGER_TYPE_SUFFIX? ;
    leaf BINARY_NUMERAL  = "0[bB][01]([01_]*[01])?" INTEGER_TYPE_SUFFIX? ;

    leaf INTEGER_TYPE_SUFFIX = 'l' | 'L' ;

    leaf FLOATING_POINT_LITERAL
     = "[0-9]+" "[fdFD]"
     | "[0-9]+[eE][+-]?[0-9]+" "[fdFD]"?
     | "[0-9]*[.][0-9]*" "[eE][+-]?(0|[1-9])+"? "[fdFD]"?
     ;

    leaf BOOLEAN_LITERAL   = 'true' | 'false' ;
    leaf CHARACTER_LITERAL = "'" ("[^'\r\n\\]" | ESCAPE_SEQUENCE) "'" ;
    leaf ESCAPE_SEQUENCE
        = '\\' "[btnfr\x27\\]"
        | '\\' ("[0-3]"? "[0-7]")? "[0-7]"
        | '\\' 'u'+ "[0-9a-fA-F]{4}"
        ;
    leaf STRING_LITERAL    = "\"([^\"\\]|\\.)*\"" ;
    leaf NULL_LITERAL      = 'null' ;
}

grammar Annotations extends Base, Literals {
    Annotation = '@' QualifiedName AnnotationArgs? ;
    AnnotationArgs = '(' (ElementValue | ElementValuePairList) ')' ;
    ElementValuePairList = [ ElementValuePair / ',' ]* ;
    ElementValuePair = IDENTIFIER '=' ElementValue ;
    // overridden in expressions
    ElementValue = Literal | ElementValueArrayInitializer | Annotation ;
    ElementValueArrayInitializer = '{' ElementValueList ','? '}' ;
    ElementValueList = [ ElementValue / ',' ]* ;
}

grammar Types extends Annotations {

    TypeReference = TypeReferenceNonArray Dims? ;
    Dims = (Annotation* '[' ']')+ ;
    TypeReferenceNonArray = QualifiedTypeReference | PrimitiveTypeReference ;

    PrimitiveTypeReference = PRIMITIVE_TYPE_LITERAL ;
    QualifiedTypeReference = UnannClassifierTypeReference ( '.' [ ClassifierTypeReference / '.' ]+)? ;
    UnannClassifierTypeReference = IDENTIFIER TypeArguments? ;
    ClassifierTypeReference = Annotation* UnannClassifierTypeReference ;

    TypeArguments = '<' TypeArgumentList '>' ;
    TypeArgumentList = [ TypeArgument / ',']* ;
    TypeArgument = TypeReference | Wildcard ;
    Wildcard = Annotation* '?' WildcardBounds? ;
    WildcardBounds = ('extends' | 'super') TypeReference ;

    leaf PRIMITIVE_TYPE_LITERAL = NumericType | 'boolean' ;
    leaf NumericType = IntegralType | FloatingPointType ;
    leaf IntegralType = 'byte' | 'short' | 'int' | 'long' | 'char' ;
    leaf FloatingPointType = 'float' | 'double' ;
}

grammar Expressions extends Types {

    // from Annotations
    override ElementValue = Expression | ElementValueArrayInitializer | Annotation ;

    Expression
     = MethodReference
     | LambdaExpression
     | Assignment
     | TernaryIf
     | InfixLogicalOr
     | InfixLogicalAnd
     | InfixBitwiseOr
     | InfixBitwiseXor
     | InfixBitwiseAnd
     | InfixEquality
     | InstanceOf
     | InfixRelational
     | InfixBitShift
     | InfixAdditive
     | InfixMultiplicative
     | Prefix
     | Postfix
     | CastExpression
     | InstanceCreation
     | MethodInvocation
     | ArrayAccess
     | Navigation
     | Primary
     ;

    Navigation = Expression '.' Navigations ;
    Navigations = [ NavigableExpression  / '.' ]+ ;
    NavigableExpression
      = MethodInvocation
      | 'super'
      | InstanceCreation
      | 'this'
      | IDENTIFIER
      ;

    Primary
      = IDENTIFIER
      | Literal
      | 'this'
      | 'super'
      | GroupedExpression
      ;
    leaf DOT_CLASS = '.' WHITESPACE 'class' ;

    MethodReference = Expression '::' TypeArguments? IDENTIFIER ;

    LambdaExpression = LambdaParameters '->' LambdaBody ;
    LambdaParameters
      = IDENTIFIER
      | '(' FormalParameterList? ')'
      | '(' InferredFormalParameterList ')'
      ;
    InferredFormalParameterList = [IDENTIFIER / ',']+ ;

    Assignment = [ Expression / ASSIGNMENT_OPERATOR ]2+ ;
    leaf ASSIGNMENT_OPERATOR
      = '=' | '+=' | '-=' | '*=' | '/=' | '%='
      | '&=' | '|=' | '^='
      | '>>=' | '>>>=' | '<<='
      ;

    TernaryIf = Expression '?' Expression ':' Expression ;

    InfixLogicalOr = [ Expression / '||' ]2+ ;
    InfixLogicalAnd = [ Expression / '&&' ]2+ ;

    InfixBitwiseOr = [ Expression / '|' ]2+ ;
    InfixBitwiseXor = [ Expression / '^' ]2+ ;
    InfixBitwiseAnd = [ Expression / '&' ]2+ ;

    InfixEquality = [ Expression / INFIX_OPERATOR_EQUALITY ]2+ ;

    leaf INFIX_OPERATOR_EQUALITY = '==' | '!=' ;

    InstanceOf = Expression 'instanceOf' TypeReference ;

    InfixRelational = [ Expression / INFIX_OPERATOR_RELATIONAL ]2+ ;
    leaf INFIX_OPERATOR_RELATIONAL = '<=' | '>=' | '<' | '>' ;

    InfixBitShift = [ Expression / INFIX_OPERATOR_BIT_SHIFT ]2+ ;
    leaf INFIX_OPERATOR_BIT_SHIFT = '>>>' | '>>' | '<<' ;

    InfixAdditive = [ Expression / INFIX_OPERATOR_ADDITIVE ]2+ ;
    leaf INFIX_OPERATOR_ADDITIVE = '+' | '-' ;

    InfixMultiplicative = [ Expression / INFIX_OPERATOR_MULTIPLICATIVE ]2+ ;
    leaf INFIX_OPERATOR_MULTIPLICATIVE = '*' | '/' | '%' ;

    Prefix = PREFIX_OPERATOR Expression ;
    leaf PREFIX_OPERATOR = '++' | '--' | '+' | '-' | '!' | '~' ;

    Postfix = Expression POSTFIX_OPERATOR ;
    leaf POSTFIX_OPERATOR = '++' | '--' ;

    CastExpression = '(' TypeReference ')' Expression ;

    InstanceCreation = 'new' TypeArguments? ClassOrInterfaceTypeToInstantiate ArgumentList ClassBody? ;
    // overridden in Classes
    ClassBody = '{' '}' ;
    ClassOrInterfaceTypeToInstantiate = Annotation* IDENTIFIER ('.' Annotation* IDENTIFIER)* TypeArgumentsOrDiamond? ;

    MethodInvocation = TypeArguments? IDENTIFIER ArgumentList ;
    ArgumentList = '(' Arguments ')' ;
    Arguments = [ Expression / ',' ]* ;

    ClassLiteral
      = QualifiedName ('[' ']')* '.' 'class'
      | NumericType ('[' ']')* '.' 'class'
      | 'boolean' ('[' ']')* '.' 'class'
      | 'void' '.' 'class'
      ;

    GroupedExpression = '(' Expression ')' ;

    TypeArgumentsOrDiamond = TypeArguments | '<>' ;

    ArrayAccess = Expression '[' Expression ']' ;

    ArrayCreationExpression
      = 'new' TypeReferenceNonArray DimExprs Dims?
      | 'new' TypeReferenceNonArray Dims ArrayInitializer
      ;

    ArrayInitializer = '{' VariableInitializerList ','? '}' ;
    VariableInitializerList = [ VariableInitializer / ',' ]* ;
    VariableInitializer = Expression | ArrayInitializer ;

    DimExprs = DimExpr DimExpr* ;
    DimExpr = Annotation* '[' Expression ']' ;

    FormalParameterList = ReceiverParameter? FormalParameters VarargsParameter? ;
    FormalParameters = [FormalParameter / ',']* ;
    FormalParameter = VariableModifier* TypeReference VariableDeclaratorId ;
    VarargsParameter = VariableModifier* TypeReference Annotation* '...' VariableDeclaratorId ;
    ReceiverParameter = Annotation* TypeReference (IDENTIFIER '.')? 'this' ;

    VariableModifier = Annotation | 'final' ;
    VariableDeclaratorId = IDENTIFIER Dims? ;
    VariableDeclaratorList = [ VariableDeclarator / ',' ]+ ;
    VariableDeclarator = VariableDeclaratorId ('=' VariableInitializer)? ;

    // overridden in BlocksAndStatements
    LambdaBody = Expression ;

    ConstantExpression = Expression ;
}

grammar BlocksAndStatements extends Expressions {

    // from Expressions
    override LambdaBody = Expression | Block ;

    Block = '{' BlockStatements '}' ;
    BlockStatements = BlockStatement* ;
    // overridden in classes
    BlockStatement = LocalVariableDeclarationStatement | Statement ;
    LocalVariableDeclarationStatement = LocalVariableDeclaration ';' ;
    LocalVariableDeclaration = VariableModifier* TypeReference VariableDeclaratorList ;
    Statement
      = StatementWithoutTrailingSubstatement
      | LabeledStatement
      | IfThenStatement
      | IfThenElseStatement
      | WhileStatement
      | ForStatement
      ;
    StatementNoShortIf
      = StatementWithoutTrailingSubstatement
      | LabeledStatementNoShortIf
      | IfThenElseStatementNoShortIf
      | WhileStatementNoShortIf
      | ForStatementNoShortIf
      ;
    StatementWithoutTrailingSubstatement
      = Block
      | EmptyStatement
      | ExpressionStatement
      | AssertStatement
      | SwitchStatement
      | DoStatement
      | BreakStatement
      | ContinueStatement
      | ReturnStatement
      | SynchronizedStatement
      | ThrowStatement
      | TryStatement
      ;
    EmptyStatement = ';' ;
    LabeledStatement = IDENTIFIER ':' Statement ;
    LabeledStatementNoShortIf = IDENTIFIER ':' StatementNoShortIf ;
    ExpressionStatement = StatementExpression ';' ;
    StatementExpression
      = Expression
      ;
    IfThenStatement = 'if' '(' Expression ')' Statement ;
    IfThenElseStatement = 'if' '(' Expression ')' StatementNoShortIf 'else' Statement ;
    IfThenElseStatementNoShortIf = 'if' '(' Expression ')' StatementNoShortIf 'else' StatementNoShortIf ;
    AssertStatement
     = 'assert' Expression (':' Expression)? ';'
     ;
    SwitchStatement = 'switch' '(' Expression ')' SwitchBlock ;
    SwitchBlock = '{' SwitchBlockStatementGroup* SwitchLabel* '}' ;
    SwitchBlockStatementGroup = SwitchLabels BlockStatements ;
    SwitchLabels = SwitchLabel+ ;
    SwitchLabel
      = 'case' ConstantExpression ':'
      | 'case' EnumConstantName ':'
      | 'default' ':'
      ;
    EnumConstantName = IDENTIFIER ;
    WhileStatement = 'while' '(' Expression ')' Statement ;
    WhileStatementNoShortIf = 'while' '(' Expression ')' StatementNoShortIf ;
    DoStatement = 'do' Statement 'while' '(' Expression ')' ';' ;
    ForStatement = BasicForStatement | EnhancedForStatement ;
    ForStatementNoShortIf = BasicForStatementNoShortIf | EnhancedForStatementNoShortIf ;
    BasicForStatement = 'for' '(' ForInit? ';' Expression? ';' ForUpdate? ')' Statement ;
    BasicForStatementNoShortIf = 'for' '(' ForInit? ';' Expression? ';' ForUpdate? ')' StatementNoShortIf ;
    ForInit = StatementExpressionList | LocalVariableDeclaration ;
    ForUpdate = StatementExpressionList ;
    StatementExpressionList = [StatementExpression /',' ]+ ;
    EnhancedForStatement = 'for' '(' VariableModifier* TypeReference VariableDeclaratorId ':' Expression ')' Statement ;
    EnhancedForStatementNoShortIf = 'for' '(' VariableModifier* TypeReference VariableDeclaratorId ':' Expression ')' StatementNoShortIf ;
    BreakStatement = 'break' IDENTIFIER? ';' ;
    ContinueStatement = 'continue' IDENTIFIER? ';' ;
    ReturnStatement = 'return' Expression? ';' ;
    ThrowStatement = 'throw' Expression ';' ;
    SynchronizedStatement = 'synchronized' '(' Expression ')' Block ;
    TryStatement
      = 'try' Block Catches
      | 'try' Block Catches? Finally
      | TryWithResourcesStatement
      ;
    Catches = CatchClause CatchClause* ;
    CatchClause = 'catch' '(' CatchFormalParameter ')' Block ;
    CatchFormalParameter = VariableModifier* CatchType VariableDeclaratorId ;
    CatchType = [TypeReference / '|']+ ;
    Finally = 'finally' Block ;
    TryWithResourcesStatement = 'try' ResourceSpecification Block Catches? Finally? ;
    ResourceSpecification = '(' ResourceList ';'? ')' ;
    ResourceList = Resource (';' Resource)* ;
    Resource = VariableModifier* TypeReference VariableDeclaratorId '=' Expression ;
}

grammar Classes extends BlocksAndStatements {

    // from BlocksAndStatements
    override BlockStatement = LocalVariableDeclarationStatement | ClassDeclaration | Statement ;
    // from Expressions
    override ClassBody = '{' ClassBodyDeclaration* '}' ;

    ClassDeclaration = NormalClassDeclaration | EnumDeclaration ;
    NormalClassDeclaration = ClassModifier* 'class' IDENTIFIER TypeParameters? Superclass? Superinterfaces? ClassBody ;
    ClassModifier = Annotation | CLASS_MODIFIER ;
    leaf CLASS_MODIFIER = 'public' | 'protected' | 'private' | 'abstract' | 'static' | 'final' | 'strictfp' ;
    TypeParameters = '<' TypeParameterList '>' ;
    TypeParameterList = [ TypeParameter / ',' ]+ ;
    Superclass = 'extends' QualifiedTypeReference ;
    Superinterfaces = 'implements' InterfaceTypeList ;
    InterfaceTypeList = [ QualifiedTypeReference / ',' ]+ ;

    TypeParameter = Annotation* IDENTIFIER TypeBound? ;
    TypeBound = 'extends' QualifiedTypeReference AdditionalBound? ;
    AdditionalBound = '&' QualifiedTypeReference ;

    ClassBodyDeclaration
       = ClassMemberDeclaration
       | InstanceInitializer
       | StaticInitializer
       | ConstructorDeclaration
       ;
    // overridden in Interfaces
    ClassMemberDeclaration
       = FieldDeclaration
       | MethodDeclaration
       | ClassDeclaration
       | ';'
       ;
    FieldDeclaration = FieldModifier* TypeReference VariableDeclaratorList ';' ;
    FieldModifier = Annotation | FIELD_MODIFIER ;
    leaf FIELD_MODIFIER = 'public' | 'protected' | 'private' | 'static' | 'final' | 'transient' | 'volatile' ;

    MethodDeclaration = MethodModifier* MethodHeader MethodBody ;
    MethodModifier  = Annotation | METHOD_MODIFIER ;
    leaf METHOD_MODIFIER = 'public' | 'protected' | 'private' | 'abstract'
      | 'static' | 'final' | 'synchronized' | 'native' | 'strictfp'
      ;
    MethodHeader
      = Result MethodDeclarator Throws?
      | TypeParameters Annotation* Result MethodDeclarator Throws?
      ;
    Result = TypeReference | 'void' ;
    MethodDeclarator = IDENTIFIER '(' FormalParameterList? ')' Dims? ;

    Throws = 'throws' ExceptionTypeList ;
    ExceptionTypeList = [ ExceptionType / ',' ]+ ;
    ExceptionType = QualifiedTypeReference ;
    MethodBody = Block | ';' ;
    InstanceInitializer = Block ;
    StaticInitializer = 'static' Block ;
    ConstructorDeclaration = ConstructorModifier* ConstructorDeclarator Throws? ConstructorBody ;
    ConstructorModifier = Annotation | CONSTRUCTOR_MODIFIER ;
    leaf CONSTRUCTOR_MODIFIER = 'public' | 'protected' | 'private' ;
    ConstructorDeclarator = TypeParameters? SimpleTypeName '(' FormalParameterList? ')' ;
    SimpleTypeName = IDENTIFIER ;
    ConstructorBody = '{' ExplicitConstructorInvocation? BlockStatements? '}' ;
    ExplicitConstructorInvocation
      = TypeArguments? 'this'  ArgumentList  ';'
      | TypeArguments? 'super'  ArgumentList  ';'
      | QualifiedName '.' TypeArguments? 'super'  ArgumentList  ';'
      | Primary '.' TypeArguments? 'super'  ArgumentList  ';'
      ;
    EnumDeclaration = ClassModifier* 'enum' IDENTIFIER Superinterfaces? EnumBody ;
    EnumBody = '{' EnumConstantList? ','? EnumBodyDeclarations? '}' ;
    EnumConstantList = [ EnumConstant / ',' ]+ ;
    EnumConstant = EnumConstantModifier* IDENTIFIER ArgumentList? ClassBody? ;
    EnumConstantModifier = Annotation ;
    EnumBodyDeclarations = ';' ClassBodyDeclaration* ;
}

grammar Interfaces extends Classes {
    // from Classes
    override ClassMemberDeclaration
                = FieldDeclaration
                | MethodDeclaration
                | ClassDeclaration
                | InterfaceDeclaration
                | ';'
                ;

    InterfaceDeclaration = NormalInterfaceDeclaration | AnnotationTypeDeclaration ;
    NormalInterfaceDeclaration = InterfaceModifier* 'interface' IDENTIFIER TypeParameters? ExtendsInterfaces? InterfaceBody ;
    InterfaceModifier = Annotation | INTERFACE_MODIFIER ;
    leaf INTERFACE_MODIFIER = 'public' | 'protected' | 'private' | 'abstract' | 'static' | 'strictfp' ;
    ExtendsInterfaces = 'extends' InterfaceTypeList ;
    InterfaceBody = '{' InterfaceMemberDeclaration* '}' ;
    InterfaceMemberDeclaration
      = ConstantDeclaration
      | InterfaceMethodDeclaration
      | ClassDeclaration
      | InterfaceDeclaration
      | ';'
      ;
    ConstantDeclaration = ConstantModifier* TypeReference VariableDeclaratorList ;
    ConstantModifier = Annotation | CONSTANT_MODIFIER ;
    leaf CONSTANT_MODIFIER = 'public' | 'static' | 'final' ;
    InterfaceMethodDeclaration = InterfaceMethodModifier* MethodHeader MethodBody ;
    InterfaceMethodModifier =  Annotation | 'public' | 'abstract' | 'default' | 'static' | 'strictfp' ;
    AnnotationTypeDeclaration = InterfaceModifier* '@' 'interface' IDENTIFIER AnnotationTypeBody ;
    AnnotationTypeBody = '{' AnnotationTypeMemberDeclaration* '}' ;
    AnnotationTypeMemberDeclaration
      = AnnotationTypeElementDeclaration
      | ConstantDeclaration
      | ClassDeclaration
      | InterfaceDeclaration
      | ';'
      ;
    AnnotationTypeElementDeclaration = AnnotationTypeElementModifier* TypeReference IDENTIFIER '(' ')' Dims? DefaultValue? ';' ;
    AnnotationTypeElementModifier = Annotation | ANNOTATION_MODIFIER ;
    leaf ANNOTATION_MODIFIER = 'public' | 'abstract' ;
    DefaultValue = 'default' ElementValue ;
}

grammar Packages extends Interfaces {
    CompilationUnit = PackageDeclaration? ImportDeclaration* TypeDeclaration* ;
    PackageDeclaration = PackageModifier* 'package' [IDENTIFIER / '.']+ ';' ;
    PackageModifier = Annotation ;
    ImportDeclaration = 'import' 'static'? QualifiedName ( '.' '*' )? ';' ;
    TypeDeclaration = ClassDeclaration | InterfaceDeclaration | ';' ;
}        """
        val grammars = Agl.registry.agl.grammar.processor!!.process(
            sentence = grammarStr,
            options = Agl.options {
                semanticAnalysis {
                    // switch off ambiguity analysis for performance
                    option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, false)
                }
            }
        ).asm!!
        val rrs = grammars.map {
            ConverterToRuntimeRules(it).runtimeRuleSet
        }
        val sentences = listOf(
            "import x; @An() interface An {  }",
            "class A { int valid = 0b0; }",
            "interface An { An[] value(); }",
            "class B {  B() {  } }",
            "classclass{voidvoid(){}}",
            "class T { void func() { getUnchecked(i++); } }",
            "class T { void func() { if (a && b) { f(); } } }",
            """
class CharBufferSpliterator implements Spliterator.OfInt {
    CharBufferSpliterator(CharBuffer buffer) {
        this(buffer, buffer.position(), buffer.limit());
    }

    CharBufferSpliterator(CharBuffer buffer, int origin, int limit) {
        assert origin <= limit;
        this.buffer = buffer;
        this.index = (origin <= limit) ? origin : limit;
        this.limit = limit;
    }

    @Override
    public OfInt trySplit() {
        int lo = index, mid = (lo + limit) >>> 1;
        return (lo >= mid)
               ? null
               : new CharBufferSpliterator(buffer, lo, index = mid);
    }

    @Override
    public void forEachRemaining(IntConsumer action) {
        if (action == null)
            throw new NullPointerException();
        CharBuffer cb = buffer;
        int i = index;
        int hi = limit;
        index = hi;
        while (i < hi) {
            action.accept(cb.getUnchecked(i++));
        }
    }

    @Override
    public boolean tryAdvance(IntConsumer action) {
        if (action == null)
            throw new NullPointerException();
        if (index >= 0 && index < limit) {
            action.accept(buffer.getUnchecked(index++));
            return true;
        }
        return false;
    }

    @Override
    public long estimateSize() {
        return (long)(limit - index);
    }

    @Override
    public int characteristics() {
        return Buffer.SPLITERATOR_CHARACTERISTICS;
    }
}
        """,
            """
/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.nio;

import java.util.Comparator;
import java.util.Spliterator;
import java.util.function.IntConsumer;

/**
 * A Spliterator.OfInt for sources that traverse and split elements
 * maintained in a CharBuffer.
 *
 * @implNote
 * The implementation is based on the code for the Array-based spliterators.
 */
class CharBufferSpliterator implements Spliterator.OfInt {
    private final CharBuffer buffer;
    private int index;   // current index, modified on advance/split
    private final int limit;

    CharBufferSpliterator(CharBuffer buffer) {
        this(buffer, buffer.position(), buffer.limit());
    }

    CharBufferSpliterator(CharBuffer buffer, int origin, int limit) {
        assert origin <= limit;
        this.buffer = buffer;
        this.index = (origin <= limit) ? origin : limit;
        this.limit = limit;
    }

    @Override
    public OfInt trySplit() {
        int lo = index, mid = (lo + limit) >>> 1;
        return (lo >= mid)
               ? null
               : new CharBufferSpliterator(buffer, lo, index = mid);
    }

    @Override
    public void forEachRemaining(IntConsumer action) {
        if (action == null)
            throw new NullPointerException();
        CharBuffer cb = buffer;
        int i = index;
        int hi = limit;
        index = hi;
        while (i < hi) {
            action.accept(cb.getUnchecked(i++));
        }
    }

    @Override
    public boolean tryAdvance(IntConsumer action) {
        if (action == null)
            throw new NullPointerException();
        if (index >= 0 && index < limit) {
            action.accept(buffer.getUnchecked(index++));
            return true;
        }
        return false;
    }

    @Override
    public long estimateSize() {
        return (long)(limit - index);
    }

    @Override
    public int characteristics() {
        return Buffer.SPLITERATOR_CHARACTERISTICS;
    }
}
        """
        )
        test(
            "CompilationUnit",
            rrs.last(),
            sentences
        )
    }
}