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

package net.akehurst.language.agl.language.reference

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.api.processor.LanguageObject
import net.akehurst.language.api.processor.ProcessOptions
import net.akehurst.language.style.api.AglStyleDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

abstract class test_LanguageObjectAbstract {

    abstract val sut:  LanguageObject<*, *>
    abstract val processStyleOptions: ProcessOptions<AglStyleDomain, SentenceContext>
    @Test
    fun styleModel_EQ_styleString() {

        val processedStyleString = Agl.registry.agl.style.processor!!.process(
            sentence = sut.styleString,
            options = processStyleOptions
        ).let {
            assertTrue(it.allIssues.errors.isEmpty(), it.allIssues.toString())
            it.asm!!
        }

        assertEquals(sut.styleString, sut.styleDomain.asString())
        assertEquals(sut.styleDomain.asString(), processedStyleString.asString())

    }

}