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

package net.akehurst.language.expressions.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.simple.contextAsmSimple
import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.api.processor.CrossReferenceString
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.asm.api.AsmReference
import net.akehurst.language.asm.api.AsmStructure
import net.akehurst.language.asm.api.PropertyValueName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.objectgraph.api.EvaluationContext
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * reading a reference property as a value gives the referred element when the reference is resolved,
 * '&' ('&name' or '.&name') gives the reference itself
 */
class test_ExpressionsInterpreter_asmReference {

    private companion object {
        val grammarStr = GrammarString(
            """
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                unit = item* ;
                item = requirement | link ;
                requirement = 'req' id ;
                link = 'link' kind target ;
                kind = 'satisfies' ;
                leaf id = ID ;
                leaf target = ID ;
                leaf ID = "[A-Z0-9-]+" ;
            }
            """.trimIndent()
        )
        val referencesStr = CrossReferenceString(
            """
            namespace test.Test
                identify Requirement by id
                references {
                    in Link {
                        property target refers-to Requirement
                    }
                }
            """.trimIndent()
        )

        val processor = Agl.processorFromStringSimple(grammarStr, referenceStr = referencesStr).let {
            assertTrue(it.issues.errors.isEmpty(), it.issues.toString())
            it.processor!!
        }

        @Suppress("UNCHECKED_CAST")
        fun items(root: AsmStructure) = (root.getProperty(PropertyValueName("item")) as List<Any>).map { (it as? AsmStructure) ?: error("not a structure: $it") }

        fun eval(self: AsmStructure, expression: String) = let {
            val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)
            val interpreter = ExpressionsInterpreterOverTypedObjectCombined(objectGraphSimpleAsm(processor.typesDomain, null, issues, LocationMapDefault()))
            val type = interpreter.objectGraph.typeFor(self, net.akehurst.language.types.asm.StdLibDefault.AnyType)
            val actual = interpreter.evaluateStr(EvaluationContext.ofSelf(interpreter.objectGraph.typedAs(self, type)), expression)
            Pair(actual, issues)
        }
    }

    @Test
    fun resolved_reference__gives_referred_element() {
        val res = processor.process("req SYS-6 link satisfies SYS-6", Agl.options { semanticAnalysis { sentenceContext(contextAsmSimple()) } })
        assertTrue(res.allIssues.errors.isEmpty(), res.allIssues.toString())

        val (req, link) = items(res.asm!!.root.first() as AsmStructure)
        assertTrue(link.getProperty(PropertyValueName("target")) is AsmReference)

        val (target, issues) = eval(link, "target")
        assertTrue(issues.errors.isEmpty(), issues.toString())
        assertSame(req, target.self)

        val (targetId, issues2) = eval(link, "target.id")
        assertTrue(issues2.errors.isEmpty(), issues2.toString())
        assertEquals("SYS-6", targetId.self)
    }

    @Test
    fun unresolved_reference__is_nothing() {
        val res = processor.process("link satisfies SYS-7", Agl.options { semanticAnalysis { sentenceContext(contextAsmSimple()) } })
        val link = items(res.asm!!.root.first() as AsmStructure).first()
        val (target, issues) = eval(link, "target")
        assertTrue(target.isNothing, target.toString())
        assertTrue(issues.all.isEmpty(), issues.toString())
        // with '&' it is the (unresolved) reference
        val (ref, _) = eval(link, "&target")
        assertTrue(ref.self is AsmReference, ref.toString())
        assertEquals(null, (ref.self as AsmReference).value)
    }

    @Test
    fun ampersand__gives_the_reference() {
        val res = processor.process("req SYS-6 link satisfies SYS-6", Agl.options { semanticAnalysis { sentenceContext(contextAsmSimple()) } })
        assertTrue(res.allIssues.errors.isEmpty(), res.allIssues.toString())
        val (req, link) = items(res.asm!!.root.first() as AsmStructure)

        val (rootRef, issues1) = eval(link, "&target")
        assertTrue(issues1.errors.isEmpty(), issues1.toString())
        assertTrue(rootRef.self is AsmReference, rootRef.toString())
        assertSame(req, (rootRef.self as AsmReference).value)

        val (navRef, issues2) = eval(link, "\$self.&target")
        assertTrue(issues2.errors.isEmpty(), issues2.toString())
        assertTrue(navRef.self is AsmReference, navRef.toString())

        // navigating on from the reference still reaches the referred element's properties
        val (id, issues3) = eval(link, "\$self.&target.id")
        assertTrue(issues3.errors.isEmpty(), issues3.toString())
        assertEquals("SYS-6", id.self)
    }

    @Test
    fun ampersand__parse_and_asString() {
        val proc = Agl.registry.agl.expressions.processor!!
        for (text in listOf("&a", "a.&b", "a.&b.c", "\$self.&b")) {
            val pr = proc.parse(text, Agl.parseOptions { goalRuleName("expression") })
            assertTrue(pr.issues.errors.isEmpty(), "$text: ${pr.issues}")
            val sa = proc.syntaxAnalysis(pr.sppt!!)
            assertTrue(sa.issues.errors.isEmpty(), "$text: ${sa.issues}")
            assertEquals(text, (sa.asm as net.akehurst.language.expressions.api.Expression).asString(net.akehurst.kotlinx.utils.Indent(), emptyList()))
        }
    }

    @Test
    fun list_of_references__gives_referred_elements() {
        // a list property that holds references (e.g. created by a transform)
        val res = processor.process("req A req B link satisfies A", Agl.options { semanticAnalysis { sentenceContext(contextAsmSimple()) } })
        assertTrue(res.allIssues.errors.isEmpty(), res.allIssues.toString())
        val (a, b, link) = items(res.asm!!.root.first() as AsmStructure)
        val refs = net.akehurst.language.asm.simple.AsmListSimple(
            listOf(
                net.akehurst.language.asm.simple.AsmReferenceSimple("A", a),
                net.akehurst.language.asm.simple.AsmReferenceSimple("X", null),
                net.akehurst.language.asm.simple.AsmReferenceSimple("B", b)
            )
        )
        link.setProperty(PropertyValueName("targets"), refs, 3)

        // without '&': the referred elements, the unresolved 'X' is left out
        val (targets, issues) = eval(link, "targets")
        assertTrue(issues.errors.isEmpty(), issues.toString())
        val els = ((targets.self as? List<*>) ?: (targets.self as net.akehurst.language.asm.api.AsmList).elements)
            .map { (it as? net.akehurst.language.objectgraph.api.TypedObject)?.self ?: it }
        assertEquals(2, els.size, els.toString())
        assertSame(a, els[0])
        assertSame(b, els[1])

        val (firstId, issues2) = eval(link, "targets.first.id")
        assertTrue(issues2.errors.isEmpty(), issues2.toString())
        assertEquals("A", firstId.self)

        // with '&': the references
        val (refsRead, issues3) = eval(link, "&targets")
        assertTrue(issues3.errors.isEmpty(), issues3.toString())
        val rs = (refsRead.self as? List<*>) ?: (refsRead.self as net.akehurst.language.asm.api.AsmList).elements
        assertEquals(3, rs.size, rs.toString())
        assertTrue(rs.all { (it as? net.akehurst.language.objectgraph.api.TypedObject)?.self is AsmReference || it is AsmReference }, rs.toString())
    }
}
