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
import net.akehurst.language.api.processor.GrammarString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The examples in 5_Documentation/GETTING_STARTED.md.
 * The shared example language (Shapes) is used by the DSL pages too.
 */
class test_Doc_GettingStarted {

    companion object {
        // GETTING_STARTED.md: "The example language"
        val SHAPES_GRAMMAR = """
            namespace example
            grammar Shapes {
                skip leaf WS = "\s+" ;
                skip leaf COMMENT = "//[^\n]*" ;

                unit = item* ;
                item = shape | connection ;
                shape = circle | square ;
                circle = 'circle' NAME radius? ;
                radius = 'r' NUMBER ;
                square = 'square' NAME '{' [point / ',']* '}' ;
                point = NUMBER NUMBER ;
                connection = 'connect' source 'to' target ;

                leaf source = NAME ;
                leaf target = NAME ;
                leaf NAME = "[a-z]+" ;
                leaf NUMBER = "[0-9]+" ;
            }
        """.trimIndent()

        val SHAPES_SENTENCE = """
            circle c r 5
            square s { 1 2, 3 4 }  // a comment
            connect c to s
        """.trimIndent()

        val shapesProcessor by lazy {
            val result = Agl.processorFromStringSimple(GrammarString(SHAPES_GRAMMAR))
            check(result.issues.errors.isEmpty()) { result.issues.toString() }
            result.processor!!
        }
    }

    @Test
    fun create_parse_process() {
        // GETTING_STARTED.md: "Create a processor"
        val result = Agl.processorFromStringSimple(GrammarString(SHAPES_GRAMMAR))
        check(result.issues.errors.isEmpty()) { result.issues.toString() }
        val processor = result.processor!!

        // GETTING_STARTED.md: "Parse"
        val parseResult = processor.parse(SHAPES_SENTENCE)
        assertTrue(parseResult.issues.errors.isEmpty(), parseResult.issues.toString())
        assertNotNull(parseResult.sppt)

        // GETTING_STARTED.md: "Process"
        val processResult = processor.process(SHAPES_SENTENCE)
        assertTrue(processResult.allIssues.errors.isEmpty(), processResult.allIssues.toString())
        val expected = """
            :Unit {
              item = [
                :Circle {
                  name = 'c'
                  radius = :Radius {
                    number = '5'
                  }
                }
                :Square {
                  name = 's'
                  point = [
                    :Point {
                      number = '1'
                      number2 = '2'
                    }
                    :Point {
                      number = '3'
                      number2 = '4'
                    }
                  ]
                }
                :Connection {
                  source = 'c'
                  target = 's'
                }
              ]
            }
        """.trimIndent()
        assertEquals(expected, processResult.asm!!.asString().trim())
    }

    @Test
    fun errors() {
        // GETTING_STARTED.md: "Errors"
        val result = shapesProcessor.parse("circle 5")
        val error = result.issues.errors.first()
        assertEquals("Failed to match {<GOAL>} at: circle ^5", error.message)
        assertEquals(1, error.location?.line)
        assertEquals(8, error.location?.column)
    }

    @Test
    fun default_types() {
        // GETTING_STARTED.md: "What the default ASM looks like" (types generated from the grammar)
        val expected = """
            namespace example.Shapes
              import std
              data Circle : Shape {
                cmp str name: String
                cmp str radius: Radius?
              }
              data Connection : Item {
                cmp str source: String
                cmp str target: String
              }
              data Item {
                
              }
              data Point {
                cmp str number: String
                cmp str number2: String
              }
              data Radius {
                cmp str number: String
              }
              data Shape : Item {
                
              }
              data Square : Shape {
                cmp str name: String
                cmp str point: List<Point>
              }
              data Unit {
                cmp str item: List<Item>
              }
        """.trimIndent()
        assertEquals(expected, shapesProcessor.typesDomain.asString().trim())
    }
}
