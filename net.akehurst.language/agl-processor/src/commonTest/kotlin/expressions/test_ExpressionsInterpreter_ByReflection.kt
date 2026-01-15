/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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
import net.akehurst.language.agl.expressions.processor.ObjectGraphByReflection
import net.akehurst.language.agl.expressions.processor.StdLibPrimitiveExecutionsForReflection
import net.akehurst.language.agl.expressions.processor.TypedObjectAny
import net.akehurst.language.api.processor.EvaluationContext
import net.akehurst.language.asm.api.AsmValue
import net.akehurst.language.asm.builder.asmSimple
import net.akehurst.language.asm.simple.AsmListSimple
import net.akehurst.language.asm.simple.AsmNothingSimple
import net.akehurst.language.asm.simple.AsmPrimitiveSimple
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.api.asQualifiedName
import net.akehurst.language.base.processor.AglBase
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class test_ExpressionsInterpreter_ByReflection {

    companion object Companion {
        data class TestObj(
            val prop1: String,
            val propList: List<String> = emptyList(),
            val propListA: List<TestObjA> = emptyList()
        )

        data class TestObjA(
            val prop1: String
        )

        val executor = StdLibPrimitiveExecutionsForReflection<Any>().also {
            val qn = AglBase.typesDomain.findFirstDefinitionByNameOrNull(SimpleName("QualifiedName"))!!
            val qn_v = qn.property.first { it.name.value == "value" }
            it.addPropertyExecution1(qn_v, QualifiedName::value)
        }

        fun test(typesDomain: TypesDomain, self: Any, selfTypeName: String, expression: String, expected: Any) {
            val st = typesDomain.findByQualifiedNameOrNull(selfTypeName.asQualifiedName)?.type() ?: StdLibDefault.AnyType
            val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)
            val og = ObjectGraphByReflection(typesDomain, issues, primitiveExecutor = executor)
            val interpreter = ExpressionsInterpreterOverTypedObject(og, issues)
            val actual = interpreter.evaluateStr(EvaluationContext.ofSelf(TypedObjectAny(st, self)), expression)
            assertTrue(interpreter.issues.errors.isEmpty(), interpreter.issues.toString())
            assertEquals(expected, og.untyped(actual))
        }

        fun test_fail(typesDomain: TypesDomain, self: Any, selfTypeName: String, expression: String, expected: Any, expectedIssues: List<LanguageIssue>) {
            val st = typesDomain.findByQualifiedNameOrNull(selfTypeName.asQualifiedName)?.type() ?: StdLibDefault.AnyType
            val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)
            val og = ObjectGraphByReflection(typesDomain, issues, primitiveExecutor = executor)
            val interpreter = ExpressionsInterpreterOverTypedObject(og, issues)
            val actual = interpreter.evaluateStr(EvaluationContext.ofSelf(TypedObjectAny(st, self)), expression)
            assertEquals(expected, og.untyped(actual))
            assertEquals(expectedIssues, interpreter.issues.all.toList())
        }


    }

    @Test
    fun nothing() {
        val expression = $$"$nothing"
        val tm = typesDomain("test", true) {
            namespace("ns") {
                data("TestObj") {
                    propertyByEvaluation("prop1", "String").also {
                        executor.addPropertyExecution1(it, TestObj::prop1)
                    }
                }
            }
        }
        val self = TestObj("strValue")

        test(tm, self, "ns.TestObj", expression, Unit)
    }

    @Test
    fun primitive_string() {
        val expression = $$"'Hello World!'"
        val tm = typesDomain("test", true) {
        }
        val self = Unit

        test(tm, self, StdLibDefault.NothingType.qualifiedTypeName.value, expression, "Hello World!")
    }

    @Test
    fun Set_of_string() {
        val expression = $$"Set('Hello', 'World', '!')"
        val tm = typesDomain("test", true) {
        }
        val self = Unit

        test(
            tm, self, StdLibDefault.NothingType.qualifiedTypeName.value, expression,
            setOf("Hello", "World", "!")
        )
    }

    @Test
    fun Map_of_string_integer() = runTest {
        val expression = $$"List(Pair('a', 1), Pair('b', 2), Pair('c', 3), Pair('d', 4)).asMap['b']"
        val tm = typesDomain("test", true) {
        }
        val self = Unit

        test(
            tm, self, StdLibDefault.NothingType.qualifiedTypeName.value, expression,
            2L
        )
    }

    @Test
    fun structure_self() {
        val expression = $$"$self"
        val tm = typesDomain("test", true) {
            namespace("ns") {
                data("TestObj") {
                    propertyByEvaluation("prop1", "String").also {
                        executor.addPropertyExecution1(it, TestObj::prop1)
                    }
                }
            }
        }
        val self = TestObj("strValue")

        test(tm, self, "ns.TestObj", expression, self)
    }

    @Test
    fun structure_property__notfound() {
        val tm = typesDomain("test", true) {
            namespace("ns") {
                data("TestObj") {
                    propertyByEvaluation("prop1", "String").also {
                        executor.addPropertyExecution1(it, TestObj::prop1)
                    }
                }
            }
        }
        val self = TestObj("strValue")

        val expectedIssues = listOf(
            LanguageIssue(
                LanguageIssueKind.ERROR, LanguageProcessorPhase.INTERPRET,
                null,
                "Unable to evaluate property 'prop2': Property prop2 not found on object TestObj(prop1=strValue, propList=[], propListA=[])"
            )
        )
        test_fail(tm, self, "ns.TestObj", "prop2", Unit, expectedIssues)
    }

    @Test
    fun structure_property__found() {
        val tm = typesDomain("test", true) {
            namespace("ns") {
                data("TestObj") {
                    propertyByEvaluation("prop1", "String").also {
                        executor.addPropertyExecution1(it, TestObj::prop1)
                    }
                }
            }
        }
        val self = TestObj("strValue")

        test(tm, self, "ns.TestObj", "prop1", "strValue")
    }

    @Test
    fun structure_property_index__notIndexable() {
        val tm = typesDomain("test", true) {
            namespace("ns") {
                data("TestObj") {
                    propertyByEvaluation("prop1", "String").also {
                        executor.addPropertyExecution1(it, TestObj::prop1)
                    }
                }
            }
        }
        val self = TestObj("strValue")

        val expectedIssues = listOf(
            LanguageIssue(
                LanguageIssueKind.ERROR, LanguageProcessorPhase.INTERPRET,
                null,
                "Index operation on non List value is not possible: strValue"
            )
        )
        test_fail(tm, self, "ns.TestObj", "prop1[0]", Unit, expectedIssues)
    }

    @Test
    fun structure_property_index__onlyOneIndexValue() {
        val tm = typesDomain("test", true) {
            namespace("ns") {
                data("TestObj") {
                    propertyByEvaluation("prop1", "String").also {
                        executor.addPropertyExecution1(it, TestObj::prop1)
                    }
                    propertyByEvaluation("propList", "List") { typeArgument("String") }.also {
                        executor.addPropertyExecution1(it, TestObj::propList)
                    }
                }
            }
        }
        val self = TestObj("strValue", listOf("A", "B", "C"))

        val expectedIssues = listOf(
            LanguageIssue(
                LanguageIssueKind.ERROR, LanguageProcessorPhase.INTERPRET,
                null,
                "Only one index value should be used for Lists"
            )
        )
        test_fail(tm, self, "ns.TestObj", "propList[0,1,2]", Unit, expectedIssues)
    }

    @Test
    fun structure_property_index__mustBeInteger() {
        val tm = typesDomain("test", true) {
            namespace("ns") {
                data("TestObj") {
                    propertyByEvaluation("prop1", "String").also {
                        executor.addPropertyExecution1(it, TestObj::prop1)
                    }
                    propertyByEvaluation("propList", "List") { typeArgument("String") }.also {
                        executor.addPropertyExecution1(it, TestObj::propList)
                    }
                }
            }
        }
        val self = TestObj("strValue", listOf("A", "B", "C"))

        val expectedIssues = listOf(
            LanguageIssue(
                LanguageIssueKind.ERROR, LanguageProcessorPhase.INTERPRET,
                null,
                "Index value must evaluate to an Integer for Lists"
            )
        )
        test_fail(tm, self, "ns.TestObj", "propList['a']", Unit, expectedIssues)
    }

    @Test
    fun structure_property_index__outOfRange() {
        val tm = typesDomain("test", true) {
            namespace("ns") {
                data("TestObj") {
                    propertyByEvaluation("prop1", "String").also {
                        executor.addPropertyExecution1(it, TestObj::prop1)
                    }
                    propertyByEvaluation("propList", "List") { typeArgument("String") }.also {
                        executor.addPropertyExecution1(it, TestObj::propList)
                    }
                }
            }
        }
        val self = TestObj("strValue", listOf("A", "B", "C"))

        val expectedIssues = listOf(
            LanguageIssue(
                LanguageIssueKind.ERROR, LanguageProcessorPhase.INTERPRET,
                null,
                "In getFromListWithIndex argument index '4' out of range"
            )
        )
        test_fail(tm, self, "ns.TestObj", "propList[4]", Unit, expectedIssues)
    }

    @Test
    fun structure_propertyListOfString_index_0() {
        val tm = typesDomain("test", true) {
            namespace("ns") {
                data("TestObj") {
                    propertyByEvaluation("prop1", "String").also {
                        executor.addPropertyExecution1(it, TestObj::prop1)
                    }
                    propertyByEvaluation("propList", "List") { typeArgument("String") }.also {
                        executor.addPropertyExecution1(it, TestObj::propList)
                    }
                }
            }
        }
        val self = TestObj("strValue", listOf("A", "B", "C"))

        val expected = "A"
        test(tm, self, "ns.TestObj", "propList[0]", expected)
    }

    @Test
    fun structure_propertyListOfA_get() {
        val tm = typesDomain("test", true) {
            namespace("ns") {
                data("TestObj") {
                    propertyByEvaluation("prop1", "String").also {
                        executor.addPropertyExecution1(it, TestObj::prop1)
                    }
                    propertyByEvaluation("propList", "List") { typeArgument("String") }.also {
                        executor.addPropertyExecution1(it, TestObj::propList)
                    }
                    propertyByEvaluation("propListA", "List") { typeArgument("TestObjA") }.also {
                        executor.addPropertyExecution1(it, TestObj::propListA)
                    }
                }
                data("TestObjA") {
                    propertyByEvaluation("prop1", "String").also {
                        executor.addPropertyExecution1(it, TestObjA::prop1)
                    }
                }
            }
        }
        val self = TestObj("strValue", listOf("A", "B", "C"), listOf(TestObjA("A"), TestObjA("B"), TestObjA("C")))

        val expected = "B"
        test(tm, self, "ns.TestObj", "propListA.get(1).prop1", expected)
    }

    @Test
    fun structure_propertyListOfA_map() {
        val tm = typesDomain("test", true) {
            namespace("ns") {
                data("TestObj") {
                    propertyByEvaluation("prop1", "String").also {
                        executor.addPropertyExecution1(it, TestObj::prop1)
                    }
                    propertyByEvaluation("propList", "List") { typeArgument("String") }.also {
                        executor.addPropertyExecution1(it, TestObj::propList)
                    }
                    propertyByEvaluation("propListA", "List") { typeArgument("TestObjA") }.also {
                        executor.addPropertyExecution1(it, TestObj::propListA)
                    }
                }
                data("TestObjA") {
                    propertyByEvaluation("prop1", "String").also {
                        executor.addPropertyExecution1(it, TestObjA::prop1)
                    }
                }
            }
        }
        val self = TestObj("strValue", listOf("A", "B", "C"), listOf(TestObjA("A"), TestObjA("B"), TestObjA("C")))
        val expected = listOf("A", "B", "C")

        test(tm, self, "ns.TestObj", "propListA.map({it.prop1})", expected)
    }
}