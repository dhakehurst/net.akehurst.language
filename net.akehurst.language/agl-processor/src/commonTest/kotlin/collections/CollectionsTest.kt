/**
 * Copyright (C) 2022 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

internal object CollectionsTest {
    fun <E> Set<E>.matches(other: Set<E>, matches: (t: E, o: E) -> Boolean): Boolean {
        val thisList = this.toList()
        val foundThis = mutableListOf<E>()
        val foundOther = mutableListOf<E>()
        for (i in this.indices) {
            val thisElement = thisList[i]
            val otherElement = other.firstOrNull { matches(thisElement, it) }
            if (null != otherElement) {
                foundThis.add(thisElement)
                foundOther.add(otherElement)
            }
        }
        return foundThis.size == foundOther.size && foundThis.size == thisList.size
    }

    fun <E> List<E>.matches(other: List<E>, matches: (t: E, o: E) -> Boolean): Boolean {
        return if (this.size != other.size) {
            false
        } else {
            var match = true
            for (i in this.indices) {
                val thisElement = this[i]
                val otherElement = other[i]
                val b = matches(thisElement, otherElement)
                match = match && b
            }
            match
        }
    }

}