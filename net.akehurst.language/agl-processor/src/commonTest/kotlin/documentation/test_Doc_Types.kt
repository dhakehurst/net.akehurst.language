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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The examples in 5_Documentation/dsl/types.md.
 */
class test_Doc_Types {

    @Test
    fun types() {
        // types.md: "Example"
        val types = """
            namespace shapes.model
              data Model { cmp items: List<Item> }
              interface Item
              data Circle : Item { name: String  radius: Integer? }
              data Square : Item { name: String }
              data Link : Item { ref from: Item  ref to: Item }
              enum Colour { RED, GREEN }
              value Size(val n: Integer)
              union Number { Integer | Real }
              data Pair2<A,B> { a: A  b: B }
        """.trimIndent()
        val result = Agl.registry.agl.types.processor!!.process(types)
        assertTrue(result.allIssues.errors.isEmpty(), result.allIssues.toString())
        val typesDomain = result.asm!!
        val expected = """
            namespace shapes.model
              
              data Circle : Item {
                str name: String
                str radius: Integer?
              }
              data Link : Item {
                ref str from: Item
                ref str to: Item
              }
              data Model {
                cmp str items: List<Item>
              }
              data Pair2<A,B> {
                str a: A
                str b: B
              }
              data Square : Item {
                str name: String
              }
              enum Colour { RED, GREEN }
              interface Item {
                
              }
              union Number { Integer | Real }
              value Size ( val str n:Integer )
        """.trimIndent()
        assertEquals(expected, typesDomain.asString().trim())
    }

    @Test
    fun generated_types() {
        // types.md: "Types generated from a grammar"
        val typesDomain = test_Doc_GettingStarted.shapesProcessor.typesDomain
        val circle = typesDomain.findFirstDefinitionByPossiblyQualifiedNameOrNull(
            net.akehurst.language.base.api.QualifiedName("example.Shapes.Circle")
        )
        assertEquals("Circle", circle?.name?.value)
    }
}
