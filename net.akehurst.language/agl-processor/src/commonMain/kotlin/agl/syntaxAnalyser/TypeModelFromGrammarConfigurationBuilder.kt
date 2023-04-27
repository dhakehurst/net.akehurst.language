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

import net.akehurst.language.api.grammar.*
import net.akehurst.language.api.typemodel.*

interface TypeModelFromGrammarConfiguration {
    fun typeNameFor(rule: GrammarRule): String
    fun propertyNameFor(ruleItem: RuleItem, ruleItemType: RuleType): String
}

fun String.lower() = when {
    this.uppercase() == this -> this.lowercase()
    else -> this.replaceFirstChar { it.lowercase() }
}

class TypeModelFromGrammarConfigurationDefault() : TypeModelFromGrammarConfiguration {
    override fun typeNameFor(rule: GrammarRule): String = rule.name.replaceFirstChar { it.titlecase() }
    override fun propertyNameFor(ruleItem: RuleItem, ruleItemType: RuleType): String {
        val baseName = when (ruleItem) {
            is Terminal -> when (ruleItemType) {
                is StringType -> TypeModelFromGrammar.UNNAMED_PRIMITIVE_PROPERTY_NAME
                is ListSimpleType -> TypeModelFromGrammar.UNNAMED_LIST_PROPERTY_NAME
                is ListSeparatedType -> TypeModelFromGrammar.UNNAMED_LIST_PROPERTY_NAME
                is TupleType -> TypeModelFromGrammar.UNNAMED_TUPLE_PROPERTY_NAME
                else -> TypeModelFromGrammar.UNNAMED_PRIMITIVE_PROPERTY_NAME
            }

            is Embedded -> ruleItem.embeddedGoalName.lower()
            is NonTerminal -> ruleItem.name.lower()
            is Group -> TypeModelFromGrammar.UNNAMED_GROUP_PROPERTY_NAME
            is Choice -> TypeModelFromGrammar.UNNAMED_CHOICE_PROPERTY_NAME
            else -> error("Internal error, unhandled subtype of SimpleItem")
        }.replaceFirstChar { it.lowercase() }
        return when (ruleItemType) {
            is NothingType -> baseName
            is AnyType -> baseName
            is StringType -> baseName
            is UnnamedSuperTypeType -> baseName
            is ListSimpleType -> when (ruleItem) {
                is NonTerminal -> ruleItem.name.lower()
                is Terminal -> TypeModelFromGrammar.UNNAMED_LIST_PROPERTY_NAME
                is Group -> TypeModelFromGrammar.UNNAMED_LIST_PROPERTY_NAME
                else -> "${baseName}List"
            }

            is ListSeparatedType -> when (ruleItem) {
                is NonTerminal -> ruleItem.name.lower()
                is Terminal -> TypeModelFromGrammar.UNNAMED_LIST_PROPERTY_NAME
                else -> "${baseName}List"
            }

            is TupleType -> baseName
            is ElementType -> baseName
        }
    }
}
