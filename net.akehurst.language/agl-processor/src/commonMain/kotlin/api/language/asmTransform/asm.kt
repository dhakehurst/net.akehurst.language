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

interface AsmTransformModel {
    val name: String
    val qualifiedName: String

    /**
     * map from grammar-rule name to TransformationRule
     */
    val rules: Map<String, TransformationRule>

    val createObjectRules: List<CreateObjectRule>
    val modifyObjectRules: List<ModifyObjectRule>
}

interface TransformationRule {
    val grammarRuleName: String
    val typeName: String

    val resolvedType: TypeInstance
}

interface NoActionTransformationRule : TransformationRule
interface SubtypeTransformationRule : TransformationRule
interface CreateObjectRule : TransformationRule {
    val modifyStatements: List<AssignmentTransformationStatement>
}

interface ModifyObjectRule : TransformationRule {
    val modifyStatements: List<AssignmentTransformationStatement>
}

interface AssignmentTransformationStatement {
    val lhsPropertyName: String
    val rhs: Expression
}