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

class test_BinaryHeapFifoMin {

    lateinit var sut: BinaryHeapFifo<Int, String>

    @BeforeTest
    fun before() {
        sut = binaryHeapFifoMin()
    }

    private fun insert_TestSet() {
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
    fun empty_peek_notAKey() {
        assertNull(sut.peek(1))
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

    /*
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
    */

    @Test
    fun empty_iterator_toList() {
        assertEquals(emptyList(), sut.toList())
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
    fun _1_element_peek_notAKey() {
        sut.insert(2, "B")

        assertNull(sut.peek(1))
    }

    @Test
    fun _1_element_peek_isAKey() {
        sut.insert(2, "B")

        assertEquals("B", sut.peek(2))
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
    fun _1_element_insert_smallerx2_then_extractRoot() {
        sut.insert(2, "B")
        sut.insert(1, "A1")
        sut.insert(1, "A2")

        val actual = sut.extractRoot()

        assertEquals("A1", actual)
        assertEquals(2, sut.size)
        assertEquals(listOf("A2"), sut[1])
        assertEquals(listOf("B"), sut[2])
    }

    @Test
    fun _1_element_insert_smallerx2_then_extractRootx2() {
        sut.insert(2, "B")
        sut.insert(1, "A1")
        sut.insert(1, "A2")

        val actual1 = sut.extractRoot()
        val actual2 = sut.extractRoot()

        assertEquals("A1", actual1)
        assertEquals("A2", actual2)
        assertEquals(1, sut.size)
        assertEquals(emptyList(), sut[1])
        assertEquals(listOf("B"), sut[2])
    }

    @Test
    fun _1_element_insert_samex2() {
        sut.insert(2, "B1")

        sut.insert(2, "B2")
        sut.insert(2, "B3")

        assertEquals(3, sut.size)
        assertEquals(listOf("B1","B2","B3"), sut[2])
    }

    @Test
    fun _1_element_insert_samex2_then_extractRoot() {
        sut.insert(2, "B1")
        sut.insert(2, "B2")
        sut.insert(2, "B3")

        val actual = sut.extractRoot()

        assertEquals("B1", actual)
        assertEquals(2, sut.size)
        assertEquals(listOf("B2","B3"), sut[2])
    }

    @Test
    fun _1_element_insert_samex2_then_extractRootx2() {
        sut.insert(2, "B1")
        sut.insert(2, "B2")
        sut.insert(2, "B3")

        val actual1 = sut.extractRoot()
        val actual2 = sut.extractRoot()

        assertEquals("B1", actual1)
        assertEquals("B2", actual2)
        assertEquals(1, sut.size)
        assertEquals(listOf("B3"), sut[2])
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
    fun _1_element_insert_largerx2_then_extractRoot() {
        sut.insert(2, "B")
        sut.insert(3, "C1")
        sut.insert(3, "C2")

        val actual = sut.extractRoot()

        assertEquals("B", actual)
        assertEquals(2, sut.size)
        assertEquals(listOf("C1","C2"), sut[3])
        assertEquals(emptyList(), sut[2])
    }

    @Test
    fun _1_element_insert_largerx2_then_extractRootx2() {
        sut.insert(2, "B")
        sut.insert(3, "C1")
        sut.insert(3, "C2")

        val actual1 = sut.extractRoot()
        val actual2 = sut.extractRoot()

        assertEquals("B", actual1)
        assertEquals("C1", actual2)
        assertEquals(1, sut.size)
        assertEquals(listOf("C2"), sut[3])
        assertEquals(emptyList(), sut[2])
    }

    /*
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

        assertEquals("A", actual)
        assertEquals(1, sut.size)
        assertEquals(listOf("B"), sut[2])
        assertEquals(emptyList(), sut[1])
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
    */

    @Test
    fun _1_element_iterator_toList() {
        sut.insert(2, "B")

        assertEquals(listOf("B"), sut.toList())
    }

    @Test
    fun _10_element_size() {
        insert_TestSet()

        assertEquals(10, sut.size)
    }

    @Test
    fun _10_element_peekRoot() {
        insert_TestSet()

        assertEquals("C", sut.peekRoot)
    }

    @Test
    fun _10_element_peek_notAKey() {
        insert_TestSet()

        assertNull(sut.peek(1))
    }

    @Test
    fun _10_element_peek_isAKey() {
        insert_TestSet()

        assertEquals("J1",sut.peek(10))
    }

    @Test
    fun _10_element_peekAll_notAKey() {
        insert_TestSet()

        assertEquals(emptyList(), sut.peekAll(1))
    }

    @Test
    fun _10_element_peekAll_isAKey() {
        insert_TestSet()

        assertEquals(listOf("J1","J2"), sut.peekAll(10))
    }

    @Test
    fun _10_element_extractRoot() {
        insert_TestSet()

        val actual = sut.extractRoot()

        assertEquals("C", actual)
        assertEquals(9, sut.size)
        assertEquals(emptyList(), sut[3])
    }

    @Test
    fun _10_element_insert_smaller() {
        insert_TestSet()

        sut.insert(1, "A")

        assertEquals(11, sut.size)
        assertEquals(listOf("A"), sut[1])
        assertEquals(listOf("C"), sut[3])
        assertEquals("A", sut.peekRoot)
    }

    @Test
    fun _10_element_insert_same_as_smallest() {
        insert_TestSet()

        sut.insert(3, "C2")

        assertEquals(11, sut.size)
        assertEquals(listOf("C", "C2"), sut[3])
        assertEquals("C",sut.peekRoot)
    }

    @Test
    fun _10_element_insert_mid() {
        insert_TestSet()

        sut.insert(6, "F")

        assertEquals(11, sut.size)
        assertEquals(listOf("C"), sut[3])
        assertEquals(listOf("F"), sut[6])
        assertEquals("C",sut.peekRoot)
    }

    @Test
    fun _10_element_insert_mid_same_as() {
        insert_TestSet()

        sut.insert(9, "I2")

        assertEquals(11, sut.size)
        assertEquals(listOf("C"), sut[3])
        assertEquals(listOf("I","I2"), sut[9])
        assertEquals("C",sut.peekRoot)
    }

    @Test
    fun _10_element_insert_same_as_largest() {
        insert_TestSet()

        sut.insert(13, "M2")

        assertEquals(11, sut.size)
        assertEquals(listOf("C"), sut[3])
        assertEquals(listOf("M","M2"), sut[13])
        assertEquals("C",sut.peekRoot)
    }

    @Test
    fun _10_element_insert_largest() {
        insert_TestSet()

        sut.insert(26, "Z")

        assertEquals(11, sut.size)
        assertEquals(listOf("C"), sut[3])
        assertEquals(listOf("Z"), sut[26])
        assertEquals("C",sut.peekRoot)
    }

    /*
    @Test
    fun _10_element_extractRootAndThenInsert_smallest() {
        insert_10()

        val actual = sut.extractRootAndThenInsert(1, "A")

        assertEquals("C", actual)
        assertEquals(10, sut.size)
        assertEquals(listOf("A"), sut[1])
        assertEquals(emptyList(), sut[3])
        assertEquals("A", sut.peekRoot)
    }

    @Test
    fun _10_element_extractRootAndThenInsert_same_as_smallest() {
        insert_10()

        val actual = sut.extractRootAndThenInsert(3, "C2")

        assertEquals("C", actual)
        assertEquals(10, sut.size)
        assertEquals(listOf("C2"), sut[3])
        assertEquals("C2", sut.peekRoot)
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

        assertEquals("C", actual)
        assertEquals(10, sut.size)
        assertEquals(listOf("Z"), sut[26])
        assertEquals(emptyList(), sut[3])
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

        assertEquals("C", actual)
        assertEquals(10, sut.size)
        assertEquals(listOf("C2"), sut[3])
        assertEquals("C2", sut.peekRoot)
    }

    @Test
    fun _10_element_insertAndThenExtractRoot_mid() {
        insert_10()

        val actual = sut.insertAndThenExtractRoot(7, "G")

        assertEquals("C", actual)
        assertEquals(10, sut.size)
        assertEquals(listOf("G"), sut[7])
        assertEquals(emptyList(), sut[3])
        assertEquals("D", sut.peekRoot)
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

        assertEquals("C", actual)
        assertEquals(10, sut.size)
        assertEquals(setOf("M","M2"), sut[13].toSet())
        assertEquals(emptyList(), sut[3])
        assertEquals("D", sut.peekRoot)
    }

    @Test
    fun _10_element_insertAndThenExtractRoot_largest() {
        insert_10()

        val actual = sut.insertAndThenExtractRoot(26, "Z")

        assertEquals("C", actual)
        assertEquals(10, sut.size)
        assertEquals(setOf("M"), sut[13].toSet())
        assertEquals(setOf("Z"), sut[26].toSet())
        assertEquals(emptyList(), sut[3])
        assertEquals("D", sut.peekRoot)
    }
    */

    @Test
    fun _10_element_iterator_toList() {
        insert_TestSet()

        assertEquals(listOf("C", "D", "E", "H", "I", "J1", "J2", "K", "L", "M"), sut.toList())
    }

    @Test
    fun _10_element_iterator_entries() {
        insert_TestSet()

        assertEquals(
            listOf(
                3,
                4,
                5,
                8,
                9,
                10,
                11,
                12,
                13
            ), sut.keys
        )
    }

    @Test
    fun tree_order() {
        sut.insert(2, "2")
        assertEquals("2", sut.peekRoot)
        assertEquals(
            listOf(
                2
            ), sut.keys
        )

        sut.insert(6, "6")
        assertEquals("2", sut.peekRoot)
        assertEquals(
            listOf(
                2,
                6
            ), sut.keys
        )

        sut.insert(4, "4")
        assertEquals("2", sut.peekRoot)
        assertEquals(
            listOf(
                2,
                6,
                4
            ), sut.keys
        )

        sut.insert(5, "5")
        assertEquals("2", sut.peekRoot)
        assertEquals(
            listOf(
                2,
                5,
                4,
                6
            ), sut.keys
        )
    }
}