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

package net.akehurst.language.documentation

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.AsmTransformString
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.documentation.test_Doc_GettingStarted.Companion.SHAPES_GRAMMAR
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The examples in 5_Documentation/dsl/asm-transform.md.
 */
class test_Doc_AsmTransform {

    private companion object {
        const val SENTENCE = "circle c r 5 connect c to c"

        fun asm(transform: String): String {
            val result = Agl.processorFromStringSimple(GrammarString(SHAPES_GRAMMAR), transformStr = AsmTransformString(transform.trimIndent()))
            check(result.issues.errors.isEmpty()) { result.issues.toString() }
            val processed = result.processor!!.process(SENTENCE)
            assertTrue(processed.allIssues.errors.isEmpty(), processed.allIssues.toString())
            return processed.asm!!.asString().trim()
        }
    }

    @Test
    fun replace_rules() {
        // asm-transform.md: "Example"
        val transform = """
            #override-default-transform
            #create-missing-types
            namespace example
            asm-transform Shapes {
                connection: Link() {
                    from := child[1]
                    to := child[3]
                }
                radius: child[1] as String
            }
        """
        val expected = """
            :Unit {
              item = [
                :Circle {
                  name = 'c'
                  radius = '5'
                }
                :Link {
                  from = 'c'
                  to = 'c'
                }
              ]
            }
        """.trimIndent()
        assertEquals(expected, asm(transform))
    }

    @Test
    fun change_type_only() {
        // asm-transform.md: "Change only the type"
        val transform = """
            #override-default-transform
            #create-missing-types
            namespace example
            asm-transform Shapes {
                circle: Disc
            }
        """
        val expected = """
            :Unit {
              item = [
                :Disc {
                  name = 'c'
                  radius = :Radius {
                    number = '5'
                  }
                }
                :Connection {
                  source = 'c'
                  target = 'c'
                }
              ]
            }
        """.trimIndent()
        assertEquals(expected, asm(transform))
    }

    @Test
    fun existing_type_without_create_missing_types() {
        // asm-transform.md: "Options" (a rule that uses an existing type needs no '#create-missing-types')
        val transform = """
            #override-default-transform
            namespace example
            asm-transform Shapes {
                connection: Connection() {
                    source := child[3]
                    target := child[1]
                }
            }
        """
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
                  source = 'c'
                  target = 'c'
                }
              ]
            }
        """.trimIndent()
        assertEquals(expected, asm(transform))
    }
}
