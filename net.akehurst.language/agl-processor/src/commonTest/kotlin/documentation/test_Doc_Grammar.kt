/*
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.documentation

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.GrammarString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The examples in 5_Documentation/dsl/grammar.md.
 */
class test_Doc_Grammar {

    private companion object {
        fun processor(grammar: String) = Agl.processorFromStringSimple(GrammarString(grammar.trimIndent())).let {
            check(it.issues.errors.isEmpty()) { it.issues.toString() }
            it.processor!!
        }

        /** parse and return the parse tree as text */
        fun tree(grammar: String, sentence: String): String {
            val result = processor(grammar).parse(sentence)
            assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
            return result.sppt!!.toStringAll.trim()
        }

        fun parseError(grammar: String, sentence: String): String =
            processor(grammar).parse(sentence).issues.errors.first().message
    }

    @Test
    fun choice_longest_then_last() {
        // grammar.md: "Choice"
        val grammar = """
            namespace example
            grammar Choice {
                S = a | b ;
                a = 'x' ;
                b = 'x' ;
            }
        """
        assertEquals("S { b { 'x' } }", tree(grammar, "x"))
    }

    @Test
    fun choice_ambiguous() {
        // grammar.md: "Choice"
        val grammar = """
            namespace example
            grammar Choice {
                S = a || b ;
                a = 'x' ;
                b = 'x' ;
            }
        """
        val expected = """
            S|0 {
            a { 'x' } }
            S|1 {
            b { 'x' } }
        """.trimIndent()
        assertEquals(expected, tree(grammar, "x"))
    }

    @Test
    fun multiplicity() {
        // grammar.md: "Lists and multiplicity"
        val grammar = """
            namespace example
            grammar Lists {
                skip leaf WS = "\s+" ;
                S = 'a'2..3 'b'2+ [ 'c' / ',' ]* 'd'? ;
            }
        """
        val p = processor(grammar)
        for (s in listOf("a a b b", "a a a b b b c, c d")) {
            assertTrue(p.parse(s).issues.errors.isEmpty(), s)
        }
        assertTrue(p.parse("a b b").issues.errors.isNotEmpty())
    }

    @Test
    fun skip_and_leaf() {
        // grammar.md: "skip and leaf"
        val grammar = """
            namespace example
            grammar Keywords {
                skip leaf WS = "\s+" ;
                S = 'if' ID ;
                leaf ID = "[a-z]+" ;
            }
        """
        assertEquals("Failed to match {<GOAL>} at: if^x", parseError(grammar, "ifx"))
        val expected = """
            S {
              'if'
              WS : ' '
              ID : 'x'
            }
        """.trimIndent()
        assertEquals(expected, tree(grammar, "if x"))
    }

    @Test
    fun skip_not_required() {
        // grammar.md: "Options"
        val grammar = """
            namespace example
            grammar Keywords {
                #skipRequiredBetweenTokens: false
                skip leaf WS = "\s+" ;
                S = 'if' ID ;
                leaf ID = "[a-z]+" ;
            }
        """
        val expected = """
            S {
              'if'
              ID : 'x'
            }
        """.trimIndent()
        assertEquals(expected, tree(grammar, "ifx"))
    }

    @Test
    fun preference() {
        // grammar.md: "Preference rules"
        val grammar = """
            namespace example
            grammar Expr {
                skip leaf WS = "\s+" ;
                E = v | add | div ;
                add = E '+' E ;
                div = E '/' E ;
                leaf v = "[a-z]+" ;

                preference E {
                    add on '+' left
                    div on '/' left
                }
            }
        """
        val expected = """
            E { add {
              E { v : 'a' }
              '+'
              E { div {
                E { v : 'b' }
                '/'
                E { v : 'c' }
              } }
            } }
        """.trimIndent()
        assertEquals(expected, tree(grammar, "a+b/c"))
    }

    @Test
    fun embedded() {
        // grammar.md: "Embedded grammars"
        val grammar = """
            namespace example
            grammar Numbers {
                skip leaf WS = "\s+" ;
                list = [NUM / ',']+ ;
                leaf NUM = "[0-9]+" ;
            }
            grammar Doc {
                skip leaf WS = "\s+" ;
                doc = 'nums' Numbers::list ';' ;
            }
        """
        val result = processor(grammar).process("nums 1, 2 ;")
        assertTrue(result.allIssues.errors.isEmpty(), result.allIssues.toString())
        val expected = """
            :Doc {
              list = :List {
                num = [
                  '1'
                  '2'
                ]
              }
            }
        """.trimIndent()
        assertEquals(expected, result.asm!!.asString().trim())
    }

    @Test
    fun extends_and_override() {
        // grammar.md: "Extending a grammar"
        val grammar = """
            namespace example
            grammar Base {
                skip leaf WS = "\s+" ;
                S = item+ ;
                item = 'a' ;
            }
            grammar Ext : Base {
                #defaultGoalRule: S
                override item +=| 'b' ;
            }
        """
        val p = processor(grammar)
        assertTrue(p.parse("a b a").issues.errors.isEmpty())

        val grammar2 = """
            namespace example
            grammar Base {
                skip leaf WS = "\s+" ;
                S = item+ ;
                item = 'a' ;
            }
            grammar Ext : Base {
                #defaultGoalRule: S
                override item = 'b' ;
            }
        """
        val p2 = processor(grammar2)
        assertTrue(p2.parse("b b").issues.errors.isEmpty())
        assertTrue(p2.parse("a").issues.errors.isNotEmpty())
    }

    @Test
    fun goal_rule() {
        // grammar.md: "Options"
        val grammar = """
            namespace example
            grammar Goals {
                #defaultGoalRule: two
                one = 'a' ;
                two = 'b' ;
            }
        """
        val p = processor(grammar)
        assertTrue(p.parse("b").issues.errors.isEmpty())
        // any rule can be the goal for one parse
        assertTrue(p.parse("a", Agl.parseOptions { goalRuleName("one") }).issues.errors.isEmpty())
    }
}
