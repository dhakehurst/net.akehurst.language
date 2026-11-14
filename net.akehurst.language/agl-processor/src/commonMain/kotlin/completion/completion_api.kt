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

package net.akehurst.language.completion.api

import net.akehurst.language.api.processor.CompletionItemKind
import net.akehurst.language.base.api.Definition
import net.akehurst.language.base.api.Domain
import net.akehurst.language.base.api.Namespace

/**
 * The definitions (written in the AGL completion language) that customise the completion items offered for a language.
 *
 * Each [CompletionRule] applies where a grammar rule (or leaf) is expected next in the sentence,
 * optionally only when that is inside another rule (its context).
 */
interface AglCompletionDomain : Domain<CompletionNamespace, CompletionDefinition> {
    /** the rules of all completion sets, in the order they are defined */
    val allCompletionRules: List<CompletionRule>
}

interface CompletionNamespace : Namespace<CompletionDefinition> {
    val completionSet: List<CompletionSet>
}

interface CompletionDefinition : Definition<CompletionDefinition>

interface CompletionSet : CompletionDefinition {
    override val namespace: CompletionNamespace
    val rules: List<CompletionRule>
}

/**
 * `in <context> <forItem> -> <action>`
 */
interface CompletionRule {
    /** name of a rule that must enclose the position (at any level), null if the rule applies anywhere */
    val context: String?

    /** name of the grammar rule (or leaf) that is expected next */
    val forItem: String

    val action: CompletionAction
}

interface CompletionAction

/** add the [items] to the generated items */
interface CompletionActionAdd : CompletionAction {
    val items: List<CompletionItemDefinition>
}

/** leave out the generated items for the rule, and add the [items] */
interface CompletionActionReplace : CompletionAction {
    val items: List<CompletionItemDefinition>
}

/** leave out the generated items for the rule */
interface CompletionActionHide : CompletionAction

/** change the generated items for the rule, a null value keeps the generated value */
interface CompletionActionDecorate : CompletionAction {
    val label: String?
    val text: String?
    val description: String?
}

/** expand the rule to [depth] levels of rule references, instead of the depth given in the options */
interface CompletionActionDepth : CompletionAction {
    val depth: Int
}

interface CompletionItemDefinition {
    val kind: CompletionItemKind
    val label: String
    val text: String
    val description: String
}
