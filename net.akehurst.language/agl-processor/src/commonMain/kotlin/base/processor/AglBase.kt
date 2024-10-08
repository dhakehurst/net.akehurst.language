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

import net.akehurst.language.grammar.api.Grammar
import net.akehurst.language.grammar.asm.grammar
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.asm.SimpleTypeModelStdLib
import net.akehurst.language.typemodel.asm.typeModel

object AglBase {
    const val goalRuleName = "qualifiedName"

    //override val options = listOf(GrammarOptionDefault(AglGrammarGrammar.OPTION_defaultGoalRule, goalRuleName))
    //override val defaultGoalRule: GrammarRule get() = this.findAllResolvedGrammarRule(goalRuleName)!!

    const val grammarStr = """namespace net.akehurst.language
  grammar Base {
    skip leaf WHITESPACE = "\s+" ;
    skip leaf MULTI_LINE_COMMENT = "/\*[^*]*\*+(?:[^*`/`][^*]*\*+)*`/`" ;
    skip leaf SINGLE_LINE_COMMENT = "//[\n\r]*?" ;

    namespace = 'namespace' qualifiedName ;
    import = 'import' qualifiedName ;
    qualifiedName = [IDENTIFIER / '.']+ ;
    leaf IDENTIFIER = "[a-zA-Z_][a-zA-Z_0-9-]*" ;
  }"""

    val grammar: Grammar by lazy {
        grammar(
            namespace = "net.akehurst.language",
            name = "Base"
        ) {
            concatenation("WHITESPACE", isSkip = true, isLeaf = true) { pat("\\s+") }
            concatenation("MULTI_LINE_COMMENT", isSkip = true, isLeaf = true) { pat("/\\*[^*]*\\*+([^*/][^*]*\\*+)*/") }
            concatenation("SINGLE_LINE_COMMENT", isSkip = true, isLeaf = true) { pat("//[^\\n\\r]*") }

            concatenation("namespace") { lit("namespace"); ref("qualifiedName") }
            concatenation("import") { lit("import"); ref("qualifiedName") }
            separatedList("qualifiedName", 1, -1) { ref("IDENTIFIER"); lit(".") }
            concatenation("IDENTIFIER", isLeaf = true) { pat("[a-zA-Z_][a-zA-Z_0-9-]*") } //TODO: do not end with '-'
        }
    }

    const val styleStr = """namespace net.akehurst.language
  styles Base {
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

    const val formatterStr = """
    """

    //TODO: gen this from the ASM
    override fun toString(): String = grammarStr.trimIndent()

    const val komposite = """namespace net.akehurst.language.base.api
    interface Model {
        cmp namespace
    }
    interface Namespace {
        cmp definition
    }
    
namespace net.akehurst.language.base.asm
    class NamespaceAbstract {
        cmp _definition
    }
    """

    /** implemented as kotlin classes **/
    val typeModel: TypeModel by lazy {
        //TODO: NamespaceAbstract._definition wrongly generated with net.akehurst.language.base.asm.NamespaceAbstract.DT
        typeModel("Base", true, listOf(SimpleTypeModelStdLib)) {
            namespace("net.akehurst.language.base.api", listOf("std")) {
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
                }
                interfaceType("Formatable") {

                }
                interfaceType("Definition") {
                    typeParameters("DT")
                    supertype("Formatable")
                }
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
                }
                dataType("NamespaceAbstract") {
                    typeParameters("DT")
                    supertype("net.akehurst.language.base.api.Namespace") { ref("DT") }
                    constructor_ {
                        parameter("qualifiedName", "QualifiedName", false)
                    }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "_definition", "Map", false) {
                        typeArgument("net.akehurst.language.base.api.SimpleName")
                        typeArgument("DT")
                    }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "import", "List", false) {
                        typeArgument("net.akehurst.language.base.api.Import")
                    }
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "qualifiedName", "QualifiedName", false)
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
                        typeArgument("net.akehurst.language.base.asm.ModelDefault.NT")
                    }
                }
                dataType("ModelAbstract") {
                    typeParameters("NT", "DT")
                    supertype("net.akehurst.language.base.api.Model") { ref("NT"); ref("DT") }
                    constructor_ {}
                }
            }
        }
    }
}