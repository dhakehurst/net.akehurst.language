package net.akehurst.language.examples.simple

data class SimpleExampleUnit(
        val name: String
) {
}

data class ClassDefinition(
        val name: String
) {
    val properties = mutableListOf<PropertyDefinition>()
    val methods = mutableListOf<MethodDefinition>()
    val members get() = properties + methods
}

data class PropertyDefinition(
        val name: String,
        val typeName: String
)

data class MethodDefinition(
        val name: String
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
