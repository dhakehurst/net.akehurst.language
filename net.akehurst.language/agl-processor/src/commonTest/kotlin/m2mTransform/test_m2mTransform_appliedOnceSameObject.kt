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
 * As QVT Relations, a rule is applied at most once for the same binding.
 * The same binding means each variable holds the same object: for an AsmStructure its fixed identity,
 * not equal property values (David 2026-11-27).
 */
class test_m2mTransform_appliedOnceSameObject {

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

        const val RULE_CIRCLE_NAME = "top mapping CircleName { domain s c:Circle { name == n } domain d l:String := n }"
    }

    @Test
    fun two_circles_with_equal_values_each_get_the_rule_applied() = runTest {
        // equal by value (AsmStructure.equalTo: same type, equal properties), but two different structures
        val sentence = "circle c r 5 circle c r 5"
        val res = transform(sentence, header(RULE_CIRCLE_NAME))
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        assertEquals(listOf("'c'", "'c'"), res.targets.map { it.untyped.asString().trim() })
        val resS = transformSuspend(sentence, header(RULE_CIRCLE_NAME))
        assertTrue(resS.issues.errors.isEmpty(), resS.issues.toString())
        assertEquals(listOf("'c'", "'c'"), resS.targets.map { it.untyped.asString().trim() })
    }

    @Test
    fun the_same_circle_twice_gets_the_rule_applied_once() = runTest {
        val sentence = "circle c r 5"
        val res = objectGraphs().let { (ogS, ogD) ->
            val sq = items(sentence, ogS).first()
            Agl.transform(
                M2mTransformString(header(RULE_CIRCLE_NAME)),
                typeDomains = mapOf(DomainReference("s") to shapes, DomainReference("d") to drawing),
                accessorMutators = mapOf(shapes.name to ogS, drawing.name to ogD),
                domains = mapOf(DomainReference("s") to listOf(sq, sq)),
                ruleArguments = emptyMap(),
                targetDomainReference = DomainReference("d")
            )
        }
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        assertEquals(listOf("'c'"), res.targets.map { it.untyped.asString().trim() })
    }
}
