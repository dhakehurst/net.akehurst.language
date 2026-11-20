/**
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

package net.akehurst.language.agl.util

import kotlin.concurrent.Volatile

/**
 * The one (reentrant) lock under which the parser automata (states, transitions, lookahead sets) are built on demand.
 * Several parses may run at the same time (on the JVM, in different threads) with the same automaton.
 * Finding something that is already built does not take the lock, building something new does.
 * One lock for all automata, so that building an automaton that uses an embedded one cannot deadlock.
 * On JS and wasm there is only one thread and a parse does not suspend, so the lock does nothing there.
 */
internal expect object AutomatonBuildLock {
    fun <T> withLock(block: () -> T): T
}

/**
 * A map that may be read while another thread writes to it (writes are done under [AutomatonBuildLock]).
 * Keys and values must not be null.
 */
internal expect fun <K : Any, V : Any> concurrentMutableMapOf(): MutableMap<K, V>

/**
 * like 'lazy', but the value is computed under [AutomatonBuildLock].
 * Use it for a value whose computation builds part of an automaton (and so takes [AutomatonBuildLock]):
 * with a normal 'lazy' a thread could hold the lazy's lock and wait for [AutomatonBuildLock]
 * while another thread holds [AutomatonBuildLock] and waits for the lazy's lock.
 */
internal fun <T> lazyUnderBuildLock(initializer: () -> T): Lazy<T> = LazyUnderBuildLock(initializer)

private object UninitializedValue

private class LazyUnderBuildLock<T>(initializer: () -> T) : Lazy<T> {
    private var _initializer: (() -> T)? = initializer

    @Volatile
    private var _value: Any? = UninitializedValue

    override val value: T
        get() {
            val v1 = _value
            if (v1 !== UninitializedValue) {
                @Suppress("UNCHECKED_CAST")
                return v1 as T
            }
            return AutomatonBuildLock.withLock {
                val v2 = _value
                if (v2 !== UninitializedValue) {
                    @Suppress("UNCHECKED_CAST")
                    v2 as T
                } else {
                    val v3 = _initializer!!.invoke()
                    _value = v3
                    _initializer = null
                    v3
                }
            }
        }

    override fun isInitialized(): Boolean = _value !== UninitializedValue

    override fun toString(): String = if (isInitialized()) value.toString() else "Lazy value not initialized yet."
}

/**
 * A map that may be read while another thread writes to it (writes must be done under [AutomatonBuildLock]).
 * Keeps the order in which keys were first added (like [mutableMapOf]).
 * Each write copies the map, so use it for small maps that are read much more often than written.
 */
internal class CopyOnWriteMap<K, V> {
    @Volatile
    private var _map: Map<K, V> = emptyMap()

    val size: Int get() = _map.size
    val keys: Set<K> get() = _map.keys
    val values: Collection<V> get() = _map.values
    val entries: Set<Map.Entry<K, V>> get() = _map.entries

    operator fun get(key: K): V? = _map[key]
    fun containsKey(key: K): Boolean = _map.containsKey(key)

    operator fun set(key: K, value: V) {
        val m = LinkedHashMap(_map)
        m[key] = value
        _map = m
    }

    fun remove(key: K) {
        if (_map.containsKey(key)) {
            val m = LinkedHashMap(_map)
            m.remove(key)
            _map = m
        }
    }
}

/**
 * A reentrant lock usable from common code.
 * On the JVM a [java.util.concurrent.locks.ReentrantLock]; on JS and wasm (one thread) it does nothing.
 */
expect class ReentrantLockCommon() {
    fun <T> withLock(block: () -> T): T
}
