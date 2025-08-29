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

import net.akehurst.language.api.processor.LanguageIdentity
import net.akehurst.language.base.processor.AglBase
import net.akehurst.language.expressions.processor.AglExpressions
import net.akehurst.language.grammar.api.OverrideKind
import net.akehurst.language.grammar.builder.grammar
import net.akehurst.language.types.builder.typesDomain

object AglCrossReference {
    const val NAMESPACE_NAME = AglBase.NAMESPACE_NAME
    const val NAME = "CrossReferences"
    const val goalRuleName = "unit"

    val identity = LanguageIdentity("${NAMESPACE_NAME}.${NAME}")

    const val grammarStr = """namespace $NAMESPACE_NAME

grammar $NAME extends Expressions {

    override namespace = 'namespace' possiblyQualifiedName import* declarations ;
    declarations = rootIdentifiables scopes references? ;
    rootIdentifiables = identifiable* ;
    scopes = scope* ;
    scope = 'scope' simpleTypeName '{' identifiables '}' ;
    identifiables = identifiable* ;
    identifiable = 'identify' simpleTypeName 'by' expression ;

    references = 'references' '{' referenceDefinitions '}' ;
    referenceDefinitions = referenceDefinition* ;
    referenceDefinition = 'in' simpleTypeName '{' referenceExpression* '}' ;
    referenceExpression = propertyReferenceExpression | collectionReferenceExpression ;
    propertyReferenceExpression = 'property' rootOrNavigation 'refers-to' typeReferences from? ;
    from = 'from' navigationExpression ;
    collectionReferenceExpression = 'forall' rootOrNavigation ofType? '{' referenceExpressionList '}' ;
    ofType = 'of-type' possiblyQualifiedTypeReference ;
    
    rootOrNavigation = rootExpression | navigationExpression ;
    
    typeReferences = [possiblyQualifiedTypeReference / '|']+ ;
    possiblyQualifiedTypeReference = possiblyQualifiedName ;
    simpleTypeName = IDENTIFIER ;
}
"""

    val grammar = grammar(
        namespace = NAMESPACE_NAME,
        name = NAME
    ) {
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
            lit("scope"); ref("simpleTypeName"); lit("{"); ref("identifiables"); lit("}")
        }
        list("identifiables", 0, -1) { ref("identifiable") }
        concatenation("identifiable") {
            lit("identify"); ref("simpleTypeName"); lit("by"); ref("expression")
        }
        concatenation("references") {
            lit("references"); lit("{"); ref("referenceDefinitions"); lit("}")
        }
        list("referenceDefinitions", 0, -1) { ref("referenceDefinition") }
        concatenation("referenceDefinition") {
            lit("in"); ref("simpleTypeName"); lit("{"); ref("referenceExpressionList"); lit("}")
        }
        list("referenceExpressionList", 0, -1) { ref("referenceExpression") }
        choice("referenceExpression") {
            ref("propertyReferenceExpression")
            ref("collectionReferenceExpression")
        }
        concatenation("propertyReferenceExpression") {
            lit("property"); ref("rootOrNavigation"); lit("refers-to"); ref("typeReferences"); opt { ref("from") }
        }
        concatenation("from") { lit("from"); ref("navigationExpression") }
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

    const val komposite = """namespace net.akehurst.language.reference.api
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
}
interface ReferenceExpressionCollection {
    cmp expression
    cmp referenceExpressionList
}
"""

    val typesDomain by lazy {
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
                        typeArgument("SimpleName")
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
                        parameter("scopeForTypeName", "SimpleName", false)
                    }
                    propertyOf(setOf(VAR, CMP, STR), "identifiables", "List", false){
                        typeArgument("Identifiable")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "scopeForTypeName", "SimpleName", false)
                }
                data("ReferenceExpressionPropertyDefault") {
                    supertype("ReferenceExpressionAbstract")
                    supertype("ReferenceExpressionProperty")
                    constructor_ {
                        parameter("referringPropertyNavigation", "NavigationExpression", false)
                        parameter("refersToTypeName", "List", false)
                        parameter("fromNavigation", "NavigationExpression", false)
                    }
                    propertyOf(setOf(VAL, CMP, STR), "fromNavigation", "NavigationExpression", false)
                    propertyOf(setOf(VAL, CMP, STR), "referringPropertyNavigation", "NavigationExpression", false)
                    propertyOf(setOf(VAR, REF, STR), "refersToTypeName", "List", false){
                        typeArgument("PossiblyQualifiedName")
                    }
                }
                data("ReferenceExpressionCollectionDefault") {
                    supertype("ReferenceExpressionAbstract")
                    supertype("ReferenceExpressionCollection")
                    constructor_ {
                        parameter("expression", "Expression", false)
                        parameter("ofType", "PossiblyQualifiedName", false)
                        parameter("referenceExpressionList", "List", false)
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
                        parameter("inTypeName", "SimpleName", false)
                        parameter("referenceExpressionList", "List", false)
                    }
                    propertyOf(setOf(VAL, CMP, STR), "inTypeName", "SimpleName", false)
                    propertyOf(setOf(VAR, CMP, STR), "referenceExpressionList", "List", false){
                        typeArgument("ReferenceExpression")
                    }
                }
                data("IdentifiableDefault") {
                    supertype("Identifiable")
                    constructor_ {
                        parameter("typeName", "SimpleName", false)
                        parameter("identifiedBy", "Expression", false)
                    }
                    propertyOf(setOf(VAL, CMP, STR), "identifiedBy", "Expression", false)
                    propertyOf(setOf(VAL, CMP, STR), "typeName", "SimpleName", false)
                }
                data("DeclarationsForNamespaceDefault") {
                    supertype("DeclarationsForNamespace")
                    supertype("DefinitionAbstract"){ ref("net.akehurst.language.reference.api.DeclarationsForNamespace") }
                    constructor_ {
                        parameter("namespace", "CrossReferenceNamespace", false)
                        parameter("options", "OptionHolder", false)
                    }
                    propertyOf(setOf(VAL, CMP, STR), "name", "SimpleName", false)
                    propertyOf(setOf(VAL, REF, STR), "namespace", "CrossReferenceNamespace", false)
                    propertyOf(setOf(VAL, CMP, STR), "options", "OptionHolder", false)
                    propertyOf(setOf(VAR, CMP, STR), "references", "List", false){
                        typeArgument("ReferenceDefinition")
                    }
                    propertyOf(setOf(VAR, CMP, STR), "scopeDefinition", "Map", false){
                        typeArgument("SimpleName")
                        typeArgument("ScopeDefinition")
                    }
                }
                data("CrossReferenceNamespaceDefault") {
                    supertype("NamespaceAbstract"){ ref("net.akehurst.language.reference.api.DeclarationsForNamespace") }
                    supertype("CrossReferenceNamespace")
                    constructor_ {
                        parameter("qualifiedName", "QualifiedName", false)
                        parameter("options", "OptionHolder", false)
                        parameter("import", "List", false)
                    }
                    propertyOf(setOf(VAL, CMP, STR), "qualifiedName", "QualifiedName", false)
                }
                data("CrossReferenceModelDefault") {
                    supertype("ModelAbstract"){ ref("net.akehurst.language.reference.api.CrossReferenceNamespace"); ref("net.akehurst.language.reference.api.DeclarationsForNamespace") }
                    supertype("CrossReferenceModel")
                    constructor_ {
                        parameter("name", "SimpleName", false)
                        parameter("options", "OptionHolder", false)
                        parameter("namespace", "List", false)
                    }
                    propertyOf(setOf(VAL, CMP, STR), "name", "SimpleName", false)
                }
            }
        }
    }

    const val styleStr = """namespace $NAMESPACE_NAME
styles $NAME : Base {
}"""

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

    override fun toString(): String = grammarStr.trimIndent()
}