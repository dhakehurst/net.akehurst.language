/**
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
 */

package net.akehurst.language.collections

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class test_FifoQueue {

    @Test
    fun removeFront_removes_from_front() {
        val q = fifoQueue("a", "b", "c") // front a, back c
        assertEquals("a", q.removeFront())
        assertEquals(2, q.size)
        assertEquals("b", q.front)
        assertEquals("c", q.back)
    }

    @Test
    fun removeBack_removes_from_back() {
        val q = fifoQueue("a", "b", "c")
        assertEquals("c", q.removeBack())
        assertEquals(2, q.size)
        assertEquals("a", q.front)
        assertEquals("b", q.back)
    }

    @Test
    fun removeBack_last_element_empties_queue() {
        val q = fifoQueue("a")
        assertEquals("a", q.removeBack())
        assertEquals(0, q.size)
        assertNull(q.front)
        assertNull(q.back)
        q.addBack("b")
        assertEquals("b", q.front)
        assertEquals("b", q.back)
    }

    @Test
    fun removeFront_last_element_empties_queue() {
        val q = fifoQueue("a")
        assertEquals("a", q.removeFront())
        assertEquals(0, q.size)
        assertNull(q.front)
        assertNull(q.back)
        q.addFront("b")
        assertEquals("b", q.front)
        assertEquals("b", q.back)
    }
}
