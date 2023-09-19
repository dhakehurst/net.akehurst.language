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
package net.akehurst.language.processor.dot


import net.akehurst.language.agl.default.GrammarTypeNamespaceFromGrammar
import net.akehurst.language.agl.grammarTypeModel.grammarTypeModel
import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.agl.syntaxAnalyser.ContextSimple
import net.akehurst.language.api.asm.AsmSimple
import net.akehurst.language.api.asm.asmSimple
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.typemodel.test.TypeModelTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_Dot_SyntaxAnalyser {

    companion object {
        private val grammarStr = """
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

/*
 * There are two grammars.
 * The dot grammar contains Xml as an embedded grammar.
 */

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
        var processor: LanguageProcessor<AsmSimple, ContextSimple> = Agl.processorFromStringDefault(grammarStr).processor!!
    }

    @Test
    fun dotTypeModel() {
        val actual = processor.typeModel
        val expected = grammarTypeModel("net.akehurst.language.example.dot", "Dot", "Graph") {
            // graph = STRICT? type ID? '{' stmt_list '}' ;
            dataType("graph", "Graph") {
                propertyPrimitiveType("STRICT", "String", true, 0)
                propertyPrimitiveType("type", "String", false, 1)
                propertyPrimitiveType("ID", "String", true, 2)
                propertyListTypeOf("stmt_list", "stmt1", false, 3)
            }
            // stmt_list = stmt1 * ;
            listTypeOf("stmt_list", "Stmt1")
            // stmt1 = stmt  ';'? ;
            dataType("stmt1", "Stmt1") {
                propertyDataTypeOf("stmt", "Stmt", false, 0)
            }
            // 	stmt
            //	    = node_stmt
            //      | edge_stmt
            //      | attr_stmt
            //      | ID '=' ID
            //      | subgraph
            //      ;
            dataType("stmt", "Stmt") {
                subtypes("Node_stmt", "Edge_stmt", "Attr_stmt", "Subgraph")
            }
            // node_stmt = node_id attr_lists? ;
            dataType("node_stmt", "Node_stmt") {
                propertyDataTypeOf("node_id", "Node_id", false, 1)
                propertyDataTypeOf("attr_lists", "Attr_lists", true, 1)
            }
            // node_id = ID port? ;
            dataType("node_id", "Node_id") {
                propertyDataTypeOf("id", "Id", false, 0)
                propertyDataTypeOf("port", "Port", true, 1)
            }
            dataType("", "expressionStatement") {
                // expressionStatement = expression ;
                propertyDataTypeOf("expression", "expression", false, 0)
            }
            dataType("", "expression") {
                // expression
                //   = rootVariable
                //   | literalExpression
                //   | matrix
                //   | functionCallOrIndex
                //   | prefixExpression
                //   | infixExpression
                //   | groupExpression
                //   ;
                subtypes("rootVariable", "literalExpression", "matrix", "functionCallOrIndex", "prefixExpression", "infixExpression", "groupExpression")
            }
            dataType("", "groupExpression") {
                // groupExpression = '(' expression ')' ;
                propertyDataTypeOf("expression", "expression", false, 1)
            }
            dataType("", "functionCallOrIndex") {
                // functionCall = NAME '(' argumentList ')' ;
                propertyPrimitiveType("NAME", "String", false, 0)
                propertyListSeparatedTypeOf("argumentList", "argument", "String", false, 2)
            }
            dataType("argumentList", "argumentList") {
                // argumentList = [ argument / ',' ]* ;
                propertyListSeparatedTypeOf("argument", "argument", "String", false, 0)
            }
            dataType("argument", "argument") {
                // argument = expression | colonOperator ;
                subtypes("expression", "colonOperator")
            }
            dataType("", "prefixExpression") {
                // prefixExpression = prefixOperator expression ;
                propertyPrimitiveType("prefixOperator", "String", false, 0)
                propertyDataTypeOf("expression", "expression", false, 1)
            }
            stringTypeFor("prefixOperator")
            //elementType("prefixOperator") {
            // prefixOperator = '.\'' | '.^' | '\'' | '^' | '+' | '-' | '~' ;
            //    propertyUnnamedPrimitiveType(StringType, false, 0)
            //}
            dataType("", "infixExpression") {
                // infixExpression =  [ expression / infixOperator ]2+ ;
                propertyListSeparatedTypeOf("expression", "expression", "String", false, 0)
            }
            stringTypeFor("infixOperator")
            //elementType("infixOperator") {
            // infixOperator
            //        = '.*' | '*' | './' | '/' | '.\\' | '\\' | '+' | '-'    // arithmetic
            //        | '==' | '~=' | '>' | '>=' | '<' | '<='                 // relational
            //        | '&' | '|' | '&&' | '||' | '~'                         // logical
            //        | ':'                                                   // vector creation
            //        ;
            //    propertyUnnamedPrimitiveType(StringType, false, 0)
            //}
            dataType("", "colonOperator") {
                propertyPrimitiveType("COLON", "String", false, 0)
            }
            dataType("", "matrix") {
                // matrix = '['  [row / ';']*  ']' ; //strictly speaking ',' and ';' are operators in mscript for array concatination!
                propertyListSeparatedTypeOf("row", "row", "String", false, 1)
            }
            dataType("", "row") {
                // row = expression (','? expression)* ;
                propertyDataTypeOf("expression", "expression", false, 0)
                propertyListOfTupleType("\$group", false, 1) {
                    propertyPrimitiveType(GrammarTypeNamespaceFromGrammar.UNNAMED_PRIMITIVE_PROPERTY_NAME, "String", true, 0)
                    propertyDataTypeOf("expression", "expression", false, 1)
                }
            }
            dataType("", "literalExpression") {
                propertyPrimitiveType("literalValue", "String", false, 0)
            }
            stringTypeFor("literalValue")
            //elementType("literalValue") {
            //    literal
            //      = BOOLEAN
            //      | number
            //      | SINGLE_QUOTE_STRING
            //      | DOUBLE_QUOTE_STRING
            //      ;
            //    propertyUnnamedPrimitiveType(PrimitiveType.ANY, false, 0)
            //}
            dataType("", "rootVariable") {
                // rootVariable = NAME ;
                propertyPrimitiveType("NAME", "String", false, 0)
            }
            stringTypeFor("number")
            //elementType("number") {
            // number = INTEGER | REAL ;
            //    propertyUnnamedPrimitiveType(StringType, false, 0)
            //}
        }
        assertEquals(expected.asString(), actual?.asString())
        TypeModelTest.tmAssertEquals(expected, actual)
    }

    @Test
    fun one_node() {

        val sentence = """
            graph {
               a
            }
        """.trimIndent()

        val result = processor.process(sentence)
        val actual = result.asm?.rootElements?.firstOrNull()
        assertTrue(result.issues.isEmpty())
        assertNotNull(actual)

        val expected = asmSimple {
            element("Graph") {
                propertyString("strict", null)
                propertyString("type", "graph")
                propertyString("id", null)
                propertyListOfElement("stmt_list") {
                    element("Stmt1") {
                        propertyElementExplicitType("stmt", "Node_stmt") {
                            propertyElementExplicitType("node_id", "Node_id") {
                                propertyString("id", "a")
                                propertyString("port", null)
                            }
                            propertyString("attr_lists", null)
                        }
                    }
                }
            }
        }
        assertEquals(expected.asString(" "), result.asm?.asString(" "))
    }

    @Test
    fun one_edge() {

        val sentence = """
            graph {
               a -- b
            }
        """.trimIndent()

        val result = processor.process(sentence)
        val actual = result.asm?.rootElements?.firstOrNull()
        assertNotNull(actual)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun psg() {
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
        val result = processor.process(sentence)
        val actual = result.asm?.rootElements?.firstOrNull()
        assertNotNull(actual)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
    }

}