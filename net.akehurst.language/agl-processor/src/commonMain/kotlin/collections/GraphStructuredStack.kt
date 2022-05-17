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

class GraphStructuredStack<E>(previous:MutableMap<E,MutableSet<E>>, count:MutableMap<E,Int>) {
    constructor(): this(hashMapOf<E, MutableSet<E>>(), hashMapOf<E, Int>()) //no need to preserve insertion order

    companion object {
        const val DO_CHECK = false
    }

    private val _previous = previous
    private val _count = count

    val roots:List<E> = this._count.entries.filter { it.value==0 }.map { it.key }

    fun clear() {
        this._previous.clear()
    }

    fun root(head: E) {
        //_previous[head] = hashSetOf()
        //_count[head] = 0

        //TODO: should we need this? or should above be sufficient
        val hc = this._count[head]
        if (null == hc) {
            _previous[head] = hashSetOf()
            this._count[head] = 0
        } else {
            this._count[head] = hc + 1
        }

        check() //TODO: remove
    }

    /**
     * returns true if added a new head
     */
    fun push(head: E, next: E): Boolean {
        var set = _previous[next]
        val newHead = if (null == set) {
            set = hashSetOf()
            _previous[next] = set
            _count[next] = 0
            true
        } else {
            false
        }
        val added = set.add(head)
        if (added) {
            val hc = this._count[head]
            if (null == hc) {
                _previous[head] = hashSetOf()
                this._count[head] = 1
            } else {
                this._count[head] = hc + 1
            }
        } else {
            // head is already previous of next
        }
        check() //TODO: remove
        return newHead
    }

    fun contains(node: E): Boolean = _previous.containsKey(node)

    fun peek(head: E): Set<E> = _previous[head] ?: emptySet()

    fun pop(node: E): Set<E> {
        val count = this._count[node]
        return if (null == count) {
            emptySet()
        } else {
            if (count == 0) {
                // node is a head, so remove it
                _count.remove(node)
                val prev = _previous.remove(node)!!
                prev.forEach {
                    val c = this._count[it]!!
                    this._count[it] = c - 1
                }
                check() //TODO: remove
                prev
            } else {
                val prev = _previous[node]!!
                // head is not a head of the GSS, just return the previous nodes
                // do not deduce from count of prev, because head is not removed
                check() //TODO: remove
                prev
            }
        }
    }

    fun check() {
        if (DO_CHECK) {
            val next = mutableMapOf<E, MutableSet<E>>()
            this._previous.keys.forEach { next[it] = mutableSetOf() }
            val heads = this._count.entries.filter { it.value == 0 }.map { it.key }
            var check = heads.toList()
            while (check.isNotEmpty()) {
                val p = check.flatMap {
                    val prev = _previous[it]!!
                    prev.forEach { pr -> next[pr]!!.add(it) }
                    prev
                }
                check = p
            }
            if (next.any { (k, v) -> this._count[k] != v.size }) {
                error("GSS is broken")
            }
        }
    }

    fun removeStack(head: E) {
        var count = this._count[head]
        if (null == count) {
            // do nothing
        } else {
            //count -= 1
            //this._count[head] = count
            if (0 == count) {
                _count.remove(head)
                val prev = _previous.remove(head)!!
                prev.forEach {
                    val c = this._count[it]!!
                    this._count[it] = c - 1
                    removeStack(it)
                }
            } else {
                // do nothing
            }
        }
    }

    fun clone(): GraphStructuredStack<E> {
        val previous = HashMap<E, MutableSet<E>>(this._previous.size)
        for(e in this._previous) {
            previous[e.key] = e.value.toMutableSet()
        }
        val clone = GraphStructuredStack<E>(previous, HashMap(this._count))
        return clone
    }

    private fun prevOfToString(n: E): String {
        val prev = this.peek(n).toList()
        return when {
            prev.isEmpty() -> ""
            1 == prev.size -> {
                val p = prev.first()
                " --> $p${this.prevOfToString(p)}"
            }
            else -> {
                val p = prev.first()
                " -${prev.size}-> $p${this.prevOfToString(p)}"
            }
        }
    }

    override fun toString(): String {
        val heads = this._count.entries.filter { it.value == 0 }
        return when {
            heads.isEmpty() -> if (_previous.isEmpty() && _count.isEmpty()) {
                "<empty>"
            } else {
                "<error>"
            }
            else -> heads.joinToString(separator = "\n") { h ->
                val p = this.prevOfToString(h.key)
                "${h.key}$p"
            }
        }
    }

}

