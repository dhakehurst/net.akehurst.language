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

package net.akehurst.language.transform.asm.test

import net.akehurst.language.expressions.processor.test.ExpressionsTest
import net.akehurst.language.typemodel.asm.test.TypeModelTest
import net.akehurst.language.transform.api.TransformModel
import net.akehurst.language.transform.api.TransformNamespace
import net.akehurst.language.transform.api.TransformRuleSet
import net.akehurst.language.transform.api.TransformationRule
import kotlin.test.assertEquals
import kotlin.test.fail

object AsmTransformModelTest {

    fun trAssertEquals(expected: TransformModel?, actual: TransformModel?) {
        assertEquals(expected?.name, actual?.name)
        assertEquals(expected?.namespace?.size, actual?.namespace?.size, "Different number of namespaces")
        if (expected?.namespace != null && actual?.namespace != null) {
            for (i in expected.namespace.indices) {
                val exp = expected.namespace[i]
                val act = actual.namespace[i]
                trAssertEquals(exp, act)
            }
        }
    }

    fun trAssertEquals(expected: TransformNamespace?, actual: TransformNamespace?) {
        assertEquals(expected?.qualifiedName, actual?.qualifiedName)
        assertEquals(expected?.definition?.size, actual?.definition?.size, "Different number of definitions")
        if (expected?.definition != null && actual?.definition != null) {
            for (i in expected.definition.indices) {
                val exp = expected.definition[i]
                val act = actual.definition[i]
                trAssertEquals(exp, act)
            }
        }
    }

    private fun trAssertEquals(expected: TransformRuleSet?, actual: TransformRuleSet?) {
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
        assertEquals(expected.possiblyQualifiedTypeName, actual.possiblyQualifiedTypeName)
        TypeModelTest.tmAssertEquals(expected.resolvedType, actual.resolvedType, "TransformationRule")
        ExpressionsTest.exAssertEquals(expected.expression, actual.expression)
    }


}