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

fun <T> mutableStackOf(vararg elements: T): MutableStack<T> {
    val stack = MutableStack<T>()
    elements.forEach {
        stack.push(it)
    }
    return stack
}

class Stack<T>(
    val elements: List<T> = emptyList()
) {

    class PopResult<T>(
        val item: T,
        val stack: Stack<T>
    )

    val size: Int get() = this.elements.size
    val isEmpty: Boolean get() = this.elements.size == 0
    val isNotEmpty: Boolean get() = this.elements.size != 0

    fun push(item: T): Stack<T> = Stack(elements + item)
    fun pushAll(items: List<T>): Stack<T> = Stack(this.elements + items)
    fun peek(): T = elements.last()
    fun peekOrNull(): T? = elements.lastOrNull()
    fun pop(): PopResult<T> = PopResult(this.peek(), Stack(elements.subList(0, size - 1)))
    fun clone() = Stack(elements)

    override fun hashCode(): Int = elements.hashCode()
    override fun equals(other: Any?): Boolean = when (other) {
        !is Stack<*> -> false
        else -> this.elements == other.elements
    }
}

class MutableStack<T>() {
    private val list = mutableListOf<T>()

    val size: Int get() = this.list.size
    val isEmpty: Boolean get() = this.list.size == 0
    val isNotEmpty: Boolean get() = this.list.size != 0
    val elements: List<T> get() = this.list

    fun clear() {
        this.list.clear()
    }

    fun push(item: T) {
        list.add(item)
    }

    fun peek(): T = list.last()
    fun peekOrNull(): T? = list.lastOrNull()
    fun peek(n: Int): List<T> = list.subList(list.size - n, list.size)

    fun pop(): T = list.removeLast()
    fun pop(n: Int): List<T> {
        val removed = mutableListOf<T>()
        for (i in 0 until n) {
            removed.add(list.removeLast())
        }
        return removed
    }
}
