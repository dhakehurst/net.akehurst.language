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

import net.akehurst.language.agl.grammar.grammar.ConverterToRuntimeRules
import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test
import kotlin.test.assertNotNull

class test_MinimalVersionForPaper {

    private fun test(goal: String, rrs: RuntimeRuleSet, sentences: List<String>) {
        val sut = MinimalVersionForPaper.parser(goal, rrs)
        for (s in sentences) {
            println("---- '$s' ----")
            val actual = sut.parse(s)
            //println(sut.automaton.usedAutomatonToString())
            assertNotNull(actual)
        }
    }

    @Test
    fun concatenation_literal_abc() {
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
        test(
            "S",
            runtimeRuleSet {
                concatenation("S") { ref("expr") }
                choice("expr", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                    ref("root")
                    ref("mul")
                    ref("add")
                }
                concatenation("root") { literal("v") }
                concatenation("mul") { ref("expr"); literal("*"); ref("expr") }
                concatenation("add") { ref("expr"); literal("+"); ref("expr") }
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
                "a", "bac", "ac", "acc", "bacc"
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
                "a ".repeat(500)
            )
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
            "graph { a; b }",
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
    fun ScottJohnstone_RightNulled_1() {
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
                ""
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
                "ab",
                ""
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
                "b".repeat(2000)
            )
        )
    }

    @Test
    fun For_paper_G1() {
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
    fun For_paper_G2_right_recursive() {
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
    fun For_paper_G3_right_recursive_with_empty() {
        //  S = <e> | S a
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                concatenation { empty() }
                concatenation { literal("a"); ref("S"); }
            }
        }
        println(rrs.fullAutomatonToString("S", AutomatonKind.LOOKAHEAD_1))

        test(
            "S",
            rrs,
            listOf(
                "a",
                "aa",
                "aaa",
                "aaaa",
            )
        )


    }

    @Test
    fun For_paper_G4_LR1() {
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
}