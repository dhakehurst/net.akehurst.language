/**
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.grammar.grammar

import net.akehurst.language.agl.processor.Agl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class test_AglGrammarSyntaxAnalyser2 {

    companion object {
        val aglProc = Agl.registry.agl.grammar.processor!!
    }

    @Test
    fun rule_one_terminal() {
        val sentence = """
            namespace test
            grammar Test {
              a = 'a';
            }
        """.trimIndent()
        val sppt = aglProc.parse(sentence).sppt!!
        //println(sppt.toStringAll)
        val sut = AglGrammarSyntaxAnalyser2()
        val res = sut.transform(sppt) { _, _ -> TODO() }
        println(res.asm!![0].toString())
    }

    @Test
    fun extends_one() {
        val sentence = """
            namespace ns.test
            grammar Test extends ns.test.Test2 {
              leaf a = 'a' ;
            }
        """.trimIndent()
        val sppt = aglProc.parse(sentence).sppt!!
        //println(sppt.toStringAll)
        val sut = AglGrammarSyntaxAnalyser2()
        val res = sut.transform(sppt) { _, _ -> TODO() }
        println(res.asm!![0].toString())

        assertTrue(res.asm!![0].findNonTerminalRule("a") != null)
    }

    @Test
    fun extends_two() {
        val sentence = """
            namespace ns.test
            grammar Test extends ns.test.Test2, AA {
              leaf a = 'a' ;
            }
        """.trimIndent()
        val sppt = aglProc.parse(sentence).sppt!!
        //println(sppt.toStringAll)
        val sut = AglGrammarSyntaxAnalyser2()
        val res = sut.transform(sppt) { _, _ -> TODO() }
        println(res.asm!![0].toString())

        assertEquals(2, res.asm!![0].extends.size)
    }

    @Test
    fun rule_two_terminal() {
        val sentence = """
            namespace test
            grammar Test {
              a = 'a' 'b' ;
            }
        """.trimIndent()
        val sppt = aglProc.parse(sentence).sppt!!
        //println(sppt.toStringAll)
        val sut = AglGrammarSyntaxAnalyser2()
        val res = sut.transform(sppt) { _, _ -> TODO() }
        println(res.asm!![0].toString())
    }

    @Test
    fun rule_empty() {
        val sentence = """
            namespace test
            grammar Test {
              a =  ;
            }
        """.trimIndent()
        val sppt = aglProc.parse(sentence).sppt!!
        //println(sppt.toStringAll)
        val sut = AglGrammarSyntaxAnalyser2()
        val res = sut.transform(sppt) { _, _ -> TODO() }
        println(res.asm!![0].toString())
    }

    @Test
    fun rule_simpleChoice() {
        val sentence = """
            namespace test
            grammar Test {
              a = 'a' | 'b' ;
            }
        """.trimIndent()
        val sppt = aglProc.parse(sentence).sppt!!
        //println(sppt.toStringAll)
        val sut = AglGrammarSyntaxAnalyser2()
        val res = sut.transform(sppt) { _, _ -> TODO() }
        println(res.asm!![0].toString())
    }

    @Test
    fun rule_prioChoice() {
        val sentence = """
            namespace test
            grammar Test {
              a = 'a' < 'a' ;
            }
        """.trimIndent()
        val sppt = aglProc.parse(sentence).sppt!!
        //println(sppt.toStringAll)
        val sut = AglGrammarSyntaxAnalyser2()
        val res = sut.transform(sppt) { _, _ -> TODO() }
        println(res.asm!![0].toString())
    }

    @Test
    fun rule_ambigChoice() {
        val sentence = """
            namespace test
            grammar Test {
              a = 'a' || 'a' ;
            }
        """.trimIndent()
        val sppt = aglProc.parse(sentence).sppt!!
        //println(sppt.toStringAll)
        val sut = AglGrammarSyntaxAnalyser2()
        val res = sut.transform(sppt) { _, _ -> TODO() }
        println(res.asm!![0].toString())
    }

    @Test
    fun rule_leaf_terminal() {
        val sentence = """
            namespace ns.test
            grammar Test {
              leaf a = 'a' ;
            }
        """.trimIndent()
        val sppt = aglProc.parse(sentence).sppt!!
        //println(sppt.toStringAll)
        val sut = AglGrammarSyntaxAnalyser2()
        val res = sut.transform(sppt) { _, _ -> TODO() }
        println(res.asm!![0].toString())

        assertTrue(res.asm!![0].findNonTerminalRule("a") != null)
    }

    @Test
    fun rule_skip_terminal() {
        val sentence = """
            namespace ns.test
            grammar Test {
              skip WS = "\s+" ;
              a = 'a' ;
            }
        """.trimIndent()
        val sppt = aglProc.parse(sentence).sppt!!
        //println(sppt.toStringAll)
        val sut = AglGrammarSyntaxAnalyser2()
        val res = sut.transform(sppt) { _, _ -> TODO() }
        println(res.asm!![0].toString())

        assertTrue(res.asm!![0].findNonTerminalRule("a") != null)
    }

    @Test
    fun rule_multiplicity_0n() {
        val sentence = """
            namespace ns.test
            grammar Test {
              a = 'a'* ;
            }
        """.trimIndent()
        val sppt = aglProc.parse(sentence).sppt!!
        //println(sppt.toStringAll)
        val sut = AglGrammarSyntaxAnalyser2()
        val res = sut.transform(sppt) { _, _ -> TODO() }
        println(res.asm!![0].toString())

        assertTrue(res.asm!![0].findNonTerminalRule("a") != null)
    }

    @Test
    fun rule_multiplicity_1n() {
        val sentence = """
            namespace ns.test
            grammar Test {
              a = 'a'+ ;
            }
        """.trimIndent()
        val sppt = aglProc.parse(sentence).sppt!!
        //println(sppt.toStringAll)
        val sut = AglGrammarSyntaxAnalyser2()
        val res = sut.transform(sppt) { _, _ -> TODO() }
        println(res.asm!![0].toString())

        assertTrue(res.asm!![0].findNonTerminalRule("a") != null)
    }

    @Test
    fun rule_multiplicity_01() {
        val sentence = """
            namespace ns.test
            grammar Test {
              a = 'a'? ;
            }
        """.trimIndent()
        val sppt = aglProc.parse(sentence).sppt!!
        //println(sppt.toStringAll)
        val sut = AglGrammarSyntaxAnalyser2()
        val res = sut.transform(sppt) { _, _ -> TODO() }
        println(res.asm!![0].toString())

        assertTrue(res.asm!![0].findNonTerminalRule("a") != null)
    }

    @Test
    fun rule_multiplicity_4n() {
        val sentence = """
            namespace ns.test
            grammar Test {
              a = 'a'4+ ;
            }
        """.trimIndent()
        val sppt = aglProc.parse(sentence).sppt!!
        //println(sppt.toStringAll)
        val sut = AglGrammarSyntaxAnalyser2()
        val res = sut.transform(sppt) { _, _ -> TODO() }
        println(res.asm!![0].toString())

        assertTrue(res.asm!![0].findNonTerminalRule("a") != null)
    }

    @Test
    fun rule_multiplicity_4() {
        val sentence = """
            namespace ns.test
            grammar Test {
              a = 'a'4 ;
            }
        """.trimIndent()
        val sppt = aglProc.parse(sentence).sppt!!
        //println(sppt.toStringAll)
        val sut = AglGrammarSyntaxAnalyser2()
        val res = sut.transform(sppt) { _, _ -> TODO() }
        println(res.asm!![0].toString())

        assertTrue(res.asm!![0].findNonTerminalRule("a") != null)
    }

    @Test
    fun rule_multiplicity_b4() {
        val sentence = """
            namespace ns.test
            grammar Test {
              a = 'a'{4} ;
            }
        """.trimIndent()
        val sppt = aglProc.parse(sentence).sppt!!
        //println(sppt.toStringAll)
        val sut = AglGrammarSyntaxAnalyser2()
        val res = sut.transform(sppt) { _, _ -> TODO() }
        println(res.asm!![0].toString())

        assertTrue(res.asm!![0].findNonTerminalRule("a") != null)
    }

    @Test
    fun rule_multiplicity_47() {
        val sentence = """
            namespace ns.test
            grammar Test {
              a = 'a'4..7 ;
            }
        """.trimIndent()
        val sppt = aglProc.parse(sentence).sppt!!
        //println(sppt.toStringAll)
        val sut = AglGrammarSyntaxAnalyser2()
        val res = sut.transform(sppt) { _, _ -> TODO() }
        println(res.asm!![0].toString())

        assertTrue(res.asm!![0].findNonTerminalRule("a") != null)
    }

    @Test
    fun rule_multiplicity_b47() {
        val sentence = """
            namespace ns.test
            grammar Test {
              a = 'a'{4..7} ;
            }
        """.trimIndent()
        val sppt = aglProc.parse(sentence).sppt!!
        //println(sppt.toStringAll)
        val sut = AglGrammarSyntaxAnalyser2()
        val res = sut.transform(sppt) { _, _ -> TODO() }
        println(res.asm!![0].toString())

        assertTrue(res.asm!![0].findNonTerminalRule("a") != null)
    }

    @Test
    fun rule_sepList() {
        val sentence = """
            namespace ns.test
            grammar Test {
              S = [A / ',']2+ ;
              A = 'a' ;
            }
        """.trimIndent()
        val sppt = aglProc.parse(sentence).sppt!!
        //println(sppt.toStringAll)
        val sut = AglGrammarSyntaxAnalyser2()
        val res = sut.transform(sppt) { _, _ -> TODO() }
        println(res.asm!![0].toString())

        //assertTrue(res.asm!![0].findNonTerminalRule("a") != null)
    }

    @Test
    fun rule_embedded() {
        val sentence = """
            namespace ns.test
            grammar Test {
              S = [A / ',']2+ ;
              A = Inn::S ;
            }
        """.trimIndent()
        val sppt = aglProc.parse(sentence).sppt!!
        //println(sppt.toStringAll)
        val sut = AglGrammarSyntaxAnalyser2()
        val res = sut.transform(sppt) { _, _ -> TODO() }
        println(res.asm!![0].toString())

        //assertTrue(res.asm!![0].findNonTerminalRule("a") != null)
    }

    @Test
    fun rule_group() {
        val sentence = """
            namespace ns.test
            grammar Test {
              a = 'a' ( 'b' 'c' ) 'd' ;
            }
        """.trimIndent()
        val sppt = aglProc.parse(sentence).sppt!!
        //println(sppt.toStringAll)
        val sut = AglGrammarSyntaxAnalyser2()
        val res = sut.transform(sppt) { _, _ -> TODO() }
        println(res.asm!![0].toString())

        assertTrue(res.asm!![0].findNonTerminalRule("a") != null)
    }

    @Test
    fun rule_group_choice() {
        val sentence = """
            namespace ns.test
            grammar Test {
              a = 'a' ( C | B ) 'd' ;
              B = 'b' ;
              C = 'c' ;
            }
        """.trimIndent()
        val sppt = aglProc.parse(sentence).sppt!!
        //println(sppt.toStringAll)
        val sut = AglGrammarSyntaxAnalyser2()
        val res = sut.transform(sppt) { _, _ -> TODO() }
        println(res.asm!![0].toString())

        assertTrue(res.asm!![0].findNonTerminalRule("a") != null)
    }
}