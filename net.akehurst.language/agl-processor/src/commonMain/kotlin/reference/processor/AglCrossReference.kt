/**
 * Copyright (C) 2021 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.akehurst.language.reference.processor

import net.akehurst.language.completion.builder.completionDomain
import net.akehurst.language.completion.api.AglCompletionDomain
import net.akehurst.language.agl.format.builder.formatDomain
import net.akehurst.language.agl.processor.contextFromLanguageObject
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.api.processor.LanguageIdentity
import net.akehurst.language.api.processor.LanguageObjectAbstract
import net.akehurst.language.asmTransform.builder.asmTransform
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.processor.AglBase
import net.akehurst.language.expressions.processor.AglExpressions
import net.akehurst.language.grammar.api.OverrideKind
import net.akehurst.language.grammar.builder.grammarDomain
import net.akehurst.language.grammar.processor.contextFromGrammar
import net.akehurst.language.reference.api.CrossReferenceDomain
import net.akehurst.language.reference.builder.crossReferenceDomain
import net.akehurst.language.style.builder.styleDomain
import net.akehurst.language.style.processor.AglStyle
import net.akehurst.language.types.builder.typesDomain

object AglCrossReference : LanguageObjectAbstract<CrossReferenceDomain, SentenceContext>() {
    const val NAMESPACE_NAME = AglBase.NAMESPACE_NAME
    const val NAME = "CrossReferences"
    const val goalRuleName = "unit"

    override val identity = LanguageIdentity("${NAMESPACE_NAME}.${NAME}")
    override val extends by lazy { listOf(AglBase) }

    override val grammarString = """
        namespace $NAMESPACE_NAME

            grammar $NAME : Expressions {
            
                override namespace = 'namespace' possiblyQualifiedName import* declarations ;
                declarations = rootIdentifiables scopes references? ;
                rootIdentifiables = identifiable* ;
                scopes = scope* ;
                scope = 'scope' possiblyQualifiedTypeReference '{' identifiables scopeImports '}' ;
                identifiables = identifiable* ;
                identifiable = 'identify' possiblyQualifiedTypeReference 'by' identifyingExpressions visibility? aliasFor? ;
                identifyingExpressions = [expression / '|']+ ;
                visibility = 'visibility' expression ;
                aliasFor = 'alias-for' rootOrNavigation ;
                scopeImports = scopeImport* ;
                scopeImport = importDefinition | inheritDefinition ;
                importDefinition = 'import' 'forall' rootOrNavigation ofType? importKind rootOrNavigation recursive? visibility? ;
                importKind = 'members-of' | 'element' ;
                recursive = 'recursive' expression ;
                inheritDefinition = 'inherit' 'forall' rootOrNavigation ofType? 'members-of' rootOrNavigation ;
            
                references = 'references' '{' referenceDefinitions '}' ;
                referenceDefinitions = referenceDefinition* ;
                referenceDefinition = 'in' possiblyQualifiedTypeReference '{' referenceExpressionList '}' ;
                referenceExpressionList = referenceExpression* ;
                referenceExpression = propertyReferenceExpression | collectionReferenceExpression ;
                propertyReferenceExpression = 'property' rootOrNavigation 'refers-to' typeReferences from? condition? ;
                condition = 'when' expression ;
                from = 'from' rootOrNavigation fromElse? ;
                fromElse = 'else' rootOrNavigation ;
                collectionReferenceExpression = 'forall' rootOrNavigation ofType? '{' referenceExpressionList '}' ;
                ofType = 'of-type' possiblyQualifiedTypeReference ;
                
                rootOrNavigation = rootExpression | navigationExpression ;
                
                typeReferences = [possiblyQualifiedTypeReference / '|']+ ;
                possiblyQualifiedTypeReference = possiblyQualifiedName ;
                simpleTypeName = IDENTIFIER ;
            }
    """.trimIndent()

    override val typesString: String = """
    """.trimIndent()

    override val kompositeString = """
        namespace net.akehurst.language.reference.api
            interface DeclarationsForNamespace {
                cmp scopeDefinition
                cmp references
                cmp options
            }
            
            interface ScopeDefinition {
                cmp identifiables
            }
            interface Identifiable {
                cmp identifiedBy
            }
            interface ReferenceDefinition {
                cmp referenceExpressionList
            }
            interface ReferenceExpressionProperty {
                cmp referringPropertyNavigation
                cmp fromNavigation
                cmp fromElseNavigation
            }
            interface ReferenceExpressionCollection {
                cmp expression
                cmp referenceExpressionList
            }
        """.trimIndent()

    override val asmTransformString: String = """
        namespace ${NAMESPACE_NAME}
          // TODO
    """.trimIndent()

    override val crossReferenceString = """
        namespace ${NAMESPACE_NAME}
           // TODO
    """.trimIndent()

    override val styleString = """
        namespace ${NAMESPACE_NAME}
          styles ${NAME} : ${AglExpressions.NAME} {

          }
        """.trimIndent()

    override val formatString: String = """
        namespace ${NAMESPACE_NAME}.reference.api
          // TODO
    """.trimIndent()

    override val completionString: String = """
        namespace $NAMESPACE_NAME
          // TODO
    """.trimIndent()

    override val grammarDomain by lazy {
        grammarDomain(NAME) {
            namespace(NAMESPACE_NAME) {
                grammar(NAME) {
                    extendsGrammar(AglExpressions.defaultTargetGrammar.selfReference)
                    concatenation("namespace", overrideKind = OverrideKind.REPLACE) {
                        lit("namespace"); ref("possiblyQualifiedName")
                        lst(0, -1) { ref("import") }
                        ref("declarations")
                    }
                    concatenation("declarations") {
                        ref("rootIdentifiables"); ref("scopes"); opt { ref("references") }
                    }
                    list("rootIdentifiables", 0, -1) { ref("identifiable") }
                    list("scopes", 0, -1) { ref("scope") }
                    concatenation("scope") {
                        lit("scope"); ref("possiblyQualifiedTypeReference"); lit("{"); ref("identifiables"); ref("scopeImports"); lit("}")
                    }
                    list("identifiables", 0, -1) { ref("identifiable") }
                    concatenation("identifiable") {
                        lit("identify"); ref("possiblyQualifiedTypeReference"); lit("by"); ref("identifyingExpressions"); opt { ref("visibility") }; opt { ref("aliasFor") }
                    }
                    separatedList("identifyingExpressions", 1, -1) { ref("expression"); lit("|") }
                    concatenation("visibility") { lit("visibility"); ref("expression") }
                    concatenation("aliasFor") { lit("alias-for"); ref("rootOrNavigation") }
                    list("scopeImports", 0, -1) { ref("scopeImport") }
                    choice("scopeImport") {
                        ref("importDefinition")
                        ref("inheritDefinition")
                    }
                    concatenation("importDefinition") {
                        lit("import"); lit("forall"); ref("rootOrNavigation"); opt { ref("ofType") }; ref("importKind"); ref("rootOrNavigation"); opt { ref("recursive") }; opt { ref("visibility") }
                    }
                    choice("importKind") {
                        lit("members-of")
                        lit("element")
                    }
                    concatenation("recursive") { lit("recursive"); ref("expression") }
                    concatenation("inheritDefinition") {
                        lit("inherit"); lit("forall"); ref("rootOrNavigation"); opt { ref("ofType") }; lit("members-of"); ref("rootOrNavigation")
                    }
                    concatenation("references") {
                        lit("references"); lit("{"); ref("referenceDefinitions"); lit("}")
                    }
                    list("referenceDefinitions", 0, -1) { ref("referenceDefinition") }
                    concatenation("referenceDefinition") {
                        lit("in"); ref("possiblyQualifiedTypeReference"); lit("{"); ref("referenceExpressionList"); lit("}")
                    }
                    list("referenceExpressionList", 0, -1) { ref("referenceExpression") }
                    choice("referenceExpression") {
                        ref("propertyReferenceExpression")
                        ref("collectionReferenceExpression")
                    }
                    concatenation("propertyReferenceExpression") {
                        lit("property"); ref("rootOrNavigation"); lit("refers-to"); ref("typeReferences"); opt { ref("from") }; opt { ref("condition") }
                    }
                    concatenation("condition") { lit("when"); ref("expression") }
                    concatenation("from") { lit("from"); ref("rootOrNavigation"); opt { ref("fromElse") } }
                    concatenation("fromElse") { lit("else"); ref("rootOrNavigation") }
                    concatenation("collectionReferenceExpression") {
                        lit("forall"); ref("rootOrNavigation"); opt { ref("ofType") }; lit("{"); ref("referenceExpressionList"); lit("}")
                    }
                    concatenation("ofType") { lit("of-type"); ref("possiblyQualifiedTypeReference") }
                    choice("rootOrNavigation") {
                        ref("rootExpression")
                        ref("navigationExpression")
                    }
                    separatedList("typeReferences", 1, -1) { ref("possiblyQualifiedTypeReference"); lit("|") }
                    concatenation("possiblyQualifiedTypeReference") { ref("possiblyQualifiedName") }
                    concatenation("simpleTypeName") { ref("IDENTIFIER") }
                }
            }
        }
    }

    override val typesDomain by lazy {
        typesDomain(NAME, true, AglExpressions.typesDomain.namespace) {
            namespace("net.akehurst.language.reference.api", listOf("net.akehurst.language.base.api", "std", "net.akehurst.language.expressions.api")) {
                interface_("ScopeDefinition") {
                    supertype("Formatable")
                    propertyOf(setOf(VAR, CMP, STR), "identifiables", "List", false){
                        typeArgument("Identifiable")
                    }
                }
                interface_("ReferenceExpressionProperty") {
                    supertype("ReferenceExpression")
                    propertyOf(setOf(VAL, CMP, STR), "fromNavigation", "NavigationExpression", false)
                    propertyOf(setOf(VAL, CMP, STR), "fromElseNavigation", "NavigationExpression", true)
                    propertyOf(setOf(VAL, CMP, STR), "referringPropertyNavigation", "NavigationExpression", false)
                }
                interface_("ReferenceExpressionCollection") {
                    supertype("ReferenceExpression")
                    propertyOf(setOf(VAL, CMP, STR), "expression", "Expression", false)
                    propertyOf(setOf(VAR, CMP, STR), "referenceExpressionList", "List", false){
                        typeArgument("ReferenceExpression")
                    }
                }
                interface_("ReferenceExpression") {
                    supertype("Formatable")
                }
                interface_("ReferenceDefinition") {
                    supertype("Formatable")
                    propertyOf(setOf(VAR, CMP, STR), "referenceExpressionList", "List", false){
                        typeArgument("ReferenceExpression")
                    }
                }
                interface_("Identifiable") {
                    supertype("Formatable")
                    propertyOf(setOf(VAL, CMP, STR), "identifiedBy", "Expression", false)
                }
                interface_("DeclarationsForNamespace") {
                    supertype("Definition"){ ref("DeclarationsForNamespace") }
                    propertyOf(setOf(VAR, CMP, STR), "references", "List", false){
                        typeArgument("ReferenceDefinition")
                    }
                    propertyOf(setOf(VAR, CMP, STR), "scopeDefinition", "Map", false){
                        typeArgument("PossiblyQualifiedName")
                        typeArgument("ScopeDefinition")
                    }
                }
                interface_("CrossReferenceNamespace") {
                    supertype("Namespace"){ ref("DeclarationsForNamespace") }
                }
                interface_("CrossReferenceDomain") {
                    supertype("Domain"){ ref("CrossReferenceNamespace"); ref("DeclarationsForNamespace") }
                }
            }
            namespace("net.akehurst.language.reference.asm", listOf("net.akehurst.language.reference.api", "std", "net.akehurst.language.base.api", "net.akehurst.language.expressions.api", "net.akehurst.language.base.asm")) {
                data("ScopeDefinitionDefault") {
                    supertype("ScopeDefinition")
                    constructor_ {
                        parameter(setOf(), "scopeForTypeName", "PossiblyQualifiedName")
                    }
                    propertyOf(setOf(VAR, CMP, STR), "identifiables", "List", false){
                        typeArgument("Identifiable")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "scopeForTypeName", "PossiblyQualifiedName", false)
                }
                data("ReferenceExpressionPropertyDefault") {
                    supertype("ReferenceExpressionAbstract")
                    supertype("ReferenceExpressionProperty")
                    constructor_ {
                        parameter(setOf(), "referringPropertyNavigation", "NavigationExpression")
                        parameter(setOf(), "refersToTypeName", "List")
                        parameter(setOf(), "fromNavigation", "NavigationExpression")
                        parameter(setOf(), "condition", "Expression")
                        parameter(setOf(), "fromElseNavigation", "NavigationExpression")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "condition", "Expression", true)
                    propertyOf(setOf(VAL, CMP, STR), "fromNavigation", "NavigationExpression", false)
                    propertyOf(setOf(VAL, CMP, STR), "fromElseNavigation", "NavigationExpression", true)
                    propertyOf(setOf(VAL, CMP, STR), "referringPropertyNavigation", "NavigationExpression", false)
                    propertyOf(setOf(VAR, REF, STR), "refersToTypeName", "List", false){
                        typeArgument("PossiblyQualifiedName")
                    }
                }
                data("ReferenceExpressionCollectionDefault") {
                    supertype("ReferenceExpressionAbstract")
                    supertype("ReferenceExpressionCollection")
                    constructor_ {
                        parameter(setOf(), "expression", "Expression")
                        parameter(setOf(), "ofType", "PossiblyQualifiedName")
                        parameter(setOf(), "referenceExpressionList", "List")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "expression", "Expression", false)
                    propertyOf(setOf(VAL, REF, STR), "ofType", "PossiblyQualifiedName", false)
                    propertyOf(setOf(VAR, CMP, STR), "referenceExpressionList", "List", false){
                        typeArgument("ReferenceExpressionAbstract")
                    }
                }
                data("ReferenceExpressionAbstract") {
                    supertype("ReferenceExpression")
                    constructor_ {}
                }
                data("ReferenceDefinitionDefault") {
                    supertype("ReferenceDefinition")
                    constructor_ {
                        parameter(setOf(), "inTypeName", "PossiblyQualifiedName")
                        parameter(setOf(), "referenceExpressionList", "List")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "inTypeName", "PossiblyQualifiedName", false)
                    propertyOf(setOf(VAR, CMP, STR), "referenceExpressionList", "List", false){
                        typeArgument("ReferenceExpression")
                    }
                }
                data("IdentifiableDefault") {
                    supertype("Identifiable")
                    constructor_ {
                        parameter(setOf(), "typeName", "PossiblyQualifiedName")
                        parameter(setOf(), "identifiedBy", "Expression")
                        parameter(setOf(), "visibility", "Expression")
                        parameter(setOf(), "aliasFor", "NavigationExpression")
                        parameter(setOf(), "identifiedByAlternatives", "List")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "aliasFor", "NavigationExpression", true)
                    propertyOf(setOf(VAL, CMP, STR), "identifiedBy", "Expression", false)
                    propertyOf(setOf(VAL, CMP, STR), "identifiedByAlternatives", "List", false){
                        typeArgument("Expression")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "typeName", "PossiblyQualifiedName", false)
                    propertyOf(setOf(VAL, CMP, STR), "visibility", "Expression", true)
                }
                data("DeclarationsForNamespaceDefault") {
                    supertype("DeclarationsForNamespace")
                    supertype("DefinitionAbstract"){ ref("net.akehurst.language.reference.api.DeclarationsForNamespace") }
                    constructor_ {
                        parameter(setOf(), "namespace", "CrossReferenceNamespace")
                        parameter(setOf(), "options", "OptionHolder")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "name", "SimpleName", false)
                    propertyOf(setOf(VAL, REF, STR), "namespace", "CrossReferenceNamespace", false)
                    propertyOf(setOf(VAL, CMP, STR), "options", "OptionHolder", false)
                    propertyOf(setOf(VAR, CMP, STR), "references", "List", false){
                        typeArgument("ReferenceDefinition")
                    }
                    propertyOf(setOf(VAR, CMP, STR), "scopeDefinition", "Map", false){
                        typeArgument("PossiblyQualifiedName")
                        typeArgument("ScopeDefinition")
                    }
                }
                data("CrossReferenceNamespaceDefault") {
                    supertype("NamespaceAbstract"){ ref("net.akehurst.language.reference.api.DeclarationsForNamespace") }
                    supertype("CrossReferenceNamespace")
                    constructor_ {
                        parameter(setOf(), "qualifiedName", "QualifiedName")
                        parameter(setOf(), "options", "OptionHolder")
                        parameter(setOf(), "import", "List")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "qualifiedName", "QualifiedName", false)
                }
                data("CrossReferenceDomainDefault") {
                    supertype("DomainAbstract"){ ref("net.akehurst.language.reference.api.CrossReferenceNamespace"); ref("net.akehurst.language.reference.api.DeclarationsForNamespace") }
                    supertype("CrossReferenceDomain")
                    constructor_ {
                        parameter(setOf(), "name", "SimpleName")
                        parameter(setOf(), "options", "OptionHolder")
                        parameter(setOf(), "namespace", "List")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "name", "SimpleName", false)
                }
            }
        }
    }

    override val asmTransformDomain by lazy  {
        asmTransform(name = NAME, typesDomain = typesDomain, createTypes = false) {
            namespace(qualifiedName = NAMESPACE_NAME) {
                ruleSet(NAME) {
                    //TODO
                }
            }
        }
    }

    override val crossReferenceDomain by lazy {
        crossReferenceDomain(NAME) {
            //TODO
        }
    }

    override val styleDomain by lazy {
        styleDomain(NAME,  sentenceContext = contextFromGrammar(AglStyle.grammarDomain).union(contextFromLanguageObject(listOf(AglBase)))) {
            namespace(NAMESPACE_NAME) {
                styles(NAME) {
                    extends(AglExpressions.NAME)
                }
            }
        }
    }

    override val formatDomain by lazy {
        formatDomain(NAME) {
//            TODO("not implemented")
        }
    }

    override val completionDomain: AglCompletionDomain by lazy {
        completionDomain(NAME) {
        }
    }

    override val defaultTargetGrammar by lazy { grammarDomain.findDefinitionByQualifiedNameOrNull(QualifiedName("${NAMESPACE_NAME}.${NAME}"))!! }
    override val defaultTargetGoalRule = "unit"

    override val syntaxAnalyser by lazy { ReferencesSyntaxAnalyser() }
    override val semanticAnalyser by lazy { ReferencesSemanticAnalyser() }
    override val completionProvider by lazy { ReferencesCompletionProvider() }


    const val formatterStr = """
@TODO
References -> when {
    referenceDefinitions.isEmpty -> "references { }"
    else -> "
      references {
        referenceDefinitions
      }
    "
}
ReferenceDefinitions -> [referenceDefinition / '\n']
ReferenceDefinition -> "in §typeReference property §propertyReference refers-to §typeReferences"
    """

}