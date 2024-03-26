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
import net.akehurst.language.agl.language.asmTransform.AsmTransformModelSimple
import net.akehurst.language.agl.language.asmTransform.TransformationRuleAbstract
import net.akehurst.language.agl.language.asmTransform.TransformationRuleSimple
import net.akehurst.language.agl.language.asmTransform.TransformationStatementAbstract
import net.akehurst.language.agl.language.expressions.*
import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserByMethodRegistrationAbstract
import net.akehurst.language.api.language.asmTransform.AsmTransformModel
import net.akehurst.language.api.language.asmTransform.TransformationRule
import net.akehurst.language.api.language.expressions.AssignmentStatement
import net.akehurst.language.api.language.expressions.Expression
import net.akehurst.language.api.sppt.Sentence
import net.akehurst.language.api.sppt.SpptDataNodeInfo
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser

class AsmTransformSyntaxAnalyser(
) : SyntaxAnalyserByMethodRegistrationAbstract<List<AsmTransformModel>>() {

    override val extendsSyntaxAnalyser: Map<String, SyntaxAnalyser<*>> = mapOf(
        "Base" to BaseSyntaxAnalyser()
    )

    override val embeddedSyntaxAnalyser: Map<String, SyntaxAnalyser<*>> = mapOf(
        ExpressionsGrammar.qualifiedName to ExpressionsSyntaxAnalyser()
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
        super.register(this::statementList)
        super.register(this::assignmentStatement)
        super.register(this::propertyName)
        super.register(this::grammarRuleName)
        super.register(this::typeName)
    }

    // unit = namespace transformList ;
    private fun unit(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<AsmTransformModel> {
        val namespaceQName = (children[0] as List<String>).joinToString(separator = ".")
        val trMdls = children[1] as List<(String) -> AsmTransformModel>
        val asm = trMdls.map { it.invoke(namespaceQName) }
        return asm
    }

    // transformList = transform+ ;
    private fun transformList(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<(String) -> AsmTransformModel> =
        children as List<(String) -> AsmTransformModel>

    // transform = 'transform' qualifiedName '{' transformRuleList '}' ;
    private fun transform(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (String) -> AsmTransformModel {
        val name = (children[1] as List<String>).joinToString(separator = ".")
        val rules = children[3] as List<TransformationRuleAbstract>

        return { namespaceQName ->
            val asm = AsmTransformModelSimple("$namespaceQName.$name")
            rules.forEach { asm.addRule(it) }
            asm
        }
    }

    // transformRuleList = transformRule+ ;
    private fun transformRuleList(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<TransformationRuleAbstract> =
        children as List<TransformationRuleAbstract>

    // transformRule = grammarRuleName ':' transformRuleRhs ;
    private fun transformRule(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): TransformationRuleAbstract {
        val grammarRuleName = children[0] as String
        val trRule = children[2] as TransformationRuleAbstract
        trRule.grammarRuleName = grammarRuleName
        return trRule
    }

    // transformRuleRhs = createRule | modifyRule ;
    private fun transformRuleRhs(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): TransformationRule =
        children[0] as TransformationRuleAbstract

    // createRule = typeName optStatementBlock ;
    private fun createRule(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): TransformationRule {
        val typeName = children[0] as String
        val statements = children[1]?.let { it as List<AssignmentStatement> } ?: emptyList()
        val expr = CreateObjectExpressionSimple(typeName, emptyList()).also {
            it.propertyAssignments = statements
        }
        val tr = TransformationRuleSimple(typeName, expr)
        return tr
    }

    // statementBlock = '{' statementList '}' ;
    private fun statementBlock(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<AssignmentStatement> =
        children[1] as List<AssignmentStatement>

    // modifyRule = '{' typeName '->' statementList '}' ;
    private fun modifyRule(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): TransformationRule {
        val typeName = children[1] as String
        val statements = children[3] as List<AssignmentStatement>
        val expr = OnExpressionSimple(RootExpressionSimple("\$it"))
        expr.propertyAssignments = statements
        val tr = TransformationRuleSimple(typeName, expr)
        return tr
    }

    // statementList = assignmentStatement+ ;
    private fun statementList(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<TransformationStatementAbstract> =
        children as List<TransformationStatementAbstract>

    // assignmentStatement = propertyName ':=' Expression.expression ;
    private fun assignmentStatement(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): AssignmentStatementSimple {
        val propName = children[0] as String
        val expr = children[2] as Expression
        return AssignmentStatementSimple(propName, expr)
    }

    // propertyName = IDENTIFIER ;
    private fun propertyName(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): String =
        children[0] as String

    // grammarRuleName = IDENTIFIER ;
    private fun grammarRuleName(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): String =
        children[0] as String

    // typeName = qualifiedName ;
    private fun typeName(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): String =
        (children[0] as List<String>).joinToString(separator = ".")
}