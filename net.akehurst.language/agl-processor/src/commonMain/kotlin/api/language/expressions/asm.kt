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

package net.akehurst.language.api.language.expressions

interface Expression

interface RootExpression : Expression {
    val name: String
    val isNothing: Boolean
    val isSelf: Boolean
}

interface LiteralExpression : Expression {
    val typeName: String
    val value: Any
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

interface IndexOperation : NavigationPart {
    val indices: List<Expression>
}

