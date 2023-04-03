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

package net.akehurst.language.agl.collections

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class test_BinaryHeapMax {

    lateinit var sut: BinaryHeap<Int, String>

    @BeforeTest
    fun before() {
        sut = binaryHeapMax()
    }

    private fun insert_10() {
        sut.insert(3, "C")
        sut.insert(4, "D")
        sut.insert(5, "E")
        sut.insert(8, "H")
        sut.insert(9, "I")
        sut.insert(10, "J1")
        sut.insert(11, "K")
        sut.insert(12, "L")
        sut.insert(13, "M")
        sut.insert(10, "J2")
    }

    @Test
    fun empty_size() {
        assertEquals(0, sut.size)
    }

    @Test
    fun empty_peekRoot() {
        assertNull(sut.peekRoot)
    }

    @Test
    fun empty_peekOneOf_notAKey() {
        assertNull(sut.peekOneOf(1))
    }

    @Test
    fun empty_peekAll_notAKey() {
        assertEquals(emptyList(), sut.peekAll(1))
    }

    @Test
    fun empty_extractRoot() {
        val actual = sut.extractRoot()

        assertNull(actual)
        assertEquals(0, sut.size)
    }

    @Test
    fun empty_insert() {
        sut.insert(1, "A")

        assertEquals(1, sut.size)
        assertEquals(listOf("A"), sut[1])
    }

    @Test
    fun empty_extractRootAndThenInsert() {
        val actual = sut.extractRootAndThenInsert(1, "A")

        assertNull(actual)
        assertEquals(1, sut.size)
        assertEquals(listOf("A"), sut[1])
    }

    @Test
    fun empty_insertAndThenExtractRoot() {
        val actual = sut.insertAndThenExtractRoot(1, "A")

        assertEquals(0, sut.size)
        assertEquals("A", actual)
    }

    @Test
    fun empty_iterator_toList() {
        assertEquals(emptyList(), sut.toList())
    }

    @Test
    fun empty_entries() {
        assertEquals(emptyList(), sut.entries)
    }

    @Test
    fun _1_element_size() {
        sut.insert(2, "B")

        assertEquals(1, sut.size)
    }

    @Test
    fun _1_element_peekRoot() {
        sut.insert(2, "B")

        assertEquals("B", sut.peekRoot)
    }

    @Test
    fun _1_element_peekOneOf_notAKey() {
        sut.insert(2, "B")

        assertNull(sut.peekOneOf(1))
    }

    @Test
    fun _1_element_peekOneOf_isAKey() {
        sut.insert(2, "B")

        assertEquals("B", sut.peekOneOf(2))
    }

    @Test
    fun _1_element_peekAll_notAKey() {
        sut.insert(2, "B")

        assertEquals(emptyList(), sut.peekAll(1))
    }

    @Test
    fun _1_element_peekAll_isAKey() {
        sut.insert(2, "B")

        assertEquals(listOf("B"), sut.peekAll(2))
    }

    @Test
    fun _1_element_extractRoot() {
        sut.insert(2, "B")

        val actual = sut.extractRoot()

        assertEquals("B", actual)
        assertEquals(0, sut.size)
        assertEquals(emptyList(), sut[2])
    }

    @Test
    fun _1_element_insert_smaller() {
        sut.insert(2, "B")

        sut.insert(1, "A")

        assertEquals(2, sut.size)
        assertEquals(listOf("A"), sut[1])
        assertEquals(listOf("B"), sut[2])
    }

    @Test
    fun _1_element_insert_same() {
        sut.insert(2, "B1")

        sut.insert(2, "B2")

        assertEquals(2, sut.size)
        assertEquals(listOf("B1", "B2"), sut[2])
    }

    @Test
    fun _1_element_insert_larger() {
        sut.insert(2, "B")

        sut.insert(3, "C")

        assertEquals(2, sut.size)
        assertEquals(listOf("C"), sut[3])
        assertEquals(listOf("B"), sut[2])
    }

    @Test
    fun _1_element_extractRootAndThenInsert_smaller() {
        sut.insert(2, "B")

        val actual = sut.extractRootAndThenInsert(1, "A")

        assertEquals("B", actual)
        assertEquals(1, sut.size)
        assertEquals(listOf("A"), sut[1])
        assertEquals(emptyList(), sut[2])
    }

    @Test
    fun _1_element_extractRootAndThenInsert_same() {
        sut.insert(2, "B1")

        val actual = sut.extractRootAndThenInsert(2, "B2")

        assertEquals("B1", actual)
        assertEquals(1, sut.size)
        assertEquals(listOf("B2"), sut[2])
    }

    @Test
    fun _1_element_extractRootAndThenInsert_larger() {
        sut.insert(2, "B")

        val actual = sut.extractRootAndThenInsert(3, "C")

        assertEquals("B", actual)
        assertEquals(1, sut.size)
        assertEquals(listOf("C"), sut[3])
        assertEquals(emptyList(), sut[2])
    }

    @Test
    fun _1_element_insertAndThenExtractRoot_smaller() {
        sut.insert(2, "B")

        val actual = sut.insertAndThenExtractRoot(1, "A")

        assertEquals("B", actual)
        assertEquals(1, sut.size)
        assertEquals(listOf("A"), sut[1])
        assertEquals(emptyList(), sut[2])
    }

    @Test
    fun _1_element_insertAndThenExtractRoot_same() {
        sut.insert(2, "B1")

        val actual = sut.insertAndThenExtractRoot(2, "B2")

        assertEquals("B1", actual)
        assertEquals(1, sut.size)
        assertEquals(listOf("B2"), sut[2])
        assertEquals(emptyList(), sut[1])
    }

    @Test
    fun _1_element_insertAndThenExtractRoot_larger() {
        sut.insert(2, "B")

        val actual = sut.insertAndThenExtractRoot(3, "C")

        assertEquals("B", actual)
        assertEquals(1, sut.size)
        assertEquals(listOf("C"), sut[3])
        assertEquals(emptyList(), sut[2])
    }

    @Test
    fun _1_element_iterator_toList() {
        sut.insert(2, "B")

        assertEquals(listOf("B"), sut.toList())
    }

    @Test
    fun _1_element_iterator_entries() {
        sut.insert(2, "B")

        assertEquals(listOf(2 to "B"), sut.entries)
    }


    @Test
    fun _10_element_size() {
        insert_10()

        assertEquals(10, sut.size)
    }

    @Test
    fun _10_element_peekRoot() {
        insert_10()

        assertEquals("C", sut.peekRoot)
    }

    @Test
    fun _10_element_peekOneOf_notAKey() {
        insert_10()

        assertNull(sut.peekOneOf(1))
    }

    @Test
    fun _10_element_peekOneOf_isAKey() {
        insert_10()

        assertEquals("J1", sut.peekOneOf(10))
    }

    @Test
    fun _10_element_peekAll_notAKey() {
        insert_10()

        assertEquals(emptyList(), sut.peekAll(1))
    }

    @Test
    fun _10_element_peekAll_isAKey() {
        insert_10()

        assertEquals(setOf("J1","J2"), sut.peekAll(10).toSet())
    }

    @Test
    fun _10_element_extractRoot() {
        insert_10()

        val actual = sut.extractRoot()

        assertEquals("C", actual)
        assertEquals(9, sut.size)
        assertEquals(emptyList(), sut[3])
    }

    @Test
    fun _10_element_insert_smaller() {
        insert_10()

        sut.insert(1, "A")

        assertEquals(11, sut.size)
        assertEquals(listOf("A"), sut[1])
        assertEquals(listOf("M"), sut[13])
        assertEquals("M", sut.peekRoot)
    }

    @Test
    fun _10_element_insert_same_as_smallest() {
        insert_10()

        sut.insert(3, "C2")

        assertEquals(11, sut.size)
        assertEquals(listOf("C", "C2"), sut[3])
        assertEquals("M",sut.peekRoot)
    }

    @Test
    fun _10_element_insert_mid() {
        insert_10()

        sut.insert(6, "F")

        assertEquals(11, sut.size)
        assertEquals(listOf("C"), sut[3])
        assertEquals(listOf("F"), sut[6])
        assertEquals("C",sut.peekRoot)
    }

    @Test
    fun _10_element_insert_mid_same_as() {
        insert_10()

        sut.insert(9, "I2")

        assertEquals(11, sut.size)
        assertEquals(listOf("C"), sut[3])
        assertEquals(setOf("I","I2"), sut[9].toSet())
        assertEquals("M",sut.peekRoot)
    }

    @Test
    fun _10_element_insert_same_as_largest() {
        insert_10()

        sut.insert(13, "M2")

        assertEquals(11, sut.size)
        assertEquals(listOf("C"), sut[3])
        assertEquals(listOf("M","M2"), sut[13])
        assertEquals("C",sut.peekRoot)
    }

    @Test
    fun _10_element_insert_largest() {
        insert_10()

        sut.insert(26, "Z")

        assertEquals(11, sut.size)
        assertEquals(listOf("C"), sut[3])
        assertEquals(listOf("Z"), sut[26])
        assertEquals("Z",sut.peekRoot)
    }

    @Test
    fun _10_element_extractRootAndThenInsert_smallest() {
        insert_10()

        val actual = sut.extractRootAndThenInsert(1, "A")

        assertEquals("M", actual)
        assertEquals(10, sut.size)
        assertEquals(listOf("A"), sut[1])
        assertEquals(setOf("C"), sut[3].toSet())
        assertEquals("L", sut.peekRoot)
    }

    @Test
    fun _10_element_extractRootAndThenInsert_same_as_smallest() {
        insert_10()

        val actual = sut.extractRootAndThenInsert(3, "C2")

        assertEquals("M", actual)
        assertEquals(10, sut.size)
        assertEquals(setOf("C","C2"), sut[3].toSet())
        assertEquals("L", sut.peekRoot)
    }

    @Test
    fun _10_element_extractRootAndThenInsert_mid() {
        insert_10()

        val actual = sut.extractRootAndThenInsert(7, "G")

        assertEquals("C", actual)
        assertEquals(10, sut.size)
        assertEquals(listOf("G"), sut[7])
        assertEquals(emptyList(), sut[3])
        assertEquals("D", sut.peekRoot)
    }

    @Test
    fun _10_element_extractRootAndThenInsert_same_as_mid() {
        insert_10()

        val actual = sut.extractRootAndThenInsert(9, "I2")

        assertEquals("C", actual)
        assertEquals(10, sut.size)
        assertEquals(listOf("I","I2"), sut[9])
        assertEquals(emptyList(), sut[3])
        assertEquals("D", sut.peekRoot)
    }

    @Test
    fun _10_element_extractRootAndThenInsert_same_as_largest() {
        insert_10()

        val actual = sut.extractRootAndThenInsert(13, "M2")

        assertEquals("C", actual)
        assertEquals(10, sut.size)
        assertEquals(listOf("M","M2"), sut[13])
        assertEquals(emptyList(), sut[3])
    }

    @Test
    fun _10_element_extractRootAndThenInsert_largest() {
        insert_10()

        val actual = sut.extractRootAndThenInsert(26, "Z")

        assertEquals("M", actual)
        assertEquals(10, sut.size)
        assertEquals(setOf("Z"), sut[26].toSet())
        assertEquals(emptySet(), sut[13].toSet())
    }

    @Test
    fun _10_element_insertAndThenExtractRoot_smallest() {
        insert_10()

        val actual = sut.insertAndThenExtractRoot(1, "A")

        assertEquals("A", actual)
        assertEquals(10, sut.size)
        assertEquals(listOf("C"), sut[3])
        assertEquals(emptyList(), sut[1])
        assertEquals("C", sut.peekRoot)
    }

    @Test
    fun _10_element_insertAndThenExtractRoot_same_as_smallest() {
        insert_10()

        val actual = sut.insertAndThenExtractRoot(3, "C2")

        assertEquals("M", actual)
        assertEquals(10, sut.size)
        assertEquals(setOf("C","C2"), sut[3].toSet())
        assertEquals("M", sut.peekRoot)
    }

    @Test
    fun _10_element_insertAndThenExtractRoot_mid() {
        insert_10()

        val actual = sut.insertAndThenExtractRoot(7, "G")

        assertEquals("M", actual)
        assertEquals(10, sut.size)
        assertEquals(listOf("G"), sut[7])
        assertEquals(emptyList(), sut[13])
        assertEquals("L", sut.peekRoot)
    }

    @Test
    fun _10_element_insertAndThenExtractRoot_same_as_mid() {
        insert_10()

        val actual = sut.insertAndThenExtractRoot(9, "I2")

        assertEquals("C", actual)
        assertEquals(10, sut.size)
        assertEquals(setOf("I","I2"), sut[9].toSet())
        assertEquals(emptyList(), sut[3])
        assertEquals("D", sut.peekRoot)
    }

    @Test
    fun _10_element_insertAndThenExtractRoot_same_as_largest() {
        insert_10()

        val actual = sut.insertAndThenExtractRoot(13, "M2")

        assertEquals("M", actual)
        assertEquals(10, sut.size)
        assertEquals(setOf("M2"), sut[13].toSet())
        assertEquals("M2", sut.peekRoot)
    }

    @Test
    fun _10_element_insertAndThenExtractRoot_largest() {
        insert_10()

        val actual = sut.insertAndThenExtractRoot(26, "Z")

        assertEquals("Z", actual)
        assertEquals(10, sut.size)
        assertEquals(listOf("M"), sut[13])
        assertEquals(emptyList(), sut[26])
        assertEquals("M", sut.peekRoot)
    }

    @Test
    fun _10_element_iterator_toList() {
        insert_10()

        assertEquals(listOf("M", "L", "K", "J1", "J2", "I", "H", "E", "D", "C"), sut.toList())
    }

    @Test
    fun _10_element_iterator_entries() {
        insert_10()

        assertEquals(
            listOf(
                13 to "M",
                12 to "L",
                10 to "J1",
                11 to "K",
                10 to "J2",
                4 to "D",
                9 to "I",
                3 to "C",
                8 to "H",
                5 to "E",
            ), sut.entries
        )
    }

    @Test
    fun tree_order() {
        sut.insert(2, "2")
        assertEquals("2", sut.peekRoot)
        assertEquals(
            listOf(
                2 to "2"
            ), sut.entries
        )

        sut.insert(6, "6")
        assertEquals("6", sut.peekRoot)
        assertEquals(
            listOf(
                6 to "6",
                2 to "2",
            ), sut.entries
        )

        sut.insert(4, "4")
        assertEquals("6", sut.peekRoot)
        assertEquals(
            listOf(
                6 to "6",
                2 to "2",
                4 to "4"
            ), sut.entries
        )

        sut.insert(5, "5")
        assertEquals("6", sut.peekRoot)
        assertEquals(
            listOf(
                6 to "6",
                5 to "5",
                4 to "4",
                2 to "2",
            ), sut.entries
        )
    }
}