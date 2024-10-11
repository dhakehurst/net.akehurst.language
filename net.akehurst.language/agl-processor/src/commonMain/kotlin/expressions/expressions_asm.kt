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

package net.akehurst.language.expressions.asm

import net.akehurst.language.base.api.Indent
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.expressions.api.*

abstract class ExpressionAbstract : Expression {

    /**
     * defaults to 'toString' if not overriden in subclass
     */
    override fun asString(indent: Indent): String = toString()
}

data class CreateTupleExpressionSimple(
    override val propertyAssignments: List<AssignmentStatement>
) : ExpressionAbstract(), CreateTupleExpression {

    override fun asString(indent: Indent): String {
        val sb = StringBuilder()
        sb.append("tuple {\n")
        val ni = indent.inc
        val props = propertyAssignments.joinToString(separator = "\n") { "${ni}${it.asString(ni)}" }
        sb.append("${props}\n")
        sb.append("${indent}}")
        return sb.toString()
    }

    override fun toString(): String = "tuple { ... }"
}

data class CreateObjectExpressionSimple(
    override val possiblyQualifiedTypeName: PossiblyQualifiedName,
    override val arguments: List<Expression>
) : ExpressionAbstract(), CreateObjectExpression {

    override var propertyAssignments: List<AssignmentStatement> = emptyList()

    override fun asString(indent: Indent): String {
        val sb = StringBuilder()
        sb.append("$possiblyQualifiedTypeName {\n")
        val ni = indent.inc
        val props = propertyAssignments.joinToString(separator = "\n") { "${ni}${it.asString(ni)}" }
        sb.append("${props}\n")
        sb.append("${indent}}")
        return sb.toString()
    }

    override fun toString(): String = "$possiblyQualifiedTypeName { ... }"
}

class WithExpressionSimple(
    override val withContext: Expression,
    override val expression: Expression
) : ExpressionAbstract(), WithExpression {

    override fun asString(indent: Indent): String {
        val sb = StringBuilder()
        sb.append("with(${withContext.asString(indent)}) {\n")
        val ni = indent.inc
        sb.append("${ni}${expression.asString(ni)}\n")
        sb.append("${indent}}")
        return sb.toString()
    }

    override fun toString(): String = "with($withContext) $expression"
}

class WhenExpressionSimple(
    override val options: List<WhenOption>
) : ExpressionAbstract(), WhenExpression {

    override fun asString(indent: Indent): String {
        val sb = StringBuilder()
        sb.append("when {\n")
        val ni = indent.inc
        val opts = options.joinToString(separator = "\n") { "${it.condition.asString(ni)} -> ${it.expression.asString(ni.inc)}" }
        sb.append("${opts}\n")
        sb.append("${indent}}")
        return sb.toString()
    }

    override fun toString(): String = "when { ${options.joinToString(separator = " ") { it.toString() }} }"
}

class OnExpressionSimple(
    override val expression: Expression
) : ExpressionAbstract(), OnExpression {
    override var propertyAssignments: List<AssignmentStatement> = emptyList()
}

class WhenOptionSimple(
    override val condition: Expression,
    override val expression: Expression
) : WhenOption {
    override fun toString(): String = "$condition -> $expression"
}

data class RootExpressionSimple(
    override val name: String
) : ExpressionAbstract(), RootExpression {
    companion object {
        val NOTHING = RootExpressionSimple("\$nothing")
        val SELF = RootExpressionSimple("\$self")
        val ERROR = RootExpressionSimple("\$error")
    }

    override val isNothing: Boolean get() = NOTHING == this
    override val isSelf: Boolean get() = SELF == this

    override fun toString(): String = name
    override fun hashCode(): Int = name.hashCode()
    override fun equals(other: Any?): Boolean = when (other) {
        !is RootExpressionSimple -> false
        else -> other.name == this.name
    }
}

data class LiteralExpressionSimple(
    override val qualifiedTypeName: QualifiedName,
    override val value: Any
) : ExpressionAbstract(), LiteralExpression {

    override fun toString(): String = value.toString()
}

data class NavigationSimple(
    override val start: Expression,
    override val parts: List<NavigationPart>
) : ExpressionAbstract(), NavigationExpression {

    override fun toString(): String = "$start${parts.joinToString(separator = "")}"
}

data class PropertyCallSimple(
    override val propertyName: String
) : PropertyCall {
    override fun toString(): String = ".$propertyName"
}

data class MethodCallSimple(
    override val methodName: String,
    override val arguments: List<Expression>
) : MethodCall {

    override fun toString(): String = ".$methodName(${arguments.joinToString()})"
}

data class IndexOperationSimple(
    override val indices: List<Expression>
) : IndexOperation {

    override fun toString(): String = "[${indices.joinToString { it.toString() }}]"
}

class AssignmentStatementSimple(
    override val lhsPropertyName: String,
    override val rhs: Expression
) : AssignmentStatement {

    //val resolvedLhs get() = _resolvedLhs

    //private lateinit var _resolvedLhs: PropertyDeclaration

   // fun resolveLhsAs(propertyDeclaration: PropertyDeclaration) {
    //    _resolvedLhs = propertyDeclaration
   // }

    override fun asString(indent: Indent): String = "$lhsPropertyName := ${rhs.asString(indent)}"

    override fun toString(): String = "$lhsPropertyName := $rhs"

}

class InfixExpressionSimple(
    override val expressions: List<Expression>,
    override val operators: List<String>
) : InfixExpression {
    override fun asString(indent: Indent): String = "$indent$this"

    override fun toString(): String = "${expressions.first()} ${operators.indices.joinToString { operators[it] + " " + expressions[it + 1] }}"
}