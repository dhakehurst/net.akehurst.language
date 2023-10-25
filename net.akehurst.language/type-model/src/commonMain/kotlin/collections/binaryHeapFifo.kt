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

package net.akehurst.language.collections

fun <K : Comparable<K>, V> binaryHeapFifoMin(): BinaryHeapFifo<K, V> = binaryHeapFifo { parent, child ->
    when {
        parent < child -> 1
        parent > child -> -1
        else -> 0
    }
}

fun <K : Comparable<K>, V> binaryHeapFifoMax(): BinaryHeapFifo<K, V> = binaryHeapFifo { parent, child ->
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
fun <K, V> binaryHeapFifo(comparator: Comparator<K>): BinaryHeapFifo<K, V> = BinaryHeapFifoComparable(comparator)

interface BinaryHeapFifo<K, V> : Iterable<V> {

    val size: Int

    /**
     * the root of the tree - the (or one of the) element(s) with the minimum key
     * null if the BinaryHeap is empty
     */
    val peekRoot: V?

    /**
     * the keys of the heap in the order the heap stores them
     */
    val keys: List<K>

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
    fun peek(key: K): V?
    fun peekAll(key: K): List<V>
    fun extractRoot(): V?

    fun clear()
}

class BinaryHeapFifoComparable<K, V>(
    val comparator: Comparator<K> //(parent: K, child: K) -> Boolean
) : BinaryHeapFifo<K, V> {

    private val _elements = mutableMapOf<K, FifoQueue<V>>()
    private val _keys = mutableListOf<K>()

    override val size: Int get() = this._elements.values.sumOf { it.size }
    override val peekRoot: V?
        get() = when (this._elements.size) {
            0 -> null
            else -> this._elements[this._keys[0]!!]!!.back
        }

    override val keys: List<K> get() = _keys

    override operator fun set(key: K, value: V) = this.insert(key, value)
    override fun get(key: K): List<V> = this.peekAll(key)

    override fun isEmpty(): Boolean = 0 == this.size
    override fun isNotEmpty(): Boolean = 0 != this.size

    override fun insert(key: K, value: V) {
        when {
            this._elements.containsKey(key) -> {
                this._elements[key]!!.addFront(value)
            }

            else -> {
                this.addElement(key, value)
                this._keys.add(key)
                this.upHeap(this._elements.size - 1, key)
            }
        }
    }

    override fun extractRoot(): V? {
        return when (this._keys.size) {
            0 -> null
            else -> {
                val rootKey = this._keys[0]
                val q = this._elements[rootKey]!!
                return when {
                    1 == q.size -> {
                        this._elements.remove(rootKey) //TODO: might be faster not to delete the FifoQueue - just leave it empty?
                        this.swap(0, this._keys.size - 1)
                        this._keys.removeLastOrNull()
                        this.downHeap(0)
                        q.removeBack()
                    }

                    else -> {
                        q.removeBack()
                    }
                }
            }
        }
    }

    /*
    override fun extractRootAndThenInsert(key: K, value: V): V? {
        return when {
            0 == this._keys.size -> {
                this.addElement(key, value)
                this._keys.add(key)
                null
            }
            key==this._keys[0] -> {
                val q = this._elements[key]!!
                q.addFront(value)
                q.removeBack()
            }
            TODO()
            else -> {
                this.addElement(key, value)
                this._keys.add(key)
                this.swap(0, this._elements.size - 1)
                val oldRoot = this._keys.removeLastOrNull()
                this.downHeap(0)
                this.removeElement(oldRoot)
            }
        }
    }

    override fun insertAndThenExtractRoot(key: K, value: V): V {
        return when {
            0 == this._keys.size -> value
            0 < this.comparator.compare(key, this._keys[0]) -> value
            key==this._keys[0] -> {
                val q = this._elements[key]!!
                q.addFront(value)
                q.removeBack()
            }
            TODO()
            else -> {
                this.addElement(key,value)
                val oldRoot = this._keys[0]
                this._keys[0] = key
                this.downHeap(0)
                this.removeElement(key)
            }
        }
    }
    */

    override fun peek(key: K): V? = this._elements[key]?.back

    override fun peekAll(key: K): List<V> = this._elements[key]?.toList() ?: emptyList()

    override fun clear() {
        this._elements.clear()
    }

    private fun parentIndexOf(childIndex: Int) = (childIndex - 1) / 2
    private fun leftChildIndexOf(parentIndex: Int) = (2 * parentIndex) + 1
    private fun rightChildIndexOf(parentIndex: Int) = (2 * parentIndex) + 2

    private fun addElement(key: K, value: V) {
        var q = this._elements[key]
        if (null == q) {
            q = FifoQueue()
            this._elements[key] = q
        }
        q.addFront(value)
    }

    // index - of the element to sort
    // elementKey  - of the element to sort (saves fetching it)
    // return new index of element
    private fun upHeap(index: Int, elementKey: K): Int {
        var elementIndex = index
        var parentIndex = parentIndexOf(elementIndex)
        var parentKey = this._keys[parentIndex]
        while (0 > this.comparator.compare(parentKey, elementKey)) {
            swap(parentIndex, elementIndex)
            elementIndex = parentIndex
            parentIndex = parentIndexOf(elementIndex)
            parentKey = this._keys[parentIndex]
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

        if (leftChildIndex < this._elements.size && 0 < this.comparator.compare(this._keys[leftChildIndex], this._keys[smallest])) {
            smallest = leftChildIndex
        }
        if (rightChildIndex < this._elements.size && 0 < this.comparator.compare(this._keys[rightChildIndex], this._keys[smallest])) {
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
        val t = this._keys[i1]
        this._keys[i1] = this._keys[i2]
        this._keys[i2] = t
    }

    // --- Iterable<V> ---
    override fun iterator(): Iterator<V> = object : Iterator<V> {
        private var _sortedQueues = this@BinaryHeapFifoComparable._elements.entries.sortedWith { a, b -> this@BinaryHeapFifoComparable.comparator.compare(b.key, a.key) }
        private var _sorted = _sortedQueues.flatMap { it.value.toList() }
        private var _nextIndex = 0
        override fun hasNext(): Boolean = _nextIndex < _sorted.size
        override fun next(): V = _sorted[_nextIndex].also { _nextIndex++ }
    }


    override fun toString(): String = when (this.size) {
        0 -> "{}"
        else -> this._keys.map { Pair(it, _elements[it]) }.joinToString(separator = "\n") { it.toString() }
    }
}