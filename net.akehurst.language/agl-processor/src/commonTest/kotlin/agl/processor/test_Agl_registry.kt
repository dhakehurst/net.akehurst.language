/**
 * Copyright (C) 2025 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.LanguageProcessor
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_Agl_registry_agl {

    companion object {
        fun checkProcessor(processor: LanguageProcessor<*,*>?) {
            assertNotNull(processor)
            assertTrue(processor.issues.isEmpty())
            assertNotNull(processor.targetGrammar)
            assertNotNull(processor.baseTypeModel)
            assertNotNull(processor.asmTransformModel)
            assertNotNull(processor.typeModel)
            assertNotNull(processor.crossReferenceModel)
            assertNotNull(processor.spptParser)
            assertNotNull(processor.targetRuleSet)
            assertNotNull(processor.targetAsmTransformRuleSet)
        }
    }

    @Test
    fun test_Agl_registry_agl_base() {
        checkProcessor(Agl.registry.agl.base.processor)
    }

    @Test
    fun test_Agl_registry_agl_grammar() {
        checkProcessor(Agl.registry.agl.grammar.processor)
    }

    @Test
    fun test_Agl_registry_agl_expressions() {
        checkProcessor(Agl.registry.agl.expressions.processor)
    }

    @Test
    fun test_Agl_registry_agl_types() {
        checkProcessor(Agl.registry.agl.types.processor)
    }

    @Test
    fun test_Agl_registry_agl_transform() {
        checkProcessor(Agl.registry.agl.transform.processor)
    }

    @Test
    fun test_Agl_registry_agl_crossReference() {
        checkProcessor(Agl.registry.agl.crossReference.processor)
    }

    @Test
    fun test_Agl_registry_agl_style() {
        checkProcessor(Agl.registry.agl.style.processor)
    }

    @Test
    fun test_Agl_registry_agl_format() {
        checkProcessor(Agl.registry.agl.format.processor)
    }
}