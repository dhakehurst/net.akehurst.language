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
package net.akehurst.language.agl.processor.natural

import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.api.processor.parserOptions
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_NaturalLanguage_Singles {

    companion object {

        private val grammarStr = this::class.java.getResource("/natural/English.agl").readText()
        var processor: LanguageProcessor = tgqlprocessor()

        var sourceFiles = arrayOf("/natural/english-sentences-valid.txt")

        fun tgqlprocessor(): LanguageProcessor {
            //val grammarStr = ClassLoader.getSystemClassLoader().getResource("vistraq/Query.ogl").readText()
            return Agl.processorFromString(grammarStr)
        }
    }

    @Test
    fun subject() {
        val goal = "subject"
        val sentence = "my name"

        val (sppt, issues) = processor.parse(sentence, parserOptions { goalRule(goal) })
        assertNotNull(sppt)
        assertEquals(emptyList(), issues)
        val resultStr = sppt.asString
        assertEquals(sentence, resultStr)
    }

}
