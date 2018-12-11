package net.akehurst.language.processor


class Formatter {
    /*
    private val formatFunctions: MutableMap<Class<*>, Function<Any, String>>

    fun OglFormatter(): ??? {
        this.formatFunctions = HashMap<Class<*>, Function<Any, String>>()
        this.registerMethods()
    }

    internal fun registerMethods() {
        for (m in this.javaClass.getDeclaredMethods()) {
            if (1 == m.getParameterTypes().size) {
                val t = m.getParameterTypes()[0]
                val func = { o ->
                    try {
                        return m.invoke(this, o)
                    } catch (e: Exception) {
                        return e.message
                    }
                }
                this.formatFunctions[t] = func
            }
        }
    }

    fun format(`object`: Any): String {
        val c = `object`.javaClass

        for ((key, func) in this.formatFunctions) {
            if (key.isAssignableFrom(c)) {
                return func.apply(`object`)
            }
        }
        return `object`.toString()
    }
    */
}