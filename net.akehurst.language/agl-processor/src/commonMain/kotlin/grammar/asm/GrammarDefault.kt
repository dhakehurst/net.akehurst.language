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

package net.akehurst.language.grammar.asm

import net.akehurst.language.base.api.*
import net.akehurst.language.base.asm.ModelAbstract
import net.akehurst.language.base.asm.NamespaceAbstract
import net.akehurst.language.base.asm.OptionHolderDefault
import net.akehurst.language.collections.*
import net.akehurst.language.grammar.api.*
import net.akehurst.language.grammar.processor.AglGrammar


class GrammarModelDefault(
    override val name: SimpleName,
    options: OptionHolder = OptionHolderDefault(null, emptyMap()),
    namespace: List<GrammarNamespace> = emptyList()
) : GrammarModel, ModelAbstract<GrammarNamespace, Grammar>(namespace,options) {

    override fun hashCode(): Int = arrayOf(name, namespace).contentHashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is GrammarModel -> false
        this.name != other.name -> false
        this.namespace != other.namespace -> false
        else -> true
    }

    override fun toString(): String = "GrammarModel '$name'"
}

@Deprecated("Just use a GrammarModel", ReplaceWith("Just use a GrammarModel"))
fun Grammar.asGrammarModel(): GrammarModel = GrammarModelDefault(this.name, OptionHolderDefault(null, emptyMap()), listOf(this.namespace as GrammarNamespace))

class GrammarNamespaceDefault(
    override val qualifiedName: QualifiedName,
    options: OptionHolder = OptionHolderDefault(null, emptyMap()),
    import: List<Import> = emptyList()
) : GrammarNamespace, NamespaceAbstract<Grammar>(options, import) {

}

/**
 * ID -> qualifiedName
 */
class GrammarDefault(
    namespace: GrammarNamespace,
    name: SimpleName,
    options: OptionHolder
) : GrammarAbstract(namespace, name,options) {

    companion object {
        fun fromString() {

        }
    }

    override val defaultGoalRule: GrammarRule
        get() = options[AglGrammar.OPTION_defaultGoalRule]?.let { findAllResolvedGrammarRule(GrammarRuleName(it)) }
            ?: this.allResolvedGrammarRule.firstOrNull { it.isSkip.not() }
            ?: error("Could not find default grammar rule or first non skip rule")
}

data class GrammarReferenceDefault(
    override val localNamespace: Namespace<Grammar>,
    override val nameOrQName: PossiblyQualifiedName
) : GrammarReference {
    override var resolved: Grammar? = null
    override fun resolveAs(resolved: Grammar) {
        this.resolved = resolved
    }
}

abstract class GrammarAbstract(
    final override val namespace: GrammarNamespace,
    final override val name: SimpleName,
    override val options: OptionHolder,
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
                            nr.rhs = (rule as OverrideRule).overriddenRhs
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

    init {
        namespace.addDefinition(this)
    }

    override val selfReference by lazy {
        GrammarReferenceDefault(this.namespace, this.name).also {
            it.resolveAs(this)
        }
    }

    override val qualifiedName: QualifiedName get() = namespace.qualifiedName.append(name)

    override val extends = mutableListOf<GrammarReference>()

    override val grammarRule = mutableListOf<GrammarRule>()
    override val preferenceRule = mutableListOf<PreferenceRule>()

    override val allGrammarReferencesInRules: List<GrammarReference> by lazy {
        val refs: List<GrammarReference> = allGrammarRule.flatMap {
            when {
                it is OverrideRule -> {
                    if (it.overriddenRhs is NonTerminal) {
                        (it.overriddenRhs as NonTerminal).targetGrammar?.let { listOf(it) } ?: emptyList<GrammarReference>()
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
        val resolvedRules = linkedMapOf<GrammarRuleName, GrammarRule>() //use linkedMap so order stays the same
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
        val resolvedRules = linkedMapOf<GrammarRuleName, GrammarRule>() //use linkedMap so order stays the same
        val inheritedRules = allInheritedResolvedGrammarRule//emptyList<GrammarRule>()
        this.grammarRule.forEach { rule ->
            val r = resolve(rule, inheritedRules.toList()) ?: rule
            resolvedRules[r.name] = r
        }
        resolvedRules.values.toOrderedSet()
    }

    override val allResolvedGrammarRule: OrderedSet<GrammarRule> by lazy {
        val resolvedRules = linkedMapOf<GrammarRuleName, GrammarRule>() //use linkedMap so order stayes the same
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

    override val allResolvedEmbeddedGrammars: Set<Grammar> by lazy { findAllResolvedEmbeddedGrammars() }

    override fun findOwnedGrammarRuleOrNull(ruleName: GrammarRuleName): GrammarRule? {
        return grammarRule.firstOrNull { it.name == ruleName }
    }

    override fun findAllSuperGrammarRule(ruleName: GrammarRuleName): List<GrammarRule> {
        val rules = this.extends.flatMap { it.resolved?.allGrammarRule ?: emptyList() }.toMutableOrderedSet()
        return rules.filter { it.grammar != this && it.name == ruleName }
    }

    override fun findAllGrammarRuleList(ruleName: GrammarRuleName): List<GrammarRule> =
        this.allGrammarRule.filter { it.name == ruleName }

    override fun findAllResolvedGrammarRule(ruleName: GrammarRuleName): GrammarRule? {
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

    override fun findAllResolvedEmbeddedGrammars(found:Set<Grammar> ): Set<Grammar> {
        return when {
            found.contains(this) -> emptySet()
            else -> {
                val egs = this.allResolvedEmbeddedRules.mapNotNull { it.embeddedGrammarReference.resolved }.toSet()
                egs + egs.flatMap { it.findAllResolvedEmbeddedGrammars(found+egs) }.toSet()
            }
        }
    }

    override fun asString(indent: Indent): String {
        val sb = StringBuilder()
        sb.append("grammar $name")
        val extendsStr = when {
            extends.isEmpty() -> ""
            else -> " : ${extends.joinToString(separator = ", ") { it.nameOrQName.toString() }}"
        }
        sb.append(extendsStr)
        sb.append(" {\n")
        val ni = indent.inc
        val rulesStr = grammarRule.joinToString(separator = "\n") { "${ni}${it.asString(ni)}" }
        sb.append(rulesStr)
        sb.append("\n$indent}")
        return sb.toString()
    }

    override fun hashCode(): Int = this.qualifiedName.hashCode()
    override fun equals(other: Any?): Boolean = when (other) {
        !is Grammar -> false
        else -> this.qualifiedName == other.qualifiedName
    }

    override fun toString(): String = qualifiedName.value



}
