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

package net.akehurst.language.grammarTypemodel.api

import net.akehurst.language.grammar.api.GrammarRuleName
import net.akehurst.language.typemodel.api.TypeInstance
import net.akehurst.language.typemodel.api.TypeNamespace

//TODO: why is this needed..the grammar->Type mapping should be in the transform !
interface GrammarTypeNamespace : TypeNamespace {
    /**
     * grammarRuleName -> TypeUsage
     */
    val allRuleNameToType: Map<GrammarRuleName, TypeInstance>

    val allTypesByRuleName: Collection<Pair<GrammarRuleName, TypeInstance>>

    fun findTypeForRule(ruleName: GrammarRuleName): TypeInstance?

    fun setTypeForGrammarRule(grammarRuleName: GrammarRuleName, typeUse: TypeInstance)
}