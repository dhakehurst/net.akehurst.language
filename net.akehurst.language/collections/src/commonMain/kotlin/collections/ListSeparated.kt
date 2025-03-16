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

/**
 * E - type of elements
 * I - type of list Items
 * S - type of list Separators
 */
interface ListSeparated<E, I : E, S : E> : List<E> {
    val items: List<I>
    val separators: List<S>

    /**
     * all elements (items and separators)
     */
    val elements: List<E>
}

interface MutableListSeparated<E, I : E, S : E> : ListSeparated<E, I, S>, MutableList<E> {
    override val items: MutableList<I>
    override val separators: MutableList<S>
}

inline fun <reified E, reified I : E, reified S : E> emptyListSeparated(): ListSeparated<E, I, S> = listSeparatedOf() //TODO: maybe create EmptyListSeparated
inline fun <reified E, reified I : E, reified S : E> listSeparatedOf(vararg elements: E): ListSeparated<E, I, S> = ListSeparatedArrayList(elements.toList())
inline fun <reified E, reified I : E, reified S : E> mutableListSeparated(): MutableListSeparated<E, I, S> = MutableListSeparatedArrayList()

inline fun <reified E, reified I : E, reified S : E> List<E>.toSeparatedList(): ListSeparated<E, I, S> = ListSeparatedArrayList<E, I, S>(this)

class ListSeparatedArrayList<E, I : E, S : E>(
    override val elements: List<E>
) : AbstractList<E>(), ListSeparated<E, I, S> {

    override val size: Int get() = elements.size

    override fun get(index: Int): E = elements[index]

    override val items: List<I>
        get() = elements.filterIndexed { index, _ -> index % 2 == 0 } as List<I>

    override val separators: List<S>
        get() = elements.filterIndexed { index, _ -> index % 2 == 1 } as List<S>
}

class MutableListSeparatedArrayList<E, I : E, S : E> : AbstractMutableList<E>(), MutableListSeparated<E, I, S> {

    override val elements = mutableListOf<E>()

    override val size: Int get() = elements.size

    override fun get(index: Int): E = elements[index]

    override fun set(index: Int, element: E): E = elements.set(index, element)


    override fun add(index: Int, element: E) {
        elements.add(index, element)
    }

    override fun removeAt(index: Int): E = elements.removeAt(index)

    override val items: MutableList<I>
        get() = TODO("not implemented")

    override val separators: MutableList<S>
        get() = TODO("not implemented")
}