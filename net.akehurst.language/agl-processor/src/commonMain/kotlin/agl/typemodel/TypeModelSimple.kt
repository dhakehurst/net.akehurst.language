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

package net.akehurst.language.agl.agl.typemodel

import net.akehurst.language.api.typemodel.ElementType
import net.akehurst.language.api.typemodel.RuleType
import net.akehurst.language.api.typemodel.TypeModel

class TypeModelSimple(
    namespace: String,
    name: String
) : TypeModelAbstract(namespace, name) {

    fun addTypeFor(grammarRuleName: String, type: RuleType) {
        super.allTypesByRuleName[grammarRuleName] = type
        super.allTypesByName[type.name] = type
    }

    fun findOrCreateTypeFor(grammarRuleName: String, typeName: String): ElementType {
        val existing = findOrCreateTypeNamed(typeName)
        super.allTypesByRuleName[grammarRuleName] = existing
        return existing
    }

    fun findOrCreateTypeNamed(typeName: String): ElementType {
        val existing = findTypeNamed(typeName)
        return if (null == existing) {
            val t = ElementType(this, typeName)
            super.allTypesByName[typeName] = t
            t
        } else {
            existing as ElementType
        }
    }
}

abstract class TypeModelAbstract(
    override val namespace: String,
    override val name: String
) : TypeModel {

    val qualifiedName get() = "$namespace.$name"

    /**
     * RuleType.name --> RuleType
     */
    override val allTypesByName = mutableMapOf<String, RuleType>()

    /**
     * GrammarRule.name --> RuleType
     */
    override var allTypesByRuleName = mutableMapOf<String, RuleType>()

    override fun findTypeForRule(ruleName: String): RuleType? = allTypesByRuleName[ruleName]
    override fun findTypeNamed(typeName: String): RuleType? = allTypesByName[typeName]
}