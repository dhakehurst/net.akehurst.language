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

    private val _previous = mutableMapOf<E,MutableSet<E>>()

    fun clear() {
        this._previous.clear()
    }

    fun root(head:E) {
        _previous[head] = mutableSetOf()
    }

    fun push(head:E, next:E) {
        var set = _previous[next]
        if (null==set) {
            set = mutableSetOf()
            _previous[next] = set
        }
        set.add(head)
    }

    fun peek(head:E): Set<E> = _previous[head] ?: emptySet()

    fun pop(head:E): Set<E> = _previous.remove(head) ?: emptySet()


}

