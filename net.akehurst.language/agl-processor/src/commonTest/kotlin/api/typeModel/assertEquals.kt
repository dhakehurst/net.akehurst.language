/**
 * Copyright (C) 2022 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.akehurst.language.api.typeModel

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

object TypeModelTest {

    fun assertEquals(expected: TypeModel, actual: TypeModel) {
        assertEquals(expected.types.size, actual.types.size)
        for (k in expected.types.keys) {
            val expEl = expected.types[k]
            val actEl = actual.types[k]
            assertEquals(expEl, actEl)
        }
    }

    private fun assertEquals(expected: RuleType?, actual: RuleType?) {
        when {
            null == expected || null == actual -> fail("should never be null")
            expected is BuiltInType && actual is BuiltInType -> assertTrue(expected === actual)
            expected is ElementType && actual is ElementType -> assertEquals(expected, actual)
            expected is TupleType && actual is TupleType -> assertEquals(expected, actual)
            else -> fail("Types do not match $expected != $actual")
        }
    }

    private fun assertEquals(expected: ElementType, actual: ElementType) {
        assertEquals(expected.name, actual.name)
        assertEquals(expected.superType.size, actual.superType.size, "Wrong number of superTypes for '${expected.name}'")
        for (i in 0 until expected.superType.size) {
            val expEl = expected.superType[i]
            val actEl = actual.superType[i]
            assertEquals(expEl.name, actEl.name)
        }
        assertEquals(expected.subType.size, actual.subType.size, "Wrong number of subTypes for '${expected.name}'")
        for (i in 0 until expected.subType.size) {
            val expEl = expected.subType.toList()[i] //TODO: set set equality !
            val actEl = actual.subType.toList()[i]
            assertEquals(expEl.name, actEl.name)
        }
        assertEquals(expected.property.size, actual.property.size, "Wrong number of properties for '${expected.name}'")
        for (k in expected.property.keys) {
            val expEl = expected.property[k]
            val actEl = actual.property[k]
            assertNotNull(actEl, "expected PropertyDeclaration '$k' not found in actual ElementType '${expected.name}'. [${actual.property.values.joinToString { it.name }}]")
            assertEquals(expEl, actEl)
        }
    }

    private fun assertEquals(expected: TupleType, actual: TupleType) {
        assertEquals(expected.property.size, actual.property.size, "Wrong number of properties for '${expected.name}'")
        for (k in expected.property.keys) {
            val expEl = expected.property[k]
            val actEl = actual.property[k]
            assertNotNull(actEl, "expected PropertyDeclaration '$k' not found in actual TupleType '${expected.name}'. [${actual.property.values.joinToString { it.name }}]")
            assertEquals(expEl, actEl)
        }
    }

    private fun assertEquals(expected: PropertyDeclaration?, actual: PropertyDeclaration?) {
        when {
            null == expected || null == actual -> fail("should never be null")
            else -> {
                assertEquals(expected.name, actual.name)
                assertEquals(expected.isNullable, actual.isNullable, "Different nullable for ${expected}")
                assertEquals(expected.childIndex, actual.childIndex, "Different childIndex for ${expected}")
                when(expected.type) {
                    is ElementType -> assertEquals(expected.type.name, actual.type.name, "Different types for ${expected}")
                    else -> TypeModelTest.assertEquals(expected.type, actual.type)
                }
            }
        }
    }
}