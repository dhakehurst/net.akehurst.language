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

import net.akehurst.language.agl.format.builder.formatDomain
import net.akehurst.language.agl.processor.contextFromLanguageObject
import net.akehurst.language.agl.simple.SentenceContextAny
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
import net.akehurst.language.grammar.processor.contextFromGrammar
import net.akehurst.language.grammarTypemodel.builder.grammarTypeNamespace
import net.akehurst.language.reference.builder.crossReferenceDomain
import net.akehurst.language.regex.api.CommonRegexPatterns
import net.akehurst.language.style.api.AglStyleDomain
import net.akehurst.language.style.builder.styleDomain
import net.akehurst.language.types.builder.typesDomain

object AglStyle : LanguageObjectAbstract<AglStyleDomain, SentenceContextAny>() {
    const val NAMESPACE_NAME = AglBase.NAMESPACE_NAME
    const val NAME = "Style"
    const val goalRuleName = "unit"

    override val identity = LanguageIdentity("${NAMESPACE_NAME}.${NAME}")

    override val extends by lazy { listOf(AglBase) }

    override val grammarString = """
        namespace $NAMESPACE_NAME
            grammar $NAME : Base {
                override unit = namespace styleSet* ;
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
                
                leaf LITERAL = "${CommonRegexPatterns.LITERAL.escapedFoAgl.value}" ;
                leaf PATTERN = "${CommonRegexPatterns.PATTERN.escapedFoAgl.value}" ;
                leaf SPECIAL_IDENTIFIER = "[\\$][a-zA-Z_][a-zA-Z_0-9-]*" ;
                leaf STYLE_ID = "[-a-zA-Z_][-a-zA-Z_0-9]*" ;
                leaf STYLE_VALUE = "[^;: \t\n\x0B\f\r]+" ;
                leaf STRING = "'([^'\\\\]|\\'|\\\\)*'" ;
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
        namespace $NAMESPACE_NAME
          references {
            in scope property typeReference refers-to GrammarRule
            in identifiable property typeReference refers-to GrammarRule
            in referenceDefinition property typeReference refers-to GrammarRule
            in referenceDefinition property propertyReference refers-to GrammarRule
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
                    concatenation("LITERAL", isLeaf = true) { pat(CommonRegexPatterns.LITERAL.value) }
                    concatenation("PATTERN", isLeaf = true) { pat(CommonRegexPatterns.PATTERN.value) }

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
            }
        }
    }

    override val typesDomain by lazy {
        typesDomain(NAME, true, AglBase.typesDomain.namespace) {
            grammarTypeNamespace("net.akehurst.language.style.api", listOf("std", "net.akehurst.language.base.api")) {
                enum("AglStyleSelectorKind", listOf("LITERAL", "PATTERN", "RULE_NAME", "META"))
                interface_("AglStyleDomain") {
                    supertype("Domain") { ref("StyleNamespace"); ref("StyleSet") }
                }
                interface_("StyleNamespace") {
                    supertype("Namespace") { ref("StyleSet") }
                }
                interface_("StyleSetReference") {

                }
                interface_("StyleSet") {
                    supertype("Definition") { ref("StyleSet") }
                    propertyOf(setOf(VAR, CMP, STR), "extends", "List", false) {
                        typeArgument("StyleSetReference")
                    }
                    propertyOf(setOf(VAR, CMP, STR), "rules", "List", false) {
                        typeArgument("AglStyleRule")
                    }
                }
                interface_("AglStyleRule") {
                    supertype("Formatable")
                    propertyOf(setOf(VAR, CMP, STR), "declaration", "Map", false) {
                        typeArgument("String")
                        typeArgument("AglStyleDeclaration")
                    }
                    propertyOf(setOf(VAR, CMP, STR), "selector", "List", false) {
                        typeArgument("AglStyleSelector")
                    }
                }
                interface_("AglStyleMetaRule") {
                    supertype("AglStyleRule")
                }
                interface_("AglStyleTagRule") {
                    supertype("AglStyleRule")
                }
                data("AglStyleSelector") {

                    constructor_ {
                        parameter(setOf(), "value", "String")
                        parameter(setOf(), "kind", "AglStyleSelectorKind")
                    }
                    propertyOf(setOf(VAL, REF, STR), "kind", "AglStyleSelectorKind", false)
                    propertyOf(setOf(VAL, REF, STR), "value", "String", false)
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
            namespace("net.akehurst.language.style.asm", listOf("net.akehurst.language.style.api", "std", "net.akehurst.language.base.api", "net.akehurst.language.base.asm")) {
                data("StyleSetReferenceDefault") {
                    supertype("StyleSetReference")
                    constructor_ {
                        parameter(setOf(), "localNamespace", "StyleNamespace")
                        parameter(setOf(), "nameOrQName", "PossiblyQualifiedName")
                    }
                    propertyOf(setOf(VAL, REF, STR), "localNamespace", "StyleNamespace", false)
                    propertyOf(setOf(VAL, REF, STR), "nameOrQName", "PossiblyQualifiedName", false)
                    propertyOf(setOf(VAR, REF, STR), "resolved", "StyleSet", false)
                }
                data("StyleNamespaceDefault") {
                    supertype("StyleNamespace")
                    supertype("NamespaceAbstract") { ref("net.akehurst.language.style.api.StyleSet") }
                    constructor_ {
                        parameter(setOf(), "qualifiedName", "QualifiedName")
                        parameter(setOf(), "import", "List")
                    }
                    propertyOf(setOf(VAR, CMP, STR), "import", "List", false) {
                        typeArgument("Import")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "qualifiedName", "QualifiedName", false)
                }
                data("AglStyleSetDefault") {
                    supertype("StyleSet")
                    constructor_ {
                        parameter(setOf(), "namespace", "StyleNamespace")
                        parameter(setOf(), "name", "SimpleName")
                        parameter(setOf(), "extends", "List")
                    }
                    propertyOf(setOf(VAR, CMP, STR), "extends", "List", false) {
                        typeArgument("StyleSetReference")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "name", "SimpleName", false)
                    propertyOf(setOf(VAL, REF, STR), "namespace", "StyleNamespace", false)
                    propertyOf(setOf(VAR, CMP, STR), "rules", "List", false) {
                        typeArgument("AglStyleRule")
                    }
                }
                data("AglStyleRuleDefault") {
                    supertype("AglStyleRule")
                    constructor_ {
                        parameter(setOf(), "selector", "List")
                    }
                    propertyOf(setOf(VAR, CMP, STR), "declaration", "Map", false) {
                        typeArgument("String")
                        typeArgument("AglStyleDeclaration")
                    }
                    propertyOf(setOf(VAR, CMP, STR), "selector", "List", false) {
                        typeArgument("AglStyleSelector")
                    }
                }
                data("AglStyleDomainDefault") {
                    supertype("AglStyleDomain")
                    supertype("DomainAbstract") { ref("net.akehurst.language.style.api.StyleNamespace"); ref("net.akehurst.language.style.api.StyleSet") }
                    constructor_ {
                        parameter(setOf(), "name", "SimpleName")
                        parameter(setOf(), "namespace", "List")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "name", "SimpleName", false)
                    propertyOf(setOf(VAR, CMP, STR), "namespace", "List", false) {
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
                    transToListOf("extends", "StyleSetReference", $$"/* custom SyntaxAnalyser */ $nothing")
                    createObject("rule", "AglStyleRule") { /* custom SyntaxAnalyser */ }
                    createObject("metaRule", "AglStyleMetaRule") { /* custom SyntaxAnalyser */ }
                    createObject("tagRule", "AglStyleTagRule") { /* custom SyntaxAnalyser */ }
                    transToListOf("selectorExpression", "AglStyleSelector", $$"/* custom SyntaxAnalyser */ $nothing")
                    transToListOf("selectorAndComposition", "AglStyleSelector", $$"/* custom SyntaxAnalyser */ $nothing")
                    createObject("selectorSingle", "AglStyleSelector") { /* custom SyntaxAnalyser */ }
                    transToListOf("styleList", "AglStyleDeclaration", $$"/* custom SyntaxAnalyser */ $nothing")
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
            //TODO
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

    override val defaultTargetGrammar by lazy { grammarDomain.findDefinitionByQualifiedNameOrNull(QualifiedName("${NAMESPACE_NAME}.${NAME}"))!! }
    override val defaultTargetGoalRule = "unit"

    override val syntaxAnalyser: SyntaxAnalyser<AglStyleDomain>? by lazy { AglStyleSyntaxAnalyser() }
    override val semanticAnalyser: SemanticAnalyser<AglStyleDomain, SentenceContextAny>? by lazy { AglStyleSemanticAnalyser() }
    override val completionProvider: CompletionProvider<AglStyleDomain, SentenceContextAny>? by lazy { AglStyleCompletionProvider() }
}


