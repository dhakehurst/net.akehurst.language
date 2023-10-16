/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.akehurst.language.api.style

interface AglStyleModel {
    val rules: List<AglStyleRule>
}

data class AglStyleRule(
    val selector: List<AglStyleSelector>
) {
    var styles = mutableMapOf<String, AglStyle>()

    fun getStyle(name: String): AglStyle? {
        return this.styles[name]
    }

    fun toCss(): String {
        return """
            ${this.selector.joinToString(separator = ", ") { it.value }} {
                ${this.styles.values.joinToString(separator = "\n") { it.toCss() }}
            }
         """.trimIndent()
    }
}

enum class AglStyleSelectorKind { LITERAL, PATTERN, RULE_NAME, META }
data class AglStyleSelector(
    val value: String,
    val kind: AglStyleSelectorKind
)

data class AglStyle(
    val name: String,
    val value: String
) {
    fun toCss() = "$name : $value ;"
}