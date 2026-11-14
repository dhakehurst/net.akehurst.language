/*
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.completion.asm

import net.akehurst.kotlinx.utils.Indent
import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.processor.ProcessResultDefault
import net.akehurst.language.api.processor.CompletionItemKind
import net.akehurst.language.api.processor.CompletionString
import net.akehurst.language.api.processor.ProcessResult
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.base.api.Import
import net.akehurst.language.base.api.OptionHolder
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.asm.DefinitionAbstract
import net.akehurst.language.base.asm.DomainAbstract
import net.akehurst.language.base.asm.NamespaceAbstract
import net.akehurst.language.base.asm.OptionHolderDefault
import net.akehurst.language.completion.api.*
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder

class AglCompletionDomainDefault(
    override val name: SimpleName,
    options: OptionHolder = OptionHolderDefault(null, emptyMap()),
    namespaces: List<CompletionNamespace> = emptyList()
) : AglCompletionDomain, DomainAbstract<CompletionNamespace, CompletionDefinition>(namespaces, options) {

    companion object {
        /**
         * parse and check [completionStr] with the AGL completion language.
         * An empty string gives an empty domain.
         */
        fun fromString(sentenceContext: SentenceContext?, completionStr: CompletionString): ProcessResult<AglCompletionDomain> = when {
            completionStr.value.isBlank() -> ProcessResultDefault(AglCompletionDomainDefault(SimpleName("Empty")), processIssues = IssueHolder(LanguageProcessorPhase.ALL))
            else -> {
                val proc = Agl.registry.agl.completion.processor ?: error("Agl Completion language not found!")
                proc.process(
                    sentence = completionStr.value,
                    Agl.options {
                        semanticAnalysis { sentenceContext(sentenceContext) }
                    }
                )
            }
        }
    }

    override val allCompletionRules: List<CompletionRule> get() = namespace.flatMap { ns -> ns.completionSet.flatMap { it.rules } }
}

class CompletionNamespaceDefault(
    override val qualifiedName: QualifiedName,
    options: OptionHolder = OptionHolderDefault(null, emptyMap()),
    import: List<Import> = emptyList()
) : CompletionNamespace, NamespaceAbstract<CompletionDefinition>(options, import) {
    override val completionSet: List<CompletionSet> get() = super.definition.filterIsInstance<CompletionSet>()
}

class CompletionSetDefault(
    override val namespace: CompletionNamespace,
    override val name: SimpleName,
    override val options: OptionHolder = OptionHolderDefault(null, emptyMap()),
    override val rules: List<CompletionRule>
) : CompletionSet, DefinitionAbstract<CompletionDefinition>() {

    override fun asString(indent: Indent, imports: List<Import>): String {
        val sb = StringBuilder()
        sb.append("completion ${name.value} {\n")
        val ni = indent.inc
        rules.forEach { sb.append("$ni${(it as CompletionRuleDefault).asString(ni)}\n") }
        sb.append("$indent}")
        return sb.toString()
    }
}

class CompletionRuleDefault(
    override val context: String?,
    override val forItem: String,
    override val action: CompletionAction
) : CompletionRule {
    fun asString(indent: Indent): String {
        val ctx = context?.let { "in $it " } ?: ""
        val act = when (action) {
            is CompletionActionAdd -> "add ${itemsAsString(action.items, indent)}"
            is CompletionActionReplace -> "replace ${itemsAsString(action.items, indent)}"
            is CompletionActionHide -> "hide"
            is CompletionActionDecorate -> {
                val props = listOfNotNull(
                    action.label?.let { "label := '$it'" },
                    action.text?.let { "text := '$it'" },
                    action.description?.let { "description := '$it'" },
                )
                "decorate { ${props.joinToString(" ")} }"
            }

            is CompletionActionDepth -> "depth ${action.depth}"
            else -> error("Unsupported CompletionAction '${action::class.simpleName}'")
        }
        return "$ctx$forItem -> $act"
    }

    private fun itemsAsString(items: List<CompletionItemDefinition>, indent: Indent): String {
        val ni = indent.inc
        val itemsStr = items.joinToString(separator = ",\n") {
            "$ni${(it as CompletionItemDefinitionDefault).asString()}"
        }
        return "[\n$itemsStr\n$indent]"
    }

    override fun toString(): String = asString(Indent())
}

class CompletionActionAddDefault(override val items: List<CompletionItemDefinition>) : CompletionActionAdd
class CompletionActionReplaceDefault(override val items: List<CompletionItemDefinition>) : CompletionActionReplace
object CompletionActionHideDefault : CompletionActionHide
class CompletionActionDecorateDefault(
    override val label: String?,
    override val text: String?,
    override val description: String?
) : CompletionActionDecorate

class CompletionActionDepthDefault(override val depth: Int) : CompletionActionDepth

data class CompletionItemDefinitionDefault(
    override val kind: CompletionItemKind,
    override val label: String,
    override val text: String,
    override val description: String
) : CompletionItemDefinition {
    fun asString(): String {
        val props = listOfNotNull(
            if (CompletionItemKind.SEGMENT == kind) null else "kind := '${kind.name}'",
            "label := '$label'",
            "text := '$text'",
            if (description.isEmpty()) null else "description := '$description'",
        )
        return "CompletionItem() { ${props.joinToString(" ")} }"
    }
}
