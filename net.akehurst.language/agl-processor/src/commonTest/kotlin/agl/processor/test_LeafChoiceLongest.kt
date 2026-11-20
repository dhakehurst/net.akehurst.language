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

package net.akehurst.language.agl.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.api.processor.LanguageProcessor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A leaf is matched with one regex. The alternatives of a longest choice ('|') are put longest first when they all have a fixed length,
 * so the longest one is matched. Otherwise the order is kept and a warning is given.
 */
class test_LeafChoiceLongest {

    private fun processor(grammar: String): Pair<LanguageProcessor<*, *>, List<String>> {
        val res = Agl.processorFromStringSimple(GrammarString(grammar))
        val proc = res.processor ?: error(res.issues.toString())
        return Pair(proc, res.issues.warnings.map { it.message })
    }

    private fun parses(proc: LanguageProcessor<*, *>, sentence: String, leafText: String? = null) {
        val p = proc.parse(sentence)
        assertTrue(p.issues.errors.isEmpty(), p.issues.toString())
        if (null != leafText) {
            assertTrue(p.sppt!!.toStringAll.contains("'$leafText'"), p.sppt!!.toStringAll)
        }
    }

    private fun leafWarnings(warnings: List<String>) = warnings.filter { it.startsWith("Leaf rule") }

    @Test
    fun literals_shorter_first() {
        val (proc, warnings) = processor(
            """
            namespace t
            grammar T {
                skip leaf WS = "\s+" ;
                S = ID OP ID ;
                leaf OP = '<' | '<=' ;
                leaf ID = "[a-z]+" ;
            }
            """.trimIndent()
        )
        parses(proc, "a < b", "<")
        parses(proc, "a <= b", "<=")
        assertEquals(emptyList(), leafWarnings(warnings))
    }

    @Test
    fun nested_literals() {
        val (proc, warnings) = processor(
            """
            namespace t
            grammar T {
                S = X ;
                leaf X = 'p' ('n' | 'ne' | 'e') 'x'? ;
            }
            """.trimIndent()
        )
        parses(proc, "pne", "pne")
        parses(proc, "pnex", "pnex")
        parses(proc, "pn", "pn")
        assertEquals(emptyList(), leafWarnings(warnings))
    }

    @Test
    fun fixed_length_patterns_and_literals() {
        val (proc, warnings) = processor(
            """
            namespace t
            grammar T {
                S = X ;
                leaf X = "[a-z]" | 'a1' | "[a-z][0-9][0-9]" ;
            }
            """.trimIndent()
        )
        parses(proc, "a", "a")
        parses(proc, "a1", "a1")
        parses(proc, "b12", "b12")
        assertEquals(emptyList(), leafWarnings(warnings))
    }

    @Test
    fun priority_choice_is_a_syntax_error() {
        // '<' (priority choice) was removed from the grammar language
        val res = Agl.processorFromStringSimple(
            GrammarString(
                """
                namespace t
                grammar T {
                    S = OP '=' ? ;
                    leaf OP = '<' < '<=' ;
                }
                """.trimIndent()
            )
        )
        assertTrue(res.issues.errors.isNotEmpty(), res.issues.toString())
    }

    @Test
    fun varying_length_pattern_gives_warning() {
        val (proc, warnings) = processor(
            """
            namespace t
            grammar T {
                S = NUM ;
                leaf NUM = "[0-9]+" | "[0-9]+" '.' "[0-9]+" ;
            }
            """.trimIndent()
        )
        parses(proc, "12")
        assertEquals(1, leafWarnings(warnings).size, warnings.toString())
    }
}
