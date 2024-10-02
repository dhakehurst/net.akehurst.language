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
}

interface TransformNamespace : Namespace<TransformRuleSet> {

}

interface TransformRuleSet : Definition<TransformRuleSet> {

    override val namespace: TransformNamespace

    /**
     * map from grammar-rule name to TransformationRule
     */
    val rules: Map<GrammarRuleName, TransformationRule>

    val createObjectRules: List<CreateObjectRule>
    val modifyObjectRules: List<ModifyObjectRule>

    fun findTrRuleForGrammarRuleNamedOrNull(grmRuleName: GrammarRuleName): TransformationRule?
}

interface TransformationRule : Formatable {
    val grammarRuleName: GrammarRuleName
    val possiblyQualifiedTypeName: PossiblyQualifiedName

    val resolvedType: TypeInstance

    val expression: Expression
    // val modifyStatements: List<AssignmentStatement>

}

interface CreateObjectRule : TransformationRule {

}

interface SelfStatement {

}

interface ModifyObjectRule : TransformationRule {
}