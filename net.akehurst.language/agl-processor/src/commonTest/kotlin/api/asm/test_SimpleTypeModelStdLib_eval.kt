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

package net.akehurst.language.api.asm

import kotlin.test.Test
import kotlin.test.assertEquals

class test_SimpleTypeModelStdLib_eval {

    @Test
    fun collection_List_size__1() {
        val asm = asmSimple {

        }
        assertEquals(0, asm.rootElements.size)

        val root = asm.rootElements[0] as AsmElementSimple

        assertEquals(1, root.evaluate("list.size") )
    }

    @Test
    fun collection_List_first__a() {
        val asm = asmSimple {

        }
        assertEquals(0, asm.rootElements.size)

        val root = asm.rootElements[0] as AsmElementSimple

        assertEquals("a", root.evaluate("list.first") )
    }
}