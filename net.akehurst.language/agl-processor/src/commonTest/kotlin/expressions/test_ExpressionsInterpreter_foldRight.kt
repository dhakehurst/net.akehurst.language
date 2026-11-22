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
import net.akehurst.language.objectgraph.api.ObjectGraphAccessorMutator
import net.akehurst.language.objectgraph.api.TypedObject
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * List.foldRight(initial, { element, acc -> ... }), a lambda with two parameters
 */
class test_ExpressionsInterpreter_foldRight {

    companion object {
        data class Item(val name: String)
        data class Owner(val items: List<Item>, val none: List<Item>)

        val domain = typesDomain("test", true) {
            namespace("ns") {
                data("Item", Item::class) {
                    propertyOfWithBinding<Item, String>(setOf(VAL, CMP, STR), "name", "String", false, accessor = Item::name)
                }
                data("Owner", Owner::class) {
                    propertyOfWithBinding<Owner, List<Item>>(setOf(VAL, CMP, STR), "items", "List", false, accessor = Owner::items) { typeArgument("Item") }
                    propertyOfWithBinding<Owner, List<Item>>(setOf(VAL, CMP, STR), "none", "List", false, accessor = Owner::none) { typeArgument("Item") }
                }
            }
        }

        val owner = Owner(listOf(Item("a"), Item("b"), Item("c")), emptyList())

        fun accessor(issues: IssueHolder): ObjectGraphAccessorMutator = ObjectGraphAccessorMutatorByReflection(domain, issues, LocationMapDefault())

        fun selfOf(og: ObjectGraphAccessorMutator): TypedObject = og.typedAs(owner, og.typesDomain.findByQualifiedNameOrNull("ns.Owner".asQualifiedName)!!.type())

        fun check(og: ObjectGraphAccessorMutator, interpreter: ExpressionsInterpreterOverTypedObjectCombined, expression: String, expected: Any, actual: TypedObject) {
            assertTrue(interpreter.issues.errors.isEmpty(), "for '$expression': ${interpreter.issues}")
            assertEquals(expected, og.untyped(actual), "for '$expression'")
        }

        fun test(expression: String, expected: Any) {
            val og = accessor(IssueHolder(LanguageProcessorPhase.INTERPRET))
            val interpreter = ExpressionsInterpreterOverTypedObjectCombined(og)
            val actual = interpreter.evaluateStr(EvaluationContext.ofSelf(selfOf(og)), expression)
            check(og, interpreter, expression, expected, actual)
        }

        suspend fun testSuspend(expression: String, expected: Any) {
            val og = accessor(IssueHolder(LanguageProcessorPhase.INTERPRET))
            val interpreter = ExpressionsInterpreterOverTypedObjectCombined(og)
            val actual = interpreter.evaluateStrSuspend(EvaluationContext.ofSelf(selfOf(og)), expression)
            check(og, interpreter, expression, expected, actual)
        }

        val cases = listOf(
            // right-nested, the last element first
            "items.foldRight('x', { i, acc -> List(i.name, acc) })" to listOf("a", listOf("b", listOf("c", "x"))),
            // the last element as the initial value
            "items.front.foldRight(items.last.name, { i, acc -> List(i.name, acc) })" to listOf("a", listOf("b", "c")),
            // an empty list gives the initial value
            "none.foldRight('x', { i, acc -> List(i.name, acc) })" to "x",
            // one-parameter lambdas are unchanged
            "items.map({ i -> i.name })" to listOf("a", "b", "c"),
        )
    }

    @Test
    fun sync() {
        val failed = cases.mapNotNull { (expr, expected) -> runCatching { test(expr, expected) }.exceptionOrNull()?.let { "$expr: $it" } }
        assertTrue(failed.isEmpty(), failed.joinToString("\n"))
    }

    @Test
    fun suspend() = runTest {
        val failed = cases.mapNotNull { (expr, expected) ->
            try {
                testSuspend(expr, expected); null
            } catch (t: Throwable) {
                "$expr: $t"
            }
        }
        assertTrue(failed.isEmpty(), failed.joinToString("\n"))
    }
}
