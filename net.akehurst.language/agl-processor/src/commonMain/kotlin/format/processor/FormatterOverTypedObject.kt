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

import net.akehurst.kotlinx.collections.lazyMap
import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.expressions.processor.ObjectGraphAccessorMutatorByReflection
import net.akehurst.language.agl.processor.FormatResultDefault
import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.objectgraph.api.EvaluationContext
import net.akehurst.language.api.processor.FormatResult
import net.akehurst.language.api.processor.FormatString
import net.akehurst.language.api.processor.Formatter
import net.akehurst.language.api.syntaxAnalyser.LocationMap
import net.akehurst.language.base.api.Formatable
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.api.asPossiblyQualifiedName
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.api.FunctionCall
import net.akehurst.language.expressions.api.LiteralExpression
import net.akehurst.language.expressions.processor.ExpressionsInterpreterOverTypedObject
import net.akehurst.language.formatter.api.*
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.objectgraph.api.ObjectGraphAccessorMutator
import net.akehurst.language.objectgraph.api.TypedObject
import net.akehurst.language.types.api.*
import net.akehurst.language.types.asm.StdLibDefault

class FormatterOverTypedObject(
    override val formatDomain: AglFormatDomain,
    objectGraph: ObjectGraphAccessorMutator,
    issues: IssueHolder,
    val locationMap: LocationMap = LocationMapDefault()
) : ExpressionsInterpreterOverTypedObject(objectGraph, issues), Formatter {

    companion object {
        const val EVC_FORMAT_SET_NAME = "§EVC_FORMAT_SET_NAME"
        const val EOL_NAME = $$"$EOL"
        val prefixMatchPattern = Regex("\\s+")

        suspend fun <T> Iterable<T>.joinToStringSuspend(
            separator: CharSequence = ", ",
            prefix: CharSequence = "",
            postfix: CharSequence = "",
            limit: Int = -1,
            truncated: CharSequence = "...",
            transform: (suspend (T) -> CharSequence)? = null
        ): String {
            val buffer = StringBuilder()
            buffer.append(prefix)
            var count = 0
            for (element in this) {
                if (++count > 1) buffer.append(separator)
                if (limit < 0 || count <= limit) {
                    buffer.append(transform?.invoke(element) ?: "")
                } else break
            }
            if (limit >= 0 && count > limit) buffer.append(truncated)
            buffer.append(postfix)
            return buffer.toString()
        }
    }

    private val _formatSet: Map<PossiblyQualifiedName, FormatSet> = lazyMap { formatSetName: PossiblyQualifiedName ->
        formatDomain.findFirstFormatSetDefinitionByNameOrNull(formatSetName) ?: error("FormatSet named '${formatSetName.value}' cannot be found")
    }
    private val _rules: Map<PossiblyQualifiedName, Map<TypeInstance, AglFormatRule>> = lazyMap { formatSetName: PossiblyQualifiedName ->
        val fs = formatDomain.findFirstDefinitionByPossiblyQualifiedNameOrNull(formatSetName) ?: error("FormatSet named '${formatSetName.value}' cannot be found")
        when (fs) {
            is FormatSet -> {
                var map = fs.rules.associateBy { rl ->
                    super.evaluateTypeReference(rl.forTypeName)
                }
                map
            }

            else -> error("Definition named '${formatSetName.value}' is not a FormatSet")
        }
    }
    private val _output = mutableMapOf<String, String>()

    fun findRuleFor(formatSetName: PossiblyQualifiedName, type: TypeInstance): AglFormatRule? = _rules[formatSetName]?.entries?.firstOrNull { (ti, rl) ->
        type.conformsTo(ti)
    }?.value

    override fun formatSelf(formatSetName: PossiblyQualifiedName, self: Any): FormatResult {
        val typesSelf = objectGraph.toTypedObject(self, StdLibDefault.AnyType)
        val evc = EvaluationContext.ofSelf(typesSelf)
        return format(formatSetName, evc)
    }

    override fun format(formatSetName: PossiblyQualifiedName, evc: EvaluationContext): FormatResult {
        if (evc.namedValues.contains(EOL_NAME).not()) {
            evc.setNamedValue(EOL_NAME, objectGraph.createPrimitiveValue(StdLibDefault.String.qualifiedTypeName, "\n"))
        }
        val str = formatEvc(formatSetName, evc)
        return FormatResultDefault(_output + Pair(FormatResultDefault.DEFAULT, str), issues)
    }

    private fun issueError(item: Any?, message: String, data: Any? = null) {
        val location = item?.let{this.locationMap[item]}
        issues.error(location, message, data)
    }

    private fun formatEvc(formatSetName: PossiblyQualifiedName, evc: EvaluationContext): String {
        val self = evc.self
        val resultStr = when (self) {
            null -> ""
            else -> when {
                self.type.isCollection && self.type.typeArguments.isNotEmpty() -> {
                    val selfRaw = self.self
                    val (coll, elType) = when {
                        selfRaw is Iterable<*> -> Pair(selfRaw.toList(), self.type.typeArguments[0].type)
                        selfRaw is Map<*, *> -> Pair(selfRaw.toList(), StdLibDefault.Pair.type(self.type.typeArguments))
                        else -> Pair(null, null)
                    }
                    when {
                        null == coll -> error("type.isCollection but self is not Iterable!")
                        null == elType -> error("type.isCollection but self is not Iterable!")
                        else -> {
                            val formatRule = findRuleFor(formatSetName, elType)
                            coll.joinToString("") {
                                val typedElem = self.accessor.toTypedObject(it, elType)
                                val elemEvc = evc.childSelf(typedElem)
                                val resStr = when (formatRule) {
                                    null -> formatWhenNoRule(formatSetName, elemEvc)
                                    else -> formatExpression(formatSetName, elemEvc, formatRule.formatExpression)
                                }
                                setAdditionalOutputIfRequired(typedElem, formatSetName, resStr)
                            }
                        }
                    }
                }

                else -> {
                    val formatRule = findRuleFor(formatSetName, self.type)  //model?.rules?.get(self.type.typeName)
                    val resStr = when (formatRule) {
                        null -> formatWhenNoRule(formatSetName, evc)
                        else -> formatExpression(formatSetName, evc, formatRule.formatExpression)
                    }
                    setAdditionalOutputIfRequired(self, formatSetName, resStr)
                }
            }
        }

        return resultStr
    }

    private fun setAdditionalOutputIfRequired(self: TypedObject, formatSetName: PossiblyQualifiedName, resultStr: String): String {
        val formatSet = _formatSet[formatSetName] ?: error("Format named '${formatSetName.value}' cannot be found")
        val outputOptionAny: Any? = formatSet.options["output"]
        return when {
            null == outputOptionAny -> resultStr // nothing needed - use result string
            else -> when {
                outputOptionAny !is LiteralExpression -> {
                    issueError(null, "The option '#output' must be a literal String.")
                    ""
                }

                outputOptionAny.value !is String -> {
                    issueError(null, "The option '#output' must be a literal String'")
                    ""
                }

                else -> {
                    val outputOption = (outputOptionAny.value as String)
                    val applicableTypeName = outputOption.substringBefore("->").trim()
                    val applicableType = typesDomain.findFirstDefinitionByNameOrNull(SimpleName(applicableTypeName))
                    when {
                        null == applicableType -> {
                            issueError(null, "The applicable type '$applicableTypeName' of the option '#output' is not found.")
                            ""
                        }

                        !self.type.resolvedDefinition.conformsTo(applicableType) -> resultStr // do nothing type not applicable
                        else -> {
                            val formatExpr = """
                            namespace temp
                            format Temp {
                              $outputOption
                            }
                        """
                            val outputFmtDom = Agl.formatDomain(FormatString(formatExpr), typesDomain)
                            when {
                                outputFmtDom.allIssues.errors.isNotEmpty() || null == outputFmtDom.asm -> {
                                    issueError(null, "Cannot process the output directive '$outputOption'.")
                                    issues.addAll(outputFmtDom.allIssues)
                                }

                                else -> {
                                    val objectGraph = ObjectGraphAccessorMutatorByReflection(typesDomain, issues)
                                    val outputName = Agl.format(outputFmtDom.asm!!, objectGraph, self)
                                    when {
                                        outputName.issues.errors.isNotEmpty() || null == outputName.sentence -> {
                                            issueError(null, "Cannot process the output directive '$outputOption'.")
                                            issues.addAll(outputName.issues)
                                        }

                                        else -> when {
                                            _output.contains(outputName.sentence!!) -> {
                                                issueError(null, "Output named '${outputName.sentence!!}' is already defined!'.")
                                            }

                                            else -> _output[outputName.sentence!!] = resultStr
                                        }
                                    }
                                }

                            }
                            ""
                        }
                    }
                }
            }
        }
    }

    private fun formatWhenNoRule(formatSetName: PossiblyQualifiedName, evc: EvaluationContext): String {
        val self = evc.self
        return when (self) {
            null -> ""
            else -> when {
                self.self is Formatable -> (self.self as Formatable).asString()
                else -> formatWhenNoRuleBasedOnTypeInfo(formatSetName, evc, self)
            }
        }
    }

    private fun formatWhenNoRuleBasedOnTypeInfo(formatSetName: PossiblyQualifiedName, evc: EvaluationContext, self: TypedObject): String {
        val selfType = self.type.resolvedDefinition
        return when (selfType) {
            is SpecialType -> when {
                StdLibDefault.NothingType.resolvedDefinition == selfType -> ""
                StdLibDefault.AnyType.resolvedDefinition == selfType -> {
                    issues.warn(null, "No formating rule found for type '${self?.type?.typeName?.value}'.")
                    self.self.toString()
                }

                else -> error("SpecialType not handled '${self.type.resolvedDefinition::class.simpleName}'")
            }

            is PrimitiveType, is EnumType -> objectGraph.valueOf(self).toString()
            is SingletonType, is ValueType -> {
                issues.warn(null, "No formating rule found for type '${self?.type?.typeName?.value}'.")
                objectGraph.valueOf(self).toString()
            }

            is CollectionType -> when {
                selfType.isStdList || selfType.isStdSet -> {
                    val coll = objectGraph.valueOf(self) as Collection<Any>
                    coll.joinToString(separator = "") {
                        val tobj = objectGraph.toTypedObject(it, StdLibDefault.AnyType)
                        formatEvc(formatSetName, evc.childSelf(tobj))
                    }
                }

                selfType.isStdMap -> {
                    val coll = objectGraph.valueOf(self) as Map<Any, Any>
                    coll.values.joinToString(separator = "") { //TODO: what about the keys !
                        val tobj = objectGraph.toTypedObject(it, StdLibDefault.AnyType)
                        formatEvc(formatSetName, evc.childSelf(tobj))
                    }
                }

                else -> TODO()
            }

            is StructuredType -> {
                val containedProps = self.type.resolvedDefinition.allProperty
                    .filter { (pname, pdecl) -> pdecl.isComposite || pdecl.isPrimitive }
                when {
                    containedProps.isNotEmpty() -> containedProps.map { (pname, pdecl) ->
                        val propValue = self.getProperty(pname.value)
                        formatEvc(formatSetName, evc.childSelf(propValue))
                    }.joinToString(separator = "") { it }

                    else -> self.self.toString()
                }
            }

            else -> error("Subtype of TypeDeclaration not handled '${self.type.resolvedDefinition::class.simpleName}'")
        }
    }

    private fun formatExpression(formatSetName: PossiblyQualifiedName, evc: EvaluationContext, formatExpr: Expression): String = when (formatExpr) {
        is FormatEmbeddedExpression -> formatEmbeddedExpression(formatSetName, evc, formatExpr)
        is FormatExpressionTemplate -> formatFromTemplate(formatSetName, evc, formatExpr)
        //is FormatExpressionWhen -> formatFromWhen(formatSetName, evc, formatExpr)
        else -> evalExpressionAsString(formatSetName, evc, formatExpr)
    }

    private fun formatEmbeddedExpression(formatSetName: PossiblyQualifiedName, evc: EvaluationContext, formatExpr: FormatEmbeddedExpression): String {
        val res = formatEvaluateExpression(formatSetName, evc, formatExpr.expression)
        val value = res.self
        return when (value) {
            null -> {
                issueError(formatExpr, "Expression should result in a value, got 'null'")
                ""
            }

            is String -> value as String
            else -> {
                val via = formatExpr.via ?: formatSetName
                formatEvc(via, evc.childSelf(res))
            }
        }
    }

    private fun evalExpressionAsString(formatSetName: PossiblyQualifiedName, evc: EvaluationContext, expression: Expression): String {
        val res = formatEvaluateExpression(formatSetName, evc, expression)
        val value = res.self
        return when (value) {
            null -> {
                issueError(expression, "Expression should result in a value, got 'null'")
                ""
            }

            is String -> value as String
            else -> {
                issueError(expression, "Expression should result in a String, got '${value::class.simpleName}'")
                "ERROR"
            }
        }
    }

    private fun formatFromTemplate(formatSetName: PossiblyQualifiedName, evc: EvaluationContext, formatExpr: FormatExpressionTemplate): String {
        val sb = StringBuilder()
        when (formatExpr.content.size) {
            0 -> Unit
            1 -> {
                val text = formatTemplateContent(formatSetName, evc, formatExpr.content.last())
                sb.append(text)
            }

            else -> {
                // find the shortest prefix
                val textContent = formatExpr.content
                    .filter { it is TemplateElementText }
                    .associateWith { formatTemplateContent(formatSetName, evc, it) }
                val prefix = computePrefix(textContent.values)

                //handle first element
                val firstContent = formatTemplateContent(formatSetName, evc, formatExpr.content[0])
                //val prefix = computePrefix(firstContent)
                val firstWithoutLeadingEol = firstContent.substringAfter("\n", firstContent)
                val withPrefixRemoved1 = removePrefixFromAllLines(firstWithoutLeadingEol, prefix)
                sb.append(withPrefixRemoved1)

                // handle mid elements
                val contentMid = formatExpr.content.drop(1).dropLast(1) //TODO: if < 3 elements ?
                var indent = computeIndent(withPrefixRemoved1)
                var previousWasEmpty = false
                for (elem in contentMid) {
                    val res = formatTemplateContent(formatSetName, evc, elem)
                    if (res.isEmpty()) {
                        previousWasEmpty = true
                    } else {
                        val res2 = when (previousWasEmpty) {
                            true -> res.trimStart('\n')
                            else -> res
                        }
                        val withPrefixRemoved = if (elem is TemplateElementText) {
                            removePrefixFromAllLines(res2, prefix)
                        } else {
                            removePrefixAddIndent(res2, prefix, indent)
                        }
                        if (elem is TemplateElementText) {
                            indent = computeIndent(removePrefixFromAllLines(res, prefix))
                        }
                        sb.append(withPrefixRemoved)
                        previousWasEmpty = false
                    }
                }

                // handle last element
                if (formatExpr.content.last() is TemplateElementText) {
                    val lastContent = formatTemplateContent(formatSetName, evc, formatExpr.content.last())
                    val lc = removePrefixFromAllLines(lastContent, prefix)
                    val trimmedLast = if (lc.contains("\n") && lc.substringAfterLast("\n").isBlank()) {
                        lc.substringBeforeLast("\n")
                    } else {
                        lc
                    }
                    sb.append(trimmedLast)
                } else {
                    val lastContent = formatTemplateContent(formatSetName, evc, formatExpr.content.last())
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
                        it.startsWith(prefix) -> indent + it.removePrefix(prefix)
                        else -> indent + it
                    }
                }
                first + "\n" + adjusted.joinToString("\n")
            }

            else -> txt
        }
    }

    private fun removePrefixFromAllLines(txt: String, prefix: String): String {
        if (prefix.isEmpty()) return txt
        return txt.split("\n").joinToString("\n") { line ->
            if (line.startsWith(prefix)) line.removePrefix(prefix) else line
        }
    }

    private fun formatTemplateContent(formatSetName: PossiblyQualifiedName, evc: EvaluationContext, content: TemplateElement): String {
        return when (content) {
            is TemplateElementText -> formatTemplateElementText(formatSetName, evc, content)
            is TemplateElementExpressionProperty -> formatTemplateElementExpressionSimple(formatSetName, evc, content)
            is TemplateElementExpressionList -> formatTemplateElementExpressionList(formatSetName, evc, content)
            is TemplateElementExpressionEmbedded -> formatTemplateElementExpressionEmbedded(formatSetName, evc, content)
            else -> error("Internal error: subtype of TemplateElement not handled: '${content::class.simpleName}'")
        }
    }

    private fun formatTemplateElementText(formatSetName: PossiblyQualifiedName, evc: EvaluationContext, templateElement: TemplateElementText): String {
        return templateElement.text
    }

    private fun formatTemplateElementExpressionSimple(formatSetName: PossiblyQualifiedName, evc: EvaluationContext, templateElement: TemplateElementExpressionProperty): String {
        val value = getNamedValue(evc, templateElement.propertyName)
        return when {
            value.type.isPrimitive -> objectGraph.valueOf(value).toString()
            else -> formatEvc(formatSetName, evc.childSelf(value))
        }
    }

    private fun formatTemplateElementExpressionList(formatSetName: PossiblyQualifiedName, evc: EvaluationContext, templateElement: TemplateElementExpressionList): String {
        val self = evc.self
        return when (self) {
            null -> ""
            else -> {
                val frmtSepList = templateElement.formatSeparatedList
                val typedlist = formatEvaluateExpression(formatSetName, evc, frmtSepList.listExpression)
                val typedSep = formatEvaluateExpression(formatSetName, evc, frmtSepList.separator)
                val obj = objectGraph.valueOf(typedlist)
                when (obj) {
                    is Collection<*> -> {
                        val list = obj.toList()
                        val sep = objectGraph.valueOf(typedSep) as String
                        val via = frmtSepList.via ?: formatSetName
                        (list as List<Any>).joinToString(separator = sep) {
                            val typedElement = objectGraph.toTypedObject(it, typedlist.type.typeArguments[0].type)
                            formatEvc(via, evc.childSelf(typedElement))
                        }
                    }

                    is Map<*, *> -> {
                        val list = obj.entries.map { (key, value) -> Pair(key, value) }
                        val sep = objectGraph.valueOf(typedSep) as String
                        val via = frmtSepList.via ?: formatSetName
                        val entryType = StdLibDefault.Pair.type(typedlist.type.typeArguments)
                        (list as List<Pair<Any, Any>>).joinToString(separator = sep) {
                            val typedElement = objectGraph.toTypedObject(it, entryType)
                            formatEvc(via, evc.childSelf(typedElement))
                        }
                    }

                    else -> {
                        issueError(templateElement, "Expected a collection object but got a '${obj::class.simpleName}'")
                        $$"$ERROR"
                    }
                }

            }
        }
    }

    private fun formatTemplateElementExpressionEmbedded(formatSetName: PossiblyQualifiedName, evc: EvaluationContext, templateElement: TemplateElementExpressionEmbedded): String {
        val v = formatEvaluateExpression(formatSetName, evc, templateElement.expression)
        return objectGraph.valueOf(v) as String
    }

    private fun formatFromWhen(formatSetName: PossiblyQualifiedName, evc: EvaluationContext, formatExpr: FormatExpressionWhen): String {
        for (opt in formatExpr.options) {
            val condValue = formatEvaluateExpression(formatSetName, evc, opt.condition)
            when (condValue.type) {
                StdLibDefault.Boolean -> {
                    if (objectGraph.valueOf(condValue) as Boolean) {
                        val result = formatExpression(formatSetName, evc, opt.format)
                        return result // return after first condition found that is true
                    } else {
                        //condition not true
                    }
                }

                else -> error("Conditions/Options in a when expression must result in a Boolean value: '${opt.condition}'")
            }
        }
        return ""
    }

    private fun getNamedValue(evc: EvaluationContext, name: String): TypedObject {
        return if (evc.namedValues.containsKey(name)) {
            evc.namedValues[name]!!
        } else {
//            val name1 = when {
//                name.startsWith("\$") -> name.substring(1)
//                else -> name
//            }
            val self = evc.self
            when (self) {
                null -> objectGraph.createPrimitiveValue(StdLibDefault.String.qualifiedTypeName, "")
                else -> {
                    self.getProperty(name)
                }
            }
        }
    }

    fun formatEvaluateExpression(formatSetName: PossiblyQualifiedName, evc: EvaluationContext, expression: Expression): TypedObject {
        val typedFsm = objectGraph.toTypedObject(formatSetName.value, StdLibDefault.String)
        val newEvc = evc.child(mapOf(EVC_FORMAT_SET_NAME to typedFsm))
        return evaluateExpression(newEvc, expression)
    }

    // --- override Expression Interpreter ---

    // need to override ExpressionsInterpreter.evaluateExpression so that embedded FormatExpressions are interpreted correctly
    override fun evaluateExpression(evc: EvaluationContext, expression: Expression): TypedObject = when (expression) {
        is FormatExpression -> {
            val formatSetName = (evc.getOrInParent(EVC_FORMAT_SET_NAME)?.self as? String)?.asPossiblyQualifiedName ?: error("evaluating a FormatExpression requires a formatSetName!")
            val fmt = when (expression) {
                is FormatEmbeddedExpression -> formatEmbeddedExpression(formatSetName, evc, expression)
                is FormatExpressionTemplate -> formatFromTemplate(formatSetName, evc, expression)
                is FormatExpressionWhen -> formatFromWhen(formatSetName, evc, expression)
                else -> error("Subtype of FormatExpression not handled in evaluateExpression '${expression::class.simpleName}'")
            }
            objectGraph.toTypedObject(fmt, StdLibDefault.AnyType)
        }

        else -> super.evaluateExpression(evc, expression)
    }

    override fun evaluateFunctionCall(evc: EvaluationContext, expression: FunctionCall): TypedObject {
        val funcName = expression.possiblyQualifiedName.value
        val func = this.formatDomain.findFirstFunctionDefinitionByNameOrNull(SimpleName(funcName))
        val argValues = expression.arguments.map {
            evaluateExpression(evc, it)
        }
        return when (func) {
            null -> {
                objectGraph.callFunction(expression.possiblyQualifiedName.value, argValues) { tr -> evaluateTypeReference(tr) }
            }

            else -> when {
                null != func.execution -> {
                    val retType = func.returnTypeReference?.let { evaluateTypeReference(it) } ?: StdLibDefault.AnyType
                    val args = argValues.map { objectGraph.untyped(it) }
                    val res = func.execution!!.invoke(args)
                    objectGraph.toTypedObject(res, retType)
                }

                null != func.body -> {
                    val argMap = func.parameters.mapIndexed { idx, prm -> Pair(prm.name, argValues[idx]) }.associate { it }
                    val newEvc = evc.child(argMap)
                    evaluateExpression(newEvc, func.body!!)
                }

                else -> {
                    issueError(expression, "Function named '$funcName' could not be executed.")
                    objectGraph.nothing()
                }
            }
        }
    }


}