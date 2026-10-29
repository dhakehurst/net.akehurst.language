/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.collections

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class test_MutableMap2IntKeys {

    @Test
    fun a() {
        val a = arrayOf(
                7 and 1,
                7 and 2,
                7 and 3,
                7 and 4,
                7 and 5,
                7 and 6,
                7 and 7,
                7 and 8,
                7 and 9,
                7 and 10,
                7 and 11
        )
        val i = 9
    }

    @Test
    fun t() {
        val map = MutableMap2IntKeys<String>(6,6)
        map[1, 1] = "hello"
        assertEquals("hello", map[1, 1])
    }
    @Test
    fun t1() {
        val map = MutableMap2IntKeys<String>(5,3)
        for(i in -3 until 5) {
            for (j in 0 until 3) {
                map[i, j] = "index $i $j"
            }
        }
        assertEquals(8 * 3, map.size)
        assertEquals("index 1 1", map[1, 1])
        assertEquals("index -3 0", map[-3, 0])
        assertEquals("index 4 2", map[4, 2])
        // keys outside the range of the map (key1 in -5 until 5, key2 in 0 until 3) are never in it
        assertNull(map[9, 9])
        assertNull(map[77, 77])
        assertNull(map[99, 99])
        assertNull(map[0, 3]) // would otherwise be the same index as [1, 0]
        assertFailsWith<IllegalStateException> { map[0, 3] = "x" }
    }

    @Test
    fun size_does_not_count_overwrite() {
        val map = MutableMap2IntKeys<String>(6, 6)
        map[1, 1] = "a"
        map[1, 1] = "b"
        assertEquals(1, map.size)
        assertEquals("b", map[1, 1])
    }

    @Test
    fun t2() {
        val map = MutableMap2IntKeys<String>(100,100)
        for(i in 0 until 100) {
            for (j in 0 until 100) {
                map[i, j] = "index $i $j"
            }
        }
        assertEquals("index 1 1", map[1, 1])
        assertEquals("index 9 9", map[9, 9])
        assertEquals("index 77 77", map[77, 77])
        assertEquals("index 99 99", map[99,99])
    }
}