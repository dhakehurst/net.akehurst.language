/**
 * Copyright (C) 2021 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.asm.simple

import net.akehurst.language.asm.api.AsmPrimitive
import net.akehurst.language.asm.builder.asmSimple
import kotlin.test.Test
import kotlin.test.assertEquals

class test_asmSimple {

    @Test
    fun empty() {
        val asm = asmSimple {

        }
        assertEquals(0, asm.root.size)
    }

    @Test
    fun root_string() {
        val asm = asmSimple {
            string("Hello")
        }
        assertEquals(1, asm.root.size)
        assertEquals("Hello", (asm.root[0] as AsmPrimitive).value as String)
    }

    //TODO: more tests

}