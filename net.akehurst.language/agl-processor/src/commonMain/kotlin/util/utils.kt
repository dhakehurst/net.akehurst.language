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

import net.akehurst.language.collections.LazyMap
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty


internal fun <V> cached(initializer: () -> V) = CachedValue(initializer)


class CachedValue<V>(initializer:()->V)  {
    companion object {
        internal object UNINITIALIZED_VALUE
    }

    private var initializer: (() -> V)? = initializer
    private var _value: Any? = UNINITIALIZED_VALUE


    val value:V get() {
        if (_value === UNINITIALIZED_VALUE) {
            _value = initializer!!()
            initializer = null
        }
        @Suppress("UNCHECKED_CAST")
        return _value as V
    }

    // no point is making this a delegate..no way to access the delegate to enable reset
    //operator fun getValue(thisRef: Any?, property: KProperty<*>): V = value

    fun reset() {
        this._value = UNINITIALIZED_VALUE
    }

}