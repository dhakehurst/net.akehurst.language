/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.sentence.api.InputLocation
import testFixture.utils.parseError
import kotlin.test.*

class test_Agl_processorFromStringSimple_parse {

    private companion object {
        fun test_parse(grammarStr: String, sentence: String, expectedStr: String) {
            val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
            assertTrue(pr.issues.isEmpty(), pr.issues.toString())
            assertNotNull(pr.processor)

            val result = pr.processor!!.parse(sentence)
            assertTrue(result.issues.isEmpty(), result.issues.toString())

            val expected = pr.processor!!.spptParser.parse(expectedStr)
            assertEquals(expected.toStringAll, result.sppt?.toStringAll)
            //TODO: assertEquals(expected, result.sppt)
        }

        fun test_parse_fails(grammarStr: String, sentence: String, expected: Set<LanguageIssue>) {
            val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
            assertTrue(pr.issues.isEmpty(), pr.issues.toString())
            assertNotNull(pr.processor)

            val result = pr.processor!!.parse(sentence)
            assertEquals(null, result.sppt)
            assertEquals(expected, result.issues.all)
        }
    }

    @BeforeTest
    fun before() {
        //Agl.registry.agl.grammar.processor?.buildFor()
    }

    @Test
    fun single_line_comment_at_start() {
        val grammarStr = """
            // single line comment
            namespace test
            grammar Test {
                a = 'a' ;
            }
        """.trimIndent()

        test_parse(grammarStr, "a", "a { 'a' }")
    }

    @Test
    fun rule_empty() {
        val grammarStr = """
            namespace test
            grammar Test {
                a = ;
            }
        """.trimIndent()

        test_parse(grammarStr, "", "a { <EMPTY> }")
    }

    @Test
    fun literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                a = 'a' ;
            }
        """.trimIndent()

        test_parse(grammarStr, "a", "a { 'a' }")
    }

    @Test
    fun literal_empty_fails() {

        val grammarStr = """
            namespace test
            grammar Test {
                a = '' ;
            }
        """.trimIndent()

        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertNull(pr.processor, pr.issues.toString())
    }

    @Test
    fun escapeSequence() {

        val grammarStr = """
            namespace test
            grammar Test {
                EscapeSequence = '\\' "[btnfr'\\\\]" ;
            }
        """.trimIndent()

        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertNotNull(pr.processor)

        val result = pr.processor!!.parse("\\b");
        val expected = pr.processor!!.spptParser.parse(
            """
             EscapeSequence {
                '\\'
                "[btnfr'\\\\]" : 'b'
             }
        """
        )
        assertEquals(expected.toStringAll, result.sppt?.toStringAll)
        assertEquals(expected, result.sppt)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun pattern() {

        val grammarStr = """
            namespace test
            grammar Test {
                a = "[a-c]" ;
            }
        """.trimIndent()

        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertNotNull(pr.processor)

        val result = pr.processor!!.parse("b");
        val expected = pr.processor!!.spptParser.parse(
            """
             a {
                "[a-c]":'b'
             }
        """
        )
        assertEquals(expected.toStringAll, result.sppt?.toStringAll)
        assertEquals(expected, result.sppt)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun singleQuoteLiteral() {

        val grammarStr = """
            namespace test
            grammar Test {
                a = '\'' ;
            }
        """.trimIndent()

        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertNotNull(pr.processor)

        val result = pr.processor!!.parse("'");
        val expected = pr.processor!!.spptParser.parse(
            """
             a {
                '\''
             }
        """
        )
        assertEquals(expected.toStringAll, result.sppt?.toStringAll)
        assertEquals(expected, result.sppt)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun doubleQuoteLiteral() {

        val grammarStr = """
            namespace test
            grammar Test {
                a = '"' ;
            }
        """.trimIndent()

        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertNotNull(pr.processor)

        val result = pr.processor!!.parse("\"");
        val expected = pr.processor!!.spptParser.parse(
            """
             a {
                '"'
             }
        """
        )
        assertEquals(expected.toStringAll, result.sppt?.toStringAll)
        assertEquals(expected, result.sppt)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun singleQuotePattern() {

        val grammarStr = """
            namespace test
            grammar Test {
                a = "'" ;
            }
        """.trimIndent()

        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertNotNull(pr.processor)

        val result = pr.processor!!.parse("'");
        val expected = pr.processor!!.spptParser.parse(
            """
             a {
                "'":'\''
             }
        """
        )
        assertEquals(expected.toStringAll, result.sppt?.toStringAll)
        assertEquals(expected, result.sppt)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun doubleQuotePattern() {

        val grammarStr = """
            namespace test
            grammar Test {
                a = "\"" ;
            }
        """.trimIndent()

        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertNotNull(pr.processor)

        val result = pr.processor!!.parse("\"");
        val expected = pr.processor!!.spptParser.parse(
            """
             a {
                "\"" : '"'
             }
        """
        )
        assertEquals(expected.toStringAll, result.sppt?.toStringAll)
        assertEquals(expected, result.sppt)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun leaf() {

        val grammarStr = """
            namespace test
            grammar Test {
               leaf a = 'a' ;
            }
        """.trimIndent()

        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertNotNull(pr.processor)

        val result = pr.processor!!.parse("a");
        val expected = pr.processor!!.spptParser.parse(
            """
             a:'a'
        """
        )
        assertEquals(expected.toStringAll, result.sppt?.toStringAll)
        assertEquals(expected, result.sppt)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun skip_leaf() {

        val grammarStr = """
            namespace test
            grammar Test {
               skip leaf a = 'a' ;
               b = 'b' ;
            }
        """.trimIndent()

        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertNotNull(pr.processor)

        val result = pr.processor!!.parse("ba");
        val expected = pr.processor!!.spptParser.parse(
            """
             b{ 'b' a : 'a' }
        """
        )
        assertEquals(expected.toStringAll, result.sppt?.toStringAll)
//        assertEquals(expected, result.sppt)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun literal_concatenation() {

        val grammarStr = """
            namespace test
            grammar Test {
                a = 'a' 'b' 'c' ;
            }
        """.trimIndent()

        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertNotNull(pr.processor)

        val result = pr.processor!!.parse("abc");
        val expected = pr.processor!!.spptParser.parse(
            """
             a { 'a' 'b' 'c' }
        """
        )
        assertEquals(expected.toStringAll, result.sppt?.toStringAll)
        assertEquals(expected, result.sppt)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun literal_choice_equal() {

        val grammarStr = """
            namespace test
            grammar Test {
                abc = 'a' | 'b' | 'c' ;
            }
        """.trimIndent()

        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertNotNull(pr.processor)

        val result = pr.processor!!.parse("b");
        val expected = pr.processor!!.spptParser.parse(
            """
             abc|1 { 'b' }
        """
        )
        assertEquals(expected.toStringAll, result.sppt?.toStringAll)
//        assertEquals(expected, result.sppt)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun literal_choice_priority() {

        val grammarStr = """
            namespace test
            grammar Test {
                abc = 'a' < 'b' < 'c' ;
            }
        """.trimIndent()

        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertNotNull(pr.processor)

        val result = pr.processor!!.parse("b");
        val expected = pr.processor!!.spptParser.parse(
            """
             abc|1 { 'b' }
        """
        )
        assertEquals(expected.toStringAll, result.sppt?.toStringAll)
//        assertEquals(expected, result.sppt)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun nonTerminal_choice_priority() {

        val grammarStr = """
            namespace test
            grammar Test {
                choice = a < b < c < d < e < f < g;
                a = 'a' ;
                b = 'b' ;
                c = 'c' ;
                d = 'd' ;
                e = 'e' ;
                f = 'f' ;
                g = 'g' ;
            }
        """.trimIndent()

        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertNotNull(pr.processor)

        val result = pr.processor!!.parse("f");
        val expected = pr.processor!!.spptParser.parse(
            """
             choice|5 { f { 'f' } }
        """
        )
        assertEquals(expected.toStringAll, result.sppt?.toStringAll)
//        assertEquals(expected, result.sppt)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun literal_optional() {

        val grammarStr = """
            namespace test
            grammar Test {
                a = 'a'? ;
            }
        """.trimIndent()

        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertNotNull(pr.processor)

        val result1 = pr.processor!!.parse("");
        val expected1 = pr.processor!!.spptParser.parse(
            """
             a { §empty }
        """
        )
        assertEquals(expected1.toStringAll, result1.sppt?.toStringAll)
//        assertTrue(expected1.treeData.matches(result1.sppt!!.treeData))
        assertTrue(result1.issues.isEmpty())

        val result2 = pr.processor!!.parse("a");
        val expected2 = pr.processor!!.spptParser.parse(
            """
             a { 'a' }
        """
        )
        assertEquals(expected2.toStringAll, result2.sppt?.toStringAll)
//        assertEquals(expected2, result2.sppt)
        assertTrue(result2.issues.isEmpty())
    }

    @Test
    fun literal_multi_0_n() {

        val grammarStr = """
            namespace test
            grammar Test {
                a = 'a'* ;
            }
        """.trimIndent()

        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertNotNull(pr.processor)

        val result1 = pr.processor!!.parse("");
        val expected1 = pr.processor!!.spptParser.parse(
            """
             a { <EMPTY_LIST> }
        """
        )
        assertEquals(expected1.toStringAll, result1.sppt?.toStringAll)
        assertEquals(expected1, result1.sppt)
        assertTrue(result1.issues.isEmpty())

        val result2 = pr.processor!!.parse("a");
        val expected2 = pr.processor!!.spptParser.parse(
            """
             a { 'a' }
        """
        )
        assertEquals(expected2.toStringAll, result2.sppt?.toStringAll)
        assertEquals(expected2, result2.sppt)
        assertTrue(result2.issues.isEmpty())

        val result3 = pr.processor!!.parse("aaa");
        val expected3 = pr.processor!!.spptParser.parse(
            """
             a { 'a' 'a' 'a' }
        """
        )
        assertEquals(expected3.toStringAll, result3.sppt?.toStringAll)
        assertEquals(expected3, result3.sppt)
        assertTrue(result3.issues.isEmpty())
    }

    @Test
    fun literal_multi_1_n() {

        val grammarStr = """
            namespace test
            grammar Test {
                a = 'a'+ ;
            }
        """.trimIndent()

        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertNotNull(pr.processor)

        val sentence1 = ""
        val result1 = pr.processor!!.parse(sentence1)
        assertEquals(null, result1.sppt)
        val expIssues1 = setOf(parseError(InputLocation(0, 1, 1, 1, null), sentence1, setOf("<GOAL>"), setOf("'a'")))
        assertEquals(expIssues1, result1.issues.all)

        val result2 = pr.processor!!.parse("a");
        val expected2 = pr.processor!!.spptParser.parse(
            """
             a { 'a' }
        """
        )
        assertEquals(expected2.toStringAll, result2.sppt?.toStringAll)
        assertEquals(expected2, result2.sppt)
        assertTrue(result2.issues.isEmpty())

        val result3 = pr.processor!!.parse("aaa");
        val expected3 = pr.processor!!.spptParser.parse(
            """
             a { 'a' 'a' 'a' }
        """
        )
        assertEquals(expected3.toStringAll, result3.sppt?.toStringAll)
        assertEquals(expected3, result3.sppt)
        assertTrue(result3.issues.isEmpty())
    }

    @Test
    fun literal_multi_range_fixed() {

        val grammarStr = """
            namespace test
            grammar Test {
                a = 'a'4 ;
            }
        """.trimIndent()

        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertNotNull(pr.processor)

        val result0 = pr.processor!!.parse("")
        assertEquals(null, result0.sppt)
        val expIssues0 = setOf(parseError(InputLocation(0, 1, 1, 1, null), "", setOf("<GOAL>"), setOf("'a'")))
        assertEquals(expIssues0, result0.issues.all)

        val result1 = pr.processor!!.parse("a")
        assertEquals(null, result1.sppt)
        val expIssues1 = setOf(parseError(InputLocation(1, 2, 1, 1, null), "a", setOf("<GOAL>"), setOf("'a'")))
        assertEquals(expIssues1, result1.issues.all)

        val result2 = pr.processor!!.parse("aa");
        assertEquals(null, result2.sppt)
        val expIssues2 = setOf(parseError(InputLocation(2, 3, 1, 1, null), "aa", setOf("'a'"), setOf("'a'")))
        assertEquals(expIssues2, result2.issues.all)

        val result3 = pr.processor!!.parse("aaa");
        assertEquals(null, result3.sppt)
        val expIssues3 = setOf(parseError(InputLocation(3, 4, 1, 1, null), "aaa", setOf("'a'"), setOf("'a'")))
        assertEquals(expIssues3, result3.issues.all)

        val result4 = pr.processor!!.parse("aaaa");
        val expected4 = pr.processor!!.spptParser.parse(
            """
             a { 'a' 'a' 'a' 'a' }
        """
        )
        assertNotNull(result4.sppt, result4.issues.joinToString(separator = "\n") { "$it" })
        assertEquals(expected4.toStringAll, result4.sppt?.toStringAll)
        assertEquals(expected4, result4.sppt)
        assertTrue(result4.issues.isEmpty())

        val result5 = pr.processor!!.parse("aaaaa");
        assertEquals(null, result5.sppt)
        val expIssues5 = listOf(parseError(InputLocation(4, 5, 1, 1, null), "aaaaa", setOf("'a'"),setOf("<EOT>")))
        assertEquals(expIssues5, result5.issues.errors)
    }

    @Test
    fun literal_multi_range_unbraced_unbounded() {

        val grammarStr = """
            namespace test
            grammar Test {
                a = 'a'2+ ;
            }
        """.trimIndent()

        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertNotNull(pr.processor)

        val result0 = pr.processor!!.parse("")
        assertEquals(null, result0.sppt)
        val expIssues0 = setOf(parseError(InputLocation(0, 1, 1, 1, null), "",setOf("<GOAL>"), setOf("'a'")))
        assertEquals(expIssues0, result0.issues.all)

        val result1 = pr.processor!!.parse("a")
        assertEquals(null, result1.sppt)
        val expIssues1 = setOf(parseError(InputLocation(1, 2, 1, 1, null), "a", setOf("<GOAL>"),setOf("'a'")))
        assertEquals(expIssues1, result1.issues.all)

        val result2 = pr.processor!!.parse("aa");
        val expected2 = pr.processor!!.spptParser.parse(
            """
             a { 'a' 'a' }
        """
        )
        assertNotNull(result2.sppt, result2.issues.joinToString(separator = "\n") { "$it" })
        assertEquals(expected2.toStringAll, result2.sppt?.toStringAll)
        assertEquals(expected2, result2.sppt)
        assertTrue(result2.issues.isEmpty())

        val result3 = pr.processor!!.parse("aaa");
        val expected3 = pr.processor!!.spptParser.parse(
            """
             a { 'a' 'a' 'a' }
        """
        )
        assertNotNull(result3.sppt, result2.issues.joinToString(separator = "\n") { "$it" })
        assertEquals(expected3.toStringAll, result3.sppt?.toStringAll)
        assertEquals(expected3, result3.sppt)
        assertTrue(result3.issues.isEmpty())
    }

    @Test
    fun literal_multi_range_braced_unbounded() {

        val grammarStr = """
            namespace test
            grammar Test {
                a = 'a'{2+} ;
            }
        """.trimIndent()

        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertNotNull(pr.processor)

        val result0 = pr.processor!!.parse("")
        assertEquals(null, result0.sppt)
        val expIssues0 = setOf(parseError(InputLocation(0, 1, 1, 1, null), "", setOf("<GOAL>"),setOf("'a'")))
        assertEquals(expIssues0, result0.issues.all)

        val result1 = pr.processor!!.parse("a")
        assertEquals(null, result1.sppt)
        val expIssues1 = setOf(parseError(InputLocation(1, 2, 1, 1, null), "a", setOf("<GOAL>"),setOf("'a'")))
        assertEquals(expIssues1, result1.issues.all)

        val result2 = pr.processor!!.parse("aa");
        val expected2 = pr.processor!!.spptParser.parse(
            """
             a { 'a' 'a' }
        """
        )
        assertNotNull(result2.sppt, result2.issues.joinToString(separator = "\n") { "$it" })
        assertEquals(expected2.toStringAll, result2.sppt?.toStringAll)
        assertEquals(expected2, result2.sppt)
        assertTrue(result2.issues.isEmpty())

        val result3 = pr.processor!!.parse("aaa");
        val expected3 = pr.processor!!.spptParser.parse(
            """
             a { 'a' 'a' 'a' }
        """
        )
        assertNotNull(result3.sppt, result2.issues.joinToString(separator = "\n") { "$it" })
        assertEquals(expected3.toStringAll, result3.sppt?.toStringAll)
        assertEquals(expected3, result3.sppt)
        assertTrue(result3.issues.isEmpty())
    }

    @Test
    fun literal_multi_range_unbraced_bounded() {

        val grammarStr = """
            namespace test
            grammar Test {
                a = 'a'2..5 ;
            }
        """.trimIndent()

        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertNotNull(pr.processor)

        val result0 = pr.processor!!.parse("")
        assertEquals(null, result0.sppt)
        val expIssues0 = setOf(parseError(InputLocation(0, 1, 1, 1, null), "", setOf("<GOAL>"),setOf("'a'")))
        assertEquals(expIssues0, result0.issues.all)

        val result1 = pr.processor!!.parse("a")
        assertEquals(null, result1.sppt)
        val expIssues1 = setOf(parseError(InputLocation(1, 2, 1, 1, null), "a", setOf("<GOAL>"),setOf("'a'")))
        assertEquals(expIssues1, result1.issues.all)

        val result2 = pr.processor!!.parse("aa");
        val expected2 = pr.processor!!.spptParser.parse(
            """
             a { 'a' 'a' }
        """
        )
        assertNotNull(result2.sppt, result2.issues.joinToString(separator = "\n") { "$it" })
        assertEquals(expected2.toStringAll, result2.sppt?.toStringAll)
        assertEquals(expected2, result2.sppt)
        assertTrue(result2.issues.isEmpty())

        val result3 = pr.processor!!.parse("aaa");
        val expected3 = pr.processor!!.spptParser.parse(
            """
             a {'a' 'a' 'a' }
        """
        )
        assertNotNull(result3.sppt, result2.issues.joinToString(separator = "\n") { "$it" })
        assertEquals(expected3.toStringAll, result3.sppt?.toStringAll)
        assertEquals(expected3, result3.sppt)
        assertTrue(result3.issues.isEmpty())
    }

    @Test
    fun literal_multi_range_braced_bounded() {

        val grammarStr = """
            namespace test
            grammar Test {
                a = 'a'{2..5} ;
            }
        """.trimIndent()

        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertNotNull(pr.processor)

        val result0 = pr.processor!!.parse("")
        assertEquals(null, result0.sppt)
        val expIssues0 = setOf(parseError(InputLocation(0, 1, 1, 1, null), "", setOf("<GOAL>"), setOf("'a'")))
        assertEquals(expIssues0, result0.issues.all)

        val result1 = pr.processor!!.parse("a")
        assertEquals(null, result1.sppt)
        val expIssues1 = setOf(parseError(InputLocation(1, 2, 1, 1, null), "a", setOf("<GOAL>"), setOf("'a'")))
        assertEquals(expIssues1, result1.issues.all)

        val result2 = pr.processor!!.parse("aa")
        val expected2 = pr.processor!!.spptParser.parse(
            """
             a { 'a' 'a' }
        """
        )
        assertNotNull(result2.sppt, result2.issues.joinToString(separator = "\n") { "$it" })
        assertEquals(expected2.toStringAll, result2.sppt?.toStringAll)
        assertEquals(expected2, result2.sppt)
        assertTrue(result2.issues.isEmpty())

        val result3 = pr.processor!!.parse("aaa");
        val expected3 = pr.processor!!.spptParser.parse(
            """
             a { 'a' 'a' 'a' }
        """
        )
        assertNotNull(result3.sppt, result2.issues.joinToString(separator = "\n") { "$it" })
        assertEquals(expected3.toStringAll, result3.sppt?.toStringAll)
        assertEquals(expected3, result3.sppt)
        assertTrue(result3.issues.isEmpty())
    }

    @Test
    fun nonTerminal_concatenation() {

        val grammarStr = """
            namespace test
            grammar Test {
                S = a b c ;
                a = 'a' ;
                b = 'b' ;
                c = 'c' ;
            }
        """.trimIndent()

        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
        val p2 = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertNotNull(pr.processor)

        val result1 = pr.processor!!.parse("abc");
        val expected1 = pr.processor!!.spptParser.parse(
            """
             S { a {'a'} b{'b'} c{'c'} }
        """
        )
        assertEquals(expected1.toStringAll, result1.sppt?.toStringAll)
        assertEquals(expected1, result1.sppt)
        assertTrue(result1.issues.isEmpty())
    }

    @Test
    fun nonTerminal_optional() {

        val grammarStr = """
            namespace test
            grammar Test {
                S = a ? ;
                a = 'a' ;
            }
        """.trimIndent()

        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertNotNull(pr.processor)

        val result1 = pr.processor!!.parse("");
        val expected1 = pr.processor!!.spptParser.parse(
            """
             S {  §empty }
        """
        )
        assertEquals(expected1.toStringAll, result1.sppt?.toStringAll)
//        assertEquals(expected1, result1.sppt)
        assertTrue(result1.issues.isEmpty())

        val result2 = pr.processor!!.parse("a");
        val expected2 = pr.processor!!.spptParser.parse(
            """
             S { a{'a'} }
        """
        )
        assertEquals(expected2.toStringAll, result2.sppt?.toStringAll)
        assertEquals(expected2, result2.sppt)
        assertTrue(result2.issues.isEmpty())
    }

    @Test
    fun concat_nonTerminal_optional() {

        val grammarStr = """
            namespace test
            grammar Test {
                S = 'b' a? ;
                a = 'a' ;
            }
        """.trimIndent()

        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertNotNull(pr.processor)

        val result1 = pr.processor!!.parse("b");
        val expected1 = pr.processor!!.spptParser.parse(
            """
             S { 'b' §S§opt1 { <EMPTY> } }
        """
        )
        assertEquals(expected1.toStringAll, result1.sppt?.toStringAll)
//        assertEquals(expected1, result1.sppt)
        assertTrue(result1.issues.isEmpty())

        val result2 = pr.processor!!.parse("ba");
        val expected2 = pr.processor!!.spptParser.parse(
            """
             S { 'b' §S§opt1 { a{'a'} } }
        """
        )
        assertEquals(expected2.toStringAll, result2.sppt?.toStringAll)
        assertEquals(expected2, result2.sppt)
        assertTrue(result2.issues.isEmpty())
    }

    @Test
    fun concat_nonTerminal_optA_optB_optC() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = A? B? C? ;
                A = 'a' ;
                B = 'b' ;
                C = 'c' ;
            }
        """.trimIndent()

        test_parse(grammarStr, "", "S { §S§opt1 { <EMPTY> } §S§opt2 { <EMPTY> } §S§opt3 { <EMPTY> }}")
        test_parse(grammarStr, "a", "S { §S§opt1 { A {'a'} } §S§opt2 { <EMPTY> } §S§opt3 { <EMPTY> }}")
        test_parse(grammarStr, "b", "S { §S§opt1 { <EMPTY> } §S§opt2 { B {'b'} } §S§opt3 { <EMPTY> }}")
        test_parse(grammarStr, "c", "S { §S§opt1 { <EMPTY> } §S§opt2 { <EMPTY> } §S§opt3 { C {'c'} }}")
        test_parse(grammarStr, "ab", "S { §S§opt1 { A {'a'} } §S§opt2 {B {'b'} } §S§opt3 { <EMPTY> }}")
        test_parse(grammarStr, "ac", "S { §S§opt1 { A {'a'} } §S§opt2 { <EMPTY> } §S§opt3 { C {'c'} }}")
        test_parse(grammarStr, "bc", "S { §S§opt1 { <EMPTY> } §S§opt2 { B {'b'} } §S§opt3 { C {'c'} }}")
        test_parse(grammarStr, "abc", "S { §S§opt1 { A {'a'} } §S§opt2 { B {'b'} } §S§opt3 { C {'c'} }}")
    }

    @Test
    fun nonTerminal_multi_0_n() {

        val grammarStr = """
            namespace test
            grammar Test {
                S = a* ;
                a = 'a' ;
            }
        """.trimIndent()

        //NOTE: there should be no pseudo rule because there is only one item on rhs of rule 'S'

        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertNotNull(pr.processor)

        val result1 = pr.processor!!.parse("");
        val expected1 = pr.processor!!.spptParser.parse(
            """
             S { <EMPTY_LIST> }
        """
        )
        assertEquals(expected1.toStringAll, result1.sppt?.toStringAll)
//        assertEquals(expected1, result1.sppt)
        assertTrue(result1.issues.isEmpty())

        val result2 = pr.processor!!.parse("a");
        val expected2 = pr.processor!!.spptParser.parse(
            """
             S { a{'a'} }
        """
        )
        assertEquals(expected2.toStringAll, result2.sppt?.toStringAll)
        //       assertEquals(expected2, result2.sppt)
        assertTrue(result2.issues.isEmpty())

        val result3 = pr.processor!!.parse("aaa");
        val expected3 = pr.processor!!.spptParser.parse(
            """
             S { a{'a'} a{'a'} a{'a'} }
        """
        )
        assertEquals(expected3.toStringAll, result3.sppt?.toStringAll)
        assertEquals(expected3, result3.sppt)
        assertTrue(result3.issues.isEmpty())
    }

    @Test
    fun nonTerminal_multi_1_n() {

        val grammarStr = """
            namespace test
            grammar Test {
                S = a+ ;
                a = 'a' ;
            }
        """.trimIndent()

        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertNotNull(pr.processor)

        val result1 = pr.processor!!.parse("")
        assertEquals(null, result1.sppt)
        assertEquals(
            setOf(
                parseError(InputLocation(0, 1, 1, 1, null), "",setOf("<GOAL>"), setOf("'a'"))
            ), result1.issues.all
        )

        val result2 = pr.processor!!.parse("a");
        val expected2 = pr.processor!!.spptParser.parse(
            """
             S { a{'a'} } 
        """
        )
        assertEquals(expected2.toStringAll, result2.sppt?.toStringAll)
        assertEquals(expected2, result2.sppt)
        assertTrue(result2.issues.isEmpty())

        val result3 = pr.processor!!.parse("aaa");
        val expected3 = pr.processor!!.spptParser.parse(
            """
             S { a{'a'} a{'a'} a{'a'} } 
        """
        )
        assertEquals(expected3.toStringAll, result3.sppt?.toStringAll)
        assertEquals(expected3, result3.sppt)
        assertTrue(result3.issues.isEmpty())
    }

    @Test
    fun nonTerminal_multi_range_unbraced_unbounded() {

        val grammarStr = """
            namespace test
            grammar Test {
                S = a 2+ ;
                a = 'a' ;
            }
        """.trimIndent()

        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertNotNull(pr.processor)

        val result0 = pr.processor!!.parse("")
        assertEquals(null, result0.sppt)
        assertEquals(
            setOf(
                parseError(InputLocation(0, 1, 1, 1, null), "",setOf("<GOAL>"), setOf("'a'"))
            ), result0.issues.all
        )

        val result1 = pr.processor!!.parse("a")
        assertEquals(null, result1.sppt)
        assertEquals(
            setOf(
                parseError(InputLocation(1, 2, 1, 1, null), "a",setOf("<GOAL>"), setOf("'a'"))
            ), result1.issues.all
        )

        val result2 = pr.processor!!.parse("aa");
        val expected2 = pr.processor!!.spptParser.parse(
            """
             S {  a{'a'} a{'a'} }
        """
        )
        assertEquals(expected2.toStringAll, result2.sppt?.toStringAll)
        assertEquals(expected2, result2.sppt)
        assertTrue(result2.issues.isEmpty())

        val result4 = pr.processor!!.parse("aaaa");
        val expected4 = pr.processor!!.spptParser.parse(
            """
             S { a{'a'} a{'a'} a{'a'} a{'a'} }
        """
        )
        assertEquals(expected4.toStringAll, result4.sppt?.toStringAll)
        assertEquals(expected4, result4.sppt)
        assertTrue(result4.issues.isEmpty())
    }

    @Test
    fun nonTerminal_multi_range_unbraced_bounded() {

        val grammarStr = """
            namespace test
            grammar Test {
                S = a 2..5;
                a = 'a' ;
            }
        """.trimIndent()

        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertNotNull(pr.processor)

        val result0 = pr.processor!!.parse("")
        assertEquals(null, result0.sppt)
        assertEquals(
            setOf(
                parseError(InputLocation(0, 1, 1, 1, null), "",setOf("<GOAL>"), setOf("'a'"))
            ), result0.issues.all
        )

        val result1 = pr.processor!!.parse("a")
        assertEquals(null, result1.sppt)
        assertEquals(
            setOf(
                parseError(InputLocation(1, 2, 1, 1, null), "a",setOf("<GOAL>"), setOf("'a'"))
            ), result1.issues.all
        )

        val result2 = pr.processor!!.parse("aa");
        val expected2 = pr.processor!!.spptParser.parse(
            """
             S { a{'a'} a{'a'} }
        """
        )
        assertEquals(expected2.toStringAll, result2.sppt?.toStringAll)
        assertEquals(expected2, result2.sppt)
        assertTrue(result2.issues.isEmpty())

        val result5 = pr.processor!!.parse("aaaaa");
        val expected5 = pr.processor!!.spptParser.parse(
            """
             S { a{'a'} a{'a'} a{'a'} a{'a'} a{'a'} }
        """
        )
        assertEquals(expected5.toStringAll, result5.sppt?.toStringAll)
        assertEquals(expected5, result5.sppt)
        assertTrue(result5.issues.isEmpty())

        val result6 = pr.processor!!.parse("aaaaaa")
        assertEquals(null, result6.sppt)
        assertEquals(
            setOf(
                parseError(InputLocation(5, 6, 1, 1, null), "aaaaaa", setOf("a"),setOf("<EOT>"))
            ), result6.issues.all
        )

    }

    @Test
    fun nonTerminal_multi_range_braced_unbounded() {

        val grammarStr = """
            namespace test
            grammar Test {
                S = a{2+} ;
                a = 'a' ;
            }
        """.trimIndent()

        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertNotNull(pr.processor)

        val result0 = pr.processor!!.parse("")
        assertEquals(null, result0.sppt)
        assertEquals(
            setOf(
                parseError(InputLocation(0, 1, 1, 1, null), "", setOf("<GOAL>"),setOf("'a'"))
            ), result0.issues.all
        )

        val result1 = pr.processor!!.parse("a")
        assertEquals(null, result1.sppt)
        assertEquals(
            setOf(
                parseError(InputLocation(1, 2, 1, 1, null), "a",setOf("<GOAL>"), setOf("'a'"))
            ), result1.issues.all
        )

        val result2 = pr.processor!!.parse("aa");
        val expected2 = pr.processor!!.spptParser.parse(
            """
             S { a{'a'} a{'a'} }
        """
        )
        assertEquals(expected2.toStringAll, result2.sppt?.toStringAll)
        assertEquals(expected2, result2.sppt)
        assertTrue(result2.issues.isEmpty())

        val result4 = pr.processor!!.parse("aaaa");
        val expected4 = pr.processor!!.spptParser.parse(
            """
             S {a{'a'} a{'a'} a{'a'} a{'a'} }
        """
        )
        assertEquals(expected4.toStringAll, result4.sppt?.toStringAll)
        assertEquals(expected4, result4.sppt)
        assertTrue(result4.issues.isEmpty())
    }

    @Test
    fun nonTerminal_multi_range_braced_bounded() {

        val grammarStr = """
            namespace test
            grammar Test {
                S = a{2..5};
                a = 'a' ;
            }
        """.trimIndent()

        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertNotNull(pr.processor)

        val result0 = pr.processor!!.parse("")
        assertEquals(null, result0.sppt)
        assertEquals(
            setOf(
                parseError(InputLocation(0, 1, 1, 1, null), "",setOf("<GOAL>"), setOf("'a'"))
            ), result0.issues.all
        )

        val result1 = pr.processor!!.parse("a")
        assertEquals(null, result1.sppt)
        assertEquals(
            setOf(
                parseError(InputLocation(1, 2, 1, 1, null), "a", setOf("<GOAL>"),setOf("'a'"))
            ), result1.issues.all
        )

        val result2 = pr.processor!!.parse("aa");
        val expected2 = pr.processor!!.spptParser.parse(
            """
             S {a{'a'} a{'a'} }
        """
        )
        assertEquals(expected2.toStringAll, result2.sppt?.toStringAll)
        assertEquals(expected2, result2.sppt)
        assertTrue(result2.issues.isEmpty())

        val result5 = pr.processor!!.parse("aaaaa");
        val expected5 = pr.processor!!.spptParser.parse(
            """
             S { a{'a'} a{'a'} a{'a'} a{'a'} a{'a'} }
        """
        )
        assertEquals(expected5.toStringAll, result5.sppt?.toStringAll)
        assertEquals(expected5, result5.sppt)
        assertTrue(result5.issues.isEmpty())

        val result6 = pr.processor!!.parse("aaaaaa")
        assertEquals(null, result6.sppt)
        assertEquals(
            setOf(
                parseError(InputLocation(5, 6, 1, 1, null), "aaaaaa",setOf("a"), setOf("<EOT>"))
            ), result6.issues.all
        )

    }

    @Test
    fun nonTerminal_slist_0_1() {

        val grammarStr = """
            namespace test
            grammar Test {
                S = [ a / ',' ]? ;
                a = 'a' ;
            }
        """.trimIndent()

        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertNotNull(pr.processor)

        val result0 = pr.processor!!.parse("");
        val expected0 = pr.processor!!.spptParser.parse(
            """
             S { <EMPTY_LIST> }
        """
        )
        assertEquals(expected0.toStringAll, result0.sppt?.toStringAll)
        //assertEquals(expected0, result0.sppt)
        assertTrue(result0.issues.isEmpty())

        val result1 = pr.processor!!.parse("a");
        val expected1 = pr.processor!!.spptParser.parse(
            """
             S {a{'a'} }
        """
        )
        assertEquals(expected1.toStringAll, result1.sppt?.toStringAll)
        assertEquals(expected1, result1.sppt)
        assertTrue(result1.issues.isEmpty())

        val result2 = pr.processor!!.parse("aa")
        assertEquals(null, result2.sppt)
        assertEquals(
            setOf(
                parseError(InputLocation(1, 2, 1, 1, null), "aa",setOf("<GOAL>"), setOf("<EOT>"))
            ), result2.issues.all
        )
    }

    @Test
    fun nonTerminal_slist_0_n() {

        val grammarStr = """
            namespace test
            grammar Test {
                S = [ a  / ',' ]* ;
                a = 'a' ;
            }
        """.trimIndent()

        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertNotNull(pr.processor)

        val result0 = pr.processor!!.parse("")
        val expected0 = pr.processor!!.spptParser.parse(
            """
             S { <EMPTY_LIST> }
        """
        )
        assertEquals(expected0.toStringAll, result0.sppt?.toStringAll)
        //assertEquals(expected0, result0.sppt)
        assertTrue(result0.issues.isEmpty())

        val result1 = pr.processor!!.parse("a")
        val expected1 = pr.processor!!.spptParser.parse(
            """
             S { a{'a'} }
        """
        )
        assertEquals(expected1.toStringAll, result1.sppt?.toStringAll)
        assertEquals(expected1, result1.sppt)
        assertTrue(result1.issues.isEmpty())

        val result3 = pr.processor!!.parse("a,a,a");
        val expected3 = pr.processor!!.spptParser.parse(
            """
             S { a{'a'} ',' a{'a'} ',' a{'a'} }
        """
        )
        assertEquals(expected3.toStringAll, result3.sppt?.toStringAll)
        assertEquals(expected3, result3.sppt)
        assertTrue(result3.issues.isEmpty())
    }

    @Test
    fun nonTerminal_slist_1_n() {

        val grammarStr = """
            namespace test
            grammar Test {
                S = [ a  / ',' ]+ ;
                a = 'a' ;
            }
        """.trimIndent()

        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertNotNull(pr.processor)

        val result0 = pr.processor!!.parse("")
        assertEquals(null, result0.sppt)
        assertEquals(
            setOf(
                parseError(InputLocation(0, 1, 1, 1, null), "", setOf("<GOAL>"),setOf("'a'"))
            ), result0.issues.all
        )

        val result1 = pr.processor!!.parse("a")
        val expected1 = pr.processor!!.spptParser.parse(
            """
             S { a{'a'} }
        """
        )
        assertEquals(expected1.toStringAll, result1.sppt?.toStringAll)
        assertEquals(expected1, result1.sppt)
        assertTrue(result1.issues.isEmpty())

        val result3 = pr.processor!!.parse("a,a,a");
        val expected3 = pr.processor!!.spptParser.parse(
            """
             S { a{'a'} ',' a{'a'} ',' a{'a'} }
        """
        )
        assertEquals(expected3.toStringAll, result3.sppt?.toStringAll)
        assertEquals(expected3, result3.sppt)
        assertTrue(result3.issues.isEmpty())
    }

    @Test
    fun nonTerminal_slist_range_unbraced_unbounded() {

        val grammarStr = """
            namespace test
            grammar Test {
                S = [ a  / ',' ]2+ ;
                a = 'a' ;
            }
        """.trimIndent()

        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertNotNull(pr.processor)

        val result0 = pr.processor!!.parse("")
        assertEquals(null, result0.sppt)
        assertEquals(
            setOf(
                parseError(InputLocation(0, 1, 1, 1, null), "", setOf("<GOAL>"), setOf("'a'"))
            ), result0.issues.all
        )

        val result1 = pr.processor!!.parse("a")
        assertEquals(null, result1.sppt)
        assertEquals(
            setOf(
                parseError(InputLocation(1, 2, 1, 1, null), "a", setOf("<GOAL>"), setOf("','"))
            ), result1.issues.all
        )

        val result2 = pr.processor!!.parse("a,a");
        val expected2 = pr.processor!!.spptParser.parse(
            """
             S {  a{'a'} ',' a{'a'} }
        """
        )
        assertEquals(expected2.toStringAll, result2.sppt?.toStringAll)
        assertEquals(expected2, result2.sppt)
        assertTrue(result2.issues.isEmpty())

        val result5 = pr.processor!!.parse("a,a,a,a,a");
        val expected5 = pr.processor!!.spptParser.parse(
            """
             S { a{'a'} ',' a{'a'} ',' a{'a'} ',' a{'a'} ',' a{'a'} }
        """
        )
        assertEquals(expected5.toStringAll, result5.sppt?.toStringAll)
        assertEquals(expected5, result5.sppt)
        assertTrue(result5.issues.isEmpty())
    }

    @Test
    fun nonTerminal_slist_range_unbraced_bounded() {

        val grammarStr = """
            namespace test
            grammar Test {
                S = [ a  / ',' ]2..5 ;
                a = 'a' ;
            }
        """.trimIndent()

        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertNotNull(pr.processor)


        val result0 = pr.processor!!.parse("")
        assertEquals(null, result0.sppt)
        assertEquals(
            setOf(
                parseError(InputLocation(0, 1, 1, 1, null), "", setOf("<GOAL>"), setOf("'a'"))
            ), result0.issues.all
        )

        val result1 = pr.processor!!.parse("a")
        assertEquals(null, result1.sppt)
        assertEquals(
            setOf(
                parseError(InputLocation(1, 2, 1, 1, null), "a", setOf("<GOAL>"), setOf("','"))
            ), result1.issues.all
        )

        val result2 = pr.processor!!.parse("a,a");
        val expected2 = pr.processor!!.spptParser.parse(
            """
             S { a{'a'} ',' a{'a'} }
        """
        )
        assertEquals(expected2.toStringAll, result2.sppt?.toStringAll)
        assertEquals(expected2, result2.sppt)
        assertTrue(result2.issues.isEmpty())

        val result5 = pr.processor!!.parse("a,a,a,a,a");
        val expected5 = pr.processor!!.spptParser.parse(
            """
             S { a{'a'} ',' a{'a'} ',' a{'a'} ',' a{'a'} ',' a{'a'} }
        """
        )
        assertEquals(expected5.toStringAll, result5.sppt?.toStringAll)
        assertEquals(expected5, result5.sppt)
        assertTrue(result5.issues.isEmpty())

        val result6 = pr.processor!!.parse("a,a,a,a,a,a")
        assertEquals(null, result6.sppt)
        assertEquals(
            setOf(
                parseError(InputLocation(9, 10, 1, 1, null), "a,a,a,a,a,a", setOf("a"), setOf("<EOT>"))
            ), result6.issues.all
        )
    }

    @Test
    fun nonTerminal_slist_range_unbraced_bounded_maxZero_fails() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = [ a  / ',' ]2..0 ;
                a = 'a' ;
            }
        """.trimIndent()

        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))

        val expected = listOf(
            parseError(InputLocation(position = 53, column = 24, line = 3, length = 1, sentenceIdentity = null), grammarStr, setOf("rangeUnBraced"), setOf("POSITIVE_INTEGER_GT_ZERO"))
        )

        assertEquals(expected, pr.issues.errors, pr.issues.toString())
    }

    @Test
    fun nonTerminal_slist_range_braced_unbounded() {

        val grammarStr = """
            namespace test
            grammar Test {
                S = [ a  / ',' ]{2+} ;
                a = 'a' ;
            }
        """.trimIndent()

        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertNotNull(pr.processor)

        val result0 = pr.processor!!.parse("")
        assertEquals(null, result0.sppt)
        assertEquals(
            setOf(
                parseError(InputLocation(0, 1, 1, 1, null), "", setOf("<GOAL>"), setOf("'a'"))
            ), result0.issues.all
        )

        val result1 = pr.processor!!.parse("a")
        assertEquals(null, result1.sppt)
        assertEquals(
            setOf(
                parseError(InputLocation(1, 2, 1, 1, null), "a", setOf("<GOAL>"), setOf("','"))
            ), result1.issues.all
        )

        val result2 = pr.processor!!.parse("a,a");
        val expected2 = pr.processor!!.spptParser.parse(
            """
             S { a{'a'} ',' a{'a'} }
        """
        )
        assertEquals(expected2.toStringAll, result2.sppt?.toStringAll)
        assertEquals(expected2, result2.sppt)
        assertTrue(result2.issues.isEmpty())

        val result5 = pr.processor!!.parse("a,a,a,a,a");
        val expected5 = pr.processor!!.spptParser.parse(
            """
             S { a{'a'} ',' a{'a'} ',' a{'a'} ',' a{'a'} ',' a{'a'} }
        """
        )
        assertEquals(expected5.toStringAll, result5.sppt?.toStringAll)
        assertEquals(expected5, result5.sppt)
        assertTrue(result5.issues.isEmpty())
    }

    @Test
    fun nonTerminal_slist_range_braced_bounded() {

        val grammarStr = """
            namespace test
            grammar Test {
                S = [ a  / ',' ]{2..5} ;
                a = 'a' ;
            }
        """.trimIndent()

        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertNotNull(pr.processor)


        val result0 = pr.processor!!.parse("")
        assertEquals(null, result0.sppt)
        assertEquals(
            setOf(
                parseError(InputLocation(0, 1, 1, 1, null), "", setOf("<GOAL>"), setOf("'a'"))
            ), result0.issues.all
        )

        val result1 = pr.processor!!.parse("a")
        assertEquals(null, result1.sppt)
        assertEquals(
            setOf(
                parseError(InputLocation(1, 2, 1, 1, null), "a", setOf("<GOAL>"), setOf("','"))
            ), result1.issues.all
        )

        val result2 = pr.processor!!.parse("a,a");
        val expected2 = pr.processor!!.spptParser.parse(
            """
             S { a{'a'} ',' a{'a'} }
        """
        )
        assertEquals(expected2.toStringAll, result2.sppt?.toStringAll)
        assertEquals(expected2, result2.sppt)
        assertTrue(result2.issues.isEmpty())

        val result5 = pr.processor!!.parse("a,a,a,a,a");
        val expected5 = pr.processor!!.spptParser.parse(
            """
             S { a{'a'} ',' a{'a'} ',' a{'a'} ',' a{'a'} ',' a{'a'} }
        """
        )
        assertEquals(expected5.toStringAll, result5.sppt?.toStringAll)
        assertEquals(expected5, result5.sppt)
        assertTrue(result5.issues.isEmpty())

        val result6 = pr.processor!!.parse("a,a,a,a,a,a")
        assertEquals(null, result6.sppt)
        assertEquals(
            setOf(
                parseError(InputLocation(9, 10, 1, 1, null), "a,a,a,a,a,a", setOf("a"), setOf("<EOT>"))
            ), result6.issues.all
        )
    }

    @Test
    fun group1_() {

        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' ('b' | c+ ) ;
                leaf c = 'c' ;
            }
        """.trimIndent()

        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertNotNull(pr.processor)

        val sentence1 = ""
        test_parse_fails(
            grammarStr, sentence1, setOf(
                parseError(InputLocation(0, 1, 1, 1, null), sentence1, setOf("<GOAL>"), setOf("'a'"))
            )
        )

        val sentence2 = "a"
        test_parse_fails(
            grammarStr, sentence2, setOf(
                parseError(InputLocation(1, 2, 1, 1, null), sentence2, setOf("<GOAL>"), setOf("'b'", "c"))
            )
        )

        val sentence3 = "b"
        val result3 = pr.processor!!.parse(sentence3)
        assertEquals(null, result3.sppt)
        assertEquals(
            setOf(
                parseError(InputLocation(0, 1, 1, 1, null), sentence3, setOf("<GOAL>"), setOf("'a'"))
            ), result3.issues.all
        )

        val result4 = pr.processor!!.parse("c")
        assertEquals(null, result4.sppt)
        assertEquals(
            setOf(
                parseError(InputLocation(0, 1, 1, 1, null), "c", setOf("<GOAL>"), setOf("'a'"))
            ), result4.issues.all
        )

        val result5 = pr.processor!!.parse("ab");
        val expected5 = pr.processor!!.spptParser.parse(
            """
            S {
                'a'
                §S§choice1|0 { 'b' }
            }
        """
        )
        assertEquals(expected5.toStringAll, result5.sppt?.toStringAll)
        assertEquals(expected5, result5.sppt)
        assertTrue(result5.issues.isEmpty())

        val result6 = pr.processor!!.parse("accc");
        val expected6 = pr.processor!!.spptParser.parse(
            """
            S {
                'a'
                §S§choice1|1 { §S§multi1 {
                    c : 'c'
                    c : 'c'
                    c : 'c'
                } }
            }
        """
        )
        assertEquals(expected6.toStringAll, result6.sppt?.toStringAll)
        assertEquals(expected6, result6.sppt)
        assertTrue(result6.issues.isEmpty())
    }

    @Test
    fun group_ignored() {

        val grammarStr = """
            namespace test
            grammar Test {
                S = ('a' b+) ;
                leaf b = 'b' ;
            }
        """.trimIndent()

        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertNotNull(pr.processor)

        val result1 = pr.processor!!.parse("")
        assertEquals(null, result1.sppt)
        assertEquals(
            setOf(
                parseError(InputLocation(0, 1, 1, 1, null), "", setOf("<GOAL>"), setOf("'a'"))
            ), result1.issues.all
        )

        val result2 = pr.processor!!.parse("a")
        assertEquals(null, result1.sppt)
        assertEquals(
            setOf(
                parseError(InputLocation(1, 2, 1, 1, null), "a", setOf("<GOAL>"), setOf("b"))
            ), result2.issues.all
        )

        val result3 = pr.processor!!.parse("abbb");
        val expected3 = pr.processor!!.spptParser.parse(
            """
            S { §S§group1 {
                'a'
                §S§multi1 { b:'b' b:'b' b:'b' }
            } }
        """
        )
        assertEquals(expected3.toStringAll, result3.sppt?.toStringAll)
        assertEquals(expected3, result3.sppt)
        assertTrue(result3.issues.isEmpty())
    }

    @Test
    fun concat_optional_group() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a ('b' c)? ;
                a = 'a' ;
                c = 'c' ;
            }
        """.trimIndent()

        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertNotNull(pr.processor)

        val result1 = pr.processor!!.parse("a");
        val expected1 = pr.processor!!.spptParser.parse(
            """
             S { a{'a'} §S§opt1 { §empty } }
        """
        )
        assertEquals(expected1.toStringAll, result1.sppt?.toStringAll)
        //assertEquals(expected1, result1.sppt)
        assertTrue(result1.issues.isEmpty())

        val result2 = pr.processor!!.parse("abc");
        val expected2 = pr.processor!!.spptParser.parse(
            """
            S {
                a { 'a' }
                §S§opt1 { §S§group1 {
                    'b'
                    c { 'c' }
                } }
            }
        """
        )
        assertEquals(expected2.toStringAll, result2.sppt?.toStringAll)
        assertEquals(expected2, result2.sppt)
        assertTrue(result2.issues.isEmpty())
    }

    @Test
    fun eg1() {

        val grammarStr = """
            namespace test
            grammar Test {
                HexDigits = HexDigit ((HexDigit | '_')* HexDigit)? ;
                HexDigit = "[0-9a-fA-F]" ;
                Digits = "[0-9]([0-9_]*[0-9])?" ;
                HEX_FLOAT_LITERAL
                    = '0' "[xX]"
                      ( HexDigits '.'? | HexDigits? '.' HexDigits )
                      "[pP]" "[+-]"? Digits "[fFdD]"?
                    ;
                BOOL_LITERAL=       'true' | 'false' ;
                FLOAT_LITERAL=		"(((0|([1-9](0|[1-9])*)).(0|[1-9])*|.(0|[1-9])+ )([eE][+-]?(0|[1-9])+)?)|((0|N(0|[1-9]))([eE][+-]?(0|[1-9])+))";

            }
        """.trimIndent()

        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertNotNull(pr.processor)
        //TODO: more checks
    }

    @Test
    fun override() {

        val grammarStr = """
            namespace test
            grammar Original {
                S1 = ABC ;
                ABC = 'a' B 'c' ;
                B = 'b' ;
            }
            grammar Extended : Original {
                override B = 'd' ;
            }
        """.trimIndent()

        val pr = Agl.processorFromString<Any, Any>(grammarStr, Agl.configuration { targetGrammarName(("Original")); defaultGoalRuleName("S1") })
        assertTrue(pr.issues.errors.isEmpty(), pr.issues.toString())
        assertNotNull(pr.processor)

        val result1 = pr.processor!!.parse("abc", Agl.parseOptions { goalRuleName("S1") })
        val expected1 = pr.processor!!.spptParser.parse(
            """
             S1 { ABC { 'a' B { 'b' } 'c' } }
        """
        )
        assertEquals(expected1.toStringAll, result1.sppt?.toStringAll)
        assertEquals(expected1, result1.sppt)
        assertTrue(result1.issues.isEmpty())

        val pr2 = Agl.processorFromString<Any, Any>(grammarStr, Agl.configuration { targetGrammarName(("Extended")); defaultGoalRuleName("S1") })
        assertTrue(pr2.issues.errors.isEmpty(), pr2.issues.toString())
        assertNotNull(pr2.processor)
        val result2 = pr2.processor!!.parse("adc", Agl.parseOptions { goalRuleName("S1") })
        val expected2 = pr2.processor!!.spptParser.parse(
            """
             S1 { ABC { 'a' B { 'd' } 'c' } }
        """
        )
        assertEquals(expected2.toStringAll, result2.sppt?.toStringAll)
        assertEquals(expected2, result2.sppt)
        assertTrue(result2.issues.isEmpty())
    }

    @Test
    fun embedded_qualified_defaultGoal() {

        val grammarStr = """
            namespace test
            grammar Inner {
                B = 'b' ;
            }
            grammar Outer {
                S = a gB c ;
                leaf a = 'a' ;
                leaf c = 'c' ;
                gB = Inner::B ;
            }
        """.trimIndent()

        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertNotNull(pr.processor)

        val result1 = pr.processor!!.parse("abc", Agl.parseOptions { goalRuleName("S") })
        val expected1 = pr.processor!!.spptParser.parse(
            """
             S { 
               a:'a'
               gB : Inner::B { 'b' }
               c:'c'
             }
        """
        )
        assertEquals(expected1.toStringAll, result1.sppt?.toStringAll)
//        assertEquals(expected1, result1.sppt)
        assertTrue(result1.issues.isEmpty())
    }

    @Test
    fun embedded_qualified_explicitGoal() {

        val grammarStr = """
            namespace test
            grammar Inner {
                B = 'b' ;
                C = 'c' ;
            }
            grammar Outer {
                S = A gB D ;
                leaf A = 'a' ;
                leaf D = 'd' ;
                gB = Inner::C;
            }
        """.trimIndent()

        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertNotNull(pr.processor)

        val result1 = pr.processor!!.parse("acd", Agl.parseOptions { goalRuleName("S") })
        val expected1 = pr.processor!!.spptParser.parse(
            """
             S { 
               A:'a'
               gB : Inner::C { 'c' }
               D:'d'
             }
        """
        )
        assertEquals(expected1.toStringAll, result1.sppt?.toStringAll)
//        assertEquals(expected1, result1.sppt)
        assertTrue(result1.issues.isEmpty())
    }

    @Test
    fun preference_infix() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = v | A | D ;
                leaf v = "[a-z]+" ;
                A = S '+' S ;
                D = S '/' S ;
                
                preference S {
                  A on '+' left
                  D on '/' left
                }
            }
        """.trimIndent()
        test_parse(grammarStr, "v", "S{ v:'v' }")
        test_parse(grammarStr, "a+b", "S{ A{ S{v:'a'} '+' S{v:'b'} } }")
        test_parse(grammarStr, "a/b", "S{ D{ S{v:'a'} '/' S{v:'b'} } }")
        test_parse(
            grammarStr, "a+b/c", """
            S { A {
              S { v : 'a' }
              '+'
              S { D {
                S { v : 'b' }
                '/'
                S { v : 'c' }
              } }
            } }
        """.trimIndent()
        )
    }

    @Test
    fun preference_expressions_sepList() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = v | A | D ;
                leaf v = "[a-z]+" ;
                A = [S / '+']2+ ;
                D = [S / '*']2+ ;
                
                preference S {
                  A ITEM on '+' left
                  D ITEM on '*' left
                }
            }
        """.trimIndent()
        test_parse(grammarStr, "v", "S{ v:'v' }")
        test_parse(grammarStr, "a+b", "S{ A{ S{v:'a'} '+' S{v:'b'} } }")
        test_parse(grammarStr, "a*b", "S{ D{ S{v:'a'} '*' S{v:'b'} } }")
        test_parse(
            grammarStr, "a+b+c", """
            S { A {
              S { v : 'a' }
              '+'
              S { v : 'b' }
              '+'
              S { v : 'c' }
            } }
        """.trimIndent()
        )
        test_parse(
            grammarStr, "a+b*c", """
            S { A {
              S { v : 'a' }
              '+'
              S { D {
                S { v : 'b' }
                '*'
                S { v : 'c' }
              } }
            } }
        """.trimIndent()
        )
        test_parse(
            grammarStr, "a*b+c", """
            S { A {
              S { D {
                S { v : 'a' }
                '*'
                S { v : 'b' }
              } }
              '+'
              S { v : 'c' }
            } }
        """.trimIndent()
        )
    }
}
