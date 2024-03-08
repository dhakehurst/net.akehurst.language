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

import net.akehurst.language.agl.asm.AsmListSeparatedSimple
import net.akehurst.language.agl.asm.AsmListSimple
import net.akehurst.language.agl.asm.AsmStructureSimple
import net.akehurst.language.agl.language.expressions.ExpressionsInterpreterOverTypedObject
import net.akehurst.language.agl.language.expressions.TypedObject
import net.akehurst.language.agl.language.expressions.asm
import net.akehurst.language.agl.language.expressions.toTypedObject
import net.akehurst.language.api.asm.AsmPath
import net.akehurst.language.api.asm.AsmStructure
import net.akehurst.language.api.asm.AsmValue
import net.akehurst.language.api.language.asmTransform.AssignmentTransformationStatement
import net.akehurst.language.api.language.asmTransform.SelfStatement
import net.akehurst.language.api.language.asmTransform.TransformationRule
import net.akehurst.language.api.language.expressions.Expression
import net.akehurst.language.collections.toSeparatedList
import net.akehurst.language.typemodel.api.*
import net.akehurst.language.typemodel.simple.SimpleTypeModelStdLib

class TypedObjectParseNode(
    val typeModel: TypeModel,
    override val type: TypeInstance,
    val children: List<AsmValue>
) : TypedObject {
    companion object {
        val parseNodeTypeModel = typeModel("ParseNodes", true) {
            namespace("parse") {

            }
        }
        val parseNodeNamespace = parseNodeTypeModel.namespace["parse"]!!
        val PARSE_NODE_TYPE_LIST_SIMPLE = parseNodeNamespace.createTupleType().also {
            it.appendPropertyStored(
                "children",
                SimpleTypeModelStdLib.List.type(listOf(SimpleTypeModelStdLib.AnyType)),
                setOf(PropertyCharacteristic.COMPOSITE, PropertyCharacteristic.MEMBER)
            )
            it.appendPropertyStored(
                "child",
                SimpleTypeModelStdLib.List.type(listOf(SimpleTypeModelStdLib.AnyType)),
                setOf(PropertyCharacteristic.COMPOSITE, PropertyCharacteristic.MEMBER)
            )
        }
        val PARSE_NODE_TYPE_LIST_SEPARATED = parseNodeNamespace.createTupleType().also {
            it.appendPropertyStored(
                "children",
                SimpleTypeModelStdLib.ListSeparated.type(listOf(SimpleTypeModelStdLib.AnyType, SimpleTypeModelStdLib.AnyType)),
                setOf(PropertyCharacteristic.COMPOSITE, PropertyCharacteristic.MEMBER)
            )
            it.appendPropertyStored(
                "child",
                SimpleTypeModelStdLib.ListSeparated.type(listOf(SimpleTypeModelStdLib.AnyType)),
                setOf(PropertyCharacteristic.COMPOSITE, PropertyCharacteristic.MEMBER)
            )
        }
    }

    override fun getPropertyValue(propertyDeclaration: PropertyDeclaration): TypedObject {
        //TODO: should really check/test propertyDeclaration
        return when {
            propertyDeclaration.typeInstance.declaration.conformsTo(SimpleTypeModelStdLib.ListSeparated) ->
                AsmListSeparatedSimple(children.toSeparatedList()).toTypedObject(typeModel)

            propertyDeclaration.typeInstance.declaration.conformsTo(SimpleTypeModelStdLib.List) ->
                AsmListSimple(children).toTypedObject(typeModel)

            else -> TODO("not supported")
        }
    }

    override fun asString(): String = "ParseNode"
}

class AsmTransformInterpreter(
    val typeModel: TypeModel
) {

    val exprInterpreter = ExpressionsInterpreterOverTypedObject(typeModel)
    val issues get() = exprInterpreter.issues// IssueHolder(LanguageProcessorPhase.INTERPRET)

    fun evaluate(contextNode: TypedObjectParseNode, path: AsmPath, trRule: TransformationRule): AsmValue {
        val tObj = evaluateSelfStatement(contextNode, path, trRule.selfStatement)
        val asm = tObj
        when {
            trRule.modifyStatements.isEmpty() -> Unit
            else -> when (asm) {
                is AsmStructure -> {
                    for (st in trRule.modifyStatements) {
                        executeStatementOn(contextNode, st, asm)
                    }
                }

                else -> {
                    issues.error(null, "'self' value for transformation-rule is not a Structure, cannot set/modify properties")
                }
            }
        }
        return asm
    }

    private fun evaluateSelfStatement(contextNode: TypedObjectParseNode, path: AsmPath, selfStatement: SelfStatement): AsmValue {
        return when (selfStatement) {
            is ConstructSelfStatementSimple -> AsmStructureSimple(path = path, qualifiedTypeName = selfStatement.qualifiedTypeName)
            is ExpressionSelfStatementSimple -> exprInterpreter.evaluateExpression(contextNode, selfStatement.expression).asm
            else -> TODO()
        }
    }

    private fun executeStatementOn(node: TypedObjectParseNode, st: AssignmentTransformationStatement, asm: AsmStructure) {
        val propValue = evaluateExpressionOver(st.rhs, node)
        asm.setProperty(st.lhsPropertyName, propValue, asm.property.size)
    }

    fun evaluateExpressionOver(expr: Expression, node: TypedObject): AsmValue {
        val res = exprInterpreter.evaluateExpression(node, expr)
        return res.asm
    }

}