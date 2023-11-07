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
import net.akehurst.language.typemodel.api.DataType
import net.akehurst.language.typemodel.api.TypeInstance
import net.akehurst.language.typemodel.simple.TypeNamespaceAbstract

class GrammarTypeNamespaceSimple(
    override val qualifiedName: String,
    imports: List<String>
) : GrammarTypeNamespaceAbstract(imports)

abstract class GrammarTypeNamespaceAbstract(
    imports: List<String>
) : TypeNamespaceAbstract(imports), GrammarTypeNamespace {

    fun addTypeFor(grammarRuleName: String, typeUse: TypeInstance) {
        this.allRuleNameToType[grammarRuleName] = typeUse
        if (typeUse.declaration is DataType) {
            super.allTypesByName[typeUse.declaration.name] = typeUse.declaration
        }
    }

    override var allRuleNameToType = mutableMapOf<String, TypeInstance>()

    override val allTypesByRuleName: Collection<Pair<String, TypeInstance>>
        get() = allRuleNameToType.entries.map { Pair(it.key, it.value) }

    override fun findTypeUsageForRule(ruleName: String): TypeInstance? = allRuleNameToType[ruleName]

    override fun asString(): String {
        val rules = this.allRuleNameToType.entries.sortedBy { it.key }
        val ruleToType = rules.joinToString(separator = "\n") { it.key + "->" + it.value.signature(this, 0) }
        val types = this.allTypesByName.entries.sortedBy { it.key }.joinToString(separator = "\n") { it.value.asString(this) }
        val s = """namespace '$qualifiedName' {
$ruleToType

$types
}""".trimIndent()
        return s
    }
}