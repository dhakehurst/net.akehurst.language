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
 * a mapping does its steps in the order their variables need (review MT12), as a relation does:
 * create the target, record the mapping, each 'where' call, set the target's '{ ... }' properties.
 */
class test_m2mTransform_mappingStepOrder {

    private companion object {
        val DRAWING_TYPES = """
            namespace drawing
              import std
              data Drawing { cmp discs: List<Disc> }
              data Disc { constructor(val label: String)  size: String? }
        """.trimIndent()

        val shapes by lazy { shapesProcessor.typesDomain }
        val drawing by lazy { TypesDomainSimple.fromString(SimpleName("Drawing"), SentenceContextAny(), TypesString(DRAWING_TYPES)).asm!! }

        const val TABLE = """
            table Up {
                domain s :String /**/ domain d :String
                /*=================================*/
                values 's' to 'S'
            }
        """

        fun header(rules: String) =
            "namespace example\ntransform ShapesToDrawing(s:Shapes, d:Drawing) {\n$rules\n$TABLE\n}"

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

        const val SENTENCE = "square s { 1 2 }"
    }

    @Test
    fun plain_target_uses_where_variable() = runTest {
        val rule = "top mapping SqName { domain s sq:Square { name == n } domain d l:String := x where { map Up { s := n d := x } } }"
        val res = transformSuspend(SENTENCE, header(rule))
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        assertEquals(listOf("'S'"), res.targets.map { it.untyped.asString().trim() })
        val resSync = transform(SENTENCE, header(rule))
        assertTrue(resSync.issues.errors.isEmpty(), resSync.issues.toString())
        assertEquals(listOf("'S'"), resSync.targets.map { it.untyped.asString().trim() })
    }

    @Test
    fun constructor_argument_uses_where_variable() = runTest {
        val rule = "top mapping SqDisc { domain s sq:Square { name == n } domain d disc:Disc := Disc(label := x) { } where { map Up { s := n d := x } } }"
        val res = transformSuspend(SENTENCE, header(rule))
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        assertEquals(listOf("'S'"), res.targets.map { (it.self as AsmStructure).getProperty(PropertyValueName("label")).asString().trim() })
    }

    @Test
    fun where_uses_target() = runTest {
        val rule = "top mapping SqDisc { domain s sq:Square { name == n } domain d disc:Disc := Disc(label := n) { size := x } where { map Up { s := disc.label d := x } } }"
        val res = transformSuspend(SENTENCE, header(rule))
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        assertEquals(listOf("'S'"), res.targets.map { (it.self as AsmStructure).getProperty(PropertyValueName("size")).asString().trim() })
    }

    @Test
    fun target_and_where_need_each_other_is_error() = runTest {
        val rule = "top mapping SqName { domain s sq:Square { name == n } domain d l:String := x where { map Up { s := l d := x } } }"
        val res = transformSuspend(SENTENCE, header(rule))
        assertFalse(res.issues.errors.isEmpty(), res.issues.toString())
        assertTrue(res.issues.errors.any { it.message.startsWith("In mapping 'SqName' the target and the 'where' clause need each other's variables") }, res.issues.toString())
        assertEquals(emptyList(), res.targets.map { it.untyped.asString().trim() })
    }
}
