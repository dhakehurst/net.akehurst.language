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
 * I - type of list Items
 * S - type of list Separators
 */
interface ListSeparated<I, S> : List<Any?> {
    val items: List<I>
    val separators: List<S>

    /**
     * all elements (items and separators)
     */
    val elements: List<Any?>
}

interface MutableListSeparated<I, S> : ListSeparated<I, S>, MutableList<Any?> {
    override val items: MutableList<I>
    override val separators: MutableList<S>
}

inline fun <reified I, reified S> emptyListSeparated(): ListSeparated<I, S> = listSeparatedOf() //TODO: maybe create EmptyListSeparated
inline fun <reified I, reified S> listSeparatedOf(vararg elements: Any?): ListSeparated<I, S> = ListSeparatedArrayList(elements.toList())
inline fun <reified I, reified S> mutableListSeparated(): MutableListSeparated<I, S> = MutableListSeparatedArrayList()

inline fun <reified I, reified S> List<*>.toSeparatedList(): ListSeparated<I, S> = ListSeparatedArrayList<I, S>(this)

class ListSeparatedArrayList<I, S>(
    override val elements: List<Any?>
) : AbstractList<Any?>(), ListSeparated<I, S> {

    override val size: Int get() = elements.size

    override fun get(index: Int): Any? = elements[index]

    override val items: List<I>
        get() = elements.filterIndexed { index, _ -> index % 2 == 0 } as List<I>

    override val separators: List<S>
        get() = elements.filterIndexed { index, _ -> index % 2 == 1 } as List<S>
}

class MutableListSeparatedArrayList<I, S> : AbstractMutableList<Any?>(), MutableListSeparated<I, S> {

    override val elements = mutableListOf<Any?>()

    override val size: Int get() = elements.size

    override fun get(index: Int): Any? = elements[index]

    override fun set(index: Int, element: Any?): Any? = elements.set(index, element)


    override fun add(index: Int, element: Any?) {
        elements.add(index, element)
    }

    override fun removeAt(index: Int): Any? = elements.removeAt(index)

    override val items: MutableList<I>
        get() = TODO("not implemented")

    override val separators: MutableList<S>
        get() = TODO("not implemented")
}