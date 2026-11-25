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

package net.akehurst.language.asmTransform.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.AsmTransformString
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.types.api.DataType
import net.akehurst.language.types.api.PropertyName
import net.akehurst.language.types.api.UnionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A type-only rule 'r: T' (with '#override-default-transform') keeps the default rule of 'r' and renames its type to 'T' (AT-Q2 B).
 */
class test_AsmTransformSemanticAnalyser_typeOnlyRule {

    private companion object {
        val GRAMMAR = """
            namespace example
            grammar Shapes {
                skip leaf WS = "\s+" ;
                unit = item* ;
                item = circle | square ;
                circle = 'circle' ID ;
                square = 'square' ID ;
                colour = 'red' | 'blue' ;
                leaf ID = "[a-z]+" ;
            }
        """.trimIndent()

        fun processor(transform: String, grammar: String = GRAMMAR) =
            Agl.processorFromStringSimple(GrammarString(grammar), transformStr = AsmTransformString(transform.trimIndent())).processor!!

        fun transformErrors(transform: String): List<String> {
            val proc = processor(transform)
            val res = proc.process("circle c")
            return res.allIssues.errors.filter { it.message.startsWith("In the transform definition") }
                .map { it.message.removePrefix("In the transform definition of the language: ") }
        }

        fun type(proc: net.akehurst.language.api.processor.LanguageProcessor<*, *>, qn: String) =
            proc.typesDomain.findByQualifiedNameOrNull(QualifiedName(qn))
    }

    @Test
    fun choice_of_data_types__renames_the_supertype() {
        val proc = processor(
            """
            #override-default-transform
            namespace example
            asm-transform Shapes {
                item: Thing
            }
            """
        )
        val res = proc.process("circle c square s")
        assertTrue(res.allIssues.errors.isEmpty(), res.allIssues.toString())
        val expected = """
            :Unit {
              item = [
                :Circle {
                  id = 'c'
                }
                :Square {
                  id = 's'
                }
              ]
            }
        """.trimIndent()
        assertEquals(expected, res.asm!!.asString().trim())

        val thing = type(proc, "example.Shapes.Thing")
        assertTrue(thing is DataType, "Thing: $thing")
        assertEquals(null, type(proc, "example.Shapes.Item"))
        val circle = type(proc, "example.Shapes.Circle") as DataType
        assertEquals(listOf("Thing"), circle.supertypes.map { it.typeName.value })
        assertEquals(setOf("Circle", "Square"), thing.subtypes.map { it.typeName.value }.toSet())
        val unit = type(proc, "example.Shapes.Unit") as DataType
        assertEquals("Thing", unit.findAllPropertyOrNull(PropertyName("item"))!!.typeInstance.typeArguments[0].type.typeName.value)
    }

    @Test
    fun create_object__renames_the_type_where_it_is_used() {
        val proc = processor(
            """
            #override-default-transform
            namespace example
            asm-transform Shapes {
                circle: Disc
            }
            """
        )
        val res = proc.process("circle c square s")
        assertTrue(res.allIssues.errors.isEmpty(), res.allIssues.toString())
        val expected = """
            :Unit {
              item = [
                :Disc {
                  id = 'c'
                }
                :Square {
                  id = 's'
                }
              ]
            }
        """.trimIndent()
        assertEquals(expected, res.asm!!.asString().trim())
        val disc = type(proc, "example.Shapes.Disc") as DataType
        assertEquals(listOf("Item"), disc.supertypes.map { it.typeName.value })
        assertEquals(null, type(proc, "example.Shapes.Circle"))
    }

    @Test
    fun choice_with_union_type__renames_the_union() {
        val grammar = """
            namespace example
            grammar Base {
                skip leaf WS = "\s+" ;
                unit = item* ;
                item = circle | square ;
                circle = 'circle' ID ;
                square = 'square' ID ;
                leaf ID = "[a-z]+" ;
            }
            grammar Shapes : Base {
                override unit = thing* ;
                thing = circle | triangle ;
                triangle = 'triangle' ID ;
            }
        """.trimIndent()
        val proc = processor(
            """
            #override-default-transform
            namespace example
            asm-transform Shapes : Base {
                thing: Thing
            }
            """, grammar
        )
        val res = proc.process("circle c triangle t")
        assertTrue(res.allIssues.errors.isEmpty(), res.allIssues.toString())
        val thing = type(proc, "example.Shapes.Thing")
        assertTrue(thing is UnionType, "Thing: $thing")
        assertEquals(setOf("Circle", "Triangle"), thing.alternatives.map { it.typeName.value }.toSet())
    }

    @Test
    fun leaf__error() {
        assertEquals(
            listOf("Cannot change only the type of rule 'ID' to 'Name', its default type is 'std.String', which is not a type of its own, use 'child[0] as Name' or 'Name() { ... }'"),
            transformErrors(
                """
                #override-default-transform
                namespace example
                asm-transform Shapes {
                    ID: Name
                }
                """
            )
        )
    }

    @Test
    fun choice_of_literals__error() {
        assertEquals(
            listOf("Cannot change only the type of rule 'colour' to 'Colour2', its default type is 'std.String', which is not a type of its own, use 'child[0] as Colour2' or 'Colour2() { ... }'"),
            transformErrors(
                """
                #override-default-transform
                namespace example
                asm-transform Shapes {
                    colour: Colour2
                }
                """
            )
        )
    }

    @Test
    fun name_of_another_rules_type__error() {
        assertEquals(
            listOf("Cannot change the type of rule 'item' to 'Circle', it is the type of rule 'circle'"),
            transformErrors(
                """
                #override-default-transform
                namespace example
                asm-transform Shapes {
                    item: Circle
                }
                """
            )
        )
    }
}
