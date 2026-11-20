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

import net.akehurst.kotlinx.collections.ListSeparated
import net.akehurst.kotlinx.collections.toSeparatedList
import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.objectgraph.api.TypedObject
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class test_ObjectGraphByReflection_toTypedObject {

    @Test
    fun listSeparated_keeps_repeated_elements() {
        val og = ObjectGraphAccessorMutatorByReflection(typesDomain("Test", true) {}, IssueHolder(), LocationMapDefault())
        val sl = listOf("a", ",", "a", ",", "b").toSeparatedList<String, String, String>()

        val actual = og.toTypedObject(sl, StdLibDefault.AnyType).self

        assertTrue(actual is ListSeparated<*, *, *>, "was ${actual::class.simpleName}")
        // the elements are typed objects
        fun raw(l: List<*>) = l.map { (it as? TypedObject)?.self ?: it }
        assertEquals(listOf("a", ",", "a", ",", "b"), raw(actual.elements))
        assertEquals(listOf("a", "a", "b"), raw(actual.items))
        assertEquals(listOf(",", ","), raw(actual.separators))
    }
}
