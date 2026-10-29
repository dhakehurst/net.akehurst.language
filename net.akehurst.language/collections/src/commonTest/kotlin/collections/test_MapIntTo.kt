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

class test_MapIntTo {

    @Test
    fun map_0() {
        val map = MapIntTo<String>(3)
        map[0] = "hello"
        assertEquals("hello", map[0])
    }

    @Test
    fun map_0_clash() {
        val map = MapIntTo<String>(3)
        map[0] = "index 0"
        map[8] = "index 8"
        map[16] = "index 16"
        assertEquals("index 0", map[0])
        assertEquals("index 8", map[8])
        assertEquals("index 16", map[16])
    }

    @Test
    fun map_1_to_4_does_not_grow() {
        val map = MapIntTo<String>(3)
        for(i in 1 until 4) {
            map[i] = "index $i"
            assertEquals(i, map.size)
        }
        for(i in 1 until 4) {
            assertEquals("index $i", map[i])
        }
    }

    @Test
    fun map_1_to_9_does_grow() {
        val map = MapIntTo<String>(3)
        for(i in 1 until 9) {
            map[i] = "index $i"
            assertEquals(i, map.size)
        }
        for(i in 1 until 9) {
            assertEquals("index $i", map[i])
        }
    }

    @Test
    fun map_key_clash() {
        val map = MapIntTo<String>(3)

        map[1] = "index 1"
        map[9] = "index 9"

        assertEquals("index 1", map[1])
        assertEquals("index 9", map[9])
    }

    @Test
    fun size_does_not_count_overwrite() {
        val map = MapIntTo<String>(3)
        map[1] = "a"
        map[1] = "b"
        assertEquals(1, map.size)
        assertEquals("b", map[1])
    }

    @Test
    fun loadFactor_1_never_fills_the_table() {
        // capacity 4, load factor 1.0: a full table would make lookup of a missing key loop forever
        val map = MapIntTo<String>(2, 1.0)
        for (k in 1..10) map[k] = "v$k"
        assertEquals(10, map.size)
        for (k in 1..10) assertEquals("v$k", map[k])
        assertEquals(null, map[99])
    }

    //TODO: test performance ?
}