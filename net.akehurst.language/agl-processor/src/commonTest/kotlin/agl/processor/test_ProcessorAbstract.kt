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

import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.parser.leftcorner.ParseOptionsDefault
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

abstract class test_ProcessorAbstract {

    fun <AsmType : Any, ContextType : Any> test(processor: LanguageProcessor<AsmType, ContextType>, goal: String, sentence: String, vararg expectedTrees: String) {
        val result = processor.parse(sentence, ParseOptionsDefault(goalRuleName = goal))

        val sppt = processor.spptParser
        expectedTrees.forEach { sppt.parse(it, true) }
        val expected = sppt.tree

        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        assertEquals(expected.toStringAll, result.sppt?.toStringAll)
        //TODO: ? assertEquals(expected, result.sppt)

    }

}