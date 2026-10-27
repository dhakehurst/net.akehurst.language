/*
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.expressions.processor

import net.akehurst.language.types.api.AssociationEnd
import net.akehurst.language.types.api.TypesAssociation

/**
 * In-memory store of the links of binary associations whose ends are owned by the association.
 * Objects are keyed by equals/hashCode: two equal objects are the same object at a link end.
 * Multiplicity is kept at both ends: setting a single-valued end replaces the previous link,
 * and a value linked at an end whose opposite end is single-valued is unlinked from its previous object.
 */
class AssociationLinkStore {

    private class Links {
        /** for each end index, object at that end -> objects at the other end */
        val byEnd = listOf(LinkedHashMap<Any, MutableList<Any>>(), LinkedHashMap<Any, MutableList<Any>>())

        fun linked(atEnd: Int, obj: Any): List<Any> = byEnd[atEnd][obj] ?: emptyList()

        fun add(atEnd: Int, obj: Any, other: Any) {
            val l1 = byEnd[atEnd].getOrPut(obj) { mutableListOf() }
            if (l1.contains(other).not()) l1.add(other)
            val l2 = byEnd[1 - atEnd].getOrPut(other) { mutableListOf() }
            if (l2.contains(obj).not()) l2.add(obj)
        }

        fun removeAll(atEnd: Int, obj: Any) {
            val others = byEnd[atEnd].remove(obj) ?: return
            for (o in others) {
                byEnd[1 - atEnd][o]?.let { it.remove(obj); if (it.isEmpty()) byEnd[1 - atEnd].remove(o) }
            }
        }
    }

    private val _links = mutableMapOf<TypesAssociation, Links>()

    private fun endIndex(association: TypesAssociation, end: AssociationEnd): Int {
        check(2 == association.end.size) { "Links are only supported for binary associations" }
        val i = association.end.indexOf(end)
        check(-1 != i) { "End '${end.endName.value}' is not an end of the association" }
        return i
    }

    private val AssociationEnd.isCollection get() = null != this.collectionTypeName

    /** the object(s) linked to 'obj' (at the other end) at 'end': a list if 'end' is a collection, else the object or null */
    fun get(association: TypesAssociation, end: AssociationEnd, obj: Any): Any? {
        val target = endIndex(association, end)
        val links = _links[association] ?: return if (end.isCollection) emptyList<Any>() else null
        val objs = links.linked(1 - target, obj)
        return if (end.isCollection) objs.toList() else objs.lastOrNull()
    }

    /** sets the links of 'obj' (at the other end) at 'end' to 'value', replacing its previous links at that end */
    fun set(association: TypesAssociation, end: AssociationEnd, obj: Any, value: Any?) {
        val target = endIndex(association, end)
        val source = 1 - target
        val links = _links.getOrPut(association) { Links() }
        links.removeAll(source, obj)
        val values: List<Any> = when (value) {
            null -> emptyList()
            is Collection<*> -> value.filterNotNull()
            else -> listOf(value)
        }
        val sourceEnd = association.end[source]
        for (v in values) {
            // the opposite end (navigated from v back to obj) holds at most one object, unless it is a collection
            if (sourceEnd.isCollection.not()) links.removeAll(target, v)
            links.add(source, obj, v)
        }
    }

    /** removes all links */
    fun clear() {
        _links.clear()
    }
}
