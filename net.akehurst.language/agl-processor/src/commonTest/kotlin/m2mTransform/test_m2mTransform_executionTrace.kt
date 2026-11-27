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
import net.akehurst.language.expressions.processor.ExecutionTraceTree
import net.akehurst.language.objectgraph.api.ExecutionTraceListener
import net.akehurst.language.objectgraph.api.TraceStepKind
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.asm.TypesDomainSimple
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * The execution trace of a model-to-model transform: steps for the transform, each rule, 'when' and 'where',
 * with the expression steps of the expression interpreter nested inside them.
 */
class test_m2mTransform_executionTrace {

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

        fun transform(sentence: String, text: String, listener: ExecutionTraceListener?) = objectGraphs().let { (ogS, ogD) ->
            Agl.transform(
                M2mTransformString(text),
                typeDomains = mapOf(DomainReference("s") to shapes, DomainReference("d") to drawing),
                accessorMutators = mapOf(shapes.name to ogS, drawing.name to ogD),
                domains = mapOf(DomainReference("s") to items(sentence, ogS)),
                ruleArguments = emptyMap(),
                targetDomainReference = DomainReference("d"),
                traceListener = listener
            )
        }

        suspend fun transformSuspend(sentence: String, text: String, listener: ExecutionTraceListener?) = objectGraphs().let { (ogS, ogD) ->
            Agl.transformSuspend(
                M2mTransformString(text),
                typeDomains = mapOf(DomainReference("s") to shapes, DomainReference("d") to drawing),
                accessorMutators = mapOf(shapes.name to ogS, drawing.name to ogD),
                domains = mapOf(DomainReference("s") to items(sentence, ogS)),
                ruleArguments = emptyMap(),
                targetDomainReference = DomainReference("d"),
                traceListener = listener
            )
        }

        const val SENTENCE = "square s { 1 2 }"
        const val RULE = "top mapping SqName { domain s sq:Square { name == n } domain d l:String := x where { map Up { s := n d := x } } }"

        /** kind and text of each recorded step, indented by nesting depth */
        fun shape(tree: ExecutionTraceTree): List<String> {
            val res = mutableListOf<String>()
            fun walk(n: ExecutionTraceTree.Node, depth: Int) {
                res.add("${"  ".repeat(depth)}${n.step.kind} ${n.step.text}")
                n.children.forEach { walk(it, depth + 1) }
            }
            tree.roots.forEach { walk(it, 0) }
            return res
        }
    }

    @Test
    fun rule_steps() {
        val tree = ExecutionTraceTree(kinds = setOf(TraceStepKind.TRANSFORM, TraceStepKind.RULE, TraceStepKind.WHEN, TraceStepKind.WHERE))
        val res = transform(SENTENCE, header(RULE), tree)
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        assertEquals(
            listOf(
                "TRANSFORM transform 'example.ShapesToDrawing' to domain 'd'",
                "  RULE mapping 'SqName'",
                "    WHERE where of 'SqName': call 'Up'",
                "      RULE table 'Up'",
            ),
            shape(tree)
        )
    }

    @Test
    fun expression_steps_nest_in_rule_steps() {
        val tree = ExecutionTraceTree()
        val res = transform(SENTENCE, header(RULE), tree)
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        val transformStep = tree.roots.single()
        assertEquals(TraceStepKind.TRANSFORM, transformStep.step.kind)
        val ruleStep = transformStep.children.single()
        assertEquals(TraceStepKind.RULE, ruleStep.step.kind)
        // the table values are expressions, evaluated inside the table rule step
        val all = mutableListOf<ExecutionTraceTree.Node>()
        fun walk(n: ExecutionTraceTree.Node) {
            all.add(n)
            n.children.forEach { walk(it) }
        }
        walk(transformStep)
        val tableStep = all.first { it.step.kind == TraceStepKind.RULE && it.step.text == "table 'Up'" }
        assertTrue(tableStep.children.isNotEmpty() && tableStep.children.all { it.step.kind == TraceStepKind.EXPRESSION }, tree.asString())
        assertTrue(all.all { it.finished }, tree.asString())
    }

    @Test
    fun false_when_is_a_note() {
        val rule = "top mapping SqName { domain s sq:Square { name == n } domain d l:String := n when { false } }"
        val tree = ExecutionTraceTree(kinds = setOf(TraceStepKind.TRANSFORM, TraceStepKind.RULE, TraceStepKind.WHEN))
        val res = transform(SENTENCE, header(rule), tree)
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        val ruleStep = tree.roots.single().children.single()
        val whenStep = ruleStep.children.single()
        assertEquals(TraceStepKind.WHEN, whenStep.step.kind)
        assertEquals(listOf("when does not hold"), whenStep.notes)
        assertTrue(ruleStep.notes.any { it.startsWith("when clause evaluated to false") }, tree.asString())
    }

    @Test
    fun suspend_same_trace_as_sync() = runTest {
        val treeSync = ExecutionTraceTree()
        transform(SENTENCE, header(RULE), treeSync)
        val treeSusp = ExecutionTraceTree()
        transformSuspend(SENTENCE, header(RULE), treeSusp)
        assertEquals(shape(treeSync), shape(treeSusp))
    }

    @Test
    fun no_listener_same_result() {
        val r1 = transform(SENTENCE, header(RULE), null)
        val r2 = transform(SENTENCE, header(RULE), ExecutionTraceTree())
        assertEquals(r1.targets.map { it.untyped.asString().trim() }, r2.targets.map { it.untyped.asString().trim() })
    }
}
