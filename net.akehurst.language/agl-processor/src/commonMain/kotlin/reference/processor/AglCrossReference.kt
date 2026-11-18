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
                declarations = rootDeclarations scopes references? ;
                rootDeclarations = rootDeclaration* ;
                rootDeclaration = typeGroup | identifiable | importIntoScope ;
                typeGroup = 'type-group' IDENTIFIER '=' typeReferences ;
                scopes = scope* ;
                scope = 'scope' possiblyQualifiedTypeReference '{' identifiables scopeImports '}' ;
                identifiables = identifiable* ;
                identifiable = 'identify' possiblyQualifiedTypeReference 'by' identifyingExpressions visibility? aliasFor? ;
                identifyingExpressions = [expression / '|']+ ;
                visibility = 'visibility' expression ;
                aliasFor = 'alias-for' rootOrNavigation ;
                scopeImports = scopeImport* ;
                scopeImport = importDefinition | inheritDefinition | inheritNamedDefinition | inheritByPositionDefinition ;
                importDefinition = 'import' 'forall' rootOrNavigation ofType? importKind rootOrNavigation recursive? visibility? condition? ;
                importKind = 'members-of' | 'element' ;
                importIntoScope = 'import-into-scope' possiblyQualifiedTypeReference importKind rootOrNavigation recursive? visibility? condition? ;
                recursive = 'recursive' expression ;
                inheritDefinition = 'inherit' 'forall' rootOrNavigation ofType? 'members-of' rootOrNavigation condition? ;
                inheritNamedDefinition = 'inherit' 'named' namedPath condition? ;
                inheritByPositionDefinition = 'inherit' 'by-position' condition? ;
                namedPath = [namedSegment / '.']+ ;
                namedSegment = IDENTIFIER | STRING ;
            
                references = 'references' '{' referenceDefinitions '}' ;
                referenceDefinitions = referenceDefinition* ;
                referenceDefinition = 'in' possiblyQualifiedTypeReference '{' referenceExpressionList '}' ;
                referenceExpressionList = referenceExpression* ;
                referenceExpression = propertyReferenceExpression | propertyReferenceWhen | collectionReferenceExpression ;
                propertyReferenceExpression = 'property' rootOrNavigation 'refers-to' typeReferences from? condition? ;
                propertyReferenceWhen = 'property' rootOrNavigation 'when' '{' propertyWhenItem+ '}' ;
                propertyWhenItem = propertyWhenOption | propertyWhenElse ;
                propertyWhenOption = expression '->' 'refers-to' typeReferences from? ;
                propertyWhenElse = 'else' '->' 'refers-to' typeReferences from? ;
                condition = 'when' expression ;
                from = 'from' fromInherited? rootOrNavigation fromElse? ;
                fromInherited = 'inherited' ;
                fromElse = 'else' rootOrNavigation ;
                collectionReferenceExpression = 'forall' rootOrNavigation ofType? condition? '{' referenceExpressionList '}' ;
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
                cmp typeGroups
            }
            interface TypeGroup {
                cmp typeNames
            }
            
            interface ScopeDefinition {
                cmp identifiables
                cmp scopeImports
            }
            interface Identifiable {
                cmp identifiedBy
                cmp identifiedByAlternatives
                cmp visibility
                cmp aliasFor
            }
            interface ScopeImport {
                cmp elements
                cmp target
                cmp recursive
                cmp visibility
                cmp condition
            }
            interface ReferenceDefinition {
                cmp referenceExpressionList
            }
            interface ReferenceExpressionProperty {
                cmp referringPropertyNavigation
                cmp fromNavigation
                cmp fromElseNavigation
                cmp condition
            }
            interface ReferenceExpressionCollection {
                cmp expression
                cmp condition
                cmp referenceExpressionList
            }
        """.trimIndent()

    override val asmTransformString: String = """
        namespace ${NAMESPACE_NAME}
          // TODO
    """.trimIndent()

    override val crossReferenceString = """
        namespace ${NAMESPACE_NAME}.reference.asm
          references {
            in ScopeDefinitionDefault {
              property scopeForTypeName refers-to TypeDefinition
            }
            in IdentifiableDefault {
              property typeName refers-to TypeDefinition
            }
            in ScopeImportDefault {
              property ofType refers-to TypeDefinition
            }
            in ReferenceDefinitionDefault {
              property inTypeName refers-to TypeDefinition
            }
            in ReferenceExpressionPropertyDefault {
              property refersToTypeName refers-to TypeDefinition
            }
            in ReferenceExpressionCollectionDefault {
              property ofType refers-to TypeDefinition
            }
          }
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
                        ref("rootDeclarations"); ref("scopes"); opt { ref("references") }
                    }
                    list("rootDeclarations", 0, -1) { ref("rootDeclaration") }
                    choice("rootDeclaration") {
                        ref("typeGroup")
                        ref("identifiable")
                        ref("importIntoScope")
                    }
                    concatenation("typeGroup") { lit("type-group"); ref("IDENTIFIER"); lit("="); ref("typeReferences") }
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
                        ref("inheritNamedDefinition")
                        ref("inheritByPositionDefinition")
                    }
                    concatenation("importDefinition") {
                        lit("import"); lit("forall"); ref("rootOrNavigation"); opt { ref("ofType") }; ref("importKind"); ref("rootOrNavigation"); opt { ref("recursive") }; opt { ref("visibility") }; opt { ref("condition") }
                    }
                    choice("importKind") {
                        lit("members-of")
                        lit("element")
                    }
                    concatenation("importIntoScope") {
                        lit("import-into-scope"); ref("possiblyQualifiedTypeReference"); ref("importKind"); ref("rootOrNavigation"); opt { ref("recursive") }; opt { ref("visibility") }; opt { ref("condition") }
                    }
                    concatenation("recursive") { lit("recursive"); ref("expression") }
                    concatenation("inheritDefinition") {
                        lit("inherit"); lit("forall"); ref("rootOrNavigation"); opt { ref("ofType") }; lit("members-of"); ref("rootOrNavigation"); opt { ref("condition") }
                    }
                    concatenation("inheritNamedDefinition") {
                        lit("inherit"); lit("named"); ref("namedPath"); opt { ref("condition") }
                    }
                    concatenation("inheritByPositionDefinition") {
                        lit("inherit"); lit("by-position"); opt { ref("condition") }
                    }
                    separatedList("namedPath", 1, -1) { ref("namedSegment"); lit(".") }
                    choice("namedSegment") {
                        ref("IDENTIFIER")
                        ref("STRING")
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
                        ref("propertyReferenceWhen")
                        ref("collectionReferenceExpression")
                    }
                    concatenation("propertyReferenceWhen") {
                        lit("property"); ref("rootOrNavigation"); lit("when"); lit("{"); lst(1, -1) { ref("propertyWhenItem") }; lit("}")
                    }
                    // 'else' is also an expression (a navigation), last option wins, so 'else -> ...' is a propertyWhenElse
                    choice("propertyWhenItem") {
                        ref("propertyWhenOption")
                        ref("propertyWhenElse")
                    }
                    concatenation("propertyWhenOption") {
                        ref("expression"); lit("->"); lit("refers-to"); ref("typeReferences"); opt { ref("from") }
                    }
                    concatenation("propertyWhenElse") {
                        lit("else"); lit("->"); lit("refers-to"); ref("typeReferences"); opt { ref("from") }
                    }
                    concatenation("propertyReferenceExpression") {
                        lit("property"); ref("rootOrNavigation"); lit("refers-to"); ref("typeReferences"); opt { ref("from") }; opt { ref("condition") }
                    }
                    concatenation("condition") { lit("when"); ref("expression") }
                    concatenation("from") { lit("from"); opt { ref("fromInherited") }; ref("rootOrNavigation"); opt { ref("fromElse") } }
                    concatenation("fromInherited") { lit("inherited") }
                    concatenation("fromElse") { lit("else"); ref("rootOrNavigation") }
                    concatenation("collectionReferenceExpression") {
                        lit("forall"); ref("rootOrNavigation"); opt { ref("ofType") }; opt { ref("condition") }; lit("{"); ref("referenceExpressionList"); lit("}")
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
                    propertyOf(setOf(VAL, CMP, STR), "scopeForTypeName", "PossiblyQualifiedName", false)
                    propertyOf(setOf(VAR, CMP, STR), "identifiables", "List", false){
                        typeArgument("Identifiable")
                    }
                    propertyOf(setOf(VAR, CMP, STR), "scopeImports", "List", false){
                        typeArgument("ScopeImport")
                    }
                }
                enum("ScopeImportKind", listOf("IMPORT_MEMBERS", "IMPORT_ELEMENT", "INHERIT"))
                interface_("ScopeImport") {
                    supertype("Formatable")
                    propertyOf(setOf(VAL, CMP, STR), "kind", "ScopeImportKind", false)
                    propertyOf(setOf(VAL, CMP, STR), "elements", "Expression", false)
                    propertyOf(setOf(VAL, REF, STR), "ofType", "PossiblyQualifiedName", true)
                    propertyOf(setOf(VAL, CMP, STR), "target", "NavigationExpression", false)
                    propertyOf(setOf(VAL, CMP, STR), "recursive", "Expression", true)
                    propertyOf(setOf(VAL, CMP, STR), "visibility", "Expression", true)
                    propertyOf(setOf(VAL, CMP, STR), "condition", "Expression", true)
                    propertyOf(setOf(VAL, CMP, STR), "named", "List", true){
                        typeArgument("String")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "intoScope", "Boolean", false)
                    propertyOf(setOf(VAL, CMP, STR), "byPosition", "Boolean", false)
                }
                interface_("ReferenceExpressionProperty") {
                    supertype("ReferenceExpression")
                    propertyOf(setOf(VAL, CMP, STR), "fromNavigation", "NavigationExpression", true)
                    propertyOf(setOf(VAL, CMP, STR), "fromElseNavigation", "NavigationExpression", true)
                    propertyOf(setOf(VAL, CMP, STR), "fromInherited", "Boolean", false)
                    propertyOf(setOf(VAL, CMP, STR), "referringPropertyNavigation", "NavigationExpression", false)
                    propertyOf(setOf(VAL, REF, STR), "refersToTypeName", "List", false){
                        typeArgument("PossiblyQualifiedName")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "condition", "Expression", true)
                    propertyOf(setOf(VAL, CMP, STR), "whenOptionIndex", "Integer", false)
                }
                interface_("ReferenceExpressionCollection") {
                    supertype("ReferenceExpression")
                    propertyOf(setOf(VAL, CMP, STR), "expression", "Expression", false)
                    propertyOf(setOf(VAL, REF, STR), "ofType", "PossiblyQualifiedName", true)
                    propertyOf(setOf(VAL, CMP, STR), "condition", "Expression", true)
                    propertyOf(setOf(VAR, CMP, STR), "referenceExpressionList", "List", false){
                        typeArgument("ReferenceExpression")
                    }
                }
                interface_("ReferenceExpression") {
                    supertype("Formatable")
                }
                interface_("ReferenceDefinition") {
                    supertype("Formatable")
                    propertyOf(setOf(VAL, CMP, STR), "inTypeName", "PossiblyQualifiedName", false)
                    propertyOf(setOf(VAR, CMP, STR), "referenceExpressionList", "List", false){
                        typeArgument("ReferenceExpression")
                    }
                }
                interface_("Identifiable") {
                    supertype("Formatable")
                    propertyOf(setOf(VAL, CMP, STR), "typeName", "PossiblyQualifiedName", false)
                    propertyOf(setOf(VAL, CMP, STR), "identifiedBy", "Expression", false)
                    propertyOf(setOf(VAL, CMP, STR), "identifiedByAlternatives", "List", false){
                        typeArgument("Expression")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "visibility", "Expression", true)
                    propertyOf(setOf(VAL, CMP, STR), "aliasFor", "NavigationExpression", true)
                }
                interface_("TypeGroup") {
                    propertyOf(setOf(VAL, CMP, STR), "name", "SimpleName", false)
                    propertyOf(setOf(VAL, CMP, STR), "typeNames", "List", false){
                        typeArgument("PossiblyQualifiedName")
                    }
                }
                interface_("DeclarationsForNamespace") {
                    supertype("Definition"){ ref("DeclarationsForNamespace") }
                    propertyOf(setOf(VAL, CMP, STR), "typeGroups", "Map", false){
                        typeArgument("SimpleName")
                        typeArgument("TypeGroup")
                    }
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
                    propertyOf(setOf(VAR, CMP, STR), "scopeImports", "List", false){
                        typeArgument("ScopeImport")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "scopeForTypeName", "PossiblyQualifiedName", false)
                }
                data("ScopeImportDefault") {
                    supertype("ScopeImport")
                    constructor_ {
                        parameter(setOf(), "kind", "ScopeImportKind")
                        parameter(setOf(), "elements", "Expression")
                        parameter(setOf(), "ofType", "PossiblyQualifiedName")
                        parameter(setOf(), "target", "NavigationExpression")
                        parameter(setOf(), "recursive", "Expression")
                        parameter(setOf(), "visibility", "Expression")
                        parameter(setOf(), "condition", "Expression")
                        parameter(setOf(), "named", "List")
                        parameter(setOf(), "intoScope", "Boolean")
                        parameter(setOf(), "byPosition", "Boolean")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "kind", "ScopeImportKind", false)
                    propertyOf(setOf(VAL, CMP, STR), "elements", "Expression", false)
                    propertyOf(setOf(VAL, REF, STR), "ofType", "PossiblyQualifiedName", true)
                    propertyOf(setOf(VAL, CMP, STR), "target", "NavigationExpression", false)
                    propertyOf(setOf(VAL, CMP, STR), "recursive", "Expression", true)
                    propertyOf(setOf(VAL, CMP, STR), "visibility", "Expression", true)
                    propertyOf(setOf(VAL, CMP, STR), "condition", "Expression", true)
                    propertyOf(setOf(VAL, CMP, STR), "named", "List", true){
                        typeArgument("String")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "intoScope", "Boolean", false)
                    propertyOf(setOf(VAL, CMP, STR), "byPosition", "Boolean", false)
                }
                data("ReferenceExpressionPropertyDefault") {
                    supertype("ReferenceExpressionAbstract")
                    supertype("ReferenceExpressionProperty")
                    constructor_ {
                        parameter(setOf(), "referringPropertyNavigation", "NavigationExpression")
                        parameter(setOf(), "refersToTypeNameAsWritten", "List")
                        parameter(setOf(), "fromNavigation", "NavigationExpression")
                        parameter(setOf(), "condition", "Expression")
                        parameter(setOf(), "fromElseNavigation", "NavigationExpression")
                        parameter(setOf(), "fromInherited", "Boolean")
                        parameter(setOf(), "whenOptionIndex", "Integer")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "whenOptionIndex", "Integer", false)
                    propertyOf(setOf(VAL, CMP, STR), "fromInherited", "Boolean", false)
                    propertyOf(setOf(VAL, CMP, STR), "condition", "Expression", true)
                    propertyOf(setOf(VAL, CMP, STR), "fromNavigation", "NavigationExpression", true)
                    propertyOf(setOf(VAL, CMP, STR), "fromElseNavigation", "NavigationExpression", true)
                    propertyOf(setOf(VAL, CMP, STR), "referringPropertyNavigation", "NavigationExpression", false)
                    propertyOf(setOf(VAR, REF, STR), "refersToTypeName", "List", false){
                        typeArgument("PossiblyQualifiedName")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "refersToTypeNameAsWritten", "List", false){
                        typeArgument("PossiblyQualifiedName")
                    }
                    propertyOf(setOf(VAR, CMP, STR), "refersToTypeNameExpanded", "List", true){
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
                        parameter(setOf(), "condition", "Expression")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "condition", "Expression", true)
                    propertyOf(setOf(VAL, CMP, STR), "expression", "Expression", false)
                    propertyOf(setOf(VAL, REF, STR), "ofType", "PossiblyQualifiedName", true)
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
                data("TypeGroupDefault") {
                    supertype("TypeGroup")
                    constructor_ {
                        parameter(setOf(), "name", "SimpleName")
                        parameter(setOf(), "typeNames", "List")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "name", "SimpleName", false)
                    propertyOf(setOf(VAL, CMP, STR), "typeNames", "List", false){
                        typeArgument("PossiblyQualifiedName")
                    }
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
                    propertyOf(setOf(VAL, CMP, STR), "typeGroups", "Map", false){
                        typeArgument("SimpleName")
                        typeArgument("TypeGroup")
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
                    //TODO: the other rules, currently only what completion (of references definitions) needs:
                    // the assignments say which property the type names give a value to
                    importTypes("net.akehurst.language.base.api", "net.akehurst.language.reference.asm")
                    createObject("scope", "ScopeDefinitionDefault") { /* custom SyntaxAnalyser */ assignment("scopeForTypeName", 1, "child[1]") }
                    createObject("identifiable", "IdentifiableDefault") { /* custom SyntaxAnalyser */ assignment("typeName", 1, "child[1]") }
                    createObject("importDefinition", "ScopeImportDefault") { /* custom SyntaxAnalyser */ assignment("ofType", 3, "child[3]") }
                    createObject("inheritDefinition", "ScopeImportDefault") { /* custom SyntaxAnalyser */ assignment("ofType", 3, "child[3]") }
                    createObject("importIntoScope", "ScopeImportDefault") { /* custom SyntaxAnalyser */ assignment("ofType", 1, "child[1]") }
                    createObject("referenceDefinition", "ReferenceDefinitionDefault") { /* custom SyntaxAnalyser */ assignment("inTypeName", 1, "child[1]") }
                    createObject("propertyReferenceExpression", "ReferenceExpressionPropertyDefault") { /* custom SyntaxAnalyser */ assignment("refersToTypeName", 3, "child[3]") }
                    createObject("propertyWhenOption", "ReferenceExpressionPropertyDefault") { /* custom SyntaxAnalyser */ assignment("refersToTypeName", 3, "child[3]") }
                    createObject("propertyWhenElse", "ReferenceExpressionPropertyDefault") { /* custom SyntaxAnalyser */ assignment("refersToTypeName", 3, "child[3]") }
                    createObject("collectionReferenceExpression", "ReferenceExpressionCollectionDefault") { /* custom SyntaxAnalyser */ assignment("ofType", 2, "child[2]") }
                    transRule("ofType", "PossiblyQualifiedName", "child[1]")
                    transToListOf("typeReferences", "PossiblyQualifiedName", "children")
                    transRule("possiblyQualifiedTypeReference", "PossiblyQualifiedName", "child[0]")
                    transRule("possiblyQualifiedName", "PossiblyQualifiedName", "children")
                }
            }
        }
    }

    override val crossReferenceDomain by lazy {
        crossReferenceDomain(NAME) {
            // TypeDefinition is the type of the types in a context from a types domain (contextFromTypesDomain)
            declarationsFor("${NAMESPACE_NAME}.reference.asm") {
                reference("ScopeDefinitionDefault") { property("scopeForTypeName", listOf("TypeDefinition"), null) }
                reference("IdentifiableDefault") { property("typeName", listOf("TypeDefinition"), null) }
                reference("ScopeImportDefault") { property("ofType", listOf("TypeDefinition"), null) }
                reference("ReferenceDefinitionDefault") { property("inTypeName", listOf("TypeDefinition"), null) }
                reference("ReferenceExpressionPropertyDefault") { property("refersToTypeName", listOf("TypeDefinition"), null) }
                reference("ReferenceExpressionCollectionDefault") { property("ofType", listOf("TypeDefinition"), null) }
            }
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