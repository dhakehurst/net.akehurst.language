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

import kotlinx.coroutines.test.runTest
import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.base.api.asQualifiedName
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.objectgraph.api.EvaluationContext
import net.akehurst.language.objectgraph.api.ExecutionTraceListener
import net.akehurst.language.objectgraph.api.TraceStep
import net.akehurst.language.objectgraph.api.TraceStepKind
import net.akehurst.language.objectgraph.api.TypedObject
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The execution trace of the expression interpreter: one step per expression node (and per navigation part),
 * nested as the evaluation nests, given to the listener only when there is one.
 */
class test_ExecutionTrace {

    private companion object {
        data class TestObj(val name: String, val list: List<String>)

        val tm = typesDomain("test", true) {
            namespace("ns") {
                data("TestObj") {
                    propertyPrimitiveType("name", "std.String", false, 0)
                    propertyListTypeOf("list", "std.String", false, 1)
                }
            }
        }

        fun interpreter(listener: ExecutionTraceListener?): Pair<ExpressionsInterpreterOverTypedObjectCombined, EvaluationContext> {
            val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)
            val og = ObjectGraphAccessorMutatorByReflection(tm, issues, LocationMapDefault(), primitiveExecutor = StdLibPrimitiveExecutionsForReflection(issues))
            val st = tm.findByQualifiedNameOrNull("ns.TestObj".asQualifiedName)!!.type()
            val self = TestObj("abc", listOf("A", "B", "C"))
            return Pair(ExpressionsInterpreterOverTypedObjectCombined(og, traceListener = listener), EvaluationContext.ofSelf(og.typedAs(self, st)))
        }

        /** kind, text and nesting depth of each recorded step, in order */
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
    fun no_listener__same_result() {
        val (i1, evc1) = interpreter(null)
        val (i2, evc2) = interpreter(ExecutionTraceTree())
        val r1 = i1.objectGraph.untyped(i1.evaluateStr(evc1, "list.size + 1"))
        val r2 = i2.objectGraph.untyped(i2.evaluateStr(evc2, "list.size + 1"))
        assertEquals(4L, r1)
        assertEquals(r1, r2)
    }

    @Test
    fun navigation__step_per_part() {
        val tree = ExecutionTraceTree()
        val (interp, evc) = interpreter(tree)
        val res = interp.evaluateStr(evc, "list.size")
        assertEquals(3L, interp.objectGraph.untyped(res))
        assertEquals(
            listOf(
                "EXPRESSION list.size",
                "  EXPRESSION list",
                "  EXPRESSION .size",
            ),
            shape(tree).map { it.replace(Regex("EXPRESSION \\.?size$"), "EXPRESSION .size") }
        )
        // the root step finished with the result of the expression
        val root = tree.roots.single()
        assertTrue(root.finished)
        assertEquals(3L, interp.objectGraph.untyped(root.result!!))
        // a navigation part's 'self' is the value navigated from
        val sizePart = root.children[1]
        assertEquals(listOf("A", "B", "C"), interp.objectGraph.untyped(sizePart.step.self!!))
    }

    @Test
    fun infix__nested_steps_with_results() {
        val tree = ExecutionTraceTree()
        val (interp, evc) = interpreter(tree)
        interp.evaluateStr(evc, "list.size + 1")
        val root = tree.roots.single()
        assertEquals(TraceStepKind.EXPRESSION, root.step.kind)
        assertEquals("list.size + 1", root.step.text)
        assertEquals(listOf("list.size", "1"), root.children.map { it.step.text })
        assertEquals(listOf(3L, 1L), root.children.map { interp.objectGraph.untyped(it.result!!) })
        assertEquals(4L, interp.objectGraph.untyped(root.result!!))
    }

    @Test
    fun lambda_body__step_per_call_with_its_named_values() {
        val tree = ExecutionTraceTree()
        val (interp, evc) = interpreter(tree)
        val res = interp.evaluateStr(evc, "list.map({it -> it + 'x'})")
        assertEquals(listOf("Ax", "Bx", "Cx"), interp.objectGraph.untyped(res))
        // one step for the body of each call of the lambda, each sees its own 'it'
        val bodySteps = mutableListOf<ExecutionTraceTree.Node>()
        fun walk(n: ExecutionTraceTree.Node) {
            if (n.step.text == "it + 'x'") bodySteps.add(n)
            n.children.forEach { walk(it) }
        }
        tree.roots.forEach { walk(it) }
        assertEquals(3, bodySteps.size)
        assertEquals(listOf("A", "B", "C"), bodySteps.map { interp.objectGraph.untyped(it.step.namedValues["it"]!!) })
    }

    @Test
    fun suspend__same_trace_as_sync() = runTest {
        val treeSync = ExecutionTraceTree()
        val (i1, evc1) = interpreter(treeSync)
        i1.evaluateStr(evc1, "list.map({it -> it + name}).size + 1")
        val treeSusp = ExecutionTraceTree()
        val (i2, evc2) = interpreter(treeSusp)
        i2.evaluateStrSuspend(evc2, "list.map({it -> it + name}).size + 1")
        assertEquals(shape(treeSync), shape(treeSusp))
    }

    @Test
    fun filter_kinds__nothing_recorded() {
        val tree = ExecutionTraceTree(kinds = setOf(TraceStepKind.RULE))
        val (interp, evc) = interpreter(tree)
        interp.evaluateStr(evc, "list.size + 1")
        assertEquals(emptyList(), tree.roots)
    }

    @Test
    fun custom_listener__started_and_finished_balance() {
        val events = mutableListOf<String>()
        val listener = object : ExecutionTraceListener {
            override fun stepStarted(step: TraceStep) {
                events.add("start ${step.text}")
            }

            override fun stepFinished(step: TraceStep, result: TypedObject?) {
                events.add("end ${step.text}")
            }

            override fun note(step: TraceStep?, message: String) {
                events.add("note $message")
            }
        }
        val (interp, evc) = interpreter(listener)
        interp.evaluateStr(evc, "name")
        assertEquals(listOf("start name", "end name"), events)
    }
}
