/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.examples.simple

data class SimpleExampleUnit(
    val definition : List<Definition>
)

abstract class Definition

data class ClassDefinition(
        val name: String
) : Definition() {
    val properties = mutableListOf<PropertyDefinition>()
    val methods = mutableListOf<MethodDefinition>()
    val members get() = properties + methods
}

data class PropertyDefinition(
        val name: String,
        val typeName: String
)
data class ParameterDefinition(
        val name: String,
        val typeName: String
)
data class MethodDefinition(
        val name: String,
        val paramList:List<ParameterDefinition>
) {
    val body = mutableListOf<Statement>()
}

abstract class Statement

data class StatementReturn(
        val expression: Expression
) : Statement()

abstract class Expression

data class ExpressionLiteral(
        val value: Any
) : Expression()

data class ExpressionVariableReference(
        val value: String
) : Expression()

data class ExpressionInfixOperator(
        val lhs: Expression,
        val operator: String,
        val rhs: Expression
) : Expression()
