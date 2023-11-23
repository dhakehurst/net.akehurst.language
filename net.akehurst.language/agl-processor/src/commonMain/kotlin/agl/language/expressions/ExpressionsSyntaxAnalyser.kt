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

import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserByMethodRegistrationAbstract
import net.akehurst.language.api.language.expressions.Expression
import net.akehurst.language.api.language.expressions.LiteralExpression
import net.akehurst.language.api.language.expressions.RootExpression
import net.akehurst.language.api.sppt.Sentence
import net.akehurst.language.api.sppt.SpptDataNodeInfo
import net.akehurst.language.collections.toSeparatedList

class ExpressionsSyntaxAnalyser : SyntaxAnalyserByMethodRegistrationAbstract<Expression>() {

    override fun registerHandlers() {
        super.register(this::expression)
        super.register(this::navigation)
        super.register(this::root)
        super.register(this::literal)
        super.register(this::propertyReference)
        super.register(this::qualifiedName)
    }

    data class PropertyValue(
        val asmObject: Any,
        val value: String
    )

    // expression = root | literal | navigation
    private fun expression(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Expression =
        children[0] as Expression

    // navigation = [propertyReference / '.']+ ;
    private fun navigation(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): NavigationDefault =
        NavigationDefault((children as List<String>).toSeparatedList<String, String, String>().items)

    // root = NOTHING | SELF ;
    private fun root(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): RootExpression = when (nodeInfo.alt.option) {
        0 -> RootExpressionDefault(RootExpressionDefault.NOTHING)
        1 -> RootExpressionDefault(RootExpressionDefault.SELF)
        else -> error("Internal error: alternative ${nodeInfo.alt.option} not handled for 'root'")
    }

    // literal = BOOLEAN | INTEGER | REAL | STRING ;
    private fun literal(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): LiteralExpression = when (nodeInfo.alt.option) {
        0 -> LiteralExpressionDefault(LiteralExpressionDefault.BOOLEAN, children[0] as String)
        1 -> LiteralExpressionDefault(LiteralExpressionDefault.INTEGER, children[0] as String)
        2 -> LiteralExpressionDefault(LiteralExpressionDefault.REAL, children[0] as String)
        3 -> LiteralExpressionDefault(LiteralExpressionDefault.STRING, children[0] as String)
        else -> error("Internal error: alternative ${nodeInfo.alt.option} not handled for 'literal'")
    }

    // qualifiedName = [IDENTIFIER / '.']+ ;
    private fun qualifiedName(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<String> {
        return (children as List<String>).toSeparatedList<String, String, String>().items
    }

    // propertyReference = IDENTIFIER
    private fun propertyReference(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): String {
        return children[0] as String
    }

    // leaf BOOLEAN = "true|false" ;
    private fun BOOLEAN(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence) =
        RootExpressionDefault(RootExpressionDefault.SELF)
}