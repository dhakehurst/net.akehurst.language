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

package net.akehurst.language.expressions.test

import net.akehurst.language.expressions.api.*
import kotlin.test.assertEquals
import kotlin.test.fail

object ExpressionsTest {

    fun exAssertEquals(expected: Expression, actual: Expression, message: String = "") {
        when {
            expected is RootExpression && actual is RootExpression -> exAssertEquals(expected, actual)
            expected is LiteralExpression && actual is LiteralExpression -> exAssertEquals(expected, actual)
            expected is NavigationExpression && actual is NavigationExpression -> exAssertEquals(expected, actual)
            expected is InfixExpression && actual is InfixExpression -> exAssertEquals(expected, actual)
            expected is CreateTupleExpression && actual is CreateTupleExpression -> exAssertEquals(expected, actual)
            expected is CreateObjectExpression && actual is CreateObjectExpression -> exAssertEquals(expected, actual)
            expected is WithExpression && actual is WithExpression -> exAssertEquals(expected, actual)
            expected is WhenExpression && actual is WhenExpression -> exAssertEquals(expected, actual)
            expected is LambdaExpression && actual is LambdaExpression -> exAssertEquals(expected, actual)
            else -> fail("Type of transformation rules do not match: ${expected::class.simpleName} != ${actual::class.simpleName}")
        }
    }

    fun exAssertEquals(expected: RootExpression, actual: RootExpression) {
        assertEquals(expected.name, actual.name)
    }

    fun exAssertEquals(expected: LiteralExpression, actual: LiteralExpression) {
        assertEquals(expected.qualifiedTypeName, actual.qualifiedTypeName)
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

    fun exAssertEquals(expected: InfixExpression, actual: InfixExpression) {
        assertEquals(expected.expressions.size, actual.expressions.size, "number of expressions is different")
        assertEquals(expected.operators, actual.operators, "operators are different")
        for (i in expected.expressions.indices) {
            val expEl = expected.expressions[i]
            val actEl = actual.expressions[i]
            exAssertEquals(expEl, actEl)
        }
    }

    fun exAssertEquals(expected: CreateObjectExpression, actual: CreateObjectExpression) {
        assertEquals(expected.possiblyQualifiedTypeName, actual.possiblyQualifiedTypeName, "possiblyQualifiedTypeName")
        exAssertEquals(expected.constructorArguments, actual.constructorArguments, "constructorArguments")
        exAssertEquals(expected.propertyAssignments, actual.propertyAssignments, "propertyAssignments")
    }

    fun exAssertEquals(expected: CreateTupleExpression, actual: CreateTupleExpression) {
        exAssertEquals(expected.propertyAssignments, actual.propertyAssignments, "propertyAssignments")
    }

    fun exAssertEquals(expected: WithExpression, actual: WithExpression) {
        exAssertEquals(expected.withContext, actual.withContext)
        exAssertEquals(expected.expression, actual.expression)
    }

    fun exAssertEquals(expected: WhenExpression, actual: WhenExpression) {
        assertEquals(expected.options.size, actual.options.size, "number of WhenOptions is different")
        for (i in expected.options.indices) {
            val expEl = expected.options[i]
            val actEl = actual.options[i]
            exAssertEquals(expEl, actEl)//, "WhenOption")
        }
    }

    fun exAssertEquals(expected: WhenOption, actual: WhenOption) {
        exAssertEquals(expected.condition, actual.condition)
        exAssertEquals(expected.expression, actual.expression)
    }

    fun exAssertEquals(expected: LambdaExpression, actual: LambdaExpression) {
        //TODO: args
        exAssertEquals(expected.expression, actual.expression, "Lambda is different")
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

    fun exAssertEquals(expected: List<AssignmentStatement>, actual: List<AssignmentStatement>, message: String) {
        assertEquals(expected.size, actual.size, "number of AssignmentStatement is different")
        for (i in expected.indices) {
            val expEl = expected[i]
            val actEl = actual[i]
            exAssertEquals(expEl, actEl, "AssignmentStatement")
        }
    }

    private fun exAssertEquals(expected: AssignmentStatement, actual: AssignmentStatement, message: String) {
        assertEquals(expected.lhsPropertyName, actual.lhsPropertyName)
        ExpressionsTest.exAssertEquals(expected.rhs, actual.rhs)
    }

    fun <E : Expression> exAssertEqualsExprList(expected: List<E>, actual: List<E>, message: String) {
        assertEquals(expected.size, actual.size, "number of $message is different")
        for (i in expected.indices) {
            val expEl = expected[i]
            val actEl = actual[i]
            exAssertEquals(expEl, actEl, message)
        }
    }
}