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

package net.akehurst.language.types

import net.akehurst.language.types.api.PropertyName
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.asm.TypeArgumentNamedSimple
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class test_TupleTypeInstance {

    private fun tuple(vararg args: Pair<String, net.akehurst.language.types.api.TypeInstance>, nullable: Boolean = false) =
        StdLibDefault.createTupleTypeInstance(args.map { TypeArgumentNamedSimple(PropertyName(it.first), it.second) }, nullable)

    @Test
    fun equals_itself() {
        val t = tuple("a" to StdLibDefault.String, "b" to StdLibDefault.Integer)
        assertEquals(t, t)
    }

    @Test
    fun equals_same_names_and_types_in_any_order() {
        val t1 = tuple("a" to StdLibDefault.String, "b" to StdLibDefault.Integer)
        val t2 = tuple("b" to StdLibDefault.Integer, "a" to StdLibDefault.String)
        assertEquals(t1, t2)
        assertEquals(t1.hashCode(), t2.hashCode())
        assertEquals(tuple(), tuple())
    }

    @Test
    fun not_equal() {
        val t = tuple("a" to StdLibDefault.String)
        assertNotEquals(t, tuple("b" to StdLibDefault.String))
        assertNotEquals(t, tuple("a" to StdLibDefault.Integer))
        assertNotEquals(t, tuple("a" to StdLibDefault.String, nullable = true))
        assertNotEquals(t, tuple("a" to StdLibDefault.String, "b" to StdLibDefault.String))
    }
}
