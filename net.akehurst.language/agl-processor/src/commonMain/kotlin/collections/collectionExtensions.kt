/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.collections

internal fun <T> Set<T>.transitiveClosure(accumulate: Boolean = true, function: (T) -> Set<T>): Set<T> {
    var result: MutableSet<T> = this.toMutableSet()
    var newThings: MutableSet<T> = this.toMutableSet()
    var newStuff = true
    while (newStuff) {
        val temp = newThings.toSet()
        newThings.clear()
        for (nt: T in temp) {
            val s: Set<T> = function.invoke(nt)
            newThings.addAll(s)
        }
        if (accumulate) {
            newThings.removeAll(result)
            newStuff = result.addAll(newThings)
        } else {
            newStuff = result != newThings
            result = newThings.toMutableSet() //clone it
        }
    }
    return result
}

internal fun <T> Set<T>.transitiveClosure_old(function: (T) -> Set<T>): Set<T> {
    var result: MutableSet<T> = this.toMutableSet()
    var oldResult: MutableSet<T> = mutableSetOf<T>()
    while (!oldResult.containsAll(result)) {
        oldResult = result.toMutableSet()
        for (nt: T in oldResult) {
            val s: Set<T> = function.invoke(nt)
            result.addAll(s)
        }
    }
    return result
}

internal fun <T> List<T>.transitiveClosure(function: (T) -> List<T>): List<T> {
    var result: MutableList<T> = this.toMutableList()
    var newThings: MutableList<T> = this.toMutableList()
    var newStuff = true
    while (newStuff) {
        val temp = newThings.toSet()
        newThings.clear()
        for (nt: T in temp) {
            val s: List<T> = function.invoke(nt)
            newThings.addAll(s)
        }
        newThings.removeAll(result)
        newStuff = result.addAll(newThings)
    }
    return result
}

internal fun IntArray.including(value:Int) : IntArray {
    return if (this.contains(value)) {
        this
    } else {
        val na = this.copyOf(this.size+1)
        na[this.size] = value
        na
    }
}