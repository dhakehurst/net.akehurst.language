/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.language.expressions

import net.akehurst.language.api.asm.AsmElementSimple
import net.akehurst.language.api.asm.asmSimple
import kotlin.test.Test
import kotlin.test.assertEquals

class test_ExpressionsInterpreter {

    @Test
    fun evaluateStr__list() {

        val asm = asmSimple {
            element("Test") {
                propertyListOfString("list", listOf("A", "B", "C", "D"))
            }
        }
        val root = asm.rootElements[0] as AsmElementSimple

        val expression = "list"

        val actual = root.evaluateStr(expression)

        assertEquals(listOf("A", "B", "C", "D"), actual)
    }

    @Test
    fun evaluateStr__list_front() {

        val asm = asmSimple {
            element("Test") {
                propertyListOfString("list", listOf("A", "B", "C", "D"))
            }
        }
        val root = asm.rootElements[0] as AsmElementSimple

        val expression = "list.front"

        val actual = root.evaluateStr(expression)

        assertEquals(listOf("A", "B", "C"), actual)
    }

    @Test
    fun evaluateStr__list_front_join() {

        val asm = asmSimple {
            element("Test") {
                propertyListOfString("list", listOf("A", "B", "C", "D"))
            }
        }
        val root = asm.rootElements[0] as AsmElementSimple

        val expression = "list.front.join"

        val actual = root.evaluateStr(expression)

        assertEquals("ABC", actual)
    }

}