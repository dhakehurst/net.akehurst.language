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

package net.akehurst.language.grammar.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.GrammarString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * issue #20: errors in a grammar point at the part of the grammar that is wrong.
 */
class test_AglGrammar_issueLocations {

    @Test
    fun embedded_grammar_not_found__location_of_embedded_item() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' Other::x ;
            }
        """.trimIndent()
        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))
        val err = pr.issues.errors.firstOrNull { it.message == "Grammar 'Other' not found" }
        assertNotNull(err, pr.issues.toString())
        val loc = assertNotNull(err.location, err.toString())
        assertEquals(3, loc.line)
        assertEquals(grammarStr.indexOf("Other::x"), loc.position)
        assertEquals("Other::x".length, loc.length)
    }

}
