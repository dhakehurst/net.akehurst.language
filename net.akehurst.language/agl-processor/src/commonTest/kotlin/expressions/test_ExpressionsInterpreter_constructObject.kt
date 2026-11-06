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

package net.akehurst.language.expressions.processor

import kotlinx.coroutines.test.runTest
import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.objectgraph.api.EvaluationContext
import net.akehurst.language.objectgraph.api.TypedObject
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Constructor arguments are matched to the declared constructor by name, otherwise by position.
 * Runs the old sync interpreter and both modes of the combined one.
 */
class test_ExpressionsInterpreter_constructObject {

    private companion object {
        data class Node(val label: String, val key: String, val labels: List<Any>)

        val tm = typesDomain("test", true) {
            namespace("ns") {
                data("Node") {
                    constructor_ {
                        parameter(setOf(), "label", "String")
                        parameter(setOf(), "key", "String")
                        parameter(setOf(), "labels", "List") // no type arguments
                        execution { args -> Node(args[0] as String, args[1] as String, args[2] as List<Any>) }
                    }
                }
            }
        }
    }

    private enum class Mode { OLD, SYNC, SUSPEND }

    private suspend fun eval(mode: Mode, expression: String): Any {
        val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)
        val og = ObjectGraphAccessorMutatorByReflection(tm, issues, LocationMapDefault())
        val evc = EvaluationContext.of(emptyMap())
        val actual = when (mode) {
            Mode.OLD -> ExpressionsInterpreterOverTypedObject(og).evaluateStr(evc, expression)
            Mode.SYNC -> ExpressionsInterpreterOverTypedObjectCombined(og).evaluateStr(evc, expression)
            Mode.SUSPEND -> ExpressionsInterpreterOverTypedObjectCombined(og).evaluateStrSuspend(evc, expression)
        }
        assertTrue(issues.errors.isEmpty(), issues.toString())
        val node = og.untyped(actual) as Node
        // TODO: elements of created collections are TypedObjects (E8, postponed), so unwrap them here
        return node.copy(labels = node.labels.map { (it as? TypedObject)?.self ?: it })
    }

    private val expected = Node("l", "k", listOf("a"))

    private suspend fun byName_sameTypesReordered(mode: Mode) =
        assertEquals(expected, eval(mode, "Node(key := 'k', label := 'l', labels := List('a')) {}"))

    private suspend fun byName_differentTypesReordered(mode: Mode) =
        assertEquals(expected, eval(mode, "Node(labels := List('a'), key := 'k', label := 'l') {}"))

    private suspend fun byPosition_namesDoNotMatch(mode: Mode) =
        assertEquals(expected, eval(mode, "Node(x := 'l', y := 'k', z := List('a')) {}"))

    private suspend fun noArguments_noZeroParameterConstructor(mode: Mode) {
        val e = assertFailsWith<IllegalStateException> { eval(mode, "Node() { label := 'l' }") }
        assertTrue(e.message!!.contains("has no declared constructor with 0 parameters"), e.message)
    }

    @Test
    fun byName_sameTypesReordered_old() = runTest { byName_sameTypesReordered(Mode.OLD) }

    @Test
    fun byName_sameTypesReordered_combined() = runTest { byName_sameTypesReordered(Mode.SYNC) }

    @Test
    fun byName_sameTypesReordered_combinedSuspend() = runTest { byName_sameTypesReordered(Mode.SUSPEND) }

    @Test
    fun byName_differentTypesReordered_old() = runTest { byName_differentTypesReordered(Mode.OLD) }

    @Test
    fun byName_differentTypesReordered_combined() = runTest { byName_differentTypesReordered(Mode.SYNC) }

    @Test
    fun byName_differentTypesReordered_combinedSuspend() = runTest { byName_differentTypesReordered(Mode.SUSPEND) }

    @Test
    fun byPosition_namesDoNotMatch_old() = runTest { byPosition_namesDoNotMatch(Mode.OLD) }

    @Test
    fun byPosition_namesDoNotMatch_combined() = runTest { byPosition_namesDoNotMatch(Mode.SYNC) }

    @Test
    fun byPosition_namesDoNotMatch_combinedSuspend() = runTest { byPosition_namesDoNotMatch(Mode.SUSPEND) }

    @Test
    fun noArguments_noZeroParameterConstructor_old() = runTest { noArguments_noZeroParameterConstructor(Mode.OLD) }

    @Test
    fun noArguments_noZeroParameterConstructor_combined() = runTest { noArguments_noZeroParameterConstructor(Mode.SYNC) }

    @Test
    fun noArguments_noZeroParameterConstructor_combinedSuspend() = runTest { noArguments_noZeroParameterConstructor(Mode.SUSPEND) }
}
