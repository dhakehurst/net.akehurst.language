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
import net.akehurst.language.api.language.asmTransform.AsmTransformModel
import net.akehurst.language.api.language.asmTransform.TransformationRule
import net.akehurst.language.api.language.expressions.Expression
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.simple.SimpleTypeModelStdLib

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

    private fun trRule(grammarRuleName: String, typeName: String, tr: TransformationRuleAbstract) {
        tr.grammarRuleName = grammarRuleName
        _rules.add(tr)
        if (createTypes) {
            val ns = typeModel.findOrCreateNamespace(this.qualifiedName, listOf(SimpleTypeModelStdLib.qualifiedName))
            val t = ns.findOwnedOrCreatePrimitiveTypeNamed(typeName)
            tr.resolveTypeAs(t.type())
        } else {
            val ns = typeModel.namespace[this.qualifiedName]!!
            val t = ns.findOwnedTypeNamed(typeName)!!
            tr.resolveTypeAs(t.type())
        }
    }

    fun noActionRule(grammarRuleName: String, typeName: String) {
        val tr = NoActionTransformationRuleSimple(typeName)
        trRule(grammarRuleName, typeName, tr)
    }

    fun stringRule(grammarRuleName: String) {
        val tr = StringActionTransformationRuleSimple()
        tr.grammarRuleName = grammarRuleName
        tr.resolveTypeAs(SimpleTypeModelStdLib.String)
    }

    fun subtypeRule(grammarRuleName: String, typeName: String) {
        val tr = SubtypeTransformationRuleSimple(typeName)
        trRule(grammarRuleName, typeName, tr)
    }

    fun createObject(grammarRuleName: String, typeName: String, modifyStatements: AssignmentBuilder.() -> Unit = {}) {
        val tr = CreateObjectRuleSimple(typeName)
        trRule(grammarRuleName, typeName, tr)
        val ab = AssignmentBuilder()
        ab.modifyStatements()
        val list = ab.build(tr)
    }

    fun build(): AsmTransformModel {
        val res = AsmTransformModelSimple(qualifiedName)
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

    fun build(tr: TransformationRuleAbstract) {
        when (tr) {
            is CreateObjectRuleSimple -> {
                _assignments.forEach { (lhs, expr) -> tr.appendAssignment(lhs, expr) }
            }

            is ModifyObjectRuleSimple -> {
                _assignments.forEach { (lhs, expr) -> tr.appendAssignment(lhs, expr) }
            }
        }
    }
}