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
import net.akehurst.language.api.language.expressions.RootExpression
import net.akehurst.language.api.sppt.Sentence
import net.akehurst.language.api.sppt.SpptDataNodeInfo
import net.akehurst.language.collections.toSeparatedList

class ExpressionsSyntaxAnalyser : SyntaxAnalyserByMethodRegistrationAbstract<Expression>() {

    override fun registerHandlers() {
        super.register(this::expression)
        super.register(this::navigation)
        super.register(this::rootExpression)
        super.register(this::nothing)
        super.register(this::self)
        super.register(this::propertyReference)
        super.register(this::qualifiedName)
    }

    data class PropertyValue(
        val asmObject: Any,
        val value: String
    )

    // expression = rootExpression | navigation
    private fun expression(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Expression =
        children[0] as Expression

    //navigation = [propertyReference / '.']+ ;
    private fun navigation(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): NavigationDefault =
        NavigationDefault(children.toSeparatedList<String, String>().items)

    // rootExpression = nothing | self
    private fun rootExpression(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): RootExpression =
        children[0] as RootExpression

    // nothing = '§nothing'
    private fun nothing(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence) =
        RootExpressionDefault(RootExpressionDefault.NOTHING)

    // self = '§self'
    private fun self(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence) =
        RootExpressionDefault(RootExpressionDefault.SELF)

    // propertyReference = IDENTIFIER
    private fun propertyReference(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): String {
        return children[0] as String
    }

    // qualifiedName = [IDENTIFIER / '.']+ ;
    private fun qualifiedName(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<String> {
        return children.toSeparatedList<String, String>().items
    }
}