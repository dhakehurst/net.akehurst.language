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
import net.akehurst.language.base.api.asPossiblyQualifiedName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.m2mTransform.processor.M2mTransformInterpreterCombined
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * transform extends (David 2026-11-20, MT-Q25): a transform has the rules of the transforms it extends;
 * a rule marked 'override' replaces the inherited rule with the same name, also where inherited rules call it.
 */
class test_m2mTransform_transformExtends {

    private companion object {
        val DRAWING_TYPES = """
            namespace drawing
              import std
              data Drawing { cmp discs: List<Disc> }
              data Disc { constructor(val label: String)  size: String? }
        """.trimIndent()

        val shapes by lazy { shapesProcessor.typesDomain }
        val drawing by lazy { TypesDomainSimple.fromString(SimpleName("Drawing"), SentenceContextAny(), TypesString(DRAWING_TYPES)).asm!! }

        const val SENTENCE = """
            circle c r 5
            square s { 1 2 }
        """

        // Base: every item is mapped through 'Label', from a top rule
        const val BASE = """
            transform Base(s:Shapes, d:Drawing) {
                top mapping ItemToDisc {
                    domain s o:Any {}
                    domain d t:Disc := Disc(label := l) {}
                    where { map Label { s := o  d := l } }
                }
                mapping Label {
                    domain s o:Any {}
                    domain d l:String := 'item'
                }
            }
        """

        fun process(text: String) = SentenceContextAny().let { context ->
            context.addToScope(null, listOf(shapes.name.value), QualifiedName("TypesDomain"), null, shapes)
            context.addToScope(null, listOf(drawing.name.value), QualifiedName("TypesDomain"), null, drawing)
            Agl.registry.agl.m2mTransform.processor!!.process("namespace example\n" + text, options = Agl.options { semanticAnalysis { sentenceContext(context) } })
        }

        /** runs transform 'name' of 'text' over the shape items */
        fun run(text: String, name: String): Pair<net.akehurst.language.m2mTransform.processor.M2MTransformResult, List<String>> {
            val res = process(text)
            assertTrue(res.allIssues.errors.isEmpty(), res.allIssues.toString())
            val m2m = res.asm!!
            val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)
            val ogS = objectGraphSimpleAsm(shapes, null, issues, LocationMapDefault())
            val ogD = objectGraphSimpleAsm(drawing, null, issues, LocationMapDefault())
            @Suppress("UNCHECKED_CAST")
            val items = ((shapesProcessor.process(SENTENCE).asm!!.root.first() as AsmStructure).getProperty(PropertyValueName("item")) as List<Any>)
                .map { ogS.toTypedObject(it, StdLibDefault.AnyType) }
            val interpreter = M2mTransformInterpreterCombined(m2m, mapOf(shapes.name to ogS, drawing.name to ogD), issues)
            val tr = m2m.allTransformRuleSet.first { it.name.value == name }
            val result = interpreter.transform(tr, DomainReference("d"), emptyMap(), mapOf(DomainReference("s") to items))
            val labels = result.targets.map { (it.self as AsmStructure).getProperty(PropertyValueName("label")).asString().trim() }
            return Pair(result, labels)
        }
    }

    @Test
    fun base_alone() {
        val (res, labels) = run(BASE, "Base")
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        assertEquals(listOf("'item'", "'item'"), labels)
    }

    @Test
    fun inherited_rules_apply() {
        val (res, labels) = run(BASE + "transform Derived(s:Shapes, d:Drawing) : Base { }", "Derived")
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        assertEquals(listOf("'item'", "'item'"), labels)
    }

    @Test
    fun override_replaces_inherited_rule_also_where_inherited_rules_call_it() {
        val text = BASE + """
            transform Derived(s:Shapes, d:Drawing) : Base {
                override mapping Label {
                    domain s o:Any {}
                    domain d l:String := 'derived'
                }
            }
        """
        val (res, labels) = run(text, "Derived")
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        assertEquals(listOf("'derived'", "'derived'"), labels)
        // Base itself is unchanged
        assertEquals(listOf("'item'", "'item'"), run(text, "Base").second)
    }

    @Test
    fun new_rule_extends_inherited_rule() {
        // the generic + specialised case across transforms: Derived adds a rule that extends the inherited 'Label'
        val text = BASE + """
            transform Derived(s:Shapes, d:Drawing) : Base {
                mapping CircleLabel : Label {
                    domain s o:Circle { name == n }
                    domain d l:String := 'circle ' + n
                }
            }
        """
        val (res, labels) = run(text, "Derived")
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        assertEquals(listOf("'circle c'", "'item'"), labels)
    }

    @Test
    fun same_name_without_override_is_error() {
        val res = process(BASE + """
            transform Derived(s:Shapes, d:Drawing) : Base {
                mapping Label {
                    domain s o:Any {}
                    domain d l:String := 'derived'
                }
            }
        """)
        assertTrue(res.allIssues.errors.any { it.message == "Rule 'Label' in transform 'Derived' replaces an inherited rule with the same name, it must be marked 'override'." }, res.allIssues.toString())
    }

    @Test
    fun override_without_inherited_rule_is_error() {
        val res = process(BASE + """
            transform Derived(s:Shapes, d:Drawing) : Base {
                override mapping Other {
                    domain s o:Any {}
                    domain d l:String := 'derived'
                }
            }
        """)
        assertTrue(res.allIssues.errors.any { it.message == "Rule 'Other' in transform 'Derived' is marked 'override', but no rule named 'Other' is inherited." }, res.allIssues.toString())
    }

    @Test
    fun unknown_extended_transform_is_error() {
        val res = process("transform Derived(s:Shapes, d:Drawing) : Nope { }")
        assertTrue(res.allIssues.errors.any { it.message == "Transform 'Nope' extended by transform 'Derived' not found." }, res.allIssues.toString())
    }

    @Test
    fun different_domains_is_error() {
        val res = process(BASE + "transform Derived(a:Shapes, d:Drawing) : Base { }")
        assertTrue(res.allIssues.errors.any { it.message.startsWith("Transform 'Derived' extends 'Base', but their domains differ") }, res.allIssues.toString())
    }

    @Test
    fun extends_cycle_is_error() {
        val res = process("transform A(s:Shapes, d:Drawing) : B { }\ntransform B(s:Shapes, d:Drawing) : A { }")
        assertTrue(res.allIssues.errors.any { it.message == "Transform 'A' extends itself (directly or through other transforms)." }, res.allIssues.toString())
    }

    private fun aglTransform(text: String, name: String?) = run {
        val ogS = objectGraphSimpleAsm(shapes, null, IssueHolder(LanguageProcessorPhase.INTERPRET), LocationMapDefault())
        val ogD = objectGraphSimpleAsm(drawing, null, IssueHolder(LanguageProcessorPhase.INTERPRET), LocationMapDefault())
        @Suppress("UNCHECKED_CAST")
        val items = ((shapesProcessor.process(SENTENCE).asm!!.root.first() as AsmStructure).getProperty(PropertyValueName("item")) as List<Any>)
            .map { ogS.toTypedObject(it, StdLibDefault.AnyType) }
        Agl.transform(
            M2mTransformString("namespace example\n" + text),
            typeDomains = mapOf(DomainReference("s") to shapes, DomainReference("d") to drawing),
            accessorMutators = mapOf(shapes.name to ogS, drawing.name to ogD),
            domains = mapOf(DomainReference("s") to items),
            ruleArguments = emptyMap(),
            targetDomainReference = DomainReference("d"),
            transformName = name?.asPossiblyQualifiedName
        )
    }

    @Test
    fun agl_transform_by_name() {
        val text = BASE + """
            transform Derived(s:Shapes, d:Drawing) : Base {
                override mapping Label {
                    domain s o:Any {}
                    domain d l:String := 'derived'
                }
            }
        """
        val labels = { r: net.akehurst.language.m2mTransform.processor.M2MTransformResult -> r.targets.map { (it.self as AsmStructure).getProperty(PropertyValueName("label")).asString().trim() } }
        assertEquals(listOf("'item'", "'item'"), labels(aglTransform(text, null)))
        assertEquals(listOf("'derived'", "'derived'"), labels(aglTransform(text, "Derived")))
        assertEquals(listOf("'derived'", "'derived'"), labels(aglTransform(text, "example.Derived")))
        val notFound = aglTransform(text, "Nope")
        assertTrue(notFound.issues.errors.any { it.message == "Transform 'Nope' not found." }, notFound.issues.toString())
    }
}
