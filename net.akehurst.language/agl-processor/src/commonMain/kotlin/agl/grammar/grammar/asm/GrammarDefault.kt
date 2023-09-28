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

import net.akehurst.language.agl.collections.OrderedSet
import net.akehurst.language.agl.collections.plus
import net.akehurst.language.agl.collections.toMutableOrderedSet
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

    // override this so that property is correctly exported/defined in JS and available for serialisation
    //override val rule: MutableList<GrammarRule> get() = super.rule
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

    override val qualifiedName: String get() = "${namespace.qualifiedName}.$name"

    override val extends = mutableListOf<GrammarReference>()

    override val grammarRule = mutableListOf<GrammarRule>()
    override val preferenceRule = mutableListOf<PreferenceRule>()

    override val allResolvedGrammarRule: OrderedSet<GrammarRule> by lazy {
        val rules = this.extends.flatMap { it.resolved?.allResolvedGrammarRule ?: emptyList() }.toMutableOrderedSet()
        this.grammarRule.forEach { rule ->
            when {
                rule.isOverride -> {
                    val overridden = rules.find { it.name == rule.name }
                        ?: error("GrammarRule ${rule.name} is marked as override, but there is no super rule with that name to override.")
                    rules.remove(overridden)
                    rules.add(rule)
                }

                rules.contains(rule) -> { //assumes rule is identified by grammar and rule.name
                    // already got this rule...probably comes from extending same grammar more than once
                    // don't add it twice
                }

                else -> {
                    rules.add(rule)
                }
            }
        }
        rules
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

    override fun findAllSuperNonTerminalRule(ruleName: String): List<GrammarRule> {
        val rules = this.extends.flatMap { it.resolved?.allResolvedGrammarRule ?: emptyList() }.toMutableOrderedSet()
        return rules.filter { it.grammar != this && it.name == ruleName }
    }

    override fun findAllNonTerminalRule(ruleName: String): List<GrammarRule> =
        this.allResolvedGrammarRule.filter { it.name == ruleName }

    override fun findNonTerminalRule(ruleName: String): GrammarRule? {
        val all = this.allResolvedGrammarRule.filter { it.name == ruleName }
        return when {
            all.isEmpty() -> null
            all.toSet().size > 1 -> error("More than one rule named '${ruleName}' in grammar '${this.name}', have you remembered the 'override' modifier")
            else -> all.first()
        }
    }

    override fun findTerminalRule(terminalPattern: String): Terminal {
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
