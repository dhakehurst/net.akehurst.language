/*
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.expressions.processor

import net.akehurst.language.objectgraph.api.TypedObject

/**
 * A read-only List<TypedObject> over a list of plain values, that types each element only when it is first read.
 * Each element is typed at most once, so reading the same index again gives the same TypedObject,
 * as for a list whose elements were all typed when it was created.
 * Equality and hashCode are those of a List<TypedObject> (from AbstractList), so they do not depend on which elements were read.
 */
internal class LazyTypedList(
    private val raw: List<Any?>,
    private val toTyped: (Any?) -> TypedObject
) : AbstractList<TypedObject>() {

    private val typed = arrayOfNulls<TypedObject>(raw.size)

    override val size: Int get() = raw.size

    override fun get(index: Int): TypedObject {
        val existing = typed[index]
        return when (existing) {
            null -> {
                val t = toTyped.invoke(raw[index])
                typed[index] = t
                t
            }

            else -> existing
        }
    }

    /**
     * maps each element without typing it: [ifTyped] for an element already typed (read), else [ifRaw] for its plain value.
     * [ifRaw] gets the index, so it can still read (and so type) the element if it needs to.
     */
    fun <R> mapTypedOrRaw(ifTyped: (TypedObject) -> R, ifRaw: (index: Int, raw: Any?) -> R): List<R> =
        raw.indices.map { i ->
            val t = typed[i]
            when (t) {
                null -> ifRaw.invoke(i, raw[i])
                else -> ifTyped.invoke(t)
            }
        }
}
