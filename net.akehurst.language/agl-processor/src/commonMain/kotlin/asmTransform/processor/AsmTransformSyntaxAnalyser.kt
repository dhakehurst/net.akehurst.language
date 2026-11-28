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
import net.akehurst.language.asmTransform.api.AsmTransformDomain
import net.akehurst.language.asmTransform.api.AsmTransformNamespace
import net.akehurst.language.asmTransform.api.AsmTransformRuleSet
import net.akehurst.language.asmTransform.api.AsmTransformationRule
import net.akehurst.language.asmTransform.asm.*
import net.akehurst.language.base.api.Import
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.asm.OptionHolderDefault
import net.akehurst.language.base.processor.BaseSyntaxAnalyser
import net.akehurst.kotlinx.collections.toSeparatedList
import net.akehurst.language.expressions.api.VariableAssignmentStatement
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.asm.VariableAssignmentStatementDefault
import net.akehurst.language.expressions.asm.OnExpressionDefault
import net.akehurst.language.expressions.asm.RootExpressionDefault
import net.akehurst.language.expressions.asm.VariableDefinitionDefault
import net.akehurst.language.expressions.processor.AglExpressions
import net.akehurst.language.expressions.processor.ExpressionsSyntaxAnalyser
import net.akehurst.language.grammar.api.GrammarRuleName
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sppt.treedata.locationForNode
import net.akehurst.language.sppt.api.SpptDataNodeInfo

class AsmTransformSyntaxAnalyser() : SyntaxAnalyserByMethodRegistrationAbstract<AsmTransformDomain>() {

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
        super.register(this::asmTransform)
        super.register(this::extends)
        super.register(this::typeImport)
        //super.register(this::transformRuleList)
        super.register(this::transformRule)
        super.register(this::transformRuleRhs)
        super.register(this::expressionRule)
        super.register(this::modifyRule)
        super.register(this::assignmentStatement)
        super.register(this::propertyName)
        super.register(this::grammarRuleName)
        super.register(this::possiblyQualifiedTypeName)
        super.register(this::expression)
    }

    /** an option that the asm-transform does not know is a warning, e.g. a misspelt option (AT-Q6) */
    private fun warnUnknownOptions(options: List<Pair<String, Any>>) {
        options.filter { it.first !in KNOWN_OPTIONS }.forEach { opt ->
            issues.warn(
                // the option node may include following skip text, so the location is that of '#name'
                locationMap[opt]?.let { InputLocation(it.position, it.column, it.line, 1 + opt.first.length, it.sentenceIdentity) },
                "Unknown option '#${opt.first}', the asm-transform options are ${KNOWN_OPTIONS.joinToString(separator = " and ") { "'#$it'" }}"
            )
        }
    }

    private companion object {
        val KNOWN_OPTIONS = listOf(AsmTransformSemanticAnalyser.OPTION_OVERRIDE_DEFAULT, AsmTransformSemanticAnalyser.OPTION_CREATE_TYPES)
    }

    // override unit = option* namespace* ;
    private fun unit(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): AsmTransformDomain {
        val options = children[0] as List<Pair<String, String>>
        warnUnknownOptions(options)
        val namespaces = children[1] as List<AsmTransformNamespace>
        val name = SimpleName("ParsedTransformUnit") //TODO: how to specify name, does it matter?
        //val typeModel = TypeModelSimple(name) //TODO: how to specify type model ?
        // typeModel.addNamespace(SimpleTypeModelStdLib)
        val optHolder = OptionHolderDefault(null, options.toMap())
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
        warnUnknownOptions(options)
        val imports = children[3] as List<Import>
        val transformBuilders = children[4] as List<(AsmTransformNamespaceDefault) -> AsmTransformRuleSet>

        val optHolder = OptionHolderDefault(null, options.toMap())
        val namespace = AsmTransformNamespaceDefault(nsName, optHolder, imports)
        transformBuilders.map { it.invoke(namespace) }
        return namespace
    }

    /*
    // transformList = transform+ ;
    private fun transformList(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<(TransformNamespace) -> TransformRuleSet> =
        children as List<(TransformNamespace) -> TransformRuleSet>
    */

    // transform = 'asm-transform' IDENTIFIER extends? '{' option* typeImport* transformRule+ '} ;
    private fun asmTransform(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (AsmTransformNamespace) -> AsmTransformRuleSet {
        val name = SimpleName(children[1] as String)
        val extends = children[2] as List<PossiblyQualifiedName>? ?: emptyList()
        val options = children[4] as List<Pair<String, String>>
        warnUnknownOptions(options)
        val typeImports = children[5] as List<Import>
        val rules = children[6] as List<AsmTransformationRule>

        val optHolder = OptionHolderDefault(null, options.toMap())
        return { namespace ->
            val extendRefs = extends.map { AsmTransformRuleSetReferenceDefault(namespace, it) }
            // only one rule for a grammar rule (AT-Q4): the first is used, each further one is an error
            val firstRules = rules.distinctBy { it.grammarRuleName }
            rules.filter { r -> firstRules.none { it === r } }.forEach { dup ->
                issues.error(
                    locationMap.getPropertyLocation(dup, "grammarRuleName"),
                    "Rule '${dup.grammarRuleName.value}' is already transformed in '${QualifiedName(namespace.qualifiedName, name).value}'"
                )
            }
            val asm = AsmTransformRuleSetDefault(namespace, name, extendRefs, optHolder, firstRules)
            typeImports.forEach { asm.addImportType(it) }
            firstRules.forEach { asm.setRule(it) }
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
        // the rule starts with the grammar rule name, so its location is the start of this node (used for issues about the name)
        val ruleLoc = sentence.locationForNode(nodeInfo.node)
        locationMap.addPropertyLocation(trRule, "grammarRuleName", InputLocation(ruleLoc.position, ruleLoc.column, ruleLoc.line, grammarRuleName.value.length, ruleLoc.sentenceIdentity))
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
        val statements = children[3] as List<VariableAssignmentStatement>
        return ModifyObjectRuleDefault(possiblyQualifiedTypeName, statements)
    }

    // assignmentStatement = propertyName ':=' expression ;
    private fun assignmentStatement(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): VariableAssignmentStatementDefault {
        val propName = children[0] as String
        val expr = children[2] as Expression
        val variable = VariableDefinitionDefault(propName, null) //TODO: variables?
        return VariableAssignmentStatementDefault(variable, expr)
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