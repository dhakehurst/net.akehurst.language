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

package net.akehurst.language.agl.language.expressions

import net.akehurst.language.api.language.expressions.*
import net.akehurst.language.typemodel.api.PropertyDeclaration


abstract class ExpressionAbstract : Expression {

    /**
     * defaults to 'toString' if not overriden in subclass
     */
    override fun asString(indent: String, increment: String): String = toString()
}

data class CreateTupleExpressionSimple(
    override val propertyAssignments: List<AssignmentStatement>
) : ExpressionAbstract(), CreateTupleExpression {

    override fun asString(indent: String, increment: String): String {
        val sb = StringBuilder()
        sb.append("${indent}Tuple{\n")
        val ni = indent + increment
        val props = propertyAssignments.joinToString(separator = "\n") { "${ni}${it.asString(ni, increment)}" }
        sb.append(props)
        sb.append("${indent}}")
        return sb.toString()
    }

    override fun toString(): String = "Tuple{ ... }"
}

class WithExpressionSimple(
    override val withContext: Expression,
    override val expression: Expression
) : ExpressionAbstract(), WithExpression {

    override fun toString(): String = "with($withContext) { $expression }"
}

data class RootExpressionSimple(
    override val name: String
) : ExpressionAbstract(), RootExpression {
    companion object {
        const val NOTHING = "\$nothing"
        const val SELF = "\$self"
    }

    override val isNothing: Boolean get() = NOTHING == this.name
    override val isSelf: Boolean get() = SELF == this.name

    override fun toString(): String = name
}

data class LiteralExpressionSimple(
    override val typeName: String,
    override val value: Any
) : ExpressionAbstract(), LiteralExpression {

    companion object {
        const val BOOLEAN = "std.Boolean"
        const val INTEGER = "std.Integer"
        const val REAL = "std.Real"
        const val STRING = "std.String"
    }

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

    val resolvedLhs get() = _resolvedLhs

    private lateinit var _resolvedLhs: PropertyDeclaration

    fun resolveLhsAs(propertyDeclaration: PropertyDeclaration) {
        _resolvedLhs = propertyDeclaration
    }

    override fun asString(indent: String, increment: String): String = "$indent$this"

    override fun toString(): String = "$lhsPropertyName := $rhs"

}