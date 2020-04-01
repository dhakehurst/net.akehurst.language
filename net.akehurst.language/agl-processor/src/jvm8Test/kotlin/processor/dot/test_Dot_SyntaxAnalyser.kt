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
package net.akehurst.language.processor.dot

import net.akehurst.language.api.analyser.AsmElementSimple
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.agl.processor.Agl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_Dot_SyntaxAnalyser {

    companion object {
        private val grammarStr = this::class.java.getResource("/dot/Dot.agl").readText()
        var processor: LanguageProcessor = Agl.processor(grammarStr)
    }


    @Test
    fun t1() {

        val sentence = """
            graph {
               a
            }
        """.trimIndent()

        val actual = processor.process<AsmElementSimple>(sentence)

        assertNotNull(actual)
        assertEquals(null,actual.getPropertyValue("STRICT"))
        assertEquals("graph",actual.getPropertyValue("type"))
        assertEquals(null,actual.getPropertyValue("STRICT"))
        assertEquals(null,actual.getPropertyValue("STRICT"))
    }

    @Test
    fun t2() {

        val sentence = """
            graph {
               a -- b
            }
        """.trimIndent()

        val actual = processor.process<AsmElementSimple>(sentence)

        assertNotNull(actual)
    }

}