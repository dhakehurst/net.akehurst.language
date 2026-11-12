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

package net.akehurst.language.agl.simple

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.AsmTransformString
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.api.processor.TypesString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * asm-transform and types string cases found while writing the documentation.
 */
class test_ProvidedTransform_docsBugs {

    private companion object {
        val GRAMMAR = """
            namespace example
            grammar Shapes {
                skip leaf WS = "\s+" ;
                unit = item* ;
                item = circle | connection ;
                circle = 'circle' NAME radius? ;
                radius = 'r' NUMBER ;
                connection = 'connect' NAME 'to' NAME ;
                leaf NAME = "[a-z]+" ;
                leaf NUMBER = "[0-9]+" ;
            }
        """.trimIndent()
        const val SENTENCE = "circle c r 5 connect c to c"
    }

    @Test
    fun override_default_only__rule_with_existing_type() {
        // types of the default rules exist, without '#create-missing-types'
        val transform = """
            #override-default-transform
            namespace example
            asm-transform Shapes {
                connection: Connection() {
                    name := child[3]
                    name2 := child[1]
                }
            }
        """.trimIndent()
        val result = Agl.processorFromStringSimple(GrammarString(GRAMMAR), transformStr = AsmTransformString(transform))
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        val processed = result.processor!!.process(SENTENCE)
        assertTrue(processed.allIssues.errors.isEmpty(), processed.allIssues.toString())
        val expected = """
            :Unit {
              item = [
                :Circle {
                  name = 'c'
                  radius = :Radius {
                    number = '5'
                  }
                }
                :Connection {
                  name = 'c'
                  name2 = 'c'
                }
              ]
            }
        """.trimIndent()
        assertEquals(expected, processed.asm!!.asString().trim())
    }

    @Test
    fun rule_type_cannot_be_determined__is_an_error_issue() {
        val transform = """
            #override-default-transform
            #create-missing-types
            namespace example
            asm-transform Shapes {
                radius: child[1]
            }
        """.trimIndent()
        val result = Agl.processorFromStringSimple(GrammarString(GRAMMAR), transformStr = AsmTransformString(transform))
        val proc = result.processor!!
        proc.transformDomain // build it
        assertEquals(
            listOf("The type of rule 'radius' cannot be determined from 'child[1]', use 'Type() { ... }', 'tuple { ... }' or '<expression> as Type'"),
            proc.allIssues.errors.map { it.message }
        )
    }

    @Test
    fun types_string_with_transform__std_resolved() {
        val types = """
            namespace my.model
              data Disc { name: String }
        """.trimIndent()
        val transform = """
            namespace example
            asm-transform Shapes {
                #override-default-transform
                import-types my.model
                circle: Disc() { name := child[1] }
            }
        """.trimIndent()
        val result = Agl.processorFromStringSimple(GrammarString(GRAMMAR), typeStr = TypesString(types), transformStr = AsmTransformString(transform))
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        val proc = result.processor!!
        val processed = proc.process(SENTENCE)
        assertTrue(proc.allIssues.errors.isEmpty(), proc.allIssues.toString())
        assertTrue(processed.allIssues.errors.isEmpty(), processed.allIssues.toString())
        val expected = """
            :Unit {
              item = [
                :Disc {
                  name = 'c'
                }
                :Connection {
                  name = 'c'
                  name2 = 'c'
                }
              ]
            }
        """.trimIndent()
        assertEquals(expected, processed.asm!!.asString().trim())
    }
}
