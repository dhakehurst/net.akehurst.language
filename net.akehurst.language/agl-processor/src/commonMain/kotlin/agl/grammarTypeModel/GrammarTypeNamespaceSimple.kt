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

package net.akehurst.language.agl.grammarTypeModel

import net.akehurst.language.api.grammarTypeModel.GrammarTypeNamespace
import net.akehurst.language.api.language.base.Import
import net.akehurst.language.api.language.base.Indent
import net.akehurst.language.api.language.base.QualifiedName
import net.akehurst.language.api.language.grammar.GrammarRuleName
import net.akehurst.language.typemodel.api.DataType
import net.akehurst.language.typemodel.api.TypeInstance
import net.akehurst.language.typemodel.simple.TypeNamespaceAbstract

class GrammarTypeNamespaceSimple(
    qualifiedName: QualifiedName,
    imports: List<Import>
) : GrammarTypeNamespaceAbstract(qualifiedName, imports)

abstract class GrammarTypeNamespaceAbstract(
    qualifiedName: QualifiedName,
    imports: List<Import>
) : TypeNamespaceAbstract(qualifiedName, imports), GrammarTypeNamespace {

    fun addTypeFor(grammarRuleName: GrammarRuleName, typeUse: TypeInstance) {
        this.allRuleNameToType[grammarRuleName] = typeUse
        if (typeUse.declaration is DataType) {
            super.ownedTypesByName[typeUse.declaration.name] = typeUse.declaration
        }
    }

    override var allRuleNameToType = mutableMapOf<GrammarRuleName, TypeInstance>()

    override val allTypesByRuleName: Collection<Pair<GrammarRuleName, TypeInstance>>
        get() = allRuleNameToType.entries.map { Pair(it.key, it.value) }

    override fun findTypeForRule(ruleName: GrammarRuleName): TypeInstance? = allRuleNameToType[ruleName]

    override fun asString(indent: Indent, increment: String): String {
        val rules = this.allRuleNameToType.entries.sortedBy { it.key.value }
        val ruleToType = rules.joinToString(separator = "\n") { it.key.value + "->" + it.value.signature(this, 0) }
        val types = this.ownedTypesByName.entries.sortedBy { it.key.value }.joinToString(separator = "\n") { it.value.asString(this) }
        val s = """namespace '$qualifiedName' {
$ruleToType

$types
}""".trimIndent()
        return s
    }
}