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
 * Clone of original, all key and values of original are in the clone.
 * Modifying the original will not modify the clone.
 * Collections used as Keys or Values are not cloned, modifying them in the clone may modify the originals.
 */
expect fun <K, V> Map<K, V>.clone(): Map<K, V>

/**
 * clone of original, all key and values of original are in the clone
 * modifying the original will not modify the clone
 * Collections used as Keys or Values are cloned, modifying them in the clone will not modify the originals.
 */
fun <K, V> Map<K, V>.deepClone(): Map<K, V> {
    val result = HashMap<K, V>()
    for (me in this.entries) {
        val ok = me.key
        val ov = me.value
        val k = when (ok) {
            is Map<*, *> -> ok.deepClone() as K
            is Collection<*> -> ok.deepClone() as K
            else -> ok
        }
        val v = when (ov) {
            is Map<*, *> -> ov.deepClone() as V
            is Collection<*> -> ov.deepClone() as V
            else -> ov
        }
        result[k] = v
    }
    return result
}

fun <E> Collection<E>.deepClone(): Collection<E> {
    val result = when (this) {
        is Set<E> -> HashSet<E>()
        is List<E> -> ArrayList<E>()
        else -> error("Cannot clone unknown type in deepClone")
    }
    for (oe in this) {
        val e = when (oe) {
            is Collection<*> -> oe.deepClone() as E
            is Map<*, *> -> oe.deepClone() as E
            else -> oe
        }
        result.add(e)
    }
    return result
}