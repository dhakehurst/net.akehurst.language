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

import net.akehurst.language.agl.util.Debug

class GraphStructuredStack<E>(
    val _growingHeadHeap: BinaryHeap<E, E>,
    previous: MutableMap<E, MutableSet<E>>,
    count: MutableMap<E, Int>
) {
    constructor(headHeap: BinaryHeap<E, E>) : this(headHeap, hashMapOf<E, MutableSet<E>>(), hashMapOf<E, Int>()) //no need to preserve insertion order

    // TODO: is the fifo version faster ? it might help with processing heads in a better order!
    // to order the heads efficiently so we grow them in the required order
    //private val _growingHeadHeap: BinaryHeap<GrowingNodeIndex, GrowingNodeIndex> = headHeap

    private val _previous = previous

    /**
     * when (count[node]) {
     *   null -> node is not in the GSS
     *   0 -> node is the head of a stack
     *   else -> count is the number of stacks that node is in (not as the head of a stack)
     * }
     */
    private val _count = count

    val roots: List<E> get() = this._count.entries.filter { it.value == 0 }.map { it.key }

    val isEmpty: Boolean get() = _previous.isEmpty() && _count.isEmpty() && _growingHeadHeap.isEmpty()
    val numberOfHeads get() = this._growingHeadHeap.size
    val hasNextHead: Boolean get() = this._growingHeadHeap.isNotEmpty()
    val peekFirstHead: E? get() = this._growingHeadHeap.peekRoot
    val heads get() = this._growingHeadHeap.toList() //TODO: maybe not performant
    fun extractRoot() = this._growingHeadHeap.extractRoot()!!

    fun clear() {
        this._count.clear()
        this._previous.clear()
        this._growingHeadHeap.clear()
    }

    fun root(head: E): Boolean {
        //_previous[head] = hashSetOf()
        //_count[head] = 0
        this._growingHeadHeap[head] = head
        val set = _previous[head]
        val createdNewHead = if (null == set) {
            _previous[head] = hashSetOf()
            this._count[head] = 0
            true
        } else {
            false
        }
        //TODO: should we need this? or should above be sufficient
        // val hc = this._count[head]
        // if (null == hc) {
        //     _previous[head] = hashSetOf()
        //     this._count[head] = 0
        // } else {
        //     this._count[head] = hc + 1
        // }
        if (Debug.CHECK) check()
        return createdNewHead
    }

    fun pushTriple(cur: E, prev: E?, rhd: E?) {
        when {
            null == prev -> {
                this.root(cur) // root adds to head to heap
            }

            null == rhd -> {
                this._growingHeadHeap[cur] = cur
                this.push(prev, cur)
            }

            else -> {
                this._growingHeadHeap[cur] = cur
                this.push(rhd, prev)
                this.push(prev, cur)
            }
        }
    }

    /**
     * currentHead becomes the previous for nextHead
     * currentHead <-- nextHead
     * will not create new head if nextHead already exists somewhere in the GSS
     * returns true if created a new head
     */
    fun push(currentHead: E, nextHead: E) {
        this._growingHeadHeap.remove(currentHead)
        var set = _previous[nextHead]
        if (null == set) {
            set = hashSetOf()
            _previous[nextHead] = set
            _count[nextHead] = 0
            this._growingHeadHeap[nextHead] = nextHead
        } else {
            // nothing
        }
        val added = set.add(currentHead)
        if (added) {
            val hc = this._count[currentHead]
            if (null == hc) { // should not happen !
                _previous[currentHead] = hashSetOf()
                this._count[currentHead] = 1
            } else {
                this._count[currentHead] = hc + 1
            }
        } else {
            // head is already previous of next
        }
        if (Debug.CHECK) check()
    }

    fun contains(head: E): Boolean = _previous.containsKey(head)

    fun peekPrevious(head: E): Set<E> = _previous[head]?.toMutableSet() ?: emptySet()

    fun pop(head: E): Set<E> {
        this._growingHeadHeap.remove(head)
        val count = this._count[head]
        return if (null == count) {
            emptySet()
        } else {
            if (count == 0) {
                // node is a head, so remove it
                _count.remove(head)
                val prev = _previous.remove(head)!!.toMutableSet()
                prev.forEach {
                    val c = this._count[it]!!
                    this._count[it] = c - 1
                }
                if (Debug.CHECK) check()
                prev
            } else {
                val prev = _previous[head]!!.toMutableSet()
                // head is not a head of the GSS, just return the previous nodes
                // do not deduce from count of prev, because head is not removed
                if (Debug.CHECK) check()
                prev
            }
        }
    }

    private fun check() {
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

    /**
     * return true if stack was dropped
     * if the head is part of other stacks it will not be dropped
     */
    fun dropStack(head: E, ifDropped: (n: E) -> Unit): Boolean {
        val d = this._growingHeadHeap.remove(head)
        return this.removeStack(head, ifDropped)
    }

    private fun removeStack(node: E, ifDropped: (n: E) -> Unit): Boolean {
        val count = this._count[node]
        return when (count) {
            null -> true // node is not in this GSS, so result is equivalent of stack removed
            0 -> {
                // head is the head of a stack and not in any other stack
                // so remove it and decrement count for each of its prevs
                _count.remove(node)
                val prev = _previous.remove(node)!!
                prev.forEach {
                    val c = this._count[it]!!
                    this._count[it] = c - 1
                    removeStack(it, ifDropped)
                }
                ifDropped(node)
                true
            }

            else -> false // node is not a head so stack not removed
        }
    }

    /*
        fun clone(): GraphStructuredStack<E> {
            val previous = HashMap<E, MutableSet<E>>(this._previous.size)
            for (e in this._previous) {
                previous[e.key] = e.value.toMutableSet()
            }
            val heapClone = this._growingHeadHeap.clone()
            val clone = GraphStructuredStack<E>(heapClone, previous, HashMap(this._count))
            return clone
        }
    */
    private fun prevOfToString(n: E): String {
        val prev = this.peekPrevious(n).toList()
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

