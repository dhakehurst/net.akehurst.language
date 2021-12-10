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

fun <E> fifoQueue(vararg elements: E): FifoQueue<E> {
    val q = FifoQueue<E>()
    for (e in elements) {
        q.addBack(e)
    }
    return q
}

class FifoQueue<E> : Iterable<E> {

    // front ................ back
    // previous <-- entry --> next
    class Entry<E>(val value: E) {
        var previous: Entry<E>? = null; internal set
        var next: Entry<E>? = null; internal set
        override fun toString(): String {
            val pre = if (null == previous) "|-" else "<-"
            val post = if (null == next) "-|" else "->"
            return "$pre$value$post"
        }
    }

    private var _size = 0
    private var _front: Entry<E>? = null
    private var _back: Entry<E>? = null

    val size get() = this._size
    val front: E? get() = this._front?.value
    val back: E? get() = this._back?.value
    val frontEntry get() = this._front
    val backEntry get() = this._back

    /**
     * add element on front of queue
     */
    fun addFront(element: E) {
        val entry = Entry(element)
        val front = this._front
        entry.next = front
        front?.let { it.previous = entry }
        this._front = entry
        if (null == this._back) this._back = entry
        this._size++
    }

    /**
     * add element on back of queue
     */
    fun addBack(element: E) {
        val entry = Entry(element)
        val back = this._back
        entry.previous = back
        back?.let { it.next = entry }
        this._back = entry
        if (null == this._front) this._front = entry
        this._size++
    }

    /**
     * remove element from back of queue
     */
    fun removeBack(): E {
        val back = this._back ?: error("FifoQueue is empty")
        back.previous?.next = null
        this._back = back.previous
        this._size--
        return back.value
    }


    /**
     * remove element from front of queue
     */
    fun removeFront(): E {
        val back = this._back ?: error("FifoQueue is empty")
        back.previous?.next = null
        this._back = back.previous
        this._size--
        return back.value
    }

    // --- Iterable<E> ---
    override fun iterator(): Iterator<E> = object : Iterator<E> {
        private var _next: Entry<E>? = this@FifoQueue._back
        override fun hasNext(): Boolean = _next != null

        override fun next(): E {
            val n = _next ?: error("FifoQueue is empty")
            this._next = n.previous
            return n.value
        }
    }

    // --- Any ---
    override fun toString(): String = when (this.size) {
        0 -> "[]"
        else -> this.joinToString(prefix = "[", postfix = "]", separator = ",") { it.toString() }
    }

    override fun hashCode(): Int = error("Not supported - inefficient")
    override fun equals(other: Any?): Boolean = error("Not supported - inefficient")
}