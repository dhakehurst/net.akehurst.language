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
import net.akehurst.language.asm.builder.asmSimple
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.objectgraph.api.EvaluationContext
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.base.api.asPossiblyQualifiedName
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Lambda parameters, infix operator precedence, comparison operators and String.length,
 * for the sync and suspending interpreters.
 */
class test_Expressions_docsBugs {

    private val tm = typesDomain("test", true) {
        namespace("ns") {
            data("Test") {
                propertyPrimitiveType("num", "Integer", false, 0)
                propertyListTypeOf("aList", "A", false, 1)
            }
            data("A") {
                propertyPrimitiveType("prop1", "String", false, 0)
            }
        }
    }
    private val self = asmSimple(typesDomain = tm) {
        element("Test") {
            propertyInteger("num", 3L)
            propertyListOfElement("aList") {
                element("A") { propertyString("prop1", "a1") }
                element("A") { propertyString("prop1", "a2") }
            }
        }
    }.root[0]

    private fun eval(expression: String): Any? {
        val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)
        val interpreter = ExpressionsInterpreterOverTypedObject(objectGraphSimpleAsm(tm, null, issues, LocationMapDefault()))
        val st = tm.findFirstDefinitionByPossiblyQualifiedNameOrNull("ns.Test".asPossiblyQualifiedName)!!.type()
        val actual = interpreter.evaluateStr(EvaluationContext.ofSelf(interpreter.objectGraph.typedAs(self, st)), expression)
        assertEquals(emptyList(), issues.all.toList(), expression)
        return actual.untyped
    }

    private suspend fun evalCombined(expression: String, suspending: Boolean): Any? {
        val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)
        val interpreter = ExpressionsInterpreterOverTypedObjectCombined(objectGraphSimpleAsm(tm, null, issues, LocationMapDefault()))
        val st = tm.findFirstDefinitionByPossiblyQualifiedNameOrNull("ns.Test".asPossiblyQualifiedName)!!.type()
        val evc = EvaluationContext.ofSelf(interpreter.objectGraph.typedAs(self, st))
        val actual = when {
            suspending -> interpreter.evaluateStrSuspend(evc, expression)
            else -> interpreter.evaluateStr(evc, expression)
        }
        assertEquals(emptyList(), issues.all.toList(), expression)
        return actual.untyped
    }

    private val cases = listOf(
        // lambda parameter
        "aList.map({ p -> p.prop1 })" to listOf("a1", "a2"),
        "aList.map({ it -> it.prop1 })" to listOf("a1", "a2"),
        // precedence and associativity
        "1 + 2 * 3" to 7L,
        "2 * 3 + 1" to 7L,
        "10 - 2 - 3" to 5L,
        "12 / 2 / 3" to 2L,
        "1 + 2 == 3" to true,
        "num > 2 and true" to true,
        "true or false and false" to true,
        "false and true or true" to true,
        "(1 + 2) * 3" to 9L,
        // comparison
        "1 < 2" to true,
        "2 <= 2" to true,
        "1 > 2" to false,
        "num >= 3" to true,
        "1 < 2.5" to true,
        "1 < 2 and 3 < 4" to true,
        // ternary with a comparison condition
        "num > 2 ? 'many' : 'few'" to "many",
        "(num > 5) ? 'many' : 'few'" to "few",
        // String.length
        "'abc'.length" to 3L,
        "aList[0].prop1.length" to 2L,
    )

    @Test
    fun sync() {
        for ((e, expected) in cases) assertEquals(expected, eval(e), e)
    }

    @Test
    fun combined_sync() = runTest {
        for ((e, expected) in cases) assertEquals(expected, evalCombined(e, false), e)
    }

    @Test
    fun combined_suspend() = runTest {
        for ((e, expected) in cases) assertEquals(expected, evalCombined(e, true), e)
    }

    @Test
    fun unknown_property_on_String_is_nothing() {
        val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)
        val interpreter = ExpressionsInterpreterOverTypedObject(objectGraphSimpleAsm(tm, null, issues, LocationMapDefault()))
        val actual = interpreter.evaluateStr(EvaluationContext.ofSelf(interpreter.objectGraph.typedAs(self, StdLibDefault.AnyType)), "'abc'.foo")
        assertEquals(Unit, actual.untyped)
        assertEquals(1, issues.warnings.size, issues.toString())
    }
}
