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

import net.akehurst.language.base.api.*
import net.akehurst.language.base.asm.OptionHolderDefault
import net.akehurst.language.grammarTypemodel.api.GrammarTypeNamespace
import net.akehurst.language.grammar.api.GrammarRuleName
import net.akehurst.language.typemodel.api.DataType
import net.akehurst.language.typemodel.api.TypeInstance
import net.akehurst.language.typemodel.asm.TypeNamespaceAbstract

class GrammarTypeNamespaceSimple(
    override val qualifiedName: QualifiedName,
    options: OptionHolder = OptionHolderDefault(null, emptyMap()),
    import: List<Import>
) : GrammarTypeNamespaceAbstract(options,import)

abstract class GrammarTypeNamespaceAbstract(
    options: OptionHolder,
    import: List<Import>
) : TypeNamespaceAbstract(options,import), GrammarTypeNamespace {

    fun addTypeFor(grammarRuleName: GrammarRuleName, typeUse: TypeInstance) {
        this.allRuleNameToType[grammarRuleName] = typeUse
        if (typeUse.declaration is DataType) {
            addDefinition(typeUse.declaration)
        }
    }

    override var allRuleNameToType = mutableMapOf<GrammarRuleName, TypeInstance>()

    override val allTypesByRuleName: Collection<Pair<GrammarRuleName, TypeInstance>>
        get() = allRuleNameToType.entries.map { Pair(it.key, it.value) }

    override fun findTypeForRule(ruleName: GrammarRuleName): TypeInstance? = allRuleNameToType[ruleName]

    override fun asString(indent: Indent): String {
        val rules = this.allRuleNameToType.entries.sortedBy { it.key.value }
        val ruleToType = rules.joinToString(separator = "\n") { it.key.value + "->" + it.value.signature(this, 0) }
        val types = this.ownedTypesByName.entries.sortedBy { it.key.value }.joinToString(separator = "\n") { it.value.asStringInContext(this) }
        val importstr = this.import.joinToString(prefix = "  ", separator = "\n  ") { "import ${it}.*" }
        val s = """namespace '$qualifiedName' {
$ruleToType
$importstr
$types
}""".trimIndent()
        return s
    }
}