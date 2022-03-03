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

internal fun <T> mutableQueueOf(vararg elements: T): MutableQueue<T> {
    val stack = MutableQueue<T>()
    elements.forEach { stack.enqueue(it) }
    return stack
}

internal class MutableQueue<T>() {
    private val list = mutableListOf<T>()

    val size: Int get() = this.list.size
    val isEmpty: Boolean get() = this.list.size == 0
    val isNotEmpty: Boolean get() = this.list.size != 0
    val elements: List<T> get() = this.list

    fun enqueue(item: T) { list.add(item) }
    fun peek(): T = list.last()
    fun dequeue(): T = list.removeAt(0)

    override fun toString(): String = this.list.joinToString(separator = ",") { it.toString() }
}