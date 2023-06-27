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

package net.akehurst.language.api.grammarTypeModel

import net.akehurst.language.typemodel.api.PrimitiveType
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.api.TypeUsage

interface GrammarTypeModel : TypeModel {
    /**
     * grammarRuleName -> TypeUsage
     */
    val allRuleNameToType: Map<String, TypeUsage>

    val allTypesByRuleName: Collection<Pair<String, TypeUsage>>

    fun findTypeUsageForRule(ruleName: String): TypeUsage?
}

val GrammarTypeModel.StringType: PrimitiveType get() = this.findOrCreatePrimitiveTypeNamed("String")

fun GrammarTypeModel.asString(): String {
    val rules = this.allRuleNameToType.entries.sortedBy { it.key }
    val ruleToType = rules.joinToString(separator = "\n") { it.key + "->" + it.value.signature(this, 0) }
    val types = this.allTypesByName.entries.sortedBy { it.key }.joinToString(separator = "\n") { it.value.asString(this) }
    val s = """
    typemodel '$namespace.$name' {
    $ruleToType
    
    $types
    }
    """.trimIndent()
    return s
}