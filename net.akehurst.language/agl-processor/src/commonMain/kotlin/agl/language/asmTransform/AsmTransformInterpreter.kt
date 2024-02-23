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

import net.akehurst.language.agl.asm.AsmListSimple
import net.akehurst.language.agl.asm.AsmStructureSimple
import net.akehurst.language.agl.language.expressions.ExpressionsInterpreterOverTypedObject
import net.akehurst.language.agl.language.expressions.TypedObject
import net.akehurst.language.agl.language.expressions.asm
import net.akehurst.language.agl.language.expressions.toTypedObject
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.api.asm.AsmPath
import net.akehurst.language.api.asm.AsmValue
import net.akehurst.language.api.language.asmTransform.AssignmentTransformationStatement
import net.akehurst.language.api.language.asmTransform.CreateObjectRule
import net.akehurst.language.api.language.asmTransform.TransformationRule
import net.akehurst.language.api.language.expressions.Expression
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.typemodel.api.*
import net.akehurst.language.typemodel.simple.SimpleTypeModelStdLib

class TypedObjectParseNode(
    val typeModel: TypeModel,
    val children: List<AsmValue>
) : TypedObject {
    companion object {
        val parseNodeTypeModel = typeModel("ParseNodes", true) {
            namespace("parse") {

            }
        }
        val parseNodeNamespace = parseNodeTypeModel.namespace["parse"]!!
        val PARSE_NODE_TYPE = parseNodeNamespace.createTupleType().also {
            it.appendPropertyStored(
                "children",
                SimpleTypeModelStdLib.List.type(listOf(SimpleTypeModelStdLib.AnyType)),
                setOf(PropertyCharacteristic.COMPOSITE, PropertyCharacteristic.MEMBER)
            )
        }
    }

    override val type: TypeInstance = PARSE_NODE_TYPE.type()

    override fun getPropertyValue(propertyDeclaration: PropertyDeclaration): TypedObject {
        return AsmListSimple(children).toTypedObject(typeModel)
    }

    override fun asString(): String = "ParseNode"
}

class AsmTransformInterpreter(
    val typeModel: TypeModel
) {

    val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)

    fun evaluate(contextNode: TypedObjectParseNode, path: AsmPath, trRule: TransformationRule): AsmValue {
        val asm = when (trRule) {
            is CreateObjectRule -> AsmStructureSimple(
                path = path,
                qualifiedTypeName = trRule.resolvedType.qualifiedTypeName
            )

            else -> TODO()
        }

        for (st in trRule.modifyStatements) {
            executeStatementOn(contextNode, st, asm)
        }

        return asm
    }

    private fun executeStatementOn(node: TypedObjectParseNode, st: AssignmentTransformationStatement, asm: AsmStructureSimple) {
        val propValue = evaluateExpressionOver(st.rhs, node)
        asm.setProperty(st.lhsPropertyName, propValue, asm.property.size)
    }

    fun evaluateExpressionOver(expr: Expression, node: TypedObjectParseNode): AsmValue {
        val ei = ExpressionsInterpreterOverTypedObject(typeModel)
        val res = ei.evaluateExpression(node, expr)
        return res.asm
    }

}