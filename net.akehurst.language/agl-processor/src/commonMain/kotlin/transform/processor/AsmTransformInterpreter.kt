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

import net.akehurst.language.asm.api.AsmPath
import net.akehurst.language.asm.api.AsmStructure
import net.akehurst.language.asm.api.AsmValue
import net.akehurst.language.asm.api.PropertyValueName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.expressions.api.AssignmentStatement
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.processor.EvaluationContext
import net.akehurst.language.expressions.processor.ExpressionsInterpreterOverTypedObject
import net.akehurst.language.expressions.processor.ObjectGraphAsmSimple
import net.akehurst.language.expressions.processor.asmValue
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.transform.api.TransformationRule
import net.akehurst.language.typemodel.api.PropertyCharacteristic
import net.akehurst.language.typemodel.api.PropertyName
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.asm.StdLibDefault
import net.akehurst.language.typemodel.builder.typeModel

class AsmTransformInterpreter(
    val typeModel: TypeModel
) {

    companion object {
        const val SELF = "\$self"
        val PATH = PropertyName("\$path")
        val ALTERNATIVE = PropertyName("\$alternative")
        val CHILD = PropertyName("child")
        val CHILDREN = PropertyName("children")
        val MATCHED_TEXT = PropertyName("\$matchedText")
        val LIST_OF_ANY = StdLibDefault.List.type(listOf(StdLibDefault.AnyType.nullable().asTypeArgument))
        val SLIST_OF_ANY = StdLibDefault.ListSeparated.type(listOf(StdLibDefault.AnyType.asTypeArgument))
        val CMP_STR_MEM = setOf(PropertyCharacteristic.COMPOSITE, PropertyCharacteristic.READ_WRITE, PropertyCharacteristic.STORED)

        val parseNodeTypeModel = typeModel("ParseNodes", true) {
            namespace("parse") {
                dataType("Node") {
                    subtypes("BranchSeparated","Branch", "Leaf")
                    property(PATH.value, StdLibDefault.AnyType, 0)
                    property(ALTERNATIVE.value, StdLibDefault.Integer, 1)
                    property(MATCHED_TEXT.value, StdLibDefault.String, 2)
                }
                dataType("Leaf") {
                    subtypes("Node")
                    property("value", StdLibDefault.String, 0)
                }
                dataType("Branch") {
                    supertypes("Node")
                    propertyListTypeOf(CHILDREN.value, StdLibDefault.AnyType.qualifiedTypeName.value, false, 0) //TODO: typeArgument ?
                    propertyListTypeOf(CHILD.value, StdLibDefault.AnyType.qualifiedTypeName.value, false, 1)
                }
                dataType("BranchSeparated") {
                    supertypes("Node")
                    propertyListSeparatedType(CHILDREN.value, false, 0) {
                        ref(StdLibDefault.AnyType.qualifiedTypeName.value)
                        ref(StdLibDefault.AnyType.qualifiedTypeName.value)
                    }
                    propertyListSeparatedType(CHILD.value, false, 1) {
                        ref(StdLibDefault.AnyType.qualifiedTypeName.value)
                        ref(StdLibDefault.AnyType.qualifiedTypeName.value)
                    }
                }
            }
        }
        val parseNodeNamespace = parseNodeTypeModel.findNamespaceOrNull(QualifiedName("parse"))!!

        val PARSE_NODE_TYPE_LEAF = parseNodeNamespace.findOwnedTypeNamed(SimpleName("Leaf"))!!

        // TODO: create properer parse Node, Leaf, Branch, etc types
        val PARSE_NODE_TYPE_BRANCH_SIMPLE = parseNodeNamespace.findOwnedTypeNamed(SimpleName("Branch"))!!.type()
        /*= parseNodeNamespace.createTupleType().let {
            val args = mutableListOf(
                TypeArgumentNamedSimple(PATH, SimpleTypeModelStdLib.String),
                TypeArgumentNamedSimple(ALTERNATIVE, SimpleTypeModelStdLib.Integer),
                TypeArgumentNamedSimple(LEAF, SimpleTypeModelStdLib.String),
                TypeArgumentNamedSimple(CHILDREN, LIST_OF_ANY),
                TypeArgumentNamedSimple(CHILD, LIST_OF_ANY),
            )
            it.typeTuple(args)
        }*/
        /*.also {
            it.appendPropertyStored(PATH, SimpleTypeModelStdLib.String, CMP_STR_MEM)
            it.appendPropertyStored(ALTERNATIVE, SimpleTypeModelStdLib.Integer, CMP_STR_MEM)
            it.appendPropertyStored(LEAF, SimpleTypeModelStdLib.String, CMP_STR_MEM)
            it.appendPropertyStored(CHILDREN, LIST_OF_ANY, CMP_STR_MEM)
            it.appendPropertyStored(CHILD, LIST_OF_ANY, CMP_STR_MEM)
        }*/

        val PARSE_NODE_TYPE_BRANCH_SEPARATED = parseNodeNamespace.findOwnedTypeNamed(SimpleName("BranchSeparated"))!!.type()
        /*parseNodeNamespace.createTupleType().let {
        val args = mutableListOf(
            TypeArgumentNamedSimple(PATH, SimpleTypeModelStdLib.String),
            TypeArgumentNamedSimple(ALTERNATIVE, SimpleTypeModelStdLib.Integer),
            TypeArgumentNamedSimple(LEAF, SimpleTypeModelStdLib.String),
            TypeArgumentNamedSimple(CHILDREN, SLIST_OF_ANY),
            TypeArgumentNamedSimple(CHILD, SLIST_OF_ANY),
        )
        it.typeTuple(args)
    }*/
        /*.also {
            it.appendPropertyStored(PATH, SimpleTypeModelStdLib.String, CMP_STR_MEM)
            it.appendPropertyStored(ALTERNATIVE, SimpleTypeModelStdLib.Integer, CMP_STR_MEM)
            it.appendPropertyStored(LEAF, SimpleTypeModelStdLib.String, CMP_STR_MEM)
            it.appendPropertyStored(CHILDREN, SLIST_OF_ANY, CMP_STR_MEM)
            it.appendPropertyStored(CHILD, SLIST_OF_ANY, CMP_STR_MEM)
        }*/
    }

    val issues get() = IssueHolder(LanguageProcessorPhase.INTERPRET)
    val exprInterpreter = ExpressionsInterpreterOverTypedObject(ObjectGraphAsmSimple(typeModel, issues),issues)

    fun clear() {
        this.issues.clear()
    }

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
        asm.setProperty(PropertyValueName(st.lhsPropertyName), propValue, asm.property.size)
    }

    private fun evaluateExpressionOver(expr: Expression, evc: EvaluationContext): AsmValue {
        val res = exprInterpreter.evaluateExpression(evc, expr)
        return res.asmValue
    }

}