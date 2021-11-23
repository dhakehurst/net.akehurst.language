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

class test_BinaryHeap {

    lateinit var sut:BinaryHeap<Int,String>

    @BeforeTest
    fun before() {
        sut = binaryHeapMin()
    }

    private fun insert_10() {
        sut.insert(3,"C")
        sut.insert(4,"D")
        sut.insert(5,"E")
        sut.insert(8,"H")
        sut.insert(9,"I")
        sut.insert(10,"J")
        sut.insert(11,"K")
        sut.insert(12,"L")
        sut.insert(13,"M")
        sut.insert(14,"N")
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
        sut.insert(1,"A")

        assertEquals(1,sut.size)
        assertEquals(listOf("A"), sut[1])
    }

    @Test
    fun empty_extractRootAndThenInsert() {
        val actual = sut.extractRootAndThenInsert(1,"A")

        assertNull(actual)
        assertEquals(1,sut.size)
        assertEquals(listOf("A"), sut[1])
    }

    @Test
    fun empty_insertAndThenExtractRoot() {
        val actual = sut.insertAndThenExtractRoot(1,"A")

        assertEquals(0, sut.size)
        assertEquals("A", actual)
    }

    @Test
    fun empty_iterator_toList() {
        assertEquals(emptyList(),sut.toList())
    }

    @Test
    fun _1_element_size() {
        sut.insert(2,"B")

        assertEquals(1, sut.size)
    }

    @Test
    fun _1_element_peekRoot() {
        sut.insert(2,"B")

        assertEquals("B", sut.peekRoot)
    }

    @Test
    fun _1_element_peekOneOf_notAKey() {
        sut.insert(2,"B")

        assertNull( sut.peekOneOf(1))
    }

    @Test
    fun _1_element_peekOneOf_isAKey() {
        sut.insert(2,"B")

        assertEquals("B",sut.peekOneOf(2))
    }

    @Test
    fun _1_element_peekAll_notAKey() {
        sut.insert(2,"B")

        assertEquals(emptyList(),sut.peekAll(1))
    }

    @Test
    fun _1_element_peekAll_isAKey() {
        sut.insert(2,"B")

        assertEquals(listOf("B"),sut.peekAll(2))
    }

    @Test
    fun _1_element_extractRoot() {
        sut.insert(2,"B")

        val actual = sut.extractRoot()

        assertEquals("B", actual)
        assertEquals(0, sut.size)
        assertEquals(emptyList(), sut[2])
    }

    @Test
    fun _1_element_insert_smaller() {
        sut.insert(2,"B")

        sut.insert(1,"A")

        assertEquals(2,sut.size)
        assertEquals(listOf("A"), sut[1])
        assertEquals(listOf("B"), sut[2])
    }

    @Test
    fun _1_element_insert_same() {
        sut.insert(2,"B1")

        sut.insert(2,"B2")

        assertEquals(2,sut.size)
        assertEquals(listOf("B1", "B2"), sut[2])
    }

    @Test
    fun _1_element_insert_larger() {
        sut.insert(2,"B")

        sut.insert(3,"C")

        assertEquals(2,sut.size)
        assertEquals(listOf("C"), sut[3])
        assertEquals(listOf("B"), sut[2])
    }

    @Test
    fun _1_element_extractRootAndThenInsert_smaller() {
        sut.insert(2,"B")

        val actual = sut.extractRootAndThenInsert(1,"A")

        assertEquals("B", actual)
        assertEquals(1,sut.size)
        assertEquals(listOf("A"), sut[1])
        assertEquals(emptyList(),sut[2])
    }

    @Test
    fun _1_element_extractRootAndThenInsert_same() {
        sut.insert(2,"B1")

        val actual = sut.extractRootAndThenInsert(2,"B2")

        assertEquals("B1", actual)
        assertEquals(1,sut.size)
        assertEquals(listOf("B2"), sut[2])
    }

    @Test
    fun _1_element_extractRootAndThenInsert_larger() {
        sut.insert(2,"B")

        val actual = sut.extractRootAndThenInsert(3,"C")

        assertEquals("B", actual)
        assertEquals(1,sut.size)
        assertEquals(listOf("C"), sut[3])
        assertEquals(emptyList(), sut[2])
    }

    @Test
    fun _1_element_insertAndThenExtractRoot_smaller() {
        sut.insert(2,"B")

        val actual = sut.insertAndThenExtractRoot(1,"A")

        assertEquals("A", actual)
        assertEquals(1,sut.size)
        assertEquals(listOf("B"), sut[2])
        assertEquals(emptyList(),sut[1])
    }

    @Test
    fun _1_element_insertAndThenExtractRoot_same() {
        sut.insert(2,"B1")

        val actual = sut.insertAndThenExtractRoot(2,"B2")

        assertEquals("B1", actual)
        assertEquals(1,sut.size)
        assertEquals(listOf("B2"), sut[2])
        assertEquals(emptyList(),sut[1])
    }

    @Test
    fun _1_element_insertAndThenExtractRoot_larger() {
        sut.insert(2,"B")

        val actual = sut.insertAndThenExtractRoot(3,"C")

        assertEquals("B", actual)
        assertEquals(1,sut.size)
        assertEquals(listOf("C"), sut[3])
        assertEquals(emptyList(),sut[2])
    }

    @Test
    fun _1_element_iterator_toList() {
        sut.insert(2,"B")

        assertEquals(listOf("B"),sut.toList())
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

        assertNull( sut.peekOneOf(1))
    }

    @Test
    fun _10_element_peekOneOf_isAKey() {
        insert_10()

        assertEquals("B",sut.peekOneOf(2))
    }

    @Test
    fun _10_element_peekAll_notAKey() {
        insert_10()

        assertEquals(emptyList(),sut.peekAll(1))
    }

    @Test
    fun _10_element_peekAll_isAKey() {
        insert_10()

        assertEquals(listOf("B"),sut.peekAll(2))
    }

    @Test
    fun _10_element_extractRoot() {
        insert_10()

        val actual = sut.extractRoot()

        assertEquals("B", actual)
        assertEquals(0, sut.size)
        assertEquals(emptyList(), sut[2])
    }

    @Test
    fun _10_element_insert_smaller() {
        insert_10()

        sut.insert(1,"A")

        assertEquals(2,sut.size)
        assertEquals(listOf("A"), sut[1])
        assertEquals(listOf("B"), sut[2])
    }

    @Test
    fun _10_element_insert_same() {
        insert_10()

        sut.insert(2,"B2")

        assertEquals(2,sut.size)
        assertEquals(listOf("B1", "B2"), sut[2])
    }

    @Test
    fun _10_element_insert_larger() {
        insert_10()

        sut.insert(3,"C")

        assertEquals(2,sut.size)
        assertEquals(listOf("C"), sut[3])
        assertEquals(listOf("B"), sut[2])
    }

    @Test
    fun _10_element_extractRootAndThenInsert_smaller() {
        insert_10()

        val actual = sut.extractRootAndThenInsert(1,"A")

        assertEquals("B", actual)
        assertEquals(1,sut.size)
        assertEquals(listOf("A"), sut[1])
        assertEquals(emptyList(),sut[2])
    }

    @Test
    fun _10_element_extractRootAndThenInsert_same() {
        insert_10()

        val actual = sut.extractRootAndThenInsert(2,"B2")

        assertEquals("B1", actual)
        assertEquals(1,sut.size)
        assertEquals(listOf("B2"), sut[2])
    }

    @Test
    fun _10_element_extractRootAndThenInsert_larger() {
        insert_10()

        val actual = sut.extractRootAndThenInsert(3,"C")

        assertEquals("B", actual)
        assertEquals(1,sut.size)
        assertEquals(listOf("C"), sut[3])
        assertEquals(emptyList(), sut[2])
    }

    @Test
    fun _10_element_insertAndThenExtractRoot_smallest() {
        insert_10()

        val actual = sut.insertAndThenExtractRoot(1,"A")

        assertEquals("A", actual)
        assertEquals(1,sut.size)
        assertEquals(listOf("B"), sut[2])
        assertEquals(emptyList(),sut[1])
    }

    @Test
    fun _10_element_insertAndThenExtractRoot_same_as_smallest() {
        insert_10()

        val actual = sut.insertAndThenExtractRoot(2,"B2")

        assertEquals("B1", actual)
        assertEquals(1,sut.size)
        assertEquals(listOf("B2"), sut[2])
        assertEquals(emptyList(),sut[1])
    }

    @Test
    fun _10_element_insertAndThenExtractRoot_mid() {
        insert_10()

        val actual = sut.insertAndThenExtractRoot(2,"B2")

        assertEquals("B1", actual)
        assertEquals(1,sut.size)
        assertEquals(listOf("B2"), sut[2])
        assertEquals(emptyList(),sut[1])
    }

    @Test
    fun _10_element_insertAndThenExtractRoot_same_as_mid() {
        insert_10()

        val actual = sut.insertAndThenExtractRoot(2,"B2")

        assertEquals("B1", actual)
        assertEquals(1,sut.size)
        assertEquals(listOf("B2"), sut[2])
        assertEquals(emptyList(),sut[1])
    }
    
    @Test
    fun _10_element_insertAndThenExtractRoot_same_as_largest() {
        insert_10()

        val actual = sut.insertAndThenExtractRoot(3,"C")

        assertEquals("B", actual)
        assertEquals(1,sut.size)
        assertEquals(listOf("C"), sut[3])
        assertEquals(emptyList(),sut[2])
    }

    @Test
    fun _10_element_insertAndThenExtractRoot_largest() {
        insert_10()

        val actual = sut.insertAndThenExtractRoot(3,"C")

        assertEquals("B", actual)
        assertEquals(1,sut.size)
        assertEquals(listOf("C"), sut[3])
        assertEquals(emptyList(),sut[2])
    }
    
    @Test
    fun _10_element_iterator_toList() {
        insert_10()

        assertEquals(listOf("C","D","E","H","I","J","K","L","M","N"),sut.toList())
    }

}