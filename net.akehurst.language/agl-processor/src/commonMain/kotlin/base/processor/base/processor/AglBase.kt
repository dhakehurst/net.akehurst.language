/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.base.processor

import net.akehurst.language.agl.format.builder.formatModel
import net.akehurst.language.api.processor.CompletionProvider
import net.akehurst.language.api.processor.LanguageIdentity
import net.akehurst.language.api.processor.LanguageObjectAbstract
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.formatter.api.AglFormatModel
import net.akehurst.language.grammar.api.Grammar
import net.akehurst.language.grammar.api.GrammarModel
import net.akehurst.language.grammar.builder.grammarModel
import net.akehurst.language.grammarTypemodel.builder.grammarTypeNamespace
import net.akehurst.language.reference.api.CrossReferenceModel
import net.akehurst.language.reference.builder.crossReferenceModel
import net.akehurst.language.regex.api.CommonRegexPatterns
import net.akehurst.language.style.api.AglStyleModel
import net.akehurst.language.style.builder.styleModel
import net.akehurst.language.asmTransform.api.AsmTransformDomain
import net.akehurst.language.asmTransform.builder.asmTransform
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.asm.StdLibDefault
import net.akehurst.language.typemodel.builder.typeModel

object AglBase : LanguageObjectAbstract<Any, SentenceContext>() {
    const val NAMESPACE_NAME = "net.akehurst.language"
    const val NAME = "Base"

    override val identity: LanguageIdentity = LanguageIdentity("${NAMESPACE_NAME}.${NAME}")

    override val grammarString: String = """
        namespace $NAMESPACE_NAME
          grammar $NAME {
            skip leaf WHITESPACE = "\s+" ;
            skip leaf MULTI_LINE_COMMENT = "/[*][^*]*[*]+([^*/][^*]*[*]+)*/" ;
            skip leaf SINGLE_LINE_COMMENT = "//[^\n\r]*" ;
        
            unit = option* namespace* ;
            namespace = 'namespace' possiblyQualifiedName option* import* definition* ;
            import = 'import' possiblyQualifiedName ;
            definition = 'definition' IDENTIFIER ;
            possiblyQualifiedName = [IDENTIFIER / '.']+ ;
            option = '#' IDENTIFIER (':' IDENTIFIER)? ;
            leaf IDENTIFIER = "[a-zA-Z_][a-zA-Z_0-9-]*" ;
          }
      """.trimIndent()

    override val kompositeString = """namespace net.akehurst.language.base.api
    interface Model {
        cmp namespace
        cmp options
    }
    interface Namespace {
        cmp options
    }
    interface Definition {
        cmp options
    }
    
namespace net.akehurst.language.base.asm
    class NamespaceAbstract {
        cmp _definition
        cmp options
    }
"""
    override val styleString: String = """
        namespace $NAMESPACE_NAME
          styles $NAME {
            $$ "${CommonRegexPatterns.LITERAL.escapedFoAgl.value}" {
              foreground: darkgreen;
              font-weight: bold;
            }
          }
      """

    override val grammarModel: GrammarModel by lazy {
        grammarModel(NAME) {
            namespace(NAMESPACE_NAME) {
                grammar(NAME) {
                    concatenation("WHITESPACE", isSkip = true, isLeaf = true) { pat("\\s+") }
                    concatenation("MULTI_LINE_COMMENT", isSkip = true, isLeaf = true) { pat("/[*][^*]*[*]+([^*/][^*]*[*]+)*/") }
                    concatenation("SINGLE_LINE_COMMENT", isSkip = true, isLeaf = true) { pat("//[^\\n\\r]*") }

                    concatenation("unit") { lst(0, -1) { ref("option") }; lst(0, -1) { ref("namespace") } }
                    concatenation("namespace") {
                        lit("namespace"); ref("possiblyQualifiedName")
                        lst(0, -1) { ref("option") };
                        lst(0, -1) { ref("import") }
                        lst(0, -1) { ref("definition") }
                    }
                    concatenation("import") { lit("import"); ref("possiblyQualifiedName") }
                    concatenation("definition") { lit("definition"); ref("IDENTIFIER") }
                    separatedList("possiblyQualifiedName", 1, -1) { ref("IDENTIFIER"); lit(".") }
                    concatenation("option") {
                        lit("#"); ref("IDENTIFIER"); opt { grp { lit(":"); ref("IDENTIFIER") } }
                    }
                    concatenation("IDENTIFIER", isLeaf = true) { pat("[a-zA-Z_][a-zA-Z_0-9-]*") } //TODO: do not end with '-'
                }
            }
        }
    }

    /** implemented as kotlin classes **/
    override val typesModel: TypeModel by lazy {
        //TODO: NamespaceAbstract._definition wrongly generated with net.akehurst.language.base.asm.NamespaceAbstract.DT
        typeModel(NAME, true, listOf(StdLibDefault)) {
            grammarTypeNamespace("net.akehurst.language.base.api", listOf("std")) {
                value("SimpleName") {
                    supertype("PossiblyQualifiedName")
                    supertype("PublicValueType")
                    constructor_ {
                        parameter("value", "String", false)
                    }
                    propertyOf(setOf(VAL, REF, STR), "value", "String", false)
                }
                value("QualifiedName") {
                    supertype("PossiblyQualifiedName")
                    supertype("PublicValueType")
                    constructor_ {
                        parameter("value", "String", false)
                    }
                    propertyOf(setOf(VAL, REF, STR), "value", "String", false)
                }
                value("Import") {
                    supertype("PublicValueType")
                    constructor_ {
                        parameter("value", "String", false)
                    }
                    propertyOf(setOf(VAL, REF, STR), "value", "String", false)
                }
                interface_("PublicValueType") {

                }
                interface_("PossiblyQualifiedName") {

                }
                interface_("OptionHolder") {

                }
                interface_("Namespace") {
                    typeParameters("DT")
                    supertype("Formatable")
                    propertyOf(setOf(VAL, CMP, STR), "options", "OptionHolder", false)
                }
                interface_("Model") {
                    typeParameters("NT", "DT")
                    supertype("Formatable")
                    propertyOf(setOf(VAR, CMP, STR), "namespace", "List", false) {
                        typeArgument("NT")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "options", "OptionHolder", false)
                }
                interface_("Formatable") {

                }
                interface_("DefinitionReference") {
                    typeParameters("DT")

                }
                interface_("Definition") {
                    typeParameters("DT")
                    supertype("Formatable")
                    propertyOf(setOf(VAL, CMP, STR), "options", "OptionHolder", false)
                }
                data("Indent") {

                    constructor_ {
                        parameter("value", "String", false)
                        parameter("increment", "String", false)
                    }
                    propertyOf(setOf(VAL, REF, STR), "increment", "String", false)
                    propertyOf(setOf(VAL, REF, STR), "value", "String", false)
                }
                data("Asm_apiKt") {

                }
            }
            namespace("net.akehurst.language.base.asm", listOf("net.akehurst.language.base.api", "std")) {
                data("OptionHolderDefault") {
                    supertype("OptionHolder")
                    constructor_ {
                        parameter("parent", "OptionHolder", false)
                        parameter("options", "Map", false)
                    }
                    propertyOf(setOf(VAR, REF, STR), "options", "Map", false) {
                        typeArgument("String")
                        typeArgument("String")
                    }
                    propertyOf(setOf(VAR, REF, STR), "parent", "OptionHolder", false)
                }
                data("NamespaceDefault") {
                    supertype("NamespaceAbstract") { ref("DefinitionDefault") }
                    constructor_ {
                        parameter("qualifiedName", "QualifiedName", false)
                        parameter("options", "OptionHolder", false)
                        parameter("import", "List", false)
                    }
                    propertyOf(setOf(VAL, CMP, STR), "qualifiedName", "QualifiedName", false)
                }
                data("NamespaceAbstract") {
                    typeParameters("DT")
                    supertype("Namespace") { ref("DT") }
                    constructor_ {
                        parameter("options", "OptionHolder", false)
                        parameter("argImport", "List", false)
                    }
                    propertyOf(setOf(VAR, CMP, STR), "_definition", "Map", false) {
                        typeArgument("SimpleName")
                        typeArgument("DT")
                    }
                    propertyOf(setOf(VAR, CMP, STR), "import", "List", false) {
                        typeArgument("Import")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "options", "OptionHolder", false)
                }
                data("ModelDefault") {
                    supertype("ModelAbstract") { ref("NamespaceDefault"); ref("DefinitionDefault") }
                    constructor_ {
                        parameter("name", "SimpleName", false)
                        parameter("options", "OptionHolder", false)
                        parameter("namespace", "List", false)
                    }
                    propertyOf(setOf(VAL, CMP, STR), "name", "SimpleName", false)
                }
                data("ModelAbstract") {
                    typeParameters("NT", "DT")
                    supertype("Model") { ref("NT"); ref("DT") }
                    constructor_ {
                        parameter("namespace", "List", false)
                        parameter("options", "OptionHolder", false)
                    }
                    propertyOf(setOf(VAR, CMP, STR), "namespace", "List", false) {
                        typeArgument("NT")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "options", "OptionHolder", false)
                }
                data("DefinitionDefault") {
                    supertype("DefinitionAbstract") { ref("DefinitionDefault") }
                    constructor_ {
                        parameter("namespace", "Namespace", false)
                        parameter("name", "SimpleName", false)
                        parameter("options", "OptionHolder", false)
                    }
                    propertyOf(setOf(VAL, CMP, STR), "name", "SimpleName", false)
                    propertyOf(setOf(VAL, REF, STR), "namespace", "Namespace", false) {
                        typeArgument("DefinitionDefault")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "options", "OptionHolder", false)
                }
                data("DefinitionAbstract") {
                    typeParameters("DT")
                    supertype("Definition") { ref("DT") }
                    constructor_ {}
                }
            }
        }
    }

    override val asmTransformModel: AsmTransformDomain by lazy {
        asmTransform(NAME, typesModel, false) {
            namespace(NAMESPACE_NAME) {
                ruleSet(NAME) {
                    //TODO("not implemented")
                }
            }
        }
    }

    override val crossReferenceModel: CrossReferenceModel by lazy {
        crossReferenceModel(NAME) {
            //TODO
        }
    }

    override val formatModel: AglFormatModel by lazy {
        formatModel(NAME) {
            //TODO("not implemented")
        }
    }

    override val styleModel: AglStyleModel by lazy {
        styleModel(NAME) {
            namespace(NAMESPACE_NAME) {
                styles(NAME) {
                    metaRule(CommonRegexPatterns.LITERAL.value) {
                        declaration("foreground", "darkgreen")
                        declaration("font-weight", "bold")
                    }
                }
            }
        }
    }

    override val defaultTargetGoalRule: String = "qualifiedName"
    override val defaultTargetGrammar: Grammar by lazy { grammarModel.findDefinitionByQualifiedNameOrNull(QualifiedName("net.akehurst.language.Base"))!! }

    override val syntaxAnalyser: SyntaxAnalyser<Any>? by lazy { BaseSyntaxAnalyser() }
    override val semanticAnalyser: SemanticAnalyser<Any, SentenceContext>? by lazy { BaseSemanticAnalyser() }
    override val completionProvider: CompletionProvider<Any, SentenceContext>? by lazy { BaseCompletionProvider() }


    //TODO: gen this from the ASM
    override fun toString(): String = grammarString.trimIndent()

}