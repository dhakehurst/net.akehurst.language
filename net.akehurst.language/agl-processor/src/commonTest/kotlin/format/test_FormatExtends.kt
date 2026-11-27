/**
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.format.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.FormatString
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.formatter.api.FormatSet
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** 'format B : A' - the extended format set A, in the same file or not */
class test_FormatExtends {

    data class TestObject(val intValue: Int = 3)

    private val types = typesDomain("Test", true) {
        namespace("test") {
            data("TestObject")
        }
    }

    private fun process(str: String) = Agl.formatDomain(FormatString(str), types)

    private fun formatSet(str: String, qname: String): FormatSet {
        val res = process(str)
        assertNotNull(res.asm, res.allIssues.toString())
        return res.asm!!.findDefinitionByQualifiedNameOrNull(QualifiedName(qname)) as FormatSet
    }

    @Test
    fun extends_same_file_resolves() {
        val res = process(
            $$"""
            namespace test
            format A {
              TestObject -> "A:$intValue"
            }
            format B : A {
            }
            """
        )
        assertTrue(res.allIssues.errors.isEmpty(), res.allIssues.toString())
        val b = res.asm!!.findDefinitionByQualifiedNameOrNull(QualifiedName("test.B")) as FormatSet
        assertEquals(QualifiedName("test.A"), b.extends[0].resolved?.qualifiedName)
    }

    @Test
    fun extends_same_file_other_namespace_qualified() {
        val res = process(
            $$"""
            namespace base
            format A {
              TestObject -> "A:$intValue"
            }
            namespace test
            format B : base.A {
            }
            """
        )
        assertTrue(res.allIssues.errors.isEmpty(), res.allIssues.toString())
        val b = res.asm!!.findDefinitionByQualifiedNameOrNull(QualifiedName("test.B")) as FormatSet
        assertEquals(QualifiedName("base.A"), b.extends[0].resolved?.qualifiedName)
    }

    @Test
    fun extends_simple_name_of_other_namespace_not_found() {
        // a simple name is qualified with the namespace of the extending format set
        val res = process(
            $$"""
            namespace base
            format A {
            }
            namespace test
            format B : A {
            }
            """
        )
        assertEquals(1, res.allIssues.errors.size, res.allIssues.toString())
    }

    @Test
    fun extends_not_found() {
        val res = process(
            """
            namespace test
            format B : X {
            }
            """
        )
        assertEquals(1, res.allIssues.errors.size, res.allIssues.toString())
        assertTrue(res.allIssues.errors.first().message.contains("'X'"), res.allIssues.toString())
    }

    @Test
    fun extends_self_is_error() {
        val res = process(
            """
            namespace test
            format A : A {
            }
            """
        )
        assertEquals(1, res.allIssues.errors.size, res.allIssues.toString())
        val a = res.asm!!.findDefinitionByQualifiedNameOrNull(QualifiedName("test.A")) as FormatSet
        assertEquals(emptyList(), a.allRules)
    }

    @Test
    fun extends_cycle_is_error() {
        val res = process(
            """
            namespace test
            format A : B {
            }
            format B : A {
            }
            """
        )
        assertTrue(res.allIssues.errors.isNotEmpty(), res.allIssues.toString())
        val a = res.asm!!.findDefinitionByQualifiedNameOrNull(QualifiedName("test.A")) as FormatSet
        assertEquals(emptyList(), a.allRules) // terminates
    }

    @Test
    fun allRules_includes_extended_rules() {
        val b = formatSet(
            $$"""
            namespace test
            format A {
              TestObject -> "A:$intValue"
            }
            format B : A {
              Any -> "B"
            }
            """, "test.B"
        )
        assertEquals(2, b.allRules.size)
    }

    @Test
    fun format_uses_rule_of_extended_set() {
        val res = Agl.formatByReflection(
            FormatString(
                $$"""
                namespace test
                format A {
                  TestObject -> "A:$intValue"
                }
                format B : A {
                }
                """
            ), types, TestObject()
        )
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        assertEquals("A:3", res.sentence)
    }

    @Test
    fun format_own_rule_overrides_extended_rule() {
        val res = Agl.formatByReflection(
            FormatString(
                $$"""
                namespace test
                format A {
                  TestObject -> "A:$intValue"
                }
                format B : A {
                  TestObject -> "B:$intValue"
                }
                """
            ), types, TestObject()
        )
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        assertEquals("B:3", res.sentence)
    }
}
