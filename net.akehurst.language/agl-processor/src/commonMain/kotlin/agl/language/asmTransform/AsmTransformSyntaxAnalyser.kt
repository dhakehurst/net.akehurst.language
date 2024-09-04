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

package net.akehurst.language.agl.agl.language.asmTransform

import net.akehurst.language.agl.agl.language.base.BaseSyntaxAnalyser
import net.akehurst.language.agl.language.asmTransform.*
import net.akehurst.language.agl.language.expressions.*
import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserByMethodRegistrationAbstract
import net.akehurst.language.api.language.asmTransform.TransformModel
import net.akehurst.language.api.language.asmTransform.TransformNamespace
import net.akehurst.language.api.language.asmTransform.TransformRuleSet
import net.akehurst.language.api.language.asmTransform.TransformationRule
import net.akehurst.language.api.language.base.PossiblyQualifiedName
import net.akehurst.language.api.language.base.PossiblyQualifiedName.Companion.asPossiblyQualifiedName
import net.akehurst.language.api.language.base.QualifiedName
import net.akehurst.language.api.language.base.SimpleName
import net.akehurst.language.api.language.expressions.AssignmentStatement
import net.akehurst.language.api.language.expressions.Expression
import net.akehurst.language.api.language.grammar.GrammarRuleName
import net.akehurst.language.api.sppt.Sentence
import net.akehurst.language.api.sppt.SpptDataNodeInfo
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.typemodel.api.PropertyName

class AsmTransformSyntaxAnalyser(
) : SyntaxAnalyserByMethodRegistrationAbstract<TransformModel>() {

    override val extendsSyntaxAnalyser: Map<QualifiedName, SyntaxAnalyser<*>> = mapOf(
        QualifiedName("Base") to BaseSyntaxAnalyser()
    )

    override val embeddedSyntaxAnalyser: Map<QualifiedName, SyntaxAnalyser<*>> = mapOf(
        AglExpressions.grammar.qualifiedName to ExpressionsSyntaxAnalyser()
    )

    override fun registerHandlers() {
        super.register(this::unit)
        super.register(this::transformList)
        super.register(this::transform)
        super.register(this::transformRuleList)
        super.register(this::transformRule)
        super.register(this::transformRuleRhs)
        super.register(this::createRule)
        super.register(this::modifyRule)
        super.register(this::assignmentStatement)
        super.register(this::propertyName)
        super.register(this::grammarRuleName)
        super.register(this::possiblyQualifiedTypeName)
    }

    // unit = namespace transformList ;
    private fun unit(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): TransformModel {
        val namespaceQName = QualifiedName((children[0] as List<String>).joinToString(separator = "."))
        val namespace = TransformNamespaceDefault(namespaceQName)
        val transformBuilders = children[1] as List<(TransformNamespace) -> TransformRuleSet>
        transformBuilders.map {
            val tr = it.invoke(namespace)
            namespace.addDefinition(tr)
            tr
        }
        return TransformModelDefault(
            name = SimpleName(""), //TODO
            null,
            listOf(namespace)
        )
    }

    // transformList = transform+ ;
    private fun transformList(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<(TransformNamespace) -> TransformRuleSet> =
        children as List<(TransformNamespace) -> TransformRuleSet>

    // transform = 'transform' IDENTIFIER '{' transformRuleList '}' ;
    private fun transform(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (TransformNamespace) -> TransformRuleSet {
        val name = SimpleName(children[1] as String)
        val rules = children[3] as List<TransformationRule>

        return { namespace ->
            val asm = TransformRuleSetDefault(namespace, name, rules)
            rules.forEach { asm.addRule(it) }
            asm
        }
    }

    // transformRuleList = transformRule+ ;
    private fun transformRuleList(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<TransformationRule> =
        children as List<TransformationRuleAbstract>

    // transformRule = grammarRuleName ':' transformRuleRhs ;
    private fun transformRule(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): TransformationRule {
        val grammarRuleName = children[0] as GrammarRuleName
        val trRule = children[2] as TransformationRuleAbstract
        trRule.grammarRuleName = grammarRuleName
        return trRule
    }

    // transformRuleRhs = createRule | modifyRule ;
    private fun transformRuleRhs(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): TransformationRule =
        children[0] as TransformationRuleAbstract

    // createRule = possiblyQualifiedTypeName optStatementBlock ;
    private fun createRule(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): TransformationRule {
        val typeName = children[0] as PossiblyQualifiedName
        val statements = children[1]?.let { it as List<AssignmentStatement> } ?: emptyList()
        val expr = CreateObjectExpressionSimple(typeName, emptyList()).also {
            it.propertyAssignments = statements
        }
        val tr = TransformationRuleDefault(typeName, expr)
        return tr
    }

    // statementBlock = '{' assignmentStatement+ '}' ;
    private fun statementBlock(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<AssignmentStatement> {
        return children[1] as List<AssignmentStatement>
    }

    // modifyRule = '{' possiblyQualifiedTypeName '->' statementList '}' ;
    private fun modifyRule(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): TransformationRule {
        val possiblyQualifiedTypeName = children[1] as PossiblyQualifiedName
        val statements = children[3] as List<AssignmentStatement>
        val expr = OnExpressionSimple(RootExpressionSimple("\$it"))
        expr.propertyAssignments = statements
        val tr = TransformationRuleDefault(possiblyQualifiedTypeName, expr)
        return tr
    }

    // assignmentStatement = propertyName ':=' Expression.expression ;
    private fun assignmentStatement(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): AssignmentStatementSimple {
        val propName = children[0] as PropertyName
        val expr = children[2] as Expression
        return AssignmentStatementSimple(propName, expr)
    }

    // propertyName = IDENTIFIER ;
    private fun propertyName(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): PropertyName =
        PropertyName(children[0] as String)

    // grammarRuleName = IDENTIFIER ;
    private fun grammarRuleName(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): GrammarRuleName =
        GrammarRuleName(children[0] as String)

    // possiblyQualifiedTypeName = qualifiedName ;
    private fun possiblyQualifiedTypeName(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): PossiblyQualifiedName =
        ((children[0] as List<String>).joinToString(separator = ".")).asPossiblyQualifiedName
}