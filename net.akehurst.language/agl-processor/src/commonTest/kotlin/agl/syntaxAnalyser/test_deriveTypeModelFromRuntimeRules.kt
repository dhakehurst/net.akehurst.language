/**
 * Copyright (C) 2021 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.syntaxAnalyser

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.typeModel.*
import kotlin.test.*

class test_deriveTypeModelFromRuntimeRules {

    private fun _assertEquals(expected: TypeModel, actual: TypeModel) {
        assertEquals(expected.types.size, actual.types.size)
        for (k in expected.types.keys) {
            val expEl = expected.types[k]
            val actEl = actual.types[k]
            _assertEquals(expEl, actEl)
        }
    }

    private fun _assertEquals(expected: RuleType?, actual: RuleType?) {
        when {
            null == expected || null == actual -> fail("should never be null")
            expected is BuiltInType && actual is BuiltInType -> assertTrue(expected === actual)
            expected is ElementType && actual is ElementType -> _assertEquals(expected, actual)
        }
    }

    private fun _assertEquals(expected: ElementType, actual: ElementType) {
        assertEquals(expected.name, actual.name)
        assertEquals(expected.superType.size, actual.superType.size, "Wrong number of superTypes for '${expected.name}'")
        for (i in 0 until expected.superType.size) {
            val expEl = expected.superType[i]
            val actEl = actual.superType[i]
            _assertEquals(expEl, actEl)
        }
        assertEquals(expected.property.size, actual.property.size, "Wrong number of properties for '${expected.name}'")
        for (k in expected.property.keys) {
            val expEl = expected.property[k]
            val actEl = actual.property[k]
            assertNotNull(actEl, "expected PropertyDeclaration '$k' not found in actual ElementType '${expected.name}")
            _assertEquals(expEl, actEl)
        }
    }

    private fun _assertEquals(expected: PropertyDeclaration?, actual: PropertyDeclaration?) {
        when {
            null == expected || null == actual -> fail("should never be null")
            else -> {
                assertEquals(expected.name, actual.name)
                assertEquals(expected.type.name, actual.type.name)
            }
        }
    }

    @Test
    fun root_literal() {
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("a") }
        }
        val actual = TypeModelFromRuntimeRules(rrs.runtimeRules).derive()
        val expected = typeModel {
            elementType("S") {
                propertyStringType("value")
            }
        }

        _assertEquals(expected, actual)
    }

    @Test
    fun root_pattern() {
        val rrs = runtimeRuleSet {
            concatenation("S") { pattern("[a-z]+") }
        }
        val actual = TypeModelFromRuntimeRules(rrs.runtimeRules).derive()
        val expected = typeModel {
            elementType("S") {
                propertyStringType("value")
            }
        }

        _assertEquals(expected, actual)
    }

    @Test
    fun root_empty() {
        val rrs = runtimeRuleSet {
            concatenation("S") { empty() }
        }
        val actual = TypeModelFromRuntimeRules(rrs.runtimeRules).derive()
        val expected = typeModel {
            elementType("S") {
            }
        }

        _assertEquals(expected, actual)
    }

    @Test
    fun root_multi_literal() {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("as") }
            multi("as", 0, -1, "'a'")
            literal("'a'", "a")
        }
        val actual = TypeModelFromRuntimeRules(rrs.runtimeRules).derive()
        val expected = typeModel {
            elementType("S") {
                propertyListOfStringType("as") // of String
            }
        }

        _assertEquals(expected, actual)
    }

    @Test
    fun root_slist_literal() {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("as") }
            sList("as", 0, -1, "'a'", "','")
            literal("'a'", "a")
            literal("','", ",")
        }
        val actual = TypeModelFromRuntimeRules(rrs.runtimeRules).derive()
        val expected = typeModel {
            elementType("S") {
                propertyListOfStringType("as")
            }
        }

        _assertEquals(expected, actual)
    }

    @Test
    fun root_slist_nonTerm() {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("as") }
            sList("as", 0, -1, "a", "','")
            concatenation("a") { literal("a") }
            literal("','", ",")
        }
        val actual = TypeModelFromRuntimeRules(rrs.runtimeRules).derive()
        val expected = typeModel {
            elementType("S") {
                propertyListType("as", "a") // of String
            }
            elementType("a") {
                propertyStringType("value")
            }
        }

        _assertEquals(expected, actual)
    }

}