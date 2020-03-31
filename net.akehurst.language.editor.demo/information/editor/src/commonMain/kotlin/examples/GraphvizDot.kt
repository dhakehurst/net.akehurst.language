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

package net.akehurst.language.editor.information.examples

import net.akehurst.language.editor.information.Example

object GraphvizDot {
    val id = "embedded-dot"
    val label = "Graphviz DOT Language (XML Embedded in DOT)"
    val sentence = """
// file and comments taken from [https://graphviz.gitlab.io/_pages/Gallery/directed/psg.html]
/*
   I made a program to generate dot files representing the LR(0) state graph along with computed LALR(1)
   lookahead for an arbitrary context-free grammar, to make the diagrams I used in this article: http://blog.lab49.com/archives/2471.
   The program also highlights errant nodes in red if the grammar would produce a shift/reduce or
   reduce/reduce conflict -- you may be able to go to http://kthielen.dnsalias.com:8082/ to produce a
   graph more to your liking". Contributed by Kalani Thielen.
*/

##Command to get the layout: "dot -Gsize=10,15 -Tpng thisfile > thisfile.png"

digraph g {
  graph [fontsize=30 labelloc="t" label="" splines=true overlap=false rankdir = "LR"];
  ratio = auto;
  "state0" [
    style = "filled, bold"
    penwidth = 5
    fillcolor = "white"
    fontname = "Courier New"
    shape = "Mrecord"
    label = <
        <table border="0" cellborder="0" cellpadding="3" bgcolor="white">
            <tr>
                <td bgcolor="black" align="center" colspan="2">
                    <font color="white">State #0</font>
                </td>
            </tr>
            <tr><td align="left" port="r0">&#40;0&#41; s -&gt; &bull;e ${'$'} </td></tr>
            <tr><td align="left" port="r1">&#40;1&#41; e -&gt; &bull;l '=' r </td></tr>
            <tr><td align="left" port="r2">&#40;2&#41; e -&gt; &bull;r </td></tr>
            <tr><td align="left" port="r3">&#40;3&#41; l -&gt; &bull;'*' r </td></tr>
            <tr><td align="left" port="r4">&#40;4&#41; l -&gt; &bull;'n' </td></tr>
            <tr><td align="left" port="r5">&#40;5&#41; r -&gt; &bull;l </td></tr>
        </table>
    > 
    ];
  "state1" [
    style = "filled" penwidth = 1 fillcolor = "white" fontname = "Courier New" shape = "Mrecord"
    label =<
        <table border="0" cellborder="0" cellpadding="3" bgcolor="white">
            <tr><td bgcolor="black" align="center" colspan="2"><font color="white">State #1</font></td></tr>
            <tr><td align="left" port="r3">&#40;3&#41; l -&gt; &bull;'*' r </td></tr>
            <tr><td align="left" port="r3">&#40;3&#41; l -&gt; '*' &bull;r </td></tr>
            <tr><td align="left" port="r4">&#40;4&#41; l -&gt; &bull;'n' </td></tr>
            <tr><td align="left" port="r5">&#40;5&#41; r -&gt; &bull;l </td></tr>
        </table>
    >
  ];
  state0 -> state1 [ penwidth = 1 fontsize = 14 fontcolor = "grey28" label = "'*'" ];
  state1 -> state1 [ penwidth = 1 fontsize = 14 fontcolor = "grey28" label = "'*'" ];
}
    """.trimIndent()
    val grammar = """
namespace net.akehurst.language.example.dot

grammar Xml {

	skip leaf COMMENT = "<!-- [.]* -->" ;

	file = element? ;

	element = elementEmpty | elementContent ;
	elementEmpty = '<' WS? NAME WS? attribute* '/>' ;
    elementContent = startTag content endTag ;
	startTag = '<' WS? NAME WS? attribute* '>' ;
	endTag = '</' WS? NAME WS? '>' ;

	content = (CHARDATA | element)* ;

	attribute = NAME WS? '=' WS? string WS? ;
	leaf string = DOUBLE_QUOTE_STRING | SINGLE_QUOTE_STRING ;
	leaf WS = "\s+" ;
	leaf CHARDATA = "[^<]+" ;
	leaf NAME = "[a-zA-Z][a-zA-Z0-9]*" ;
	leaf DOUBLE_QUOTE_STRING = "[\"][^\"]*[\"]" ;
	leaf SINGLE_QUOTE_STRING = "['][^']*[']" ;
}

grammar Dot  {

    skip leaf WHITESPACE = "\s+" ;
	skip leaf MULTI_LINE_COMMENT = "/\*[^*]*\*+(?:[^*/][^*]*\*+)*/" ;
	skip leaf SINGLE_LINE_COMMENT = "//.*?${'$'}" ;
	skip leaf C_PREPROCESSOR = "#.*?${'$'}" ;

	graph =
	  STRICT? type ID? '{' stmt_list '}'
	;
    type = GRAPH | DIGRAPH ;

	stmt_list = stmt1 * ;
    stmt1 = stmt  ';'? ;
	stmt =
	    node_stmt
      | edge_stmt
      | attr_stmt
      | ID '=' ID
      | subgraph
      ;

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
    attr_list = '[' a_list? ']' ;
    a_list = a  a_list? ;
    a = ID '=' ID a_list_sep? ;
    a_list_sep = ';' | ',' ;

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

	leaf NUMERAL = "[-+]?([0-9]*(.[0-9]+)?)" ;
	leaf DOUBLE_QUOTE_STRING = "\"(?:\\?.)*?\"" ;
	HTML = '<' Xml.elementContent '>' ;
}
    """.trimIndent()
    val style = """
        C_PREPROCESSOR {
          foreground: gray;
          font-style: italic;
        }
        SINGLE_LINE_COMMENT {
          foreground: DarkSlateGrey;
          font-style: italic;
        }
        MULTI_LINE_COMMENT {
          foreground: DarkSlateGrey;
          font-style: italic;
        }
        
        STRICT {
          foreground: purple;
          font-style: bold;
        }
        GRAPH {
          foreground: purple;
          font-style: bold;
        }
        DIGRAPH {
          foreground: purple;
          font-style: bold;
        }
        SUBGRAPH {
          foreground: purple;
          font-style: bold;
        }
        NODE {
          foreground: purple;
          font-style: bold;
        }
        EDGE {
          foreground: purple;
          font-style: bold;
        }
        ALPHABETIC_ID {
          foreground: red;
          font-style: italic;
        }
        
        HTML {
          background: LemonChiffon;
        }
        NAME {
            foreground: green;
        }
    """.trimIndent()
    val format = """
        
    """.trimIndent()

    val example = Example(id, label, sentence, grammar, style, format)

}