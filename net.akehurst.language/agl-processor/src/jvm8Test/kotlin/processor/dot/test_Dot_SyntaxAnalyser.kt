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


import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.agl.processor.test_mscript
import net.akehurst.language.agl.syntaxAnalyser.ContextSimple
import net.akehurst.language.api.asm.AsmSimple
import net.akehurst.language.api.asm.asmSimple
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.api.typeModel.StringType
import net.akehurst.language.api.typeModel.TypeModelTest
import net.akehurst.language.api.typeModel.asString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_Dot_SyntaxAnalyser {

    companion object {
        private val grammarStr = this::class.java.getResource("/dot/Dot.agl").readText()
        var processor: LanguageProcessor<AsmSimple, ContextSimple> = Agl.processorFromStringDefault(grammarStr)
    }

    @Test
    fun typeModel() {
        val actual = processor.typeModel
        val expected = net.akehurst.language.api.typeModel.typeModel("net.akehurst.language.example.dot","Dot") {
            elementType("graph") {
                // graph = STRICT? type ID? '{' stmt_list '}' ;
                propertyStringType("STRICT", true, 0)
                propertyStringType("type", false, 1)
                propertyStringType("ID", true, 2)
                propertyListTypeOf("stmt_list", "stmt1",false, 3)
            }
            elementType("statementList") {
                // statementList = [line / "\R"]* ;
                propertyListSeparatedTypeOf("line", "line", StringType, false, 0)
            }
            elementType("line") {
                // line = [statement / ';']* ';'? ;
                propertyListSeparatedTypeOf("statement", "statement", StringType, false, 0)
                propertyUnnamedStringType(true, 1)
            }
            elementType("statement") {
                // statement
                //   = conditional
                //   | assignment
                //   | expressionStatement
                //   //TODO: others
                //   ;
                subTypes("conditional", "assignment", "expressionStatement")
            }
            elementType("conditional") {
                // conditional = 'if' expression 'then' statementList 'else' statementList 'end' ;
                propertyElementTypeOf("expression", "expression", false, 1)
                propertyListSeparatedTypeOf("statementList", "line", StringType, false, 3)
                propertyListSeparatedTypeOf("statementList2", "line", StringType, false, 5)
            }
            elementType("assignment") {
                // assignment = rootVariable '=' expression ;
                propertyElementTypeOf("rootVariable", "rootVariable", false, 0)
                propertyElementTypeOf("expression", "expression", false, 2)
            }
            elementType("expressionStatement") {
                // expressionStatement = expression ;
                propertyElementTypeOf("expression", "expression", false, 0)
            }
            elementType("expression") {
                // expression
                //   = rootVariable
                //   | literalExpression
                //   | matrix
                //   | functionCallOrIndex
                //   | prefixExpression
                //   | infixExpression
                //   | groupExpression
                //   ;
                subTypes("rootVariable", "literalExpression", "matrix", "functionCallOrIndex", "prefixExpression", "infixExpression", "groupExpression")
            }
            elementType("groupExpression") {
                // groupExpression = '(' expression ')' ;
                //superType("expression")
                propertyElementTypeOf("expression", "expression", false, 1)
            }
            elementType("functionCallOrIndex") {
                // functionCall = NAME '(' argumentList ')' ;
                //superType("expression")
                propertyStringType("NAME", false, 0)
                propertyListSeparatedTypeOf("argumentList", "argument", StringType, false, 2)
            }
            elementType("argumentList") {
                // argumentList = [ argument / ',' ]* ;
                propertyListSeparatedTypeOf("argument", "argument", StringType, false, 0)
            }
            elementType("argument") {
                // argument = expression | colonOperator ;
                subTypes("expression", "colonOperator")
            }
            elementType("prefixExpression") {
                // prefixExpression = prefixOperator expression ;
                propertyStringType("prefixOperator", false, 0)
                propertyElementTypeOf("expression", "expression", false, 1)
            }
            stringTypeFor("prefixOperator")
            //elementType("prefixOperator") {
            // prefixOperator = '.\'' | '.^' | '\'' | '^' | '+' | '-' | '~' ;
            //    propertyUnnamedPrimitiveType(StringType, false, 0)
            //}
            elementType("infixExpression") {
                // infixExpression =  [ expression / infixOperator ]2+ ;
                propertyListSeparatedTypeOf("expression", "expression", StringType, false, 0)
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
            elementType("colonOperator") {
                propertyStringType("COLON", false, 0)
            }
            elementType("matrix") {
                // matrix = '['  [row / ';']*  ']' ; //strictly speaking ',' and ';' are operators in mscript for array concatination!
                propertyListSeparatedTypeOf("row", "row", StringType, false, 1)
            }
            elementType("row") {
                // row = expression (','? expression)* ;
                propertyElementTypeOf("expression", "expression", false, 0)
                propertyListOfTupleType("\$group", false, 1) {
                    propertyUnnamedStringType(true, 0)
                    propertyElementTypeOf("expression", "expression", false, 1)
                }
            }
            elementType("literalExpression") {
                propertyStringType("literalValue", false, 0)
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
            elementType("rootVariable") {
                // rootVariable = NAME ;
                propertyStringType("NAME", false, 0)
            }
            stringTypeFor("number")
            //elementType("number") {
            // number = INTEGER | REAL ;
            //    propertyUnnamedPrimitiveType(StringType, false, 0)
            //}
        }
        assertEquals(expected.asString(), actual?.asString())
        TypeModelTest.assertEquals(expected, actual)
    }

    @Test
    fun t1() {

        val sentence = """
            graph {
               a
            }
        """.trimIndent()

        val result = processor.process(sentence)
        val actual = result.asm?.rootElements?.firstOrNull()
        assertEquals(emptyList(), result.issues)
        assertNotNull(actual)

        val expected = asmSimple {
            root("graph") {
                propertyString("STRICT", null)
                propertyString("type", "graph")
                propertyString("ID", null)
                propertyListOfElement("stmt_list") {
                    element("stmt1") {
                        propertyElementExplicitType("stmt","node_stmt") {
                                propertyElement("node_id") {
                                    propertyString("ID","a")
                                    propertyString("port", null)
                                }
                                propertyString("attr_lists", null)
                            }
                        propertyUnnamedString(null)
                    }
                }
            }
        }
        assertEquals(expected.asString(" "), result.asm?.asString(" "))
    }

    @Test
    fun t2() {

        val sentence = """
            graph {
               a -- b
            }
        """.trimIndent()

        val result = processor.process(sentence)
        val actual = result.asm?.rootElements?.firstOrNull()
        assertNotNull(actual)
        assertEquals(emptyList(), result.issues)
    }

    @Test
    fun t3() {
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
        assertEquals(emptyList(), result.issues)
    }

}