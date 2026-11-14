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
import net.akehurst.language.api.processor.CompletionString
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.documentation.test_Doc_GettingStarted.Companion.SHAPES_GRAMMAR
import net.akehurst.language.documentation.test_Doc_GettingStarted.Companion.shapesProcessor
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The examples in 5_Documentation/dsl/completion.md.
 */
class test_Doc_Completion {

    private fun labels(items: List<net.akehurst.language.api.processor.CompletionItem>) = items.map { "${it.kind} ${it.label} ${it.text}" }

    @Test
    fun default_items() {
        // completion.md: "Without a completion definition"
        val result = shapesProcessor.expectedItemsAt("circle c ", 9)
        assertEquals(
            listOf(
                "SEGMENT item <item>",
                "SEGMENT radius <radius>",
                "LITERAL 'circle' circle",
                "LITERAL 'connect' connect",
                "LITERAL 'r' r",
                "LITERAL 'square' square",
            ),
            labels(result.items)
        )
    }

    @Test
    fun depth() {
        // completion.md: "Without a completion definition"
        val result = shapesProcessor.expectedItemsAt("circle c ", 9, Agl.options { completionProvider { depth(1) } })
        assertEquals(
            listOf(
                "SEGMENT connection <connection>",
                "SEGMENT radius r <NUMBER>",
                "SEGMENT shape <shape>",
                "LITERAL 'circle' circle",
                "LITERAL 'connect' connect",
                "LITERAL 'r' r",
                "LITERAL 'square' square",
            ),
            labels(result.items)
        )
    }

    @Test
    fun completion_definition() {
        // completion.md: "Example"
        val completion = """
            namespace example
            completion Shapes {
                item -> hide
                radius -> decorate { label := 'radius' description := 'the radius of the circle' }
                in circle NAME -> add [ CompletionItem() { text := 'c1' description := 'a circle name' } ]
            }
        """.trimIndent()
        val result = Agl.processorFromStringSimple(GrammarString(SHAPES_GRAMMAR), completionStr = CompletionString(completion))
        check(result.issues.errors.isEmpty()) { result.issues.toString() }
        val processor = result.processor!!

        val items = processor.expectedItemsAt("circle c ", 9).items
        assertEquals(
            listOf(
                "SEGMENT radius <radius>",
                "LITERAL 'circle' circle",
                "LITERAL 'connect' connect",
                "LITERAL 'r' r",
                "LITERAL 'square' square",
            ),
            labels(items)
        )
        assertEquals("the radius of the circle", items[0].description)

        val items2 = processor.expectedItemsAt("circle ", 7).items
        assertEquals(listOf("SEGMENT c1 c1", "PATTERN NAME <NAME>"), labels(items2))
    }
}
