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

import net.akehurst.language.agl.grammarTypeModel.grammarTypeModel
import net.akehurst.language.agl.language.asmTransform.asmTransform
import net.akehurst.language.agl.language.base.AglBase
import net.akehurst.language.agl.language.grammar.asm.builder.grammar
import net.akehurst.language.api.language.base.SimpleName

internal object AglGrammar {
    //companion object {
    const val OPTION_defaultGoalRule = "defaultGoalRule"
    const val goalRuleName = "grammarDefinition"

    //override val options = listOf(GrammarOptionDefault(OPTION_defaultGoalRule, "grammarDefinition"))
    //override val defaultGoalRule: GrammarRule get() = this.findAllResolvedGrammarRule("grammarDefinition")!!

    val grammar = grammar(
        namespace = "net.akehurst.language.agl.language",
        name = "Grammar"
    ) {
        extendsGrammar(AglBase.grammar.selfReference)
        concatenation("unit") {
            ref("namespace"); lst(1, -1) { ref("grammar") }
        }
        concatenation("grammar") {
            lit("grammar"); ref("IDENTIFIER"); opt { ref("extends") }; lit("{");
            lst(0, -1) { ref("options") }
            lst(1, -1) { ref("rule") }
            lit("}")
        }
        concatenation("extends") {
            lit("extends"); spLst(1, -1) { ref("qualifiedName") }
        }
        concatenation("option") {
            lit("@"); ref("IDENTIFIER"); lit(":"); ref("value")
        }
        choice("value") {
            ref("IDENTIFIER")
            ref("LITERAL")
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
        concatenation("nonTerminal") { ref("qualifiedName") }
        concatenation("embedded") {
            ref("qualifiedName"); lit("::"); ref("nonTerminal")
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
            ref("nonTerminal"); ref("choiceNumber"); lit("on"); ref("terminalList"); ref("associativity")
        }
        optional("choiceNumber") { ref("POSITIVE_INTEGER") }
        separatedList("terminalList", 1, -1) { ref("simpleItem"); lit(",") }
        choice("associativity") {
            lit("left")
            lit("right")
        }
    }

    /** implemented as kotlin classes **/
    val typeModel = grammarTypeModel(
        namespaceQualifiedName = grammar.qualifiedName.append(SimpleName("asm")).value,
        modelName = "Grammar",
        imports = listOf(),
    ) {
        listTypeOf("unit", "DefinitionBlock")
        dataType("namespace", "Namespace") {
            propertyPrimitiveType("qualifiedName", "String", false, 0)
        }
        dataType("grammar", "Grammar") {

        }
    }

    val asmTransform = asmTransform(
        name = grammar.name.value,
        typeModel = typeModel,
        createTypes = false
    ) {
        namespace(qualifiedName = grammar.qualifiedName.append(SimpleName("transform")).value) {
            transform("Grammar") {
                createObject("unit", "DefinitionBlock") {
                    assignment("definitions", "child[1]")
                }
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
                    assignment("isPattern", "1 == §alternative")
                }

                transRule("qualifiedName", "String", "children.join()")

                leafStringRule("LITERAL")
                leafStringRule("PATTERN")
                leafStringRule("POSITIVE_INTEGER")
                leafStringRule("POSITIVE_INTEGER_GT_ZERO")
            }
        }
    }

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

    //TODO: gen this from the ASM
    override fun toString(): String = """
namespace net.akehurst.language.agl

grammar AglGrammar extends Base {

    unit = namespace grammar+ ;
    grammar = 'grammar' IDENTIFIER extends? '{' option* rule+ '}' ;
    extends = 'extends' [qualifiedName / ',']+ ;
    option = '@' IDENTIFIER ':' value ;
    value = IDENTIFIER | LITERAL ;
    rule = grammarRule | overrideRule | preferenceRule ;
    grammarRule = ruleTypeLabels IDENTIFIER '=' rhs ';' ;
    overrideRule = 'override' ruleTypeLabels IDENTIFIER overrideOperator rhs ';' ;
    overrideOperator = '==' | '=' | '+=|' ;
    ruleTypeLabels = 'skip'? 'leaf'? ;
    rhs = empty | concatenation | choice ;
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
    nonTerminal = qualifiedName ;
    embedded = qualifiedName '::' nonTerminal ;
    terminal = LITERAL | PATTERN ;
    leaf LITERAL = "'([^'\\]|\\'|\\\\)*'" ;
    leaf PATTERN = "\"(\\\"|[^\"])*\"" ;
    leaf POSITIVE_INTEGER = "[0-9]+" ;
    leaf POSITIVE_INTEGER_GT_ZERO = "[1-9][0-9]*" ;
    
    preferenceRule = 'preference' simpleItem '{' preferenceOption* '}' ;
    preferenceOption = nonTerminal choiceNumber 'on' terminalList associativity ;
    choiceNumber = POSITIVE_INTEGER? ;
    terminalList = [simpleItem / ',']+ ;
    associativity = 'left' | 'right' ;
}
    """.trimIndent()
}

