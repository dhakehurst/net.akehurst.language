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

import net.akehurst.language.api.grammarTypeModel.GrammarTypeModel
import net.akehurst.language.typemodel.api.ElementType
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.api.TypeUsage
import net.akehurst.language.typemodel.simple.TypeModelAbstract

object GrammarTypeModelStdLib : TypeModelAbstract("grammar", "std", null, emptySet<TypeModel>()) {
    init {
        super.findOrCreatePrimitiveTypeNamed("String")
    }
}

class GrammarTypeModelSimple(
    namespace: String,
    name: String,
    rootTypeName: String,
    imports: Set<TypeModel>
) : GrammarTypeModelAbstract(namespace, name, rootTypeName, imports)

abstract class GrammarTypeModelAbstract(
    namespace: String,
    name: String,
    rootTypeName: String,
    imports: Set<TypeModel>
) : TypeModelAbstract(namespace, name, rootTypeName, imports), GrammarTypeModel {

    fun addTypeFor(grammarRuleName: String, typeUse: TypeUsage) {
        this.allRuleNameToType[grammarRuleName] = typeUse
        if (typeUse.type is ElementType) {
            super.allTypesByName[typeUse.type.name] = typeUse.type
        }
    }

    /*
    fun findOrCreateTypeFor(grammarRuleName: String, typeName: String): ElementType {
        val existing = findOrCreateElementTypeNamed(typeName)
        this.allRuleNameToType[grammarRuleName] = TypeUsage.ofType(existing)
        return existing
    }
*/
    override var allRuleNameToType = mutableMapOf<String, TypeUsage>()

    override val allTypesByRuleName: Collection<Pair<String, TypeUsage>>
        get() = allRuleNameToType.entries.map { Pair(it.key, it.value) }

    override fun findTypeUsageForRule(ruleName: String): TypeUsage? = allRuleNameToType[ruleName]
}