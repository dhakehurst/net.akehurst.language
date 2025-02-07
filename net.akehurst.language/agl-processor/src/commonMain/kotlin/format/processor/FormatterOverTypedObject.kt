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

import net.akehurst.language.agl.processor.FormatResultDefault
import net.akehurst.language.api.processor.FormatResult
import net.akehurst.language.api.processor.Formatter
import net.akehurst.language.asm.api.*
import net.akehurst.language.expressions.processor.*
import net.akehurst.language.formatter.api.*
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.typemodel.api.*
import net.akehurst.language.typemodel.asm.StdLibDefault

class FormatterOverTypedObject<SelfType>(
    val formatSet: FormatSet,
    val objectGraph: ObjectGraph<SelfType>
) : Formatter<TypedObject<SelfType>> {

    private val _issues = IssueHolder(LanguageProcessorPhase.FORMAT)

    val typeModel: TypeModel = objectGraph.typeModel

    val expressionInterpreter by lazy {
        ExpressionsInterpreterOverTypedObject(objectGraph, _issues)
    }

    private val _rules by lazy {
        formatSet.rules.values.associateBy { rl ->
            expressionInterpreter.evaluateTypeReference(rl.forTypeName)
        }
    }

    fun findRuleFor(type: TypeInstance): AglFormatRule? = _rules[type]

    override fun format(asm: TypedObject<SelfType>): FormatResult {
        val evc = EvaluationContext.ofSelf(asm)
        val str = formatSelf(evc)
        return FormatResultDefault(str, _issues)
    }

    private fun formatSelf(evc: EvaluationContext<SelfType>): String {
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
            else -> {
                val selfType = self.type.resolvedDeclaration
                when (selfType) {
                    is SpecialType -> when {
                        StdLibDefault.NothingType.resolvedDeclaration == selfType -> ""
                        StdLibDefault.AnyType.resolvedDeclaration == selfType -> self.toString()
                        else ->error("SpecialType not handled '${self.type.resolvedDeclaration::class.simpleName}'")
                    }
                    is SingletonType -> objectGraph.valueOf(self).toString()
                    is PrimitiveType -> objectGraph.valueOf(self).toString()
                    is ValueType -> objectGraph.valueOf(self).toString()
                    is EnumType -> objectGraph.valueOf(self).toString()
                    is CollectionType -> when {
                        selfType.isStdList -> {
                            val coll = objectGraph.valueOf(self) as List<SelfType>
                            coll.joinToString(separator = "") {
                                val tobj = objectGraph.toTypedObject(it)
                                formatSelf(EvaluationContext.ofSelf(tobj))
                            }
                        }
                        selfType.isStdSet -> TODO()
                        selfType.isStdMap -> {
                            val coll = objectGraph.valueOf(self) as Map<Any,SelfType>
                            coll.values.joinToString(separator = "") {
                                val tobj = objectGraph.toTypedObject(it)
                                formatSelf(EvaluationContext.ofSelf(tobj))
                            }
                        }
                        else -> TODO()
                    }

                    is StructuredType -> {
                        val containedProps = self.type.resolvedDeclaration.allProperty
                            .filter { (pname, pdecl) -> pdecl.isComposite || pdecl.isPrimitive }
                        containedProps.map { (pname, pdecl) ->
                            val propValue = objectGraph.getProperty(self, pname.value)
                            formatSelf(EvaluationContext.ofSelf(propValue))
                        }.joinToString(separator = "") { it }
                    }

                    else -> error("Subtype of TypeDeclaration not handled '${self.type.resolvedDeclaration::class.simpleName}'")
                }
            }
        }
    }

    private fun formatExpression(evc: EvaluationContext<SelfType>, formatExpr: FormatExpression) = when (formatExpr) {
        is FormatExpressionExpression -> formatFromExpression(evc, formatExpr)
        is FormatExpressionTemplate -> formatFromTemplate(evc, formatExpr)
        is FormatExpressionWhen -> formatFromWhen(evc, formatExpr)
        else -> error("Internal error: subtype of AglFormatExpression not handled: '${formatExpr::class.simpleName}'")
    }

    private fun formatFromExpression(evc: EvaluationContext<SelfType>, formatExpr: FormatExpressionExpression): String {
        val res = expressionInterpreter.evaluateExpression(evc, formatExpr.expression)
        return (res.self as AsmPrimitive).value as String
    }

    private fun formatFromTemplate(evc: EvaluationContext<SelfType>, formatExpr: FormatExpressionTemplate): String {
        return formatExpr.content.joinToString(separator = "") {
            when (it) {
                is TemplateElementText -> formatTemplateElementText(evc, it)
                is TemplateElementExpressionSimple -> TODO()
                is TemplateElementExpressionEmbedded -> TODO()
                else -> error("Internal error: subtype of TemplateElement not handled: '${it::class.simpleName}'")
            }
        }
    }

    private fun formatTemplateElementText(evc: EvaluationContext<SelfType>, templateElement: TemplateElementText): String {
        return templateElement.text
    }

    private fun formatTemplateElementExpressionSimple(): String {
        TODO()
        //val id1 = (it as AsmStructure).getProperty(PropertyValueName("dollar_identifier"))
        //val id = (id1 as AsmPrimitive).value.toString().substringAfter("\$")
       // val pv = el.getProperty(PropertyValueName(id))
       // pv.format(model)
    }

    private fun formatTemplateElementExpressionEmbedded(): String {
        TODO()
    }

    private fun formatFromWhen(evc: EvaluationContext<SelfType>, formatExpr: FormatExpressionWhen): String {
        TODO()
    }

}