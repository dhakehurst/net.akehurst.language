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

package net.akehurst.language.expressions.processor

import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserByMethodRegistrationAbstract
import net.akehurst.language.api.syntaxAnalyser.AsmFactory
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.api.asPossiblyQualifiedName
import net.akehurst.language.base.processor.BaseSyntaxAnalyser
import net.akehurst.language.collections.toSeparatedList
import net.akehurst.language.expressions.api.*
import net.akehurst.language.expressions.asm.*
import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sppt.api.SpptDataNodeInfo
import net.akehurst.language.typemodel.asm.SimpleTypeModelStdLib

class ExpressionsSyntaxAnalyser : SyntaxAnalyserByMethodRegistrationAbstract<Expression>() {

    override val extendsSyntaxAnalyser: Map<QualifiedName, SyntaxAnalyser<*>> = mapOf(
        QualifiedName("Base") to BaseSyntaxAnalyser()
    )

    override fun registerHandlers() {
        super.register(this::expression)
        super.register(this::rootExpression)
        super.register(this::literalExpression)
        super.register(this::navigationExpression)
        super.register(this::navigationRoot)
        super.register(this::navigationPartList)
        super.register(this::navigationPart)
        super.register(this::infixExpression)
        super.registerFor("object", this::object_)
        super.register(this::constructorArguments)
        super.register(this::tuple)
        super.register(this::assignmentBlock)
        super.register(this::assignmentList)
        super.register(this::assignment)
        super.register(this::propertyName)
        super.register(this::with)
        super.registerFor("when", this::when_)
        super.register(this::whenOptionList)
        super.register(this::whenOption)
        super.register(this::cast)
        super.register(this::group)
        super.register(this::propertyCall)
        super.register(this::methodCall)
        super.register(this::argumentList)
        super.register(this::lambda)
        super.register(this::indexOperation)
        super.register(this::indexList)
        super.register(this::propertyReference)
        super.register(this::methodReference)
        super.register(this::literal)
    }

    data class PropertyValue(
        val asmObject: Any,
        val value: String
    )

    // expression = root | literal | navigation | tuple  | object | with | when | cast | group
    private fun expression(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Expression =
        children[0] as Expression

    // root = propertyReference ;
    private fun rootExpression(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): RootExpression {
        val v = children[0] as String
        return when {
            v.startsWith("\$") -> when (v) {
                RootExpressionSimple.NOTHING.name -> RootExpressionSimple.NOTHING
                RootExpressionSimple.SELF.name -> RootExpressionSimple.SELF
                else -> RootExpressionSimple(v)
            }

            else -> RootExpressionSimple(v)
        }
    }

    // literalExpression = literal ;
    private fun literalExpression(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): LiteralExpression =
        children[0] as LiteralExpression

    // literal = BOOLEAN | INTEGER | REAL | STRING ;
    private fun literal(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): LiteralExpression = when (nodeInfo.alt.option.asIndex) {
        0 -> LiteralExpressionSimple(SimpleTypeModelStdLib.Boolean.qualifiedTypeName, (children[0] as String).toBoolean())
        1 -> LiteralExpressionSimple(SimpleTypeModelStdLib.Integer.qualifiedTypeName, (children[0] as String).toInt())
        2 -> LiteralExpressionSimple(SimpleTypeModelStdLib.Real.qualifiedTypeName, (children[0] as String).toDouble())
        3 -> LiteralExpressionSimple(SimpleTypeModelStdLib.String.qualifiedTypeName, (children[0] as String).trim('\''))
        else -> error("Internal error: alternative ${nodeInfo.alt.option} not handled for 'literal'")
    }

    // navigation = navigationRoot navigationPartList ;
    private fun navigationExpression(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): NavigationSimple {
        val navigationRoot = children[0] as Expression
        val parts = children[1] as List<NavigationPart>
        return NavigationSimple(navigationRoot, parts)
    }

    // navigationRoot = root | literal ;
    private fun navigationRoot(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Expression = when (nodeInfo.alt.option.asIndex) {
        0 -> children[0] as Expression
        1 -> children[0] as Expression
        else -> error("Internal error: alternative ${nodeInfo.alt.option} not handled for 'navigationRoot'")
    }

    // navigationPartList = navigationPart+ ;
    private fun navigationPartList(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<NavigationPart> =
        children as List<NavigationPart>

    // navigationPart = propertyCall | methodCall | indexOperation ;
    private fun navigationPart(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence) =
        children[0]

    // infix = expression INFIX_OPERATOR expression ;
    // infix = [expression / INFIX_OPERATOR]2+ ;
    private fun infixExpression(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): InfixExpression {
        val expressions = children.filterIsInstance<Expression>()
        val operators = children.filterIsInstance<String>()
        return InfixExpressionSimple(expressions, operators)
    }

    // object = possiblyQualifiedName constructorArguments assignmentBlock? ;
    private fun object_(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): CreateObjectExpression {
        val pqn = children[0] as PossiblyQualifiedName
        val args = children[1] as List<Expression>
        val propertyAssignments = children[2] as List<AssignmentStatement>?
        val exp = CreateObjectExpressionSimple(pqn, args)
        exp.propertyAssignments = propertyAssignments ?: emptyList()
        return exp
    }

    // constructorArguments = '(' argumentList ')' ;
    private fun constructorArguments(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<Expression> =
        children[1] as List<Expression> //TODO: maybe should also be assignments ?


    // tuple = 'tuple' assignmentBlock ;
    private fun tuple(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): CreateTupleExpression {
        val propertyAssignments = children[1] as List<AssignmentStatement>
        return CreateTupleExpressionSimple(propertyAssignments)
    }

    // assignmentBlock = '{' assignmentList  '}' ;
    private fun assignmentBlock(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<AssignmentStatement> =
        children[1] as List<AssignmentStatement>

    // assignmentList = assignment* ;
    private fun assignmentList(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<AssignmentStatement> =
        children as List<AssignmentStatement>

    // assignment = propertyName ':=' expression ;
    private fun assignment(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): AssignmentStatement {
        val lhsPropertyName = children[0] as String
        val rhs = children[2] as Expression
        return AssignmentStatementSimple(lhsPropertyName, rhs)
    }

    // propertyName = IDENTIFIER | SPECIAL
    private fun propertyName(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence) =
        children[0] as String

    // with = 'with' '(' expression ')' expression ;
    private fun with(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): WithExpression {
        val withContext = children[2] as Expression
        val expression = children[4] as Expression
        return WithExpressionSimple(withContext, expression)
    }

    // when = 'when' '{' whenOptionList '}' ;
    private fun when_(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): WhenExpression {
        val optionList = children[2] as List<WhenOption>
        return WhenExpressionSimple(optionList)
    }

    // whenOptionList = whenOption+ ;
    private fun whenOptionList(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<WhenOption> =
        children as List<WhenOption>

    // whenOption = expression '->' expression ;
    private fun whenOption(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): WhenOption {
        val condition = children[0] as Expression
        val expression = children[2] as Expression
        return WhenOptionSimple(condition, expression)
    }

    // cast = expression 'as' possiblyQualifiedName ;
    private fun cast(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): CastExpression {
        val expression = children[0] as Expression
        val pqn = children[2] as PossiblyQualifiedName
        return CastExpressionSimple( expression,pqn)
    }

    // cast = '(' expression ')' ;
    private fun group(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): GroupExpression {
        val expression = children[1] as Expression
        return GroupExpressionSimple( expression)
    }

    // propertyCall = '.' propertyReference ;
    private fun propertyCall(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): PropertyCall {
        val id = children[1] as String
        return PropertyCallSimple(id)
    }

    // methodCall = '.' methodReference '(' argumentList ')' lambda? ;
    private fun methodCall(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): MethodCall {
        val methodReference = children[1] as String
        val argumentList = children[3] as List<Expression>
        val lambda = children[5] as LambdaExpression?
        val args = argumentList + (lambda?.let{ listOf(it) } ?: emptyList())
        return MethodCallSimple(methodReference, args)
    }

    // argumentList = [expression / ',']* ;
    private fun argumentList(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<Expression> {
        return (children as List<Any>).toSeparatedList<Any, Expression, String>().items
    }

    // lambda = '{' expression '}' ;
    private fun lambda(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): LambdaExpression {
        val expression = children[1] as Expression
        return LambdaExpressionSimple(expression)
    }

    // indexOperation = '[' indexList ']' ;
    private fun indexOperation(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): IndexOperation {
        val expr = children[1] as List<Expression>
        return IndexOperationSimple(expr)
    }

    // indexList = [expression / ',']+ ;
    private fun indexList(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<Expression> =
        children.toSeparatedList<Any?, Expression, String>().items

    // propertyReference = IDENTIFIER | SPECIAL
    private fun propertyReference(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence) =
        children[0] as String

    //methodReference = IDENTIFIER ;
    private fun methodReference(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence) =
        children[0] as String

}