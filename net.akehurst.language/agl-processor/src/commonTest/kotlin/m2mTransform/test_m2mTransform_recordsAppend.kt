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
 * records of rule applications are appended in place (not copied for each entry): results are the same as before,
 * also with many items, with overrides (entries shared by two rules) and when a 'where' reads the record of a rule that it adds to
 */
class test_m2mTransform_recordsAppend {

    private companion object {
        /** a NAME (letters only) for each index */
        fun nm(i: Int): String = if (i < 26) ('a' + i).toString() else nm(i / 26 - 1) + ('a' + i % 26)
        fun sentence(n: Int) = (0 until n).joinToString("\n") { i -> if (0 == i % 2) "circle c${nm(i)} r 5" else "square s${nm(i)} { 1 2 }" }

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


        const val OVERRIDE_RULES = """
            top mapping AnyToTarget {
                domain s o:Any {}
                domain d t:Disc := Disc(label := o.${'$'}type.name) {}
            }
            top mapping CircleToTarget : AnyToTarget {
                domain s o:Circle { name == n }
                domain d t:Disc := Disc(label := 'Circle ' + n) {}
            }
        """

        const val WHERE_RULES = """
            top mapping UnitToDrawing {
                domain s u:Unit { item == items }
                domain d dr:Drawing := Drawing() { discs := a + b }
                where {
                    map all CircleToDisc { s := items  d := a }
                    map all CircleToDisc { s := items  d := b }
                }
            }
            mapping CircleToDisc {
                domain s c:Circle { name == n }
                domain d disc:Disc := Disc(label := n) {}
            }
        """
    }

    @Test
    fun override_many_items_each_target_once() = runTest {
        val n = 300
        // the circles' entries are in the records of CircleToTarget and AnyToTarget (both top), counted once, at their first place
        val expected = (0 until n).filter { 0 == it % 2 }.map { "'Circle c${nm(it)}'" } + (0 until n).filter { 1 == it % 2 }.map { "'Square'" }
        val res = transform(sentence(n), header(OVERRIDE_RULES))
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        assertEquals(expected, labels(res.targets))
        val resS = transformSuspend(sentence(n), header(OVERRIDE_RULES))
        assertTrue(resS.issues.errors.isEmpty(), resS.issues.toString())
        assertEquals(expected, labels(resS.targets))
        // AnyToTarget holds every source object: the circles' entries are the same objects as in CircleToTarget
        val any = res.record.entries.first { it.key.name.value == "AnyToTarget" }.value
        val circle = res.record.entries.first { it.key.name.value == "CircleToTarget" }.value
        assertEquals(n, any.alternatives.size)
        assertEquals(n / 2, circle.alternatives.size)
        assertEquals(any.alternatives.size, any.arguments.size)
        assertEquals(any.alternatives.size, any.entryIds.size)
        circle.alternatives.forEach { c -> assertEquals(1, any.alternatives.count { it === c }) }
    }

    @Test
    fun where_reads_record_of_rule_that_is_being_added_to() = runTest {
        val n = 100
        // the first 'map all' adds an entry per circle, the second reads the record and finds each of them (applied once per binding)
        val res = transformRootSuspend(sentence(n), header(WHERE_RULES))
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        assertEquals(1, res.targets.size, res.targets.toString())
        val discs = (res.targets[0].self as AsmStructure).getProperty(PropertyValueName("discs")).asString()
        val circles = (0 until n).filter { 0 == it % 2 }.map { "'c${nm(it)}'" }
        assertEquals(circles + circles, Regex("label = ('[^']*')").findAll(discs).map { it.groupValues[1] }.toList(), discs)
        val rec = res.record.entries.first { it.key.name.value == "CircleToDisc" }.value
        assertEquals(n / 2, rec.alternatives.size)
        assertEquals(n / 2, rec.arguments.size)
    }
}
