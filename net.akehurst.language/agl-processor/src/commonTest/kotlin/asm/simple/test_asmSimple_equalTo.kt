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

import net.akehurst.language.asm.api.PropertyValueName
import net.akehurst.language.base.api.asQualifiedName
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * equalTo is the same as equals (identity for a structure); equalToDeep compares values (David 2026-11-27).
 */
class test_asmSimple_equalTo {

    private fun point(x: Long, y: Long) = AsmStructureSimple("test.Point".asQualifiedName).also {
        it.setProperty(PropertyValueName("x"), x, 0)
        it.setProperty(PropertyValueName("y"), y, 1)
    }

    private fun square(name: String, vararg points: AsmStructureSimple) = AsmStructureSimple("test.Square".asQualifiedName).also {
        it.setProperty(PropertyValueName("name"), name, 0)
        it.setProperty(PropertyValueName("point"), points.toList(), 1)
    }

    @Test
    fun equalTo_is_identity_for_structures() {
        val a = point(1, 2)
        val b = point(1, 2)
        assertTrue(a.equalTo(a))
        assertFalse(a.equalTo(b))
        assertTrue(a.equalToDeep(b))
    }

    @Test
    fun equalToDeep_follows_lists_of_structures() {
        assertTrue(square("s", point(1, 2), point(3, 4)).equalToDeep(square("s", point(1, 2), point(3, 4))))
        assertFalse(square("s", point(1, 2), point(3, 4)).equalToDeep(square("s", point(1, 2), point(3, 5))))
        assertFalse(square("s", point(1, 2), point(3, 4)).equalToDeep(square("s", point(3, 4), point(1, 2))))
        assertFalse(square("s", point(1, 2)).equalToDeep(square("s", point(1, 2), point(3, 4))))
    }

    @Test
    fun equalToDeep_needs_same_type_and_property_names() {
        val p = point(1, 2)
        val q = AsmStructureSimple("test.Other".asQualifiedName).also {
            it.setProperty(PropertyValueName("x"), 1L, 0)
            it.setProperty(PropertyValueName("y"), 2L, 1)
        }
        val r = AsmStructureSimple("test.Point".asQualifiedName).also {
            it.setProperty(PropertyValueName("x"), 1L, 0)
        }
        assertFalse(p.equalToDeep(q))
        assertFalse(p.equalToDeep(r))
    }

    @Test
    fun equalToDeep_ignores_semantic_identity() {
        val a = point(1, 2).also { it.semanticQualifiedPath = listOf("a") }
        val b = point(3, 4).also { it.semanticQualifiedPath = listOf("a") }
        val c = point(1, 2).also { it.semanticQualifiedPath = listOf("c") }
        assertFalse(a.equalToDeep(b))
        assertTrue(a.equalToDeep(c))
    }

    @Test
    fun equalToDeep_compares_references_by_text_only() {
        val t1 = point(1, 2)
        val t2 = point(9, 9)
        assertTrue(AsmReferenceSimple("x", t1).equalToDeep(AsmReferenceSimple("x", t2)))
        assertFalse(AsmReferenceSimple("x", t1).equalToDeep(AsmReferenceSimple("y", t1)))
    }

    @Test
    fun equalToDeep_wrapped_primitive_equals_unwrapped_value() {
        val a = point(1, 2)
        val b = AsmStructureSimple("test.Point".asQualifiedName).also {
            it.setProperty(PropertyValueName("x"), AsmPrimitiveSimple.stdInteger(1), 0)
            it.setProperty(PropertyValueName("y"), 2L, 1)
        }
        assertTrue(a.equalToDeep(b))
    }

    @Test
    fun asmSet_equals_asmSet() {
        assertTrue(AsmSetSimple(setOf(AsmPrimitiveSimple.stdString("a"))) == AsmSetSimple(setOf(AsmPrimitiveSimple.stdString("a"))))
    }
}
