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

/**
 * If there are multiple values that contribute to a key,
 * it should be faster to use the keys separately, rather than create a separate 'key' object
 */
internal class MutableMap2IntKeys<V>(val key1Max:Int, val key2Max:Int) {

    companion object {
        const val EMPTY = 0
        //const val ZERO = Int.MAX_VALUE
        //val PRIMES = arrayOf(3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97)
        //fun newHashFactor() = PRIMES[PRIMES.indices.random()]
    }

    fun clear() {
        _size = 0
        _values = arrayOfNulls<Any>(_currentCapacity)
    }

    val size: Int get() = this._size

    operator fun set(key1: Int, key2: Int, value: V) {
        val index = _calcValueIndex(key1, key2)
        //check(0 != index) { "Internal Error: MutableMap2IntKeys index of $key1 and $key2 is 0" } //TODO: could remove this if certain!
        when {
            index < _currentCapacity -> {
                //_keys1[index] = if(0==key1) ZERO else key1
                //_keys2[index] = key2
                _values[index] = value
                this._size++
                //this._growIfNeeded()
                return
            }
            else -> {
                error("Internal Error: map size not big enough for index $key1, $key2")
            }
        }
    }

    operator fun get(key1: Int, key2: Int): V? {
        val index = _calcValueIndex(key1, key2)
        return _values[index] as V
    }

    private var _currentCapacity = (key1Max+5)*key2Max+1
    private var _size: Int = 0
    //private var _keys1 = IntArray(_currentCapacity)
    //private var _keys2 = IntArray(_currentCapacity)
    private var _values = arrayOfNulls<Any>(_currentCapacity)

    private fun _calcHash(k1: Int, k2: Int): Int = k2 + (k1*key2Max)
    private fun _calcValueIndex(k1: Int, k2: Int): Int {
        val hash = _calcHash(k1+5, k2)
        return hash
    }
/*
    private fun _growIfNeeded() {
        if (this._size > _maxBeforeResize) {
            this._grow()
        }
    }

    private fun _grow() {
        val newMap = MutableMap2IntKeys<V>(this._currentCapacity*2, loadFactor)
        for (i in _values.indices) {
            val k1 = _keys1[i]
            if (EMPTY != k1) {
                val value = _values[i] as V
                val k2 = _keys2[i]
                newMap[k1, k2] = value
            }
        }
        this._currentCapacity = newMap._currentCapacity
        this._mask = newMap._mask
        this._maxBeforeResize = newMap._maxBeforeResize
        this._size = newMap._size
        this._keys1 = newMap._keys1
        this._keys2 = newMap._keys2
        this._values = newMap._values
    }
*/
}
