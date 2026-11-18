/*
 * Copyright (C) 2025 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.asm.TypesDomainSimple
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * as QVT Relations: every occurrence of a variable in the templates denotes the same value (review MT9).
 * Equal values match, different values are no match (no issue).
 */
class test_m2mTransform_repeatedVariable {

    private companion object {
        val DRAWING_TYPES = """
            namespace drawing
              data Drawing { cmp discs: List<Disc> }
              data Disc { label: String  size: String? }
        """.trimIndent()

        val shapes by lazy { shapesProcessor.typesDomain }
        val drawing by lazy { TypesDomainSimple.fromString(SimpleName("Drawing"), SentenceContextAny(), TypesString(DRAWING_TYPES)).asm!! }

        fun header(rules: String) =
            "namespace example\ntransform ShapesToDrawing(s:Shapes, d:Drawing) {\n$rules\n}"

        fun objectGraphs() = Pair(
            objectGraphSimpleAsm(shapes, null, IssueHolder(LanguageProcessorPhase.INTERPRET), LocationMapDefault()),
            objectGraphSimpleAsm(drawing, null, IssueHolder(LanguageProcessorPhase.INTERPRET), LocationMapDefault())
        )

        fun items(sentence: String, ogS: net.akehurst.language.objectgraph.api.ObjectGraphAccessorMutator) =
            @Suppress("UNCHECKED_CAST")
            ((shapesProcessor.process(sentence).asm!!.root.first() as AsmStructure).getProperty(PropertyValueName("item")) as List<Any>)
                .map { ogS.toTypedObject(it, StdLibDefault.AnyType) }

        fun transform(sentence: String, text: String) = objectGraphs().let { (ogS, ogD) ->
            Agl.transform(
                M2mTransformString(text),
                typeDomains = mapOf(DomainReference("s") to shapes, DomainReference("d") to drawing),
                accessorMutators = mapOf(shapes.name to ogS, drawing.name to ogD),
                domains = mapOf(DomainReference("s") to items(sentence, ogS)),
                ruleArguments = emptyMap(),
                targetDomainReference = DomainReference("d")
            )
        }

        suspend fun transformSuspend(sentence: String, text: String) = objectGraphs().let { (ogS, ogD) ->
            Agl.transformSuspend(
                M2mTransformString(text),
                typeDomains = mapOf(DomainReference("s") to shapes, DomainReference("d") to drawing),
                accessorMutators = mapOf(shapes.name to ogS, drawing.name to ogD),
                domains = mapOf(DomainReference("s") to items(sentence, ogS)),
                ruleArguments = emptyMap(),
                targetDomainReference = DomainReference("d")
            )
        }

        // x is used twice in one object template
        const val RULE_SAME_OBJECT = "top mapping SamePoint { domain s sq:Square { name == n  point == [ ... p:Point { number == x  number2 == x } ] } domain d l:String := n + x }"

        // x is used in two element templates of one collection template
        const val RULE_TWO_ELEMENTS = "top mapping SameNumber { domain s sq:Square { name == n  point == [ p:Point { number == x }, q:Point { number2 == x } ] } domain d l:String := n + x }"
    }

    @Test
    fun same_object_values_differ_is_no_match() = runTest {
        val sentence = "square s { 1 2, 3 4 }"
        val res = transform(sentence, header(RULE_SAME_OBJECT))
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        assertEquals(emptyList(), res.targets.map { it.untyped.asString().trim() })
        val resS = transformSuspend(sentence, header(RULE_SAME_OBJECT))
        assertTrue(resS.issues.errors.isEmpty(), resS.issues.toString())
        assertEquals(emptyList(), resS.targets.map { it.untyped.asString().trim() })
    }

    @Test
    fun same_object_values_equal_matches() = runTest {
        val sentence = "square s { 1 2, 3 3 }"
        val res = transform(sentence, header(RULE_SAME_OBJECT))
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        assertEquals(listOf("'s3'"), res.targets.map { it.untyped.asString().trim() })
        val resS = transformSuspend(sentence, header(RULE_SAME_OBJECT))
        assertTrue(resS.issues.errors.isEmpty(), resS.issues.toString())
        assertEquals(listOf("'s3'"), resS.targets.map { it.untyped.asString().trim() })
    }

    @Test
    fun two_elements_values_differ_is_no_match() {
        val res = transform("square s { 1 2, 3 4 }", header(RULE_TWO_ELEMENTS))
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        assertEquals(emptyList(), res.targets.map { it.untyped.asString().trim() })
    }

    @Test
    fun two_elements_values_equal_matches() {
        val res = transform("square s { 1 2, 3 1 }", header(RULE_TWO_ELEMENTS))
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        assertEquals(listOf("'s1'"), res.targets.map { it.untyped.asString().trim() })
    }
}
