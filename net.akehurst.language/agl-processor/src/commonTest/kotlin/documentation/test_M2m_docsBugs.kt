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
 * m2m behaviour found while writing the docs (D20, D23), as QVT Relations.
 */
class test_M2m_docsBugs {

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

        //TODO: D21 - the doc uses the names 'Shapes' and 'Drawing', remove this once the types domains have those names
        fun header(rules: String) =
            "namespace example\ntransform ShapesToDrawing(s:${shapes.name.value}, d:${drawing.name.value}) {\n$rules\n}"

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

        fun transformResult(rules: String, input: List<TypedObject> = items) = Agl.transform(
            M2mTransformString(header(rules)),
            typeDomains = mapOf(DomainReference("s") to shapes, DomainReference("d") to drawing),
            accessorMutators = mapOf(shapes.name to ogS, drawing.name to ogD),
            domains = mapOf(DomainReference("s") to input),
            ruleArguments = emptyMap(),
            targetDomainReference = DomainReference("d")
        )

        fun input(sentence: String, withUnit: Boolean = false): List<TypedObject> {
            val u = shapesProcessor.process(sentence).asm!!.root.first() as AsmStructure
            @Suppress("UNCHECKED_CAST")
            val its = (u.getProperty(PropertyValueName("item")) as List<Any>).map { ogS.toTypedObject(it, StdLibDefault.AnyType) }
            return when {
                withUnit -> its + ogS.toTypedObject(u, StdLibDefault.AnyType)
                else -> its
            }
        }

        const val CIRCLE_TO_DISC = """
            top mapping CircleToDisc {
              domain s c:Circle { name == n }
              domain d disc:Disc := Disc() { label := n }
            }
        """

        fun disc(label: String, size: String? = null) = when (size) {
            null -> ":Disc {\n  label = '$label'\n}"
            else -> ":Disc {\n  label = '$label'\n  size = '$size'\n}"
        }
    }

    @Test
    fun D21_types_domain_names() {
        // the generated types domain is named after the grammar, a types string domain gets the given name
        assertEquals("Shapes", shapes.name.value)
        assertEquals("Drawing", drawing.name.value)
    }

    @Test
    fun D20_nested_template_does_not_match_missing_property() {
        val actual = transform(
            """
            top mapping CircleToDisc {
              domain s c:Circle { name == n  radius == Radius { number == r } }
              domain d disc:Disc := Disc() { label := n  size := r }
            }
            """, input = input("circle c r 5  circle e")
        )
        assertEquals(listOf(disc("c", "5")), actual)
    }

    @Test
    fun D23_when_mapped_binds_target() {
        val actual = transform(
            CIRCLE_TO_DISC + """
            top mapping CircleToCopy {
              domain s c:Circle { name == n }
              domain d disc:Disc := Disc() { label := 'copy'  size := x.label }
              when { mapped CircleToDisc { s := c  d := x } }
            }
            """, input = input("circle c r 5  circle d r 50")
        )
        assertEquals(listOf(disc("c"), disc("d"), disc("copy", "c"), disc("copy", "d")), actual)
    }

    @Test
    fun D23_when_related_binds_target() {
        val actual = transform(
            """
            top relation CircleDisc {
              domain s c:Circle { name == n }
              domain d disc:Disc { label == n }
            }
            top mapping CircleToCopy {
              domain s c:Circle { name == n }
              domain d disc:Disc := Disc() { label := 'copy'  size := x.label }
              when { related CircleDisc { s := c  d := x } }
            }
            """, input = input("circle c r 5  circle d r 50")
        )
        assertEquals(listOf(disc("c"), disc("d"), disc("copy", "c"), disc("copy", "d")), actual)
    }

    @Test
    fun D23_when_not_applied_does_not_hold() {
        val actual = transform(
            """
            top mapping CircleToCopy {
              domain s c:Circle { name == n }
              domain d disc:Disc := Disc() { label := 'copy'  size := x.label }
              when { mapped CircleToDisc { s := c  d := x } }
            }
            mapping CircleToDisc {
              domain s c:Circle { name == n }
              domain d disc:Disc := Disc() { label := n }
            }
            """, input = input("circle c r 5")
        )
        assertEquals(emptyList(), actual)
    }

    @Test
    fun D23_when_missing_target_argument_is_an_error() {
        val result = transformResult(
            CIRCLE_TO_DISC + """
            top mapping CircleToCopy {
              domain s c:Circle { name == n }
              domain d disc:Disc := Disc() { label := 'copy' }
              when { mapped CircleToDisc { s := c } }
            }
            """, input = input("circle c r 5")
        )
        assertTrue(result.issues.errors.any { it.message.startsWith("Argument for target domain 'd' not found in rule call 'CircleToDisc' in 'when' clause") }, result.issues.toString())
    }

    @Test
    fun D23_when_mapped_all_binds_list() {
        val actual = transform(
            CIRCLE_TO_DISC + """
            top mapping UnitToDrawing {
              domain s u:Unit { item == its }
              domain d dr:Drawing := Drawing() { discs := ds }
              when { mapped all CircleToDisc { s := its  d := ds } }
            }
            """, input = input("circle c r 5  circle d r 50", withUnit = true)
        )
        assertEquals(3, actual.size, actual.toString())
        assertTrue(actual[2].startsWith(":Drawing"), actual.toString())
        assertTrue(actual[2].contains("label = 'c'") && actual[2].contains("label = 'd'"), actual.toString())
    }

    @Test
    fun D23_when_mapped_all_does_not_hold_if_one_element_not_mapped() {
        val actual = transform(
            CIRCLE_TO_DISC + """
            top mapping UnitToDrawing {
              domain s u:Unit { item == its }
              domain d dr:Drawing := Drawing() { discs := ds }
              when { mapped all CircleToDisc { s := its  d := ds } }
            }
            """, input = input("circle c r 5  square s { 1 2 }", withUnit = true)
        )
        assertEquals(listOf(disc("c")), actual)
    }

    @Test
    fun D23_where_map_all_warns_only_if_nothing_matched() {
        val rules = """
            top mapping UnitToDrawing {
              domain s u:Unit { item == its }
              domain d dr:Drawing := Drawing() { discs := ds }
              where { map all CircleToDisc { s := its  d := ds } }
            }
            mapping CircleToDisc {
              domain s c:Circle { name == n }
              domain d disc:Disc := Disc() { label := n }
            }
        """
        val some = transformResult(rules, input("circle c r 5  square s { 1 2 }", withUnit = true))
        assertTrue(some.issues.warnings.none { it.message.contains("matched nothing") }, some.issues.toString())
        val none = transformResult(rules, input("square s { 1 2 }", withUnit = true))
        assertTrue(none.issues.warnings.any { it.message.contains("matched nothing") }, none.issues.toString())
    }
}
