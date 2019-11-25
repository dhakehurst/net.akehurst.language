package net.akehurst.language.agl.runtime.structure

fun <K,V> lazyMapNonNull(accessor: (K) -> V) = LazyMapNonNull(accessor)

class LazyMapNonNull<K,V>(val accessor: (K) -> V) : Map<K,V> {

    val map = mutableMapOf<K,V>()

    override operator fun get(key:K): V {
        return if(map.containsKey(key)) {
            map.get(key) ?: throw Exception("This map should never contain nulls")
        } else {
            val v = accessor.invoke(key)
            map[key] = v
            v
        }
    }

    override val entries: Set<Map.Entry<K, V>>
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
