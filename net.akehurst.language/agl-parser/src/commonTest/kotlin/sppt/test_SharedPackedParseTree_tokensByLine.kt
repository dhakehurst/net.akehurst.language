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

package net.akehurst.language.agl.sppt

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.api.ParseResult
import net.akehurst.language.parser.leftcorner.LeftCornerParser
import net.akehurst.language.parser.leftcorner.ParseOptionsDefault
import net.akehurst.language.sentence.common.SentenceDefault
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.common.ScannerOnDemand
import net.akehurst.language.sppt.api.LeafData
import net.akehurst.language.sentence.api.Sentence
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_SharedPackedParseTree_tokensByLine {

    private companion object {

        //   namespace test
        //   grammar Test {
        //       skip leaf WS = "(\s|[.])+" ;
        //       skip leaf COMMENT = "/\*[^*]*\*+(?:[^*/][^*]*\*+)*/" ;
        //       S = 'aaa' 'bbb' 'ccc' ;
        //   }
        val rrs = runtimeRuleSet("test.Test") {
            pattern("WS", "(\\s|[.])+", true)
            pattern("COMMENT", "/\\*[^*]*\\*+(?:[^*/][^*]*\\*+)*/", true)

            concatenation("S") {
                literal("aaa"); literal("bbb"); literal("ccc")
            }
        }
        val scaner = ScannerOnDemand(RegexEnginePlatform, rrs.terminals)
        val parser = LeftCornerParser(scaner, rrs)

        fun parse(sentence: Sentence): ParseResult {
            return parser.parse(sentence.text, ParseOptionsDefault("S"))
        }
    }

    @Test
    fun all_on_one_line() {
        val sentence = SentenceDefault("aaa bbb ccc")
        val result = parse(sentence)
        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
        val actual = result.sppt!!.tokensByLine(0)

        val expected = listOf(
            LeafData("'aaa'", false, 0, 3, listOf("S", "'aaa'")),
            LeafData("WS", true, 3, 1, listOf("S", "WS")),
            LeafData("'bbb'", false, 4, 3, listOf("S", "'bbb'")),
            LeafData("WS", true, 7, 1, listOf("S", "WS")),
            LeafData("'ccc'", false, 8, 3, listOf("S", "'ccc'")),
        )

        assertEquals(expected, actual)
    }

    @Test
    fun separate_lines() {
        val sentence = SentenceDefault(
            """
            aaa
            bbb
            ccc
        """.trimIndent()
        )
        val result = parse(sentence)
        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())

        val actual_1 = result.sppt!!.tokensByLine(0)
        val actual_2 = result.sppt!!.tokensByLine(1)
        val actual_3 = result.sppt!!.tokensByLine(2)

        val expected_1 = listOf(
            LeafData("'aaa'", false, 0, 3, listOf("S", "'aaa'")),
            LeafData("WS", true, 3, 1, listOf("S", "WS")),
        )
        val expected_2 = listOf(
            LeafData("'bbb'", false, 4, 3, listOf("S", "'bbb'")),
            LeafData("WS", true, 7, 1, listOf("S", "WS")),
        )
        val expected_3 = listOf(
            LeafData("'ccc'", false, 8, 3, listOf("S", "'ccc'")),
        )
        assertEquals(expected_1, actual_1)
        assertEquals(expected_2, actual_2)
        assertEquals(expected_3, actual_3)
    }

    @Test
    fun separate_lines_with_indent() {
        val sentence = SentenceDefault(
            """
            aaa..
            ..bbb
            ccc
        """.trimIndent()
        )
        val result = parse(sentence)
        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
        println(result.sppt!!.toStringAll)

        val actual_1 = result.sppt!!.tokensByLine(0)
        val actual_2 = result.sppt!!.tokensByLine(1)
        val actual_3 = result.sppt!!.tokensByLine(2)

        val expected_1 = listOf(
            LeafData("'aaa'", false, 0, 3, listOf("S", "'aaa'")),
            LeafData("WS", true, 3, 2, listOf("S", "WS")),
            LeafData("WS", true, 5, 1, listOf("S", "WS")),
        )
        val expected_2 = listOf(
            LeafData("WS", true, 6, 2, listOf("S", "WS")),
            LeafData("'bbb'", false, 8, 3, listOf("S", "'bbb'")),
            LeafData("WS", true, 11, 1, listOf("S", "WS")),
        )
        val expected_3 = listOf(
            LeafData("'ccc'", false, 12, 3, listOf("S", "'ccc'")),
        )
        assertEquals(expected_1, actual_1)
        assertEquals(expected_2, actual_2)
        assertEquals(expected_3, actual_3)
    }

    @Test
    fun comment_over_1_lines() {
        val sentence = SentenceDefault(
            """
            /* comment */
            aaa bbb ccc
        """.trimIndent()
        )
        val result = parse(sentence)
        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
        println(result.sppt!!.toStringAll)

        val actual_1 = result.sppt!!.tokensByLine(0)
        val actual_2 = result.sppt!!.tokensByLine(1)

        val expected_1 = listOf(
            LeafData("COMMENT", true, 0, 13, listOf("S", "COMMENT")),
            LeafData("WS", true, 13, 1, listOf("S", "WS")),
        )
        val expected_2 = listOf(
            LeafData("'aaa'", false, 14, 3, listOf("S", "'aaa'")),
            LeafData("WS", true, 17, 1, listOf("S", "WS")),
            LeafData("'bbb'", false, 18, 3, listOf("S", "'bbb'")),
            LeafData("WS", true, 21, 1, listOf("S", "WS")),
            LeafData("'ccc'", false, 22, 3, listOf("S", "'ccc'")),
        )

        assertEquals(expected_1, actual_1)
        assertEquals(expected_2, actual_2)
    }

    @Test
    fun comment_over_2_lines() {
        val sentence = SentenceDefault(
            """
            /* comment
             over two lines */
            aaa bbb ccc
        """.trimIndent()
        )
        val result = parse(sentence)
        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
        println(result.sppt!!.toStringAll)

        val actual_1 = result.sppt!!.tokensByLine(0)
        val actual_2 = result.sppt!!.tokensByLine(1)
        val actual_3 = result.sppt!!.tokensByLine(2)

        val expected_1 = listOf(
            LeafData("COMMENT", true, 0, 10, listOf("S", "COMMENT")),  // first line of comment
            LeafData("COMMENT", true, 10, 1, listOf("S", "COMMENT")),  // EOL inside the comment
        )
        val expected_2 = listOf(
            LeafData("COMMENT", true, 11, 18, listOf("S", "COMMENT")), // rest of comment
            LeafData("WS", true, 29, 1, listOf("S", "WS")),            // EOL after comment
        )
        val expected_3 = listOf(
            LeafData("'aaa'", false, 30, 3, listOf("S", "'aaa'")),
            LeafData("WS", true, 33, 1, listOf("S", "WS")),
            LeafData("'bbb'", false, 34, 3, listOf("S", "'bbb'")),
            LeafData("WS", true, 37, 1, listOf("S", "WS")),
            LeafData("'ccc'", false, 38, 3, listOf("S", "'ccc'")),
        )

        assertEquals(expected_1, actual_1)
        assertEquals(expected_2, actual_2)
        assertEquals(expected_3, actual_3)
    }

    @Test
    fun comment_over_3_lines() {
        val sentence = SentenceDefault(
            """
            /* comment
             second line
             third line */
            aaa bbb ccc
        """.trimIndent()
        )
        val result = parse(sentence)
        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
        println(result.sppt!!.toStringAll)

        val actual_1 = result.sppt!!.tokensByLine(0)
        val actual_2 = result.sppt!!.tokensByLine(1)
        val actual_3 = result.sppt!!.tokensByLine(2)
        val actual_4 = result.sppt!!.tokensByLine(3)

        val expected_1 = listOf(
            LeafData("COMMENT", true, 0, 10, listOf("S", "COMMENT")),  // first line of comment
            LeafData("COMMENT", true, 10, 1, listOf("S", "COMMENT")),  // EOL inside the comment
        )
        val expected_2 = listOf(
            LeafData("COMMENT", true, 11, 12, listOf("S", "COMMENT")),  // first line of comment
            LeafData("COMMENT", true, 23, 1, listOf("S", "COMMENT")),  // EOL inside the comment
        )
        val expected_3 = listOf(
            LeafData("COMMENT", true, 24, 14, listOf("S", "COMMENT")), // rest of comment
            LeafData("WS", true, 38, 1, listOf("S", "WS")),            // EOL after comment
        )
        val expected_4 = listOf(
            LeafData("'aaa'", false, 39, 3, listOf("S", "'aaa'")),
            LeafData("WS", true, 42, 1, listOf("S", "WS")),
            LeafData("'bbb'", false, 43, 3, listOf("S", "'bbb'")),
            LeafData("WS", true, 46, 1, listOf("S", "WS")),
            LeafData("'ccc'", false, 47, 3, listOf("S", "'ccc'")),
        )

        assertEquals(expected_1, actual_1)
        assertEquals(expected_2, actual_2)
        assertEquals(expected_3, actual_3)
        assertEquals(expected_4, actual_4)
    }
}

