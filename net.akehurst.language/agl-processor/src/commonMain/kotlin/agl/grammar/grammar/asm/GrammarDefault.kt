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

import net.akehurst.language.api.grammar.*
import net.akehurst.language.api.processor.LanguageRegistry

class GrammarDefault(
    override val namespace: Namespace,
    override val name: String
) : GrammarAbstract(namespace, name) {

    // override this so that property is correctly exported/defined in JS and available for serialisation
    //override val rule: MutableList<GrammarRule> get() = super.rule
}

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

    override val extends: MutableList<GrammarReference> = mutableListOf<GrammarReference>()

    override val rule: MutableList<GrammarRule> = mutableListOf<GrammarRule>()

    override val allResolvedRule: List<GrammarRule> by lazy {
        //TODO: Handle situation where super grammar/rule is included more than once ?
        val rules = this.extends.flatMap { it.resolved?.allResolvedRule?: emptyList() }.toMutableList()
        this.rule.forEach { rule ->
            if (rule.isOverride) {
                val overridden = rules.find { it.name == rule.name }
                    ?: throw GrammarRuleNotFoundException("GrammarRule ${rule.name} is marked as overridden, but there is no super rule with that name to override.")
                rules.remove(overridden)
                rules.add(rule)
            } else {
                rules.add(rule)
            }
        }
        rules
    }

    override val allResolvedTerminal: Set<Terminal> by lazy {
        this.allResolvedRule.toSet().flatMap {
            when (it.isLeaf) {
                true -> setOf(it.compressedLeaf)
                false -> it.rhs.allTerminal
            }
        }.toSet()
    }

    override val allResolvedNonTerminalRule: Set<GrammarRule> by lazy {
        this.allResolvedRule.filter { it.isLeaf.not() }.toSet()
    }

    override val allResolvedEmbeddedRules: Set<Embedded>by lazy {
        this.allResolvedRule.flatMap { it.rhs.allEmbedded }.toSet()
    }

    override val allResolvedEmbeddedGrammars: Set<Grammar> by lazy {
        val egs = this.allResolvedEmbeddedRules.mapNotNull {  it.embeddedGrammarReference.resolved  }.toSet()
        egs + egs.flatMap { it.allResolvedEmbeddedGrammars }.toSet()//FIXME: recursion
    }

    override fun findNonTerminalRule(ruleName: String): GrammarRule? {
        val all = this.allResolvedRule.filter { it.name == ruleName }
        return when {
            all.isEmpty() -> null//throw GrammarRuleNotFoundException("NonTerminal GrammarRule '${ruleName}' not found in grammar '${this.name}'")
            all.size > 1 -> throw GrammarRuleNotFoundException("More than one rule named '${ruleName}' in grammar '${this.name}', have you remembered the 'override' modifier")
            else -> all.first()
        }
    }

    override fun findTerminalRule(terminalPattern: String): Terminal {
        val all = this.allResolvedTerminal.filter { it.value == terminalPattern }
        when {
            all.isEmpty() -> throw GrammarRuleNotFoundException("$terminalPattern in Grammar(${this.name}).findTerminalRule")
            all.size > 1 -> throw GrammarRuleNotFoundException("More than one rule named $terminalPattern in Grammar(${this.name}).findTerminalRule")
        }
        return all.first()
    }

}
