/**
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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
package net.akehurst.language.agl.language.style.asm

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.language.base.DefinitionBlockAbstract
import net.akehurst.language.agl.language.base.NamespaceAbstract
import net.akehurst.language.agl.language.grammar.ContextFromGrammar
import net.akehurst.language.api.language.base.Import
import net.akehurst.language.api.language.base.Indent
import net.akehurst.language.api.language.base.QualifiedName
import net.akehurst.language.api.language.base.SimpleName
import net.akehurst.language.api.language.base.SimpleName.Companion.asSimpleName
import net.akehurst.language.api.language.style.*
import net.akehurst.language.api.processor.ProcessResult

class AglStyleModelDefault(
    override val name: SimpleName,
    namespace: List<StyleNamespace>
) : AglStyleModel, DefinitionBlockAbstract<StyleNamespace, AglStyleRule>(namespace) {

    companion object {
        //not sure if this should be here or in grammar object
        const val KEYWORD_STYLE_ID = "\$keyword"
        const val NO_STYLE_ID = "\$nostyle"

        val STD_NS = StyleNamespaceDefault(QualifiedName("std"), emptyList())
        val DEFAULT_NO_STYLE = AglStyleRuleDefault(
            namespace = STD_NS,
            listOf(AglStyleSelector(NO_STYLE_ID, AglStyleSelectorKind.META))
        ).also {
            it.declaration["foreground"] = AglStyleDeclaration("foreground", "black")
            it.declaration["background"] = AglStyleDeclaration("background", "white")
            it.declaration["font-style"] = AglStyleDeclaration("font-style", "normal")
            STD_NS.addDefinition(it)
        }

        fun fromString(context: ContextFromGrammar, aglStyleModelSentence: String): ProcessResult<AglStyleModel> {
            val proc = Agl.registry.agl.style.processor ?: error("Scopes language not found!")
            return proc.process(
                sentence = aglStyleModelSentence,
                options = Agl.options { semanticAnalysis { context(context) } }
            )
        }
    }


}

class StyleNamespaceDefault(
    qualifiedName: QualifiedName,
    override val import: List<Import>
) : StyleNamespace, NamespaceAbstract<AglStyleRule>(qualifiedName) {

    //override val rules: List<AglStyleRule> = if (_rules.any { it.selector.any { it.value == NO_STYLE_ID } }) {
    //    // NO_STYLE defined
    //    _rules
    //} else {
    //    listOf(DEFAULT_NO_STYLE) + _rules
    //}

    override val rules: List<AglStyleRule> get() = super.definition

}

data class AglStyleRuleDefault(
    override val namespace: StyleNamespace,
    override val selector: List<AglStyleSelector>
) : AglStyleRule {

    override val name: SimpleName
        get() = selector.joinToString(separator = ".") { it.value }.asSimpleName

    override val qualifiedName: QualifiedName
        get() = TODO("not implemented")

    override var declaration = mutableMapOf<String, AglStyleDeclaration>()

    override fun asString(indent: Indent): String {
        TODO("not implemented")
    }

    override fun toCss(): String {
        return """
            ${this.selector.joinToString(separator = ", ") { it.value }} {
                ${this.declaration.values.joinToString(separator = "\n") { it.toCss() }}
            }
         """.trimIndent()
    }
}