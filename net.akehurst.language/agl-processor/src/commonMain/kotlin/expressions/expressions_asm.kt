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

import net.akehurst.language.base.api.Import
import net.akehurst.language.base.api.Indent
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.expressions.api.*

abstract class ExpressionAbstract : Expression {

    /**
     * defaults to 'toString' if not overriden in subclass
     */
    override fun asString(indent: Indent, imports: List<Import>): String = toString()
}

data class CreateTupleExpressionDefault(
    override val propertyAssignments: List<AssignmentStatement>
) : ExpressionAbstract(), CreateTupleExpression {

    override fun asString(indent: Indent, imports: List<Import>): String {
        val sb = StringBuilder()
        sb.append("tuple {\n")
        val ni = indent.inc
        val props = propertyAssignments.joinToString(separator = "\n") { "${ni}${it.asString(ni, imports)}" }
        sb.append("${props}\n")
        sb.append("${indent}}")
        return sb.toString()
    }

    override fun toString(): String = "tuple { ... }"
}

data class CreateObjectExpressionDefault(
    override val possiblyQualifiedTypeName: PossiblyQualifiedName,
    override val arguments: List<Expression>
) : ExpressionAbstract(), CreateObjectExpression {

    override var propertyAssignments: List<AssignmentStatement> = emptyList()

    override fun asString(indent: Indent, imports: List<Import>): String {
        val sb = StringBuilder()
        val pqn = when {
            imports.any { it.value == possiblyQualifiedTypeName.asQualifiedName(null).front.value } -> possiblyQualifiedTypeName.simpleName.value
            else -> possiblyQualifiedTypeName.value
        }
        sb.append("$pqn {\n")
        val ni = indent.inc
        val props = propertyAssignments.joinToString(separator = "\n") { "${ni}${it.asString(ni, imports)}" }
        sb.append("${props}\n")
        sb.append("${indent}}")
        return sb.toString()
    }

    override fun toString(): String = "$possiblyQualifiedTypeName { ... }"
}

class WithExpressionDefault(
    override val withContext: Expression,
    override val expression: Expression
) : ExpressionAbstract(), WithExpression {

    override fun asString(indent: Indent, imports: List<Import>): String {
        val sb = StringBuilder()
        sb.append("with(${withContext.asString(indent, imports)}) ")
        val ni = indent.inc
        sb.append(expression.asString(ni, imports))
        return sb.toString()
    }

    override fun toString(): String = "with($withContext) $expression"
}

class WhenExpressionDefault(
    override val options: List<WhenOption>,
    override val elseOption: WhenOptionElse,
) : ExpressionAbstract(), WhenExpression {

    override fun asString(indent: Indent, imports: List<Import>): String {
        val sb = StringBuilder()
        sb.append("when {\n")
        val ni = indent.inc
        val opts = options.joinToString(separator = "\n") { "${it.condition.asString(ni, imports)} -> ${it.expression.asString(ni.inc, imports)}" }
        sb.append("${opts}\n")
        sb.append("${indent}}")
        return sb.toString()
    }

    override fun toString(): String = "when { ${options.joinToString(separator = " ") { it.toString() }} }"
}

class OnExpressionDefault(
    override val expression: Expression
) : ExpressionAbstract(), OnExpression {
    override var propertyAssignments: List<AssignmentStatement> = emptyList()
}

class WhenOptionDefault(
    override val condition: Expression,
    override val expression: Expression
) : WhenOption {
    override fun toString(): String = "$condition -> $expression"
}

class WhenOptionElseDefault(
    override val expression: Expression
) : WhenOptionElse {

    override fun toString(): String = "else -> $expression"
}


data class RootExpressionDefault(
    override val name: String
) : ExpressionAbstract(), RootExpression {
    companion object {
        val NOTHING = RootExpressionDefault("\$nothing")
        val SELF = RootExpressionDefault("\$self")
        val ERROR = RootExpressionDefault("\$error")
    }

    override val isNothing: Boolean get() = NOTHING == this
    override val isSelf: Boolean get() = SELF == this

    override fun toString(): String = name
    override fun hashCode(): Int = name.hashCode()
    override fun equals(other: Any?): Boolean = when (other) {
        !is RootExpressionDefault -> false
        else -> other.name == this.name
    }
}

data class LiteralExpressionDefault(
    override val qualifiedTypeName: QualifiedName,
    override val value: Any
) : ExpressionAbstract(), LiteralExpression {

    override fun toString(): String = value.toString()
}

data class NavigationExpressionDefault(
    override val start: Expression,
    override val parts: List<NavigationPart>
) : ExpressionAbstract(), NavigationExpression {

    override fun toString(): String = "$start${parts.joinToString(separator = "")}"
}

data class PropertyCallDefault(
    override val propertyName: String
) : PropertyCall {
    override fun toString(): String = ".$propertyName"
}

data class MethodCallDefault(
    override val methodName: String,
    override val arguments: List<Expression>
) : MethodCall {

    override fun toString(): String = ".$methodName(${arguments.joinToString()})"
}

data class LambdaExpressionDefault(
    override val expression: Expression
) : LambdaExpression {

    override fun asString(indent: Indent, imports: List<Import>): String {
        return "{ ${expression.asString(indent, imports)}} }"
    }

    override fun toString(): String = "{ $expression }"
}

data class IndexOperationDefault(
    override val indices: List<Expression>
) : IndexOperation {

    override fun toString(): String = "[${indices.joinToString { it.toString() }}]"
}

class AssignmentStatementDefault(
    override val lhsPropertyName: String,
    override val lhsGrammarRuleIndex: Int?,
    override val rhs: Expression
) : AssignmentStatement {

    override fun asString(indent: Indent, imports: List<Import>): String = "$lhsPropertyName := ${rhs.asString(indent, imports)}"

    override fun toString(): String = "$lhsPropertyName := $rhs"

}

class InfixExpressionDefault(
    override val expressions: List<Expression>,
    override val operators: List<String>
) : InfixExpression {
    override fun asString(indent: Indent, imports: List<Import>): String = "$indent$this"

    override fun toString(): String = "${expressions.first()} ${operators.indices.joinToString { operators[it] + " " + expressions[it + 1] }}"
}

class CastExpressionDefault(
    override val expression: Expression,
    override val targetType: TypeReference
) : CastExpression {
    override fun asString(indent: Indent, imports: List<Import>): String {
        val ttn = targetType.asString(indent, imports)
        return "${expression.asString(indent, imports)} as $ttn"
    }
    override fun toString(): String = "$expression as $targetType"
}

class TypeTestExpressionDefault(
    override val expression: Expression,
    override val targetType: TypeReference
) : TypeTestExpression {
    override fun asString(indent: Indent, imports: List<Import>): String {
        val ttn = targetType.asString(indent, imports)
        return "${expression.asString(indent, imports)} as $ttn"
    }
    override fun toString(): String = "$expression as $targetType"
}

data class TypeReferenceDefault(
    override val possiblyQualifiedName: PossiblyQualifiedName,
    override val typeArguments: List<TypeReference>,
    override val isNullable: Boolean
) : TypeReference {
    override fun asString(indent: Indent, imports: List<Import>): String {
        val tn = when {
            imports.any { it.asQualifiedName.value == possiblyQualifiedName.value } -> possiblyQualifiedName.simpleName.value
            else -> possiblyQualifiedName.value
        }
        val targs = when {
            typeArguments.isEmpty() -> ""
            else -> "<${typeArguments.joinToString { it.asString(indent, imports) }}>"
        }
        return "$tn$targs"
    }

    override fun toString(): String = "${possiblyQualifiedName.value}: $typeArguments ${if (isNullable) "?" else ""}"
}

class GroupExpressionDefault(
    override val expression: Expression
) : GroupExpression {
    override fun asString(indent: Indent, imports: List<Import>): String = "(${expression.asString(indent, imports)})"

    override fun toString(): String = "($expression)"
}