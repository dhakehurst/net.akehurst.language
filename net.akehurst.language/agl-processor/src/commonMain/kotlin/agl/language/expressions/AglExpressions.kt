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

import net.akehurst.language.agl.language.base.AglBase
import net.akehurst.language.agl.language.grammar.asm.builder.grammar
import net.akehurst.language.agl.runtime.structure.RulePosition

internal object AglExpressions {
    const val goalRuleName = "expression"

    val grammar = grammar(
        namespace = "net.akehurst.language.agl.language",
        name = "Expressions"
    ) {
        extendsGrammar(AglBase.grammar.selfReference)
        choice("expression") {
            ref("root")
            ref("literal")
            ref("navigation")
            ref("infix")
            ref("tuple")
            ref("object")
            ref("with")
            ref("when")
        }
        concatenation("root") {
            ref("propertyReference")
        }
        choice("literal") {
            ref("BOOLEAN")
            ref("INTEGER")
            ref("REAL")
            ref("STRING")
        }
        concatenation("navigation") {
            ref("navigationRoot"); lst(1, -1) { ref("navigationPart") }
        }
        choice("navigationRoot") {
            ref("root")
            ref("literal")
        }
        choice("navigationPart") {
            ref("propertyCall")
            ref("methodCall")
            ref("indexOperation")
        }
        separatedList("infix", 2, -1) {
            ref("expression"); ref("INFIX_OPERATOR")
        }
        choice("INFIX_OPERATOR", isLeaf = true) {
            // logical
            lit("or"); lit("and"); lit("xor")
            // comparison
            lit("=="); lit("!="); lit("<="); lit(">="); lit("<"); lit(">");
            // arithmetic
            lit("/"); lit("*"); lit("%"); lit("+"); lit("-");
        }
        concatenation("object") {
            ref("IDENTIFIER"); lit("("); ref("argumentList"); lit(")"); opt { ref("assignmentBlock") }
        }
        concatenation("tuple") {
            lit("tuple"); ref("assignmentBlock")
        }
        concatenation("assignmentBlock") {
            lit("{"); ref("assignmentList"); lit("}")
        }
        list("assignmentList", 0, -1) {
            ref("assignment")
        }
        concatenation("assignment") {
            ref("propertyName"); lit(":="); ref("expression")
        }
        choice("propertyName") {
            ref("SPECIAL")
            ref("IDENTIFIER")
        }
        concatenation("with") {
            lit("with"); lit("("); ref("expression"); lit(")"); ref("expression")
        }
        concatenation("when") {
            lit("when"); lit("{"); ref("whenOptionList"); lit("}")
        }
        list("whenOptionList", 1, -1) {
            ref("whenOption")
        }
        concatenation("whenOption") {
            ref("expression"); lit("->"); ref("expression")
        }
        concatenation("propertyCall") {
            lit("."); ref("propertyReference")
        }
        concatenation("methodCall") {
            lit("."); ref("methodReference"); lit("("); ref("argumentList"); lit(")")
        }
        separatedList("argumentList", 0, -1) {
            ref("expression"); lit(",")
        }
        choice("propertyReference") {
            ref("SPECIAL")
            ref("IDENTIFIER")
        }
        concatenation("methodReference") { ref("IDENTIFIER") }
        concatenation("indexOperation") {
            lit("["); ref("indexList"); lit("]")
        }
        separatedList("indexList", 1, -1) {
            ref("expression"); lit(",")
        }

        concatenation("SPECIAL", isLeaf = true) { lit("$"); ref("IDENTIFIER") }
        concatenation("BOOLEAN", isLeaf = true) { pat("true|false") }
        concatenation("INTEGER", isLeaf = true) { pat("[0-9]+") }
        concatenation("REAL", isLeaf = true) { pat("[0-9]+[.][0-9]+") }
        concatenation("STRING", isLeaf = true) { pat("'([^'\\\\]|\\\\'|\\\\\\\\)*'") }

        // If we have an 'expression'
        // ideally graft it into a 'whenOption'
        // next best thing is to graft into an infix an end it if lh=='->'
        preference("expression") {
            optionRight("infix", RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, listOf("INFIX_OPERATOR"))
            // really want to match the 'ER' position ??
            optionRight("infix", RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, listOf("->"))
            optionRight("whenOption", 0, listOf("->"))
        }
    }

    //override val options = listOf(GrammarOptionDefault(AglGrammarGrammar.OPTION_defaultGoalRule, goalRuleName))
    //override val defaultGoalRule: GrammarRule get() = this.findAllResolvedGrammarRule(goalRuleName)!!

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
    root = propertyReference ;
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
    
    propertyCall = '.' propertyReference ;
    methodCall = '.' methodReference '(' argumentList ')' ;
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

//    init {
//        super.extends.add(
//            GrammarReferenceDefault(namespace, qualifiedName).also {
//                it.resolveAs(BaseGrammar)
//            }
//        )
//        super.grammarRule.addAll(createGrammarRules())
//        super.preferenceRule.addAll(createPreferenceRules())
//    }

    //TODO: gen this from the ASM
    override fun toString(): String = grammarStr.trimIndent()
}