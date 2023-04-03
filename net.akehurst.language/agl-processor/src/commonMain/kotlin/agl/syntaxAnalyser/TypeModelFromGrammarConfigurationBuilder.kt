/**
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.syntaxAnalyser

import net.akehurst.language.api.grammar.GrammarRule
import net.akehurst.language.api.grammar.SimpleItem
import net.akehurst.language.api.typeModel.RuleType

class TypeModelFromGrammarConfiguration(
    val typeNameFor:(rule:GrammarRule)->String,
    val propertyNameFor:(ruleItem: SimpleItem, ruleItemType:RuleType)->String
)

@DslMarker
annotation class TypeModelFromGrammarConfigurationBuilderMarker

@TypeModelFromGrammarConfigurationBuilderMarker
class TypeModelFromGrammarConfigurationBuilder {

    private lateinit var _typeNameFor:(rule:GrammarRule)->String
    private lateinit var _propertyNameFor:(ruleItem: SimpleItem, ruleItemType:RuleType)->String

    fun typeNameFor(func:(rule:GrammarRule)->String) {
        _typeNameFor = func
    }

    fun propertyNameFor(func:(ruleItem: SimpleItem, ruleItemType:RuleType)->String) {
        _propertyNameFor = func
    }

    fun build() : TypeModelFromGrammarConfiguration {
        return TypeModelFromGrammarConfiguration(_typeNameFor, _propertyNameFor)
    }
}