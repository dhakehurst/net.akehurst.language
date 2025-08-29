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
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.processor.BaseSyntaxAnalyser
import net.akehurst.language.collections.toSeparatedList
import net.akehurst.language.expressions.api.*
import net.akehurst.language.expressions.asm.*
import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sppt.api.SpptDataNodeInfo
import net.akehurst.language.types.asm.StdLibDefault

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
        super.register(this::grammarRuleIndex)
        super.register(this::with)
        super.registerFor("when", this::when_)
        super.register(this::whenOption)
        super.register(this::whenOptionElse)
        super.register(this::cast)
        super.register(this::typeTest)
        super.register(this::group)
        super.register(this::propertyCall)
        super.register(this::methodCall)
        super.register(this::argumentList)
        super.register(this::lambda)
        super.register(this::propertyReference)
        super.register(this::methodReference)
        super.register(this::indexOperation)
        super.register(this::indexList)
        super.register(this::typeReference)
        super.register(this::typeArgumentList)
        super.register(this::literal)
    }

    data class PropertyValue(
        val asmObject: Any,
        val value: String
    )

    // expression = root | literal | navigation | tuple  | object | with | when | cast | typeTest | group
    private fun expression(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Expression =
        children[0] as Expression

    // root = propertyReference ;
    private fun rootExpression(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): RootExpression {
        val v = children[0] as String
        return when {
            v.startsWith("\$") -> when (v) {
                RootExpressionDefault.NOTHING.name -> RootExpressionDefault.NOTHING
                RootExpressionDefault.SELF.name -> RootExpressionDefault.SELF
                else -> RootExpressionDefault(v)
            }

            else -> RootExpressionDefault(v)
        }
    }

    // literalExpression = literal ;
    private fun literalExpression(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): LiteralExpression =
        children[0] as LiteralExpression

    // literal = BOOLEAN | INTEGER | REAL | STRING ;
    private fun literal(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): LiteralExpression = when (nodeInfo.alt.option.asIndex) {
        0 -> LiteralExpressionDefault(StdLibDefault.Boolean.qualifiedTypeName, (children[0] as String).toBoolean())
        1 -> LiteralExpressionDefault(StdLibDefault.Integer.qualifiedTypeName, (children[0] as String).toLong())
        2 -> LiteralExpressionDefault(StdLibDefault.Real.qualifiedTypeName, (children[0] as String).toDouble())
        3 -> LiteralExpressionDefault(StdLibDefault.String.qualifiedTypeName, (children[0] as String).trim('\''))
        else -> error("Internal error: alternative ${nodeInfo.alt.option} not handled for 'literal'")
    }

    // navigation = navigationRoot navigationPartList ;
    private fun navigationExpression(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): NavigationExpressionDefault {
        val navigationRoot = children[0] as Expression
        val parts = children[1] as List<NavigationPart>
        return NavigationExpressionDefault(navigationRoot, parts)
    }

    // navigationRoot = root | literal | group;
    private fun navigationRoot(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Expression =
        children[0] as Expression

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
        return InfixExpressionDefault(expressions, operators)
    }

    // object = possiblyQualifiedName constructorArguments assignmentBlock? ;
    private fun object_(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): CreateObjectExpression {
        val pqn = children[0] as PossiblyQualifiedName
        val args = children[1] as List<AssignmentStatement>
        val propertyAssignments = children[2] as List<AssignmentStatement>?
        val exp = CreateObjectExpressionDefault(pqn, args)
        exp.propertyAssignments = propertyAssignments ?: emptyList()
        return exp
    }

    // constructorArguments = '(' assignmentList ')' ;
    private fun constructorArguments(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<AssignmentStatement> =
        children[1] as List<AssignmentStatement> //TODO: maybe should also be assignments ?


    // tuple = 'tuple' assignmentBlock ;
    private fun tuple(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): CreateTupleExpression {
        val propertyAssignments = children[1] as List<AssignmentStatement>
        return CreateTupleExpressionDefault(propertyAssignments)
    }

    // assignmentBlock = '{' assignmentList  '}' ;
    private fun assignmentBlock(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<AssignmentStatement> =
        children[1] as List<AssignmentStatement>

    // assignmentList = assignment* ;
    private fun assignmentList(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<AssignmentStatement> =
        children as List<AssignmentStatement>

    // assignment = propertyName  grammarRuleIndex? ':=' expression ;
    private fun assignment(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): AssignmentStatement {
        val lhsPropertyName = children[0] as String
        val  lhsGrammarRuleIndex = children[1] as Int?
        val rhs = children[3] as Expression
        return AssignmentStatementDefault(lhsPropertyName, lhsGrammarRuleIndex, rhs)
    }

    // propertyName = IDENTIFIER | SPECIAL
    private fun propertyName(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence) =
        children[0] as String

    // grammarRuleIndex = '$' POSITIVE_INTEGER ;
    private fun grammarRuleIndex(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Int =
        (children[1] as String).toInt()

    // with = 'with' '(' expression ')' expression ;
    private fun with(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): WithExpression {
        val withContext = children[2] as Expression
        val expression = children[4] as Expression
        return WithExpressionDefault(withContext, expression)
    }

    // when = 'when' '{' whenOption+ whenOptionElse '}' ;
    private fun when_(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): WhenExpression {
        val optionList = children[2] as List<WhenOption>
        val whenOptionElse = children[3] as WhenOptionElse
        return WhenExpressionDefault(optionList, whenOptionElse)
    }

    // whenOption = expression '->' expression ;
    private fun whenOption(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): WhenOption {
        val condition = children[0] as Expression
        val expression = children[2] as Expression
        return WhenOptionDefault(condition, expression)
    }

    // whenOption = 'else' '->' expression ;
    private fun whenOptionElse(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): WhenOptionElse {
        val expression = children[2] as Expression
        return WhenOptionElseDefault(expression)
    }

    // cast = expression 'as' typeReference ;
    private fun cast(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): CastExpression {
        val expression = children[0] as Expression
        val pqn = children[2] as TypeReference
        return CastExpressionDefault(expression, pqn)
    }

    // typeTest = expression 'is' typeReference ;
    private fun typeTest(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): TypeTestExpression {
        val expression = children[0] as Expression
        val pqn = children[2] as TypeReference
        return TypeTestExpressionDefault(expression, pqn)
    }

    // group = '(' expression ')' ;
    private fun group(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): GroupExpression {
        val expression = children[1] as Expression
        return GroupExpressionDefault(expression)
    }

    // propertyCall = '.' propertyReference ;
    private fun propertyCall(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): PropertyCall {
        val id = children[1] as String
        return PropertyCallDefault(id)
    }

    // methodCall = '.' methodReference '(' argumentList ')' lambda? ;
    private fun methodCall(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): MethodCall {
        val methodReference = children[1] as String
        val argumentList = children[3] as List<Expression>
        val lambda = children[5] as LambdaExpression?
        val args = argumentList + (lambda?.let { listOf(it) } ?: emptyList())
        return MethodCallDefault(methodReference, args)
    }

    // argumentList = [expression / ',']* ;
    private fun argumentList(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<Expression> {
        return (children as List<Any>).toSeparatedList<Any, Expression, String>().items
    }

    // lambda = '{' expression '}' ;
    private fun lambda(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): LambdaExpression {
        val expression = children[1] as Expression
        return LambdaExpressionDefault(expression)
    }

    // indexOperation = '[' indexList ']' ;
    private fun indexOperation(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): IndexOperation {
        val expr = children[1] as List<Expression>
        return IndexOperationDefault(expr)
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

    // typeReference = possiblyQualifiedName typeArgumentList? '?'?;
    private fun typeReference(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): TypeReference {
        val pqn = children[0] as PossiblyQualifiedName
        val targs = (children[1] as List<TypeReference>?) ?: emptyList()
        val isNullable = children[2] != null
        return TypeReferenceDefault(pqn, targs, isNullable)
    }

    // typeArgumentList = '<' [ typeReference / ',']+ '>' ;
    private fun typeArgumentList(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<TypeReference> {
        val list = children[1] as List<Any>
        val slist = list.toSeparatedList<Any, TypeReference, String>()
        return slist.items
    }

}