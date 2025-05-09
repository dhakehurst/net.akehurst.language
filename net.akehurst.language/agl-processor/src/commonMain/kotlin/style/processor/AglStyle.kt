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
import net.akehurst.language.grammar.api.OverrideKind
import net.akehurst.language.grammar.builder.grammar
import net.akehurst.language.typemodel.builder.typeModel

object AglStyle {
    const val goalRuleName = "unit"

    //override val options = listOf(GrammarOptionDefault(AglGrammar.OPTION_defaultGoalRule, "rules"))
    //override val defaultGoalRule: GrammarRule get() = this.findAllResolvedGrammarRule("rules")!!

    const val grammarStr = $$$"""namespace net.akehurst.language
grammar Style : Base {
    unit = namespace styleSet* ;
    styleSet = 'styles' IDENTIFIER extends? '{' rule* '}' ;
    extends = ':' [possiblyQualifiedName / ',']+ ;
    rule = metaRule | tagRule ;
    metaRule = '$$' PATTERN '{' styleList '}' ;
    tagRule = selectorExpression '{' styleList '}' ;
    selectorExpression
     = selectorAndComposition
     | selectorSingle
     ; //TODO
    selectorAndComposition = [selectorSingle /',']2+ ;
    selectorSingle = LITERAL | PATTERN | IDENTIFIER | SPECIAL_IDENTIFIER ;
    styleList = style* ;
    style = STYLE_ID ':' styleValue ';' ;
    styleValue = STYLE_VALUE | STRING ;
    
    leaf LITERAL = "'([^'\\]|\\.)+'" ;
    leaf PATTERN = "\"([^\"\\]|\\.)+\"" ;
    leaf SPECIAL_IDENTIFIER = "[\\${'$'}][a-zA-Z_][a-zA-Z_0-9-]*" ;
    leaf STYLE_ID = "[-a-zA-Z_][-a-zA-Z_0-9]*" ;
    leaf STYLE_VALUE = "[^;: \t\n\x0B\f\r]+" ;
    leaf STRING = "'([^'\\]|\\'|\\\\)*'" ;
}
"""

    val grammar = grammar(
        namespace = "net.akehurst.language",
        name = "Style"
    ) {
        extendsGrammar(AglBase.defaultTargetGrammar.selfReference)

        concatenation("unit", overrideKind = OverrideKind.REPLACE) {
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
        choice("rule") {
            ref("metaRule")
            ref("tagRule")
        }
        concatenation("metaRule") {
            lit("$$"); ref("PATTERN"); lit("{"); lst(0, -1) { ref("style") }; lit("}")
        }
        concatenation("tagRule") {
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
            ref("SPECIAL_IDENTIFIER")
        }
        // these must match what is in the AglGrammarGrammar
        concatenation("LITERAL", isLeaf = true) { pat("'([^'\\\\]|\\\\.)*'") }
        concatenation("PATTERN", isLeaf = true) { pat("\"([^\"\\\\]|\\\\.)*\"") }

        concatenation("SPECIAL_IDENTIFIER", isLeaf = true) { pat("[\\$][a-zA-Z_][a-zA-Z_0-9-]*") }

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
                enum("AglStyleSelectorKind", listOf("LITERAL", "PATTERN", "RULE_NAME", "META"))
                interface_("StyleSetReference") {

                }
                interface_("StyleSet") {
                    supertype("Definition") { ref("StyleSet") }
                    propertyOf(setOf(VAR, CMP, STORED), "extends", "List", false) {
                        typeArgument("StyleSetReference")
                    }
                    propertyOf(setOf(VAR, CMP, STORED), "rules", "List", false) {
                        typeArgument("AglStyleRule")
                    }
                }
                interface_("StyleNamespace") {
                    supertype("Namespace") { ref("StyleSet") }
                }
                interface_("AglStyleRule") {
                    supertype("Formatable")
                    propertyOf(setOf(VAR, CMP, STORED), "declaration", "Map", false) {
                        typeArgument("String")
                        typeArgument("AglStyleDeclaration")
                    }
                    propertyOf(setOf(VAR, CMP, STORED), "selector", "List", false) {
                        typeArgument("AglStyleSelector")
                    }
                }
                interface_("AglStyleModel") {
                    supertype("Model") { ref("StyleNamespace"); ref("StyleSet") }
                }
                data("AglStyleSelector") {

                    constructor_ {
                        parameter("value", "String", false)
                        parameter("kind", "AglStyleSelectorKind", false)
                    }
                    propertyOf(setOf(VAL, REF, STORED), "kind", "AglStyleSelectorKind", false)
                    propertyOf(setOf(VAL, REF, STORED), "value", "String", false)
                }
                data("AglStyleDeclaration") {

                    constructor_ {
                        parameter("name", "String", false)
                        parameter("value", "String", false)
                    }
                    propertyOf(setOf(VAL, REF, STORED), "name", "String", false)
                    propertyOf(setOf(VAL, REF, STORED), "value", "String", false)
                }
            }
            namespace("net.akehurst.language.style.asm", listOf("net.akehurst.language.style.api", "std", "net.akehurst.language.base.api", "net.akehurst.language.base.asm")) {
                data("StyleSetReferenceDefault") {
                    supertype("StyleSetReference")
                    constructor_ {
                        parameter("localNamespace", "StyleNamespace", false)
                        parameter("nameOrQName", "PossiblyQualifiedName", false)
                    }
                    propertyOf(setOf(VAL, REF, STORED), "localNamespace", "StyleNamespace", false)
                    propertyOf(setOf(VAL, REF, STORED), "nameOrQName", "PossiblyQualifiedName", false)
                    propertyOf(setOf(VAR, REF, STORED), "resolved", "StyleSet", false)
                }
                data("StyleNamespaceDefault") {
                    supertype("StyleNamespace")
                    supertype("NamespaceAbstract") { ref("net.akehurst.language.style.api.StyleSet") }
                    constructor_ {
                        parameter("qualifiedName", "QualifiedName", false)
                        parameter("import", "List", false)
                    }
                    propertyOf(setOf(VAR, CMP, STORED), "import", "List", false) {
                        typeArgument("Import")
                    }
                    propertyOf(setOf(VAL, CMP, STORED), "qualifiedName", "QualifiedName", false)
                }
                data("AglStyleSetDefault") {
                    supertype("StyleSet")
                    constructor_ {
                        parameter("namespace", "StyleNamespace", false)
                        parameter("name", "SimpleName", false)
                        parameter("extends", "List", false)
                    }
                    propertyOf(setOf(VAR, CMP, STORED), "extends", "List", false) {
                        typeArgument("StyleSetReference")
                    }
                    propertyOf(setOf(VAL, CMP, STORED), "name", "SimpleName", false)
                    propertyOf(setOf(VAL, REF, STORED), "namespace", "StyleNamespace", false)
                    propertyOf(setOf(VAR, CMP, STORED), "rules", "List", false) {
                        typeArgument("AglStyleRule")
                    }
                }
                data("AglStyleRuleDefault") {
                    supertype("AglStyleRule")
                    constructor_ {
                        parameter("selector", "List", false)
                    }
                    propertyOf(setOf(VAR, CMP, STORED), "declaration", "Map", false) {
                        typeArgument("String")
                        typeArgument("AglStyleDeclaration")
                    }
                    propertyOf(setOf(VAR, CMP, STORED), "selector", "List", false) {
                        typeArgument("AglStyleSelector")
                    }
                }
                data("AglStyleModelDefault") {
                    supertype("AglStyleModel")
                    supertype("ModelAbstract") { ref("net.akehurst.language.style.api.StyleNamespace"); ref("net.akehurst.language.style.api.StyleSet") }
                    constructor_ {
                        parameter("name", "SimpleName", false)
                        parameter("namespace", "List", false)
                    }
                    propertyOf(setOf(VAL, CMP, STORED), "name", "SimpleName", false)
                    propertyOf(setOf(VAR, CMP, STORED), "namespace", "List", false) {
                        typeArgument("StyleNamespace")
                    }
                }
            }
        }
    }

    const val styleStr = $$$"""namespace net.akehurst.language
styles Style {
    $$ "'([^']+)'" {
      foreground: darkgreen;
      font-style: bold;
    }
    SPECIAL_IDENTIFIER {
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


