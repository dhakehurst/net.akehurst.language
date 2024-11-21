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

import net.akehurst.language.base.processor.AglBase
import net.akehurst.language.expressions.processor.AglExpressions
import net.akehurst.language.grammar.api.OverrideKind
import net.akehurst.language.grammar.builder.grammar
import net.akehurst.language.typemodel.builder.typeModel

object AglCrossReference {
    //: GrammarAbstract(NamespaceDefault("net.akehurst.language.agl"), "References") {
    const val goalRuleName = "unit"

    //override val options = listOf(GrammarOptionDefault(AglGrammarGrammar.OPTION_defaultGoalRule, goalRuleName))
    //override val defaultGoalRule: GrammarRule get() = this.findAllResolvedGrammarRule(goalRuleName)!!
    const val grammarStr = """namespace net.akehurst.language

grammar CrossReferences extends Expressions {

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
    from = 'from' navigation ;
    collectionReferenceExpression = 'forall' rootOrNavigation ofType? '{' referenceExpressionList '}' ;
    ofType = 'of-type' possiblyQualifiedTypeReference ;
    
    rootOrNavigation = root | navigation ;
    
    typeReferences = [possiblyQualifiedTypeReference / '|']+ ;
    possiblyQualifiedTypeReference = possiblyQualifiedName ;
    simpleTypeName = IDENTIFIER ;
}
"""

    val grammar = grammar(
        namespace = "net.akehurst.language",
        name = "CrossReferences"
    ) {
        extendsGrammar(AglExpressions.grammar.selfReference)
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
        concatenation("from") { lit("from"); ref("navigation") }
        concatenation("collectionReferenceExpression") {
            lit("forall"); ref("rootOrNavigation"); opt { ref("ofType") }; lit("{"); ref("referenceExpressionList"); lit("}")
        }
        concatenation("ofType") { lit("of-type"); ref("possiblyQualifiedTypeReference") }
        choice("rootOrNavigation") {
            ref("root")
            ref("navigation")
        }
        separatedList("typeReferences", 1, -1) { ref("possiblyQualifiedTypeReference"); lit("|") }
        concatenation("possiblyQualifiedTypeReference") { ref("possiblyQualifiedName") }
        concatenation("simpleTypeName") { ref("IDENTIFIER") }
    }

    const val komposite = """namespace net.akehurst.language.reference.api
interface DeclarationsForNamespace {
    cmp scopeDefinition
    cmp references
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

    val typeModel by lazy {
        typeModel("CrossReferences", true, AglExpressions.typeModel.namespace) {
            namespace("net.akehurst.language.reference.api", listOf("net.akehurst.language.base.api", "std", "net.akehurst.language.expressions.api", "net.akehurst.language.reference.asm")) {
                interfaceType("ScopeDefinition") {
                    supertype("Formatable")
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "identifiables", "List", false) {
                        typeArgument("Identifiable")
                    }
                }
                interfaceType("ReferenceExpressionProperty") {
                    supertype("ReferenceExpression")
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "fromNavigation", "NavigationExpression", false)
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "referringPropertyNavigation", "NavigationExpression", false)
                }
                interfaceType("ReferenceExpressionCollection") {
                    supertype("ReferenceExpression")
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "expression", "Expression", false)
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "referenceExpressionList", "List", false) {
                        typeArgument("ReferenceExpressionAbstract")
                    }
                }
                interfaceType("ReferenceExpression") {
                    supertype("Formatable")
                }
                interfaceType("ReferenceDefinition") {
                    supertype("Formatable")
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "referenceExpressionList", "List", false) {
                        typeArgument("ReferenceExpression")
                    }
                }
                interfaceType("Identifiable") {
                    supertype("Formatable")
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "identifiedBy", "Expression", false)
                }
                interfaceType("DeclarationsForNamespace") {
                    supertype("Definition") { ref("DeclarationsForNamespace") }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "references", "List", false) {
                        typeArgument("ReferenceDefinition")
                    }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "scopeDefinition", "Map", false) {
                        typeArgument("SimpleName")
                        typeArgument("ScopeDefinition")
                    }
                }
                interfaceType("CrossReferenceNamespace") {
                    supertype("Namespace") { ref("DeclarationsForNamespace") }
                }
                interfaceType("CrossReferenceModel") {
                    supertype("Model") { ref("CrossReferenceNamespace"); ref("DeclarationsForNamespace") }
                }
            }
            namespace(
                "net.akehurst.language.reference.asm",
                listOf("net.akehurst.language.reference.api", "std", "net.akehurst.language.base.api", "net.akehurst.language.expressions.api", "net.akehurst.language.base.asm")
            ) {
                dataType("ScopeDefinitionDefault") {
                    supertype("ScopeDefinition")
                    constructor_ {
                        parameter("scopeForTypeName", "SimpleName", false)
                    }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "identifiables", "List", false) {
                        typeArgument("Identifiable")
                    }
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "scopeForTypeName", "SimpleName", false)
                }
                dataType("ReferenceExpressionPropertyDefault") {
                    supertype("ReferenceExpressionAbstract")
                    supertype("ReferenceExpressionProperty")
                    constructor_ {
                        parameter("referringPropertyNavigation", "NavigationExpression", false)
                        parameter("refersToTypeName", "List", false)
                        parameter("fromNavigation", "NavigationExpression", false)
                    }
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "fromNavigation", "NavigationExpression", false)
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "referringPropertyNavigation", "NavigationExpression", false)
                    propertyOf(setOf(READ_WRITE, REFERENCE, STORED), "refersToTypeName", "List", false) {
                        typeArgument("PossiblyQualifiedName")
                    }
                }
                dataType("ReferenceExpressionCollectionDefault") {
                    supertype("ReferenceExpressionAbstract")
                    supertype("ReferenceExpressionCollection")
                    constructor_ {
                        parameter("expression", "Expression", false)
                        parameter("ofType", "PossiblyQualifiedName", false)
                        parameter("referenceExpressionList", "List", false)
                    }
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "expression", "Expression", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "ofType", "PossiblyQualifiedName", false)
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "referenceExpressionList", "List", false) {
                        typeArgument("ReferenceExpressionAbstract")
                    }
                }
                dataType("ReferenceExpressionAbstract") {
                    supertype("ReferenceExpression")
                    constructor_ {}
                }
                dataType("ReferenceDefinitionDefault") {
                    supertype("ReferenceDefinition")
                    constructor_ {
                        parameter("inTypeName", "SimpleName", false)
                        parameter("referenceExpressionList", "List", false)
                    }
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "inTypeName", "SimpleName", false)
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "referenceExpressionList", "List", false) {
                        typeArgument("ReferenceExpression")
                    }
                }
                dataType("IdentifiableDefault") {
                    supertype("Identifiable")
                    constructor_ {
                        parameter("typeName", "SimpleName", false)
                        parameter("identifiedBy", "Expression", false)
                    }
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "identifiedBy", "Expression", false)
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "typeName", "SimpleName", false)
                }
                dataType("DeclarationsForNamespaceDefault") {
                    supertype("DeclarationsForNamespace")
                    constructor_ {
                        parameter("namespace", "CrossReferenceNamespace", false)
                    }
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "name", "SimpleName", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "namespace", "CrossReferenceNamespace", false)
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "references", "List", false) {
                        typeArgument("ReferenceDefinition")
                    }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "scopeDefinition", "Map", false) {
                        typeArgument("SimpleName")
                        typeArgument("ScopeDefinition")
                    }
                }
                dataType("CrossReferenceNamespaceDefault") {
                    supertype("NamespaceAbstract") { ref("net.akehurst.language.reference.api.DeclarationsForNamespace") }
                    supertype("CrossReferenceNamespace")
                    constructor_ {
                        parameter("qualifiedName", "QualifiedName", false)
                        parameter("import", "List", false)
                    }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "import", "List", false) {
                        typeArgument("Import")
                    }
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "qualifiedName", "QualifiedName", false)
                }
                dataType("CrossReferenceModelDefault") {
                    supertype("ModelAbstract") { ref("net.akehurst.language.reference.api.CrossReferenceNamespace"); ref("net.akehurst.language.reference.api.DeclarationsForNamespace") }
                    supertype("CrossReferenceModel")
                    constructor_ {
                        parameter("name", "SimpleName", false)
                        parameter("namespace", "List", false)
                    }
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "name", "SimpleName", false)
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "namespace", "List", false) {
                        typeArgument("CrossReferenceNamespace")
                    }
                }
            }
        }
    }

    const val styleStr = """namespace net.akehurst.language
styles CrossReferences : Base {
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