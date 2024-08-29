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

package net.akehurst.language.agl.language.asmTransform

import net.akehurst.language.agl.language.expressions.EvaluationContext
import net.akehurst.language.agl.language.expressions.ExpressionsInterpreterOverTypedObject
import net.akehurst.language.agl.language.expressions.asm
import net.akehurst.language.api.asm.AsmPath
import net.akehurst.language.api.asm.AsmStructure
import net.akehurst.language.api.asm.AsmValue
import net.akehurst.language.api.language.asmTransform.TransformationRule
import net.akehurst.language.api.language.base.QualifiedName
import net.akehurst.language.api.language.expressions.AssignmentStatement
import net.akehurst.language.api.language.expressions.Expression
import net.akehurst.language.typemodel.api.PropertyCharacteristic
import net.akehurst.language.typemodel.api.PropertyName
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.api.typeModel
import net.akehurst.language.typemodel.simple.SimpleTypeModelStdLib

class AsmTransformInterpreter(
    val typeModel: TypeModel
) {

    companion object {
        const val SELF = "\$self"
        val ALTERNATIVE = PropertyName("\$alternative")
        val LEAF = PropertyName("leaf")
        val CHILD = PropertyName("child")
        val CHILDREN = PropertyName("children")
        val LIST_OF_ANY = SimpleTypeModelStdLib.List.type(listOf(SimpleTypeModelStdLib.AnyType))
        val SLIST_OF_ANY = SimpleTypeModelStdLib.ListSeparated.type(listOf(SimpleTypeModelStdLib.AnyType))
        val COMPOSITE_MEMBER = setOf(PropertyCharacteristic.COMPOSITE, PropertyCharacteristic.MEMBER)

        val parseNodeTypeModel = typeModel("ParseNodes", true) {
            namespace("parse") {
            }
        }
        val parseNodeNamespace = parseNodeTypeModel.findNamespaceOrNull(QualifiedName("parse"))!!
        val PARSE_NODE_TYPE_LIST_SIMPLE = parseNodeNamespace.createTupleType().also {
            it.appendPropertyStored(ALTERNATIVE, SimpleTypeModelStdLib.Integer, setOf(PropertyCharacteristic.COMPOSITE, PropertyCharacteristic.MEMBER))
            it.appendPropertyStored(LEAF, SimpleTypeModelStdLib.String, COMPOSITE_MEMBER)
            it.appendPropertyStored(CHILDREN, LIST_OF_ANY, COMPOSITE_MEMBER)
            it.appendPropertyStored(CHILD, LIST_OF_ANY, COMPOSITE_MEMBER)
        }
        val PARSE_NODE_TYPE_LIST_SEPARATED = parseNodeNamespace.createTupleType().also {
            it.appendPropertyStored(ALTERNATIVE, SimpleTypeModelStdLib.Integer, COMPOSITE_MEMBER)
            it.appendPropertyStored(LEAF, SimpleTypeModelStdLib.String, COMPOSITE_MEMBER)
            it.appendPropertyStored(CHILDREN, SLIST_OF_ANY, COMPOSITE_MEMBER)
            it.appendPropertyStored(CHILD, SLIST_OF_ANY, COMPOSITE_MEMBER)
        }
    }

    val exprInterpreter = ExpressionsInterpreterOverTypedObject(typeModel)
    val issues get() = exprInterpreter.issues// IssueHolder(LanguageProcessorPhase.INTERPRET)

    fun evaluate(evc: EvaluationContext, path: AsmPath, trRule: TransformationRule): AsmValue {
        val tObj = evaluateSelfStatement(evc, path, trRule.expression)
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

    private fun evaluateSelfStatement(evc: EvaluationContext, path: AsmPath, expression: Expression): AsmValue {
        return exprInterpreter.evaluateExpression(evc, expression).asm
    }

    private fun executeStatementOn(evc: EvaluationContext, st: AssignmentStatement, asm: AsmStructure) {
        val propValue = evaluateExpressionOver(st.rhs, evc)
        asm.setProperty(st.lhsPropertyName, propValue, asm.property.size)
    }

    private fun evaluateExpressionOver(expr: Expression, evc: EvaluationContext): AsmValue {
        val res = exprInterpreter.evaluateExpression(evc, expr)
        return res.asm
    }

}