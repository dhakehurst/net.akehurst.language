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

import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.api.parser.InputLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_SharedPackedParseTree_tokensByLine {

    companion object {
        private val grammarStr = """
            namespace test
            grammar Test {
                skip WS = "\s+" ;
                S = 'aaa' 'bbb' 'ccc' ;
            }
        """.trimIndent()

        val processor = Agl.processorFromString<Any, Any>(grammarStr)
    }

    @Test
    fun all_on_one_line() {
        val result = processor.parse("aaa bbb ccc")
        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)
        val actual = result.sppt!!.tokensByLine(0)

        assertEquals("aaa", actual[0].matchedText)
        assertEquals(InputLocation(0, 1, 1, 3), actual[0].location)
        assertEquals(" ", actual[1].matchedText)
        assertEquals(InputLocation(3, 4, 1, 1), actual[1].location)
        assertEquals("bbb", actual[2].matchedText)
        assertEquals(InputLocation(4, 5, 1, 3), actual[2].location)
        assertEquals(" ", actual[3].matchedText)
        assertEquals(InputLocation(7, 8, 1, 1), actual[3].location)
        assertEquals("ccc", actual[4].matchedText)
        assertEquals(InputLocation(8, 9, 1, 3), actual[4].location)
    }

    @Test
    fun separate_lines() {
        val result = processor.parse(
            """
            aaa
            bbb
            ccc
        """.trimIndent()
        )
        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)
        val actual_1 = result.sppt!!.tokensByLine(0)
        val actual_2 = result.sppt!!.tokensByLine(1)
        val actual_3 = result.sppt!!.tokensByLine(2)

        assertEquals(2, actual_1.size)
        assertEquals("aaa", actual_1[0].matchedText)
        assertEquals(InputLocation(0, 1, 1, 3), actual_1[0].location)
        assertEquals("\n", actual_1[1].matchedText)
        assertEquals(InputLocation(3, 4, 1, 1), actual_1[1].location)

        assertEquals(2, actual_2.size)
        assertEquals("bbb", actual_2[0].matchedText)
        assertEquals(InputLocation(4, 1, 2, 3), actual_2[0].location)
        assertEquals("\n", actual_2[1].matchedText)
        assertEquals(InputLocation(7, 4, 2, 1), actual_2[1].location)

        assertEquals(1, actual_3.size)
        assertEquals("ccc", actual_3[0].matchedText)
        assertEquals(InputLocation(8, 1, 3, 3), actual_3[0].location)
    }

    @Test
    fun separate_lines_with_indent() {
        val result = processor.parse(
            """
            aaa
              bbb
            ccc
        """.trimIndent()
        )
        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)
        val actual = result.sppt!!.tokensByLine(0)

        val actual_1 = result.sppt!!.tokensByLine(0)
        val actual_2 = result.sppt!!.tokensByLine(1)
        val actual_3 = result.sppt!!.tokensByLine(2)

        assertEquals(2, actual_1.size)
        assertEquals("aaa", actual_1[0].matchedText)
        assertEquals(InputLocation(0, 1, 1, 3), actual_1[0].location)
        assertEquals("\n", actual_1[1].matchedText)
        assertEquals(InputLocation(3, 4, 1, 1), actual_1[1].location)

        assertEquals(3, actual_2.size)
        assertEquals("  ", actual_2[0].matchedText)
        assertEquals(InputLocation(4, 1, 2, 2), actual_2[0].location)
        assertEquals("bbb", actual_2[1].matchedText)
        assertEquals(InputLocation(6, 3, 2, 3), actual_2[1].location)
        assertEquals("\n", actual_2[2].matchedText)
        assertEquals(InputLocation(9, 6, 2, 1), actual_2[2].location)

        assertEquals(1, actual_3.size)
        assertEquals("ccc", actual_3[0].matchedText)
        assertEquals(InputLocation(10, 1, 3, 3), actual_3[0].location)
    }
}

