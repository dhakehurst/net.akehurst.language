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

class test_SimpleTypeModelStdLib_eval {

    @Test
    fun collection_List_size__empty() {
        val asm = asmSimple {
            element("Test") {
                propertyListOfString("list", listOf())
            }
        }
        assertEquals(1, asm.rootElements.size)

        val root = asm.rootElements[0] as AsmElementSimple

        assertEquals(0, root.evaluateStr("list.size"))
    }

    @Test
    fun collection_List_size() {
        val asm = asmSimple {
            element("Test") {
                propertyListOfString("list", listOf("A", "B", "C", "D"))
            }
        }
        assertEquals(1, asm.rootElements.size)

        val root = asm.rootElements[0] as AsmElementSimple

        assertEquals(4, root.evaluateStr("list.size"))
    }

    @Test
    fun collection_List_size__missing_prop_name() {
        val asm = asmSimple {
            element("Test") {
                propertyListOfString("list", listOf())
            }
        }
        assertEquals(1, asm.rootElements.size)

        val root = asm.rootElements[0] as AsmElementSimple

        assertEquals(null, root.evaluateStr("list2.size"))
    }

    @Test
    fun collection_List_first() {
        val asm = asmSimple {
            element("Test") {
                propertyListOfString("list", listOf("A", "B", "C", "D"))
            }
        }
        assertEquals(1, asm.rootElements.size)

        val root = asm.rootElements[0] as AsmElementSimple

        assertEquals("A", root.evaluateStr("list.first"))
    }

    @Test
    fun collection_List_last() {
        val asm = asmSimple {
            element("Test") {
                propertyListOfString("list", listOf("A", "B", "C", "D"))
            }
        }
        assertEquals(1, asm.rootElements.size)

        val root = asm.rootElements[0] as AsmElementSimple

        assertEquals("D", root.evaluateStr("list.last"))
    }

    @Test
    fun collection_List_back() {
        val asm = asmSimple {
            element("Test") {
                propertyListOfString("list", listOf("A", "B", "C", "D"))
            }
        }
        assertEquals(1, asm.rootElements.size)

        val root = asm.rootElements[0] as AsmElementSimple

        assertEquals(listOf("B", "C", "D"), root.evaluateStr("list.back"))
    }

    @Test
    fun collection_List_front() {
        val asm = asmSimple {
            element("Test") {
                propertyListOfString("list", listOf("A", "B", "C", "D"))
            }
        }
        assertEquals(1, asm.rootElements.size)

        val root = asm.rootElements[0] as AsmElementSimple

        assertEquals(listOf("A", "B", "C"), root.evaluateStr("list.front"))
    }

    @Test
    fun collection_List_join() {
        val asm = asmSimple {
            element("Test") {
                propertyListOfString("list", listOf("A", "B", "C", "D"))
            }
        }
        assertEquals(1, asm.rootElements.size)

        val root = asm.rootElements[0] as AsmElementSimple

        assertEquals("ABCD", root.evaluateStr("list.join"))
    }
}