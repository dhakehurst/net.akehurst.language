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

import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserByMethodRegistrationAbstract
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.base.api.Import
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.asm.OptionHolderDefault
import net.akehurst.language.base.processor.BaseSyntaxAnalyser
import net.akehurst.language.collections.toSeparatedList
import net.akehurst.language.expressions.api.AssignmentStatement
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.asm.AssignmentStatementDefault
import net.akehurst.language.expressions.asm.OnExpressionDefault
import net.akehurst.language.expressions.asm.RootExpressionDefault
import net.akehurst.language.expressions.processor.AglExpressions
import net.akehurst.language.expressions.processor.ExpressionsSyntaxAnalyser
import net.akehurst.language.grammar.api.GrammarRuleName
import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sppt.api.SpptDataNodeInfo
import net.akehurst.language.asmTransform.api.AsmTransformDomain
import net.akehurst.language.asmTransform.api.AsmTransformNamespace
import net.akehurst.language.asmTransform.api.AsmTransformRuleSet
import net.akehurst.language.asmTransform.api.AsmTransformationRule
import net.akehurst.language.asmTransform.asm.*

class AsmTransformSyntaxAnalyser(
) : SyntaxAnalyserByMethodRegistrationAbstract<AsmTransformDomain>() {

    override val extendsSyntaxAnalyser: Map<QualifiedName, SyntaxAnalyser<*>> = mapOf(
        QualifiedName("Base") to BaseSyntaxAnalyser()
    )

    override val embeddedSyntaxAnalyser: Map<QualifiedName, SyntaxAnalyser<*>> = mapOf(
        AglExpressions.defaultTargetGrammar.qualifiedName to ExpressionsSyntaxAnalyser()
    )

    override fun registerHandlers() {
        super.register(this::unit)
        super.register(this::namespace)
        //super.register(this::transformList)
        super.register(this::transform)
        super.register(this::extends)
        super.register(this::typeImport)
        //super.register(this::transformRuleList)
        super.register(this::transformRule)
        super.register(this::transformRuleRhs)
        super.register(this::expressionRule)
        super.register(this::modifyRule)
        super.register(this::assignmentStatement)
        super.register(this::propertyName)
        super.register(this::grammarRuleIndex)
        super.register(this::grammarRuleName)
        super.register(this::possiblyQualifiedTypeName)
        super.register(this::expression)
    }

    // override unit = option* namespace* ;
    private fun unit(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): AsmTransformDomain {
        val options = children[0] as List<Pair<String, String>>
        val namespaces = children[1] as List<AsmTransformNamespace>
        val name = SimpleName("ParsedTransformUnit") //TODO: how to specify name, does it matter?
        //val typeModel = TypeModelSimple(name) //TODO: how to specify type model ?
        // typeModel.addNamespace(SimpleTypeModelStdLib)
        val optHolder = OptionHolderDefault(null, options.associate { it })
        return AsmTransformDomainDefault(
            name = name,
            optHolder,
            namespaces
        )
    }

    // override namespace = 'namespace' possiblyQualifiedName option* import* transform* ;
    private fun namespace(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): AsmTransformNamespace {
        val pqn = children[1] as PossiblyQualifiedName
        val nsName = pqn.asQualifiedName(null)
        val options = children[2] as List<Pair<String, String>>
        val imports = children[3] as List<Import>
        val transformBuilders = children[4] as List<(AsmTransformNamespaceDefault) -> AsmTransformRuleSet>

        val optHolder = OptionHolderDefault(null, options.associate { it })
        val namespace = AsmTransformNamespaceDefault(nsName, optHolder, imports)
        transformBuilders.map { it.invoke(namespace) }
        return namespace
    }

    /*
    // transformList = transform+ ;
    private fun transformList(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<(TransformNamespace) -> TransformRuleSet> =
        children as List<(TransformNamespace) -> TransformRuleSet>
    */

    // transform = 'transform' IDENTIFIER extends? '{' option* typeImport* transformRule+ '} ;
    private fun transform(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (AsmTransformNamespace) -> AsmTransformRuleSet {
        val name = SimpleName(children[1] as String)
        val extends = children[2] as List<PossiblyQualifiedName>? ?: emptyList()
        val options = children[4] as List<Pair<String, String>>
        val typeImports = children[5] as List<Import>
        val rules = children[6] as List<AsmTransformationRule>

        val optHolder = OptionHolderDefault(null, options.associate { it })
        return { namespace ->
            val extendRefs = extends.map { AsmTransformRuleSetReferenceDefault(namespace, it) }
            val asm = AsmTransformRuleSetDefault(namespace, name, extendRefs, optHolder, rules)
            typeImports.forEach { asm.addImportType(it) }
            rules.forEach { asm.setRule(it) }
            asm.also { setLocationFor(it, nodeInfo, sentence) }
        }
    }

    // extends = ':' [possiblyQualifiedName / ',']+ ;
    private fun extends(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<PossiblyQualifiedName> =
        (children[1] as List<Any>).toSeparatedList<Any, PossiblyQualifiedName, String>().items

    // typeImport = 'import-types' possiblyQualifiedName ;
    private fun typeImport(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Import =
        Import( (children[1] as PossiblyQualifiedName).value )

    /*
    // transformRuleList = transformRule+ ;
    private fun transformRuleList(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<TransformationRule> =
        children as List<TransformationRule>
    */

    // transformRule = grammarRuleName ':' transformRuleRhs ;
    private fun transformRule(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): AsmTransformationRule {
        val grammarRuleName = children[0] as GrammarRuleName
        val trRule = children[2] as AsmTransformationRule
        trRule.grammarRuleName = grammarRuleName
        return trRule
    }

    // transformRuleRhs = expressionRule | modifyRule ;
    private fun transformRuleRhs(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): AsmTransformationRule =
        children[0] as AsmTransformationRule

    // expressionRule = expression ;
    private fun expressionRule(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): AsmTransformationRule {
        val expression = children[0] as Expression
        val tr = AsmTransformationRuleDefault(expression)
        return tr
    }

    // modifyRule = '{' possiblyQualifiedTypeName '->' statementList '}' ;
    private fun modifyRule(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): AsmTransformationRule {
        val possiblyQualifiedTypeName = children[1] as PossiblyQualifiedName
        val statements = children[3] as List<AssignmentStatement>
        val expr = OnExpressionDefault(RootExpressionDefault("\$it"))
        expr.propertyAssignments = statements
        val tr = AsmTransformationRuleDefault(expr)
        return tr
    }

    // assignmentStatement = propertyName grammarRuleIndex? ':=' expression ;
    private fun assignmentStatement(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): AssignmentStatementDefault {
        val propName = children[0] as String
        val grIndex = children[1] as Int?
        val expr = children[3] as Expression
        return AssignmentStatementDefault(propName, grIndex, expr)
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

    // grammarRuleIndex = '$' POSITIVE_INTEGER ;
    private fun grammarRuleIndex(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Int =
        (children[1] as String).toInt()

    // possiblyQualifiedTypeName = qualifiedName ;
    private fun possiblyQualifiedTypeName(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): PossiblyQualifiedName =
        children[0] as PossiblyQualifiedName
}