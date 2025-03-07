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
import net.akehurst.language.grammar.processor.AglGrammar
import net.akehurst.language.reference.api.CrossReferenceModel
import net.akehurst.language.style.api.AglStyleModel
import net.akehurst.language.style.builder.styleModel
import net.akehurst.language.transform.api.TransformModel
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.asm.StdLibDefault
import net.akehurst.language.typemodel.builder.typeModel

object AglBase : LanguageObjectAbstract<Any, SentenceContext>() {
    const val NAMESPACE_NAME = "net.akehurst.language"
    const val NAME = "Base"

    override val identity: LanguageIdentity = LanguageIdentity("${NAMESPACE_NAME}.${NAME}")

    override val grammarString: String = """namespace net.akehurst.language
  grammar $NAME {
    skip leaf WHITESPACE = "\s+" ;
    skip leaf MULTI_LINE_COMMENT = "/\*[^*]*\*+(?:[^*`/`][^*]*\*+)*`/`" ;
    skip leaf SINGLE_LINE_COMMENT = "//[\n\r]*?" ;

    unit = option* namespace* ;
    namespace = 'namespace' possiblyQualifiedName option* import* definition*;
    import = 'import' possiblyQualifiedName ;
    definition = 'definition' IDENTIFIER ;
    possiblyQualifiedName = [IDENTIFIER / '.']+ ;
    option = '#' IDENTIFIER (':' IDENTIFIER)? ;
    leaf IDENTIFIER = "[a-zA-Z_][a-zA-Z_0-9-]*" ;
  }"""

    override val kompositeString = """namespace net.akehurst.language.base.api
    interface Model {
        cmp namespace
    }
    
namespace net.akehurst.language.base.asm
    class NamespaceAbstract {
        cmp _definition
    }
"""
    override val styleString: String = """namespace net.akehurst.language
  styles $NAME {
    ${'$'}nostyle {
      foreground: black;
      background: white;
      font-style: normal;
    }    
    ${"$"}keyword {
      foreground: darkgreen;
      font-style: bold;
    }
  }"""

    override val grammarModel: GrammarModel by lazy {
        grammarModel(NAME) {
            namespace("net.akehurst.language") {
                grammar(NAME) {
                    concatenation("WHITESPACE", isSkip = true, isLeaf = true) { pat("\\s+") }
                    concatenation("MULTI_LINE_COMMENT", isSkip = true, isLeaf = true) { pat("/\\*[^*]*\\*+([^*/][^*]*\\*+)*/") }
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
    override val typeModel: TypeModel by lazy {
        //TODO: NamespaceAbstract._definition wrongly generated with net.akehurst.language.base.asm.NamespaceAbstract.DT
        typeModel(NAME, true, listOf(StdLibDefault)) {
            namespace("net.akehurst.language.base.api", listOf("std")) {
                interfaceType("PublicValueType") {}
                valueType("SimpleName") {
                    supertype("PossiblyQualifiedName")
                    constructor_ {
                        parameter("value", "String", false)
                    }
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "value", "String", false)
                }
                valueType("QualifiedName") {
                    supertype("PossiblyQualifiedName")
                    constructor_ {
                        parameter("value", "String", false)
                    }
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "value", "String", false)
                }
                valueType("Import") {

                    constructor_ {
                        parameter("value", "String", false)
                    }
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "value", "String", false)
                }
                interfaceType("PossiblyQualifiedName") {

                }
                interfaceType("Namespace") {
                    typeParameters("DT")
                    supertype("Formatable")
                }
                interfaceType("Model") {
                    typeParameters("NT", "DT")
                    supertype("Formatable")
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "namespace", "List", false) {
                        typeArgument("NT")
                    }
                }
                interfaceType("Formatable") {

                }
                interfaceType("Definition") {
                    typeParameters("DT")
                    supertype("Formatable")
                }
                interfaceType("DefinitionReference") {}
                interfaceType("OptionHolder") {}
                dataType("Indent") {

                    constructor_ {
                        parameter("value", "String", false)
                        parameter("increment", "String", false)
                    }
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "increment", "String", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "value", "String", false)
                }
                dataType("Asm_apiKt") {
                }
            }
            namespace("net.akehurst.language.base.asm", listOf("net.akehurst.language.base.api", "std")) {
                dataType("NamespaceDefault") {
                    typeParameters("DT")
                    supertype("NamespaceAbstract") { ref("DT") }
                    constructor_ {
                        parameter("qualifiedName", "QualifiedName", false)
                    }
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "qualifiedName", "QualifiedName", false)
                }
                dataType("NamespaceAbstract") {
                    typeParameters("DT")
                    supertype("Namespace") { ref("DT") }
                    constructor_ {}
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "import", "List", false) {
                        typeArgument("Import")
                    }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "_definition", "Map", false) {
                        typeArgument("SimpleName")
                        typeArgument("DT")
                    }
                }
                dataType("ModelDefault") {
                    typeParameters("NT", "DT")
                    supertype("ModelAbstract") { ref("NT"); ref("DT") }
                    constructor_ {
                        parameter("name", "SimpleName", false)
                        parameter("namespace", "List", false)
                    }
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "name", "SimpleName", false)
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "namespace", "List", false) {
                        typeArgument("NT")
                    }
                }
                dataType("ModelAbstract") {
                    typeParameters("NT", "DT")
                    supertype("Model") { ref("NT"); ref("DT") }
                    constructor_ {}
                }
                dataType("OptionHolderDefault") {
                    supertype("OptionHolder")
                }
            }
        }
    }

    override val asmTransformModel: TransformModel get() = TODO("not implemented")

    override val crossReferenceModel: CrossReferenceModel get() = TODO("not implemented")

    override val formatModel: AglFormatModel by lazy {
        formatModel(NAME) {
            TODO("not implemented")
        }
    }

    override val styleModel: AglStyleModel by lazy {
        styleModel(NAME) {
            TODO("not implemented")
        }
    }

    override val defaultTargetGoalRule: String = "qualifiedName"
    override val defaultTargetGrammar: Grammar by lazy { grammarModel.findDefinitionByQualifiedNameOrNull(QualifiedName("net.akehurst.language.Base"))!! }

    override val syntaxAnalyser: SyntaxAnalyser<Any>? = null
    override val semanticAnalyser: SemanticAnalyser<Any, SentenceContext>? = null
    override val completionProvider: CompletionProvider<Any, SentenceContext>? = null


    //TODO: gen this from the ASM
    override fun toString(): String = grammarString.trimIndent()

}