/**
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.formatter.api

import net.akehurst.language.api.language.expressions.Expression

interface AglFormatterModel {

    val defaultWhiteSpace: String

    /**
     * all format rules indexed by the type name that the rule applies to
     */
    val rules: Map<String, AglFormatterRule>

}

interface AglFormatterRule {
    val model: AglFormatterModel
    val forTypeName: String
    val formatExpression: FormatExpression
}

interface FormatExpression {

}

interface FormatExpressionLiteral : FormatExpression {
    val value: String
}

interface FormatExpressionTemplate : FormatExpression {
    val content: List<TemplateElement>
}

interface FormatExpressionWhen : FormatExpression {
    val options: List<FormatWhenOption>
}

interface FormatWhenOption {
    val condition: Expression
    val format: FormatExpression
}

interface TemplateElement

interface TemplateElementText : TemplateElement {
    val text: String
}

interface TemplateElementExpressionSimple : TemplateElement {
    val identifier: String
}

interface TemplateElementExpressionEmbedded : TemplateElement {
    val expression: FormatExpression
}