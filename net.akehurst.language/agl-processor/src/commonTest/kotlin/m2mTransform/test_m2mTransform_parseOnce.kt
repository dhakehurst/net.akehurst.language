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

package net.akehurst.language.m2mTransform.processor

import kotlinx.coroutines.test.runTest
import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.simple.SentenceContextAny
import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.api.processor.M2mTransformString
import net.akehurst.language.api.processor.TypesString
import net.akehurst.language.asm.api.AsmStructure
import net.akehurst.language.asm.api.PropertyValueName
import net.akehurst.language.asm.simple.AnyExt.asString
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.documentation.test_Doc_GettingStarted.Companion.shapesProcessor
import net.akehurst.language.expressions.processor.objectGraphSimpleAsm
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.m2mTransform.api.DomainReference
import net.akehurst.language.m2mTransform.api.M2mTransformDomain
import net.akehurst.language.objectgraph.api.ObjectGraphAccessorMutator
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.asm.TypesDomainSimple
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * a transform parsed once with Agl.m2mTransformDomain can be run many times with Agl.transform / Agl.transformSuspend,
 * giving the same results as the versions that take the transform text
 */
class test_m2mTransform_parseOnce {

    private companion object {
        val DRAWING_TYPES = """
            namespace drawing
              import std
              data Drawing { cmp discs: List<Disc> }
              data Disc { constructor(val label: String)  size: String? }
        """.trimIndent()

        val shapes by lazy { shapesProcessor.typesDomain }
        val drawing by lazy { TypesDomainSimple.fromString(SimpleName("Drawing"), SentenceContextAny(), TypesString(DRAWING_TYPES)).asm!! }
        val typeDomains by lazy { mapOf(DomainReference("s") to shapes, DomainReference("d") to drawing) }

        val TEXT = M2mTransformString(
            """
            namespace example
            transform ShapesToDrawing(s:Shapes, d:Drawing) {
                top mapping SqDisc { domain s sq:Square { name == n } domain d disc:Disc := Disc(label := n) { } }
            }
            """.trimIndent()
        )

        const val SENTENCE = "square s { 1 2 }"

        fun objectGraphs() = Pair(
            objectGraphSimpleAsm(shapes, null, IssueHolder(LanguageProcessorPhase.INTERPRET), LocationMapDefault()),
            objectGraphSimpleAsm(drawing, null, IssueHolder(LanguageProcessorPhase.INTERPRET), LocationMapDefault())
        )

        fun items(sentence: String, ogS: ObjectGraphAccessorMutator) =
            @Suppress("UNCHECKED_CAST")
            ((shapesProcessor.process(sentence).asm!!.root.first() as AsmStructure).getProperty(PropertyValueName("item")) as List<Any>)
                .map { ogS.toTypedObject(it, StdLibDefault.AnyType) }

        fun parsed(): M2mTransformDomain {
            val res = Agl.m2mTransformDomain(TEXT, typeDomains)
            assertTrue(res.allIssues.errors.isEmpty(), res.allIssues.toString())
            return assertNotNull(res.asm)
        }

        fun M2MTransformResult.targetStrings() = targets.map { it.untyped.asString().trim() }
    }

    @Test
    fun run_many_times_gives_same_result_as_text_version() {
        val m2mDomain = parsed()
        val expected = objectGraphs().let { (ogS, ogD) ->
            Agl.transform(TEXT, typeDomains, mapOf(shapes.name to ogS, drawing.name to ogD), mapOf(DomainReference("s") to items(SENTENCE, ogS)), emptyMap(), DomainReference("d"))
        }
        assertTrue(expected.issues.errors.isEmpty(), expected.issues.toString())
        assertEquals(1, expected.targets.size)
        repeat(3) {
            val res = objectGraphs().let { (ogS, ogD) ->
                Agl.transform(m2mDomain, mapOf(shapes.name to ogS, drawing.name to ogD), mapOf(DomainReference("s") to items(SENTENCE, ogS)), emptyMap(), DomainReference("d"))
            }
            assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
            assertEquals(expected.targetStrings(), res.targetStrings())
        }
    }

    @Test
    fun run_many_times_suspend() = runTest {
        val m2mDomain = parsed()
        val expected = objectGraphs().let { (ogS, ogD) ->
            Agl.transformSuspend(TEXT, typeDomains, mapOf(shapes.name to ogS, drawing.name to ogD), mapOf(DomainReference("s") to items(SENTENCE, ogS)), emptyMap(), DomainReference("d"))
        }
        assertTrue(expected.issues.errors.isEmpty(), expected.issues.toString())
        repeat(3) {
            val (ogS, ogD) = objectGraphs()
            val res = Agl.transformSuspend(m2mDomain, mapOf(shapes.name to ogS, drawing.name to ogD), mapOf(DomainReference("s") to items(SENTENCE, ogS)), emptyMap(), DomainReference("d"))
            assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
            assertEquals(expected.targetStrings(), res.targetStrings())
        }
    }

    @Test
    fun errors_in_text_are_issues_of_the_parse_result() {
        val res = Agl.m2mTransformDomain(M2mTransformString("namespace example transform T(s:Shapes, d:Drawing) { top mapping"), typeDomains)
        assertTrue(res.allIssues.errors.isNotEmpty())
    }

    @Test
    fun unknown_transform_name_is_an_error() {
        val m2mDomain = parsed()
        val (ogS, ogD) = objectGraphs()
        val res = Agl.transform(
            m2mDomain, mapOf(shapes.name to ogS, drawing.name to ogD), mapOf(DomainReference("s") to items(SENTENCE, ogS)), emptyMap(), DomainReference("d"),
            transformName = SimpleName("NoSuchTransform")
        )
        assertEquals(listOf("Transform 'NoSuchTransform' not found."), res.issues.errors.map { it.message })
    }
}
