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
package net.akehurst.language.agl.processor.dot

import net.akehurst.language.agl.grammar.grammar.ConverterToRuntimeRules
import net.akehurst.language.agl.parser.ScanOnDemandParser
import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.agl.syntaxAnalyser.ContextSimple
import net.akehurst.language.api.asm.AsmSimple
import net.akehurst.language.api.processor.AutomatonKind
import net.akehurst.language.api.processor.LanguageProcessor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_Dot_Singles {

    private companion object {

        private val grammarStr = this::class.java.getResource("/dot/Dot.agl")?.readText() ?: error("File not found")
        var processor: LanguageProcessor<AsmSimple, ContextSimple> = Agl.processorFromStringDefault(grammarStr).processor!!

    }

    @Test()
    fun scan() {
        processor.scan("graph { }")
    }

    @Test
    fun SINGLE_LINE_COMMENT() {
        val goal = "graph"
        val sentence = """
          // a comment
          graph { }
        """.trimIndent()
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun MULTI_LINE_COMMENT() {
        val goal = "graph"
        val sentence = """
          /* a comment */
          graph { }
        """.trimIndent()
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun ID__from_HTML() {

        val goal = "ID"
        val sentence = """
        < <xml >xxxx</xml> >
        """.trimIndent()
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt,result.issues.joinToString(separator = "\n") { "$it" })
        assertTrue(result.issues.errors.isEmpty())
        println(result.sppt!!.toStringAll)

        val expected = processor.spptParser.parse("""
            ID { HTML {
              '<' WHITESPACE : ' '
              §Xml§elementContent§embedded1 { Xml::elementContent {
                startTag {
                  '<'
                  §startTag§multi1 { §empty }
                  NAME { "[a-zA-Z][a-zA-Z0-9]*" : 'xml' }
                  §startTag§multi2 { WS { "\s+" : ' ' } }
                  §startTag§multi3 { §empty }
                  '>'
                }
                content { §content§multi1 { §content§group1 { CHARDATA { "[^<]+" : 'xxxx' } } } }
                endTag {
                  '</'
                  §endTag§multi1 { §empty }
                  NAME { "[a-zA-Z][a-zA-Z0-9]*" : 'xml' }
                  §endTag§multi2 { §empty }
                  '>'
                }
              } } WHITESPACE : ' '
              '>'
            } }
        """.trimIndent())
        val actual = result.sppt!!
        assertEquals(expected.toStringAll,actual.toStringAll)
        assertEquals(expected,actual)
    }

    @Test
    fun a_list__from_Data_Structures() {
        val goal = "attr_list_content"
        val sentence = """
        label = "<f0> 0x10ba8| <f1>"
        shape = "record"
        """
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun attr_list__from_Data_Structures() {
        val goal = "attr_list"
        val sentence = """[
        label = "<f0> 0x10ba8| <f1>"
        shape = "record"
        ]"""
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun attr_stmt__from_Data_Structures() {
        val goal = "attr_stmt"
        val sentence = """
            edge [ ]
        """.trimIndent()
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun stmt_list__from_Data_Structures() {
        val goal = "stmt_list"
        val sentence = """
            graph [
            rankdir = "LR"
            ];
            node [
            fontsize = "16"
            shape = "ellipse"
            ];
            edge [
            ];
        """.trimIndent()
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty(), result.issues.joinToString(separator = "\n") { "$it" })
    }

    @Test
    fun graph__from_Data_Structures() {
        val goal = "graph"
        val sentence = """
            digraph g {
            graph [
            rankdir = "LR"
            ];
            node [
            fontsize = "16"
            shape = "ellipse"
            ];
            edge [
            ];
            "node0" [
            label = "<f0> 0x10ba8| <f1>"
            shape = "record"
            ];
            }
        """.trimIndent()
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty(), result.issues.joinToString(separator = "\n") { "$it" })

    }

    @Test
    fun stmt_list__1() {
        val goal = "stmt_list"
        val sentence = "graph[a=a ]; node [b=b c=c]; edge[];"
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty(), result.issues.joinToString(separator = "\n") { "$it" })
    }

    @Test
    fun attr_list__2s() {
        val goal = "attr_list"
        val sentence = "[x = x; y=y]"
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())

    }

    @Test
    fun attr_list__2n() {
        val goal = "attr_list"
        val sentence = "[x = x y=y]"
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())

    }

    @Test
    fun LionShare__node_id() {
        val goal = "node_id"
        val sentence = """
            "001"
        """.trimIndent()
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun LionShare__attr_list() {
        val goal = "attr_list"
        val sentence = """
            [shape=box     , regular=1,style=filled,fillcolor=white   ]
        """.trimIndent()

        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun LionShare__node_stmt() {
        val goal = "node_stmt"
        val sentence = """
            "001" [shape=box     , regular=1,style=filled,fillcolor=white   ]
        """.trimIndent()

        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())

    }

    @Test
    fun stmt_list_automaton() {
        val goal = "stmt_list"
        val sentence = "a -> b ;"

        val converterToRuntimeRules = ConverterToRuntimeRules(processor.grammar!!)
        val parser = ScanOnDemandParser(converterToRuntimeRules.runtimeRuleSet)

        //fails at season 9 with edge_list
        val result = parser.parseForGoal(goal, sentence, AutomatonKind.LOOKAHEAD_1)
        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun stmt_list1() {
        val goal = "stmt_list"
        val sentence = "a -> b ;"

        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun LionShare__stmt_list1() {
        val goal = "stmt_list"
        val sentence = """
            "marr0017" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "026" -> "marr0017" [dir=none,weight=1] ;
            "027" -> "marr0017" [dir=none,weight=1] ;
            "marr0017" -> "028" [dir=none, weight=2] ;
        """.trimIndent()

        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun LionShare__stmt_list() {
        val goal = "stmt_list"
        val sentence = """
            "001" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "002" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "003" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "004" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "005" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "006" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "007" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "009" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "014" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "015" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "016" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "ZZ01" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "ZZ02" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "017" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "012" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "008" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "011" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "013" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "010" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "023" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "020" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "021" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "018" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "025" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "019" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "022" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "024" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "027" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "026" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "028" [shape=box     , regular=1,style=filled,fillcolor=grey    ] ;
            "marr0001" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "001" -> "marr0001" [dir=none,weight=1] ;
            "007" -> "marr0001" [dir=none,weight=1] ;
            "marr0001" -> "017" [dir=none, weight=2] ;
            "marr0002" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "001" -> "marr0002" [dir=none,weight=1] ;
            "ZZ02" -> "marr0002" [dir=none,weight=1] ;
            "marr0002" -> "012" [dir=none, weight=2] ;
            "marr0003" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "002" -> "marr0003" [dir=none,weight=1] ;
            "003" -> "marr0003" [dir=none,weight=1] ;
            "marr0003" -> "008" [dir=none, weight=2] ;
            "marr0004" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "002" -> "marr0004" [dir=none,weight=1] ;
            "006" -> "marr0004" [dir=none,weight=1] ;
            "marr0004" -> "011" [dir=none, weight=2] ;
            "marr0005" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "002" -> "marr0005" [dir=none,weight=1] ;
            "ZZ01" -> "marr0005" [dir=none,weight=1] ;
            "marr0005" -> "013" [dir=none, weight=2] ;
            "marr0006" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "004" -> "marr0006" [dir=none,weight=1] ;
            "009" -> "marr0006" [dir=none,weight=1] ;
            "marr0006" -> "010" [dir=none, weight=2] ;
            "marr0007" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "005" -> "marr0007" [dir=none,weight=1] ;
            "015" -> "marr0007" [dir=none,weight=1] ;
            "marr0007" -> "023" [dir=none, weight=2] ;
            "marr0008" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "005" -> "marr0008" [dir=none,weight=1] ;
            "016" -> "marr0008" [dir=none,weight=1] ;
            "marr0008" -> "020" [dir=none, weight=2] ;
            "marr0009" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "005" -> "marr0009" [dir=none,weight=1] ;
            "012" -> "marr0009" [dir=none,weight=1] ;
            "marr0009" -> "021" [dir=none, weight=2] ;
            "marr0010" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "008" -> "marr0010" [dir=none,weight=1] ;
            "017" -> "marr0010" [dir=none,weight=1] ;
            "marr0010" -> "018" [dir=none, weight=2] ;
            "marr0011" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "011" -> "marr0011" [dir=none,weight=1] ;
            "023" -> "marr0011" [dir=none,weight=1] ;
            "marr0011" -> "025" [dir=none, weight=2] ;
            "marr0012" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "013" -> "marr0012" [dir=none,weight=1] ;
            "014" -> "marr0012" [dir=none,weight=1] ;
            "marr0012" -> "019" [dir=none, weight=2] ;
            "marr0013" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "010" -> "marr0013" [dir=none,weight=1] ;
            "021" -> "marr0013" [dir=none,weight=1] ;
            "marr0013" -> "022" [dir=none, weight=2] ;
            "marr0014" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "019" -> "marr0014" [dir=none,weight=1] ;
            "020" -> "marr0014" [dir=none,weight=1] ;
            "marr0014" -> "024" [dir=none, weight=2] ;
            "marr0015" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "022" -> "marr0015" [dir=none,weight=1] ;
            "025" -> "marr0015" [dir=none,weight=1] ;
            "marr0015" -> "027" [dir=none, weight=2] ;
            "marr0016" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "024" -> "marr0016" [dir=none,weight=1] ;
            "018" -> "marr0016" [dir=none,weight=1] ;
            "marr0016" -> "026" [dir=none, weight=2] ;
            "marr0017" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "026" -> "marr0017" [dir=none,weight=1] ;
            "027" -> "marr0017" [dir=none,weight=1] ;
            "marr0017" -> "028" [dir=none, weight=2] ;
        """.trimIndent()

        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun LionShare() {
        val goal = "graph"
        val sentence = """
            ##"A few people in the field of genetics are using dot to draw "marriage node diagram"  pedigree drawings.  Here is one I have done of a test pedigree from the FTREE pedigree drawing package (Lion Share was a racehorse)." Contributed by David Duffy.
            
            ##Command to get the layout: "dot -Tpng thisfile > thisfile.png"
            
            digraph Ped_Lion_Share           {
            # page = "8.2677165,11.692913" ;
            ratio = "auto" ;
            mincross = 2.0 ;
            label = "Pedigree Lion_Share" ;
            
            "001" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "002" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "003" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "004" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "005" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "006" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "007" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "009" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "014" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "015" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "016" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "ZZ01" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "ZZ02" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "017" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "012" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "008" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "011" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "013" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "010" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "023" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "020" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "021" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "018" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "025" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "019" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "022" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "024" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "027" [shape=circle  , regular=1,style=filled,fillcolor=white   ] ;
            "026" [shape=box     , regular=1,style=filled,fillcolor=white   ] ;
            "028" [shape=box     , regular=1,style=filled,fillcolor=grey    ] ;
            "marr0001" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "001" -> "marr0001" [dir=none,weight=1] ;
            "007" -> "marr0001" [dir=none,weight=1] ;
            "marr0001" -> "017" [dir=none, weight=2] ;
            "marr0002" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "001" -> "marr0002" [dir=none,weight=1] ;
            "ZZ02" -> "marr0002" [dir=none,weight=1] ;
            "marr0002" -> "012" [dir=none, weight=2] ;
            "marr0003" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "002" -> "marr0003" [dir=none,weight=1] ;
            "003" -> "marr0003" [dir=none,weight=1] ;
            "marr0003" -> "008" [dir=none, weight=2] ;
            "marr0004" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "002" -> "marr0004" [dir=none,weight=1] ;
            "006" -> "marr0004" [dir=none,weight=1] ;
            "marr0004" -> "011" [dir=none, weight=2] ;
            "marr0005" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "002" -> "marr0005" [dir=none,weight=1] ;
            "ZZ01" -> "marr0005" [dir=none,weight=1] ;
            "marr0005" -> "013" [dir=none, weight=2] ;
            "marr0006" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "004" -> "marr0006" [dir=none,weight=1] ;
            "009" -> "marr0006" [dir=none,weight=1] ;
            "marr0006" -> "010" [dir=none, weight=2] ;
            "marr0007" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "005" -> "marr0007" [dir=none,weight=1] ;
            "015" -> "marr0007" [dir=none,weight=1] ;
            "marr0007" -> "023" [dir=none, weight=2] ;
            "marr0008" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "005" -> "marr0008" [dir=none,weight=1] ;
            "016" -> "marr0008" [dir=none,weight=1] ;
            "marr0008" -> "020" [dir=none, weight=2] ;
            "marr0009" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "005" -> "marr0009" [dir=none,weight=1] ;
            "012" -> "marr0009" [dir=none,weight=1] ;
            "marr0009" -> "021" [dir=none, weight=2] ;
            "marr0010" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "008" -> "marr0010" [dir=none,weight=1] ;
            "017" -> "marr0010" [dir=none,weight=1] ;
            "marr0010" -> "018" [dir=none, weight=2] ;
            "marr0011" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "011" -> "marr0011" [dir=none,weight=1] ;
            "023" -> "marr0011" [dir=none,weight=1] ;
            "marr0011" -> "025" [dir=none, weight=2] ;
            "marr0012" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "013" -> "marr0012" [dir=none,weight=1] ;
            "014" -> "marr0012" [dir=none,weight=1] ;
            "marr0012" -> "019" [dir=none, weight=2] ;
            "marr0013" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "010" -> "marr0013" [dir=none,weight=1] ;
            "021" -> "marr0013" [dir=none,weight=1] ;
            "marr0013" -> "022" [dir=none, weight=2] ;
            "marr0014" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "019" -> "marr0014" [dir=none,weight=1] ;
            "020" -> "marr0014" [dir=none,weight=1] ;
            "marr0014" -> "024" [dir=none, weight=2] ;
            "marr0015" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "022" -> "marr0015" [dir=none,weight=1] ;
            "025" -> "marr0015" [dir=none,weight=1] ;
            "marr0015" -> "027" [dir=none, weight=2] ;
            "marr0016" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "024" -> "marr0016" [dir=none,weight=1] ;
            "018" -> "marr0016" [dir=none,weight=1] ;
            "marr0016" -> "026" [dir=none, weight=2] ;
            "marr0017" [shape=diamond,style=filled,label="",height=.1,width=.1] ;
            "026" -> "marr0017" [dir=none,weight=1] ;
            "027" -> "marr0017" [dir=none,weight=1] ;
            "marr0017" -> "028" [dir=none, weight=2] ;
            }
            """.trimIndent()
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun emptyString() {
        val goal = "DOUBLE_QUOTE_STRING"
        val sentence = """
            ""
        """.trimIndent()

        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun node_stmt_with_emptyString() {
        val goal = "node_stmt"
        val sentence = """
            node[style=filled,label=""]
        """.trimIndent()

        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun Synchronous_Digital_Hierarchy_Stack() {
        val goal = "graph"
        val sentence = """
digraph G {
	graph [bgcolor=black];	/* set background */
	edge [color=white];
	graph[page="8.5,11",size="7.5,7",ratio=fill,center=1];
	node[style=filled,label=""];
	subgraph ds3CTP {
		rank = same;
		node[shape=box,color=green];
		ds3CTP_1_1;
		ds3CTP_1_2;
		ds3CTP_5_1;
		ds3CTP_5_2;
	}
	subgraph t3TTP {
		rank = same;
		node[shape=invtriangle,color=red];
		t3TTP_1_1;
		t3TTP_5_2;
	}
	subgraph vc3TTP {
		rank = same;
		node[shape=invtriangle,color=red];
		vc3TTP_1_2;
		vc3TTP_5_1;
	}
	subgraph fabric {
		rank = same;
		node[shape=hexagon,color=blue];
		fabric_1_2;
		fabric_4_1;
		fabric_5_1;
	}
	subgraph xp {
		rank = same;
		node[shape=diamond,color=blue];
		xp_1_2;
		xp_4_1;
		xp_5_1;
	}
	subgraph au3CTP {
		rank = same;
		node[shape=box,color=green];
		au3CTP_1_2;
		au3CTP_5_1;
	}
	subgraph aug {
		rank = same;
		node[shape=invtrapezium,color=pink];
		aug_1_2;
		aug_4_1;
		aug_4_2;
		aug_5_1;
	}
	subgraph protectionTTP {
		rank = same;
		node[shape=invtriangle,color=red];
	}
	subgraph protectionGroup {
		rank = same;
		node[shape=hexagon,color=blue];
	}
	subgraph protectionUnit {
		rank = same;
		node[shape=diamond,color=blue];
	}
	subgraph protectionCTP {
		node[shape=box,color=green];
	}
	subgraph msTTP {
		rank = same;
		node[shape=invtriangle,color=red];
	}
	subgraph msCTP {
		rank = same;
		node[shape=box,color=green];
	}
	subgraph rsTTP {
		rank = same;
		node[shape=invtriangle,color=red];
	}
	subgraph rsCTP {
		rank = same;
		node[shape=box,color=green];
	}
	subgraph spiTTP {
		rank = same;
		node[shape=invtriangle,color=red];
	}
	subgraph me {
		rank = same;
		node[shape=box,peripheries=2];
		me_1;
	}
	subgraph client_server {
		edge[style=dotted,dir=none,weight=100];
		ds3CTP_1_1->t3TTP_1_1;
		ds3CTP_1_2->vc3TTP_1_2;
		au3CTP_1_2->aug_1_2->prTTP_1_2;
	}
}
        """.trimIndent()

        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty(), result.issues.joinToString(separator = "\n") { "$it" })
    }

    @Test
    fun Synchronous_Digital_Hierarchy_Stack2() {
        val goal = "graph"
        val sentence = """
digraph G {
	graph [bgcolor=black];	/* set background */
	edge [color=white];
	graph[page="8.5,11",size="7.5,7",ratio=fill,center=1];
	node[style=filled,label=""];
	subgraph ds3CTP {
		rank = same;
		node[shape=box,color=green];
		ds3CTP_1_1;
		ds3CTP_1_2;
		ds3CTP_5_1;
		ds3CTP_5_2;
	}
	subgraph t3TTP {
		rank = same;
		node[shape=invtriangle,color=red];
		t3TTP_1_1;
		t3TTP_5_2;
	}
	subgraph vc3TTP {
		rank = same;
		node[shape=invtriangle,color=red];
		vc3TTP_1_2;
		vc3TTP_5_1;
	}
	subgraph fabric {
		rank = same;
		node[shape=hexagon,color=blue];
		fabric_1_2;
		fabric_4_1;
		fabric_5_1;
	}
	subgraph xp {
		rank = same;
		node[shape=diamond,color=blue];
		xp_1_2;
		xp_4_1;
		xp_5_1;
	}
	subgraph au3CTP {
		rank = same;
		node[shape=box,color=green];
		au3CTP_1_2;
		au3CTP_4_1;
		au3CTP_4_2;
		au3CTP_5_1;
	}
	subgraph aug {
		rank = same;
		node[shape=invtrapezium,color=pink];
		aug_1_2;
		aug_4_1;
		aug_4_2;
		aug_5_1;
	}
	subgraph protectionTTP {
		rank = same;
		node[shape=invtriangle,color=red];
		prTTP_1_2;
		prTTP_4_1;
		prTTP_4_2;
		prTTP_5_1;
	}
	subgraph protectionGroup {
		rank = same;
		node[shape=hexagon,color=blue];
		pg_1_2;
		pg_4_1;
		pg_4_2;
		pg_5_1;
	}
	subgraph protectionUnit {
		rank = same;
		node[shape=diamond,color=blue];
		pu_1_2;
		pu_4_1;
		pu_4_2;
		pu_5_1;
	}
	subgraph protectionCTP {
		node[shape=box,color=green];
		prCTP_1_2;
		prCTP_4_1;
		prCTP_4_2;
		prCTP_5_1;
	}
	subgraph msTTP {
		rank = same;
		node[shape=invtriangle,color=red];
		msTTP_1_2;
		msTTP_4_1;
		msTTP_4_2;
		msTTP_5_1;
	}
	subgraph msCTP {
		rank = same;
		node[shape=box,color=green];
		msCTP_1_2;
		msCTP_3_1;
		msCTP_3_2;
		msCTP_4_1;
		msCTP_4_2;
		msCTP_5_1;
	}
	subgraph rsTTP {
		rank = same;
		node[shape=invtriangle,color=red];
		rsTTP_1_2;
		rsTTP_3_1;
		rsTTP_3_2;
		rsTTP_4_1;
		rsTTP_4_2;
		rsTTP_5_1;
	}
	subgraph rsCTP {
		rank = same;
		node[shape=box,color=green];
		rsCTP_1_2;
		rsCTP_2_1;
		rsCTP_2_2;
		rsCTP_3_1;
		rsCTP_3_2;
		rsCTP_4_1;
		rsCTP_4_2;
		rsCTP_5_1;
	}
	subgraph spiTTP {
		rank = same;
		node[shape=invtriangle,color=red];
		spiTTP_1_2;
		spiTTP_2_1;
		spiTTP_2_2;
		spiTTP_3_1;
		spiTTP_3_2;
		spiTTP_4_1;
		spiTTP_4_2;
		spiTTP_5_1;
	}
	subgraph me {
		rank = same;
		node[shape=box,peripheries=2];
		me_1;
		me_2;
		me_3;
		me_4;
		me_5;
	}
	subgraph client_server {
		edge[style=dotted,dir=none,weight=100];
		ds3CTP_1_1->t3TTP_1_1;
		ds3CTP_1_2->vc3TTP_1_2;
		au3CTP_1_2->aug_1_2->prTTP_1_2;
		prCTP_1_2->msTTP_1_2;
		msCTP_1_2->rsTTP_1_2;
		rsCTP_1_2->spiTTP_1_2;
		rsCTP_2_1->spiTTP_2_1;
		rsCTP_2_2->spiTTP_2_2;
		msCTP_3_1->rsTTP_3_1;
		rsCTP_3_1->spiTTP_3_1;
		msCTP_3_2->rsTTP_3_2;
		rsCTP_3_2->spiTTP_3_2;
		au3CTP_4_1->aug_4_1->prTTP_4_1;
		prCTP_4_1->msTTP_4_1;
		msCTP_4_1->rsTTP_4_1;
		rsCTP_4_1->spiTTP_4_1;
		au3CTP_4_2->aug_4_2->prTTP_4_2;
		prCTP_4_2->msTTP_4_2;
		msCTP_4_2->rsTTP_4_2;
		rsCTP_4_2->spiTTP_4_2;
		ds3CTP_5_1->vc3TTP_5_1;
		au3CTP_5_1->aug_5_1->prTTP_5_1;
		prCTP_5_1->msTTP_5_1;
		msCTP_5_1->rsTTP_5_1;
		rsCTP_5_1->spiTTP_5_1;
		ds3CTP_5_2->t3TTP_5_2;
	}
	subgraph trail {
		edge[style=dashed,dir=none];
		vc3TTP_1_2->vc3TTP_5_1;
		prTTP_1_2->prTTP_4_1;
		prTTP_4_2->prTTP_5_1;
		msTTP_1_2->msTTP_4_1;
		msTTP_4_2->msTTP_5_1;
		rsTTP_1_2->rsTTP_3_1;
		rsTTP_3_2->rsTTP_4_1;
		rsTTP_4_2->rsTTP_5_1;
		spiTTP_1_2->spiTTP_2_1;
		spiTTP_2_2->spiTTP_3_1;
		spiTTP_3_2->spiTTP_4_1;
		spiTTP_4_2->spiTTP_5_1;
	}
	subgraph contain {
		pu_1_2->pg_1_2;
		pu_4_1->pg_4_1;
		pu_4_2->pg_4_2;
		pu_5_1->pg_5_1;
		xp_1_2->fabric_1_2;
		xp_4_1->fabric_4_1;
		xp_5_1->fabric_5_1;
		fabric_1_2->me_1;
		fabric_4_1->me_4;
		fabric_5_1->me_5;
		pg_1_2->me_1;
		pg_4_1->me_4;
		pg_4_2->me_4;
		pg_5_1->me_5;
		t3TTP_1_1->me_1;
		t3TTP_5_2->me_5;
		vc3TTP_1_2->me_1;
		vc3TTP_5_1->me_5;
		prTTP_1_2->me_1;
		prTTP_4_1->me_4;
		prTTP_4_2->me_4;
		prTTP_5_1->me_5;
		msTTP_1_2->me_1;
		msTTP_4_1->me_4;
		msTTP_4_2->me_4;
		msTTP_5_1->me_5;
		rsTTP_1_2->me_1;
		rsTTP_3_1->me_3;
		rsTTP_3_2->me_3;
		rsTTP_4_1->me_4;
		rsTTP_4_2->me_4;
		rsTTP_5_1->me_5;
		spiTTP_1_2->me_1;
		spiTTP_2_1->me_2;
		spiTTP_2_2->me_2;
		spiTTP_3_1->me_3;
		spiTTP_3_2->me_3;
		spiTTP_4_1->me_4;
		spiTTP_4_2->me_4;
		spiTTP_5_1->me_5;
	}
	subgraph connectedBy {
		vc3TTP_1_2->fabric_1_2;
		au3CTP_1_2->fabric_1_2;
		au3CTP_4_1->fabric_4_1;
		au3CTP_4_2->fabric_4_1;
		vc3TTP_5_1->fabric_5_1;
		au3CTP_5_1->fabric_5_1;
		prTTP_1_2->pg_1_2;
		prTTP_4_1->pg_4_1;
		prTTP_4_2->pg_4_2;
		prTTP_5_1->pg_5_1;
		prCTP_1_2->pg_1_2;
		prCTP_4_1->pg_4_1;
		prCTP_4_2->pg_4_2;
		prCTP_5_1->pg_5_1;
	}
	subgraph crossConnection {
		edge[style=dotted,dir=none];
		vc3TTP_1_2->xp_1_2->au3CTP_1_2;
		prTTP_1_2->pu_1_2->prCTP_1_2;
		prTTP_4_1->pu_4_1->prCTP_4_1;
		au3CTP_4_1->xp_4_1->au3CTP_4_2;
		prTTP_4_2->pu_4_2->prCTP_4_2;
		prTTP_5_1->pu_5_1->prCTP_5_1;
		vc3TTP_5_1->xp_5_1->au3CTP_5_1;
	}
	subgraph bindingConnection {
		edge[style=bold,dir=none,weight=100];
		ds3CTP_1_1->ds3CTP_1_2;
		vc3TTP_1_2->au3CTP_1_2;
		prTTP_1_2->prCTP_1_2;
		msTTP_1_2->msCTP_1_2;
		rsTTP_1_2->rsCTP_1_2;
		rsCTP_2_1->rsCTP_2_2;
		rsTTP_3_1->rsCTP_3_1;
		msCTP_3_1->msCTP_3_2;
		rsTTP_3_2->rsCTP_3_2;
		prTTP_4_1->prCTP_4_1;
		msTTP_4_1->msCTP_4_1;
		rsTTP_4_1->rsCTP_4_1;
		au3CTP_4_1->au3CTP_4_2;
		prTTP_4_2->prCTP_4_2;
		msTTP_4_2->msCTP_4_2;
		rsTTP_4_2->rsCTP_4_2;
		prTTP_5_1->prCTP_5_1;
		msTTP_5_1->msCTP_5_1;
		rsTTP_5_1->rsCTP_5_1;
		ds3CTP_5_1->ds3CTP_5_2;
		vc3TTP_5_1->au3CTP_5_1;
	}
}
        """.trimIndent()

        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty())
    }
}
