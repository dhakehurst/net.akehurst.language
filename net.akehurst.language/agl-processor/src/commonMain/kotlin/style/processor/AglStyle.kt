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

import net.akehurst.language.completion.builder.completionDomain
import net.akehurst.language.completion.api.AglCompletionDomain
import net.akehurst.language.agl.format.builder.formatDomain
import net.akehurst.language.agl.processor.contextFromLanguageObject
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.api.processor.CompletionItemKind
import net.akehurst.language.api.processor.CompletionProvider
import net.akehurst.language.api.processor.LanguageIdentity
import net.akehurst.language.api.processor.LanguageObjectAbstract
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.asmTransform.builder.asmTransform
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.processor.AglBase
import net.akehurst.language.grammar.api.OverrideKind
import net.akehurst.language.grammar.builder.grammarDomain
import net.akehurst.language.grammar.processor.AglGrammar
import net.akehurst.language.grammar.processor.contextFromGrammar
import net.akehurst.language.grammarTypemodel.builder.grammarTypeNamespace
import net.akehurst.language.reference.builder.crossReferenceDomain
import net.akehurst.language.regex.api.CommonRegexPatterns
import net.akehurst.language.style.api.AglStyleDomain
import net.akehurst.language.style.asm.AglStyleDomainDefault
import net.akehurst.language.style.builder.styleDomain
import net.akehurst.language.types.builder.typesDomain

object AglStyle : LanguageObjectAbstract<AglStyleDomain, SentenceContext>() {
    const val NAMESPACE_NAME = AglBase.NAMESPACE_NAME
    const val NAME = "Style"
    const val goalRuleName = "unit"

    override val identity = LanguageIdentity("${NAMESPACE_NAME}.${NAME}")

    override val extends by lazy { listOf(AglBase) }

    override val grammarString = """
        namespace $NAMESPACE_NAME
            grammar $NAME : ${AglBase.NAME} {
                override unit = namespace styleSet* ;
                styleSet = 'styles' IDENTIFIER extends? '{' typeImport* rule* '}' ;
                typeImport = 'import-types' possiblyQualifiedName ;
                extends = ':' [possiblyQualifiedName / ',']+ ;
                rule = metaRule | tagRule ;
                metaRule = '$$' PATTERN '{' style* '}' ;
                tagRule = selectorExpression '{' style* '}' ;
                selectorExpression
                 = selectorAndComposition
                 | selectorSingle
                 ; //TODO
                selectorAndComposition = [selectorSingle /',']2+ ;
                selectorSingle = LITERAL | PATTERN | IDENTIFIER | SPECIAL_IDENTIFIER | semanticSelector ;
                semanticSelector = semanticKind possiblyQualifiedName ;
                semanticKind = 'type' | 'identify' | 'refers-to' ;
                leaf LITERAL = "${CommonRegexPatterns.LITERAL.escapedFoAgl.value}" ;
                leaf PATTERN = "${CommonRegexPatterns.PATTERN.escapedFoAgl.value}" ;
                leaf SPECIAL_IDENTIFIER = "[\$][a-zA-Z_][a-zA-Z_0-9-]*" ;
                style = STYLE_ID ':' styleValue ';' ;
                styleValue = STYLE_VALUE | STRING ;
                leaf STYLE_ID = "[-a-zA-Z_][-a-zA-Z_0-9]*" ;
                leaf STYLE_VALUE = "[^;: \t\n\x0B\f\r]+" ;
            }
        """.trimIndent()

    override val typesString: String = """
        namespace ${NAMESPACE_NAME}.style.api
          // TODO
    """.trimIndent()

    override val kompositeString: String = """
        namespace ${NAMESPACE_NAME}.style.api
            interface StyleSet {
                cmp extends
                cmp rules
            }
            interface AglStyleRule {
                cmp selector
                cmp declaration
            }
    """.trimIndent()

    override val asmTransformString: String = """
        namespace ${NAMESPACE_NAME}
          // TODO
    """.trimIndent()

    override val crossReferenceString = """
        namespace ${NAMESPACE_NAME}.style.api
          references {
            in AglStyleSelector {
              property value refers-to GrammarRule | LITERAL | PATTERN
            }
          }
    """.trimIndent()

    override val styleString = """
        namespace ${NAMESPACE_NAME}
          styles $NAME : ${AglBase.NAME} {
            SPECIAL_IDENTIFIER {
              foreground: orange;
              font-weight: bold;
            }
            LITERAL {
              foreground: blue;
              font-weight: bold;
            }
            PATTERN {
              foreground: darkblue;
              font-weight: bold;
            }
            STYLE_ID {
              foreground: purple;
            }
          }
        """.trimIndent()

    override val formatString: String = """
        namespace ${NAMESPACE_NAME}.style.api
          // TODO
    """.trimIndent()

    override val completionString: String = """
        namespace $NAMESPACE_NAME
          completion $NAME {
            style -> replace [
              CompletionItem() { label := 'style' text := '<STYLE_ID>: <STYLE_VALUE>;' }
            ]
            STYLE_ID -> replace [
              CompletionItem() { kind := 'LITERAL' label := 'STYLE_ID' text := 'foreground' },
              CompletionItem() { kind := 'LITERAL' label := 'STYLE_ID' text := 'background' },
              CompletionItem() { kind := 'LITERAL' label := 'STYLE_ID' text := 'font-style' }
            ]
            STYLE_VALUE -> replace [
              CompletionItem() { kind := 'LITERAL' label := 'STYLE_VALUE' text := '<colour>' },
              CompletionItem() { kind := 'LITERAL' label := 'STYLE_VALUE' text := 'bold' },
              CompletionItem() { kind := 'LITERAL' label := 'STYLE_VALUE' text := 'italic' }
            ]
            SPECIAL_IDENTIFIER -> add [
              CompletionItem() { kind := 'LITERAL' label := 'SPECIAL_IDENTIFIER' text := '${'$'}keyword' },
              CompletionItem() { kind := 'LITERAL' label := 'SPECIAL_IDENTIFIER' text := '${'$'}nostyle' }
            ]
            styleValue -> hide
            STRING -> hide
            // the choice 'rule' does not enclose a position, so its alternatives are used as context
            in tagRule '{' -> replace [
              CompletionItem() { kind := 'LITERAL' label := 'rule' text := '{' },
              CompletionItem() { label := 'rule' text := '{
          <STYLE_ID>: <STYLE_VALUE>;
        }' }
            ]
            in tagRule '}' -> decorate { label := 'rule' }
            in metaRule '{' -> replace [
              CompletionItem() { kind := 'LITERAL' label := 'rule' text := '{' },
              CompletionItem() { label := 'rule' text := '{
          <STYLE_ID>: <STYLE_VALUE>;
        }' }
            ]
            in metaRule '}' -> decorate { label := 'rule' }
            in style ':' -> decorate { label := 'style' }
            in style ';' -> decorate { label := 'style' }
            in selectorAndComposition ',' -> decorate { label := 'selectorAndComposition' }
          }
    """.trimIndent()

    override val grammarDomain by lazy {
        grammarDomain(NAME) {
            namespace(NAMESPACE_NAME) {
                grammar(NAME) {
                    extendsGrammar(AglBase.defaultTargetGrammar.selfReference)

                    concatenation("unit", overrideKind = OverrideKind.REPLACE) {
                        ref("namespace"); lst(0, -1) { ref("styleSet") }
                    }
                    concatenation("styleSet") {
                        lit("styles"); ref("IDENTIFIER"); opt { ref("extends") }; lit("{");
                        lst(0, -1) { ref("typeImport") }
                        lst(0, -1) { ref("rule") }
                        lit("}")
                    }
                    concatenation("typeImport") { lit("import-types"); ref("possiblyQualifiedName") }
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
                        ref("semanticSelector")
                    }
                    concatenation("semanticSelector") {
                        ref("semanticKind"); ref("possiblyQualifiedName")
                    }
                    choice("semanticKind") {
                        lit("type")
                        lit("identify")
                        lit("refers-to")
                    }
                    // these must match what is in the AglGrammarGrammar
                    concatenation("LITERAL", isLeaf = true) { pat(CommonRegexPatterns.LITERAL.value) }
                    concatenation("PATTERN", isLeaf = true) { pat(CommonRegexPatterns.PATTERN.value) }

                    concatenation("SPECIAL_IDENTIFIER", isLeaf = true) { pat("[\\$][a-zA-Z_][a-zA-Z_0-9-]*") }

                    concatenation("style") {
                        ref("STYLE_ID"); lit(":"); ref("styleValue"); lit(";")
                    }
                    choice("styleValue") {
                        ref("STYLE_VALUE")
                        ref("STRING")
                    }
                    concatenation("STYLE_ID", isLeaf = true) { pat("[-a-zA-Z_][-a-zA-Z_0-9]*") }
                    concatenation("STYLE_VALUE", isLeaf = true) { pat("[^;: \\t\\n\\x0B\\f\\r]+") }
                }
            }
        }
    }

    /** implemented as kotlin classes (style_asm_api.kt, style_asm.kt); these types must match them (test_AglStyle_typesDomain) **/
    override val typesDomain by lazy {
        val regexNs = AglGrammar.typesDomain.findNamespaceOrNull(QualifiedName("net.akehurst.language.regex.api"))!!
        typesDomain(NAME, true, AglBase.typesDomain.namespace + regexNs) {
            grammarTypeNamespace("net.akehurst.language.style.api", listOf("std", "net.akehurst.language.base.api", "net.akehurst.language.regex.api")) {
                enum("AglStyleSelectorKind", listOf("SPECIAL", "LITERAL", "PATTERN", "RULE_NAME", "TYPE", "IDENTIFY", "REFERS_TO"))
                interface_("AglStyleDomain") {
                    supertype("Domain") { ref("StyleNamespace"); ref("StyleSet") }
                }
                interface_("StyleNamespace") {
                    supertype("Namespace") { ref("StyleSet") }
                    propertyOf(setOf(VAL, REF, STR), "styleSet", "List", false) {
                        typeArgument("StyleSet")
                    }
                }
                interface_("StyleSetReference") {
                    propertyOf(setOf(VAL, REF, STR), "localNamespace", "StyleNamespace", false)
                    propertyOf(setOf(VAL, CMP, STR), "nameOrQName", "PossiblyQualifiedName", false)
                    propertyOf(setOf(VAL, REF, STR), "resolved", "StyleSet", true)
                }
                interface_("StyleSet") {
                    supertype("Definition") { ref("StyleSet") }
                    propertyOf(setOf(VAL, REF, STR), "namespace", "StyleNamespace", false)
                    propertyOf(setOf(VAL, CMP, STR), "extends", "List", false) {
                        typeArgument("StyleSetReference")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "importTypes", "List", false) {
                        typeArgument("Import")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "rules", "List", false) {
                        typeArgument("AglStyleRule")
                    }
                    propertyOf(setOf(VAL, REF, STR), "metaRules", "List", false) {
                        typeArgument("AglStyleMetaRule")
                    }
                    propertyOf(setOf(VAL, REF, STR), "tagRules", "List", false) {
                        typeArgument("AglStyleTagRule")
                    }
                    propertyOf(setOf(VAL, REF, STR), "allRules", "List", false) {
                        typeArgument("AglStyleRule")
                    }
                }
                interface_("AglStyleRule") {
                    supertype("Formatable")
                    propertyOf(setOf(VAL, CMP, STR), "declaration", "Map", false) {
                        typeArgument("String")
                        typeArgument("AglStyleDeclaration")
                    }
                }
                interface_("AglStyleMetaRule") {
                    supertype("AglStyleRule")
                    propertyOf(setOf(VAL, CMP, STR), "pattern", "EscapedPattern", false)
                }
                interface_("AglStyleTagRule") {
                    supertype("AglStyleRule")
                    propertyOf(setOf(VAL, CMP, STR), "selector", "List", false) {
                        typeArgument("AglStyleSelector")
                    }
                }
                data("AglStyleSelector") {
                    constructor_ {
                        parameter(setOf(), "value", "String")
                        parameter(setOf(), "kind", "AglStyleSelectorKind")
                    }
                    propertyOf(setOf(VAL, REF, STR), "value", "String", false)
                    propertyOf(setOf(VAL, REF, STR), "kind", "AglStyleSelectorKind", false)
                    propertyOf(setOf(VAR, REF, STR), "resolvedTypeName", "QualifiedName", true)
                    propertyOf(setOf(VAL, REF, STR), "matchTag", "String", false)
                }
                data("AglStyleDeclaration") {
                    constructor_ {
                        parameter(setOf(), "name", "String")
                        parameter(setOf(), "value", "String")
                    }
                    propertyOf(setOf(VAL, REF, STR), "name", "String", false)
                    propertyOf(setOf(VAL, REF, STR), "value", "String", false)
                }
            }
            namespace("net.akehurst.language.style.asm", listOf("net.akehurst.language.style.api", "std", "net.akehurst.language.base.api", "net.akehurst.language.base.asm", "net.akehurst.language.regex.api")) {
                data("StyleSetReferenceDefault") {
                    supertype("StyleSetReference")
                    constructor_ {
                        parameter(setOf(), "localNamespace", "StyleNamespace")
                        parameter(setOf(), "nameOrQName", "PossiblyQualifiedName")
                    }
                    propertyOf(setOf(VAL, REF, STR), "localNamespace", "StyleNamespace", false)
                    propertyOf(setOf(VAL, CMP, STR), "nameOrQName", "PossiblyQualifiedName", false)
                    propertyOf(setOf(VAR, REF, STR), "resolved", "StyleSet", true)
                }
                data("StyleNamespaceDefault") {
                    supertype("StyleNamespace")
                    supertype("NamespaceAbstract") { ref("net.akehurst.language.style.api.StyleSet") }
                    constructor_ {
                        parameter(setOf(), "qualifiedName", "QualifiedName")
                        parameter(setOf(), "options", "OptionHolder")
                        parameter(setOf(), "import", "List")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "qualifiedName", "QualifiedName", false)
                    propertyOf(setOf(VAR, CMP, STR), "import", "List", false) {
                        typeArgument("Import")
                    }
                }
                data("AglStyleSetDefault") {
                    supertype("StyleSet")
                    supertype("DefinitionAbstract") { ref("net.akehurst.language.style.api.StyleSet") }
                    constructor_ {
                        parameter(setOf(), "namespace", "StyleNamespace")
                        parameter(setOf(), "name", "SimpleName")
                        parameter(setOf(), "extends", "List")
                        parameter(setOf(), "options", "OptionHolder")
                    }
                    propertyOf(setOf(VAL, REF, STR), "namespace", "StyleNamespace", false)
                    propertyOf(setOf(VAL, CMP, STR), "name", "SimpleName", false)
                    propertyOf(setOf(VAL, CMP, STR), "extends", "List", false) {
                        typeArgument("StyleSetReference")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "importTypes", "List", false) {
                        typeArgument("Import")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "rules", "List", false) {
                        typeArgument("AglStyleRule")
                    }
                }
                data("AglStyleMetaRuleDefault") {
                    supertype("AglStyleMetaRule")
                    constructor_ {
                        parameter(setOf(), "pattern", "EscapedPattern")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "pattern", "EscapedPattern", false)
                    propertyOf(setOf(VAR, CMP, STR), "declaration", "Map", false) {
                        typeArgument("String")
                        typeArgument("AglStyleDeclaration")
                    }
                }
                data("AglStyleTagRuleDefault") {
                    supertype("AglStyleTagRule")
                    constructor_ {
                        parameter(setOf(), "selector", "List")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "selector", "List", false) {
                        typeArgument("AglStyleSelector")
                    }
                    propertyOf(setOf(VAR, CMP, STR), "declaration", "Map", false) {
                        typeArgument("String")
                        typeArgument("AglStyleDeclaration")
                    }
                }
                data("AglStyleDomainDefault") {
                    supertype("AglStyleDomain")
                    supertype("DomainAbstract") { ref("net.akehurst.language.style.api.StyleNamespace"); ref("net.akehurst.language.style.api.StyleSet") }
                    constructor_ {
                        parameter(setOf(), "name", "SimpleName")
                        parameter(setOf(), "options", "OptionHolder")
                        parameter(setOf(), "namespace", "List")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "name", "SimpleName", false)
                    propertyOf(setOf(VAL, CMP, STR), "namespace", "List", false) {
                        typeArgument("StyleNamespace")
                    }
                }
            }
        }
    }

    override val asmTransformDomain by lazy {
        asmTransform(name = NAME, typesDomain = typesDomain, createTypes = false) {
            namespace(qualifiedName = NAMESPACE_NAME) {
                ruleSet(NAME) {
                    importTypes("net.akehurst.language.style.api", "net.akehurst.language.base.api")
                    createObject("unit", "AglStyleDomain") { /* custom SyntaxAnalyser */ }
                    createObject("namespace", "StyleNamespace") { /* custom SyntaxAnalyser */ }
                    createObject("styleSet", "StyleSet") { /* custom SyntaxAnalyser */ }
                    createObject("typeImport", "Import") { /* custom SyntaxAnalyser */ }
                    transToListOf("extends", "StyleSetReference", $$"/* custom SyntaxAnalyser */ $nothing")
                    createObject("rule", "AglStyleRule") { /* custom SyntaxAnalyser */ }
                    createObject("metaRule", "AglStyleMetaRule") { /* custom SyntaxAnalyser */ }
                    createObject("tagRule", "AglStyleTagRule") { /* custom SyntaxAnalyser */ }
                    transToListOf("selectorExpression", "AglStyleSelector", $$"/* custom SyntaxAnalyser */ $nothing")
                    transToListOf("selectorAndComposition", "AglStyleSelector", $$"/* custom SyntaxAnalyser */ $nothing")
                    createObject("selectorSingle", "AglStyleSelector") {
                        /* custom SyntaxAnalyser, the assignment says which property the leaf gives a value to (used for completion) */
                        assignment("value", 0, "child[0]")
                    }
                    createObject("semanticSelector", "AglStyleSelector") { /* custom SyntaxAnalyser */ }
                    leafStringRule("semanticKind")
                    createObject("style", "AglStyleDeclaration") { /* custom SyntaxAnalyser */ }
                    child0StringRule("styleValue")
                    leafStringRule("LITERAL")
                    leafStringRule("PATTERN")
                    leafStringRule("SPECIAL_IDENTIFIER")
                    leafStringRule("STYLE_ID")
                    leafStringRule("STYLE_VALUE")
                    leafStringRule("STRING")
                    //TODO: from Base...should be inherited?
                    leafStringRule("IDENTIFIER")
                    createObject("possiblyQualifiedName", "PossiblyQualifiedName") { /* custom SyntaxAnalyser */ }
                }
            }
        }
    }

    override val crossReferenceDomain by lazy {
        crossReferenceDomain(NAME) {
            // GrammarRule, LITERAL and PATTERN are the types of the items in a context from a grammar (contextFromGrammar)
            declarationsFor("${NAMESPACE_NAME}.style.api") {
                reference("AglStyleSelector") {
                    property("value", listOf("GrammarRule", "LITERAL", "PATTERN"), null)
                }
            }
        }
    }

    override val styleDomain by lazy {
        styleDomain(NAME,  sentenceContext = contextFromGrammar(AglStyle.grammarDomain).union(contextFromLanguageObject(listOf(AglBase)))) {
            namespace(NAMESPACE_NAME) {
                styles(NAME) {
                    extends(AglBase.NAME)
                    tagRule("SPECIAL_IDENTIFIER") {
                        declaration("foreground", "orange")
                        declaration("font-weight", "bold")
                    }
                    tagRule("LITERAL") {
                        declaration("foreground", "blue")
                        declaration("font-weight", "bold")
                    }
                    tagRule("PATTERN") {
                        declaration("foreground", "darkblue")
                        declaration("font-weight", "bold")
                    }
                    tagRule("STYLE_ID") {
                        declaration("foreground", "purple")
                    }
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
            namespace(NAMESPACE_NAME) {
                completion(NAME) {
                    replace("style") {
                        item(label = "style", text = "<STYLE_ID>: <STYLE_VALUE>;")
                    }
                    replace("STYLE_ID") {
                        item(kind = CompletionItemKind.LITERAL, label = "STYLE_ID", text = "foreground")
                        item(kind = CompletionItemKind.LITERAL, label = "STYLE_ID", text = "background")
                        item(kind = CompletionItemKind.LITERAL, label = "STYLE_ID", text = "font-style")
                    }
                    replace("STYLE_VALUE") {
                        item(kind = CompletionItemKind.LITERAL, label = "STYLE_VALUE", text = "<colour>")
                        item(kind = CompletionItemKind.LITERAL, label = "STYLE_VALUE", text = "bold")
                        item(kind = CompletionItemKind.LITERAL, label = "STYLE_VALUE", text = "italic")
                    }
                    add("SPECIAL_IDENTIFIER") {
                        item(kind = CompletionItemKind.LITERAL, label = "SPECIAL_IDENTIFIER", text = AglStyleDomainDefault.KEYWORD_STYLE_ID.value)
                        item(kind = CompletionItemKind.LITERAL, label = "SPECIAL_IDENTIFIER", text = AglStyleDomainDefault.NO_STYLE_ID.value)
                    }
                    hide("styleValue")
                    hide("STRING")
                    // a choice (e.g. rule) is not an enclosing rule of a position, so its alternatives (tagRule, metaRule) are used
                    replace("{", context = "tagRule", forLiteral = true) {
                        item(kind = CompletionItemKind.LITERAL, label = "rule", text = "{")
                        item(label = "rule", text = "{\n  <STYLE_ID>: <STYLE_VALUE>;\n}")
                    }
                    decorate("}", context = "tagRule", label = "rule", forLiteral = true)
                    replace("{", context = "metaRule", forLiteral = true) {
                        item(kind = CompletionItemKind.LITERAL, label = "rule", text = "{")
                        item(label = "rule", text = "{\n  <STYLE_ID>: <STYLE_VALUE>;\n}")
                    }
                    decorate("}", context = "metaRule", label = "rule", forLiteral = true)
                    decorate(":", context = "style", label = "style", forLiteral = true)
                    decorate(";", context = "style", label = "style", forLiteral = true)
                    decorate(",", context = "selectorAndComposition", label = "selectorAndComposition", forLiteral = true)
                }
            }
        }
    }

    override val defaultTargetGrammar by lazy { grammarDomain.findDefinitionByQualifiedNameOrNull(QualifiedName("${NAMESPACE_NAME}.${NAME}"))!! }
    override val defaultTargetGoalRule = "unit"

    override val syntaxAnalyser: SyntaxAnalyser<AglStyleDomain>? by lazy { AglStyleSyntaxAnalyser() }
    override val semanticAnalyser: SemanticAnalyser<AglStyleDomain, SentenceContext>? by lazy { AglStyleSemanticAnalyser() }
    override val completionProvider: CompletionProvider<AglStyleDomain, SentenceContext>? by lazy { AglStyleCompletionProvider() }
}


