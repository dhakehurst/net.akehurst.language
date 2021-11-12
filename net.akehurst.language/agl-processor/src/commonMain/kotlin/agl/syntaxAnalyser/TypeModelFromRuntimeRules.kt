/**
 * Copyright (C) 2021 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

import net.akehurst.language.api.typeModel.*
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsItemsKind

internal class TypeModelFromRuntimeRules(
    private val _runtimeRules: List<RuntimeRule>
) {

    private val _ruleToType = mutableMapOf<RuntimeRule, RuleType>()
    private val _typeModel = TypeModel()

    private val RuntimeRule.isUnnamed: Boolean
        get() = (this.tag.startsWith("'") && this.tag.endsWith("'"))
                || (this.tag.startsWith("\"") && this.tag.endsWith("\""))

    internal fun derive(): TypeModel {
        for (rr in _runtimeRules) {
            typeForRule(rr)
        }
        return _typeModel
    }

    private fun typeForRule(runtimeRule: RuntimeRule): RuleType? {
        val type = _ruleToType[runtimeRule]
        return if (null != type) {
            type // return the type if it exists, also stops infinite recursion
        } else {
            when (runtimeRule.kind) {
                RuntimeRuleKind.GOAL -> null // should not be used
                RuntimeRuleKind.TERMINAL -> if (runtimeRule.isEmptyRule) null else BuiltInType.STRING
                RuntimeRuleKind.EMBEDDED -> TODO()
                RuntimeRuleKind.NON_TERMINAL -> when (runtimeRule.rhs.itemsKind) {
                    RuntimeRuleRhsItemsKind.EMPTY -> null // nothing created for this
                    RuntimeRuleRhsItemsKind.CONCATENATION -> {
                        val concatType = findOrCreateElementType(runtimeRule)
                        runtimeRule.rhs.items.forEach {
                            if (it.isEmptyRule.not()) {
                                createPropertyDeclaration(concatType, it)
                            } else {
                                /*don't create */
                            }
                        }
                        concatType
                    }
                    RuntimeRuleRhsItemsKind.CHOICE -> {
                        // if all rhs gives ElementType then this ruleType is a super type of all rhs
                        // else rhs maps to properties
                        val choiceType = findOrCreateElementType(runtimeRule)
                        val subtypes = runtimeRule.rhs.items.map { typeForRule(it) }
                        if (subtypes.all { it is ElementType }) {
                            subtypes.forEach {
                                (it as ElementType).superType.add(choiceType)
                            }
                        } else {
                            runtimeRule.rhs.items.forEach { createPropertyDeclaration(choiceType, it) }
                        }
                        choiceType
                    }
                    RuntimeRuleRhsItemsKind.LIST -> BuiltInType.LIST
                }
            }
        }
    }

    private fun findOrCreateElementType(runtimeRule: RuntimeRule): ElementType {
        val rt = _typeModel.findOrCreateType(runtimeRule.tag) as ElementType
        _ruleToType[runtimeRule] = rt
        return rt
    }

    private fun createPropertyDeclaration(et: ElementType, rhsRule: RuntimeRule): PropertyDeclaration {
        val propType = typeForRule(rhsRule) ?: error("should never be null")
        val propName = when {
            rhsRule.isUnnamed -> "value"
            else -> rhsRule.tag
        }
        val pt = PropertyDeclaration(et, propName, propType)
        return pt
    }

}