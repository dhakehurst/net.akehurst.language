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

package net.akehurst.language.agl.default

import net.akehurst.language.api.grammar.*
import net.akehurst.language.typemodel.api.*

interface TypeModelFromGrammarConfiguration {
    fun typeNameFor(rule: GrammarRule): String
    fun propertyNameFor(context: Grammar, ruleItem: RuleItem, ruleItemType: TypeDefinition): String
}

fun String.lower() = when {
    this.uppercase() == this -> this.lowercase()
    else -> this.replaceFirstChar { it.lowercase() }
}

class TypeModelFromGrammarConfigurationDefault() : TypeModelFromGrammarConfiguration {
    override fun typeNameFor(rule: GrammarRule): String = rule.name.replaceFirstChar { it.titlecase() }
    override fun propertyNameFor(context: Grammar, ruleItem: RuleItem, ruleItemType: TypeDefinition): String {
        val prefix = when (context) {
            ruleItem.owningRule.grammar -> ""
            else -> "${ruleItem.owningRule.grammar.name.lower()}_"
        }
        val baseName = when (ruleItem) {
            is Terminal -> when (ruleItemType) {
                is PrimitiveType -> GrammarTypeNamespaceFromGrammar.UNNAMED_PRIMITIVE_PROPERTY_NAME
                is CollectionType -> GrammarTypeNamespaceFromGrammar.UNNAMED_LIST_PROPERTY_NAME
                is TupleType -> GrammarTypeNamespaceFromGrammar.UNNAMED_TUPLE_PROPERTY_NAME
                else -> GrammarTypeNamespaceFromGrammar.UNNAMED_PRIMITIVE_PROPERTY_NAME
            }

            //is Embedded -> "${ruleItem.embeddedGrammarReference.resolved!!.name}_${ruleItem.embeddedGoalName.lower()}"
            is Embedded -> "${ruleItem.embeddedGoalName.lower()}"
            is NonTerminal -> ruleItem.name.lower()
            is Group -> GrammarTypeNamespaceFromGrammar.UNNAMED_GROUP_PROPERTY_NAME
            is Choice -> GrammarTypeNamespaceFromGrammar.UNNAMED_CHOICE_PROPERTY_NAME
            else -> error("Internal error, unhandled subtype of SimpleItem")
        }.replaceFirstChar { it.lowercase() }
        val name = when (ruleItemType) {
            is PrimitiveType -> baseName
            is UnnamedSuperTypeType -> baseName
            is CollectionType -> when (ruleItem) {
                is NonTerminal -> ruleItem.name.lower()
                is Terminal -> GrammarTypeNamespaceFromGrammar.UNNAMED_LIST_PROPERTY_NAME
                is Group -> GrammarTypeNamespaceFromGrammar.UNNAMED_LIST_PROPERTY_NAME
                else -> "${baseName}List"
            }

            is TupleType -> baseName
            is DataType -> baseName
            else -> baseName
        }
        return name //prefix + name
    }
}
