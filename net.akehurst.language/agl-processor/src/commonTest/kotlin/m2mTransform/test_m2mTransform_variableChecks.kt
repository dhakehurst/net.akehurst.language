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

package net.akehurst.language.m2mTransform.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.simple.SentenceContextAny
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * issue #20, ER8: a variable used in an m2m rule must be bound, and a value assigned with ':=' must conform to the type
 * it is assigned to, else an error when the transform is checked.
 */
class test_m2mTransform_variableChecks {

    private companion object {
        val d1 = typesDomain("Domain1", true) {
            namespace("n1") {
                data("A") {
                    propertyOf(emptySet(), "name", "String")
                    propertyOf(emptySet(), "items", "List", false) { typeArgument("B") }
                }
                data("B") {
                    propertyOf(emptySet(), "name", "String")
                    propertyOf(emptySet(), "size", "Integer")
                }
            }
        }
        val d2 = typesDomain("Domain2", true) {
            namespace("n2") {
                data("X") {
                    propertyOf(emptySet(), "label", "String")
                    propertyOf(emptySet(), "labels", "List", false) { typeArgument("String") }
                    propertyOf(emptySet(), "count", "Integer")
                }
            }
        }

        fun errors(rules: String): List<String> {
            val context = SentenceContextAny()
            listOf(d1, d2).forEach { context.addToScope(null, listOf(it.name.value), QualifiedName("TypesDomain"), null, it) }
            val transform = $$"""
                namespace test
                transform Test(d1:Domain1, d2:Domain2) {
                  import-types n1
                  import-types n2
                  $$rules
                }
            """.trimIndent()
            val res = Agl.registry.agl.m2mTransform.processor!!.process(transform, options = Agl.options { semanticAnalysis { sentenceContext(context) } })
            assertTrue(res.allIssues.errors.none { !it.message.contains("never bound") && !it.message.contains("does not conform") }, res.allIssues.toString())
            return res.allIssues.errors.map { it.message }
        }
    }

    @Test
    fun used_in_target_expression_not_bound__error() {
        val errs = errors(
            """
            top mapping AX {
              domain d1 a:A { name == n }
              domain d2 x:X := X() { label := nn }
            }
            """
        )
        // a name in '{ p := ... }' may be a property of the created object, so only the expression is checked
        assertEquals(emptyList(), errs)
        val errs2 = errors(
            """
            top mapping AX {
              domain d1 a:A { name == n }
              domain d2 l:String := 'x' + nn
            }
            """
        )
        assertEquals(1, errs2.size, errs2.joinToString("\n"))
        assertTrue(errs2[0].contains("the variable 'nn' is used but never bound"), errs2[0])
        assertTrue(errs2[0].contains("rule 'AX'"), errs2[0])
    }

    @Test
    fun bound_by_template_signature_and_lambda__no_error() {
        val errs = errors(
            """
            top mapping AX {
              domain d1 a:A { name == n  items == bs }
              domain d2 l:String := n + a.name + bs.map({ b -> b.name }).first
            }
            """
        )
        assertEquals(emptyList(), errs)
    }

    @Test
    fun bound_by_where_target__no_error() {
        val errs = errors(
            """
            top mapping AX {
              domain d1 a:A { name == n }
              domain d2 l:String := y
              where { map Up { d1 := n  d2 := y } }
            }
            table Up {
              domain d1 :String   domain d2 :String
              values 'a' to 'b'
            }
            """
        )
        assertEquals(emptyList(), errs)
    }

    @Test
    fun used_in_when_not_bound__error() {
        val errs = errors(
            """
            top mapping AX {
              domain d1 a:A { name == n }
              domain d2 l:String := n
              when { m == 'c' }
            }
            """
        )
        assertEquals(1, errs.size, errs.joinToString("\n"))
        assertTrue(errs[0].contains("the variable 'm'"), errs[0])
    }

    @Test
    fun assigned_value_of_wrong_type__error() {
        val errs = errors(
            """
            top mapping BX {
              domain d1 b:B { name == n  size == sz }
              domain d2 x:X := X() { label := sz  count := sz }
            }
            """
        )
        assertEquals(1, errs.size, errs.joinToString("\n"))
        assertTrue(errs[0].contains("the value 'sz' has type 'Integer', which does not conform to the type 'String' of property 'label' of 'X'"), errs[0])
    }

    @Test
    fun assigned_target_variable_of_wrong_type__error() {
        val errs = errors(
            """
            top mapping BX {
              domain d1 b:B { name == n }
              domain d2 i:Integer := b.name
            }
            """
        )
        assertEquals(1, errs.size, errs.joinToString("\n"))
        assertTrue(errs[0].contains("the value 'b.name' has type 'String', which does not conform to the type 'Integer' of 'i'"), errs[0])
    }

    @Test
    fun assigned_values_of_right_type__no_error() {
        val errs = errors(
            """
            top mapping BX {
              domain d1 b:B { name == n  size == sz }
              domain d2 x:X := X() { label := n  count := b.size }
            }
            """
        )
        assertEquals(emptyList(), errs)
    }

}
