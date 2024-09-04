/*
 * Copyright (C) 2024 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.api.language.style

import net.akehurst.language.api.language.base.Definition
import net.akehurst.language.api.language.base.Model
import net.akehurst.language.api.language.base.Namespace

interface AglStyleModel : Model<StyleNamespace, AglStyleRule> {

}

interface StyleNamespace : Namespace<AglStyleRule> {
    val rules: List<AglStyleRule>
}

interface AglStyleRule : Definition<AglStyleRule> {
    val selector: List<AglStyleSelector>

    val declaration: Map<String, AglStyleDeclaration>

    fun toCss(): String
}

enum class AglStyleSelectorKind { LITERAL, PATTERN, RULE_NAME, META }
data class AglStyleSelector(
    val value: String,
    val kind: AglStyleSelectorKind
)

data class AglStyleDeclaration(
    val name: String,
    val value: String
) {
    fun toCss() = "$name : $value ;"
}