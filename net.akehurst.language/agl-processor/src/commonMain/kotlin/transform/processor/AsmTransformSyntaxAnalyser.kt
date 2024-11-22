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

import net.akehurst.language.expressions.processor.AglExpressions
import net.akehurst.language.expressions.processor.ExpressionsSyntaxAnalyser
import net.akehurst.language.expressions.asm.AssignmentStatementSimple
import net.akehurst.language.expressions.asm.CreateObjectExpressionSimple
import net.akehurst.language.expressions.asm.OnExpressionSimple
import net.akehurst.language.expressions.asm.RootExpressionSimple
import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserByMethodRegistrationAbstract
import net.akehurst.language.expressions.api.AssignmentStatement
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.base.api.*
import net.akehurst.language.base.processor.BaseSyntaxAnalyser
import net.akehurst.language.expressions.api.CreateObjectExpression
import net.akehurst.language.grammar.api.GrammarNamespace
import net.akehurst.language.grammar.api.GrammarRuleName
import net.akehurst.language.grammar.asm.GrammarNamespaceDefault
import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sppt.api.SpptDataNodeInfo
import net.akehurst.language.transform.api.TransformModel
import net.akehurst.language.transform.api.TransformNamespace
import net.akehurst.language.transform.api.TransformRuleSet
import net.akehurst.language.transform.api.TransformationRule
import net.akehurst.language.transform.asm.*
import net.akehurst.language.typemodel.api.PropertyName
import net.akehurst.language.typemodel.asm.SimpleTypeModelStdLib
import net.akehurst.language.typemodel.asm.TypeModelSimple

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
        super.register(this::namespace)
        super.register(this::transformList)
        super.register(this::transform)
        super.register(this::transformRuleList)
        super.register(this::transformRule)
        super.register(this::transformRuleRhs)
        super.register(this::createRule)
        super.register(this::modifyRule)
        super.register(this::assignmentStatement)
        super.register(this::propertyName)
        super.register(this::expression)
        super.register(this::grammarRuleName)
        super.register(this::possiblyQualifiedTypeName)
    }

    // override unit = option* namespace* ;
    private fun unit(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): TransformModel {
        val options = children[0] as List<Option>
        val namespaces = children[1] as List<TransformNamespace>
        val name = SimpleName("ParsedTransformUnit") //TODO: how to specify name, does it matter?
        //val typeModel = TypeModelSimple(name) //TODO: how to specify type model ?
       // typeModel.addNamespace(SimpleTypeModelStdLib)
        return TransformModelDefault(
            name = name,
            options,
            namespaces
        )
    }

    // override namespace = 'namespace' possiblyQualifiedName option* typeImport* import* transform* ;
    private fun namespace(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): TransformNamespace {
        val pqn = children[1] as PossiblyQualifiedName
        val nsName = pqn.asQualifiedName(null)
        val options = children[2] as List<Option>
        val typeImports = children[3] as List<Import>
        val imports = children[4] as List<Import>
        val transformBuilders = children[5] as List<(TransformNamespaceDefault) -> TransformRuleSet>

        val namespace = TransformNamespaceDefault(nsName,options,imports)
        transformBuilders.map {
            val tr = it.invoke(namespace)
            namespace.addDefinition(tr)
            tr
        }
        return namespace
    }

    // transformList = transform+ ;
    private fun transformList(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<(TransformNamespace) -> TransformRuleSet> =
        children as List<(TransformNamespace) -> TransformRuleSet>

    // transform = 'transform' IDENTIFIER extends? '{' option* transformRule+ '} ;
    private fun transform(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (TransformNamespace) -> TransformRuleSet {
        val name = SimpleName(children[1] as String)
        val extends = children[2] as List<PossiblyQualifiedName>? ?: emptyList()
        val options = children[4] as List<Option>
        val rules = children[5] as List<TransformationRule>

        return { namespace ->
            val extendRefs = extends.map { TransformRuleSetReferenceDefault(namespace, it) }
            val asm = TransformRuleSetDefault(namespace, name, extendRefs, options, rules)
            rules.forEach { asm.addRule(it) }
            asm
        }
    }
    // extends = ':' [possiblyQualifiedName / ',']+ ;


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
        val expression = children[0] as Expression
        val tr = when (expression) {
            is CreateObjectExpression -> TransformationRuleDefault(expression.possiblyQualifiedTypeName, expression)
            else -> error("Unsupported")
        }
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
        val propName = children[0] as String
        val expr = children[2] as Expression
        return AssignmentStatementSimple(propName, expr)
    }

    // propertyName = IDENTIFIER ;
    private fun propertyName(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): String =
        children[0] as String

    // expression = Expression::expression ;
    private fun expression(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Expression =
        children[0] as Expression

    // grammarRuleName = IDENTIFIER ;
    private fun grammarRuleName(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): GrammarRuleName =
        GrammarRuleName(children[0] as String)

    // possiblyQualifiedTypeName = qualifiedName ;
    private fun possiblyQualifiedTypeName(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): PossiblyQualifiedName =
        children[0] as PossiblyQualifiedName
}