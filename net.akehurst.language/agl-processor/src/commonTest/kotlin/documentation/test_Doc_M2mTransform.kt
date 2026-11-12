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
import net.akehurst.language.objectgraph.api.TypedObject
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.asm.TypesDomainSimple
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The examples in 5_Documentation/dsl/m2m-transform.md.
 */
class test_Doc_M2mTransform {

    private companion object {
        const val SENTENCE = "circle c r 5  square s { 1 2, 3 4 }  circle d r 50  connect c to s"
        val DRAWING_TYPES = """
            namespace drawing
              data Drawing { cmp discs: List<Disc> }
              data Disc { label: String  size: String? }
        """.trimIndent()

        val shapes by lazy { shapesProcessor.typesDomain }
        val drawing by lazy {
            val res = TypesDomainSimple.fromString(SimpleName("Drawing"), SentenceContextAny(), TypesString(DRAWING_TYPES))
            assertTrue(res.allIssues.errors.isEmpty(), res.allIssues.toString())
            res.asm!!
        }
        val ogS by lazy { objectGraphSimpleAsm(shapes, null, IssueHolder(LanguageProcessorPhase.INTERPRET), LocationMapDefault()) }
        val ogD by lazy { objectGraphSimpleAsm(drawing, null, IssueHolder(LanguageProcessorPhase.INTERPRET), LocationMapDefault()) }
        val unit by lazy { shapesProcessor.process(SENTENCE).asm!!.root.first() as AsmStructure }
        val items by lazy {
            @Suppress("UNCHECKED_CAST")
            (unit.getProperty(PropertyValueName("item")) as List<Any>).map { ogS.toTypedObject(it, StdLibDefault.AnyType) }
        }

        fun header(rules: String) =
            "namespace example\ntransform ShapesToDrawing(s:Shapes, d:Drawing) {\n$rules\n}"

        fun transform(rules: String, input: List<TypedObject> = items, source: String = "s", target: String = "d"): List<String> {
            val result = Agl.transform(
                M2mTransformString(header(rules)),
                typeDomains = mapOf(DomainReference("s") to shapes, DomainReference("d") to drawing),
                accessorMutators = mapOf(shapes.name to ogS, drawing.name to ogD),
                domains = mapOf(DomainReference(source) to input),
                ruleArguments = emptyMap(),
                targetDomainReference = DomainReference(target)
            )
            assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
            return result.targets.map { it.untyped.asString().trim() }
        }

        fun disc(label: String, size: String? = null) = when (size) {
            null -> ":Disc {\n  label = '$label'\n}"
            else -> ":Disc {\n  label = '$label'\n  size = '$size'\n}"
        }
    }

    @Test
    fun first_transform() {
        // m2m-transform.md: "A first transform"
        val actual = transform(
            """
            top mapping CircleToDisc {
              domain s c:Circle { name == n  radius == Radius { number == r } }
              domain d disc:Disc := Disc() { label := n  size := r }
            }
            """
        )
        assertEquals(listOf(disc("c", "5"), disc("d", "50")), actual)
    }

    @Test
    fun nested_template_does_not_match_missing_property() {
        // m2m-transform.md: "A first transform" - no value means no match (D20, as QVT Relations)
        val input = (shapesProcessor.process("circle c r 5  circle e").asm!!.root.first() as AsmStructure).let {
            @Suppress("UNCHECKED_CAST")
            (it.getProperty(PropertyValueName("item")) as List<Any>).map { ogS.toTypedObject(it, StdLibDefault.AnyType) }
        }
        val actual = transform(
            """
            top mapping CircleToDisc {
              domain s c:Circle { name == n  radius == Radius { number == r } }
              domain d disc:Disc := Disc() { label := n  size := r }
            }
            """, input = input
        )
        assertEquals(listOf(disc("c", "5")), actual)
    }

    @Test
    fun relation_both_directions() {
        // m2m-transform.md: "Mapping and relation"
        val rules = """
            top relation CircleDisc {
              domain s c:Circle { name == n }
              domain d disc:Disc { label == n }
            }
        """
        val result = Agl.transform(
            M2mTransformString(header(rules)),
            mapOf(DomainReference("s") to shapes, DomainReference("d") to drawing),
            mapOf(shapes.name to ogS, drawing.name to ogD),
            mapOf(DomainReference("s") to items), emptyMap(), DomainReference("d")
        )
        assertEquals(listOf(disc("c"), disc("d")), result.targets.map { it.untyped.asString().trim() })

        val back = transform(rules, input = result.targets, source = "d", target = "s")
        assertEquals(listOf(":Circle {\n  name = 'c'\n}", ":Circle {\n  name = 'd'\n}"), back)
    }

    @Test
    fun pivot() {
        // m2m-transform.md: "Other parts"
        val actual = transform(
            """
            top relation CircleDisc {
              pivot n:String
              domain s c:Circle { name == n }
              domain d disc:Disc { label == n }
            }
            """
        )
        assertEquals(listOf(disc("c"), disc("d")), actual)
    }

    @Test
    fun transform_test_and_import_types_parse() {
        // m2m-transform.md: "Other parts" - parsed only, nothing runs them yet
        val proc = Agl.registry.agl.m2mTransform.processor!!
        val test = """
            namespace example
            transform-test ShapesToDrawingTest(s:Shapes, d:Drawing) {
              test-case OneCircle {
                domain s := Circle() { name := 'c' }
                domain d := Disc() { label := 'c' }
              }
            }
        """.trimIndent()
        val r1 = proc.parse(test, Agl.parseOptions { goalRuleName("testUnit") })
        assertTrue(r1.issues.errors.isEmpty(), r1.issues.toString())
        val withImport = "namespace example\ntransform ShapesToDrawing(s:Shapes, d:Drawing) {\n  import-types drawing\n}"
        val r2 = proc.parse(withImport)
        assertTrue(r2.issues.errors.isEmpty(), r2.issues.toString())
    }

    @Test
    fun when_condition() {
        // m2m-transform.md: "when"
        val actual = transform(
            """
            top mapping CircleToDisc {
              domain s c:Circle { name == n }
              domain d disc:Disc := Disc() { label := n }
              when { n == 'c' }
            }
            """
        )
        assertEquals(listOf(disc("c")), actual)
    }

    @Test
    fun when_related() {
        // m2m-transform.md: "when" - related (D23)
        val actual = transform(
            """
            top relation CircleDisc {
              domain s c:Circle { name == n }
              domain d disc:Disc { label == n }
            }
            top mapping CircleToLabel {
              domain s c:Circle { name == n }
              domain d l:String := 'disc ' + x.label
              when { related CircleDisc { s := c  d := x } }
            }
            """
        )
        assertEquals(setOf(disc("c"), disc("d"), "'disc c'", "'disc d'"), actual.toSet())
    }

    @Test
    fun when_mapped() {
        // m2m-transform.md: "when" - mapped (D23), fails until implemented
        val actual = transform(
            """
            top mapping CircleToDisc {
              domain s c:Circle { name == n }
              domain d disc:Disc := Disc() { label := n }
            }
            top mapping CircleToLabel {
              domain s c:Circle { name == n }
              domain d l:String := 'disc ' + x.label
              when { mapped CircleToDisc { s := c  d := x } }
            }
            """
        )
        assertEquals(setOf(disc("c"), disc("d"), "'disc c'", "'disc d'"), actual.toSet())
    }

    @Test
    fun when_rule_applied_first_order_does_not_matter() {
        // m2m-transform.md: "when" - a rule in when is applied first if needed
        val actual = transform(
            """
            top mapping CircleToLabel {
              domain s c:Circle { name == n }
              domain d l:String := 'disc ' + x.label
              when { related CircleDisc { s := c  d := x } }
            }
            top relation CircleDisc {
              domain s c:Circle { name == n }
              domain d disc:Disc { label == n }
            }
            """
        )
        assertEquals(setOf(disc("c"), disc("d"), "'disc c'", "'disc d'"), actual.toSet())
        assertEquals(4, actual.size)
    }

    @Test
    fun rule_applied_once_per_binding() {
        // m2m-transform.md: "Calling rules" - a top rule also called from where gives no duplicates
        val actual = transform(
            """
            top mapping UnitToDrawing {
              domain s u:Unit { item == items }
              domain d dr:Drawing := Drawing() { discs := ds }
              where { map all CircleToDisc { s := items  d := ds } }
            }
            top mapping CircleToDisc {
              domain s c:Circle { name == n }
              domain d disc:Disc := Disc() { label := n }
            }
            """,
            input = listOf(ogS.toTypedObject(unit, StdLibDefault.AnyType)) + items
        )
        assertEquals(3, actual.size, actual.joinToString("\n"))   // the Drawing and the 2 discs, each once
        assertEquals(2, actual.count { it == disc("c") || it == disc("d") })
    }

    @Test
    fun table() {
        // m2m-transform.md: "table"
        val actual = transform(
            """
            top mapping CircleToDisc {
              domain s c:Circle { name == n  radius == Radius { number == r } }
              domain d disc:Disc := Disc() { label := n  size := sz }
              where { map SizeName { s := r  d := sz } }
            }
            table SizeName {
              domain s :String   domain d :String
              values '5'    to   'small'
              values '50'   to   'big'
            }
            """
        )
        assertEquals(listOf(disc("c", "small"), disc("d", "big")), actual)
    }

    @Test
    fun calling_rules_map_all_with_parameter() {
        // m2m-transform.md: "Calling rules"
        val actual = transform(
            """
            top mapping UnitToDrawing {
              domain s u:Unit { item == items }
              domain d dr:Drawing := Drawing() { discs := ds }
              where { map all CircleToDisc(prefix := 'disc-') { s := items  d := ds } }
            }
            mapping CircleToDisc(prefix:String) {
              domain s c:Circle { name == n }
              domain d disc:Disc := Disc() { label := prefix + n }
            }
            """,
            input = listOf(ogS.toTypedObject(unit, StdLibDefault.AnyType))
        )
        val expected = """
            :Drawing {
              discs = [
                :Disc {
                  label = 'disc-c'
                }
                :Disc {
                  label = 'disc-d'
                }
              ]
            }
        """.trimIndent()
        assertEquals(listOf(expected), actual)
    }

    @Test
    fun abstract_rules() {
        // m2m-transform.md: "Abstract rules"
        val actual = transform(
            """
            abstract top rule ShapeToLabel {
              domain s sh:Shape
              domain d l:String
            }
            mapping CircleLabel : ShapeToLabel {
              domain s sh:Circle { name == n }
              domain d l:String := 'circle ' + n
            }
            mapping SquareLabel : ShapeToLabel {
              domain s sh:Square { name == n }
              domain d l:String := 'square ' + n
            }
            """
        )
        assertEquals(setOf("'circle c'", "'circle d'", "'square s'"), actual.toSet())
    }

    @Test
    fun collection_templates() {
        // m2m-transform.md: "Collection templates"
        val actual = transform(
            """
            top mapping SquarePoint {
              domain s sq:Square { name == n  point == [ ... p:Point { number == x } ] }
              domain d l:String := n + ' has x=' + x
            }
            """
        )
        assertEquals(listOf("'s has x=1'", "'s has x=3'"), actual)
    }

    @Test
    fun transform_suspend() = runTest {
        // m2m-transform.md: "Running a transform"
        val result = Agl.transformSuspend(
            M2mTransformString(header("top mapping CircleToDisc { domain s c:Circle { name == n } domain d disc:Disc := Disc() { label := n } }")),
            mapOf(DomainReference("s") to shapes, DomainReference("d") to drawing),
            mapOf(shapes.name to ogS, drawing.name to ogD),
            mapOf(DomainReference("s") to items), emptyMap(), DomainReference("d")
        )
        assertEquals(listOf(disc("c"), disc("d")), result.targets.map { it.untyped.asString().trim() })
    }
}
