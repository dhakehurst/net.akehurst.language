/*
 * Copyright (C) 2024 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.language.expressions.test

import net.akehurst.language.api.language.expressions.*
import kotlin.test.assertEquals
import kotlin.test.fail

object ExpressionsTest {

    fun exAssertEquals(expected: Expression, actual: Expression) {
        when {
            expected is RootExpression && actual is RootExpression -> exAssertEquals(expected, actual)
            expected is LiteralExpression && actual is LiteralExpression -> exAssertEquals(expected, actual)
            expected is NavigationExpression && actual is NavigationExpression -> exAssertEquals(expected, actual)
            else -> fail("Type of transformation rules do not match: ${expected::class.simpleName} != ${actual::class.simpleName}")
        }
    }

    fun exAssertEquals(expected: RootExpression, actual: RootExpression) {
        assertEquals(expected.name, actual.name)
    }

    fun exAssertEquals(expected: LiteralExpression, actual: LiteralExpression) {
        assertEquals(expected.typeName, actual.typeName)
        assertEquals(expected.value, actual.value)
    }

    fun exAssertEquals(expected: NavigationExpression, actual: NavigationExpression) {
        exAssertEquals(expected.start, actual.start)
        exAssertEquals(expected.parts, actual.parts)
    }

    fun exAssertEquals(expected: List<NavigationPart>, actual: List<NavigationPart>) {
        assertEquals(expected.size, actual.size)
        for (i in expected.indices) {
            val exp = expected[i]
            val act = actual[i]
            exAssertEquals(exp, act)
        }
    }

    fun exAssertEquals(expected: NavigationPart, actual: NavigationPart) {
        when {
            expected is PropertyCall && actual is PropertyCall -> exAssertEquals(expected, actual)
            expected is MethodCall && actual is MethodCall -> exAssertEquals(expected, actual)
            expected is IndexOperation && actual is IndexOperation -> exAssertEquals(expected, actual)
            else -> fail("Type of transformation rules do not match: ${expected::class.simpleName} != ${actual::class.simpleName}")
        }
    }

    fun exAssertEquals(expected: PropertyCall, actual: PropertyCall) {
        assertEquals(expected.propertyName, actual.propertyName)
    }

    fun exAssertEquals(expected: MethodCall, actual: MethodCall) {
        assertEquals(expected.methodName, actual.methodName)
        assertEquals(expected.arguments.size, actual.arguments.size)
        for (i in expected.arguments.indices) {
            val exp = expected.arguments[i]
            val act = actual.arguments[i]
            exAssertEquals(exp, act)
        }
    }

    fun exAssertEquals(expected: IndexOperation, actual: IndexOperation) {
        assertEquals(expected.indices.size, actual.indices.size)
        for (i in expected.indices.indices) {
            val exp = expected.indices[i]
            val act = actual.indices[i]
            exAssertEquals(exp, act)
        }
    }
}