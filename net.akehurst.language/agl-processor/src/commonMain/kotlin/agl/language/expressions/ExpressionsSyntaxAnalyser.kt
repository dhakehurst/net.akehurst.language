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
        super.register(this::propertyCall)
        super.register(this::methodCall)
        super.register(this::indexOperation)
        super.register(this::propertyReference)
    }

    data class PropertyValue(
        val asmObject: Any,
        val value: String
    )

    // expression = root | literal | navigation
    private fun expression(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Expression =
        children[0] as Expression

    // root = NOTHING | SELF | propertyReference ;
    private fun root(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): RootExpression = when (nodeInfo.alt.option) {
        0 -> RootExpressionDefault(RootExpressionDefault.NOTHING)
        1 -> RootExpressionDefault(RootExpressionDefault.SELF)
        2 -> RootExpressionDefault(children[0] as String)
        else -> error("Internal error: alternative ${nodeInfo.alt.option} not handled for 'root'")
    }

    // literal = BOOLEAN | INTEGER | REAL | STRING ;
    private fun literal(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): LiteralExpression = when (nodeInfo.alt.option) {
        0 -> LiteralExpressionDefault(LiteralExpressionDefault.BOOLEAN, (children[0] as String).toBoolean())
        1 -> LiteralExpressionDefault(LiteralExpressionDefault.INTEGER, (children[0] as String).toInt())
        2 -> LiteralExpressionDefault(LiteralExpressionDefault.REAL, (children[0] as String).toDouble())
        3 -> LiteralExpressionDefault(LiteralExpressionDefault.STRING, (children[0] as String))
        else -> error("Internal error: alternative ${nodeInfo.alt.option} not handled for 'literal'")
    }

    // navigation = navigationRoot navigationPartList ;
    private fun navigation(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): NavigationDefault {
        val navigationRoot = children[0] as Expression
        val parts = children[1] as List<NavigationPart>
        return NavigationDefault(navigationRoot, parts)
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

    // propertyCall = '.' IDENTIFIER ;
    private fun propertyCall(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): PropertyCall {
        val id = children[1] as String
        return PropertyCallDefault(id)
    }

    // methodCall = '.' IDENTIFIER '(' ')' ;
    private fun methodCall(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): MethodCall {
        val id = children[1] as String
        //TODO: arguments
        return MethodCallDefault(id, emptyList())
    }

    // indexOperation = '[' expression+ ']' ;
    private fun indexOperation(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): IndexOperation {
        val expr = children[1] as List<Expression>
        return IndexOperationDefault(expr)
    }

    // propertyReference = IDENTIFIER
    private fun propertyReference(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): String {
        return children[0] as String
    }

}