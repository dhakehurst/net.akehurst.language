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

package net.akehurst.language.agl.default_

import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.grammar.api.*
import net.akehurst.language.typemodel.api.*

interface Grammar2TypeModelMapping {
    fun typeNameFor(rule: GrammarRule): SimpleName
    fun propertyNameFor(context: Grammar, ruleItem: RuleItem, ruleItemType: TypeDeclaration): PropertyName
}

fun String.lower() = when {
    this.uppercase() == this -> this.lowercase()
    else -> this.replaceFirstChar { it.lowercase() }
}

class TypeModelFromGrammarConfigurationDefault() : Grammar2TypeModelMapping {
    override fun typeNameFor(rule: GrammarRule): SimpleName = SimpleName(rule.name.value.replaceFirstChar { it.titlecase() })
    override fun propertyNameFor(context: Grammar, ruleItem: RuleItem, ruleItemType: TypeDeclaration): PropertyName {
//        val prefix = when (context) {
//            ruleItem.owningRule.grammar -> ""
//            else -> "${ruleItem.owningRule.grammar.name.lower()}_"
//        }
        val baseName = when (ruleItem) {
            is Terminal -> when (ruleItemType) {
                is PrimitiveType -> Grammar2TransformRuleSet.UNNAMED_PRIMITIVE_PROPERTY_NAME.value
                is CollectionType -> Grammar2TransformRuleSet.UNNAMED_LIST_PROPERTY_NAME.value
                is TupleType -> Grammar2TransformRuleSet.UNNAMED_TUPLE_PROPERTY_NAME.value
                else -> Grammar2TransformRuleSet.UNNAMED_PRIMITIVE_PROPERTY_NAME.value
            }

            //is Embedded -> "${ruleItem.embeddedGrammarReference.resolved!!.name}_${ruleItem.embeddedGoalName.lower()}"
            is Embedded -> ruleItem.embeddedGoalName.value.lower()
            is NonTerminal -> ruleItem.ruleReference.value.lower()
            is Group -> Grammar2TransformRuleSet.UNNAMED_GROUP_PROPERTY_NAME.value
            is Choice -> Grammar2TransformRuleSet.UNNAMED_CHOICE_PROPERTY_NAME.value
            else -> error("Internal error, unhandled subtype of SimpleItem")
        }.replaceFirstChar { it.lowercase() }
        val name = when (ruleItemType) {
            is PrimitiveType -> baseName
            is UnnamedSupertypeType -> baseName
            is CollectionType -> when (ruleItem) {
                is NonTerminal -> ruleItem.ruleReference.value.lower()
                is Terminal -> Grammar2TransformRuleSet.UNNAMED_LIST_PROPERTY_NAME.value
                is Group -> Grammar2TransformRuleSet.UNNAMED_LIST_PROPERTY_NAME.value
                else -> "${baseName}List"
            }

            is TupleType -> baseName
            is DataType -> baseName
            else -> baseName
        }
        return PropertyName(name) //prefix + name
    }
}
