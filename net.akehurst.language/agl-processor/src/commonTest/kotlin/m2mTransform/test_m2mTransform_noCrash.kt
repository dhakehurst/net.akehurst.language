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
 * cases that used to throw an exception, they must give an issue or no match (review MT1, MT2, MT10, MT11)
 */
class test_m2mTransform_noCrash {

    private companion object {
        const val SENTENCE = "circle c r 5  square s { 1 2, 3 4 }  circle d r 50  connect c to s"
        val DRAWING_TYPES = """
            namespace drawing
              data Drawing { cmp discs: List<Disc> }
              data Disc { label: String  size: String? }
        """.trimIndent()

        val shapes by lazy { shapesProcessor.typesDomain }
        val drawing by lazy { TypesDomainSimple.fromString(SimpleName("Drawing"), SentenceContextAny(), TypesString(DRAWING_TYPES)).asm!! }

        fun header(rules: String, extends: String = "") =
            "namespace example\ntransform ShapesToDrawing(s:Shapes, d:Drawing)$extends {\n$rules\n}"

        fun objectGraphs() = Pair(
            objectGraphSimpleAsm(shapes, null, IssueHolder(LanguageProcessorPhase.INTERPRET), LocationMapDefault()),
            objectGraphSimpleAsm(drawing, null, IssueHolder(LanguageProcessorPhase.INTERPRET), LocationMapDefault())
        )

        fun items(ogS: net.akehurst.language.objectgraph.api.ObjectGraphAccessorMutator) =
            @Suppress("UNCHECKED_CAST")
            ((shapesProcessor.process(SENTENCE).asm!!.root.first() as AsmStructure).getProperty(PropertyValueName("item")) as List<Any>)
                .map { ogS.toTypedObject(it, StdLibDefault.AnyType) }

        fun transform(text: String) = objectGraphs().let { (ogS, ogD) ->
            Agl.transform(
                M2mTransformString(text),
                typeDomains = mapOf(DomainReference("s") to shapes, DomainReference("d") to drawing),
                accessorMutators = mapOf(shapes.name to ogS, drawing.name to ogD),
                domains = mapOf(DomainReference("s") to items(ogS)),
                ruleArguments = emptyMap(),
                targetDomainReference = DomainReference("d")
            )
        }

        suspend fun transformSuspend(text: String) = objectGraphs().let { (ogS, ogD) ->
            Agl.transformSuspend(
                M2mTransformString(text),
                typeDomains = mapOf(DomainReference("s") to shapes, DomainReference("d") to drawing),
                accessorMutators = mapOf(shapes.name to ogS, drawing.name to ogD),
                domains = mapOf(DomainReference("s") to items(ogS)),
                ruleArguments = emptyMap(),
                targetDomainReference = DomainReference("d")
            )
        }
    }

    @Test
    fun MT1_transform_extends_parses() {
        val res = Agl.registry.agl.m2mTransform.processor!!.parse(header("", " : Other"))
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        val sa = Agl.registry.agl.m2mTransform.processor!!.syntaxAnalysis(res.sppt!!)
        assertTrue(sa.issues.errors.isEmpty(), sa.issues.toString())
        assertEquals(listOf("Other"), sa.asm!!.allTransformRuleSet.first().extends.map { it.nameOrQName.value })
    }

    @Test
    fun MT2_table_with_where_gives_error() {
        val text = header(
            """
            top mapping CircleToDisc { domain s c:Circle { name == n } domain d disc:Disc := Disc() { label := n } }
            table T { domain s :String domain d :String values 'a' to 'b' where { map CircleToDisc { s := 'x' d := y } } }
            """
        )
        val res = Agl.registry.agl.m2mTransform.processor!!.parse(text)
        val sa = Agl.registry.agl.m2mTransform.processor!!.syntaxAnalysis(res.sppt!!)
        assertEquals(listOf("'when' and 'where' are not supported in table rule 'T'."), sa.issues.errors.map { it.message })
    }

    @Test
    fun MT10_collection_size_differs_is_no_match() {
        // the square has 2 points, the template (without '...') has 1, the circles have no points
        val res = transform(header("top mapping SquarePoint { domain s sq:Square { name == n  point == [ p:Point { number == x } ] } domain d l:String := n + x }"))
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        assertEquals(emptyList(), res.targets.map { it.untyped.asString() })
    }

    @Test
    fun MT10_collection_size_same_matches() {
        val res = transform(header("top mapping SquarePoint { domain s sq:Square { name == n  point == [ p:Point { number == x }, q:Point { number == y } ] } domain d l:String := n + x + y }"))
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        assertTrue(res.targets.isNotEmpty())
    }

    @Test
    fun MT10_collection_property_without_value_is_no_match() = runTest {
        // circles have no 'point' value, only the square matches
        val rules = "top mapping A { domain s sh:Shape { name == n  point == [ ... p:Point { number == x } ] } domain d l:String := n + x }"
        val res = transform(header(rules))
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        assertEquals(listOf("'s1'", "'s3'"), res.targets.map { it.untyped.asString().trim() })
        val resS = transformSuspend(header(rules))
        assertTrue(resS.issues.errors.isEmpty(), resS.issues.toString())
        assertEquals(listOf("'s1'", "'s3'"), resS.targets.map { it.untyped.asString().trim() })
    }

    @Test
    fun MT11_when_not_boolean_gives_error() {
        val res = transform(header("top mapping A { domain s c:Circle { name == n } domain d l:String := n  when { n } }"))
        assertEquals(1, res.issues.errors.map { it.message }.toSet().size, res.issues.toString())
        assertTrue(res.issues.errors.first().message.contains("must give a Boolean"), res.issues.toString())
        assertEquals(emptyList(), res.targets.map { it.untyped.asString() })
    }
}
