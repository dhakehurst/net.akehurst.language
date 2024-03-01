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

package net.akehurst.language.agl.language.asmTransform.test

import net.akehurst.language.agl.language.expressions.test.ExpressionsTest
import net.akehurst.language.agl.language.typemodel.test.TypeModelTest
import net.akehurst.language.api.language.asmTransform.AsmTransformModel
import net.akehurst.language.api.language.asmTransform.AssignmentTransformationStatement
import net.akehurst.language.api.language.asmTransform.TransformationRule
import kotlin.test.assertEquals
import kotlin.test.fail

object AsmTransformModelTest {

    fun trAssertEquals(expected: AsmTransformModel?, actual: AsmTransformModel?) {
        when {
            (expected == null && actual == null) -> Unit // pass
            expected == null -> fail()
            actual == null -> fail()
            else -> {
                assertEquals(expected.name, actual.name)
                assertEquals(expected.qualifiedName, actual.qualifiedName)
                assertEquals(expected.rules.size, actual.rules.size, "number of rules in AsmTransformModel is different")
                for (k in expected.rules.keys) {
                    val expEl = expected.rules[k]!!
                    val actEl = actual.rules[k]!!
                    trAssertEquals(expEl, actEl, "AsmTransformModel")
                }
            }
        }
    }

    private fun trAssertEquals(expected: TransformationRule, actual: TransformationRule, message: String) {
        assertEquals(expected.grammarRuleName, actual.grammarRuleName)
        assertEquals(expected.typeName, actual.typeName)
        TypeModelTest.tmAssertEquals(expected.resolvedType, actual.resolvedType, "TransformationRule")
        trAssertEquals(expected.modifyStatements, actual.modifyStatements, "")
    }

    private fun trAssertEquals(expected: List<AssignmentTransformationStatement>, actual: List<AssignmentTransformationStatement>, message: String) {
        assertEquals(expected.size, actual.size, "number of AssignmentTransformationStatement is different")
        for (i in expected.indices) {
            val expEl = expected[i]
            val actEl = actual[i]
            trAssertEquals(expEl, actEl, "AssignmentTransformationStatement")
        }
    }

    private fun trAssertEquals(expected: AssignmentTransformationStatement, actual: AssignmentTransformationStatement, message: String) {
        assertEquals(expected.lhsPropertyName, actual.lhsPropertyName)
        ExpressionsTest.exAssertEquals(expected.rhs, actual.rhs)
    }
}