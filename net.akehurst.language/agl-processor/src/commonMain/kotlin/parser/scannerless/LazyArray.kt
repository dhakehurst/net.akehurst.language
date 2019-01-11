package net.akehurst.language.agl.runtime.structure

fun <T> lazyArray(size:Int,accessor: (Int) -> T) = LazyArray(size,accessor)

class LazyArray<T>(size:Int, val accessor: (Int) -> T) {

    val arr = arrayOfNulls<Any?>(size) as Array<T?>

    operator fun get(index: Int): T {
        return arr[index] ?: {
            val v = accessor.invoke(index)
            arr[index] = v
            v
        }.invoke()
    }
}