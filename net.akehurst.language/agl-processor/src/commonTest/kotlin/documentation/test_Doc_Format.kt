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
import net.akehurst.language.api.processor.FormatString
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.documentation.test_Doc_GettingStarted.Companion.SHAPES_GRAMMAR
import net.akehurst.language.documentation.test_Doc_GettingStarted.Companion.SHAPES_SENTENCE
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The examples in 5_Documentation/dsl/format.md.
 */
class test_Doc_Format {

    @Test
    fun format() {
        // format.md: "Example"
        val format = $$"""
            namespace example
            format Shapes {
                Unit -> "$[item sep $EOL]"
                Circle -> "circle $name${radius}"
                Radius -> " r $number"
                Square -> "square $name { $[point sep ', '] }"
                Point -> "$number $number2"
                Connection -> "connect $source to $target"
            }
        """.trimIndent()
        val result = Agl.processorFromStringSimple(GrammarString(SHAPES_GRAMMAR), formatterStr = FormatString(format))
        check(result.issues.errors.isEmpty()) { result.issues.toString() }
        val processor = result.processor!!

        val formatted = processor.format("circle   c r 5 circle d square s{1 2,3 4}  connect c to s")
        assertTrue(formatted.issues.errors.isEmpty(), formatted.issues.toString())
        val expected = """
            circle c r 5
            circle d
            square s { 1 2, 3 4 }
            connect c to s
        """.trimIndent()
        assertEquals(expected, formatted.sentence)

        // or format an ASM you already have
        val asm = processor.process(SHAPES_SENTENCE).asm!!
        val expected2 = """
            circle c r 5
            square s { 1 2, 3 4 }
            connect c to s
        """.trimIndent()
        assertEquals(expected2, processor.formatAsm(asm).sentence)
    }
}
