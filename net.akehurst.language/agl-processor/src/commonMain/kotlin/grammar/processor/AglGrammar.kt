/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.grammar.processor

import net.akehurst.language.agl.format.builder.formatModel
import net.akehurst.language.agl.simple.ContextWithScope
import net.akehurst.language.api.processor.CompletionProvider
import net.akehurst.language.api.processor.LanguageIdentity
import net.akehurst.language.api.processor.LanguageObjectAbstract
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.processor.AglBase
import net.akehurst.language.formatter.api.AglFormatModel
import net.akehurst.language.grammar.api.Grammar
import net.akehurst.language.grammar.api.GrammarModel
import net.akehurst.language.grammar.api.OverrideKind
import net.akehurst.language.grammar.builder.grammarModel
import net.akehurst.language.grammarTypemodel.builder.grammarTypeNamespace
import net.akehurst.language.reference.api.CrossReferenceModel
import net.akehurst.language.reference.builder.crossReferenceModel
import net.akehurst.language.style.api.AglStyleModel
import net.akehurst.language.style.builder.styleModel
import net.akehurst.language.transform.api.TransformModel
import net.akehurst.language.transform.builder.asmTransform
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.builder.typeModel

object AglGrammar : LanguageObjectAbstract<GrammarModel, ContextWithScope<Any,Any>>() {
    const val OPTION_defaultGoalRule = "defaultGoalRule"

    const val NAMESPACE_NAME = AglBase.NAMESPACE_NAME
    const val NAME = "Grammar"

    override val identity: LanguageIdentity = LanguageIdentity("${NAMESPACE_NAME}.$NAME")

    override val grammarString = """
        namespace $NAMESPACE_NAME
          grammar $NAME : Base {
            override namespace = 'namespace' possiblyQualifiedName option* import* grammar* ;
            grammar = 'grammar' IDENTIFIER extends? '{' option* rule+ '}' ;
            extends = ':' [possiblyQualifiedName / ',']+ ;
            rule = grammarRule | overrideRule | preferenceRule ;
            grammarRule = ruleTypeLabels IDENTIFIER '=' rhs ';' ;
            overrideRule = 'override' ruleTypeLabels IDENTIFIER overrideOperator rhs ';' ;
            overrideOperator = '==' | '=' | '+=|' ;
            ruleTypeLabels = 'skip'? 'leaf'? ;
            rhs = empty | concatenation | choice ;
            empty = /* empty */ ;
            choice = ambiguousChoice | priorityChoice | simpleChoice ;
            ambiguousChoice = [concatenation / '||'] 2+ ;
            priorityChoice = [concatenation / '<'] 2+ ;
            simpleChoice = [concatenation / '|'] 2+ ;
            concatenation = concatenationItem+ ;
            concatenationItem = simpleItemOrGroup | listOfItems ;
            simpleItemOrGroup = simpleItem | group ;
            simpleItem = terminal | nonTerminal | embedded ;
            listOfItems = simpleList | separatedList ;
            multiplicity = '*' | '+' | '?' | range ;
            range = rangeUnBraced | rangeBraced ;
            rangeUnBraced = POSITIVE_INTEGER rangeMax? ;
            rangeBraced = '{' POSITIVE_INTEGER rangeMax? '}' ;
            rangeMax = rangeMaxUnbounded | rangeMaxBounded ;
            rangeMaxUnbounded = '+' ;
            rangeMaxBounded = '..' POSITIVE_INTEGER_GT_ZERO ;
            simpleList = simpleItemOrGroup multiplicity ;
            separatedList = '[' simpleItemOrGroup '/' simpleItemOrGroup ']' multiplicity ;
            group = '(' groupedContent ')' ;
            groupedContent = concatenation | choice ;
            nonTerminal = possiblyQualifiedName ;
            embedded = possiblyQualifiedName '::' nonTerminal ;
            terminal = LITERAL | PATTERN ;
            leaf LITERAL = "'([^'\\\\]|\\\\.)+'" ;
            leaf PATTERN = "\"([^\"\\\\]|\\\\.)+\"" ;
            leaf POSITIVE_INTEGER = "[0-9]+" ;
            leaf POSITIVE_INTEGER_GT_ZERO = "[1-9][0-9]*" ;
            preferenceRule = 'preference' simpleItem '{' preferenceOption+ '}' ;
            preferenceOption = spine choiceNumber? 'on' terminalList associativity ;
            choiceNumber = POSITIVE_INTEGER | CHOICE_INDICATOR ;
            terminalList = [simpleItem / ',']+ ;
            spine = [nonTerminal / '<-']+ ;
            associativity = 'left' | 'right' ;
            leaf CHOICE_INDICATOR = "EMPTY|ITEM" ;
          }
        """.trimIndent()

    override val kompositeString = """
        namespace $NAMESPACE_NAME.grammar.api
            interface Grammar {
                cmp extends
                cmp options
                cmp grammarRule
                cmp preferenceRule
            }
            interface GrammarRule {
                cmp rhs
            }
            interface OverrideRule {
                cmp overridenRhs
            }
            interface PreferenceRule {
                cmp optionList
            }
            interface Choice {
                cmp alternative
            }
            interface Concatenation {
                cmp items
            }
            interface OptionalItem {
                cmp item
            }
            interface ListOfItems {
                cmp item
            }
            interface SeparatedList {
                cmp separator
            }
            interface Group {
                cmp groupedContent
            }
            interface NonTerminal {
                cmp targetGrammar
            }
            interface Embedded {
                cmp embeddedGrammarReference
            }
        """.trimIndent()

    override val styleString: String = """
        namespace $NAMESPACE_NAME
          styles $NAME {
            $$ "'[^']+'" {
              foreground: darkgreen;
              font-weight: bold;
            }
            LITERAL {
              foreground: blue;
            }
            PATTERN {
              foreground: darkblue;
            }
            IDENTIFIER {
              foreground: darkred;
              font-style: italic;
            }
            SINGLE_LINE_COMMENT, MULTI_LINE_COMMENT {
              foreground: LightSlateGrey;
            }
          }
      """.trimIndent()

    override val grammarModel: GrammarModel by lazy {
        grammarModel(NAME) {
            namespace(NAMESPACE_NAME) {
                grammar(NAME) {
                    extendsGrammar(AglBase.defaultTargetGrammar.selfReference)
                    concatenation("namespace", overrideKind = OverrideKind.REPLACE) {
                        lit("namespace"); ref("possiblyQualifiedName")
                        lst(0, -1) { ref("option") }
                        lst(0, -1) { ref("import") }
                        lst(0, -1) { ref("grammar") }
                    }
                    concatenation("grammar") {
                        lit("grammar"); ref("IDENTIFIER"); opt { ref("extends") }; lit("{")
                        lst(0, -1) { ref("option") }
                        lst(1, -1) { ref("rule") }
                        lit("}")
                    }
                    concatenation("extends") {
                        lit(":"); spLst(1, -1) { ref("possiblyQualifiedName"); lit(",") }
                    }
                    choice("rule") {
                        ref("grammarRule")
                        ref("overrideRule")
                        ref("preferenceRule")
                    }
                    concatenation("grammarRule") {
                        ref("ruleTypeLabels"); ref("IDENTIFIER"); lit("="); ref("rhs"); lit(";")
                    }
                    concatenation("overrideRule") {
                        lit("override"); ref("ruleTypeLabels"); ref("IDENTIFIER"); ref("overrideOperator"); ref("rhs"); lit(";")
                    }
                    choice("overrideOperator") {
                        lit("==")
                        lit("=")
                        lit("+=|")
                    }
                    concatenation("ruleTypeLabels") {
                        opt { lit("skip") }; opt { lit("leaf") }
                    }
                    choice("rhs") {
                        ref("empty");
                        ref("concatenation")
                        ref("choice")
                    }
                    empty("empty")
                    choice("choice") {
                        ref("ambiguousChoice")
                        ref("priorityChoice") //TODO: remove this
                        ref("simpleChoice")
                    }
                    separatedList("ambiguousChoice", 2, -1) { ref("concatenation");lit("||") }
                    separatedList("priorityChoice", 2, -1) { ref("concatenation");lit("<") }
                    separatedList("simpleChoice", 2, -1) { ref("concatenation");lit("|") }
                    list("concatenation", 1, -1) { ref("concatenationItem") }
                    choice("concatenationItem") {
                        ref("simpleItemOrGroup")
                        ref("listOfItems")
                    }
                    choice("simpleItemOrGroup") {
                        ref("simpleItem")
                        ref("group")
                    }
                    choice("simpleItem") {
                        ref("terminal")
                        ref("nonTerminal")
                        ref("embedded")
                    }
                    choice("listOfItems") {
                        ref("simpleList")
                        ref("separatedList")
                        // TODO: Associative lists ?
                    }
                    choice("multiplicity") {
                        lit("*")
                        lit("+")
                        lit("?")
                        ref("range")
                    }
                    choice("range") {
                        ref("rangeUnBraced")
                        ref("rangeBraced")
                    }
                    concatenation("rangeUnBraced") {
                        ref("POSITIVE_INTEGER"); opt { ref("rangeMax") }
                    }
                    concatenation("rangeBraced") {
                        lit("{"); ref("POSITIVE_INTEGER"); opt { ref("rangeMax") }; lit("}")
                    }
                    choice("rangeMax") {
                        ref("rangeMaxUnbounded")
                        ref("rangeMaxBounded")
                    }
                    concatenation("rangeMaxUnbounded") { lit("+") }
                    concatenation("rangeMaxBounded") {
                        lit(".."); ref("POSITIVE_INTEGER_GT_ZERO")
                    }
                    concatenation("simpleList") {
                        ref("simpleItemOrGroup"); ref("multiplicity")
                    }
                    concatenation("separatedList") {
                        lit("["); ref("simpleItemOrGroup"); lit("/"); ref("simpleItemOrGroup"); lit("]"); ref("multiplicity")
                    }
                    concatenation("group") {
                        lit("("); ref("groupedContent"); lit(")")
                    }
                    choice("groupedContent") {
                        ref("concatenation")
                        ref("choice")
                    }
                    concatenation("nonTerminal") { ref("possiblyQualifiedName") }
                    concatenation("embedded") {
                        ref("possiblyQualifiedName"); lit("::"); ref("nonTerminal")
                    }
                    choice("terminal") {
                        ref("LITERAL")
                        ref("PATTERN")
                    }
                    concatenation("LITERAL", isLeaf = true) { pat("'([^'\\\\]|\\\\.)+'") }
                    concatenation("PATTERN", isLeaf = true) { pat("\"([^\"\\\\]|\\\\.)+\"") }
                    concatenation("POSITIVE_INTEGER", isLeaf = true) { pat("[0-9]+") }
                    concatenation("POSITIVE_INTEGER_GT_ZERO", isLeaf = true) { pat("[1-9][0-9]*") }

                    concatenation("preferenceRule") {
                        lit("preference"); ref("simpleItem"); lit("{")
                        lst(1, -1) { ref("preferenceOption") }
                        lit("}")
                    }
                    concatenation("preferenceOption") {
                        ref("spine"); opt { ref("choiceNumber") }; lit("on"); ref("terminalList"); ref("associativity")
                    }
                    choice("choiceNumber") {
                        concat { ref("POSITIVE_INTEGER") }
                        concat { ref("CHOICE_INDICATOR") }
                    }
                    separatedList("terminalList", 1, -1) { ref("simpleItem"); lit(",") }
                    separatedList("spine", 1, -1) { ref("nonTerminal"); lit("<-") }
                    choice("associativity") {
                        lit("left")
                        lit("right")
                    }
                    concatenation("CHOICE_INDICATOR", isLeaf = true) { pat("EMPTY|ITEM") }
                }
            }
        }
    }

    /** implemented as kotlin classes **/
    override val typesModel: TypeModel by lazy {
        //TODO: GrammarTypeNamespace?
        typeModel("Grammar", true, AglBase.typesModel.namespace) {
            grammarTypeNamespace("net.akehurst.language.grammar.api", listOf("std", "net.akehurst.language.base.api")) {
                enum("SeparatedListKind", listOf("Flat", "Left", "Right"))
                enum("OverrideKind", listOf("REPLACE", "APPEND_ALTERNATIVE", "SUBSTITUTION"))
                enum("Associativity", listOf("LEFT", "RIGHT"))
                value("GrammarRuleName") {
                    constructor_ {
                        parameter("value", "String", false)
                    }
                    propertyOf(setOf(VAL, REF, STORED), "value", "String", false)
                }
                interface_("Terminal") {
                    supertype("TangibleItem")
                }
                interface_("TangibleItem") {
                    supertype("SimpleItem")
                }
                interface_("SimpleList") {
                    supertype("ListOfItems")
                }
                interface_("SimpleItem") {
                    supertype("ConcatenationItem")
                }
                interface_("SeparatedList") {
                    supertype("ListOfItems")
                    propertyOf(setOf(VAL, CMP, STORED), "separator", "RuleItem", false)
                }
                interface_("RuleItem") {

                }
                interface_("PreferenceRule") {
                    supertype("GrammarItem")
                    propertyOf(setOf(VAR, CMP, STORED), "optionList", "List", false) {
                        typeArgument("PreferenceOption")
                    }
                }
                interface_("PreferenceOption") {
                    supertype("Formatable")
                }
                interface_("OverrideRule") {
                    supertype("GrammarRule")
                }
                interface_("OptionalItem") {
                    supertype("ConcatenationItem")
                    propertyOf(setOf(VAL, CMP, STORED), "item", "RuleItem", false)
                }
                interface_("NormalRule") {
                    supertype("GrammarRule")
                }
                interface_("NonTerminal") {
                    supertype("TangibleItem")
                    propertyOf(setOf(VAL, CMP, STORED), "targetGrammar", "GrammarReference", false)
                }
                interface_("ListOfItems") {
                    supertype("ConcatenationItem")
                    propertyOf(setOf(VAL, CMP, STORED), "item", "RuleItem", false)
                }
                interface_("Group") {
                    supertype("SimpleItem")
                    propertyOf(setOf(VAL, CMP, STORED), "groupedContent", "RuleItem", false)
                }
                interfaceFor("grammarRule","GrammarRule") {
                    supertype("GrammarItem")
                    propertyOf(setOf(VAL, CMP, STORED), "rhs", "RuleItem", false)
                }
                interface_("GrammarReference") {

                }
                interface_("GrammarOption") {

                }
                interface_("GrammarNamespace") {
                    supertype("Namespace") { ref("Grammar") }
                }
                interface_("GrammarModel") {
                    supertype("Model") { ref("GrammarNamespace"); ref("Grammar") }
                }
                interface_("GrammarItem") {
                    supertype("Formatable")
                }
                interface_("Grammar") {
                    supertype("Definition") { ref("Grammar") }
                    propertyOf(setOf(VAR, CMP, STORED), "extends", "List", false) {
                        typeArgument("GrammarReference")
                    }
                    propertyOf(setOf(VAR, CMP, STORED), "grammarRule", "List", false) {
                        typeArgument("GrammarRule")
                    }
                    propertyOf(setOf(VAR, CMP, STORED), "options", "List", false) {
                        typeArgument("GrammarOption")
                    }
                    propertyOf(setOf(VAR, CMP, STORED), "preferenceRule", "List", false) {
                        typeArgument("PreferenceRule")
                    }
                }
                interface_("EmptyRule") {
                    supertype("TangibleItem")
                }
                interface_("Embedded") {
                    supertype("TangibleItem")
                    propertyOf(setOf(VAL, CMP, STORED), "embeddedGrammarReference", "GrammarReference", false)
                }
                interface_("ConcatenationItem") {
                    supertype("RuleItem")
                }
                interface_("Concatenation") {
                    supertype("RuleItem")
                    propertyOf(setOf(VAR, CMP, STORED), "items", "List", false) {
                        typeArgument("RuleItem")
                    }
                }
                interface_("ChoicePriority") {
                    supertype("Choice")
                }
                interface_("ChoiceLongest") {
                    supertype("Choice")
                }
                interface_("ChoiceAmbiguous") {
                    supertype("Choice")
                }
                interface_("Choice") {
                    supertype("RuleItem")
                    propertyOf(setOf(VAR, CMP, STORED), "alternative", "List", false) {
                        typeArgument("RuleItem")
                    }
                }
            }
            namespace("net.akehurst.language.grammar.asm", listOf("net.akehurst.language.grammar.api", "std", "net.akehurst.language.base.api", "net.akehurst.language.base.asm")) {
                data("TerminalDefault") {
                    supertype("TangibleItemAbstract")
                    supertype("Terminal")
                    constructor_ {
                        parameter("value", "String", false)
                        parameter("isPattern", "Boolean", false)
                    }
                    propertyOf(setOf(VAL, REF, STORED), "id", "String", false)
                    propertyOf(setOf(VAL, REF, STORED), "isPattern", "Boolean", false)
                    propertyOf(setOf(VAL, REF, STORED), "value", "String", false)
                }
                data("TangibleItemAbstract") {
                    supertype("SimpleItemAbstract")
                    supertype("TangibleItem")
                    constructor_ {}
                }
                data("SimpleListDefault") {
                    supertype("ListOfItemsAbstract")
                    supertype("SimpleList")
                    constructor_ {
                        parameter("min", "Integer", false)
                        parameter("max", "Integer", false)
                        parameter("item", "RuleItem", false)
                    }
                    propertyOf(setOf(VAL, CMP, STORED), "item", "RuleItem", false)
                    propertyOf(setOf(VAL, REF, STORED), "max", "Integer", false)
                    propertyOf(setOf(VAL, REF, STORED), "min", "Integer", false)
                }
                data("SimpleItemAbstract") {
                    supertype("ConcatenationItemAbstract")
                    supertype("SimpleItem")
                    constructor_ {}
                }
                data("SeparatedListDefault") {
                    supertype("ListOfItemsAbstract")
                    supertype("SeparatedList")
                    constructor_ {
                        parameter("min", "Integer", false)
                        parameter("max", "Integer", false)
                        parameter("item", "RuleItem", false)
                        parameter("separator", "RuleItem", false)
                    }
                    propertyOf(setOf(VAL, CMP, STORED), "item", "RuleItem", false)
                    propertyOf(setOf(VAL, REF, STORED), "max", "Integer", false)
                    propertyOf(setOf(VAL, REF, STORED), "min", "Integer", false)
                    propertyOf(setOf(VAL, CMP, STORED), "separator", "RuleItem", false)
                }
                data("RuleItemAbstract") {
                    supertype("RuleItem")
                    constructor_ {}
                    propertyOf(setOf(VAR, REF, STORED), "index", "List", false) {
                        typeArgument("Integer")
                    }
                }
                data("PreferenceRuleDefault") {
                    supertype("GrammarItemAbstract")
                    supertype("PreferenceRule")
                    constructor_ {
                        parameter("grammar", "Grammar", false)
                        parameter("forItem", "SimpleItem", false)
                        parameter("optionList", "List", false)
                    }
                    propertyOf(setOf(VAL, REF, STORED), "forItem", "SimpleItem", false)
                    propertyOf(setOf(VAL, REF, STORED), "grammar", "Grammar", false)
                    propertyOf(setOf(VAR, CMP, STORED), "optionList", "List", false) {
                        typeArgument("PreferenceOption")
                    }
                }
                data("PreferenceOptionDefault") {
                    supertype("PreferenceOption")
                    constructor_ {
                        parameter("item", "NonTerminal", false)
                        parameter("choiceNumber", "Integer", false)
                        parameter("onTerminals", "List", false)
                        parameter("associativity", "Associativity", false)
                    }
                    propertyOf(setOf(VAL, REF, STORED), "associativity", "Associativity", false)
                    propertyOf(setOf(VAL, REF, STORED), "choiceNumber", "Integer", false)
                    propertyOf(setOf(VAL, REF, STORED), "item", "NonTerminal", false)
                    propertyOf(setOf(VAR, REF, STORED), "onTerminals", "List", false) {
                        typeArgument("SimpleItem")
                    }
                }
                data("OverrideRuleDefault") {
                    supertype("GrammarRuleAbstract")
                    supertype("OverrideRule")
                    constructor_ {
                        parameter("grammar", "Grammar", false)
                        parameter("name", "GrammarRuleName", false)
                        parameter("isSkip", "Boolean", false)
                        parameter("isLeaf", "Boolean", false)
                        parameter("overrideKind", "OverrideKind", false)
                    }
                    propertyOf(setOf(VAL, REF, STORED), "grammar", "Grammar", false)
                    propertyOf(setOf(VAL, REF, STORED), "isLeaf", "Boolean", false)
                    propertyOf(setOf(VAL, REF, STORED), "isOverride", "Boolean", false)
                    propertyOf(setOf(VAL, REF, STORED), "isSkip", "Boolean", false)
                    propertyOf(setOf(VAL, CMP, STORED), "name", "GrammarRuleName", false)
                    propertyOf(setOf(VAL, REF, STORED), "overrideKind", "OverrideKind", false)
                    propertyOf(setOf(VAL, CMP, STORED), "rhs", "RuleItem", false)
                }
                data("OptionalItemDefault") {
                    supertype("ConcatenationItemAbstract")
                    supertype("OptionalItem")
                    constructor_ {
                        parameter("item", "RuleItem", false)
                    }
                    propertyOf(setOf(VAL, CMP, STORED), "item", "RuleItem", false)
                }
                data("NormalRuleDefault") {
                    supertype("GrammarRuleAbstract")
                    supertype("NormalRule")
                    constructor_ {
                        parameter("grammar", "Grammar", false)
                        parameter("name", "GrammarRuleName", false)
                        parameter("isSkip", "Boolean", false)
                        parameter("isLeaf", "Boolean", false)
                    }
                    propertyOf(setOf(VAL, REF, STORED), "grammar", "Grammar", false)
                    propertyOf(setOf(VAL, REF, STORED), "isLeaf", "Boolean", false)
                    propertyOf(setOf(VAL, REF, STORED), "isOverride", "Boolean", false)
                    propertyOf(setOf(VAL, REF, STORED), "isSkip", "Boolean", false)
                    propertyOf(setOf(VAL, CMP, STORED), "name", "GrammarRuleName", false)
                    propertyOf(setOf(VAR, CMP, STORED), "rhs", "RuleItem", false)
                }
                data("NonTerminalDefault") {
                    supertype("TangibleItemAbstract")
                    supertype("NonTerminal")
                    constructor_ {
                        parameter("targetGrammar", "GrammarReference", false)
                        parameter("ruleReference", "GrammarRuleName", false)
                    }
                    propertyOf(setOf(VAL, CMP, STORED), "ruleReference", "GrammarRuleName", false)
                    propertyOf(setOf(VAL, CMP, STORED), "targetGrammar", "GrammarReference", false)
                }
                data("ListOfItemsAbstract") {
                    supertype("ConcatenationItemAbstract")
                    supertype("ListOfItems")
                    constructor_ {}
                }
                data("GroupDefault") {
                    supertype("SimpleItemAbstract")
                    supertype("Group")
                    constructor_ {
                        parameter("groupedContent", "RuleItem", false)
                    }
                    propertyOf(setOf(VAL, CMP, STORED), "groupedContent", "RuleItem", false)
                }
                data("GrammarRuleAbstract") {
                    supertype("GrammarItemAbstract")
                    supertype("GrammarRule")
                    constructor_ {}
                }
                data("GrammarReferenceDefault") {
                    supertype("GrammarReference")
                    constructor_ {
                        parameter("localNamespace", "Namespace", false)
                        parameter("nameOrQName", "PossiblyQualifiedName", false)
                    }
                    propertyOf(setOf(VAL, REF, STORED), "localNamespace", "Namespace", false) {
                        typeArgument("Grammar")
                    }
                    propertyOf(setOf(VAL, REF, STORED), "nameOrQName", "PossiblyQualifiedName", false)
                    propertyOf(setOf(VAR, REF, STORED), "resolved", "Grammar", false)
                }
                data("GrammarOptionDefault") {
                    supertype("GrammarOption")
                    constructor_ {
                        parameter("name", "String", false)
                        parameter("value", "String", false)
                    }
                    propertyOf(setOf(VAL, REF, STORED), "name", "String", false)
                    propertyOf(setOf(VAL, REF, STORED), "value", "String", false)
                }
                data("GrammarNamespaceDefault") {
                    supertype("GrammarNamespace")
                    supertype("NamespaceAbstract") { ref("net.akehurst.language.grammar.api.Grammar") }
                    constructor_ {
                        parameter("qualifiedName", "QualifiedName", false)
                    }
                    propertyOf(setOf(VAL, CMP, STORED), "qualifiedName", "QualifiedName", false)
                }
                data("GrammarModelDefault") {
                    supertype("GrammarModel")
                    supertype("ModelAbstract") { ref("net.akehurst.language.grammar.api.GrammarNamespace"); ref("net.akehurst.language.grammar.api.Grammar") }
                    constructor_ {
                        parameter("name", "SimpleName", false)
                        parameter("namespace", "List", false)
                    }
                    propertyOf(setOf(VAL, CMP, STORED), "name", "SimpleName", false)
                    propertyOf(setOf(VAR, CMP, STORED), "namespace", "List", false) {
                        typeArgument("GrammarNamespace")
                    }
                }
                data("GrammarItemAbstract") {
                    supertype("GrammarItem")
                    constructor_ {}
                }
                data("GrammarDefaultKt") {

                }
                data("GrammarDefault") {
                    supertype("GrammarAbstract")
                    constructor_ {
                        parameter("namespace", "GrammarNamespace", false)
                        parameter("name", "SimpleName", false)
                        parameter("options", "List", false)
                    }
                    propertyOf(setOf(VAR, CMP, STORED), "options", "List", false) {
                        typeArgument("GrammarOption")
                    }
                }
                data("GrammarAbstract") {
                    supertype("Grammar")
                    constructor_ {
                        parameter("namespace", "GrammarNamespace", false)
                        parameter("name", "SimpleName", false)
                    }
                    propertyOf(setOf(VAR, CMP, STORED), "extends", "List", false) {
                        typeArgument("GrammarReference")
                    }
                    propertyOf(setOf(VAR, CMP, STORED), "grammarRule", "List", false) {
                        typeArgument("GrammarRule")
                    }
                    propertyOf(setOf(VAL, CMP, STORED), "name", "SimpleName", false)
                    propertyOf(setOf(VAL, REF, STORED), "namespace", "GrammarNamespace", false)
                    propertyOf(setOf(VAR, CMP, STORED), "preferenceRule", "List", false) {
                        typeArgument("PreferenceRule")
                    }
                }
                data("EmptyRuleDefault") {
                    supertype("TangibleItemAbstract")
                    supertype("EmptyRule")
                    constructor_ {}
                }
                data("EmbeddedDefault") {
                    supertype("TangibleItemAbstract")
                    supertype("Embedded")
                    constructor_ {
                        parameter("embeddedGoalName", "GrammarRuleName", false)
                        parameter("embeddedGrammarReference", "GrammarReference", false)
                    }
                    propertyOf(setOf(VAL, CMP, STORED), "embeddedGoalName", "GrammarRuleName", false)
                    propertyOf(setOf(VAL, CMP, STORED), "embeddedGrammarReference", "GrammarReference", false)
                }
                data("ConcatenationItemAbstract") {
                    supertype("RuleItemAbstract")
                    supertype("ConcatenationItem")
                    constructor_ {}
                }
                data("ConcatenationDefault") {
                    supertype("RuleItemAbstract")
                    supertype("Concatenation")
                    constructor_ {
                        parameter("items", "List", false)
                    }
                    propertyOf(setOf(VAR, CMP, STORED), "items", "List", false) {
                        typeArgument("RuleItem")
                    }
                }
                data("ChoicePriorityDefault") {
                    supertype("ChoiceAbstract")
                    supertype("ChoicePriority")
                    constructor_ {
                        parameter("alternative", "List", false)
                    }
                    propertyOf(setOf(VAR, CMP, STORED), "alternative", "List", false) {
                        typeArgument("RuleItem")
                    }
                }
                data("ChoiceLongestDefault") {
                    supertype("ChoiceAbstract")
                    supertype("ChoiceLongest")
                    constructor_ {
                        parameter("alternative", "List", false)
                    }
                    propertyOf(setOf(VAR, CMP, STORED), "alternative", "List", false) {
                        typeArgument("RuleItem")
                    }
                }
                data("ChoiceAmbiguousDefault") {
                    supertype("ChoiceAbstract")
                    supertype("ChoiceAmbiguous")
                    constructor_ {
                        parameter("alternative", "List", false)
                    }
                    propertyOf(setOf(VAR, CMP, STORED), "alternative", "List", false) {
                        typeArgument("RuleItem")
                    }
                }
                data("ChoiceAbstract") {
                    supertype("RuleItemAbstract")
                    supertype("Choice")
                    constructor_ {
                        parameter("alternative", "List", false)
                    }
                    propertyOf(setOf(VAR, CMP, STORED), "alternative", "List", false) {
                        typeArgument("RuleItem")
                    }
                }
            }
            /*
            grammarTypeNamespace("net.akehurst.language.grammar.asm", listOf("net.akehurst.language.grammar.api", "std", "net.akehurst.language.base.api", "net.akehurst.language.base.asm")) {
                dataType("grammar","GrammarDefault") {
                    supertype("GrammarAbstract")
                    constructor_ {
                        parameter("namespace", "GrammarNamespace", false)
                        parameter("name", "SimpleName", false)
                        parameter("options", "List", false)
                    }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "options", "List", false) {
                        typeArgument("GrammarOption")
                    }
                }
                dataType("rule","GrammarRuleAbstract") {
                    supertype("GrammarItemAbstract")
                    supertype("GrammarRule")
                    constructor_ {}
                }
                dataType("grammarRule","NormalRuleDefault") {
                    supertype("GrammarRuleAbstract")
                    supertype("NormalRule")
                    constructor_ {
                        parameter("grammar", "Grammar", false)
                        parameter("name", "GrammarRuleName", false)
                        parameter("isSkip", "Boolean", false)
                        parameter("isLeaf", "Boolean", false)
                    }
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "grammar", "Grammar", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "isLeaf", "Boolean", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "isOverride", "Boolean", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "isSkip", "Boolean", false)
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "name", "GrammarRuleName", false)
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "rhs", "RuleItem", false)
                }
                dataType("overrideRule","OverrideRuleDefault") {
                    supertype("GrammarRuleAbstract")
                    supertype("OverrideRule")
                    constructor_ {
                        parameter("grammar", "Grammar", false)
                        parameter("name", "GrammarRuleName", false)
                        parameter("isSkip", "Boolean", false)
                        parameter("isLeaf", "Boolean", false)
                        parameter("overrideKind", "OverrideKind", false)
                    }
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "grammar", "Grammar", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "isLeaf", "Boolean", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "isOverride", "Boolean", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "isSkip", "Boolean", false)
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "name", "GrammarRuleName", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "overrideKind", "OverrideKind", false)
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "rhs", "RuleItem", false)
                }
                stringTypeFor("overrideOperator")
                dataType("rhs","RuleItemAbstract") {
                    supertype("RuleItem")
                    constructor_ {}
                    propertyOf(setOf(READ_WRITE, REFERENCE, STORED), "index", "List", false) {
                        typeArgument("Integer")
                    }
                }
                dataType("empty","EmptyRuleDefault") {
                    supertype("TangibleItemAbstract")
                    supertype("EmptyRule")
                    constructor_ {}
                }
                dataType("choice","ChoiceAbstract") {
                    supertype("RuleItemAbstract")
                    supertype("Choice")
                    constructor_ {
                        parameter("alternative", "List", false)
                    }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "alternative", "List", false) {
                        typeArgument("RuleItem")
                    }
                }
                dataType("ambiguousChoice","ChoiceAmbiguousDefault") {
                    supertype("ChoiceAbstract")
                    supertype("ChoiceAmbiguous")
                    constructor_ {
                        parameter("alternative", "List", false)
                    }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "alternative", "List", false) {
                        typeArgument("RuleItem")
                    }
                }
                dataType("priorityChoice","ChoicePriorityDefault") {
                    supertype("ChoiceAbstract")
                    supertype("ChoicePriority")
                    constructor_ {
                        parameter("alternative", "List", false)
                    }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "alternative", "List", false) {
                        typeArgument("RuleItem")
                    }
                }
                dataType("simpleChoice","ChoiceLongestDefault") {
                    supertype("ChoiceAbstract")
                    supertype("ChoiceLongest")
                    constructor_ {
                        parameter("alternative", "List", false)
                    }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "alternative", "List", false) {
                        typeArgument("RuleItem")
                    }
                }
                dataType("concatenation","ConcatenationDefault") {
                    supertype("RuleItemAbstract")
                    supertype("Concatenation")
                    constructor_ {
                        parameter("items", "List", false)
                    }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "items", "List", false) {
                        typeArgument("RuleItem")
                    }
                }
                dataType("concatenationItem","ConcatenationItemAbstract") {
                    supertype("RuleItemAbstract")
                    supertype("ConcatenationItem")
                    constructor_ {}
                }
                dataType("simpleItemOrGroup","SimpleItemAbstract") {
                    supertype("ConcatenationItemAbstract")
                    supertype("SimpleItem")
                    constructor_ {}
                }
                dataType("simpleItem","TangibleItemAbstract") {
                    supertype("SimpleItemAbstract")
                    supertype("TangibleItem")
                    constructor_ {}
                }
                dataType("simpleList","SimpleListDefault") {
                    supertype("ListOfItemsAbstract")
                    supertype("SimpleList")
                    constructor_ {
                        parameter("min", "Integer", false)
                        parameter("max", "Integer", false)
                        parameter("item", "RuleItem", false)
                    }
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "item", "RuleItem", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "max", "Integer", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "min", "Integer", false)
                }
                dataType("separatedList","SeparatedListDefault") {
                    supertype("ListOfItemsAbstract")
                    supertype("SeparatedList")
                    constructor_ {
                        parameter("min", "Integer", false)
                        parameter("max", "Integer", false)
                        parameter("item", "RuleItem", false)
                        parameter("separator", "RuleItem", false)
                    }
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "item", "RuleItem", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "max", "Integer", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "min", "Integer", false)
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "separator", "RuleItem", false)
                }
                dataType("group","GroupDefault") {
                    supertype("SimpleItemAbstract")
                    supertype("Group")
                    constructor_ {
                        parameter("groupedContent", "RuleItem", false)
                    }
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "groupedContent", "RuleItem", false)
                }
                dataType("nonTerminal","NonTerminalDefault") {
                    supertype("TangibleItemAbstract")
                    supertype("NonTerminal")
                    constructor_ {
                        parameter("targetGrammar", "GrammarReference", false)
                        parameter("ruleReference", "GrammarRuleName", false)
                    }
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "ruleReference", "GrammarRuleName", false)
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "targetGrammar", "GrammarReference", false)
                }
                dataType("embedded","EmbeddedDefault") {
                    supertype("TangibleItemAbstract")
                    supertype("Embedded")
                    constructor_ {
                        parameter("embeddedGoalName", "GrammarRuleName", false)
                        parameter("embeddedGrammarReference", "GrammarReference", false)
                    }
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "embeddedGoalName", "GrammarRuleName", false)
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "embeddedGrammarReference", "GrammarReference", false)
                }
                dataType("terminal","TerminalDefault") {
                    supertype("TangibleItemAbstract")
                    supertype("Terminal")
                    constructor_ {
                        parameter("value", "String", false)
                        parameter("isPattern", "Boolean", false)
                    }
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "id", "String", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "isPattern", "Boolean", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "value", "String", false)
                }

                dataType("preferenceRule","PreferenceRuleDefault") {
                    supertype("GrammarItemAbstract")
                    supertype("PreferenceRule")
                    constructor_ {
                        parameter("grammar", "Grammar", false)
                        parameter("forItem", "SimpleItem", false)
                        parameter("optionList", "List", false)
                    }
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "forItem", "SimpleItem", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "grammar", "Grammar", false)
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "optionList", "List", false) {
                        typeArgument("PreferenceOption")
                    }
                }
                dataType("preferenceOption","PreferenceOptionDefault") {
                    supertype("PreferenceOption")
                    constructor_ {
                        parameter("item", "NonTerminal", false)
                        parameter("choiceNumber", "Integer", false)
                        parameter("onTerminals", "List", false)
                        parameter("associativity", "Associativity", false)
                    }
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "associativity", "Associativity", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "choiceNumber", "Integer", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "item", "NonTerminal", false)
                    propertyOf(setOf(READ_WRITE, REFERENCE, STORED), "onTerminals", "List", false) {
                        typeArgument("SimpleItem")
                    }
                }
            }
             */
        }
    }

    override val asmTransformModel: TransformModel by lazy {
        asmTransform(
            name = NAME,
            typeModel = typesModel,
            createTypes = false
        ) {
            namespace(qualifiedName = NAMESPACE_NAME) {
                transform(NAME) {
                    importTypes(
                        "net.akehurst.language.grammar.api",
                        "net.akehurst.language.grammar.asm"
                    )
                    createObject("unit", "GrammarModel") {

                    }
                    //TODO: currently the types are not found in the typemodel
                    //    createObject("unit", "DefinitionBlock") {
                    //        assignment("definitions", "child[1]")
                    //    }
                    /*
                    createObject("grammar", "Grammar") {
                        assignment("namespace", "child[1]")
                        assignment("name", "child[1]")
                        assignment("options", "child[4]")
                    }


                    createObject("embedded", "Embedded") {
                        assignment("embeddedGoalName", "child[2].name")
                        assignment(
                            "embeddedGrammarReference",
                            """
                            GrammarReference {
                                localNamespace := ???
                                nameOrQName := child[0]
                            }
                            """.trimIndent()
                        )
                    }
                    createObject("terminal", "Terminal") {
                        assignment("value", "child[0].dropAtBothEnds(1)")
                        assignment("isPattern", "1 == \$alternative")
                    }

                    transRule("qualifiedName", "String", "children.join()")

                    leafStringRule("LITERAL")
                    leafStringRule("PATTERN")
                    leafStringRule("POSITIVE_INTEGER")
                    leafStringRule("POSITIVE_INTEGER_GT_ZERO")

                     */
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
        formatModel(AglBase.NAME) {
//            TODO("not implemented")
        }
    }

    override val styleModel: AglStyleModel by lazy {
        styleModel(NAME) {
            namespace(NAMESPACE_NAME) {
                styles(NAME) {
                    metaRule("'[^']+'") {
                        declaration("foreground","darkgreen")
                        declaration("font-weight","bold")
                    }
                    tagRule("LITERAL") {
                        declaration("foreground","blue")
                    }
                    tagRule("PATTERN") {
                        declaration("foreground","darkblue")
                    }
                    tagRule("IDENTIFIER") {
                        declaration("foreground","darkred")
                        declaration("font-style","italic")
                    }
                    tagRule("SINGLE_LINE_COMMENT", "MULTI_LINE_COMMENT") {
                        declaration("foreground","LightSlateGrey")
                    }
                }
            }
        }
    }

    val formatStr = $$"""
namespace net.akehurst.language.Grammar {
    Namespace -> 'namespace $possiblyQualifiedName'
    Grammar -> 'grammar $name ${extendsOpt()}{
                 ${options.join($eol)}
                 ${rules.join($eol)}
               }'
    fun Grammar.extendsOpt() = when {
      extends.isEmpty -> ''
      else -> ': ${extends.join(',')} '
    }
    GrammarReference -> nameOrQName
    GrammarOption -> '@$name $value'
    GrammarRule -> '${isOverride?'override ':''}${isSkip?'skip ':''}${isLeaf?'leaf ':''}$name = $rhs ;'
    PreferenceRule -> ''
    ChoiceLongest -> when {
         2 >= alternative.size -> alternative.join(' | ')
         else -> alternative.join('$eol$indent| ')
    }
    ChoiceAmbiguous -> when {
         2 >= alternative.size -> alternative.join(' || ')
         else -> alternative.join('$eol$indent| ')
    }
    Concatenation -> items.join(' ')
    OptionalItem -> '${item}?'
    SimpleList -> '$item${multiplicity()}'
    SeparatedList -> '[ $item / $separator ]${multiplicity()}'
    fun ListOfItems.multiplicity() = when {
        0==min && 1==max -> '?'
        1==min && -1==max -> '+'
        0==min && -1==max -> '*'
        -1==max -> '$min+'
        else -> '{$min..$max}'
    }
    Group -> '($groupedContent)'
    EmptyRule -> ''
    Terminal -> when {
        isPattern -> '"value"'
        else '\'$value\''
    }
    NonTerminal -> name
    Embedded -> '${embeddedGrammarReference}::${embeddedGoalName}'
}
""".trimIndent().replace("$", "\$")

    override val defaultTargetGrammar: Grammar by lazy { grammarModel.findDefinitionByQualifiedNameOrNull(QualifiedName("${NAMESPACE_NAME}.$NAME"))!! }
    override val defaultTargetGoalRule: String = "unit"

    override val syntaxAnalyser: SyntaxAnalyser<GrammarModel> by lazy { AglGrammarSyntaxAnalyser() }
    override val semanticAnalyser: SemanticAnalyser<GrammarModel, ContextWithScope<Any,Any>> by lazy { AglGrammarSemanticAnalyser() }
    override val completionProvider: CompletionProvider<GrammarModel, ContextWithScope<Any,Any>> by lazy { AglGrammarCompletionProvider() }

    override fun toString(): String = "${NAMESPACE_NAME}.$NAME"

}

