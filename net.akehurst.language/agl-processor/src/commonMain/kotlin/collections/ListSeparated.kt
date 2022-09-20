package net.akehurst.language.agl.collections

/**
 * I - type of list Items
 * S - type of list Separators
 */
interface ListSeparated<I, S> : List<Any?> {
    val items: List<I>
    val separators: List<S>
}

interface MutableListSeparated<I, S> : ListSeparated<I, S>, MutableList<Any?> {
    override val items: MutableList<I>
    override val separators: MutableList<S>
}

inline fun <reified I, reified S> emptyListSeparated(): ListSeparated<I, S> = listSeparatedOf() //TODO: maybe create EmptyListSeparated
inline fun <reified I, reified S> listSeparatedOf(vararg elements: Any?): ListSeparated<I, S> = ListSeparatedArrayList(elements.toList())
inline fun <reified I, reified S> mutableListSeparated(): MutableListSeparated<I, S> = MutableListSeparatedArrayList()

class ListSeparatedArrayList<I, S>(val elements: List<Any?>) : AbstractList<Any?>(), ListSeparated<I, S> {

    override val size: Int get() = elements.size

    override fun get(index: Int): Any? = elements[index]

    override val items: MutableList<I>
        get() = TODO("not implemented")

    override val separators: MutableList<S>
        get() = TODO("not implemented")
}

class MutableListSeparatedArrayList<I, S> : AbstractMutableList<Any?>(), MutableListSeparated<I, S> {

    val elements = mutableListOf<Any?>()

    override val size: Int get() = elements.size

    override fun get(index: Int): Any? = elements[index]

    override fun set(index: Int, element: Any?): Any? = elements.set(index, element)


    override fun add(index: Int, element: Any?) {
        elements.add(index, element)
    }

    override fun removeAt(index: Int): Any? = elements.removeAt(index)

    override val items: MutableList<I>
        get() = TODO("not implemented")

    override val separators: MutableList<S>
        get() = TODO("not implemented")
}