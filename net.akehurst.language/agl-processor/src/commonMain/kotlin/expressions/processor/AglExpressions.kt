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

package net.akehurst.language.expressions.processor

import net.akehurst.language.base.processor.AglBase
import net.akehurst.language.grammar.asm.ChoiceIndicator
import net.akehurst.language.grammar.builder.grammar
import net.akehurst.language.grammar.processor.AglGrammar
import net.akehurst.language.parser.api.RulePosition
import net.akehurst.language.typemodel.builder.typeModel

object AglExpressions {
    const val goalRuleName = "expression"

    const val grammarStr = """
namespace net.akehurst.language

grammar Expression extends Base {

    expression
      = rootExpression
      | literalExpression
      | navigationExpression
      | infixExpression
      | object
      | tuple
      | with
      | when
      | cast
      | group
      ;
    rootExpression = propertyReference ;
    literalExpression = literal ;
    
    navigationExpression = navigationRoot navigationPart+ ;
    navigationRoot 
     = rootExpression
     | literalExpression
     | group
    ;
    navigationPart
     = propertyCall
     | methodCall
     | indexOperation
    ;
    
    infixExpression = [expression / INFIX_OPERATOR]2+ ;
    leaf INFIX_OPERATOR
      = 'or' | 'and' | 'xor'  // logical 
      | '==' | '!=' | '<=' | '>=' | '<' | '>'  // comparison
      | '/' | '*' | '%' | '+' | '-' // arithmetic
      ;
    
    object = possiblyQualifiedName constructorArgument assignmentBlock? ;
    constructorArguments = '(' argumentList ')' ;

    tuple = 'tuple' assignmentBlock ;
    assignmentBlock = '{' assignmentList  '}' ;
    assignmentList = assignment* ;
    assignment = propertyName ':=' expression ;
    propertyName = SPECIAL | IDENTIFIER ;
        
    with = 'with' '(' expression ')' expression ;
    
    when = 'when' '{' whenOptionList '}' ;
    whenOptionList = whenOption+ ;
    whenOption = expression '->' expression ;
    
    cast = expression 'as' typeReference
    
    group = '(' expression ')' ;
    
    propertyCall = '.' propertyReference ;
    methodCall = '.' methodReference '(' argumentList ')' lambda? ;
    argumentList = [expression / ',']* ;
    
    lambda = '{' expression '}' ;
    
    propertyReference = SPECIAL | IDENTIFIER ;
    methodReference = IDENTIFIER ;
    indexOperation = '[' indexList ']' ;
    indexList = [expression / ',']+ ;
    
    typeReference = possiblyQualifiedName typeArgumentList? '?'?;
    typeArgumentList = '<' [ typeReference / ',']+ '>' ;
    
    literal = BOOLEAN | INTEGER | REAL | STRING ;

    leaf SPECIAL = '${"$"}' IDENTIFIER ;
    leaf BOOLEAN = "true|false" ;
    leaf INTEGER = "[0-9]+" ;
    leaf REAL = "[0-9]+[.][0-9]+" ;
    leaf STRING = "'([^'\\]|\\'|\\\\)*'" ;
}
"""

    val grammar = grammar(
        namespace = "net.akehurst.language",
        name = "Expressions"
    ) {
        extendsGrammar(AglBase.grammar.selfReference)
        choice("expression") {
            ref("rootExpression")
            ref("literalExpression")
            ref("navigationExpression")
            ref("infixExpression")
            ref("tuple")
            ref("object")
            ref("with")
            ref("when")
            ref("cast")
            ref("group")
        }
        concatenation("rootExpression") {
            ref("propertyReference")
        }
        concatenation("literalExpression") { ref("literal")}
        concatenation("navigationExpression") {
            ref("navigationRoot"); lst(1, -1) { ref("navigationPart") }
        }
        choice("navigationRoot") {
            ref("rootExpression")
            ref("literalExpression")
            ref("group")
        }
        choice("navigationPart") {
            ref("propertyCall")
            ref("methodCall")
            ref("indexOperation")
        }
        separatedList("infixExpression", 2, -1) {
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
            ref("possiblyQualifiedName"); ref("constructorArguments"); opt { ref("assignmentBlock") }
        }
        concatenation("constructorArguments") {
            lit("("); ref("argumentList"); lit(")");
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
        concatenation("cast") { ref("expression"); lit("as"); ref("typeReference") }
        concatenation("group") { lit("("); ref("expression"); lit(")") }
        concatenation("propertyCall") {
            lit("."); ref("propertyReference")
        }
        concatenation("methodCall") {
            lit("."); ref("methodReference")
            lit("("); ref("argumentList"); lit(")")
            opt { ref("lambda") }
        }
        separatedList("argumentList", 0, -1) {
            ref("expression"); lit(",")
        }
        concatenation("lambda") {lit("{"); ref("expression"); lit("}")}
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
        concatenation("typeReference") { ref("possiblyQualifiedName"); opt { ref("typeArgumentList") }; opt{lit("?")} }
        concatenation("typeArgumentList") {
            lit("<"); spLst(1,-1){ ref("typeReference"); lit(",")}; lit(">")
        }

        choice("literal") {
            ref("BOOLEAN")
            ref("INTEGER")
            ref("REAL")
            ref("STRING")
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
            optionRight(listOf("infixExpression"), ChoiceIndicator.ITEM,-1, listOf("INFIX_OPERATOR"))
            // really want to match the 'ER' position ??
            optionRight(listOf("infixExpression"), ChoiceIndicator.ITEM,-1, listOf("->"))
            optionRight(listOf("whenOption"), ChoiceIndicator.NONE,-1, listOf("->"))
        }
    }

    //override val options = listOf(GrammarOptionDefault(AglGrammarGrammar.OPTION_defaultGoalRule, goalRuleName))
    //override val defaultGoalRule: GrammarRule get() = this.findAllResolvedGrammarRule(goalRuleName)!!

    const val komposite = """namespace net.akehurst.language.expressions.api
interface CreateTupleExpression {
    cmp propertyAssignments
}
interface CreateObjectExpression {
    cmp arguments
}
interface WithExpression {
    cmp withContext
    cmp expression
}
interface WhenExpression {
    cmp options
}
interface WhenOption {
    cmp condition
    cmp expression
}
interface OnExpression {
    cmp expression
}
interface NavigationExpression {
    cmp start
    cmp parts
}
interface MethodCall {
    cmp arguments
}
interface IndexOperation {
    cmp indices
}
interface AssignmentStatement {
    cmp rhs
}
interface InfixExpression {
    cmp expressions
}
"""

    val typeModel by lazy {
        typeModel("Expressions", true, AglBase.typeModel.namespace) {
            namespace("net.akehurst.language.expressions.api", listOf("std", "net.akehurst.language.base.api")) {
                interfaceType("WithExpression") {
                    supertype("Expression")
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "expression", "Expression", false)
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "withContext", "Expression", false)
                }
                interfaceType("WhenOption") {

                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "condition", "Expression", false)
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "expression", "Expression", false)
                }
                interfaceType("WhenExpression") {
                    supertype("Expression")
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "options", "List", false) {
                        typeArgument("WhenOption")
                    }
                }
                interfaceType("RootExpression") {
                    supertype("Expression")
                }
                interfaceType("PropertyCall") {
                    supertype("NavigationPart")
                }
                interfaceType("OnExpression") {
                    supertype("Expression")
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "expression", "Expression", false)
                }
                interfaceType("NavigationPart") {

                }
                interfaceType("NavigationExpression") {
                    supertype("Expression")
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "parts", "List", false) {
                        typeArgument("NavigationPart")
                    }
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "start", "Expression", false)
                }
                interfaceType("MethodCall") {
                    supertype("NavigationPart")
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "arguments", "List", false) {
                        typeArgument("Expression")
                    }
                }
                interfaceType("LiteralExpression") {
                    supertype("Expression")
                }
                interfaceType("InfixExpression") {
                    supertype("Expression")
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "expressions", "List", false) {
                        typeArgument("Expression")
                    }
                }
                interfaceType("IndexOperation") {
                    supertype("NavigationPart")
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "indices", "List", false) {
                        typeArgument("Expression")
                    }
                }
                interfaceType("Expression") {

                }
                interfaceType("CreateTupleExpression") {
                    supertype("Expression")
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "propertyAssignments", "List", false) {
                        typeArgument("AssignmentStatement")
                    }
                }
                interfaceType("CreateObjectExpression") {
                    supertype("Expression")
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "arguments", "List", false) {
                        typeArgument("Expression")
                    }
                }
                interfaceType("AssignmentStatement") {

                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "rhs", "Expression", false)
                }
            }
            namespace("net.akehurst.language.expressions.asm", listOf("net.akehurst.language.expressions.api", "std", "net.akehurst.language.base.api")) {
                dataType("WithExpressionSimple") {
                    supertype("ExpressionAbstract")
                    supertype("WithExpression")
                    constructor_ {
                        parameter("withContext", "Expression", false)
                        parameter("expression", "Expression", false)
                    }
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "expression", "Expression", false)
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "withContext", "Expression", false)
                }
                dataType("WhenOptionSimple") {
                    supertype("WhenOption")
                    constructor_ {
                        parameter("condition", "Expression", false)
                        parameter("expression", "Expression", false)
                    }
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "condition", "Expression", false)
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "expression", "Expression", false)
                }
                dataType("WhenExpressionSimple") {
                    supertype("ExpressionAbstract")
                    supertype("WhenExpression")
                    constructor_ {
                        parameter("options", "List", false)
                    }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "options", "List", false) {
                        typeArgument("WhenOption")
                    }
                }
                dataType("RootExpressionSimple") {
                    supertype("ExpressionAbstract")
                    supertype("RootExpression")
                    constructor_ {
                        parameter("name", "String", false)
                    }
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "name", "String", false)
                }
                dataType("PropertyCallSimple") {
                    supertype("PropertyCall")
                    constructor_ {
                        parameter("propertyName", "String", false)
                    }
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "propertyName", "String", false)
                }
                dataType("OnExpressionSimple") {
                    supertype("ExpressionAbstract")
                    supertype("OnExpression")
                    constructor_ {
                        parameter("expression", "Expression", false)
                    }
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "expression", "Expression", false)
                    propertyOf(setOf(READ_WRITE, REFERENCE, STORED), "propertyAssignments", "List", false) {
                        typeArgument("AssignmentStatement")
                    }
                }
                dataType("NavigationSimple") {
                    supertype("ExpressionAbstract")
                    supertype("NavigationExpression")
                    constructor_ {
                        parameter("start", "Expression", false)
                        parameter("parts", "List", false)
                    }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "parts", "List", false) {
                        typeArgument("NavigationPart")
                    }
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "start", "Expression", false)
                }
                dataType("MethodCallSimple") {
                    supertype("MethodCall")
                    constructor_ {
                        parameter("methodName", "String", false)
                        parameter("arguments", "List", false)
                    }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "arguments", "List", false) {
                        typeArgument("Expression")
                    }
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "methodName", "String", false)
                }
                dataType("LiteralExpressionSimple") {
                    supertype("ExpressionAbstract")
                    supertype("LiteralExpression")
                    constructor_ {
                        parameter("qualifiedTypeName", "QualifiedName", false)
                        parameter("value", "Any", false)
                    }
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "qualifiedTypeName", "QualifiedName", false)
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "value", "Any", false)
                }
                dataType("InfixExpressionSimple") {
                    supertype("InfixExpression")
                    constructor_ {
                        parameter("expressions", "List", false)
                        parameter("operators", "List", false)
                    }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "expressions", "List", false) {
                        typeArgument("Expression")
                    }
                    propertyOf(setOf(READ_WRITE, REFERENCE, STORED), "operators", "List", false) {
                        typeArgument("String")
                    }
                }
                dataType("IndexOperationSimple") {
                    supertype("IndexOperation")
                    constructor_ {
                        parameter("indices", "List", false)
                    }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "indices", "List", false) {
                        typeArgument("Expression")
                    }
                }
                dataType("ExpressionAbstract") {
                    supertype("Expression")
                    constructor_ {}
                }
                dataType("CreateTupleExpressionSimple") {
                    supertype("ExpressionAbstract")
                    supertype("CreateTupleExpression")
                    constructor_ {
                        parameter("propertyAssignments", "List", false)
                    }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "propertyAssignments", "List", false) {
                        typeArgument("AssignmentStatement")
                    }
                }
                dataType("CreateObjectExpressionSimple") {
                    supertype("ExpressionAbstract")
                    supertype("CreateObjectExpression")
                    constructor_ {
                        parameter("possiblyQualifiedTypeName", "PossiblyQualifiedName", false)
                        parameter("arguments", "List", false)
                    }
                    propertyOf(setOf(READ_WRITE, COMPOSITE, STORED), "arguments", "List", false) {
                        typeArgument("Expression")
                    }
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "possiblyQualifiedTypeName", "PossiblyQualifiedName", false)
                    propertyOf(setOf(READ_WRITE, REFERENCE, STORED), "propertyAssignments", "List", false) {
                        typeArgument("AssignmentStatement")
                    }
                }
                dataType("AssignmentStatementSimple") {
                    supertype("AssignmentStatement")
                    constructor_ {
                        parameter("lhsPropertyName", "String", false)
                        parameter("rhs", "Expression", false)
                    }
                    propertyOf(setOf(READ_ONLY, REFERENCE, STORED), "lhsPropertyName", "String", false)
                    propertyOf(setOf(READ_ONLY, COMPOSITE, STORED), "rhs", "Expression", false)
                }
            }
        }
    }

    const val styleStr = """namespace net.akehurst.language
styles Expressions {
    ${"$"}keyword {
      foreground: darkgreen;
      font-style: bold;
    }
}"""

    const val formatterStr = """
    """

    //TODO: gen this from the ASM
    override fun toString(): String = grammarStr.trimIndent()
}