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

class GraphStructuredStack<E> {

    private val _previous = hashMapOf<E, MutableSet<E>>() //no need to preserve insertion order
    private val _count = hashMapOf<E, Int>() //no need to preserve insertion order

    fun clear() {
        this._previous.clear()
    }

    fun root(head: E) {
        _previous[head] = mutableSetOf()
        _count[head] = 0
    }

    /**
     * returns true if added a new head
     */
    fun push(head: E, next: E): Boolean {
        var set = _previous[next]
        val newHead = if (null == set) {
            set = mutableSetOf()
            _previous[next] = set
            _count[next] = 0
            true
        } else {
            false
        }
        set.add(head)
        val hc = this._count[head]
        if (null==hc) {
            _previous[head] = mutableSetOf()
            this._count[head] = 1
        } else {
            this._count[head] = hc+1
        }
        return newHead
    }

    fun contains(node:E):Boolean = _previous.containsKey(node)

    fun peek(head: E): Set<E> = _previous[head] ?: emptySet()

    fun pop(head: E): Set<E> {
        val count = this._count[head]
        return if (null == count) {
            emptySet()
        } else {
            if (count==0) {
                _count.remove(head)
                val prev = _previous.remove(head)!!
                prev.forEach {
                    val c = this._count[it]!!
                    this._count[it] = c - 1
                }
                prev
            } else {
                _previous[head]!!
            }
        }
    }

    override fun toString(): String {
        return this._count.entries.filter { it.value==0 }.joinToString{it.key.toString()}
    }

}

