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

package net.akehurst.language.api.language.asmTransform

import net.akehurst.language.api.language.expressions.Expression
import net.akehurst.language.typemodel.api.TypeInstance
import net.akehurst.language.typemodel.api.TypeModel

interface AsmTransformModel {
    val name: String
    val qualifiedName: String

    /**
     * Access to the TypeModel ensuring that the AsmTransform has first been evaluated
     */
    val typeModel: TypeModel?

    /**
     * map from grammar-rule name to TransformationRule
     */
    val rules: Map<String, TransformationRule>

    val createObjectRules: List<CreateObjectRule>
    val modifyObjectRules: List<ModifyObjectRule>


    fun findTrRuleForGrammarRuleNamedOrNull(grmRuleName: String): TransformationRule?

    fun asString(indent: String = "", increment: String = "  "): String
}

interface TransformationRule {
    val grammarRuleName: String
    val typeName: String

    val resolvedType: TypeInstance

    val selfStatement: SelfStatement
    val modifyStatements: List<AssignmentTransformationStatement>

    fun asString(indent: String = "", increment: String = "  "): String
}

interface NoActionTransformationRule : TransformationRule
interface SubtypeTransformationRule : TransformationRule
interface SelfAssignChild0TransformationRule : TransformationRule
interface ListTransformationRule : TransformationRule
interface CreateObjectRule : TransformationRule {

}

interface SelfStatement {

}

interface ModifyObjectRule : TransformationRule {
}

interface AssignmentTransformationStatement {
    val lhsPropertyName: String
    val rhs: Expression

    fun asString(indent: String = "", increment: String = "  "): String
}