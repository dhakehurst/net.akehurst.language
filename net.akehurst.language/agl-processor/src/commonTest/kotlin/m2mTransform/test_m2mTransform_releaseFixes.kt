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
 * release fixes (David 2026-11-20): MT13 rule results are kept per rule arguments; MT8 errors in the transform text are returned, not thrown
 */
class test_m2mTransform_releaseFixes {

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

        suspend fun transformRootSuspend(sentence: String, text: String) = objectGraphs().let { (ogS, ogD) ->
            Agl.transformSuspend(
                M2mTransformString(text),
                typeDomains = mapOf(DomainReference("s") to shapes, DomainReference("d") to drawing),
                accessorMutators = mapOf(shapes.name to ogS, drawing.name to ogD),
                domains = mapOf(DomainReference("s") to listOf(ogS.toTypedObject(shapesProcessor.process(sentence).asm!!.root.first(), StdLibDefault.AnyType))),
                ruleArguments = emptyMap(),
                targetDomainReference = DomainReference("d")
            )
        }

        fun labels(targets: List<net.akehurst.language.objectgraph.api.TypedObject>) =
            targets.map { (it.self as AsmStructure).getProperty(PropertyValueName("label")).asString().trim() }

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

    }

    @Test
    fun MT13_same_source_different_rule_arguments() = runTest {
        val rules = """
            top mapping UnitToDrawing {
                domain s u:Unit { item == items }
                domain d dr:Drawing := Drawing() { discs := a + b }
                where {
                    map all CircleToDisc(prefix := 'a-') { s := items  d := a }
                    map all CircleToDisc(prefix := 'b-') { s := items  d := b }
                }
            }
            mapping CircleToDisc(prefix:String) {
                domain s c:Circle { name == n }
                domain d disc:Disc := Disc(label := prefix + n) {}
            }
        """
        val res = transformRootSuspend(SENTENCE, header(rules))
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        assertEquals(1, res.targets.size, res.targets.toString())
        val discs = (res.targets[0].self as AsmStructure).getProperty(PropertyValueName("discs")).asString()
        assertEquals(listOf("'a-c'", "'b-c'"), Regex("label = ('[^']*')").findAll(discs).map { it.groupValues[1] }.toList(), discs)
    }

    @Test
    fun MT8_transform_text_errors_are_returned() = runTest {
        val text = header("top mapping Broken { domain s o:Any {} domain d t:Disc := ")
        val res = transform(SENTENCE, text)
        assertTrue(res.issues.errors.isNotEmpty(), res.issues.toString())
        assertEquals(emptyList(), res.targets)
        val resS = transformSuspend(SENTENCE, text)
        assertTrue(resS.issues.errors.isNotEmpty(), resS.issues.toString())
        assertEquals(emptyList(), resS.targets)
    }

    @Test
    fun MT15_table_row_matches_only_if_all_source_domains_match() = runTest {
        val text = """
            namespace example
            transform Pairs(s:Shapes, t:Shapes, d:Drawing) {
                top table PairName {
                    domain s :String   domain t :String   domain d :String
                    values 'a' to 'x' to 'ax'
                    values 'a' to 'y' to 'ay'
                }
            }
        """.trimIndent()
        val (ogS, ogD) = objectGraphs()
        val res = Agl.transform(
            M2mTransformString(text),
            typeDomains = mapOf(DomainReference("s") to shapes, DomainReference("t") to shapes, DomainReference("d") to drawing),
            accessorMutators = mapOf(shapes.name to ogS, drawing.name to ogD),
            domains = mapOf(
                DomainReference("s") to listOf(ogS.toTypedObject("a", StdLibDefault.String)),
                DomainReference("t") to listOf(ogS.toTypedObject("y", StdLibDefault.String))
            ),
            ruleArguments = emptyMap(),
            targetDomainReference = DomainReference("d")
        )
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        assertEquals(listOf("ay"), res.targets.map { it.self.toString() })
    }
}
