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

import net.akehurst.language.agl.util.ReentrantLockCommon
import kotlin.concurrent.Volatile

internal fun <E> MutableSet<E>.addIfNotNull(e: E?) = e?.let { this.add(it) }

fun <V> cached(initializer: () -> V) = CachedValue(initializer)

/** like [cached] but using the given [lock], e.g. one lock for several values whose initializers use each other */
fun <V> cachedWithLock(lock: ReentrantLockCommon, initializer: () -> V) = CachedValue(initializer, lock)

/**
 * A value that is computed when first used, and computed again after [reset].
 * Safe to use from several threads: the value is computed once (under [lock]), other threads wait for it.
 */
class CachedValue<V>(
    initializer: () -> V,
    private val lock: ReentrantLockCommon = ReentrantLockCommon()
) {
    companion object {
        internal object UNINITIALIZED_VALUE
    }

    private var initializer: (() -> V) = initializer

    @Volatile
    private var _value: Any? = UNINITIALIZED_VALUE

    var value: V
        get() {
            val v = _value
            if (v !== UNINITIALIZED_VALUE) {
                @Suppress("UNCHECKED_CAST")
                return v as V
            }
            return lock.withLock {
                if (_value === UNINITIALIZED_VALUE) {
                    _value = initializer.invoke()
                }
                @Suppress("UNCHECKED_CAST")
                _value as V
            }
        }
        set(value) {
            lock.withLock { this._value = value }
        }

    // no point is making this a delegate..no way to access the delegate to enable reset
    //operator fun getValue(thisRef: Any?, property: KProperty<*>): V = value

    var resetAction:(old:V)->Unit = {}

    fun reset() {
        lock.withLock {
            if (_value === UNINITIALIZED_VALUE) {
                //do nothing
            } else {
                @Suppress("UNCHECKED_CAST")
                val old = this._value as V
                this._value = UNINITIALIZED_VALUE
                this.resetAction.invoke(old)
            }
        }
    }

}
