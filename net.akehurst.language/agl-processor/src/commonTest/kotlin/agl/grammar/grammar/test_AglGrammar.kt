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

package net.akehurst.language.agl.grammar.grammar

import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.api.asm.AsmSimple
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.LanguageIssueKind
import net.akehurst.language.api.processor.LanguageProcessorPhase
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_AglGrammar {

    @BeforeTest
    fun before() {
        Agl.registry.agl.grammar.processor?.buildForDefaultGoal()
    }

    @Test
    fun single_line_comment() {

        val grammarStr = """
            // single line comment
            namespace test
            grammar Test {
                a = 'a' ;
            }
        """.trimIndent()

        val p = Agl.processorFromString(grammarStr)
        assertNotNull(p)

        val (actual, issues) = p.parse("a")
        val expected = p.spptParser.parse(
            """
            a { 'a' }
        """
        )
        assertEquals(expected.toStringAll, actual?.toStringAll)
        assertEquals(expected, actual)
        assertEquals(emptyList(), issues)
    }

    @Test
    fun empty() {

        val grammarStr = """
            namespace test
            grammar Test {
                a = ;
            }
        """.trimIndent()

        val p = Agl.processorFromString(grammarStr)
        assertNotNull(p)

        val (actual, issues) = p.parse("");
        val expected = p.spptParser.parse(
            """
            a { §empty }
        """
        )
        assertEquals(expected.toStringAll, actual?.toStringAll)
        assertEquals(expected, actual)
        assertEquals(emptyList(), issues)
    }

    @Test
    fun literal() {

        val grammarStr = """
            namespace test
            grammar Test {
                a = 'a' ;
            }
        """.trimIndent()

        val p = Agl.processorFromString(grammarStr)
        assertNotNull(p)

        val (actual, issues) = p.parse("a");
        val expected = p.spptParser.parse(
            """
            a { 'a' }
        """
        )
        assertEquals(expected.toStringAll, actual?.toStringAll)
        assertEquals(expected, actual)
        assertEquals(emptyList(), issues)
    }

    @Test
    fun literal_empty() {

        val grammarStr = """
            namespace test
            grammar Test {
                a = '' 'a';
            }
        """.trimIndent()

        val p = Agl.processorFromString(grammarStr)
        assertNotNull(p)

        val (actual, issues) = p.parse("a");
        val expected = p.spptParser.parse(
            """
            a { '' 'a' }
        """
        )
        assertEquals(expected.toStringAll, actual?.toStringAll)
        assertEquals(expected, actual)
        assertEquals(emptyList(), issues)
    }

    @Test
    fun escapeSequence() {

        val grammarStr = """
            namespace test
            grammar Test {
                EscapeSequence = '\\' "[btnfr'\\]" ;
            }
        """.trimIndent()

        val p = Agl.processorFromString(grammarStr)
        assertNotNull(p)

        val (actual, issues) = p.parse("\\b");
        val expected = p.spptParser.parse(
            """
             EscapeSequence {
                '\\'
                "[btnfr'\\]" : 'b'
             }
        """
        )
        assertEquals(expected.toStringAll, actual?.toStringAll)
        assertEquals(expected, actual)
        assertEquals(emptyList(), issues)
    }

    @Test
    fun pattern() {

        val grammarStr = """
            namespace test
            grammar Test {
                a = "[a-c]" ;
            }
        """.trimIndent()

        val p = Agl.processorFromString(grammarStr)
        assertNotNull(p)

        val (actual, issues) = p.parse("b");
        val expected = p.spptParser.parse(
            """
             a {
                "[a-c]":'b'
             }
        """
        )
        assertEquals(expected.toStringAll, actual?.toStringAll)
        assertEquals(expected, actual)
        assertEquals(emptyList(), issues)
    }

    @Test
    fun leaf() {

        val grammarStr = """
            namespace test
            grammar Test {
               leaf a = 'a' ;
            }
        """.trimIndent()

        val p = Agl.processorFromString(grammarStr)
        assertNotNull(p)

        val (actual, issues) = p.parse("a");
        val expected = p.spptParser.parse(
            """
             a:'a'
        """
        )
        assertEquals(expected.toStringAll, actual?.toStringAll)
        assertEquals(expected, actual)
        assertEquals(emptyList(), issues)
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

        val p = Agl.processorFromString(grammarStr)
        assertNotNull(p)

        val (actual, issues) = p.parse("ba");
        val expected = p.spptParser.parse(
            """
             b{ 'b' a : 'a' }
        """
        )
        assertEquals(expected.toStringAll, actual?.toStringAll)
        assertEquals(expected, actual)
        assertEquals(emptyList(), issues)
    }

    @Test
    fun literal_concatenation() {

        val grammarStr = """
            namespace test
            grammar Test {
                a = 'a' 'b' 'c' ;
            }
        """.trimIndent()

        val p = Agl.processorFromString(grammarStr)
        assertNotNull(p)

        val (actual, issues) = p.parse("abc");
        val expected = p.spptParser.parse(
            """
             a { 'a' 'b' 'c' }
        """
        )
        assertEquals(expected.toStringAll, actual?.toStringAll)
        assertEquals(expected, actual)
        assertEquals(emptyList(), issues)
    }

    @Test
    fun literal_choice_equal() {

        val grammarStr = """
            namespace test
            grammar Test {
                abc = 'a' | 'b' | 'c' ;
            }
        """.trimIndent()

        val p = Agl.processorFromString(grammarStr)
        assertNotNull(p)

        val (actual, issues) = p.parse("b");
        val expected = p.spptParser.parse(
            """
             abc|1 { 'b' }
        """
        )
        assertEquals(expected.toStringAll, actual?.toStringAll)
        assertEquals(expected, actual)
        assertEquals(emptyList(), issues)
    }

    @Test
    fun literal_choice_priority() {

        val grammarStr = """
            namespace test
            grammar Test {
                abc = 'a' < 'b' < 'c' ;
            }
        """.trimIndent()

        val p = Agl.processorFromString(grammarStr)
        assertNotNull(p)

        val (actual, issues) = p.parse("b");
        val expected = p.spptParser.parse(
            """
             abc|1 { 'b' }
        """
        )
        assertEquals(expected.toStringAll, actual?.toStringAll)
        assertEquals(expected, actual)
        assertEquals(emptyList(), issues)
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

        val p = Agl.processorFromString(grammarStr)
        assertNotNull(p)

        val (actual, issues) = p.parse("f");
        val expected = p.spptParser.parse(
            """
             choice|5 { f { 'f' } }
        """
        )
        assertEquals(expected.toStringAll, actual?.toStringAll)
        assertEquals(expected, actual)
        assertEquals(emptyList(), issues)
    }

    @Test
    fun literal_multi_0_1() {

        val grammarStr = """
            namespace test
            grammar Test {
                a = 'a'? ;
            }
        """.trimIndent()

        val p = Agl.processorFromString(grammarStr)
        assertNotNull(p)

        val (actual1, issues1) = p.parse("");
        val expected1 = p.spptParser.parse(
            """
             a|1 { §empty }
        """
        )
        assertEquals(expected1.toStringAll, actual1?.toStringAll)
        assertEquals(expected1, actual1)
        assertEquals(emptyList(), issues1)

        val (actual2, issues2) = p.parse("a");
        val expected2 = p.spptParser.parse(
            """
             a { 'a' }
        """
        )
        assertEquals(expected2.toStringAll, actual2?.toStringAll)
        assertEquals(expected2, actual2)
        assertEquals(emptyList(), issues2)
    }

    @Test
    fun literal_multi_0_n() {

        val grammarStr = """
            namespace test
            grammar Test {
                a = 'a'* ;
            }
        """.trimIndent()

        val p = Agl.processorFromString(grammarStr)
        assertNotNull(p)

        val (actual1, issues1) = p.parse("");
        val expected1 = p.spptParser.parse(
            """
             a|1 { §empty }
        """
        )
        assertEquals(expected1.toStringAll, actual1?.toStringAll)
        assertEquals(expected1, actual1)
        assertEquals(emptyList(), issues1)

        val (actual2, issues2) = p.parse("a");
        val expected2 = p.spptParser.parse(
            """
             a { 'a' }
        """
        )
        assertEquals(expected2.toStringAll, actual2?.toStringAll)
        assertEquals(expected2, actual2)
        assertEquals(emptyList(), issues2)

        val (actual3, issues3) = p.parse("aaa");
        val expected3 = p.spptParser.parse(
            """
             a { 'a' 'a' 'a' }
        """
        )
        assertEquals(expected3.toStringAll, actual3?.toStringAll)
        assertEquals(expected3, actual3)
        assertEquals(emptyList(), issues3)
    }

    @Test
    fun literal_multi_1_n() {

        val grammarStr = """
            namespace test
            grammar Test {
                a = 'a'+ ;
            }
        """.trimIndent()

        val p = Agl.processorFromString(grammarStr)
        assertNotNull(p)

        val (actual1, issues1) = p.parse("")
        assertEquals(null, actual1)
        val expIssues1 = listOf(LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.PARSE, InputLocation(0, 1, 1, 1), "^", setOf("'a'")))
        assertEquals(expIssues1, issues1)

        val (actual2, issues2) = p.parse("a");
        val expected2 = p.spptParser.parse(
            """
             a { 'a' }
        """
        )
        assertEquals(expected2.toStringAll, actual2?.toStringAll)
        assertEquals(expected2, actual2)
        assertEquals(emptyList(), issues2)

        val (actual3, issues3) = p.parse("aaa");
        val expected3 = p.spptParser.parse(
            """
             a { 'a' 'a' 'a' }
        """
        )
        assertEquals(expected3.toStringAll, actual3?.toStringAll)
        assertEquals(expected3, actual3)
        assertEquals(emptyList(), issues3)
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

        val p = Agl.processorFromString(grammarStr)
        assertNotNull(p)

        val (actual1, issues1) = p.parse("abc");
        val expected1 = p.spptParser.parse(
            """
             S { a {'a'} b{'b'} c{'c'} }
        """
        )
        assertEquals(expected1.toStringAll, actual1?.toStringAll)
        assertEquals(expected1, actual1)
        assertEquals(emptyList(), issues1)
    }

    @Test
    fun nonTerminal_multi_0_1() {

        val grammarStr = """
            namespace test
            grammar Test {
                S = a ? ;
                a = 'a' ;
            }
        """.trimIndent()

        val p = Agl.processorFromString(grammarStr)
        assertNotNull(p)

        val (actual1, issues1) = p.parse("");
        val expected1 = p.spptParser.parse(
            """
             S|1 {  §empty }
        """
        )
        assertEquals(expected1.toStringAll, actual1?.toStringAll)
        assertEquals(expected1, actual1)
        assertEquals(emptyList(), issues1)

        val (actual2, issues2) = p.parse("a");
        val expected2 = p.spptParser.parse(
            """
             S { a{'a'} }
        """
        )
        assertEquals(expected2.toStringAll, actual2?.toStringAll)
        assertEquals(expected2, actual2)
        assertEquals(emptyList(), issues2)
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

        val p = Agl.processorFromString(grammarStr)
        assertNotNull(p)

        val (actual1, issues1) = p.parse("");
        val expected1 = p.spptParser.parse(
            """
             S|1 { §empty }
        """
        )
        assertEquals(expected1.toStringAll, actual1?.toStringAll)
        assertEquals(expected1, actual1)
        assertEquals(emptyList(), issues1)

        val (actual2, issues2) = p.parse("a");
        val expected2 = p.spptParser.parse(
            """
             S { a{'a'} }
        """
        )
        assertEquals(expected2.toStringAll, actual2?.toStringAll)
        assertEquals(expected2, actual2)
        assertEquals(emptyList(), issues2)

        val (actual3, issues3) = p.parse("aaa");
        val expected3 = p.spptParser.parse(
            """
             S { a{'a'} a{'a'} a{'a'} }
        """
        )
        assertEquals(expected3.toStringAll, actual3?.toStringAll)
        assertEquals(expected3, actual3)
        assertEquals(emptyList(), issues3)
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

        val p = Agl.processorFromString(grammarStr)
        assertNotNull(p)

        val (actual1, issues1) = p.parse("")
        assertEquals(null, actual1)
        assertEquals(listOf(
            LanguageIssue(LanguageIssueKind.ERROR,LanguageProcessorPhase.PARSE,InputLocation(0,1,1,1),"^", setOf("'a'"))
        ), issues1)

        val (actual2, issues2) = p.parse("a");
        val expected2 = p.spptParser.parse(
            """
             S {  a{'a'} }
        """
        )
        assertEquals(expected2.toStringAll, actual2?.toStringAll)
        assertEquals(expected2, actual2)
        assertEquals(emptyList(), issues2)

        val (actual3, issues3) = p.parse("aaa");
        val expected3 = p.spptParser.parse(
            """
             S {  a{'a'} a{'a'} a{'a'} }
        """
        )
        assertEquals(expected3.toStringAll, actual3?.toStringAll)
        assertEquals(expected3, actual3)
        assertEquals(emptyList(), issues3)
    }

    @Test
    fun nonTerminal_multi_2_n() {

        val grammarStr = """
            namespace test
            grammar Test {
                S = a 2+ ;
                a = 'a' ;
            }
        """.trimIndent()

        val p = Agl.processorFromString(grammarStr)
        assertNotNull(p)

        val (actual0, issues0) = p.parse("")
        assertEquals(null, actual0)
        assertEquals(listOf(
            LanguageIssue(LanguageIssueKind.ERROR,LanguageProcessorPhase.PARSE,InputLocation(0,1,1,1),"^", setOf("'a'"))
        ), issues0)

        val (actual1, issues1) = p.parse("a")
        assertEquals(null, actual1)
        assertEquals(listOf(
            LanguageIssue(LanguageIssueKind.ERROR,LanguageProcessorPhase.PARSE,InputLocation(1,2,1,1),"a^", setOf("'a'"))
        ), issues1)

        val (actual2, issues2) = p.parse("aa");
        val expected2 = p.spptParser.parse(
            """
             S { a{'a'} a{'a'} }
        """
        )
        assertEquals(expected2.toStringAll, actual2?.toStringAll)
        assertEquals(expected2, actual2)
        assertEquals(emptyList(), issues2)

        val (actual4, issues4) = p.parse("aaaa");
        val expected4 = p.spptParser.parse(
            """
             S { a{'a'} a{'a'} a{'a'} a{'a'} }
        """
        )
        assertEquals(expected4.toStringAll, actual4?.toStringAll)
        assertEquals(expected4, actual4)
        assertEquals(emptyList(), issues4)
    }

    @Test
    fun nonTerminal_multi_2_5() {

        val grammarStr = """
            namespace test
            grammar Test {
                S = a 2..5 ;
                a = 'a' ;
            }
        """.trimIndent()

        val p = Agl.processorFromString(grammarStr)
        assertNotNull(p)

        val (actual0, issues0) = p.parse("")
        assertEquals(null, actual0)
        assertEquals(listOf(
            LanguageIssue(LanguageIssueKind.ERROR,LanguageProcessorPhase.PARSE,InputLocation(0,1,1,1),"^", setOf("'a'"))
        ), issues0)

        val (actual1, issues1) = p.parse("a")
        assertEquals(null, actual1)
        assertEquals(listOf(
            LanguageIssue(LanguageIssueKind.ERROR,LanguageProcessorPhase.PARSE,InputLocation(1,2,1,1),"a^", setOf("'a'"))
        ), issues1)

        val (actual2, issues2) = p.parse("aa");
        val expected2 = p.spptParser.parse(
            """
             S { a{'a'} a{'a'} }
        """
        )
        assertEquals(expected2.toStringAll, actual2?.toStringAll)
        assertEquals(expected2, actual2)
        assertEquals(emptyList(), issues2)

        val (actual5, issues5) = p.parse("aaaaa");
        val expected5 = p.spptParser.parse(
            """
             S { a{'a'} a{'a'} a{'a'} a{'a'} a{'a'} }
        """
        )
        assertEquals(expected5.toStringAll, actual5?.toStringAll)
        assertEquals(expected5, actual5)
        assertEquals(emptyList(), issues5)

        val (actual6,issues6) = p.parse("aaaaaa")
        assertEquals(null,actual6)
        assertEquals(listOf(
            LanguageIssue(LanguageIssueKind.ERROR,LanguageProcessorPhase.PARSE,InputLocation(6,7,1,1),"aaaaaa^", setOf("<EOT>"))
        ),issues6)

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

        val p = Agl.processorFromString(grammarStr)
        assertNotNull(p)

        val (actual0, issues0) = p.parse("");
        val expected0 = p.spptParser.parse(
            """
             S|1 { §empty }
        """
        )
        assertEquals(expected0.toStringAll, actual0?.toStringAll)
        assertEquals(expected0, actual0)
        assertEquals(emptyList(), issues0)

        val (actual1, issues1) = p.parse("a");
        val expected1 = p.spptParser.parse(
            """
             S { a{'a'} }
        """
        )
        assertEquals(expected1.toStringAll, actual1?.toStringAll)
        assertEquals(expected1, actual1)
        assertEquals(emptyList(), issues1)

        val (actual2, issues2) = p.parse("aa")
        assertEquals(null, actual2)
        assertEquals(listOf(
            LanguageIssue(LanguageIssueKind.ERROR,LanguageProcessorPhase.PARSE,InputLocation(1,2,1,1),"a^a", setOf("<EOT>"))
        ), issues2)
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

        val p = Agl.processorFromString(grammarStr)
        assertNotNull(p)

        val (actual0, issues0) = p.parse("")
        val expected0 = p.spptParser.parse(
            """
             S|1 { §empty }
        """
        )
        assertEquals(expected0.toStringAll, actual0?.toStringAll)
        assertEquals(expected0, actual0)
        assertEquals(emptyList(), issues0)

        val (actual1, issues1) = p.parse("a")
        val expected1 = p.spptParser.parse(
            """
             S { a{'a'} }
        """
        )
        assertEquals(expected1.toStringAll, actual1?.toStringAll)
        assertEquals(expected1, actual1)
        assertEquals(emptyList(), issues1)

        val (actual3, issues3) = p.parse("a,a,a");
        val expected3 = p.spptParser.parse(
            """
             S { a{'a'} ',' a{'a'} ',' a{'a'} }
        """
        )
        assertEquals(expected3.toStringAll, actual3?.toStringAll)
        assertEquals(expected3, actual3)
        assertEquals(emptyList(), issues3)
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

        val p = Agl.processorFromString(grammarStr)
        assertNotNull(p)

        val (actual0, issues0) = p.parse("")
        assertEquals(null, actual0)
        assertEquals(listOf(
            LanguageIssue(LanguageIssueKind.ERROR,LanguageProcessorPhase.PARSE,InputLocation(0,1,1,1),"^", setOf("'a'"))
        ), issues0)

        val (actual1, issues1) = p.parse("a")
        val expected1 = p.spptParser.parse(
            """
             S { a{'a'} }
        """
        )
        assertEquals(expected1.toStringAll, actual1?.toStringAll)
        assertEquals(expected1, actual1)
        assertEquals(emptyList(), issues1)

        val (actual3, issues3) = p.parse("a,a,a");
        val expected3 = p.spptParser.parse(
            """
             S { a{'a'} ',' a{'a'} ',' a{'a'} }
        """
        )
        assertEquals(expected3.toStringAll, actual3?.toStringAll)
        assertEquals(expected3, actual3)
        assertEquals(emptyList(), issues3)
    }

    @Test
    fun nonTerminal_slist_2_n() {

        val grammarStr = """
            namespace test
            grammar Test {
                S = [ a  / ',' ]2+ ;
                a = 'a' ;
            }
        """.trimIndent()

        val p = Agl.processorFromString(grammarStr)
        assertNotNull(p)

        val (actual0, issues0) = p.parse("")
        assertEquals(null, actual0)
        assertEquals(listOf(
            LanguageIssue(LanguageIssueKind.ERROR,LanguageProcessorPhase.PARSE,InputLocation(0,1,1,1),"^", setOf("'a'"))
        ), issues0)

        val (actual1, issues1) = p.parse("a")
        assertEquals(null, actual1)
        assertEquals(listOf(
            LanguageIssue(LanguageIssueKind.ERROR,LanguageProcessorPhase.PARSE,InputLocation(1,2,1,1),"a^", setOf("','"))
        ), issues1)

        val (actual2, issues2) = p.parse("a,a");
        val expected2 = p.spptParser.parse(
            """
             S { a{'a'} ',' a{'a'}}
        """
        )
        assertEquals(expected2.toStringAll, actual2?.toStringAll)
        assertEquals(expected2, actual2)
        assertEquals(emptyList(), issues2)

        val (actual5, issues5) = p.parse("a,a,a,a,a");
        val expected5 = p.spptParser.parse(
            """
             S { a{'a'} ',' a{'a'} ',' a{'a'} ',' a{'a'} ',' a{'a'} }
        """
        )
        assertEquals(expected5.toStringAll, actual5?.toStringAll)
        assertEquals(expected5, actual5)
        assertEquals(emptyList(), issues5)
    }

    @Test
    fun nonTerminal_slist_2_5() {

        val grammarStr = """
            namespace test
            grammar Test {
                S = [ a  / ',' ]2..5 ;
                a = 'a' ;
            }
        """.trimIndent()

        val p = Agl.processorFromString(grammarStr)
        assertNotNull(p)


        val (actual0, issues0) = p.parse("")
        assertEquals(null, actual0)
        assertEquals(listOf(
            LanguageIssue(LanguageIssueKind.ERROR,LanguageProcessorPhase.PARSE,InputLocation(0,1,1,1),"^",setOf("'a'"))
        ), issues0)

        val (actual1, issues1) = p.parse("a")
        assertEquals(null, actual1)
        assertEquals(listOf(
            LanguageIssue(LanguageIssueKind.ERROR,LanguageProcessorPhase.PARSE,InputLocation(1,2,1,1),"a^",setOf("','"))
        ), issues1)

        val (actual2, issues2) = p.parse("a,a");
        val expected2 = p.spptParser.parse(
            """
             S { a{'a'} ',' a{'a'}}
        """
        )
        assertEquals(expected2.toStringAll, actual2?.toStringAll)
        assertEquals(expected2, actual2)
        assertEquals(emptyList(), issues2)

        val (actual5, issues5) = p.parse("a,a,a,a,a");
        val expected5 = p.spptParser.parse(
            """
             S { a{'a'} ',' a{'a'} ',' a{'a'} ',' a{'a'} ',' a{'a'} }
        """
        )
        assertEquals(expected5.toStringAll, actual5?.toStringAll)
        assertEquals(expected5, actual5)
        assertEquals(emptyList(), issues5)

        val (actual6, issues6) = p.parse("a,a,a,a,a,a")
        assertEquals(null, actual6)
        assertEquals(emptyList(), issues6)
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

        val p = Agl.processorFromString(grammarStr)
        assertNotNull(p)

        val (actual1, issues1) = p.parse("")
        assertEquals(null,actual1)
        assertEquals(listOf(
            LanguageIssue(LanguageIssueKind.ERROR,LanguageProcessorPhase.PARSE,InputLocation(0,1,1,1),"^", setOf("'a'"))
        ), issues1)

        val (actual2, issues2) = p.parse("a")
        assertEquals(null,actual2)
        assertEquals(listOf(
            LanguageIssue(LanguageIssueKind.ERROR,LanguageProcessorPhase.PARSE,InputLocation(1,2,1,1),"a^", setOf("'b'","c"))
        ), issues2)

        val (actual3, issues3) = p.parse("b")
        assertEquals(null,actual3)
        assertEquals(listOf(
            LanguageIssue(LanguageIssueKind.ERROR,LanguageProcessorPhase.PARSE,InputLocation(0,1,1,1),"^b", setOf("'a'"))
        ), issues3)

        val (actual4, issues4) = p.parse("c")
        assertEquals(null,actual4)
        assertEquals(listOf(
            LanguageIssue(LanguageIssueKind.ERROR,LanguageProcessorPhase.PARSE,InputLocation(0,1,1,1),"^c", setOf("'a'"))
        ), issues4)

        val (actual5, issues5) = p.parse("ab");
        val expected5 = p.spptParser.parse(
            """
            S {
                'a'
                §S§group1 { 'b' }
            }
        """
        )
        assertEquals(expected5.toStringAll, actual5?.toStringAll)
        assertEquals(expected5, actual5)
        assertEquals(emptyList(),issues5)

        val (actual6, issues6) = p.parse("accc");
        val expected6 = p.spptParser.parse(
            """
            S {
                'a'
                §S§group1|1 { §S§group1§multi1 {
                    c : 'c'
                    c : 'c'
                    c : 'c'
                } }
            }
        """
        )
        assertEquals(expected6.toStringAll, actual6?.toStringAll)
        assertEquals(expected6, actual6)
        assertEquals(emptyList(),issues6)
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

        val p = Agl.processorFromString(grammarStr)
        assertNotNull(p)

        val (actual1,issues1) = p.parse("")
        assertEquals(null,actual1)
        assertEquals(listOf(
            LanguageIssue(LanguageIssueKind.ERROR,LanguageProcessorPhase.PARSE,InputLocation(0,1,1,1),"^", setOf("'a'"))
        ),issues1)

        val (actual2,issues2) = p.parse("a")
        assertEquals(null,actual2)
        assertEquals(listOf(
            LanguageIssue(LanguageIssueKind.ERROR,LanguageProcessorPhase.PARSE,InputLocation(1,2,1,1),"a^", setOf("b"))
        ),issues2)

        val (actual3,issues3) = p.parse("abbb");
        val expected3 = p.spptParser.parse(
            """
            S {
                'a'
                §S§multi1 { b:'b' b:'b' b:'b' }
            }
        """
        )
        assertEquals(expected3.toStringAll, actual3?.toStringAll)
        assertEquals(expected3, actual3)
        assertEquals(emptyList(),issues3)
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

        val p = Agl.processorFromString(grammarStr)
        assertNotNull(p)

        val (actual1,issues1) = p.parse("a");
        val expected1 = p.spptParser.parse(
            """
             S { a{'a'} §S§multi1|1 { §empty } }
        """
        )
        assertEquals(expected1.toStringAll, actual1?.toStringAll)
        assertEquals(expected1, actual1)
        assertEquals(emptyList(),issues1)

        val (actual2,issues2) = p.parse("abc");
        val expected2 = p.spptParser.parse(
            """
            S {
                a { 'a' }
                §S§multi1 { §S§multi1§choice1 {
                    'b'
                    c { 'c' }
                } }
            }
        """
        )
        assertEquals(expected2.toStringAll, actual2?.toStringAll)
        assertEquals(expected2, actual2)
        assertEquals(emptyList(),issues2)
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

        val p = Agl.processorFromString(grammarStr)
        assertNotNull(p)
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
            grammar Extended extends Original {
                override B = 'd' ;
            }
        """.trimIndent()

        val p = Agl.processorFromString(grammarStr,"Original","S1")
        assertNotNull(p)

        val (actual1,issues1) = p.parse("abc", "S1")
        val expected1 = p.spptParser.parse(
            """
             S1 { ABC { 'a' B { 'b' } 'c' } }
        """
        )
        assertEquals(expected1.toStringAll, actual1?.toStringAll)
        assertEquals(expected1, actual1)
        assertEquals(emptyList(),issues1)

        val p2 = Agl.processorFromString(grammarStr,"Extended","S1")
        assertNotNull(p2)
        val (actual2,issues2) = p2.parse("adc", "S1")
        val expected2 = p2.spptParser.parse(
            """
             S1 { ABC { 'a' B { 'd' } 'c' } }
        """
        )
        assertEquals(expected2.toStringAll, actual2?.toStringAll)
        assertEquals(expected2, actual2)
        assertEquals(emptyList(),issues2)
    }

    @Test
    fun embedded_qualified_defaultGoal() {

        val grammarStr = """
            namespace test
            grammar Inner {
                B = 'b' ;
            }
            grammar Outer {
                S = A gB A ;
                leaf A = 'A' ;
                gB = test.Inner.B ;
            }
        """.trimIndent()

        val p = Agl.processorFromString(grammarStr)
        assertNotNull(p)
        //TODO: more checks
    }

    @Test
    fun embedded_unqualified_explicitGoal() {

        val grammarStr = """
            namespace test
            grammar Inner {
                B = 'b' ;
                C = 'c' ;
            }
            grammar Outer {
                S = A gB A ;
                leaf A = 'a' ;
                gB = Inner.C;
            }
        """.trimIndent()

        val p = Agl.processorFromString(grammarStr)
        assertNotNull(p)
        //TODO: more checks

        val (actual, issues) = p.process<AsmSimple, Any>("aca")
    }

}
