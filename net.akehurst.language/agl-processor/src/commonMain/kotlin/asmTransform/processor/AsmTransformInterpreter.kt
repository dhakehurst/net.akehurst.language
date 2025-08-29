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

package net.akehurst.language.asmTransform.processor

import net.akehurst.language.api.processor.EvaluationContext
import net.akehurst.language.asm.api.AsmStructure
import net.akehurst.language.asmTransform.api.AsmTransformationRule
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.expressions.api.AssignmentStatement
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.processor.ExpressionsInterpreterOverTypedObject
import net.akehurst.language.expressions.processor.ObjectGraph
import net.akehurst.language.expressions.processor.TypedObject
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.types.api.PropertyCharacteristic
import net.akehurst.language.types.api.PropertyName
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.builder.typesDomain

class AsmTransformInterpreter<AsmValueType:Any>(
    val typesDomain: TypesDomain,
    val objectGraph: ObjectGraph<AsmValueType>,
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

        val parseNodeTypesDomain = typesDomain("ParseNodes", true) {
            namespace("parse") {
                data("Node") {
                    subtypes("BranchSeparated","Branch", "Leaf")
                    property(PATH.value, StdLibDefault.AnyType, 0)
                    property(ALTERNATIVE.value, StdLibDefault.Integer, 1)
                    property(MATCHED_TEXT.value, StdLibDefault.String, 2)
                }
                data("Leaf") {
                    subtypes("Node")
                    property("value", StdLibDefault.String, 0)
                }
                data("Branch") {
                    supertypes("Node")
                    propertyListTypeOf(CHILDREN.value, StdLibDefault.AnyType.qualifiedTypeName.value, false, 0) //TODO: typeArgument ?
                    propertyListTypeOf(CHILD.value, StdLibDefault.AnyType.qualifiedTypeName.value, false, 1)
                }
                data("BranchSeparated") {
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
        val parseNodeNamespace = parseNodeTypesDomain.findNamespaceOrNull(QualifiedName("parse"))!!

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
    val exprInterpreter = ExpressionsInterpreterOverTypedObject(objectGraph,issues)

    fun clear() {
        this.issues.clear()
    }

    fun evaluate(evc: EvaluationContext<AsmValueType>,trRule: AsmTransformationRule): TypedObject<AsmValueType> {
        val tObj = evaluateSelfStatement(evc, trRule.expression)
        val asm = tObj
        return asm
    }

    private fun evaluateSelfStatement(evc: EvaluationContext<AsmValueType>, expression: Expression): TypedObject<AsmValueType> {
        return exprInterpreter.evaluateExpression(evc, expression)
    }

    private fun executeStatementOn(evc: EvaluationContext<AsmValueType>, st: AssignmentStatement, asm: AsmStructure) {
        val propertyName = st.lhsPropertyName
        val propValue = evaluateExpressionOver(st.rhs, evc)
        val tObj = objectGraph.toTypedObject(asm as AsmValueType)
        val pv = objectGraph.toTypedObject(propValue)
        objectGraph.setProperty(tObj, propertyName, pv)
        //asm.setProperty(PropertyValueName(st.lhsPropertyName), propValue, asm.property.size)
    }

    private fun evaluateExpressionOver(expr: Expression, evc: EvaluationContext<AsmValueType>): AsmValueType {
        val res = exprInterpreter.evaluateExpression(evc, expr)
        return res.self
    }

}