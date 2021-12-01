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

fun <K : Comparable<K>, V> binaryHeapMin(): BinaryHeap<K, V> = binaryHeap { parent, child ->
    when {
        parent < child -> 1
        parent > child -> -1
        else -> 0
    }
}

fun <K : Comparable<K>, V> binaryHeapMax(): BinaryHeap<K, V> = binaryHeap { parent, child ->
    when {
        parent > child -> 1
        parent < child -> -1
        else -> 0
    }
}

/**
 * comparator: parent,child -> when {
 *   1 -> move parent up
 *  -1 -> move child up
 *   0 -> do nothing
 * }
 */
fun <K, V> binaryHeap(comparator: Comparator<K>): BinaryHeap<K, V> = BinaryHeapComparable(comparator)

infix fun <K, V> K.to(that: V): BinaryHeap.Entry<K, V> = BinaryHeapComparable.Entry(this, that)

interface BinaryHeap<K, V> : Iterable<V> {

    interface Entry<K, V> {
        val key: K
        val value: V
    }

    val size: Int

    /**
     * the root of the tree - the (or one of the) element(s) with the minimum key
     * null if the BinaryHeap is empty
     */
    val peekRoot: V?

    val entries: List<Entry<K, V>>

    /**
     * insert(key,value)
     */
    operator fun set(key: K, value: V)

    /**
     * peek(key)
     * order is not predictable, but faster to return a list than a set
     */
    operator fun get(key: K): List<V>

    fun isEmpty(): Boolean
    fun isNotEmpty(): Boolean
    fun insert(key: K, value: V)
    fun peekOneOf(key: K): V?
    fun peekAll(key: K): List<V>
    fun extractRoot(): V?
    fun extractRootAndThenInsert(key: K, value: V): V?
    fun insertAndThenExtractRoot(key: K, value: V): V

    fun clear()
}

class BinaryHeapComparable<K, V>(
    val comparator: Comparator<K> //(parent: K, child: K) -> Boolean
) : BinaryHeap<K, V> {

    class Entry<K, V>(override val key: K, override val value: V) : BinaryHeap.Entry<K, V> {
        override fun hashCode(): Int = (key.hashCode() * 31) + value.hashCode()
        override fun equals(other: Any?): Boolean = when (other) {
            !is BinaryHeap.Entry<*, *> -> false
            else -> this.key == other.key && this.value == other.value
        }

        override fun toString(): String = "$key -> $value"
    }

    private val _elements = mutableListOf<BinaryHeap.Entry<K, V>>()

    override val size: Int get() = this._elements.size
    override val peekRoot: V?
        get() = when (this._elements.size) {
            0 -> null
            else -> this._elements[0].value
        }

    override val entries: List<BinaryHeap.Entry<K, V>> get() = _elements

    override operator fun set(key: K, value: V) = this.insert(key, value)
    override fun get(key: K): List<V> = this.peekAll(key)

    override fun isEmpty(): Boolean = 0 == this.size
    override fun isNotEmpty(): Boolean = 0 != this.size

    override fun insert(key: K, value: V) {
        val e = Entry(key, value)
        this._elements.add(e)
        this.upHeap(this._elements.size - 1, key)
    }

    override fun extractRoot(): V? {
        return if (0 == this.size) {
            null
        } else {
            this.swap(0, this._elements.size - 1)
            val oldRoot = this._elements.removeLastOrNull()
            this.downHeap(0)
            oldRoot?.value
        }
    }

    override fun extractRootAndThenInsert(key: K, value: V): V? {
        return when (this.size) {
            0 -> {
                this._elements.add(Entry(key, value))
                null
            }
            1 -> {
                val oldRoot = this._elements[0]
                this._elements[0] = Entry(key, value)
                oldRoot.value
            }
            else -> {
                this._elements.add(Entry(key, value))
                this.swap(0, this._elements.size - 1)
                val oldRoot = this._elements.removeLastOrNull()
                this.downHeap(0)
                oldRoot?.value
            }
        }
    }

    override fun insertAndThenExtractRoot(key: K, value: V): V {
        return when {
            0 == this.size -> value
            0 < this.comparator.compare(key, this._elements[0].key) -> value
            else -> {
                val oldRoot = this._elements[0]
                this._elements[0] = Entry(key, value)
                this.downHeap(0)
                oldRoot.value
            }
        }
    }

    override fun peekOneOf(key: K): V? = searchSubTreeFor(0, key).firstOrNull()

    override fun peekAll(key: K): List<V> = searchSubTreeFor(0, key)

    override fun clear() {
        this._elements.clear()
    }

    private fun parentIndexOf(childIndex: Int) = (childIndex - 1) / 2
    private fun leftChildIndexOf(parentIndex: Int) = (2 * parentIndex) + 1
    private fun rightChildIndexOf(parentIndex: Int) = (2 * parentIndex) + 2

    private fun searchSubTreeFor(startEntryIndex: Int, key: K): List<V> {
        val elements = mutableListOf<V>()
        val left = leftChildIndexOf(startEntryIndex)
        val right = rightChildIndexOf(startEntryIndex)
        return when {
            startEntryIndex >= this._elements.size -> elements
            key == this._elements[startEntryIndex].key -> elements + this._elements[startEntryIndex].value + searchSubTreeFor(left, key) + searchSubTreeFor(right, key)
            0 < this.comparator.compare(key, this._elements[startEntryIndex].key) -> elements
            else -> elements + searchSubTreeFor(left, key) + searchSubTreeFor(right, key)
        }
    }

    // index - of the element to sort
    // elementKey  - of the element to sort (saves fetching it)
    // return new index of element
    private fun upHeap(index: Int, elementKey: K): Int {
        var elementIndex = index
        var parentIndex = parentIndexOf(elementIndex)
        var parentKey = this._elements[parentIndex].key
        while (0 > this.comparator.compare(parentKey, elementKey)) {
            swap(parentIndex, elementIndex)
            elementIndex = parentIndex
            parentIndex = parentIndexOf(elementIndex)
            parentKey = this._elements[parentIndex].key
        }
        return elementIndex
    }

    // index - of the element to sort
    // elementKey  - of the element to sort (saves fetching it)
    // return new index of element
    private fun downHeap(index: Int): Int {
        val leftChildIndex = leftChildIndexOf(index)
        val rightChildIndex = rightChildIndexOf(index)
        var smallest = index

        if (leftChildIndex < this._elements.size && 0 < this.comparator.compare(this._elements[leftChildIndex].key, this._elements[smallest].key)) {
            smallest = leftChildIndex
        }
        if (rightChildIndex < this._elements.size && 0 < this.comparator.compare(this._elements[rightChildIndex].key, this._elements[smallest].key)) {
            smallest = rightChildIndex
        }

        return if (smallest != index) {
            swap(index, smallest)
            downHeap(smallest)
        } else {
            index
        }
    }

    fun swap(i1: Int, i2: Int) {
        val t = this._elements[i1]
        this._elements[i1] = this._elements[i2]
        this._elements[i2] = t
    }

    // --- Iterable<V> ---
    override fun iterator(): Iterator<V> = object : Iterator<V> {
        private var _sorted = this@BinaryHeapComparable._elements.sortedWith { a, b -> this@BinaryHeapComparable.comparator.compare(b.key, a.key) }
        private var _nextIndex = 0
        override fun hasNext(): Boolean = _nextIndex < _sorted.size
        override fun next(): V = _sorted[_nextIndex].value.also { _nextIndex++ }
    }


    override fun toString(): String = when(this.size) {
        0 -> "{}"
        else -> this._elements.joinToString(separator = "\n") { it.toString() }
    }
}