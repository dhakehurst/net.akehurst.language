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
import kotlin.test.assertFalse

/**
 * rule extends as override (David 2026-11-20): for a source binding where the sub-rule applies, only the sub-rule is applied,
 * the extended (generic) rule is applied to every other binding. One target per source object.
 */
class test_m2mTransform_ruleOverride {

    private companion object {
        val DRAWING_TYPES = """
            namespace drawing
              import std
              data Drawing { cmp discs: List<Disc> }
              data Disc { constructor(val label: String)  size: String? }
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

        const val SENTENCE = """
            circle c r 5
            square s { 1 2 }
        """

        const val RULES = """
            top mapping AnyToTarget {
                domain s o:Any {}
                domain d t:Disc := Disc(label := 'any') {}
            }
            top mapping CircleToTarget : AnyToTarget {
                domain s o:Circle { name == n }
                domain d t:Disc := Disc(label := 'Circle ' + n) {}
            }
        """
    }

    @Test
    fun generic_and_specialised_mapping() = runTest {
        val res = transformSuspend(SENTENCE, header(RULES))
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        assertEquals(listOf("'Circle c'", "'any'"), res.targets.map { (it.self as AsmStructure).getProperty(PropertyValueName("label")).asString().trim() }.sorted())
        val resSync = transform(SENTENCE, header(RULES))
        assertTrue(resSync.issues.errors.isEmpty(), resSync.issues.toString())
        assertEquals(listOf("'Circle c'", "'any'"), resSync.targets.map { (it.self as AsmStructure).getProperty(PropertyValueName("label")).asString().trim() }.sorted())
    }
}
