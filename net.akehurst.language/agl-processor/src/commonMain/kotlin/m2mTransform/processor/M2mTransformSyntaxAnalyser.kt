/*
 * Copyright (C) 2025 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.m2mTransform.processor

import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserByMethodRegistrationAbstract
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.base.api.Import
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.asm.OptionHolderDefault
import net.akehurst.language.base.processor.BaseSyntaxAnalyser
import net.akehurst.language.collections.toSeparatedList
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.api.TypeReference
import net.akehurst.language.expressions.processor.AglExpressions
import net.akehurst.language.expressions.processor.ExpressionsSyntaxAnalyser
import net.akehurst.language.m2mTransform.api.*
import net.akehurst.language.m2mTransform.asm.*
import net.akehurst.language.parser.api.OptionNum
import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sppt.api.SpptDataNodeInfo

class M2mTransformSyntaxAnalyser : SyntaxAnalyserByMethodRegistrationAbstract<M2mTransformDomain>() {

    override val extendsSyntaxAnalyser: Map<QualifiedName, SyntaxAnalyser<*>> = mapOf(
        QualifiedName("Base") to BaseSyntaxAnalyser()
    )

    override val embeddedSyntaxAnalyser: Map<QualifiedName, SyntaxAnalyser<*>> = mapOf(
        AglExpressions.defaultTargetGrammar.qualifiedName to ExpressionsSyntaxAnalyser()
    )

    override fun registerHandlers() {
        super.register(this::unit)
        super.register(this::namespace)
        super.register(this::transform)
        super.register(this::domainParams)
        super.register(this::parameterDefinition)
        super.register(this::extends)
        //super.register(this::import)
        super.register(this::typeImport)
        super.register(this::transformRule)
        super.register(this::relation)
        super.register(this::mapping)
        super.register(this::pivot)
        super.register(this::domainPrimitive)
        super.register(this::domainObjectPattern)
        super.register(this::domainAssignment)
        super.register(this::variableDefinition)
        super.register(this::expression)
        super.register(this::typeReference)
        super.registerFor("when", this::when_)
        super.register(this::where)

        super.register(this::objectPattern)
        super.register(this::propertyPattern)
        super.register(this::propertyName)
        super.register(this::propertyPatternRhs)
        super.register(this::namedObjectPattern)
        super.register(this::variableName)

        super.register(this::testUnit)
        super.register(this::testNamespace)
        super.register(this::transformTest)
        super.register(this::testDomain)
    }

    // override unit = option* namespace* ;
    private fun unit(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): M2mTransformDomain {
        val options = children[0] as List<Pair<String, String>>
        val namespaces = children[1] as List<M2mTransformNamespace>
        val name = SimpleName("ParsedTransformUnit") //TODO: how to specify name, does it matter?
        val optHolder = OptionHolderDefault(null, options.associate { it })
        return M2mTransformDomainDefault(
            name = name,
            optHolder,
            namespaces
        )
    }

    // override namespace = 'namespace' possiblyQualifiedName option* import* transform* ;
    private fun namespace(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): M2mTransformNamespace {
        val pqn = children[1] as PossiblyQualifiedName
        val nsName = pqn.asQualifiedName(null)
        val options = children[2] as List<Pair<String, String>>
        val imports = children[3] as List<Import>
        val transformBuilders = children[4] as List<(M2mTransformNamespaceDefault) -> M2mTransformRuleSet>

        val optHolder = OptionHolderDefault(null, options.associate { it })
        val namespace = M2mTransformNamespaceDefault(nsName, optHolder, imports)
        transformBuilders.map { it.invoke(namespace) }
        return namespace
    }

    //  transform = 'transform' IDENTIFIER '(' domainParams ')' extends? '{' option* typeImport* transformRule* '} ;
    private fun transform(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (M2mTransformNamespace) -> M2mTransformRuleSet {
        val name = SimpleName(children[1] as String)
        val domParams = children[3] as List<Pair<String, SimpleName>>
        val extends = children[5] as List<PossiblyQualifiedName>? ?: emptyList()
        val options = children[7] as List<Pair<String, String>>
        val typeImports = children[8] as List<Import>
        val rules = children[9] as List<M2mTransformRule>

        val optHolder = OptionHolderDefault(null, options.associate { it })
        return { namespace ->
            val extendRefs = extends.map { M2mTransformRuleSetReferenceDefault(namespace, it) }
            val asm = M2mTransformRuleSetDefault(namespace, name, domParams.associate { Pair(DomainReference(it.first), it.second) }, extendRefs, optHolder, rules)
            typeImports.forEach { asm.addImportType(it) }
            rules.forEach { asm.setRule(it) }
            asm.also { setLocationFor(it, nodeInfo, sentence) }
        }
    }

    // domainParams = [parameterDefinition / ',']2+ ;
    private fun domainParams(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<Pair<String, SimpleName>> =
        (children as List<Any>).toSeparatedList<Any, Pair<String, SimpleName>, String>().items

    // parameterDefinition = IDENTIFIER ':' DOMAIN_NAME ;
    private fun parameterDefinition(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Pair<String, SimpleName> {
        val id = children[0] as String
        val dn = children[2] as String
        return Pair(id, SimpleName(dn))
    }

    //  leaf DOMAIN_NAME = IDENTIFIER ;
    // extends = ':' [possiblyQualifiedName / ',']+ ;
    private fun extends(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<PossiblyQualifiedName> =
        (children[1] as List<Any>).toSeparatedList<Any, PossiblyQualifiedName, String>().items

    // option = 'option' IDENTIFIER '=' expression ;
    // import = 'import' qualifiedName ;
    //  typeImport = 'import-types' possiblyQualifiedName ;
    private fun typeImport(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Import =
        Import((children[1] as PossiblyQualifiedName).value)

    // transformRule = relation | mapping ;
    private fun transformRule(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): M2mTransformRule =
        children[0] as M2mTransformRule

    // relation = 'abstract'? 'top'? 'relation' IDENTIFIER '{' pivot* domainPrimitive* relDomain{2+} when? where? '}' ;
    private fun relation(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): M2mRelation {
        val isAbstract = children[0] == "abstract"
        val isTop = children[1] == "top"
        val name = SimpleName(children[3] as String)
        val pivots = children[5] as List<VariableDefinition>
        val primDomains = children[6] as List<VariableDefinition>
        val relDomains = children[7] as List<Pair<DomainItem, ObjectPattern>>
        val whenNode = children[8]
        val whereNode = children[9]
        return M2mRelationDefault(isAbstract, isTop, name).also { rel ->
            pivots.forEach { (rel.pivot as MutableMap)[it.name] = it }
            (rel.primitiveDomains as MutableList).addAll(primDomains)
            relDomains.forEach { rd ->
                (rel.domainItem as MutableMap)[rd.first.domainRef] = rd.first
                (rel.objectPattern as MutableMap)[rd.first.domainRef] = rd.second
            }
        }
    }

    // mapping = 'abstract'? 'top'? 'mapping' IDENTIFIER '{' domainPrimitive* domainObjectPattern+ domainAssignment when? where? '}' ;
    private fun mapping(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): M2mMapping {
        val isAbstract = children[0] == "abstract"
        val isTop = children[1] == "top"
        val name = SimpleName(children[3] as String)
        val primDomains = children[5] as List<VariableDefinition>
        val inputDomains = children[6] as List<Pair<DomainItem, Expression?>>
        val outputDomain = children[7] as Pair<DomainItem, Expression?>
        val whenNode = children[8]
        val whereNode = children[9]
        return M2mMappingDefault(isAbstract, isTop, name).also {
            (it.primitiveDomains as MutableList).addAll(primDomains)
            inputDomains.forEach { rd ->
                (it.domainItem as MutableMap)[rd.first.domainRef] = rd.first
                (it.expression as MutableMap)[rd.first.domainRef] = rd.second
            }
            (it.domainItem as MutableMap)[outputDomain.first.domainRef] = outputDomain.first
            (it.expression as MutableMap)[outputDomain.first.domainRef] = outputDomain.second
        }
    }

    // pivot = 'pivot' variableDefinition ;
    private fun pivot(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): VariableDefinition {
        val vd = children[1] as VariableDefinition
        return vd
    }

    // domainPrimitive = 'primitive 'domain' variableDefinition ;
    private fun domainPrimitive(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): VariableDefinition {
        return children[2] as VariableDefinition
    }

    // domainObjectPattern = 'domain' IDENTIFIER IDENTIFIER ':' objectPattern
    private fun domainObjectPattern(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Pair<DomainItem, ObjectPattern> {
        val dn = children[1] as String
        val id = children[2] as String
        val pat = children[4] as ObjectPattern
        val vd = VariableDefinitionDefault(SimpleName(id), (pat as ObjectPatternDefault).typeRef)
        val di = DomainItemDefault(DomainReference(dn), vd)
        return Pair(di, pat)
    }

    // domainAssignment = 'domain' domainReference variableDefinition ':=' expression ;
    private fun domainAssignment(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Pair<DomainItem, Expression?> {
        val dn = children[1] as String
        val vd = children[2] as VariableDefinition
        val expr = children[4] as Expression

        val di = DomainItemDefault(DomainReference(dn), vd)
        return Pair(di, expr)
    }

    // variableDefinition = IDENTIFIER ':' typeReference ;
    private fun variableDefinition(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): VariableDefinition {
        val name = children[0] as String
        val typeRef = children[2] as TypeReference
        return VariableDefinitionDefault(SimpleName(name), typeRef)
    }

    // expression = Expressions::expression ;
    private fun expression(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Expression =
        children[0] as Expression

    // typeReference = Expressions::typeReference ;
    private fun typeReference(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): TypeReference =
        children[0] as TypeReference

    // when = 'when' '{' expression '}' ;
    private fun when_(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Expression =
        children[2] as Expression

    // where = 'where' '{' expression '}' ;
    private fun where(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Expression =
        children[2] as Expression

    // objectPattern = typeReference '{' propertyPattern*  '}';
    private fun objectPattern(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): ObjectPattern {
        val typeRef = children[0] as TypeReference
        val propPats = children[2] as List<PropertyPattern>
        return ObjectPatternDefault(typeRef, propPats.associateBy { it.propertyName })
    }

    // propertyPattern = propertyName '==' propertyPatternRhs ;
    private fun propertyPattern(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): PropertyPattern {
        val id = children[0] as String
        val rhs = children[2] as PropertyPatternRhs
        return PropertyPatternDefault(SimpleName(id), rhs)
    }

    // propertyName = IDENTIFIER ;
    private fun propertyName(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): String =
        children[0] as String

    // propertyPatternRhs = expression | namedObjectPattern ;
    private fun propertyPatternRhs(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): PropertyPatternRhs =
        when (nodeInfo.alt.option) {
            OptionNum(0) -> PropertyPatternExpressionDefault(children[0] as Expression)
            OptionNum(1) -> children[0] as ObjectPattern
            else -> error("Invalid state")
        }

    // namedObjectPattern = (variableName ':')? objectPattern ;
    private fun namedObjectPattern(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): ObjectPattern {
        val id = (children[0] as? List<Any> )?.getOrNull(0) as? String
        val op = children[1] as ObjectPattern
        return op.apply {
            id?.let { setIdentifier(SimpleName(id)) }
        }
    }

    // variableName := IDENTIFIER ;
    private fun variableName(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): String {
        return children[0] as String
    }

    // testUnit = option* import* testNamespace* ;
    private fun testUnit(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): M2mTransformDomain {
        val options = children[0] as List<Pair<String, String>>
        val namespaces = children[1] as List<M2mTransformNamespace>
        val name = SimpleName("ParsedTransformTestUnit") //TODO: how to specify name, does it matter?
        val optHolder = OptionHolderDefault(null, options.associate { it })
        return M2mTransformDomainDefault(
            name = name,
            optHolder,
            namespaces
        )
    }

    // testNamespace = 'namespace' possiblyQualifiedName option* import* transformTest* ;
    private fun testNamespace(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): M2mTransformNamespace {
        val pqn = children[1] as PossiblyQualifiedName
        val nsName = pqn.asQualifiedName(null)
        val options = children[2] as List<Pair<String, String>>
        val imports = children[3] as List<Import>
        val transformBuilders = children[4] as List<(M2mTransformNamespaceDefault) -> M2mTransformTest>

        val optHolder = OptionHolderDefault(null, options.associate { it })
        val namespace = M2mTransformNamespaceDefault(nsName, optHolder, imports)
        transformBuilders.map { it.invoke(namespace) }
        return namespace
    }

    //     transformTest = 'transform-test' IDENTIFIER '(' domainParams ')' '{'
    //       testDomain2+
    //    '}' ;
    private fun transformTest(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (M2mTransformNamespaceDefault) -> M2mTransformTest {
        val id = children[1] as String
        val domParams = children[3] as List<Pair<String, SimpleName>>
        val testDomains = children[6] as List<Pair<String, Expression>>
        return { namespace ->
            M2MTransformTestDefault(
                namespace = namespace,
                name = SimpleName(id),
                domainParameters = domParams.associate { Pair(DomainReference(it.first), it.second) }
            ).apply {
                testDomains.forEach { (dr, exp) ->
                    (domain as MutableMap)[DomainReference(dr)] = exp
                }
            }
        }
    }

    // testDomain := 'domain' IDENTIFIER ':=' expression ;
    private fun testDomain(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Pair<String, Expression> {
        val id = children[1] as String
        val exp = children[3] as Expression
        return Pair(id, exp)
    }
}