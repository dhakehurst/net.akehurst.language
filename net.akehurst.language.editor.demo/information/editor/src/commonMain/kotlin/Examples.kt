/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.editor.information

object Examples {

    val map = mutableMapOf<String, Example>()

    operator fun get(key: String): Example {
        return this.map[key]!!
    }

    fun add(eg:Example) {
        this.map[eg.id] = eg
    }

    fun add(
            id: String,
            label: String,
            sentence: String,
            grammar: String,
            style: String,
            format: String
    ) {
        this.map[id] = Example(id, label, sentence, grammar, style, format)
    }
}

class Example(
        val id: String,
        val label: String,
        val sentence: String,
        val grammar: String,
        val style: String,
        val format: String
)