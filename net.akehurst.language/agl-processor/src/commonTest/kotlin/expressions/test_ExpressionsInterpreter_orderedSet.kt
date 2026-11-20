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
import net.akehurst.kotlinx.collections.OrderedSet
import net.akehurst.kotlinx.collections.orderedSetOf
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
 * reading a property of a Kotlin object whose value is an OrderedSet (ObjectGraphAccessorMutatorByReflection)
 */
class test_ExpressionsInterpreter_orderedSet {

    companion object {
        data class Item(val name: String)
        data class Owner(val items: OrderedSet<Item>)

        val domain = typesDomain("test", true) {
            namespace("ns") {
                data("Item", Item::class) {
                    propertyOfWithBinding<Item, String>(setOf(VAL, CMP, STR), "name", "String", false, accessor = Item::name)
                }
                data("Owner", Owner::class) {
                    propertyOfWithBinding<Owner, OrderedSet<Item>>(setOf(VAL, CMP, STR), "items", "OrderedSet", false, accessor = Owner::items) { typeArgument("Item") }
                }
            }
        }

        val i2 = Item("i2")
        val i1 = Item("i1")
        val i3 = Item("i3")
        val owner = Owner(orderedSetOf(i2, i1, i3)) // not sorted, so the order is checked

        fun accessor(issues: IssueHolder): ObjectGraphAccessorMutator = ObjectGraphAccessorMutatorByReflection(domain, issues, LocationMapDefault())

        fun selfOf(og: ObjectGraphAccessorMutator): TypedObject = og.typedAs(owner, og.typesDomain.findByQualifiedNameOrNull("ns.Owner".asQualifiedName)!!.type())

        fun check(og: ObjectGraphAccessorMutator, interpreter: ExpressionsInterpreterOverTypedObjectCombined, expression: String, expected: Any, actual: TypedObject) {
            assertTrue(interpreter.issues.errors.isEmpty(), "for '$expression': ${interpreter.issues}")
            val value = og.untyped(actual)
            assertEquals(expected, value, "for '$expression'")
            if (expected is OrderedSet<*>) {
                assertTrue(value is OrderedSet<*>, "for '$expression': expected an OrderedSet, was ${value::class}")
                assertEquals(expected.toList(), value.toList(), "order for '$expression'")
            }
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
            "items" to orderedSetOf(i2, i1, i3),
            $$"$self.items" to orderedSetOf(i2, i1, i3),
            $$"$self.$type.findPropertyByName('items').getValue($self)" to orderedSetOf(i2, i1, i3),
            $$"$self.$type.property.filter({ p -> p.type.isCollection }).map({ p -> p.getValue($self) })" to listOf(orderedSetOf(i2, i1, i3)),
            $$"items.$type.name" to "OrderedSet",
            $$"$self.$type.findPropertyByName('items').getValue($self).$type.name" to "OrderedSet",
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
