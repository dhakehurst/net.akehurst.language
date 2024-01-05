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

import net.akehurst.language.agl.agl.parser.SentenceDefault
import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.api.sppt.LeafData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_SharedPackedParseTree_tokensByLine {

    companion object {
        private val grammarStr = """
            namespace test
            grammar Test {
                skip leaf WS = "(\s|[.])+" ;
                S = 'aaa' 'bbb' 'ccc' ;
            }
        """.trimIndent()

        val processor = Agl.processorFromString<Any, Any>(grammarStr).processor!!
    }

    @Test
    fun all_on_one_line() {
        val sentence = SentenceDefault("aaa bbb ccc")
        val result = processor.parse(sentence.text)
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
        val result = processor.parse(sentence.text)
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
        val result = processor.parse(sentence.text)
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
}

