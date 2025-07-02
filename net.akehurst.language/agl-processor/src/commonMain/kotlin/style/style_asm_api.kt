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

package net.akehurst.language.style.api

import net.akehurst.language.base.api.*
import net.akehurst.language.regex.api.EscapedPattern

interface AglStyleModel : Model<StyleNamespace, StyleSet> {

}

interface StyleNamespace : Namespace<StyleSet> {
    val styleSet: List<StyleSet>
}

interface StyleSetReference {
    val localNamespace: StyleNamespace
    val nameOrQName: PossiblyQualifiedName
    val resolved: StyleSet?

    fun resolveAs(resolved: StyleSet)
}

interface StyleSet : Definition<StyleSet> {
    override val namespace: StyleNamespace
    val extends: List<StyleSetReference>
    val rules : List<AglStyleRule>
    val metaRules: List<AglStyleMetaRule>
    val tagRules: List<AglStyleTagRule>
}

interface AglStyleRule : Formatable {
    val declaration: Map<String, AglStyleDeclaration>
}

interface AglStyleMetaRule : AglStyleRule {
    val pattern: EscapedPattern
}

interface AglStyleTagRule : AglStyleRule {
    val selector: List<AglStyleSelector>
}

enum class AglStyleSelectorKind { SPECIAL, LITERAL, PATTERN, RULE_NAME }
data class AglStyleSelector(
    val value: String,
    val kind: AglStyleSelectorKind
)

data class AglStyleDeclaration(
    val name: String,
    val value: String
)