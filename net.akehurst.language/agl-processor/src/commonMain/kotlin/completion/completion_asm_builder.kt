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

package net.akehurst.language.completion.builder

import net.akehurst.language.api.processor.CompletionItemKind
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.asm.OptionHolderDefault
import net.akehurst.language.completion.api.*
import net.akehurst.language.completion.asm.*

@DslMarker
annotation class CompletionDomainDslMarker

fun completionDomain(name: String, init: CompletionDomainBuilder.() -> Unit): AglCompletionDomain {
    val b = CompletionDomainBuilder(SimpleName(name))
    b.init()
    return b.build()
}

@CompletionDomainDslMarker
class CompletionDomainBuilder(
    val name: SimpleName
) {
    private val _options = mutableMapOf<String, String>()
    private val _domain = AglCompletionDomainDefault(name, OptionHolderDefault(null, _options))

    fun namespace(qualifiedName: String, init: CompletionNamespaceBuilder.() -> Unit): CompletionNamespace {
        val b = CompletionNamespaceBuilder(QualifiedName(qualifiedName))
        b.init()
        val ns = b.build()
        _domain.addNamespace(ns)
        return ns
    }

    fun build(): AglCompletionDomain = _domain
}

@CompletionDomainDslMarker
class CompletionNamespaceBuilder(
    qualifiedName: QualifiedName
) {
    private val _namespace = CompletionNamespaceDefault(qualifiedName)

    fun completion(name: String, init: CompletionSetBuilder.() -> Unit): CompletionSet {
        val b = CompletionSetBuilder(_namespace, SimpleName(name))
        b.init()
        val set = b.build()
        _namespace.addDefinition(set)
        return set
    }

    fun build(): CompletionNamespace = _namespace
}

@CompletionDomainDslMarker
class CompletionSetBuilder(
    private val namespace: CompletionNamespace,
    private val name: SimpleName
) {
    private val _rules = mutableListOf<CompletionRule>()

    /** `[in <context>] <forItem> -> add [ items ]` */
    fun add(forItem: String, context: String? = null, init: CompletionItemsBuilder.() -> Unit) {
        val b = CompletionItemsBuilder()
        b.init()
        _rules.add(CompletionRuleDefault(context, forItem, CompletionActionAddDefault(b.build())))
    }

    /** `[in <context>] <forItem> -> replace [ items ]` */
    fun replace(forItem: String, context: String? = null, init: CompletionItemsBuilder.() -> Unit) {
        val b = CompletionItemsBuilder()
        b.init()
        _rules.add(CompletionRuleDefault(context, forItem, CompletionActionReplaceDefault(b.build())))
    }

    /** `[in <context>] <forItem> -> hide` */
    fun hide(forItem: String, context: String? = null) {
        _rules.add(CompletionRuleDefault(context, forItem, CompletionActionHideDefault))
    }

    /** `[in <context>] <forItem> -> decorate { .. }`, a null value keeps the generated value */
    fun decorate(forItem: String, context: String? = null, label: String? = null, text: String? = null, description: String? = null) {
        _rules.add(CompletionRuleDefault(context, forItem, CompletionActionDecorateDefault(label, text, description)))
    }

    /** `[in <context>] <forItem> -> depth <depth>` */
    fun depth(forItem: String, depth: Int, context: String? = null) {
        _rules.add(CompletionRuleDefault(context, forItem, CompletionActionDepthDefault(depth)))
    }

    fun build(): CompletionSet = CompletionSetDefault(namespace, name, OptionHolderDefault(null, emptyMap()), _rules)
}

@CompletionDomainDslMarker
class CompletionItemsBuilder {
    private val _items = mutableListOf<CompletionItemDefinition>()

    /** a label defaults to the text */
    fun item(text: String, label: String = text, description: String = "", kind: CompletionItemKind = CompletionItemKind.SEGMENT) {
        _items.add(CompletionItemDefinitionDefault(kind, label, text, description))
    }

    fun build(): List<CompletionItemDefinition> = _items
}
