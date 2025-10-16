/*
 * Copyright (C) 2025 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.m2mTransform.processor

import net.akehurst.language.m2mTransform.processor.M2mTransformInterpreter.Companion.cartesianProduct
import kotlin.test.Test
import kotlin.test.assertEquals


class test_m2mTransformInterpreterHelperFunctions {

    @Test
    fun cartesianProduct_empty() {
        val actual = emptyList<List<Any>>().cartesianProduct().toSet()
        val expected = emptySet<Any>()
        assertEquals(expected, actual)
    }

    @Test
    fun cartesianProduct_one_empty_content() {
        val actual = listOf(
            emptyList<Any>()
        ).cartesianProduct().toSet()
        val expected = emptySet<Any>()
        assertEquals(expected, actual)
    }

    @Test
    fun cartesianProduct_one_content() {
        val actual = listOf(
            listOf("A")
        ).cartesianProduct().toSet()
        val expected = setOf(listOf("A"))
        assertEquals(expected, actual)
    }

    @Test
    fun cartesianProduct_2_content_one_empty() {
        val actual = listOf(
            listOf("A"),
            emptyList()
        ).cartesianProduct().toSet()
        val expected = emptySet<Any>()
        assertEquals(expected, actual)
    }

    @Test
    fun cartesianProduct_2x1_content() {
        val actual = listOf(
            listOf("A"),
            listOf(1),
        ).cartesianProduct().toSet()
        val expected = setOf(listOf("A", 1))
        assertEquals(expected, actual)
    }

    @Test
    fun cartesianProduct_3x1_content() {
        val actual = listOf(
            listOf("A"),
            listOf(1),
            listOf("x"),
        ).cartesianProduct().toSet()
        val expected = setOf(listOf("A", 1, "x"))
        assertEquals(expected, actual)
    }

    @Test
    fun cartesianProduct_3x2_content() {
        val actual = listOf(
            listOf("A", "B"),
            listOf(1, 2),
            listOf("x", "y"),
        ).cartesianProduct().toSet()
        val expected = setOf(
            listOf("A", 1, "x"),
            listOf("B", 1, "x"),
            listOf("A", 2, "x"),
            listOf("B", 2, "x"),
            listOf("A", 1, "y"),
            listOf("A", 2, "y"),
            listOf("B", 1, "y"),
            listOf("B", 2, "y"),
        )
        assertEquals(expected, actual)
    }

    @Test
    fun cartesianProduct_3x2x3x1_content() {
        val actual = listOf(
            listOf("A", "B"),
            listOf(1, 2, 3),
            listOf("x"),
        ).cartesianProduct().toSet()
        val expected = setOf(
            listOf("A", 1, "x"),
            listOf("B", 1, "x"),
            listOf("A", 2, "x"),
            listOf("B", 2, "x"),
            listOf("A", 3, "x"),
            listOf("B", 3, "x"),
            listOf("A", 3, "x"),
            listOf("B", 3, "x"),
        )
        assertEquals(expected, actual)
    }

    @Test
    fun findCoveringSubsets_1() {
        val map = mapOf(
            1 to listOf("a", "b"),
            2 to listOf("z"),
            3 to listOf("c")
        )
        val cover = listOf(1, 3)
        val subsets = listOf("a", "b", "c")

        val actual = M2mTransformInterpreter.findCoveringSubsets2(cover, subsets) { a, b ->
            Pair(map[a]!!.contains(b), "$a$b")
        }
        println(actual)
        assertEquals(listOf(listOf("1a", "3c"), listOf("1b", "3c")), actual)
    }

    @Test
    fun findCoveringSubsets_2() {
        val map = mapOf(
            1 to listOf("a", "b"),
            2 to listOf("b", "a"),
            3 to listOf("c")
        )
        val cover = listOf(1, 2, 3)
        val subsets = listOf("a", "b", "c")

        val actual = M2mTransformInterpreter.findCoveringSubsets2(cover, subsets) { a, b ->
            Pair(map[a]!!.contains(b), "$a$b")
        }
        println(actual)
        val expected = setOf(
            listOf("1a", "2a", "3c"),
            listOf("1a", "2b", "3c"),
            listOf("1b", "2a", "3c"),
            listOf("1b", "2b", "3c")
        )
        assertEquals(expected, actual.toSet())
    }
}