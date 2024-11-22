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

package net.akehurst.language.transform.builder

import net.akehurst.language.agl.Agl
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.api.asPossiblyQualifiedName
import net.akehurst.language.expressions.api.AssignmentStatement
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.asm.*
import net.akehurst.language.grammar.api.GrammarRuleName
import net.akehurst.language.grammarTypemodel.asm.GrammarTypeNamespaceSimple
import net.akehurst.language.transform.api.TransformModel
import net.akehurst.language.transform.api.TransformNamespace
import net.akehurst.language.transform.api.TransformRuleSet
import net.akehurst.language.transform.api.TransformationRule
import net.akehurst.language.transform.asm.*
import net.akehurst.language.typemodel.api.TypeDeclaration
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.api.UnnamedSupertypeType
import net.akehurst.language.typemodel.asm.SimpleTypeModelStdLib
import net.akehurst.language.typemodel.builder.SubtypeListBuilder

@DslMarker
annotation class AsmTransformModelDslMarker

fun asmTransform(
    name: String,
    typeModel: TypeModel,
    createTypes: Boolean,
    init: AsmTransformModelBuilder.() -> Unit
): TransformModel {
    val b = AsmTransformModelBuilder(SimpleName(name), typeModel, createTypes)
    b.init()
    val m = b.build()
    return m
}

@AsmTransformModelDslMarker
class AsmTransformModelBuilder internal constructor(
    private val name: SimpleName,
    private val typeModel: TypeModel,
    private val createTypes: Boolean
) {

    private val namespaces = mutableListOf<TransformNamespace>()

    fun namespace(qualifiedName: String, init: AsmTransformNamespaceBuilder.() -> Unit) {
        val b = AsmTransformNamespaceBuilder(QualifiedName(qualifiedName), typeModel, createTypes)
        b.init()
        val v = b.build()
        namespaces.add(v)
    }

    fun build(): TransformModel = TransformDomainDefault(name, namespace = namespaces).also { it.typeModel = typeModel }
}

@AsmTransformModelDslMarker
class AsmTransformNamespaceBuilder internal constructor(
    private val qualifiedName: QualifiedName,
    private val typeModel: TypeModel,
    private val createTypes: Boolean
) {

    private val namespace = TransformNamespaceDefault(qualifiedName)

    fun transform(name: String, init: AsmTransformRuleSetBuilder.() -> Unit) {
        val b = AsmTransformRuleSetBuilder(namespace, SimpleName(name), typeModel, createTypes)
        b.init()
        b.build()
    }

    fun build(): TransformNamespace = namespace
}

@AsmTransformModelDslMarker
class AsmTransformRuleSetBuilder internal constructor(
    private val namespace: TransformNamespaceDefault,
    private val name: SimpleName,
    private val typeModel: TypeModel,
    private val createTypes: Boolean
) {
    private val _rules = mutableListOf<TransformationRule>()
    private val defaultTypeNamespaceQualifiedName = namespace.qualifiedName.append(name)

    private fun resolveType(grName: GrammarRuleName, typeName: String): TypeDeclaration {
        val pqt = typeName.asPossiblyQualifiedName
        return if (createTypes) {
            when (pqt) {
                is SimpleName -> {
                    val tns = typeModel.findOrCreateNamespace(defaultTypeNamespaceQualifiedName, listOf(SimpleTypeModelStdLib.qualifiedName.asImport))
                    val td = tns.findOwnedOrCreateDataTypeNamed(pqt)
                    if (tns is GrammarTypeNamespaceSimple) {
                        tns.addTypeFor(grName, td.type())
                    }
                    td
                }

                is QualifiedName -> {
                    val nsqn = pqt.front
                    val tn = pqt.last
                    val tns = typeModel.findOrCreateNamespace(nsqn, listOf(SimpleTypeModelStdLib.qualifiedName.asImport))
                    val td = tns.findOwnedOrCreateDataTypeNamed(tn)
                    if (tns is GrammarTypeNamespaceSimple) {
                        tns.addTypeFor(grName, td.type())
                    }
                    td
                }

                else -> error("Unsupported")
            }
        } else {
            val ns = typeModel.findNamespaceOrNull(defaultTypeNamespaceQualifiedName)!!
            val qt = ns.findTypeNamed(pqt) ?: error("Type '$pqt' not found")
            return qt
        }
    }

    fun expression(expressionStr: String): Expression {
        val res = Agl.registry.agl.expressions.processor!!.process(expressionStr)
        check(res.issues.isEmpty()) { res.issues.toString() }
        return res.asm!!
    }

    private fun trRule(grammarRuleName: String, typeName: String, expression: Expression) {
        val qt = resolveType(GrammarRuleName(grammarRuleName), typeName)
        val tr = TransformationRuleDefault(qt.qualifiedName, expression)
        tr.grammarRuleName = GrammarRuleName(grammarRuleName)
        _rules.add(tr)
        tr.resolveTypeAs(qt.type())
        /*
        if (createTypes) {
            val tns = typeModel.findOrCreateNamespace(defaultTypeNamespaceQualifiedName, listOf(SimpleTypeModelStdLib.qualifiedName.asImport))
            val t = tns.findTypeNamed(tr.possiblyQualifiedTypeName) ?: tns.findOwnedOrCreateDataTypeNamed(tr.possiblyQualifiedTypeName.simpleName)
            tr.resolveTypeAs(t.type())
        } else {
            val tns = typeModel.findNamespaceOrNull(defaultTypeNamespaceQualifiedName)!!
            val t = tns.findTypeNamed(tr.possiblyQualifiedTypeName) ?: error("Type '${tr.possiblyQualifiedTypeName}' not found")
            tr.resolveTypeAs(t.type())
        }
         */
    }

    //fun nothingRule(grammarRuleName: String) {
    //    val tr = TransformationRuleDefault(SimpleTypeModelStdLib.NothingType.qualifiedTypeName, RootExpressionSimple.NOTHING)
    //    trRule(grammarRuleName, tr)
    //}

    fun leafStringRule(grammarRuleName: String) {
        val tr = TransformationRuleDefault(SimpleTypeModelStdLib.String.qualifiedTypeName, RootExpressionSimple.SELF) //("leaf"))
        tr.grammarRuleName = GrammarRuleName(grammarRuleName)
        tr.resolveTypeAs(SimpleTypeModelStdLib.String)
        _rules.add(tr)
    }

    fun transRule(grammarRuleName: String, typeName: String, expressionStr: String) {
        val expression = expression(expressionStr)
        val typeDef = typeModel.findFirstByNameOrNull(SimpleName(typeName)) ?: error("Type '$typeName' not found in type-model '${typeModel.name}'")
        val tr = transformationRule(
            type = typeDef.type(),
            expression = expression
        )
        tr.grammarRuleName = GrammarRuleName(grammarRuleName)
        tr.resolveTypeAs(SimpleTypeModelStdLib.String)
        _rules.add(tr)
    }

    fun child0StringRule(grammarRuleName: String) = transRule(grammarRuleName, "String", "child[0]")

    fun subtypeRule(grammarRuleName: String, typeName: String) {
        trRule(grammarRuleName, typeName, expression("child[0]"))
    }

    fun unnamedSubtypeRule(grammarRuleName: String, expressionStr: String, init: SubtypeListBuilder.() -> Unit) {
        val ns = typeModel.findOrCreateNamespace(defaultTypeNamespaceQualifiedName, listOf(SimpleTypeModelStdLib.qualifiedName.asImport))
        val b = SubtypeListBuilder(ns, mutableListOf())
        b.init()
        val subtypes = b.build()
        val expr = expression(expressionStr)
        val tr = TransformationRuleDefault(UnnamedSupertypeType.NAME, expr)
        tr.grammarRuleName = GrammarRuleName(grammarRuleName)
        val t = ns.createUnnamedSupertypeType(subtypes)
        tr.resolveTypeAs(t.type())
        _rules.add(tr)
    }

    fun createObject(grammarRuleName: String, typeName: String, modifyStatements: AssignmentBuilder.() -> Unit = {}) {
        val qtn = resolveType(GrammarRuleName(grammarRuleName), typeName).qualifiedName
        val expr = CreateObjectExpressionSimple(qtn, emptyList())
        val ab = AssignmentBuilder()
        ab.modifyStatements()
        val ass = ab.build()
        expr.propertyAssignments = ass
        trRule(grammarRuleName, typeName, expr)
    }

    fun build(): TransformRuleSet {
        val rule = TransformRuleSetDefault(namespace, name, _rules= _rules)
        namespace.addDefinition(rule)
        return rule
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