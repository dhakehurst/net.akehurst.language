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

package net.akehurst.language.agl.expressions.processor

import kotlinx.coroutines.test.runTest
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class test_ObjectGraphByReflectionSuspending {

    private companion object {
        data class TestClass(
            val prop1: String,
            val prop2: Int,
            val prop3: TestClass?,
        )

        val testTypeModel = typesDomain("Test", true) {
            namespace("net.akehurst.language.agl.expressions.processor") {
                data("TestClass") {
                }
            }
        }

        fun test() {

        }
    }

    @Test
    fun nothing() {
        val og = ObjectGraphAccessorMutatorSuspendingByReflection(testTypeModel, IssueHolder(LanguageProcessorPhase.INTERPRET))

        val actual = og.nothing()
        val expected = Unit
        assertEquals(expected, actual.self)
    }

    @Test
    fun createPrimitiveValue_boolean() {
        val og = ObjectGraphAccessorMutatorByReflection(testTypeModel, IssueHolder(LanguageProcessorPhase.INTERPRET))

        val actual = og.createPrimitiveValue(StdLibDefault.Boolean.qualifiedTypeName, true)
        val expected = true
        assertEquals(expected, actual.self)
    }

    @Test
    fun createTupleValue() = runTest {
        val og = ObjectGraphAccessorMutatorSuspendingByReflection(testTypeModel, IssueHolder(LanguageProcessorPhase.INTERPRET))

        val actual = og.createTupleValue(listOf())
        og.setProperty(actual, "a", og.createPrimitiveValue(StdLibDefault.Integer.qualifiedTypeName, 1L))
        og.setProperty(actual, "b", og.createPrimitiveValue(StdLibDefault.Boolean.qualifiedTypeName, true))
        val expected = mapOf(
            "a" to 1L,
            "b" to true
        )
        assertEquals(expected, actual.self)
    }

    @Test
    fun getIndex() {
        val og = ObjectGraphAccessorMutatorSuspendingByReflection(testTypeModel, IssueHolder(LanguageProcessorPhase.INTERPRET))
        val list = og.typedAs(listOf("Adam", "Betty", "Charles"), StdLibDefault.List.type(listOf(StdLibDefault.String.asTypeArgument)))

        val actual1 = og.getFromListWithIndex(list, 0)
        assertEquals("Adam", actual1.self)

        val actual2 = og.getFromListWithIndex(list, 1)
        assertEquals("Betty", actual2.self)

        val actual3 = og.getFromListWithIndex(list, 2)
        assertEquals("Charles", actual3.self)

        val actual4 = og.getFromListWithIndex(list, -1)
        assertEquals(Unit, actual4.self)
        assertEquals(1, og.issues.size)
        assertTrue(og.issues.contains(LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.INTERPRET, null, "In getIndex argument index '-1' out of range", null)), og.issues.toString())

        val actual5 = og.getFromListWithIndex(list, 5)
        assertEquals(Unit, actual5.self)
        assertEquals(2, og.issues.size)
        assertTrue(og.issues.contains(LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.INTERPRET, null, "In getIndex argument index '5' out of range", null)), og.issues.toString())

    }

    @Test
    fun object_getProperty() = runTest {
        val og = ObjectGraphAccessorMutatorSuspendingByReflection(testTypeModel, IssueHolder(LanguageProcessorPhase.INTERPRET))
        val obj = TestClass("A", 1, TestClass("B", 2, null))
        val tp = testTypeModel.findFirstDefinitionByNameOrNull(SimpleName("TestClass"))!!.type()
        val tobj =  og.typedAs(obj,tp)

        val actual1 = og.getProperty(tobj, "prop1")
        assertEquals("A", actual1.self)

        val actual2 = og.getProperty(tobj, "prop2")
        assertEquals(1, actual2.self)

        val actual3 = og.getProperty(tobj, "prop3")
        assertEquals(TestClass("B", 2, null), actual3.self)

        val actual4 = og.getProperty(og.getProperty(tobj, "prop3"), "prop1")
        assertEquals("B", actual4.self)
    }

    @Test
    fun tuple_getProperty() = runTest {
        val og = ObjectGraphAccessorMutatorSuspendingByReflection(testTypeModel, IssueHolder(LanguageProcessorPhase.INTERPRET))
        val obj = mapOf(
            "prop1" to "A",
            "prop2" to 1,
            "prop3" to TestClass("B", 2, null)
        )
        val tp = StdLibDefault.TupleType.type()
        val tobj =  og.typedAs(obj,tp)

        val actual1 = og.getProperty(tobj, "prop1")
        assertEquals("A", actual1.self)

        val actual2 = og.getProperty(tobj, "prop2")
        assertEquals(1, actual2.self)

        val actual3 = og.getProperty(tobj, "prop3")
        assertEquals(TestClass("B", 2, null), actual3.self)

        val actual4 = og.getProperty(og.getProperty(tobj, "prop3"), "prop1")
        assertEquals("B", actual4.self)
    }

    @Test
    fun executeMethod_Primitive_map() = runTest {
        //given
        val og = ObjectGraphAccessorMutatorSuspendingByReflection(testTypeModel, IssueHolder(LanguageProcessorPhase.INTERPRET))
        val tObj = og.typedAs(
            listOf(
                og.typedAs(1L, StdLibDefault.Integer),
                og.typedAs(2L, StdLibDefault.Integer),
                og.typedAs(3L, StdLibDefault.Integer)
            ),
            StdLibDefault.List.type(listOf(StdLibDefault.Integer.asTypeArgument))
        )
        val sl: suspend (Any) -> String = { it: Any -> it.toString() }
        val lambda = og.typedAs(sl, StdLibDefault.Lambda)
        //when
        val actual = og.executeMethod(tObj, "map", listOf(lambda))

        //then
        assertTrue(actual.self is List<*>)
        assertEquals(listOf("1", "2", "3"), og.untyped(actual))
    }

    @Ignore
    @Test
    fun executeMethod_notInLib() {
        TODO()
    }

    @Ignore
    @Test
    fun cast() {
        TODO()
    }

    @Ignore
    @Test
    fun createStructureValue() {
        TODO()
    }
}