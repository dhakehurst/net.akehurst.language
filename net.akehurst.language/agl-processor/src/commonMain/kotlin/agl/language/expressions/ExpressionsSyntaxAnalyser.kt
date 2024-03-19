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

import net.akehurst.language.agl.agl.language.base.BaseSyntaxAnalyser
import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserByMethodRegistrationAbstract
import net.akehurst.language.api.language.expressions.*
import net.akehurst.language.api.sppt.Sentence
import net.akehurst.language.api.sppt.SpptDataNodeInfo
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.collections.toSeparatedList

class ExpressionsSyntaxAnalyser : SyntaxAnalyserByMethodRegistrationAbstract<Expression>() {

    override val extendsSyntaxAnalyser: Map<String, SyntaxAnalyser<*>> = mapOf(
        "Base" to BaseSyntaxAnalyser()
    )

    override fun registerHandlers() {
        super.register(this::expression)
        super.register(this::root)
        super.register(this::literal)
        super.register(this::navigation)
        super.register(this::navigationRoot)
        super.register(this::navigationPartList)
        super.register(this::navigationPart)
        super.register(this::tuple)
        super.register(this::assignmentList)
        super.register(this::assignment)
        super.register(this::propertyName)
        super.register(this::with)
        super.register(this::propertyCall)
        super.register(this::methodCall)
        super.register(this::indexOperation)
        super.register(this::indexList)
        super.register(this::propertyReference)
    }

    data class PropertyValue(
        val asmObject: Any,
        val value: String
    )

    // expression = root | literal | navigation
    private fun expression(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Expression =
        children[0] as Expression

    // root = NOTHING | SELF | propertyReference | SPECIAL ;
    private fun root(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): RootExpression = when (nodeInfo.alt.option) {
        0 -> RootExpressionSimple(RootExpressionSimple.NOTHING)
        1 -> RootExpressionSimple(RootExpressionSimple.SELF)
        2 -> RootExpressionSimple(children[0] as String)
        3 -> RootExpressionSimple(children[0] as String)
        else -> error("Internal error: alternative ${nodeInfo.alt.option} not handled for 'root'")
    }

    // literal = BOOLEAN | INTEGER | REAL | STRING ;
    private fun literal(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): LiteralExpression = when (nodeInfo.alt.option) {
        0 -> LiteralExpressionSimple(LiteralExpressionSimple.BOOLEAN, (children[0] as String).toBoolean())
        1 -> LiteralExpressionSimple(LiteralExpressionSimple.INTEGER, (children[0] as String).toInt())
        2 -> LiteralExpressionSimple(LiteralExpressionSimple.REAL, (children[0] as String).toDouble())
        3 -> LiteralExpressionSimple(LiteralExpressionSimple.STRING, (children[0] as String))
        else -> error("Internal error: alternative ${nodeInfo.alt.option} not handled for 'literal'")
    }

    // navigation = navigationRoot navigationPartList ;
    private fun navigation(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): NavigationSimple {
        val navigationRoot = children[0] as Expression
        val parts = children[1] as List<NavigationPart>
        return NavigationSimple(navigationRoot, parts)
    }

    // navigationRoot = root | literal ;
    private fun navigationRoot(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Expression = when (nodeInfo.alt.option) {
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

    // tuple = 'tuple' '{' assignmentList  '}' ;
    private fun tuple(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): CreateTupleExpression {
        val propertyAssignments = children[2] as List<AssignmentStatement>
        return CreateTupleExpressionSimple(propertyAssignments)
    }

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
    private fun propertyName(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): String {
        return children[0] as String
    }

    //    with = 'with' '(' expression ')' expression ;
    private fun with(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): WithExpression {
        val withContext = children[2] as Expression
        val expression = children[4] as Expression
        return WithExpressionSimple(withContext, expression)
    }

    // propertyCall = '.' IDENTIFIER ;
    private fun propertyCall(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): PropertyCall {
        val id = children[1] as String
        return PropertyCallSimple(id)
    }

    // methodCall = '.' IDENTIFIER '(' ')' ;
    private fun methodCall(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): MethodCall {
        val id = children[1] as String
        //TODO: arguments
        return MethodCallSimple(id, emptyList())
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
    private fun propertyReference(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): String {
        return children[0] as String
    }

}