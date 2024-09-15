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

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.agl.processor.SemanticAnalysisResultDefault
import net.akehurst.language.api.grammarTypeModel.GrammarTypeNamespace
import net.akehurst.language.api.language.expressions.*
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.api.processor.SemanticAnalysisOptions
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.parser.api.InputLocation
import net.akehurst.language.typemodel.api.*
import net.akehurst.language.typemodel.simple.SimpleTypeModelStdLib

fun TypeDeclaration.typeOfExpressionStr(expression: String): TypeInstance? {
    val result = Agl.registry.agl.expressions.processor!!.process(expression)
    check(result.issues.errors.isEmpty()) { result.issues.toString() }
    val asm = result.asm!!
    return asm.typeOfExpressionFor(this.type())
}

fun Expression.typeOfExpressionFor(self: TypeInstance): TypeInstance? = when (this) {
    is RootExpression -> this.typeOfRootExpressionFor(self)
    is NavigationExpression -> this.typeOfNavigationExpressionFor(self)
    else -> error("Subtype of Expression not handled in 'typeOfExpressionFor'")
}

fun RootExpression.typeOfRootExpressionFor(self: TypeInstance): TypeInstance = when {
    this.isNothing -> SimpleTypeModelStdLib.NothingType
    this.isSelf -> self
    else -> {
        when (self.declaration) {
            is StructuredType -> {
                self.resolvedProperty[PropertyName(this.name)]?.typeInstance ?: error("type of RootExpression '$self' not handled")
            }

            else -> error("type of RootExpression '$self' not handled")
        }
    }
}

fun NavigationExpression.typeOfNavigationExpressionFor(self: TypeInstance): TypeInstance? {
    val st = this.start
    val start = when (st) {
        is LiteralExpression -> TODO()
        is RootExpression -> when {
            st.isNothing -> SimpleTypeModelStdLib.NothingType
            st.isSelf -> self
            else -> {
                self.resolvedProperty[PropertyName(st.name)]?.typeInstance
            }
        }

        else -> error("type of Navigation.start not handled")
    }
    val r = this.parts.fold(start) { acc, it ->
        when (acc) {
            null -> SimpleTypeModelStdLib.NothingType
            else -> it.typeOfNavigationPartFor(acc)
        }
    }
    return r
}

fun NavigationPart.typeOfNavigationPartFor(self: TypeInstance): TypeInstance? = when (this) {
    is PropertyCall -> this.typeOfPropertyCallFor(self)
    else -> error("subtype of NavigationPart not handled")
}

fun NavigationExpression.lastPropertyDeclarationFor(self: TypeInstance): PropertyDeclaration? {
    val st = this.start
    var pd: PropertyDeclaration? = when (st) {
        is LiteralExpression -> null
        is RootExpression -> when {
            st.isNothing -> null
            st.isSelf -> null
            else -> {
                self.resolvedProperty[PropertyName(st.name)]
            }
        }

        else -> error("type of Navigation.start not handled")
    }
    var acc = pd?.typeInstance
    val r = parts.forEach {
        pd = when (it) {
            is PropertyCall -> acc?.resolvedProperty?.get(it.propertyName)
            else -> null //everything must be property calls
        }
        acc = pd?.typeInstance
    }
    return pd
}

fun PropertyCall.typeOfPropertyCallFor(self: TypeInstance?): TypeInstance? =
    self?.resolvedProperty?.get(this.propertyName)?.typeInstance


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