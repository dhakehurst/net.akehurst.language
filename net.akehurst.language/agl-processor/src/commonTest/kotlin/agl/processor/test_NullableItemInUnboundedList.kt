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
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A list with no maximum whose item can be empty gives infinitely many trees for a sentence, warn about it.
 */
class test_NullableItemInUnboundedList {

    private fun warnings(grammar: String): List<String> {
        val res = Agl.processorFromStringSimple(GrammarString(grammar))
        res.processor ?: error(res.issues.toString())
        return res.issues.warnings.map { it.message }.filter { it.contains("infinitely many trees") }
    }

    @Test
    fun list_of_optional() {
        val w = warnings(
            """
            namespace t
            grammar T {
                S = A* ;
                A = 'a'? ;
            }
            """.trimIndent()
        )
        assertEquals(listOf("In rule 'S', the list of 'A' has no maximum and its item can be empty: a sentence has infinitely many trees."), w)
    }

    @Test
    fun list_of_indirectly_nullable() {
        val w = warnings(
            """
            namespace t
            grammar T {
                S = B+ ;
                B = C D ;
                C = 'c' | E ;
                E = ;
                D = 'd'* ;
            }
            """.trimIndent()
        )
        assertEquals(listOf("In rule 'S', the list of 'B' has no maximum and its item can be empty: a sentence has infinitely many trees."), w)
    }

    @Test
    fun list_with_max_no_warning() {
        val w = warnings(
            """
            namespace t
            grammar T {
                S = A 0..3 ;
                A = 'a'? ;
            }
            """.trimIndent()
        )
        assertEquals(emptyList(), w)
    }

    @Test
    fun list_of_not_nullable_no_warning() {
        val w = warnings(
            """
            namespace t
            grammar T {
                S = A* ;
                A = 'a' B ;
                B = 'b'? ;
            }
            """.trimIndent()
        )
        assertEquals(emptyList(), w)
    }

    @Test
    fun separated_list_nullable_item_only_no_warning() {
        // the separators bound the number of items
        val w = warnings(
            """
            namespace t
            grammar T {
                S = [A / ',']* ;
                A = 'a'? ;
            }
            """.trimIndent()
        )
        assertEquals(emptyList(), w)
    }

    @Test
    fun separated_list_nullable_item_and_separator() {
        val w = warnings(
            """
            namespace t
            grammar T {
                S = [A / C]+ ;
                A = 'a'? ;
                C = ','? ;
            }
            """.trimIndent()
        )
        assertEquals(listOf("In rule 'S', the separated list of 'A' has no maximum and both its item and separator can be empty: a sentence has infinitely many trees."), w)
    }
}
