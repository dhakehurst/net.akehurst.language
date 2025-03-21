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

package net.akehurst.language.util

internal fun <E> MutableSet<E>.addIfNotNull(e: E?) = e?.let { this.add(it) }

fun <V> cached(initializer: () -> V) = CachedValue(initializer)

class CachedValue<V>(initializer: () -> V) {
    companion object {
        internal object UNINITIALIZED_VALUE
    }

    private var initializer: (() -> V) = initializer
    private var _value: Any? = UNINITIALIZED_VALUE

    var value: V
        get() {
            if (_value === UNINITIALIZED_VALUE) {
                _value = initializer.invoke()
            }
            @Suppress("UNCHECKED_CAST")
            return _value as V
        }
        set(value) {
            this._value = value
        }

    // no point is making this a delegate..no way to access the delegate to enable reset
    //operator fun getValue(thisRef: Any?, property: KProperty<*>): V = value

    var resetAction:(old:V)->Unit = {}

    fun reset() {
        if (_value==UNINITIALIZED_VALUE) {
            //do nothing
        } else {
            val old = this._value as V
            this._value = UNINITIALIZED_VALUE
            this.resetAction.invoke(old)
        }
    }

}