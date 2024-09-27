/*
 * Copyright (C) 2024 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.transform.processor

import net.akehurst.language.asm.simple.asValueName
import net.akehurst.language.expressions.processor.EvaluationContext
import net.akehurst.language.expressions.processor.ExpressionsInterpreterOverTypedObject
import net.akehurst.language.expressions.processor.asmValue
import net.akehurst.language.asm.api.AsmPath
import net.akehurst.language.asm.api.AsmStructure
import net.akehurst.language.asm.api.AsmValue
import net.akehurst.language.expressions.api.AssignmentStatement
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.transform.api.TransformationRule
import net.akehurst.language.typemodel.api.PropertyCharacteristic
import net.akehurst.language.typemodel.api.PropertyName
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.asm.SimpleTypeModelStdLib
import net.akehurst.language.typemodel.asm.typeModel

class AsmTransformInterpreter(
    val typeModel: TypeModel
) {

    companion object {
        const val SELF = "\$self"
        val PATH = PropertyName("\$path")
        val ALTERNATIVE = PropertyName("\$alternative")
        val LEAF = PropertyName("leaf")
        val CHILD = PropertyName("child")
        val CHILDREN = PropertyName("children")
        val LIST_OF_ANY = SimpleTypeModelStdLib.List.type(listOf(SimpleTypeModelStdLib.AnyType.nullable()))
        val SLIST_OF_ANY = SimpleTypeModelStdLib.ListSeparated.type(listOf(SimpleTypeModelStdLib.AnyType))
        val CMP_STR_MEM = setOf(PropertyCharacteristic.COMPOSITE, PropertyCharacteristic.READ_WRITE, PropertyCharacteristic.STORED)

        val parseNodeTypeModel = typeModel("ParseNodes", true) {
            namespace("parse") {
            }
        }
        val parseNodeNamespace = parseNodeTypeModel.findNamespaceOrNull(QualifiedName("parse"))!!
        val PARSE_NODE_TYPE_LIST_SIMPLE = parseNodeNamespace.createTupleType().also {
            it.appendPropertyStored(PATH, SimpleTypeModelStdLib.String, CMP_STR_MEM)
            it.appendPropertyStored(ALTERNATIVE, SimpleTypeModelStdLib.Integer, CMP_STR_MEM)
            it.appendPropertyStored(LEAF, SimpleTypeModelStdLib.String, CMP_STR_MEM)
            it.appendPropertyStored(CHILDREN, LIST_OF_ANY, CMP_STR_MEM)
            it.appendPropertyStored(CHILD, LIST_OF_ANY, CMP_STR_MEM)
        }
        val PARSE_NODE_TYPE_LIST_SEPARATED = parseNodeNamespace.createTupleType().also {
            it.appendPropertyStored(PATH, SimpleTypeModelStdLib.String, CMP_STR_MEM)
            it.appendPropertyStored(ALTERNATIVE, SimpleTypeModelStdLib.Integer, CMP_STR_MEM)
            it.appendPropertyStored(LEAF, SimpleTypeModelStdLib.String, CMP_STR_MEM)
            it.appendPropertyStored(CHILDREN, SLIST_OF_ANY, CMP_STR_MEM)
            it.appendPropertyStored(CHILD, SLIST_OF_ANY, CMP_STR_MEM)
        }
    }

    val exprInterpreter = ExpressionsInterpreterOverTypedObject(typeModel)
    val issues get() = exprInterpreter.issues// IssueHolder(LanguageProcessorPhase.INTERPRET)

    fun evaluate(evc: EvaluationContext, path: AsmPath, trRule: TransformationRule): AsmValue {
        val tObj = evaluateSelfStatement(evc, trRule.expression)
        val asm = tObj
//        when {
//            trRule.modifyStatements.isEmpty() -> Unit
//            else -> when (asm) {
//                is AsmStructure -> {
//                    for (st in trRule.modifyStatements) {
//                        executeStatementOn(self, st, asm)
//                    }
//                }
//
//                else -> {
//                    issues.error(null, "'self' value for transformation-rule is not a Structure, cannot set/modify properties")
//                }
//            }
//        }
        return asm
    }

    private fun evaluateSelfStatement(evc: EvaluationContext, expression: Expression): AsmValue {
        return exprInterpreter.evaluateExpression(evc, expression).asmValue
    }

    private fun executeStatementOn(evc: EvaluationContext, st: AssignmentStatement, asm: AsmStructure) {
        val propValue = evaluateExpressionOver(st.rhs, evc)
        asm.setProperty(st.lhsPropertyName.asValueName, propValue, asm.property.size)
    }

    private fun evaluateExpressionOver(expr: Expression, evc: EvaluationContext): AsmValue {
        val res = exprInterpreter.evaluateExpression(evc, expr)
        return res.asmValue
    }

}