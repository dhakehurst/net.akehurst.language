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

package net.akehurst.language.expressions.api

import net.akehurst.language.base.api.Import
import net.akehurst.language.base.api.Indent
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.QualifiedName

interface Expression {
    fun asString(indent: Indent, imports: List<Import> = emptyList()): String
}

interface RootExpression : Expression {
    val name: String
    val isNothing: Boolean
    val isSelf: Boolean
}

interface LiteralExpression : Expression {
    val qualifiedTypeName: QualifiedName
    val value: Any
}

interface CreateObjectExpression : Expression {
    val possiblyQualifiedTypeName: PossiblyQualifiedName
    val constructorArguments: List<AssignmentStatement>
    val propertyAssignments: List<AssignmentStatement>
}

interface FunctionOrConstructorCall {
    val possiblyQualifiedName: PossiblyQualifiedName
    val arguments: List<Expression>
}

interface CreateTupleExpression : Expression {
    val propertyAssignments: List<AssignmentStatement>
}

interface OnExpression : Expression {
    val expression: Expression
    val propertyAssignments: List<AssignmentStatement>
}

interface NavigationExpression : Expression {
    val start: Expression
    val parts: List<NavigationPart>
}

interface NavigationPart

interface PropertyCall : NavigationPart {
    val propertyName: String
}

interface MethodCall : NavigationPart {
    val methodName: String
    val arguments: List<Expression>
}

interface LambdaExpression : Expression {
    val expression: Expression
}

interface IndexOperation : NavigationPart {
    val indices: List<Expression>
}

interface AssignmentStatement {
    val lhsPropertyName: String
    val lhsGrammarRuleIndex: Int?
    val rhs: Expression

    fun asString(indent: Indent, imports: List<Import> = emptyList()): String
}

interface WithExpression : Expression {
    val withContext: Expression
    val expression: Expression
}

interface WhenExpression : Expression {
    val options: List<WhenOption>
    val elseOption: WhenOptionElse
}

interface WhenOption {
    val condition: Expression
    val expression: Expression
}

interface WhenOptionElse {
    val expression: Expression
}

interface InfixExpression : Expression {
    val expressions: List<Expression>
    val operators: List<String>
}

interface CastExpression  : Expression {
    val expression: Expression
    val targetType:TypeReference
}

interface TypeTestExpression  : Expression {
    val expression: Expression
    val targetType:TypeReference
}

interface TypeReference {
    val possiblyQualifiedName:PossiblyQualifiedName
    val typeArguments:List<TypeReference>
    val isNullable:Boolean

    fun asString(indent: Indent, imports: List<Import>): String
}

interface GroupExpression : Expression {
    val expression: Expression
}