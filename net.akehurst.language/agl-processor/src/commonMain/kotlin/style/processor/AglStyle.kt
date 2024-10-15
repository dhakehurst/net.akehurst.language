/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.style.processor

import net.akehurst.language.base.processor.AglBase
import net.akehurst.language.grammar.builder.grammar
import net.akehurst.language.typemodel.builder.typeModel

object AglStyle {
    const val goalRuleName = "unit"

    //override val options = listOf(GrammarOptionDefault(AglGrammar.OPTION_defaultGoalRule, "rules"))
    //override val defaultGoalRule: GrammarRule get() = this.findAllResolvedGrammarRule("rules")!!

    const val grammarStr = """namespace net.akehurst.language.agl.language
grammar Style : Base {
    unit = namespace styleSet* ;
    styleSet = 'styles' IDENTIFIER extends? '{' rule* '}' ;
    extends = ':' [possiblyQualifiedName / ',']+ ;
    rule = selectorExpression '{' styleList '}' ;
    selectorExpression
     = selectorAndComposition
     | selectorSingle
     ; //TODO
    selectorAndComposition = [selectorSingle /',']2+ ;
    selectorSingle = LITERAL | PATTERN | IDENTIFIER | META_IDENTIFIER ;
    styleList = style* ;
    style = STYLE_ID ':' styleValue ';' ;
    styleValue = STYLE_VALUE | STRING ;
    
    leaf LITERAL = "'([^'\\]|\\.)+'" ;
    leaf PATTERN = "\"([^\"\\]|\\.)+\"" ;
    leaf META_IDENTIFIER = "[\\${'$'}][a-zA-Z_][a-zA-Z_0-9-]*" ;
    leaf STYLE_ID = "[-a-zA-Z_][-a-zA-Z_0-9]*" ;
    leaf STYLE_VALUE = "[^;: \t\n\x0B\f\r]+" ;
    leaf STRING = "'([^'\\]|\\'|\\\\)*'" ;
}
"""

    val grammar = grammar(
        namespace = "net.akehurst.language",
        name = "Style"
    ) {
        extendsGrammar(AglBase.grammar.selfReference)

        concatenation("unit") {
            ref("namespace"); lst(0, -1) { ref("styleSet") }
        }
        concatenation("styleSet") {
            lit("styles"); ref("IDENTIFIER"); opt { ref("extends") }; lit("{");
            lst(0, -1) { ref("rule") }
            lit("}")
        }
        concatenation("extends") {
            lit(":"); spLst(1, -1) { ref("possiblyQualifiedName"); lit(",") }
        }
        concatenation("rule") {
            ref("selectorExpression"); lit("{"); lst(0, -1) { ref("style") }; lit("}")
        }
        choice("selectorExpression") {
            ref("selectorAndComposition")
            ref("selectorSingle")
        }
        separatedList("selectorAndComposition", 2, -1) { ref("selectorSingle"); lit(",") }
        choice("selectorSingle") {
            ref("LITERAL")
            ref("PATTERN")
            ref("IDENTIFIER")
            ref("META_IDENTIFIER")
        }
        // these must match what is in the AglGrammarGrammar
        concatenation("LITERAL", isLeaf = true) { pat("'([^'\\\\]|\\\\.)*'") }
        concatenation("PATTERN", isLeaf = true) { pat("\"([^\"\\\\]|\\\\.)*\"") }

        concatenation("META_IDENTIFIER", isLeaf = true) { pat("[\\$][a-zA-Z_][a-zA-Z_0-9-]*") }

        concatenation("style") {
            ref("STYLE_ID"); lit(":"); ref("styleValue"); lit(";")
        }
        choice("styleValue") {
            ref("STYLE_ID")
            ref("STRING")
        }
        concatenation("STYLE_ID", isLeaf = true) { pat("[-a-zA-Z_][-a-zA-Z_0-9]*") }
        concatenation("STYLE_VALUE", isLeaf = true) { pat("[^;: \\t\\n\\x0B\\f\\r]+") }
        concatenation("STRING", isLeaf = true) { pat("'([^'\\\\]|\\\\'|\\\\\\\\)*'") }
    }

    const val komposite = """namespace net.akehurst.language.style.api
interface StyleSet {
    cmp extends
    cmp rules
}
interface AglStyleRule {
    cmp selector
    cmp declaration
}
"""

    val typeModel by lazy {
        typeModel("Style", true, AglBase.typeModel.namespace) {
            namespace("net.akehurst.language.style.api", listOf("std", "net.akehurst.language.base.api")) {
                enumType("AglStyleSelectorKind", listOf("LITERAL", "PATTERN", "RULE_NAME", "META"))
                interfaceType("StyleSetReference") {

                }
                interfaceType("StyleSet") {
                    supertype("Definition") { ref("StyleSet") }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "extends", "List", false) {
                        typeArgument("StyleSetReference")
                    }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "rules", "List", false) {
                        typeArgument("AglStyleRule")
                    }
                }
                interfaceType("StyleNamespace") {
                    supertype("Namespace") { ref("StyleSet") }
                }
                interfaceType("AglStyleRule") {
                    supertype("Formatable")
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "declaration", "Map", false) {
                        typeArgument("String")
                        typeArgument("AglStyleDeclaration")
                    }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "selector", "List", false) {
                        typeArgument("AglStyleSelector")
                    }
                }
                interfaceType("AglStyleModel") {
                    supertype("Model") { ref("StyleNamespace"); ref("StyleSet") }
                }
                dataType("AglStyleSelector") {

                    constructor_ {
                        parameter("value", "String", false)
                        parameter("kind", "AglStyleSelectorKind", false)
                    }
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "kind", "AglStyleSelectorKind", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "value", "String", false)
                }
                dataType("AglStyleDeclaration") {

                    constructor_ {
                        parameter("name", "String", false)
                        parameter("value", "String", false)
                    }
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "name", "String", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "value", "String", false)
                }
            }
            namespace("net.akehurst.language.style.asm", listOf("net.akehurst.language.style.api", "std", "net.akehurst.language.base.api", "net.akehurst.language.base.asm")) {
                dataType("StyleSetReferenceDefault") {
                    supertype("StyleSetReference")
                    constructor_ {
                        parameter("localNamespace", "StyleNamespace", false)
                        parameter("nameOrQName", "PossiblyQualifiedName", false)
                    }
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "localNamespace", "StyleNamespace", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "nameOrQName", "PossiblyQualifiedName", false)
                    propertyOf(setOf(READ_WRITE, REFERENCE, STORED), "resolved", "StyleSet", false)
                }
                dataType("StyleNamespaceDefault") {
                    supertype("StyleNamespace")
                    supertype("NamespaceAbstract") { ref("net.akehurst.language.style.api.StyleSet") }
                    constructor_ {
                        parameter("qualifiedName", "QualifiedName", false)
                        parameter("import", "List", false)
                    }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "import", "List", false) {
                        typeArgument("Import")
                    }
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "qualifiedName", "QualifiedName", false)
                }
                dataType("AglStyleSetDefault") {
                    supertype("StyleSet")
                    constructor_ {
                        parameter("namespace", "StyleNamespace", false)
                        parameter("name", "SimpleName", false)
                        parameter("extends", "List", false)
                    }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "extends", "List", false) {
                        typeArgument("StyleSetReference")
                    }
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "name", "SimpleName", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "namespace", "StyleNamespace", false)
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "rules", "List", false) {
                        typeArgument("AglStyleRule")
                    }
                }
                dataType("AglStyleRuleDefault") {
                    supertype("AglStyleRule")
                    constructor_ {
                        parameter("selector", "List", false)
                    }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "declaration", "Map", false) {
                        typeArgument("String")
                        typeArgument("AglStyleDeclaration")
                    }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "selector", "List", false) {
                        typeArgument("AglStyleSelector")
                    }
                }
                dataType("AglStyleModelDefault") {
                    supertype("AglStyleModel")
                    supertype("ModelAbstract") { ref("net.akehurst.language.style.api.StyleNamespace"); ref("net.akehurst.language.style.api.StyleSet") }
                    constructor_ {
                        parameter("name", "SimpleName", false)
                        parameter("namespace", "List", false)
                    }
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "name", "SimpleName", false)
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "namespace", "List", false) {
                        typeArgument("StyleNamespace")
                    }
                }
            }
        }
    }

    const val styleStr = """namespace net.akehurst.language
styles Style {
    META_IDENTIFIER {
      foreground: orange;
      font-style: bold;
    }
    IDENTIFIER {
      foreground: blue;
      font-style: bold;
    }
    LITERAL {
      foreground: blue;
      font-style: bold;
    }
    PATTERN {
      foreground: darkblue;
      font-style: bold;
    }
    STYLE_ID {
      foreground: darkred;
      font-style: italic;
    }
}"""

    const val scopeModelStr = """
references {
    in scope property typeReference refers-to GrammarRule
    in identifiable property typeReference refers-to GrammarRule
    in referenceDefinition property typeReference refers-to GrammarRule
    in referenceDefinition property propertyReference refers-to GrammarRule
}
    """

    //TODO: gen this from the ASM
    override fun toString(): String = grammarStr
}


