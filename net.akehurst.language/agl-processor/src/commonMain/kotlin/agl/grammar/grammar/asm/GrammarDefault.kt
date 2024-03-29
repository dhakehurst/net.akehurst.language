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

package net.akehurst.language.agl.grammar.grammar.asm

import net.akehurst.language.agl.collections.*
import net.akehurst.language.api.grammar.*

/**
 * ID -> qualifiedName
 */
class GrammarDefault(
    override val namespace: Namespace,
    override val name: String,
    override val options: List<GrammarOption>
) : GrammarAbstract(namespace, name) {

    companion object {
        fun fromString() {

        }
    }

    override val defaultRule: GrammarRule
        get() = options.firstOrNull { it.name == "defaultGoal" }?.let { findAllResolvedGrammarRule(it.value) }
            ?: this.allResolvedGrammarRule.first { it.isSkip.not() }
}

data class GrammarOptionDefault(
    override val name: String,
    override val value: String
) : GrammarOption

data class GrammarReferenceDefault(
    override val localNamespace: Namespace,
    override val nameOrQName: String
) : GrammarReference {
    override var resolved: Grammar? = null
    override fun resolveAs(resolved: Grammar) {
        this.resolved = resolved
    }
}

abstract class GrammarAbstract(
    override val namespace: Namespace,
    override val name: String
) : Grammar {

    private companion object {
        fun resolve(rule: GrammarRule, inheritedRules: List<GrammarRule>): GrammarRule? {
            return when {
                rule is OverrideRule -> {
                    val overridden = inheritedRules.find { it.name == rule.name }
                    when (overridden) {
                        null -> {
                            // overridden rule not found, so add it as a NormalRule to enable best-effort, though this is an error
                            val nr = NormalRuleDefault(rule.grammar, rule.name, rule.isSkip, rule.isLeaf)
                            nr.rhs = (rule as OverrideRule).overridenRhs
                            nr.rhs.setOwningRule(nr, emptyList())
                            nr
                        }

                        else -> rule
                    }
                }

                else -> {
                    val overriddenBy = inheritedRules.find { it.name == rule.name && it is OverrideRule }
                    when (overriddenBy) {
                        null -> rule
                        else -> overriddenBy
                    }
                }
            }
        }
    }

    override val qualifiedName: String get() = "${namespace.qualifiedName}.$name"

    override val extends = mutableListOf<GrammarReference>()

    override val grammarRule = mutableListOf<GrammarRule>()
    override val preferenceRule = mutableListOf<PreferenceRule>()

    override val allGrammarReferencesInRules: List<GrammarReference> by lazy {
        val refs: List<GrammarReference> = allGrammarRule.flatMap {
            when {
                it is OverrideRule -> {
                    if (it.overridenRhs is NonTerminal) {
                        (it.overridenRhs as NonTerminal).targetGrammar?.let { listOf(it) } ?: emptyList<GrammarReference>()
                    } else {
                        emptyList<GrammarReference>()
                    }
                }

                else -> {
                    it.rhs.allEmbedded.map {
                        it.embeddedGrammarReference
                    }
                }
            }
        }

        this.extends + refs
    }

    override val extendsResolved: List<Grammar> get() = extends.mapNotNull { it.resolved }

    override val allExtends: OrderedSet<GrammarReference> by lazy {
        val inherited = extends.flatMap { it.resolved?.allExtends ?: orderedSetOf() }.toOrderedSet()
        inherited + extends
    }

    override val allExtendsResolved: OrderedSet<Grammar>
        get() {
            return allExtends.flatMap {
                it.resolved?.allExtendsResolved ?: orderedSetOf()
            }.toOrderedSet() + extendsResolved
        }

    override val directInheritedGrammarRule: List<GrammarRule> by lazy {
        val inheritedRules = this.extends.flatMap { it.resolved?.grammarRule ?: emptyList() }
        inheritedRules
    }

    override val allGrammarRule: List<GrammarRule> by lazy {
        val inheritedRules = this.extends.flatMap { it.resolved?.allGrammarRule ?: emptyList() }
        inheritedRules + grammarRule
    }

    override val directInheritedResolvedGrammarRule: OrderedSet<GrammarRule> by lazy {
        val resolvedRules = linkedMapOf<String, GrammarRule>() //use linkedMap so order stays the same
        val inheritedRules = this.extends.flatMap { it.resolved?.resolvedGrammarRule ?: emptyList() }
        inheritedRules.forEach { rule ->
            val r = resolve(rule, inheritedRules) ?: rule
            resolvedRules[r.name] = r
        }
        resolvedRules.values.toOrderedSet()
    }

    override val allInheritedResolvedGrammarRule: OrderedSet<GrammarRule> by lazy {
//        val resolvedRules = linkedSetOf<GrammarRule>() //use linkedMap so order stays the same
//        val inheritedRules = this.extends.flatMap { it.resolved?.allResolvedGrammarRule ?: emptyList() }
//        inheritedRules.forEach { rule ->
//            val r = resolve(rule, inheritedRules) ?: rule
//            resolvedRules.add(r)
//        }
//        resolvedRules.toOrderedSet()
        this.extends.flatMap { it.resolved?.allResolvedGrammarRule ?: emptyList() }.toOrderedSet()
    }

    override val resolvedGrammarRule: OrderedSet<GrammarRule> by lazy {
        val resolvedRules = linkedMapOf<String, GrammarRule>() //use linkedMap so order stays the same
        val inheritedRules = emptyList<GrammarRule>()
        this.grammarRule.forEach { rule ->
            val r = resolve(rule, inheritedRules) ?: rule
            resolvedRules[r.name] = r
        }
        resolvedRules.values.toOrderedSet()
    }

    override val allResolvedGrammarRule: OrderedSet<GrammarRule> by lazy {
        val resolvedRules = linkedMapOf<String, GrammarRule>() //use linkedMap so order stayes the same
        val inheritedRules = this.extends.flatMap { it.resolved?.allResolvedGrammarRule ?: emptyList() }

        inheritedRules.forEach { rule ->
            val r = resolve(rule, inheritedRules) ?: rule
            resolvedRules[r.name] = r
        }
        this.grammarRule.forEach { rule ->
            val r = resolve(rule, inheritedRules) ?: rule
            resolvedRules[r.name] = r
        }
        resolvedRules.values.toOrderedSet()
    }

    override val allResolvedTerminal: Set<Terminal> by lazy {
        this.allResolvedGrammarRule.toSet().flatMap {
            when (it.isLeaf) {
                true -> setOf(it.compressedLeaf)
                false -> it.rhs.allTerminal
            }
        }.toSet()
    }

    override val allResolvedSkipTerminal: Set<Terminal> by lazy {
        this.allResolvedGrammarRule
            .filter { it.isSkip }
            .flatMap { it.rhs.allTerminal }
            .toSet()
    }

    override val allResolvedNonTerminalRule: Set<GrammarRule> by lazy {
        this.allResolvedGrammarRule.filter { it.isLeaf.not() }.toSet()
    }

    override val allResolvedPreferenceRuleRule: OrderedSet<PreferenceRule> by lazy {
        val rules = this.extends.flatMap { it.resolved?.allResolvedPreferenceRuleRule ?: emptyList() }.toMutableOrderedSet()
        rules + this.preferenceRule
    }

    override val allResolvedEmbeddedRules: Set<Embedded> by lazy {
        this.allResolvedGrammarRule.flatMap { it.rhs.allEmbedded }.toSet()
    }

    override val allResolvedEmbeddedGrammars: Set<Grammar> by lazy {
        val egs = this.allResolvedEmbeddedRules.mapNotNull { it.embeddedGrammarReference.resolved }.toSet()
        egs + egs.flatMap { it.allResolvedEmbeddedGrammars }.toSet()//FIXME: recursion
    }

    override fun findAllSuperGrammarRule(ruleName: String): List<GrammarRule> {
        val rules = this.extends.flatMap { it.resolved?.allGrammarRule ?: emptyList() }.toMutableOrderedSet()
        return rules.filter { it.grammar != this && it.name == ruleName }
    }

    override fun findAllGrammarRuleList(ruleName: String): List<GrammarRule> =
        this.allGrammarRule.filter { it.name == ruleName }

    override fun findAllResolvedGrammarRule(ruleName: String): GrammarRule? {
        val all = this.allResolvedGrammarRule.filter { it.name == ruleName }
        return when {
            all.isEmpty() -> null
            all.toSet().size > 1 -> error("More than one rule named '${ruleName}' in grammar '${this.name}', have you remembered the 'override' modifier")
            else -> all.first()
        }
    }

    override fun findAllResolvedTerminalRule(terminalPattern: String): Terminal {
        val all = this.allResolvedTerminal.filter { it.value == terminalPattern }
        when {
            all.isEmpty() -> error("$terminalPattern in Grammar(${this.name}).findTerminalRule")
            all.size > 1 -> error("More than one rule named $terminalPattern in Grammar(${this.name}).findTerminalRule")
        }
        return all.first()
    }

    override fun hashCode(): Int = this.qualifiedName.hashCode()
    override fun equals(other: Any?): Boolean = when (other) {
        !is Grammar -> false
        else -> this.qualifiedName == other.qualifiedName
    }

    override fun toString(): String = qualifiedName

}
