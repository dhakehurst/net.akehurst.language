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

package net.akehurst.language.agl.language.grammar

import net.akehurst.language.agl.language.grammar.asm.GrammarAbstract
import net.akehurst.language.agl.language.grammar.asm.GrammarBuilderDefault
import net.akehurst.language.agl.language.grammar.asm.GrammarOptionDefault
import net.akehurst.language.agl.language.grammar.asm.NamespaceDefault
import net.akehurst.language.api.language.grammar.GrammarRule

internal object AglGrammarGrammar : GrammarAbstract(NamespaceDefault("net.akehurst.language.agl"), "AglGrammar") {
    //companion object {
    const val OPTION_defaultGoalRule = "defaultGoalRule"
    const val goalRuleName = "grammarDefinition"
    private fun createRules(): List<GrammarRule> {
        val b: GrammarBuilderDefault = GrammarBuilderDefault(NamespaceDefault("net.akehurst.language.agl"), "AglGrammar")
        b.skip("WHITESPACE", true).concatenation(b.terminalPattern("\\s+"))
        b.skip("MULTI_LINE_COMMENT", true).concatenation(b.terminalPattern("/\\*[^*]*\\*+([^*/][^*]*\\*+)*/"))
        b.skip("SINGLE_LINE_COMMENT", true).concatenation(b.terminalPattern("//[^\\n\\r]*"))

        b.rule("grammarDefinition").concatenation(b.nonTerminal("namespace"), b.nonTerminal("definitions"))
        b.rule("definitions").multi(1, -1, b.nonTerminal("grammar"))
        b.rule("namespace").concatenation(b.terminalLiteral("namespace"), b.nonTerminal("qualifiedName"))
        b.rule("grammar").concatenation(
            b.terminalLiteral("grammar"), b.nonTerminal("IDENTIFIER"), b.nonTerminal("extendsOpt"),
            b.terminalLiteral("{"), b.nonTerminal("options"), b.nonTerminal("rules"), b.terminalLiteral("}")
        )
        b.rule("options").multi(0, -1, b.nonTerminal("option"))
        b.rule("option").concatenation(b.terminalLiteral("@"), b.nonTerminal("IDENTIFIER"), b.terminalLiteral(":"), b.nonTerminal("value"))
        b.rule("value").choiceLongestFromConcatenationItem(b.nonTerminal("IDENTIFIER"), b.nonTerminal("LITERAL"))
        b.rule("extendsOpt").optional(b.nonTerminal("extends"))
        b.rule("extends").concatenation(b.terminalLiteral("extends"), b.nonTerminal("extendsList"))
        b.rule("extendsList").separatedList(1, -1, b.terminalLiteral(","), b.nonTerminal("qualifiedName"))
        b.rule("rules").multi(1, -1, b.nonTerminal("rule"))
        b.rule("rule").choiceLongestFromConcatenationItem(b.nonTerminal("overrideRule"), b.nonTerminal("grammarRule"), b.nonTerminal("preferenceRule"))
        b.rule("grammarRule").concatenation(b.nonTerminal("ruleTypeLabels"), b.nonTerminal("IDENTIFIER"), b.terminalLiteral("="), b.nonTerminal("rhs"), b.terminalLiteral(";"))
        b.rule("overrideRule").concatenation(
            b.terminalLiteral("override"),
            b.nonTerminal("ruleTypeLabels"),
            b.nonTerminal("IDENTIFIER"),
            b.nonTerminal("overrideOperator"),
            b.nonTerminal("rhs"),
            b.terminalLiteral(";")
        )
        b.rule("overrideOperator").choiceLongestFromConcatenationItem(b.terminalLiteral("=="), b.terminalLiteral("="), b.terminalLiteral("+=|"))
        b.rule("ruleTypeLabels").concatenation(b.nonTerminal("isSkip"), b.nonTerminal("isLeaf"))
        b.rule("isSkip").optional(b.terminalLiteral("skip"))
        b.rule("isLeaf").optional(b.terminalLiteral("leaf"))
        b.rule("rhs").choiceLongestFromConcatenationItem(b.nonTerminal("empty"), b.nonTerminal("concatenation"), b.nonTerminal("choice"))
        b.rule("empty").empty()
        b.rule("choice").choiceLongestFromConcatenationItem(b.nonTerminal("ambiguousChoice"), b.nonTerminal("priorityChoice"), b.nonTerminal("simpleChoice"))
        b.rule("ambiguousChoice").separatedList(2, -1, b.terminalLiteral("||"), b.nonTerminal("concatenation"))
        b.rule("priorityChoice").separatedList(2, -1, b.terminalLiteral("<"), b.nonTerminal("concatenation"))
        b.rule("simpleChoice").separatedList(2, -1, b.terminalLiteral("|"), b.nonTerminal("concatenation"))
        b.rule("concatenation").multi(1, -1, b.nonTerminal("concatenationItem"))
        b.rule("concatenationItem").choiceLongestFromConcatenationItem(b.nonTerminal("simpleItemOrGroup"), b.nonTerminal("listOfItems"))
        b.rule("simpleItemOrGroup").choiceLongestFromConcatenationItem(b.nonTerminal("simpleItem"), b.nonTerminal("group"))
        b.rule("simpleItem")
            .choiceLongestFromConcatenationItem(b.nonTerminal("terminal"), b.nonTerminal("nonTerminal"), b.nonTerminal("embedded"))
        b.rule("listOfItems").choiceLongestFromConcatenationItem(b.nonTerminal("simpleList"), b.nonTerminal("separatedList"))  // TODO: Associative lists
        b.rule("multiplicity").choiceLongestFromConcatenationItem(
            b.terminalLiteral("*"),
            b.terminalLiteral("+"),
            b.terminalLiteral("?"),
            b.nonTerminal("range")
        )
        b.rule("range").choiceLongestFromConcatenationItem(
            b.nonTerminal("rangeUnBraced"),
            b.nonTerminal("rangeBraced"),
        )
        b.rule("rangeUnBraced").concatenation(b.nonTerminal("POSITIVE_INTEGER"), b.nonTerminal("rangeMaxOpt"))
        b.rule("rangeBraced").concatenation(b.terminalLiteral("{"), b.nonTerminal("POSITIVE_INTEGER"), b.nonTerminal("rangeMaxOpt"), b.terminalLiteral("}"))
        b.rule("rangeMaxOpt").optional(b.nonTerminal("rangeMax"))
        b.rule("rangeMax").choiceLongestFromConcatenationItem(
            b.nonTerminal("rangeMaxUnbounded"),
            b.nonTerminal("rangeMaxBounded"),
        )
        b.rule("rangeMaxUnbounded").concatenation(b.terminalLiteral("+"))
        b.rule("rangeMaxBounded").concatenation(b.terminalLiteral(".."), b.nonTerminal("POSITIVE_INTEGER_GT_ZERO"))
        b.rule("simpleList").concatenation(b.nonTerminal("simpleItemOrGroup"), b.nonTerminal("multiplicity"))
        b.rule("separatedList").concatenation(
            b.terminalLiteral("["), b.nonTerminal("simpleItemOrGroup"), b.terminalLiteral("/"),
            b.nonTerminal("simpleItemOrGroup"), b.terminalLiteral("]"), b.nonTerminal("multiplicity")
        )
        b.rule("group").concatenation(b.terminalLiteral("("), b.nonTerminal("groupedContent"), b.terminalLiteral(")"))
        b.rule("groupedContent").choiceLongestFromConcatenationItem(b.nonTerminal("concatenation"), b.nonTerminal("choice"))
        b.rule("nonTerminal").concatenation(b.nonTerminal("qualifiedName"))
        b.rule("embedded").concatenation(b.nonTerminal("qualifiedName"), b.terminalLiteral("::"), b.nonTerminal("nonTerminal"))
        b.rule("qualifiedName").separatedList(1, -1, b.terminalLiteral("."), b.nonTerminal("IDENTIFIER"))
        b.rule("terminal").choiceLongestFromConcatenationItem(b.nonTerminal("LITERAL"), b.nonTerminal("PATTERN"))
        b.leaf("LITERAL").concatenation(b.terminalPattern("'([^'\\\\]|\\\\.)+'"))
        b.leaf("PATTERN").concatenation(b.terminalPattern("\"([^\"\\\\]|\\\\.)+\""))
        b.leaf("IDENTIFIER").concatenation(b.terminalPattern("[a-zA-Z_][a-zA-Z_0-9-]*"))
        b.leaf("POSITIVE_INTEGER").concatenation(b.terminalPattern("[0-9]+"))
        b.leaf("POSITIVE_INTEGER_GT_ZERO").concatenation(b.terminalPattern("[1-9][0-9]*"))

        b.rule("preferenceRule").concatenation(
            b.terminalLiteral("preference"), b.nonTerminal("simpleItem"), b.terminalLiteral("{"),
            b.nonTerminal("preferenceOptionList"),
            b.terminalLiteral("}")
        )
        b.rule("preferenceOptionList").multi(1, -1, b.nonTerminal("preferenceOption"))
        b.rule("preferenceOption")
            .concatenation(b.nonTerminal("nonTerminal"), b.nonTerminal("choiceNumber"), b.terminalLiteral("on"), b.nonTerminal("terminalList"), b.nonTerminal("associativity"))
        b.rule("choiceNumber").optional(b.nonTerminal("POSITIVE_INTEGER"))
        b.rule("terminalList").separatedList(1, -1, b.terminalLiteral(","), b.nonTerminal("simpleItem"))
        b.rule("associativity").choiceLongestFromConcatenationItem(b.terminalLiteral("left"), b.terminalLiteral("right"))
        return b.grammar.grammarRule
    }
    //}

    override val options = listOf(GrammarOptionDefault(OPTION_defaultGoalRule, "grammarDefinition"))
    override val defaultGoalRule: GrammarRule get() = this.findAllResolvedGrammarRule("grammarDefinition")!!

    const val styleStr: String = """'namespace' {
  foreground: darkgreen;
  font-style: bold;
}
'grammar', 'extends', 'override', 'skip', 'leaf' {
  foreground: darkgreen;
  font-style: bold;
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
}"""

    val formatStr = """
namespace net.akehurst.language.agl.AglGrammar {
    Namespace -> 'namespace §qualifiedName'
    Grammar -> 'grammar §name §{extendsOpt()}{
                 §{options.join(§eol)}
                 §{rules.join(§eol)}
               }'
    fun Grammar.extendsOpt() = when {
      extends.isEmpty -> ''
      else -> ': §{extends.join(',')} '
    }
    GrammarReference -> nameOrQName
    GrammarOption -> '@§name §value'
    GrammarRule -> '§{isOverride?'override ':''}§{isSkip?'skip ':''}§{isLeaf?'leaf ':''}§name = §rhs ;'
    PreferenceRule -> ''
    ChoiceLongest -> when {
         2 >= alternative.size -> alternative.join(' | ')
         else -> alternative.join('§eol§indent| ')
    }
    ChoiceAmbiguous -> when {
         2 >= alternative.size -> alternative.join(' || ')
         else -> alternative.join('§eol§indent| ')
    }
    Concatenation -> items.join(' ')
    OptionalItem -> '§{item}?'
    SimpleList -> '§item§{multiplicity()}'
    SeparatedList -> '[ §item / §separator ]§{multiplicity()}'
    fun ListOfItems.multiplicity() = when {
        0==min && 1==max -> '?'
        1==min && -1==max -> '+'
        0==min && -1==max -> '*'
        -1==max -> '§min+'
        else -> '{§min..§max}'
    }
    Group -> '(§groupedContent)'
    EmptyRule -> ''
    Terminal -> when {
        isPattern -> '"value"'
        else '\'§value\''
    }
    NonTerminal -> name
    Embedded -> '§{embeddedGrammarReference}::§{embeddedGoalName}'
}
""".trimIndent().replace("§", "\$")

    init {
        super.grammarRule.addAll(createRules())
    }

    //TODO: gen this from the ASM
    override fun toString(): String = """
namespace net.akehurst.language.agl

grammar AglGrammar {
    skip WHITESPACE = "\s+" ;
    skip MULTI_LINE_COMMENT = "/\*[^*]*\*+(?:[^*`/`][^*]*\*+)*/" ;
    skip SINGLE_LINE_COMMENT = "//[^\n\r]*" ;

    grammarDefinition = namespace definitions ;
    namespace = 'namespace' qualifiedName ;
    definitions = grammar+ ;
    grammar = 'grammar' IDENTIFIER extendsOpt '{' options rules '}' ;
    extendsOpt = extends? ;
    extends = 'extends' extendsList ;
    extendsList = [qualifiedName / ',']+ ;
    options = option* ;
    option = '@' IDENTIFIER ':' value ;
    value = IDENTIFIER | LITERAL ;
    rules = rule+ ;
    rule = grammarRule | overrideRule | preferenceRule ;
    grammarRule = ruleTypeLabels IDENTIFIER '=' rhs ';' ;
    overrideRule = 'override' ruleTypeLabels IDENTIFIER overrideOperator rhs ';' ;
    overrideOperator = '==' | '=' | '+=|' ;
    rhs = empty | concatenation | choice ;
    ruleTypeLabels = 'skip'? 'leaf'? ;
    empty = ;
    choice = ambiguousChoice | priorityChoice | simpleChoice ;
    ambiguousChoice = [ concatenation / '||' ]2+ ;
    priorityChoice = [ concatenation / '<' ]2+ ;
    simpleChoice = [ concatenation / '|' ]2+ ;
    concatenation = concatenationItem+ ;
    concatenationItem = simpleItemOrGroup | listOfItems ;
    simpleItemOrGroup = simpleItem | group ;
    simpleItem = terminal | nonTerminal | embedded ;
    listOfItems = simpleList | separatedList ;
    multiplicity = '*' | '+' | '?' | oneOrMore | range ;
    oneOrMore = POSITIVE_INTEGER '+' ;
    range = POSITIVE_INTEGER '..' POSITIVE_INTEGER ;
    simpleList = simpleItemOrGroup multiplicity ;
    separatedList = '[' simpleItemOrGroup '/' terminal ']' multiplicity ;
    group = '(' groupedContent ')' ;
    groupedContent = concatenation | choice ;
    nonTerminal = qualifiedName ;
    embedded = qualifiedName '::' nonTerminal ;
    terminal = LITERAL | PATTERN ;
    qualifiedName = [IDENTIFIER / '.']+ ;
    leaf IDENTIFIER = "[a-zA-Z_][a-zA-Z_0-9]*";
    leaf LITERAL = "'([^'\\]|\\'|\\\\)*'" ;
    leaf PATTERN = "\"(\\\"|[^\"])*\"" ;
    leaf POSITIVE_INTEGER = "[0-9]+" ;
    
    preferenceRule = 'preference' simpleItem '{' preferenceOptionList '}' ;
    preferenceOptionList = preferenceOption* ;
    preferenceOption = nonTerminal choiceNumber 'on' terminalList associativity ;
    choiceNumber = POSITIVE_INTEGER? ;
    terminalList = [simpleItem / ',']+ ;
    associativity = 'left' | 'right' ;
}
    """.trimIndent()
}

