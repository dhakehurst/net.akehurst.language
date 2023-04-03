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
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_NaturalLanguage_Singles {

    companion object {

        private val grammarStr = this::class.java.getResource("/natural/English.agl").readText()
        var processor = processor()

        var sourceFiles = arrayOf("/natural/english-sentences-valid.txt")

        fun processor() =  Agl.processorFromStringDefault(grammarStr).processor!!

    }

    @Test
    fun subject() {
        val goal = "subject"
        val sentence = "my name"

        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
        val resultStr = result.sppt!!.asString
        assertEquals(sentence, resultStr)
    }

}
