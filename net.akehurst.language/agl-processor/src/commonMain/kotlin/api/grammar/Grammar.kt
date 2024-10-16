/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.api.grammar

import net.akehurst.language.agl.collections.OrderedSet

interface GrammarItem {
    val grammar: Grammar
}

interface GrammarReference {
    val localNamespace: Namespace
    val nameOrQName: String
    val resolved: Grammar?
    fun resolveAs(resolved: Grammar)
}

interface GrammarOption {
    val name: String
    val value: String
}

/**
 *
 * The definition of a Grammar. A grammar defines a list of rules and may be defined to extend a number of other Grammars.
 *
 */
interface Grammar {

    /**
     *
     * the namespace of this grammar;
     */
    val namespace: Namespace

    /**
     *
     * the name of this grammar
     */
    val name: String

    /**
     * namespace.name
     */
    val qualifiedName: String

    /**
     * the List of grammar references directly extended by this one (non-transitive)
     */
    val extends: List<GrammarReference>

    val extendsResolved: List<Grammar>

    val options: List<GrammarOption>

    val defaultRule: GrammarRule

    val grammarRule: List<GrammarRule>
    val preferenceRule: List<PreferenceRule>

    val allGrammarReferencesInRules: List<GrammarReference>

    /**
     * the OrderedSet of grammars references extended by this one or those it extends (transitive)
     */
    val allExtends: OrderedSet<GrammarReference>

    /**
     * the OrderedSet of grammars extended by this one or those it extends (transitive)
     */
    val allExtendsResolved: OrderedSet<Grammar>

    /**
     * List of all grammar rules that belong to grammars this one extends (non-transitive)
     */
    val directInheritedGrammarRule: List<GrammarRule>

    /**
     * List of all grammar rules (transitive over extended grammars), including those overridden
     * the order of the rules is the order they are defined in with the top of the grammar extension
     * hierarchy coming first (in extension order where more than one grammar is extended)
     */
    val allGrammarRule: List<GrammarRule>

    /**
     * List of all grammar rules that belong to grammars this one extends (non-transitive)
     * with best-effort to resolve repetition and override
     */
    val directInheritedResolvedGrammarRule: OrderedSet<GrammarRule>

    /**
     * List of all grammar rules that belong to grammars this one extends (transitive)
     * with best-effort to resolve repetition and override
     */
    val allInheritedResolvedGrammarRule: OrderedSet<GrammarRule>

    val resolvedGrammarRule: OrderedSet<GrammarRule>

    /**
     * the List of rules defined by this grammar and those that this grammar extends (transitive),
     * with best-effort to handle repetition and overrides
     * the order of the rules is the order they are defined in with the top of the grammar extension
     * hierarchy coming first (in extension order where more than one grammar is extended)
     */
    val allResolvedGrammarRule: OrderedSet<GrammarRule>

    val allResolvedPreferenceRuleRule: OrderedSet<PreferenceRule>

    /**
     * the Set of all non-terminal rules in this grammar and those that this grammar extends
     */
    val allResolvedNonTerminalRule: Set<GrammarRule>

    /**
     * the Set of all terminals in this grammar and those that this grammar extends
     */
    val allResolvedTerminal: Set<Terminal>

    /**
     * the Set of all terminals that are part of skip rules in this grammar and those that this grammar extends
     */
    val allResolvedSkipTerminal: Set<Terminal>

    val allResolvedEmbeddedRules: Set<Embedded>

    val allResolvedEmbeddedGrammars: Set<Grammar>

    /**
     * find rule with given name in all rules that this grammar extends - but not in this grammar
     */
    fun findAllSuperGrammarRule(ruleName: String): List<GrammarRule>

    /**
     * find rule with given name in all rules from this grammar and ones that this grammar extends
     */
    fun findAllGrammarRuleList(ruleName: String): List<GrammarRule>

    fun findAllResolvedGrammarRule(ruleName: String): GrammarRule?

    fun findAllResolvedTerminalRule(terminalPattern: String): Terminal

}
