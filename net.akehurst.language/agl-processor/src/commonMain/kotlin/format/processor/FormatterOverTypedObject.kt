/**
 * Copyright (C) 2025 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.format.processor

import net.akehurst.language.objectgraph.api.*
import net.akehurst.language.agl.processor.FormatResultDefault
import net.akehurst.language.api.processor.EvaluationContext
import net.akehurst.language.api.processor.FormatResult
import net.akehurst.language.api.processor.Formatter
import net.akehurst.language.base.api.Formatable
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.processor.ExpressionsInterpreterOverTypedObject
import net.akehurst.language.formatter.api.*
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.types.api.*
import net.akehurst.language.types.asm.StdLibDefault

class FormatterOverTypedObject<SelfType : Any>(
    override val formatDomain: AglFormatDomain,
    objectGraph: ObjectGraphAccessorMutator<SelfType>,
    issues: IssueHolder
) : ExpressionsInterpreterOverTypedObject<SelfType>(objectGraph, issues), Formatter<SelfType> {

    companion object {
        const val EOL_NAME = $$"$EOL"
        val prefixMatchPattern = Regex("\\s+")
    }

    private lateinit var _formatSet: FormatSet
    private val _rules by lazy {
        _formatSet.rules.associateBy { rl ->
            super.evaluateTypeReference(rl.forTypeName)
        }
    }

    fun findRuleFor(type: TypeInstance): AglFormatRule? = _rules.entries.firstOrNull { (ti, rl) ->
        type.conformsTo(ti)
    }?.value

    override fun formatSelf(formatSetName: PossiblyQualifiedName, self: SelfType): FormatResult {
        _formatSet = formatDomain.findFirstDefinitionByPossiblyQualifiedNameOrNull(formatSetName) ?: error("FormatSet named '${formatSetName.value}' cannot be found")
        val typesSelf = objectGraph.toTypedObject(self)
        val evc = EvaluationContext.ofSelf(typesSelf)
        return format(formatSetName, evc)
    }

    override fun format(formatSetName: PossiblyQualifiedName, evc: EvaluationContext<SelfType>): FormatResult {
        _formatSet = formatDomain.findFirstDefinitionByPossiblyQualifiedNameOrNull(formatSetName) ?: error("FormatSet named '${formatSetName.value}' cannot be found")
        val evc2 = if (evc.namedValues.contains(EOL_NAME).not()) {
            evc.copy(namedValues = evc.namedValues + Pair(EOL_NAME, objectGraph.createPrimitiveValue(StdLibDefault.String.qualifiedTypeName, "\n")))
        } else {
            evc
        }
        val str = formatEvc(evc2)
        return FormatResultDefault(str, issues)
    }

    private fun formatEvc(evc: EvaluationContext<SelfType>): String {
        val self = evc.self
        return when (self) {
            null -> ""
            else -> {
                val formatRule = findRuleFor(self.type)  //model?.rules?.get(self.type.typeName)
                when (formatRule) {
                    null -> formatWhenNoRule(evc)
                    else -> formatExpression(evc, formatRule.formatExpression)
                }
            }
        }
    }

    private fun formatWhenNoRule(evc: EvaluationContext<SelfType>): String {
        val self = evc.self
        return when (self) {
            null -> ""
            else -> when {
                self.self is Formatable -> (self.self as Formatable).asString()
                else -> formatWhenNoRuleBasedOnTypeInfo(evc, self)
            }
        }
    }

    private fun formatWhenNoRuleBasedOnTypeInfo(evc: EvaluationContext<SelfType>, self: TypedObject<SelfType>): String {
        val selfType = self.type.resolvedDeclaration
        return when (selfType) {
            is SpecialType -> when {
                StdLibDefault.NothingType.resolvedDeclaration == selfType -> ""
                StdLibDefault.AnyType.resolvedDeclaration == selfType -> self.self.toString()
                else -> error("SpecialType not handled '${self.type.resolvedDeclaration::class.simpleName}'")
            }

            is SingletonType -> objectGraph.valueOf(self).toString()
            is PrimitiveType -> {
                objectGraph.valueOf(self).toString()
            }

            is ValueType -> objectGraph.valueOf(self).toString()
            is EnumType -> objectGraph.valueOf(self).toString()
            is CollectionType -> when {
                selfType.isStdList -> {
                    val coll = objectGraph.valueOf(self) as List<SelfType>
                    coll.joinToString(separator = "") {
                        val tobj = objectGraph.toTypedObject(it)
                        formatEvc(evc.childSelf(tobj))
                    }
                }

                selfType.isStdSet -> TODO()
                selfType.isStdMap -> {
                    val coll = objectGraph.valueOf(self) as Map<Any, SelfType>
                    coll.values.joinToString(separator = "") {
                        val tobj = objectGraph.toTypedObject(it)
                        formatEvc(evc.childSelf(tobj))
                    }
                }

                else -> TODO()
            }

            is StructuredType -> {
                val containedProps = self.type.resolvedDeclaration.allProperty
                    .filter { (pname, pdecl) -> pdecl.isComposite || pdecl.isPrimitive }
                containedProps.map { (pname, pdecl) ->
                    val propValue = objectGraph.getProperty(self, pname.value)
                    formatEvc(evc.childSelf(propValue))
                }.joinToString(separator = "") { it }
            }

            else -> error("Subtype of TypeDeclaration not handled '${self.type.resolvedDeclaration::class.simpleName}'")
        }
    }

    private fun formatExpression(evc: EvaluationContext<SelfType>, formatExpr: FormatExpression) = when (formatExpr) {
        is FormatExpressionExpression -> formatFromExpression(evc, formatExpr)
        is FormatExpressionTemplate -> formatFromTemplate(evc, formatExpr)
        is FormatExpressionWhen -> formatFromWhen(evc, formatExpr)
        else -> error("Internal error: subtype of AglFormatExpression not handled: '${formatExpr::class.simpleName}'")
    }

    private fun formatFromExpression(evc: EvaluationContext<SelfType>, formatExpr: FormatExpressionExpression): String {
        val res = super.evaluateExpression(evc, formatExpr.expression)
        val value = res.self
        return when (value) {
            null -> {
                issues.error(null, "Expression should result in a value, got 'null'")
                ""
            }

            is String -> value as String
            else -> {
                //issues.error(null, "Expression should result in a String value, got '${value::class.simpleName}'")
                formatEvc(evc.childSelf(res))
            }
        }
    }

    private fun formatFromTemplate(evc: EvaluationContext<SelfType>, formatExpr: FormatExpressionTemplate): String {
        val sb = StringBuilder()
        when (formatExpr.content.size) {
            0 -> Unit
            1 -> {
                val text = formatTemplateContent(evc, formatExpr.content.last())
                sb.append(text)
            }

            else -> {
                // find the shortest prefix
                val textContent = formatExpr.content
                    .filter { it is TemplateElementText }
                    .associateWith { formatTemplateContent(evc, it) }
                val prefix = computePrefix(textContent.values)

                //handle first element
                val firstContent = formatTemplateContent(evc, formatExpr.content[0])
                //val prefix = computePrefix(firstContent)
                val withPrefixRemoved1 = firstContent.substringAfter("\n").drop(prefix.length)
                sb.append(withPrefixRemoved1)

                // handle mid elements
                var contentMid = formatExpr.content.drop(1).dropLast(1) //TODO: if < 3 elements ?
                var indent = ""
                var previousWasEmpty = false
                for (elem in contentMid) {
                    val res = formatTemplateContent(evc, elem)
                    if (res.isEmpty()) {
                        previousWasEmpty = true
                    } else {
                        val res2 = when (previousWasEmpty) {
                            true -> res.trimStart('\n').trimStart()
                            else -> res
                        }
                        val withPrefixRemoved = removePrefixAddIndent(res2, prefix, indent)
                        if (elem is TemplateElementText) {
                            indent = computeIndent(removePrefixAddIndent(res, prefix, ""))
                        }
                        sb.append(withPrefixRemoved)
                        previousWasEmpty = false
                    }
                }

                // handle last element
                if (formatExpr.content.last() is TemplateElementText) {
                    val lastContent = formatTemplateContent(evc, formatExpr.content.last())
                    val lc = removePrefixAddIndent(lastContent, prefix, indent)
                    val txt = lc.substringAfter("\n")
                    if (txt.isBlank()) {
                        sb.append(lc.substringBefore("\n"))
                    } else {
                        sb.append(lc)
                    }
                } else {
                    val lastContent = formatTemplateContent(evc, formatExpr.content.last())
                    val lc = removePrefixAddIndent(lastContent, prefix, indent)
                    sb.append(lc)
                }
            }
        }

        return sb.toString()
    }

    private fun computePrefix(texts: Collection<String>): String {
        val lines = texts.flatMap {
            it.splitToSequence("\n") // get all text
                .drop(1)             // that is after an EOL
        }
            .dropLast(1) // don't consider very last line, which is closing quote
        val prefixes = lines
            .filter { it.isNotEmpty() }
            .map { prefixMatchPattern.matchAt(it, 0)?.value ?: "" }
        return prefixes.minByOrNull { it.length } ?: ""
    }

    private fun computePrefix(txt: String): String {
        return when {
            txt.startsWith("\n") -> {
                val txt = txt.substringAfter("\n")
                val len = txt.length - txt.trimStart().length
                txt.substring(0, len)
            }

            else -> ""
        }
    }

    private fun computeIndent(txt: String): String {
        return when {
            txt.contains("\n") -> {
                val txt = txt.substringAfterLast("\n")
                val len = txt.length - txt.trimStart().length
                txt.substring(0, len)
            }

            else -> ""
        }
    }

    private fun removePrefixAddIndent(txt: String, prefix: String, indent: String): String {
        return when {
            txt.contains("\n") -> {
                val lines = txt.split("\n")
                val first = lines[0]
                val adjusted = lines.drop(1).map {
                    when {
                        it.startsWith(prefix) -> indent + it.substringAfter(prefix)
                        else -> indent + it
                    }
                }
                "${indent}$first" + "\n" + adjusted.joinToString("\n")
            }

            else -> txt
        }
    }

    private fun formatTemplateContent(evc: EvaluationContext<SelfType>, content: TemplateElement): String {
        return when (content) {
            is TemplateElementText -> formatTemplateElementText(evc, content)
            is TemplateElementExpressionProperty -> formatTemplateElementExpressionSimple(evc, content)
            is TemplateElementExpressionList -> formatTemplateElementExpressionList(evc, content)
            is TemplateElementExpressionEmbedded -> formatTemplateElementExpressionEmbedded(evc, content)
            else -> error("Internal error: subtype of TemplateElement not handled: '${content::class.simpleName}'")
        }
    }

    private fun formatTemplateElementText(evc: EvaluationContext<SelfType>, templateElement: TemplateElementText): String {
        return templateElement.text
    }

    private fun formatTemplateElementExpressionSimple(evc: EvaluationContext<SelfType>, templateElement: TemplateElementExpressionProperty): String {
        val value = getNamedValue(evc, templateElement.propertyName)
        return formatEvc(evc.childSelf(value))
    }

    private fun formatTemplateElementExpressionList(evc: EvaluationContext<SelfType>, templateElement: TemplateElementExpressionList): String {
        val self = evc.self
        return when (self) {
            null -> ""
            else -> {
                //val typedlist = expressionInterpreter.evaluateExpression(evc, templateElement.listPropertyName)
                val typedlist = super.evaluateExpression(evc, templateElement.listExpression)
                val typedSep = evaluateExpression(evc, templateElement.separator)
                val list = objectGraph.valueOf(typedlist)
                when (list) {
                    is List<*> -> {
                        val sep = objectGraph.valueOf(typedSep) as String
                        (list as List<Any>).joinToString(separator = sep) {
                            val typedElement = objectGraph.toTypedObject(it as SelfType)
                            formatEvc(evc.childSelf(typedElement))
                        }
                    }

                    else -> {
                        issues.error(null, "Expected a list object but got a '${list::class.simpleName}'")
                        ""
                    }
                }

            }
        }
    }

    private fun formatTemplateElementExpressionEmbedded(evc: EvaluationContext<SelfType>, templateElement: TemplateElementExpressionEmbedded): String {
        val v = evaluateExpression(evc, templateElement.expression)
        return objectGraph.valueOf(v) as String
    }

    private fun formatFromWhen(evc: EvaluationContext<SelfType>, formatExpr: FormatExpressionWhen): String {
        for (opt in formatExpr.options) {
            val condValue = super.evaluateExpression(evc, opt.condition)
            when (condValue.type) {
                StdLibDefault.Boolean -> {
                    if (objectGraph.valueOf(condValue) as Boolean) {
                        val result = formatExpression(evc, opt.format)
                        return result // return after first condition found that is true
                    } else {
                        //condition not true
                    }
                }

                else -> error("Conditions/Options in a when expression must result in a Boolean value")
            }
        }
        return ""
    }

    private fun getNamedValue(evc: EvaluationContext<SelfType>, name: String): TypedObject<SelfType> {
        return if (evc.namedValues.containsKey(name)) {
            evc.namedValues[name]!!
        } else {
            val name1 = when {
                name.startsWith("\$") -> name.substring(1)
                else -> name
            }
            val self = evc.self
            when (self) {
                null -> objectGraph.createPrimitiveValue(StdLibDefault.String.qualifiedTypeName, "")
                else -> {
                    objectGraph.getProperty(self, name1) //drop the dollar
                }
            }
        }
    }

    override fun evaluateExpression(evc: EvaluationContext<SelfType>, expression: Expression): TypedObject<SelfType> = when (expression) {
        is FormatExpression -> {
            val fmt = when (expression) {
                is FormatExpressionExpression -> formatFromExpression(evc, expression)
                is FormatExpressionTemplate -> formatFromTemplate(evc, expression)
                is FormatExpressionWhen -> formatFromWhen(evc, expression)
                else -> error("Subtype of FormatExpression not handled in evaluateExpression '${expression::class.simpleName}'")
            }
            objectGraph.toTypedObject(fmt as SelfType)
        }

        else -> super.evaluateExpression(evc, expression)
    }
}