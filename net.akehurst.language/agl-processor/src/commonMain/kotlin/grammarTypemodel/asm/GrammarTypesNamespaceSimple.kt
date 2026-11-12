/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.grammarTypemodel.asm

import net.akehurst.language.base.api.Import
import net.akehurst.language.base.api.OptionHolder
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.asm.OptionHolderDefault
import net.akehurst.language.grammar.api.GrammarRuleName
import net.akehurst.language.grammarTypemodel.api.GrammarTypesNamespace
import net.akehurst.language.types.api.TypeInstance
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.api.TypesNamespace
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.asm.TypesNamespaceAbstract
import net.akehurst.language.types.asm.TypesDomainSimple

class GrammarTypesNamespaceSimple(
    override val qualifiedName: QualifiedName,
    options: OptionHolder = OptionHolderDefault(null, emptyMap()),
    import: List<Import>
) : GrammarTypesNamespaceAbstract(options, import) {
    companion object {
        fun findOrCreateGrammarNamespace(typesDomain: TypesDomain, qualifiedName: QualifiedName) =
            typesDomain.findNamespaceOrNull(qualifiedName) as GrammarTypesNamespaceSimple?
                ?: let {
                    val imports = listOf(Import(StdLibDefault.qualifiedName.value))
                    val ns = GrammarTypesNamespaceSimple(
                        qualifiedName = qualifiedName,
                        import = imports
                    )
                    typesDomain.addNamespace(ns)
                    ns
                }

        /**
         * Types given by the user (e.g. a types string) may use the qualified name of a grammar as a namespace.
         * The types for a grammar must be in a GrammarTypesNamespace,
         * so if any such namespace is a plain TypesNamespace, returns a copy of the domain in which it is a GrammarTypesNamespace
         * (holding the same types). The default transform then uses those types for the grammar rules with the same type name.
         * Otherwise returns the given domain.
         */
        fun withGrammarNamespaces(typesDomain: TypesDomain, grammarQualifiedNames: Collection<QualifiedName>): TypesDomain {
            val toConvert = typesDomain.namespace.filter { it !is GrammarTypesNamespace && grammarQualifiedNames.contains(it.qualifiedName) }
            return when {
                toConvert.isEmpty() -> typesDomain
                else -> {
                    val result = TypesDomainSimple(typesDomain.name, typesDomain.options)
                    // keep the order of the namespaces, it matters for lookup
                    typesDomain.namespace.forEach { ns ->
                        when {
                            toConvert.contains(ns) -> result.addNamespace(GrammarTypesNamespaceSimple(ns.qualifiedName, ns.options, ns.import))
                            ns is StdLibDefault -> result.addNamespace(ns)
                            else -> ns.findInOrCloneTo(result)
                        }
                    }
                    typesDomain.namespace.filter { it !is StdLibDefault }.forEach { ns ->
                        ns.ownedTypes.forEach { it.findInOrCloneTo(result) }
                    }
                    result.resolveImports()
                    result
                }
            }
        }
    }

    override fun findInOrCloneTo(other: TypesDomain): TypesNamespace =
        other.findNamespaceOrNull(this.qualifiedName)
            ?: GrammarTypesNamespaceSimple(
                this.qualifiedName,
                this.options,
                this.import
            ).also {
                it.allRuleNameToType = this.allRuleNameToType
                other.addNamespace(it)
            }

}

abstract class GrammarTypesNamespaceAbstract(
    options: OptionHolder,
    import: List<Import>
) : TypesNamespaceAbstract(options, import), GrammarTypesNamespace {

    override fun setTypeForGrammarRule(grammarRuleName: GrammarRuleName, typeUse: TypeInstance) {
        this.allRuleNameToType[grammarRuleName] = typeUse
    }

    override var allRuleNameToType = mutableMapOf<GrammarRuleName, TypeInstance>()

    override val allTypesByRuleName: Collection<Pair<GrammarRuleName, TypeInstance>>
        get() = allRuleNameToType.entries.map { Pair(it.key, it.value) }

    override fun findTypeForRule(ruleName: GrammarRuleName, excludingImports:Set<Import>): TypeInstance? {
        // excludingImports is needed in order to eliminate infinite recursion
        return allRuleNameToType[ruleName]
            ?: _importedNamespaces.values.firstNotNullOfOrNull {
                when {
                    null==it -> null //FIXME: why is an importedNamespace maybe null !
                    excludingImports.contains(it.qualifiedName.asImport) -> null
                    it is GrammarTypesNamespace -> it.findTypeForRule(ruleName, excludingImports+this.qualifiedName.asImport)
                    else -> null
                }
            }
    }

//    override fun asString(indent: Indent): String {
//        //val rules = this.allRuleNameToType.entries.sortedBy { it.key.value }
//        //val ruleToType = rules.joinToString(separator = "\n") { it.key.value + "->" + it.value.signature(this, 0) }
//        val types = this.ownedTypesByName.entries
//            .sortedBy { it.key.value }
//            .joinToString(separator = "\n") { it.value.asStringInContext(this) }
//        val importstr = this.import.joinToString(prefix = "  ", separator = "\n  ") { "import ${it}" }
//        val s = """namespace $qualifiedName
//$importstr
//$types
//""".trimIndent()
//        return s
//    }
}