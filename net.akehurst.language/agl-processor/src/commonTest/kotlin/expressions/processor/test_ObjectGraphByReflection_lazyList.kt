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

package net.akehurst.language.expressions.processor

import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.objectgraph.api.TypedObject
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** a Kotlin List given to toTypedObject is typed lazily, see LazyTypedList */
class test_ObjectGraphByReflection_lazyList {

    private fun og() = ObjectGraphAccessorMutatorByReflection(typesDomain("Test", true) {}, IssueHolder(), LocationMapDefault())

    @Test
    fun elements_are_TypedObjects_typed_once() {
        val og = og()
        val list = og.toTypedObject(listOf("a", "b"), StdLibDefault.AnyType)
        assertEquals(StdLibDefault.List.qualifiedName, list.type.qualifiedTypeName)
        val self = list.self as List<*>
        assertTrue(self.all { it is TypedObject })
        assertSame(self[1], self[1])
        assertEquals("b", og.getFromListWithIndex(list, 1).self)
    }

    @Test
    fun element_type_from_ifNotFound() {
        val og = og()
        val list = og.toTypedObject(listOf("a"), StdLibDefault.List.type(listOf(StdLibDefault.String.asTypeArgument)))
        assertEquals(StdLibDefault.String, list.type.typeArguments[0].type)
    }

    @Test
    fun untyped_normalises_primitives_as_toTypedObject_does() {
        val og = og()
        val raw = listOf<Any?>(1, 2.toShort(), 3.toByte(), 4L, 1.5f, 2.5, 'c', "s", true, null, listOf(5))
        val expected = listOf<Any>(1L, 2L, 3L, 4L, 1.5, 2.5, "c", "s", true, Unit, listOf(5L))
        // none read
        assertEquals(expected, og.untyped(og.toTypedObject(raw, StdLibDefault.AnyType)))
        // some read
        val list = og.toTypedObject(raw, StdLibDefault.AnyType)
        og.getFromListWithIndex(list, 0)
        og.getFromListWithIndex(list, 6)
        assertEquals(expected, og.untyped(list))
    }

    @Test
    fun later_change_to_the_original_list_is_not_seen() {
        val og = og()
        val ml = mutableListOf("a")
        val list = og.toTypedObject(ml, StdLibDefault.AnyType)
        ml.add("b")
        assertEquals(listOf("a"), og.untyped(list))
    }
}
