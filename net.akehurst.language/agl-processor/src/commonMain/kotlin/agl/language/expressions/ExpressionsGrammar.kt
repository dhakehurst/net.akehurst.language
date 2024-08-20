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

package net.akehurst.language.agl.language.expressions

import net.akehurst.language.agl.language.base.BaseGrammar
import net.akehurst.language.agl.language.grammar.AglGrammarGrammar
import net.akehurst.language.agl.language.grammar.asm.*
import net.akehurst.language.api.language.grammar.GrammarRule

internal object ExpressionsGrammar : GrammarAbstract(NamespaceDefault("net.akehurst.language.agl"), "Expressions") {
    const val goalRuleName = "expression"
    private fun createRules(): List<GrammarRule> {
        val b = GrammarBuilderDefault(NamespaceDefault("net.akehurst.language.agl.language"), "Expressions")
        b.extendsGrammar(BaseGrammar)

        b.rule("expression").choiceLongestFromConcatenationItem(
            b.nonTerminal("root"),
            b.nonTerminal("literal"),
            b.nonTerminal("navigation"),
            b.nonTerminal("infix"),
            b.nonTerminal("tuple"),
            b.nonTerminal("object"),
            b.nonTerminal("with"),
            b.nonTerminal("when")
        )
        b.rule("root").choiceLongestFromConcatenationItem(
            b.nonTerminal("propertyReference"),
            b.nonTerminal("SPECIAL"),
        )
        b.rule("literal").choiceLongestFromConcatenationItem(
            b.nonTerminal("BOOLEAN"),
            b.nonTerminal("INTEGER"),
            b.nonTerminal("REAL"),
            b.nonTerminal("STRING"),
        )
        b.rule("navigation").concatenation(
            b.nonTerminal("navigationRoot"),
            b.nonTerminal("navigationPartList")
        )
        b.rule("navigationRoot").choiceLongestFromConcatenationItem(
            b.nonTerminal("root"),
            b.nonTerminal("literal"),
        )
        b.rule("navigationPartList").multi(1, -1, b.nonTerminal("navigationPart"))
        b.rule("navigationPart").choiceLongestFromConcatenationItem(
            b.nonTerminal("propertyCall"),
            b.nonTerminal("methodCall"),
            b.nonTerminal("indexOperation")
        )

        b.rule("infix").separatedList(
            2, -1,
            b.nonTerminal("INFIX_OPERATOR"),
            b.nonTerminal("expression")
        )
        b.leaf("INFIX_OPERATOR").choiceLongestFromConcatenationItem(
            b.terminalLiteral("or"), b.terminalLiteral("and"), b.terminalLiteral("xor"),
            b.terminalLiteral("=="), b.terminalLiteral("!="),
            b.terminalLiteral("<="), b.terminalLiteral(">="), b.terminalLiteral("<"), b.terminalLiteral(">"),
            b.terminalLiteral("/"), b.terminalLiteral("*"), b.terminalLiteral("%"),
            b.terminalLiteral("+"), b.terminalLiteral("-"),
        )

        b.rule("tuple").concatenation(
            b.terminalLiteral("tuple"),
            b.nonTerminal("assignmentBlock"),
        )
        b.rule("assignmentBlock").concatenation(
            b.terminalLiteral("{"),
            b.nonTerminal("assignmentList"),
            b.terminalLiteral("}")
        )
        b.rule("assignmentList").multi(1, -1, b.nonTerminal("assignment"))
        b.rule("assignment").concatenation(
            b.nonTerminal("propertyName"),
            b.terminalLiteral(":="),
            b.nonTerminal("expression"),
        )
        b.rule("propertyName").choiceLongestFromConcatenationItem(
            b.nonTerminal("SPECIAL"),
            b.nonTerminal("IDENTIFIER")
        )

        b.rule("object").concatenation(
            b.nonTerminal("IDENTIFIER"),
            b.terminalLiteral("("),
            b.nonTerminal("argumentList"),
            b.terminalLiteral(")"),
            b.nonTerminal("optAssignmentBlock")
        )
        b.rule("optAssignmentBlock").optional(b.nonTerminal("assignmentBlock"))

        b.rule("with").concatenation(
            b.terminalLiteral("with"),
            b.terminalLiteral("("),
            b.nonTerminal("expression"),
            b.terminalLiteral(")"),
            b.nonTerminal("expression"),
        )

        b.rule("when").concatenation(
            b.terminalLiteral("when"),
            b.terminalLiteral("{"),
            b.nonTerminal("whenOptionList"),
            b.terminalLiteral("}")
        )
        b.rule("whenOptionList").multi(1, -1, b.nonTerminal("whenOption"))
        b.rule("whenOption").concatenation(
            b.nonTerminal("expression"),
            b.terminalLiteral("->"),
            b.nonTerminal("expression"),
        )

        b.rule("propertyCall").concatenation(
            b.terminalLiteral("."),
            b.nonTerminal("propertyReference")
        )
        b.rule("methodCall").concatenation(
            b.terminalLiteral("."),
            b.nonTerminal("IDENTIFIER"),
            b.terminalLiteral("("),
            b.nonTerminal("argumentList"),
            b.terminalLiteral(")"),
        )
        b.rule("indexOperation").concatenation(
            b.terminalLiteral("["),
            b.nonTerminal("indexList"),
            b.terminalLiteral("]"),
        )
        b.rule("argumentList").separatedList(0, -1, b.terminalLiteral(","), b.nonTerminal("expression"))
        b.rule("indexList").separatedList(1, -1, b.terminalLiteral(","), b.nonTerminal("expression"))

        b.rule("propertyReference").choiceLongestFromConcatenationItem(
            b.nonTerminal("SPECIAL"),
            b.nonTerminal("IDENTIFIER")
        )
        b.leaf("SPECIAL").concatenation(b.terminalLiteral("\$"), b.nonTerminal("IDENTIFIER"))
        b.leaf("BOOLEAN").concatenation(b.terminalPattern("true|false"))
        b.leaf("INTEGER").concatenation(b.terminalPattern("[0-9]+"))
        b.leaf("REAL").concatenation(b.terminalPattern("[0-9]+[.][0-9]+"))
        b.leaf("STRING").concatenation(b.terminalPattern("'([^'\\\\]|\\\\'|\\\\\\\\)*'"))

        return b.grammar.grammarRule
    }

    override val options = listOf(GrammarOptionDefault(AglGrammarGrammar.OPTION_defaultGoalRule, goalRuleName))
    override val defaultGoalRule: GrammarRule get() = this.findAllResolvedGrammarRule(goalRuleName)!!

    const val grammarStr = """
namespace net.akehurst.language.agl

grammar Expression extends Base {

    expression
      = root
      | literal
      | navigation
      | infix
      | tuple
      | object
      | with
      | when
      ;
    root = propertyReference | SPECIAL ;
    literal = BOOLEAN | INTEGER | REAL | STRING ;
    
    navigation = navigationRoot navigationPart+ ;
    navigationRoot 
     = root
     | literal
    ;
    navigationPart
     = propertyCall
     | methodCall
     | indexOperation
    ;
    
    infix = [expression / INFIX_OPERATOR]2+ ;
    leaf INFIX_OPERATOR
      = 'or' | 'and' | 'xor'  // logical 
      | '==' | '!=' | '<=' | '>=' | '<' | '>'  // comparison
      | '/' | '*' | '%' | '+' | '-' // arithmetic
      ;
    
    object = IDENTIFIER '(' argumentList ')' assignmentBlock? ;

    tuple = 'tuple' assignmentBlock ;
    assignmentBlock = '{' assignmentList  '}' ;
    assignmentList = assignment* ;
    assignment = propertyName ':=' expression ;
    propertyName = SPECIAL | IDENTIFIER ;
        
    with = 'with' '(' expression ')' expression ;
    
    when = 'when' '{' whenOptionList '}' ;
    whenOptionList = whenOption+ ;
    whenOption = expression '->' expression ;
    
    propertyCall = "." propertyReference ;
    methodCall = "." methodReference '(' argumentList ')' ;
    argumentList = [expression / ',']* ;
    propertyReference = SPECIAL | IDENTIFIER ;
    methodReference = IDENTIFIER ;
    indexOperation = '[' indexList ']' ;
    indexList = [expression / ',']+ ;
    
    leaf SPECIAL = '${"$"}' IDENTIFIER ;
    leaf BOOLEAN = "true|false" ;
    leaf INTEGER = "[0-9]+" ;
    leaf REAL = "[0-9]+[.][0-9]+" ;
    leaf STRING = "'([^'\\]|\\'|\\\\)*'" ;
}
"""

    const val styleStr = """${"$"}keyword {
  foreground: darkgreen;
  font-style: bold;
}"""

    const val formatterStr = """
    """

    init {
        super.extends.add(
            GrammarReferenceDefault(namespace, qualifiedName).also {
                it.resolveAs(BaseGrammar)
            }
        )
        super.grammarRule.addAll(createRules())
    }

    //TODO: gen this from the ASM
    override fun toString(): String = grammarStr.trimIndent()
}