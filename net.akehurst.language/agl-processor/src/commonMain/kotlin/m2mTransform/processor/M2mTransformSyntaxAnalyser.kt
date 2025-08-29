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
import net.akehurst.language.asmTransform.api.AsmTransformNamespace
import net.akehurst.language.asmTransform.api.AsmTransformRuleSet
import net.akehurst.language.asmTransform.api.AsmTransformationRule
import net.akehurst.language.asmTransform.asm.AsmTransformNamespaceDefault
import net.akehurst.language.asmTransform.asm.AsmTransformRuleSetDefault
import net.akehurst.language.asmTransform.asm.AsmTransformRuleSetReferenceDefault
import net.akehurst.language.base.api.Import
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.api.asPossiblyQualifiedName
import net.akehurst.language.base.asm.OptionHolderDefault
import net.akehurst.language.base.processor.BaseSyntaxAnalyser
import net.akehurst.language.collections.ListSeparated
import net.akehurst.language.collections.toSeparatedList
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.processor.AglExpressions
import net.akehurst.language.expressions.processor.ExpressionsSyntaxAnalyser
import net.akehurst.language.grammar.api.SeparatedList
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
        super.register(this::relDomain)
        super.register(this::mapDomain)
        super.register(this::variableDefinition)
        super.register(this::expression)
        super.registerFor("when",this::when_)
        super.register(this::where)

        super.register(this::objectPattern)
        super.register(this::propertyPattern)
        super.register(this::propertyPatternRhs)
        super.register(this::namedObjectPattern)
        super.register(this::typeName)
    }

    // override unit = option* namespace* ;
    private fun unit(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): M2mTransformDomain {
        val options = children[0] as List<Pair<String, String>>
        val namespaces = children[1] as List<M2mTransformNamespace>
        val name = SimpleName("ParsedTransformUnit") //TODO: how to specify name, does it matter?
        //val typeModel = TypeModelSimple(name) //TODO: how to specify type model ?
        // typeModel.addNamespace(SimpleTypeModelStdLib)
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
            val asm = M2mTransformRuleSetDefault(namespace, name, domParams.associate { Pair(DomainReference(it.first),it.second,) }, extendRefs, optHolder, rules)
            typeImports.forEach { asm.addImportType(it) }
            rules.forEach { asm.setRule(it) }
            asm.also { setLocationFor(it, nodeInfo, sentence) }
        }
    }

    // domainParams = [parameterDefinition / ',']2+ ;
    private fun domainParams(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<Pair<String, SimpleName>> =
        (children as List<Any>).toSeparatedList<Any,Pair<String, SimpleName>,String>().items

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

    // relation = 'abstract'? 'top'? 'relation' IDENTIFIER '{' pivot* relDomain{2+} when? where? '}' ;
    private fun relation(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): M2mRelation {
        val isAbstract = children[0] == "abstract"
        val isTop = children[1] == "top"
        val name = SimpleName(children[3] as String)
        val pivots = children[5] as List<VariableDefinition>
        val relDomains = children[6] as List<Pair<DomainItem, ObjectPattern>>
        val whenNode = children[7]
        val whereNode = children[8]
        return M2mRelationDefault(isAbstract, isTop, name).also { rel ->
            pivots.forEach { (rel.pivot as MutableMap)[it.name] = it }
            relDomains.forEach { rd ->
                (rel.domainItem as MutableMap)[rd.first.domainRef] = rd.first
                (rel.objectPattern as MutableMap)[rd.first.domainRef] = rd.second
            }
        }
    }

    // mapping = 'abstract'? 'top'? 'mapping' IDENTIFIER '{' mapDomain{2+} when? where? '}' ;
    private fun mapping(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): M2mMapping {
        val isAbstract = children[0] == "abstract"
        val isTop = children[1] == "top"
        val name = SimpleName(children[3] as String)
        val relDomains = children[5] as List<Pair<DomainItem, Expression?>>
        val whenNode = children[6]
        val whereNode = children[7]
        return M2mMappingDefault(isAbstract, isTop, name).also {
            relDomains.forEach { rd ->
                (it.domainItem as MutableMap)[rd.first.domainRef] = rd.first
                (it.expression as MutableMap)[rd.first.domainRef] = rd.second
            }
        }
    }

    // pivot = 'pivot' variableDefinition ;
    private fun pivot(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): VariableDefinition {
        val vd = children[1] as VariableDefinition
        return vd
    }

    // relDomain = 'domain' IDENTIFIER IDENTIFIER ':' objectPattern
    private fun relDomain(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Pair<DomainItem, ObjectPattern> {
        val dn = children[1] as String
        val id = children[2] as String
        val pat = children[4] as ObjectPattern
        val vd = VariableDefinitionDefault(SimpleName(id), (pat as ObjectPatternDefault).typeRef)
        val di = DomainItemDefault(DomainReference(dn), vd)
        return Pair(di, pat)
    }

    // mapDomain = 'domain' IDENTIFIER variableDefinition (':=' expression)?
    private fun mapDomain(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Pair<DomainItem, Expression?> {
        val dn = children[1] as String
        val vd = children[2] as VariableDefinition
        val expr = (children[3] as? List<Any>)?.getOrNull(1) as? Expression

        val di = DomainItemDefault(DomainReference(dn), vd)
        return Pair(di, expr)
    }

    // variableDefinition = IDENTIFIER ':' typeName ;
    private fun variableDefinition(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): VariableDefinition {
        val name = children[0] as String
        val typeRef = children[2] as PossiblyQualifiedName
        return VariableDefinitionDefault(SimpleName(name), typeRef)
    }

    // leaf TYPE_NAME = IDENTIFIER ;
    // expression = Expressions::expression ;
    private fun expression(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Expression =
        children[0] as Expression

    // when = 'when' '{' expression '}' ;
    private fun when_(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Expression =
        children[2] as Expression

    // where = 'where' '{' expression '}' ;
    private fun where(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Expression =
        children[2] as Expression

    // objectPattern = typeName '{' propertyPattern*  '}';
    private fun objectPattern(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): ObjectPattern {
        val typeRef = children[0] as PossiblyQualifiedName
        val propPats = children[2] as List<PropertyPattern>
        return ObjectPatternDefault(typeRef, propPats.associateBy { it.propertyName })
    }

    // propertyPattern = IDENTIFIER '=' propertyPatternRhs ;
    private fun propertyPattern(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): PropertyPattern {
        val id = children[0] as String
        val rhs = children[2] as PropertyPatternRhs
        return PropertyPatternDefault(SimpleName(id), rhs)
    }

    // propertyPatternRhs = expression | namedObjectPattern ;
    private fun propertyPatternRhs(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): PropertyPatternRhs =
        when(nodeInfo.alt.option) {
            OptionNum(0) -> PropertyPatternExpressionDefault(children[0] as Expression)
            OptionNum(1) -> children[0] as ObjectPattern
            else -> error("Invalid state")
        }

    // namedObjectPattern = (IDENTIFIER ':')? objectPattern ;
    private fun namedObjectPattern(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): ObjectPattern {
        val id = children[0] as String
        val op = children[1] as ObjectPattern
        return op.also {
            it.setIdentifier(SimpleName(id))
        }
    }

    private fun typeName(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): PossiblyQualifiedName =
        children[0] as PossiblyQualifiedName
}