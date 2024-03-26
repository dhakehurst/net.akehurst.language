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

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.language.expressions.AssignmentStatementSimple
import net.akehurst.language.agl.language.expressions.CreateObjectExpressionSimple
import net.akehurst.language.agl.language.expressions.RootExpressionSimple
import net.akehurst.language.api.language.asmTransform.AsmTransformModel
import net.akehurst.language.api.language.asmTransform.TransformationRule
import net.akehurst.language.api.language.expressions.AssignmentStatement
import net.akehurst.language.api.language.expressions.Expression
import net.akehurst.language.typemodel.api.TypeInstance
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.simple.SimpleTypeModelStdLib
import net.akehurst.language.typemodel.simple.UnnamedSupertypeTypeSimple

@DslMarker
annotation class AsmTransformModelDslMarker

fun asmTransform(
    qualifiedName: String,
    typeModel: TypeModel,
    createTypes: Boolean,
    init: AsmTransformModelBuilder.() -> Unit
): AsmTransformModel {
    val b = AsmTransformModelBuilder(qualifiedName, typeModel, createTypes)
    b.init()
    val m = b.build()
    return m
}

@AsmTransformModelDslMarker
class AsmTransformModelBuilder(
    val qualifiedName: String,
    val typeModel: TypeModel,
    val createTypes: Boolean
) {
    private val _rules = mutableListOf<TransformationRule>()

    public fun expression(expressionStr: String): Expression {
        val res = Agl.registry.agl.expressions.processor!!.process(expressionStr)
        check(res.issues.isEmpty()) { res.issues.toString() }
        return res.asm!!
    }

    private fun trRule(grammarRuleName: String, tr: TransformationRuleAbstract) {
        tr.grammarRuleName = grammarRuleName
        _rules.add(tr)
        if (createTypes) {
            val ns = typeModel.findOrCreateNamespace(this.qualifiedName, listOf(SimpleTypeModelStdLib.qualifiedName))
            val t = ns.findOwnedOrCreatePrimitiveTypeNamed(tr.qualifiedTypeName)
            tr.resolveTypeAs(t.type())
        } else {
            val ns = typeModel.namespace[this.qualifiedName]!!
            val t = ns.findOwnedTypeNamed(tr.qualifiedTypeName) ?: error("Type '${tr.qualifiedTypeName}' not found")
            tr.resolveTypeAs(t.type())
        }
    }

    fun nothingRule(grammarRuleName: String) {
        val tr = TransformationRuleSimple(SimpleTypeModelStdLib.NothingType.qualifiedTypeName, RootExpressionSimple(RootExpressionSimple.NOTHING))
        trRule(grammarRuleName, tr)
    }

    fun leafStringRule(grammarRuleName: String) {
        val tr = TransformationRuleSimple(SimpleTypeModelStdLib.String.qualifiedTypeName, RootExpressionSimple("leaf"))
        tr.grammarRuleName = grammarRuleName
        tr.resolveTypeAs(SimpleTypeModelStdLib.String)
        _rules.add(tr)
    }

    fun transRule(grammarRuleName: String, type: TypeInstance, expression: Expression) {
        val tr = transformationRule(
            type = type,
            expression = expression
        )
        tr.grammarRuleName = grammarRuleName
        tr.resolveTypeAs(SimpleTypeModelStdLib.String)
        _rules.add(tr)
    }

    fun child0StringRule(grammarRuleName: String) = transRule(grammarRuleName, SimpleTypeModelStdLib.String, expression("child[0]"))

    fun subtypeRule(grammarRuleName: String, typeName: String) {
        val tr = TransformationRuleSimple(typeName, expression("child[0]"))
        trRule(grammarRuleName, tr)
    }

    fun unnamedSubtypeRule(grammarRuleName: String, subtypeNames: List<String>) {
        val tr = TransformationRuleSimple(UnnamedSupertypeTypeSimple.NAME, expression("child[0]"))
        tr.grammarRuleName = grammarRuleName
        val ns = typeModel.findOrCreateNamespace(this.qualifiedName, listOf(SimpleTypeModelStdLib.qualifiedName))
        val stList = subtypeNames.map { n ->
            _rules.first { it.grammarRuleName == n }.resolvedType
        }
        val t = ns.createUnnamedSupertypeType(stList)
        tr.resolveTypeAs(t.type())
        _rules.add(tr)
    }

    fun createObject(grammarRuleName: String, typeName: String, modifyStatements: AssignmentBuilder.() -> Unit = {}) {
        val expr = CreateObjectExpressionSimple(typeName, emptyList())
        val tr = TransformationRuleSimple(typeName, expr)
        trRule(grammarRuleName, tr)
        val ab = AssignmentBuilder()
        ab.modifyStatements()
        val ass = ab.build()
        expr.propertyAssignments = ass
    }

    fun build(): AsmTransformModel {
        val res = AsmTransformModelSimple(qualifiedName)
        res.typeModel = typeModel
        _rules.forEach { res.addRule(it) }
        return res
    }
}

@AsmTransformModelDslMarker
class AssignmentBuilder() {

    private val _assignments = mutableListOf<Pair<String, Expression>>()

    fun assignment(lhsPropertyName: String, expressionStr: String) {
        val res = Agl.registry.agl.expressions.processor!!.process(expressionStr)
        check(res.issues.isEmpty()) { res.issues.toString() }
        val expr = res.asm!!
        _assignments.add(Pair(lhsPropertyName, expr))
    }

    fun build(): List<AssignmentStatement> {
        return _assignments.map { AssignmentStatementSimple(it.first, it.second) }
    }
}