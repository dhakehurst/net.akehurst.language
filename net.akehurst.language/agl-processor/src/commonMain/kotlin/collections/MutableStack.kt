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

class Stack<T>(
        val items : List<T> = emptyList()
) {

    class PopResult<T>(
            val item : T,
            val stack: Stack<T>
    )

    val size: Int get() = this.items.size
    val isEmpty:Boolean get() = this.items.size ==0
    val elements:List<T> get() = this.items

    fun push(item: T) : Stack<T>  = Stack( items + item )
    fun pushAll(items:List<T>) : Stack<T> = Stack(this.items + items)
    fun peek(): T = items.last()
    fun peekOrNull(): T? = items.lastOrNull()
    fun pop(): PopResult<T> = PopResult(this.peek(), Stack( items.subList(0,size-1) ))

}

class MutableStack<T>() {
    private val list = mutableListOf<T>()

    val size: Int get() = this.list.size
    val isEmpty:Boolean get() = this.list.size ==0
    val elements:List<T> get() = this.list

    fun push(item: T) {
        list.add(item)
    }
    fun peek(): T = list.last()
    fun pop(): T = list.removeAt(list.size - 1)

}

