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
package net.akehurst.language.style.asm

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.StyleString
import net.akehurst.language.base.asm.NamespaceAbstract
import net.akehurst.language.grammar.processor.ContextFromGrammar
import net.akehurst.language.api.processor.ProcessResult
import net.akehurst.language.base.api.*
import net.akehurst.language.base.asm.ModelAbstract
import net.akehurst.language.style.api.*

class AglStyleModelDefault(
    override val name: SimpleName,
    override val namespace: List<StyleNamespace>
) : AglStyleModel, ModelAbstract<StyleNamespace, StyleSet>() {

    companion object {
        //not sure if this should be here or in grammar object
        const val KEYWORD_STYLE_ID = "\$keyword"
        const val NO_STYLE_ID = "\$nostyle"

        //val DEFAULT_NO_STYLE = AglStyleTagRuleDefault(
        //    listOf(AglStyleSelector(NO_STYLE_ID, AglStyleSelectorKind.SPECIAL))
       // ).also {
        //    it.declaration["foreground"] = AglStyleDeclaration("foreground", "black")
        //    it.declaration["background"] = AglStyleDeclaration("background", "white")
        //    it.declaration["font-style"] = AglStyleDeclaration("font-style", "normal")
        //}

        fun fromString(context: ContextFromGrammar, aglStyleModelSentence: StyleString): ProcessResult<AglStyleModel> {
            val proc = Agl.registry.agl.style.processor ?: error("Scopes language not found!")
            return proc.process(
                sentence = aglStyleModelSentence.value,
                options = Agl.options { semanticAnalysis { context(context) } }
            )
        }
    }

}

class StyleNamespaceDefault(
    override val qualifiedName: QualifiedName,
    override val import: List<Import>
) : StyleNamespace, NamespaceAbstract<StyleSet>() {

    override val styleSet: List<StyleSet>  get() = super.definition

}

class AglStyleSetDefault(
    override val namespace: StyleNamespace,
    override val name: SimpleName,
    override val extends: List<StyleSetReference>
) : StyleSet {

    override val qualifiedName: QualifiedName get() = namespace.qualifiedName.append(name)

    override val rules: List<AglStyleRule> = mutableListOf()

    override val metaRules: List<AglStyleMetaRule> get() = rules.filterIsInstance<AglStyleMetaRule>()
    override val tagRules: List<AglStyleTagRule> get() = rules.filterIsInstance<AglStyleTagRule>()

    override fun asString(indent: Indent): String {
        val sb = StringBuilder()
        sb.append("styles ${name.value} {\n")
        val newIndent = indent.inc
        val rules = rules // do not sort, order matters
            .joinToString(separator = "\n") { "$newIndent${it.asString(newIndent)}" }
        sb.append(rules)
        sb.append("\n$indent}")
        return sb.toString()
    }

}

data class StyleSetReferenceDefault(
    override val localNamespace: StyleNamespace,
    override val nameOrQName: PossiblyQualifiedName
) : StyleSetReference {
    override var resolved: StyleSet? = null
    override fun resolveAs(resolved: StyleSet) {
        this.resolved = resolved
    }
}

data class AglStyleMetaRuleDefault(
    override val pattern: Regex
) : AglStyleMetaRule {

    // order matters
    override var declaration = linkedMapOf<String, AglStyleDeclaration>()

    override fun asString(indent: Indent): String {
        val sb = StringBuilder()
        val sel = this.pattern.pattern
        sb.append("$sel {\n")
        val newIndent = indent.inc
        val decls = declaration.values // do not sort, order matters
            .joinToString(separator = "\n") { "$newIndent${it.name}: ${it.value};" }
        sb.append(decls)
        sb.append("\n$indent}")
        return sb.toString()
    }
}

data class AglStyleTagRuleDefault(
    override val selector: List<AglStyleSelector>
) : AglStyleTagRule {

    // order matters
    override var declaration = linkedMapOf<String, AglStyleDeclaration>()

    override fun asString(indent: Indent): String {
        val sb = StringBuilder()
        val sel = this.selector.joinToString { it.value }
        sb.append("$sel {\n")
        val newIndent = indent.inc
        val decls = declaration.values // do not sort, order matters
            .joinToString(separator = "\n") { "$newIndent${it.name}: ${it.value};" }
        sb.append(decls)
        sb.append("\n$indent}")
        return sb.toString()
    }

}