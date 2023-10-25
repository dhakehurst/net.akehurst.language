/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.runtime.graph

import net.akehurst.language.collections.binaryHeap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_GraphStructuredStack {

    @Test
    fun construct() {
        val sut = GraphStructuredStack<Int>(binaryHeap { parent, child ->
            when {
                parent < child -> 1
                parent > child -> -1
                else -> 0
            }
        })

        assertNotNull(sut)

    }

    @Test
    fun peek_empty() {
        val sut = GraphStructuredStack<Int>(binaryHeap { parent, child ->
            when {
                parent < child -> 1
                parent > child -> -1
                else -> 0
            }
        })

        val actual = sut.peekPrevious(1)

        assertEquals(emptySet(), actual)
    }

    @Test
    fun pop_empty() {
        val sut = GraphStructuredStack<Int>(binaryHeap { parent, child ->
            when {
                parent < child -> 1
                parent > child -> -1
                else -> 0
            }
        })

        val actual = sut.pop(1)

        assertEquals(emptySet(), actual)
    }

    @Test
    fun push_empty() {
        val sut = GraphStructuredStack<Int>(binaryHeap { parent, child ->
            when {
                parent < child -> 1
                parent > child -> -1
                else -> 0
            }
        })

        sut.push(0, 1)

        assertEquals(emptySet(), sut.peekPrevious(0))
        assertEquals(setOf(0), sut.peekPrevious(1))
    }

    @Test
    fun push_toNext() {
        val sut = GraphStructuredStack<Int>(binaryHeap { parent, child ->
            when {
                parent < child -> 1
                parent > child -> -1
                else -> 0
            }
        })
        sut.push(0, 1)

        sut.push(1, 2)

        assertEquals(emptySet(), sut.peekPrevious(0))
        assertEquals(setOf(0), sut.peekPrevious(1))
        assertEquals(setOf(1), sut.peekPrevious(2))
    }

    @Test
    fun push_additionalToRoot() {
        val sut = GraphStructuredStack<Int>(binaryHeap { parent, child ->
            when {
                parent < child -> 1
                parent > child -> -1
                else -> 0
            }
        })
        sut.push(0, 1)

        sut.push(0, 2)

        assertEquals(emptySet(), sut.peekPrevious(0))
        assertEquals(setOf(0), sut.peekPrevious(1))
        assertEquals(setOf(0), sut.peekPrevious(2))
    }

    @Test
    fun push_toNextAfterFork() {
        val sut = GraphStructuredStack<Int>(binaryHeap { parent, child ->
            when {
                parent < child -> 1
                parent > child -> -1
                else -> 0
            }
        })
        sut.push(0, 1)
        sut.push(0, 2)

        sut.push(1, 3)

        assertEquals(emptySet(), sut.peekPrevious(0))
        assertEquals(setOf(0), sut.peekPrevious(1))
        assertEquals(setOf(0), sut.peekPrevious(2))
        assertEquals(setOf(1), sut.peekPrevious(3))
    }

    @Test
    fun push_toMergeAfterFork() {
        val sut = GraphStructuredStack<Int>(binaryHeap { parent, child ->
            when {
                parent < child -> 1
                parent > child -> -1
                else -> 0
            }
        })
        sut.push(0, 1)
        sut.push(0, 2)
        sut.push(1, 3)

        sut.push(2, 3)

        assertEquals(emptySet(), sut.peekPrevious(0))
        assertEquals(setOf(0), sut.peekPrevious(1))
        assertEquals(setOf(0), sut.peekPrevious(2))
        assertEquals(setOf(1, 2), sut.peekPrevious(3))
    }

    @Test
    fun push_toAdditionalAfterDiamond() {
        val sut = GraphStructuredStack<Int>(binaryHeap { parent, child ->
            when {
                parent < child -> 1
                parent > child -> -1
                else -> 0
            }
        })
        sut.push(0, 1)
        sut.push(0, 2)
        sut.push(1, 3)
        sut.push(2, 3)

        sut.push(3, 4)
        sut.push(2, 5)

        assertEquals(emptySet(), sut.peekPrevious(0))
        assertEquals(setOf(0), sut.peekPrevious(1))
        assertEquals(setOf(0), sut.peekPrevious(2))
        assertEquals(setOf(1, 2), sut.peekPrevious(3))
        assertEquals(setOf(3), sut.peekPrevious(4))
        assertEquals(setOf(2), sut.peekPrevious(5))
    }
}