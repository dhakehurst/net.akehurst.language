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
import net.akehurst.kotlinx.utils.Indent
import net.akehurst.language.regex.api.EscapedPattern

interface AglStyleDomain : Domain<StyleNamespace, StyleSet> {

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
    /** 'import-types ns': namespaces of types that semantic selectors can name with a simple name */
    val importTypes: List<Import>
    val rules : List<AglStyleRule>
    val metaRules: List<AglStyleMetaRule>
    val tagRules: List<AglStyleTagRule>

    val allRules: List<AglStyleRule>

    fun addImportType(value: Import)
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

/**
 * TYPE, IDENTIFY and REFERS_TO are semantic selectors ('type T', 'identify T', 'refers-to T'),
 * their value is the semantic tag they match (see SemanticTag)
 */
enum class AglStyleSelectorKind { SPECIAL, LITERAL, PATTERN, RULE_NAME, TYPE, IDENTIFY, REFERS_TO }
data class AglStyleSelector(
    val value: String,
    val kind: AglStyleSelectorKind
) {
    /** for a semantic selector: the qualified name of its type, set by semantic analysis (the value keeps the name as written) */
    var resolvedTypeName: QualifiedName? = null
        private set

    fun resolveTypeAs(qualifiedName: QualifiedName) {
        resolvedTypeName = qualifiedName
    }

    /**
     * what a token's tag must equal for this selector to match it:
     * for a resolved semantic selector '<kind> <qualified type name>' (see SemanticTag), otherwise the value
     */
    val matchTag: String
        get() = resolvedTypeName?.let { "${value.substringBefore(' ')} ${it.value}" } ?: value
}

data class AglStyleDeclaration(
    val name: String,
    val value: String
)