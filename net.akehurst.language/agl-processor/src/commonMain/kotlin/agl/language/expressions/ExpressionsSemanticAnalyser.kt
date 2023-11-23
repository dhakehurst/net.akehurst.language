/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.akehurst.language.agl.language.expressions

import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.agl.processor.SemanticAnalysisResultDefault
import net.akehurst.language.api.grammarTypeModel.GrammarTypeNamespace
import net.akehurst.language.api.language.expressions.Expression
import net.akehurst.language.api.language.expressions.NavigationExpression
import net.akehurst.language.api.language.expressions.RootExpression
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.api.processor.SemanticAnalysisOptions
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.typemodel.api.PropertyDeclaration
import net.akehurst.language.typemodel.api.TypeDeclaration
import net.akehurst.language.typemodel.api.TypeInstance
import net.akehurst.language.typemodel.simple.SimpleTypeModelStdLib

fun TypeDeclaration.typeOfExpressionStr(expression: String): TypeInstance {
    val result = Agl.registry.agl.expressions.processor!!.process(expression)
    check(result.issues.errors.isEmpty()) { result.issues.toString() }
    val asm = result.asm!!
    return asm.typeOfExpressionFor(this.type())
}

fun Expression.typeOfExpressionFor(self: TypeInstance): TypeInstance = when (this) {
    is RootExpression -> this.typeOfExpressionFor(self)
    is NavigationExpression -> this.typeOfExpressionFor(self)
    else -> error("Subtype of Expression not handled in 'typeOfExpressionFor'")
}

fun RootExpression.typeOfExpressionFor(self: TypeInstance): TypeInstance = when {
    this.isNothing -> SimpleTypeModelStdLib.NothingType
    this.isSelf -> self
    else -> error("type of RootExpression not handled")
}

fun NavigationExpression.typeOfExpressionFor(self: TypeInstance): TypeInstance =
    this.propertyDeclarationFor(self)?.typeInstance ?: SimpleTypeModelStdLib.NothingType

fun NavigationExpression.propertyDeclarationFor(self: TypeInstance?): PropertyDeclaration? {
    var typeInstance = self
    var pd: PropertyDeclaration? = null

    for (pn in this.value) {
        pd = typeInstance?.resolvedProperty?.get(pn)
        typeInstance = pd?.typeInstance
    }
    return pd
}

class ExpressionsSemanticAnalyser(
) : SemanticAnalyser<Expression, SentenceContext<String>> {

    private val issues = IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS)
    private var _locationMap: Map<Any, InputLocation> = emptyMap()

    private var _grammarNamespace: GrammarTypeNamespace? = null

    override fun clear() {
        _grammarNamespace = null
        _locationMap = emptyMap()
        issues.clear()
    }

    override fun analyse(
        asm: Expression,
        locationMap: Map<Any, InputLocation>?,
        context: SentenceContext<String>?,
        options: SemanticAnalysisOptions<Expression, SentenceContext<String>>
    ): SemanticAnalysisResult {
        return SemanticAnalysisResultDefault(issues)
    }

}