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

interface OrderedSet<out E> : Set<E> {
    operator fun get(index: Int): E
}

interface MutableOrderedSet<E> : OrderedSet<E>, MutableSet<E>

fun <T> emptyOrderedSet(): OrderedSet<T> = EmptyOrderedSet
fun <E> orderedSetOf(vararg elements: E): OrderedSet<E> = OrderedSetImpl<E>(elements.toList())
fun <E> mutableOrderedSetOf(vararg elements: E): MutableOrderedSet<E> = MutableOrderedSetImpl<E>(elements.toList())

fun <T> Iterable<T>.toOrderedSet(): OrderedSet<T> = when (this) {
    is Collection<T> -> MutableOrderedSetImpl(this)
    else -> MutableOrderedSetImpl(this.toMutableSet())
}

fun <T> Iterable<T>.toMutableOrderedSet(): MutableOrderedSet<T> = when (this) {
    is Collection<T> -> MutableOrderedSetImpl(this)
    else -> MutableOrderedSetImpl(this.toMutableSet())
}

operator fun <T> OrderedSet<T>.plus(element: T): OrderedSet<T> {
    val result = this.toMutableOrderedSet()
    result.add(element)
    return result
}

operator fun <T> OrderedSet<T>.plus(elements: Iterable<T>): OrderedSet<T> {
    val result = this.toMutableList()
    result.addAll(elements)
    return OrderedSetImpl<T>(result)
}

operator fun <T> MutableOrderedSet<T>.plusAssign(elements: Iterable<T>) {
    this.addAll(elements)
}

class OrderedSetImpl<E> : OrderedSet<E> {
    constructor() {
        this._impl = LinkedHashSet()
    }

    constructor(collection: Collection<E>) {
        this._impl = LinkedHashSet(collection)
    }

    constructor(initialCapacity: Int) {
        this._impl = LinkedHashSet(initialCapacity)
    }

    private val _impl: LinkedHashSet<E>

    override fun get(index: Int): E {
        _impl.forEachIndexed { i, e ->
            if (index == i) return e
        }
        throw IndexOutOfBoundsException("$index")
    }

    override val size: Int get() = _impl.size
    override fun iterator(): Iterator<E> = _impl.iterator()
    override fun isEmpty(): Boolean = _impl.isEmpty()
    override fun contains(element: E): Boolean = _impl.contains(element)
    override fun containsAll(elements: Collection<E>): Boolean = _impl.containsAll(elements)

    override fun equals(other: Any?): Boolean {
        return when {
            other !is OrderedSet<*> -> false
            other.size != this.size -> false
            else -> {
                for (i in 0..this.size) {
                    if (this[i] != other[i]) return false
                }
                true
            }
        }
    }

    override fun hashCode() = _impl.hashCode()
    override fun toString() = _impl.toString()
}

class MutableOrderedSetImpl<E> : MutableOrderedSet<E> {
    constructor() {
        this._impl = LinkedHashSet()
    }

    constructor(collection: Collection<E>) {
        this._impl = LinkedHashSet(collection)
    }

    constructor(initialCapacity: Int) {
        this._impl = LinkedHashSet(initialCapacity)
    }

    private val _impl: LinkedHashSet<E>

    override fun get(index: Int): E {
        _impl.forEachIndexed { i, e ->
            if (index == i) return e
        }
        throw IndexOutOfBoundsException("$index")
    }

    override val size: Int get() = _impl.size
    override fun iterator(): MutableIterator<E> = _impl.iterator()
    override fun isEmpty(): Boolean = _impl.isEmpty()
    override fun contains(element: E): Boolean = _impl.contains(element)
    override fun containsAll(elements: Collection<E>): Boolean = _impl.containsAll(elements)

    override fun add(element: E): Boolean = _impl.add(element)
    override fun addAll(elements: Collection<E>): Boolean = _impl.addAll(elements)
    override fun remove(element: E): Boolean = _impl.remove(element)
    override fun removeAll(elements: Collection<E>): Boolean = _impl.removeAll(elements)
    override fun retainAll(elements: Collection<E>): Boolean = _impl.retainAll(elements)
    override fun clear() {
        _impl.clear()
    }

    override fun equals(other: Any?): Boolean {
        return when {
            other !is OrderedSet<*> -> false
            other.size != this.size -> false
            else -> {
                for (i in 0 until this.size) {
                    if (this[i] != other[i]) return false
                }
                true
            }
        }
    }

    override fun hashCode() = _impl.hashCode()
    override fun toString() = _impl.toString()
}

internal object EmptyIterator : ListIterator<Nothing> {
    override fun hasNext(): Boolean = false
    override fun hasPrevious(): Boolean = false
    override fun nextIndex(): Int = 0
    override fun previousIndex(): Int = -1
    override fun next(): Nothing = throw NoSuchElementException()
    override fun previous(): Nothing = throw NoSuchElementException()
}

internal object EmptyOrderedSet : OrderedSet<Nothing> {

    override val size: Int get() = 0
    override fun isEmpty(): Boolean = true
    override fun contains(element: Nothing): Boolean = false
    override fun containsAll(elements: Collection<Nothing>): Boolean = elements.isEmpty()

    override fun get(index: Int): Nothing = throw IndexOutOfBoundsException("An empty OrderedSet does not contain element at index $index.")
    override fun iterator(): Iterator<Nothing> = EmptyIterator

    override fun equals(other: Any?): Boolean = other is OrderedSet<*> && other.isEmpty()
    override fun hashCode(): Int = 0
    override fun toString(): String = "[]"
}