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
import net.akehurst.language.agl.syntaxAnalyser.ContextSimple
import net.akehurst.language.api.asm.AsmSimple
import net.akehurst.language.api.asm.asmSimple
import net.akehurst.language.api.processor.LanguageProcessor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_Dot_SyntaxAnalyser {

    companion object {
        private val grammarStr = this::class.java.getResource("/dot/Dot.agl").readText()
        var processor: LanguageProcessor<AsmSimple, ContextSimple> = Agl.processorFromStringDefault(grammarStr)
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
        assertNotNull(actual)
        assertEquals(emptyList(), result.issues)

        val expected = asmSimple {
            root("graph") {
                propertyString("STRICT", null)
                propertyString("type", "graph")
                propertyString("ID", null)
                propertyListOfElement("stmt_list") {
                    element("stmt1") {
                        propertyElement("stmt") {
                            propertyUnnamedElement("node_stmt") {
                                propertyElement("node_id") {
                                    propertyElement("ID") {
                                        propertyUnnamedString("a")
                                    }
                                    propertyString("port", null)
                                }
                                propertyString("attr_lists", null)
                            }
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