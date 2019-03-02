/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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