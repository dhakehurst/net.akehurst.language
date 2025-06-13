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

package net.akehurst.language.transform.api

import net.akehurst.language.base.api.*
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.grammar.api.GrammarRuleName
import net.akehurst.language.typemodel.api.TypeInstance
import net.akehurst.language.typemodel.api.TypeModel

interface TransformModel : Model<TransformNamespace, TransformRuleSet> {
    /**
     * Access to the TypeModel ensuring that the AsmTransform has first been evaluated
     */
    val typeModel: TypeModel?

    override val namespace: List<TransformNamespace>

    fun findOrCreateNamespace(qualifiedName: QualifiedName, imports: List<Import>): TransformNamespace
    fun findTypeForGrammarRule(grammarQualifiedName: QualifiedName, ruleName: GrammarRuleName): TypeInstance?
}

interface TransformNamespace : Namespace<TransformRuleSet> {
    fun createOwnedTransformRuleSet(name: SimpleName, extends: List<TransformRuleSetReference>, options: OptionHolder): TransformRuleSet
}

interface TransformRuleSetReference : DefinitionReference<TransformRuleSet> {
   // val localNamespace: TransformNamespace
  //  val nameOrQName: PossiblyQualifiedName
   // var resolved: TransformRuleSet?

   // fun resolveAs(resolved: TransformRuleSet)

   fun cloneTo(ns: TransformNamespace): TransformRuleSetReference
}

interface TransformRuleSet : Definition<TransformRuleSet> {

    override val namespace: TransformNamespace

    val extends: List<TransformRuleSetReference>

    /**
     * Types in these namespaces can be referenced non-qualified
     * ordered so that 'first' imported name takes priority
     */
    val importTypes: List<Import>

    /**
     * map from grammar-rule name to TransformationRule
     */
    val rules: Map<GrammarRuleName, TransformationRule>

    val createObjectRules: List<CreateObjectRule>
    val modifyObjectRules: List<ModifyObjectRule>

    fun findOwnedTrRuleForGrammarRuleNamedOrNull(grmRuleName: GrammarRuleName): TransformationRule?
    fun findAllTrRuleForGrammarRuleNamedOrNull(grmRuleName: GrammarRuleName): TransformationRule?

    fun addImportType(value:Import)

    /**
     * set the rule for its GrammarRuleName (rules[rule.grammarRuleName] = rule)
     */
    fun setRule(rule: TransformationRule)

    fun cloneTo(ns: TransformNamespace): TransformRuleSet
}

interface TransformationRule {
    var grammarRuleName: GrammarRuleName
    val expression: Expression
    val isResolved: Boolean
    val resolvedType: TypeInstance

    fun resolveTypeAs(type: TypeInstance)
    fun asString(indent: Indent = Indent(), imports: List<Import> = emptyList()): String
}

interface CreateObjectRule : TransformationRule {

}

interface SelfStatement {

}

interface ModifyObjectRule : TransformationRule {
}