package net.akehurst.language.agl.runtime.structure

fun <K,V> lazyMap(accessor: (K) -> V) = LazyMap(accessor)

class LazyMap<K,V>(val accessor: (K) -> V) : MutableMap<K,V> {

    val map = mutableMapOf<K,V>()

    override operator fun get(key:K): V? {
        return if(map.containsKey(key)) {
            map.get(key)
        } else {
            val v = accessor.invoke(key)
            map[key] = v
            v
        }
    }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = map.entries

    override val keys: Set<K>
        get() = map.keys

    override val size: Int
        get() = map.size

    override val values: Collection<V>
        get() = map.values

    override fun containsKey(key: K): Boolean {
        return map.containsKey(
            key
        )
    }

    override fun containsValue(value: V): Boolean {
        return map.containsValue(value)
    }

    override fun isEmpty(): Boolean {
        return map.isEmpty()
    }

}