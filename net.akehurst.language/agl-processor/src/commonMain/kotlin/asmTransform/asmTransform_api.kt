/*
 * Copyright (C) 2024 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.asmTransform.api

import net.akehurst.language.base.api.*
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.grammar.api.GrammarRuleName
import net.akehurst.language.types.api.TypeInstance
import net.akehurst.language.types.api.TypesDomain

interface AsmTransformDomain : Domain<AsmTransformNamespace, AsmTransformRuleSet> {
    /**
     * Access to the TypeModel ensuring that the AsmTransform has first been evaluated
     */
    val typesDomain: TypesDomain?

    override val namespace: List<AsmTransformNamespace>

    fun findOrCreateNamespace(qualifiedName: QualifiedName, imports: List<Import>): AsmTransformNamespace
    fun findTypeForGrammarRule(grammarQualifiedName: QualifiedName, ruleName: GrammarRuleName): TypeInstance?

}

interface AsmTransformNamespace : Namespace<AsmTransformRuleSet> {
    fun createOwnedTransformRuleSet(name: SimpleName, extends: List<AsmTransformRuleSetReference>, options: OptionHolder): AsmTransformRuleSet
}

interface AsmTransformRuleSetReference : DefinitionReference<AsmTransformRuleSet> {
   fun cloneTo(ns: AsmTransformNamespace): AsmTransformRuleSetReference
}

interface AsmTransformRuleSet : Definition<AsmTransformRuleSet> {

    override val namespace: AsmTransformNamespace

    val extends: List<AsmTransformRuleSetReference>

    /**
     * Types in these namespaces can be referenced non-qualified
     * ordered so that 'first' imported name takes priority
     */
    val importTypes: List<Import>

    /**
     * map from grammar-rule name to TransformationRule
     */
    val rules: Map<GrammarRuleName, AsmTransformationRule>

    val createObjectRules: List<CreateObjectRule>
    val modifyObjectRules: List<ModifyObjectRule>

    fun findOwnedTrRuleForGrammarRuleNamedOrNull(grmRuleName: GrammarRuleName): AsmTransformationRule?
    fun findAllTrRuleForGrammarRuleNamedOrNull(grmRuleName: GrammarRuleName): AsmTransformationRule?

    fun addImportType(value:Import)

    /**
     * set the rule for its GrammarRuleName (rules[rule.grammarRuleName] = rule)
     */
    fun setRule(rule: AsmTransformationRule)

    fun cloneTo(ns: AsmTransformNamespace): AsmTransformRuleSet
}

interface AsmTransformationRule {
    var grammarRuleName: GrammarRuleName
    val expression: Expression
    val isResolved: Boolean
    val resolvedType: TypeInstance

    fun resolveTypeAs(type: TypeInstance)
    fun asString(indent: Indent = Indent(), imports: List<Import> = emptyList()): String
}

interface CreateObjectRule : AsmTransformationRule {

}

interface SelfStatement {

}

interface ModifyObjectRule : AsmTransformationRule {
}