/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.processor

import net.akehurst.language.agl.grammar.grammar.ConverterToRuntimeRules
import net.akehurst.language.agl.sppt.SPPTParserDefault
import net.akehurst.language.api.processor.LanguageProcessor
import kotlin.test.assertEquals

abstract class test_ProcessorAbstract {

    fun test(processor:LanguageProcessor, goal:String, sentence:String, vararg expectedTrees:String) {
        val (actual,issues) = processor.parse(sentence,goal)

        val sppt = processor.spptParser
        expectedTrees.forEach { sppt.parse(it, true) }
        val expected = sppt.tree

        assertEquals(expected.toStringAll, actual?.toStringAll)
        assertEquals(expected, actual)
        assertEquals(emptyList(),issues)
    }

}