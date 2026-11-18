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

package net.akehurst.language.documentation

import net.akehurst.language.agl.Agl
import net.akehurst.language.m2mTransform.api.DomainReference
import net.akehurst.language.m2mTransformTest.processor.M2mTransformTest
import net.akehurst.language.m2mTransformTest.processor.contextFromM2mTransformDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The examples in 5_Documentation/dsl/m2m-transform-test.md.
 */
class test_Doc_M2mTransformTest {

    @Test
    fun example_and_processing() {
        val transformText = "namespace example\ntransform ShapesToDrawing(s:Shapes, d:Drawing) {\n}"
        val testText = """
            namespace example.test
              import example
            transform-test ShapesToDrawingTest for ShapesToDrawing {
              test-case OneCircle {
                domain s := Circle() { name := 'c' }
                domain d := Disc() { label := 'c' }
              }
            }
        """.trimIndent()
        val m2m = Agl.registry.agl.m2mTransform.processor!!.process(transformText, Agl.options { semanticAnalysis { enabled(false) } }).asm!!
        val result = Agl.registry.agl.m2mTransformTest.processor!!.process(
            testText,
            Agl.options { semanticAnalysis { sentenceContext(contextFromM2mTransformDomain(m2m)) } }
        )
        assertTrue(result.allIssues.errors.isEmpty(), result.allIssues.toString())
        val test = result.asm!!.allTransformTest.first()
        assertEquals("example.ShapesToDrawing", test.transform.resolved?.qualifiedName?.value)
        assertEquals(setOf(DomainReference("s"), DomainReference("d")), test.domainParameters.keys)
        assertEquals("agl-mtt", M2mTransformTest.FILE_EXTENSION)
    }
}
