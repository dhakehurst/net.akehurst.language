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

import kotlin.math.pow

class MapIntTo<V> private constructor( val loadFactor: Double, initialCapacity: Int, val initialiser:(()->V)?=null) {
    constructor(initialCapacityPower:Int=5, loadFactor: Double=0.75, initialiser:(()->V)? = null) : this(loadFactor, (2.0).pow(initialCapacityPower).toInt(), initialiser)

    companion object {
        const val EMPTY = 0
        const val ZERO = Int.MAX_VALUE
        //val PRIMES = arrayOf(3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97)
        //fun newHashFactor() = PRIMES[PRIMES.indices.random()]
    }

    var isInitialised = false
        private set
    val size: Int get() = this._size

    fun setToInitialised(key: Int) {
        if (0 == key) {
            _zeroIsEmpty = false
        } else {
            this._growIfNeeded()
            val index = this._calcIndex(key)
            this._keys[index] = key
            // initialised value already in _values
            this._size++
        }
    }

    operator fun get(key: Int): V? {
        return if (0 == key) {
            if (_zeroIsEmpty) null else _zeroValue
        } else {
            val index = _calcIndex(key)
             when {
                index < 0 -> null
                else -> if (_keys[index]== EMPTY) null else _values[index] as V
            }
        }
    }

    operator fun set(key: Int, value: V) {
        if (0 == key) {
            _zeroValue = value
            _zeroIsEmpty = false
        } else {
            this._growIfNeeded()
            val index = _calcIndex(key)
            _keys[index] = key
            _values[index] = value
            this._size++
        }
    }

    fun clear() {
        _size = 0
        _zeroValue = null
        _zeroIsEmpty = true
        _keys = IntArray(_currentCapacity)
        _values = arrayOfNulls<Any>(_currentCapacity)
    }

    private var _currentCapacity = initialCapacity
    private var _mask: Int = _currentCapacity - 1
    private var _maxBeforeResize = (_currentCapacity * loadFactor).toInt()
    private var _size: Int = 0
    private var _zeroIsEmpty = true
    private var _zeroValue: V? = null
    private var _keys = IntArray(_currentCapacity)
    private var _values = arrayOfNulls<Any>(_currentCapacity)

    init{
        if(null!=initialiser) {
            this._initialise(initialiser)
        }
    }

    private fun _hash(key: Int) = key and _mask
    private fun _calcIndex(key: Int): Int {
        var index = _hash(key)
        var keyIndex = _keys[index]
        while (keyIndex != key && keyIndex != EMPTY) {
            index++
            if (index == _currentCapacity) index = 0
            keyIndex = _keys[index]
        }
        return index
    }

    private fun _initialise(initialiser:()->V) {
        _zeroValue = initialiser()
        for(i in _values.indices) {
            _values[i] = initialiser()
        }
        this.isInitialised = true
    }

    private fun _growIfNeeded() {
        if (this._size > _maxBeforeResize) {
            this._grow()
        }
    }

    private fun _grow() {
        val newMap = MapIntTo<V>(loadFactor, this._currentCapacity shl 2, this.initialiser)
        for (i in _keys.indices) {
            val key = _keys[i]
            if (EMPTY != key) {
                val value = _values[i] as V
                newMap[key] = value
            }
        }
        this._currentCapacity = newMap._currentCapacity
        this._mask = newMap._mask
        this._maxBeforeResize = newMap._maxBeforeResize
        this._size = newMap._size
        //no need to update zero value as part of grow
        this._keys = newMap._keys
        this._values = newMap._values
    }


}