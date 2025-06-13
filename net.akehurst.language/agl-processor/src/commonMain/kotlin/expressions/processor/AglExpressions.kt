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

import net.akehurst.language.agl.format.builder.formatModel
import net.akehurst.language.agl.simple.ContextWithScope
import net.akehurst.language.api.processor.LanguageIdentity
import net.akehurst.language.api.processor.LanguageObjectAbstract
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.processor.AglBase
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.grammar.api.ChoiceIndicator
import net.akehurst.language.grammar.builder.grammarModel
import net.akehurst.language.grammar.processor.AglGrammar
import net.akehurst.language.grammarTypemodel.builder.grammarTypeNamespace
import net.akehurst.language.reference.builder.crossReferenceModel
import net.akehurst.language.style.builder.styleModel
import net.akehurst.language.transform.builder.asmTransform
import net.akehurst.language.typemodel.builder.typeModel

object AglExpressions : LanguageObjectAbstract<Expression, ContextWithScope<Any, Any>>() {
    const val NAMESPACE_NAME = AglBase.NAMESPACE_NAME
    const val NAME = "Expressions"

    override val identity = LanguageIdentity("${NAMESPACE_NAME}.${NAME}")

    override val grammarString = """
        namespace $NAMESPACE_NAME
          grammar $NAME : Base {
            expression
              = rootExpression
              | literalExpression
              | navigationExpression
              | infixExpression
              | tuple
              | object
              | with
              | when
              | cast
              | typeTest
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
            
            object = possiblyQualifiedName constructorArguments assignmentBlock? ;
            constructorArguments = '(' assignmentList ')' ;
        
            tuple = 'tuple' assignmentBlock ;
            assignmentBlock = '{' assignmentList  '}' ;
            assignmentList = assignment* ;
            assignment = propertyName grammarRuleIndex? ':=' expression ;
            propertyName = SPECIAL | IDENTIFIER ;
            grammarRuleIndex = '$' POSITIVE_INTEGER ;
                
            with = 'with' '(' expression ')' expression ;
            
            when = 'when' '{' whenOption+ whenOptionElse '}' ;
            whenOption = expression '->' expression ;
            whenOptionElse = 'else' '->' expression ;
            
            cast = expression 'as' typeReference ;
            typeTest = expression 'is' typeReference ;
            
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
            leaf STRING = "'([^'\\\\]|\\\\.)*'" ;
            leaf POSITIVE_INTEGER = "[0-9]+" ;
          }
      """.trimIndent()

    override val kompositeString = """
        namespace $NAMESPACE_NAME.expressions.api
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
        """.trimIndent()

    override val styleString = """
        namespace $NAMESPACE_NAME
          styles $NAME {
            $$ "'[^']+'" {
              foreground: darkgreen;
              font-weight: bold;
            }
          }
        """.trimIndent()

    override val grammarModel by lazy {
        grammarModel(NAME) {
            namespace(NAMESPACE_NAME) {
                grammar(NAME) {
                    extendsGrammar(AglBase.defaultTargetGrammar.selfReference)
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
                        ref("typeTest")
                        ref("group")
                    }
                    concatenation("rootExpression") {
                        ref("propertyReference")
                    }
                    concatenation("literalExpression") { ref("literal") }
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
                        lit("("); ref("assignmentList"); lit(")");
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
                        ref("propertyName"); opt { ref("grammarRuleIndex") }; lit(":="); ref("expression")
                    }
                    choice("propertyName") {
                        ref("SPECIAL")
                        ref("IDENTIFIER")
                    }
                    concatenation("grammarRuleIndex") { lit("$"); ref("POSITIVE_INTEGER") }
                    concatenation("with") {
                        lit("with"); lit("("); ref("expression"); lit(")"); ref("expression")
                    }
                    concatenation("when") {
                        lit("when"); lit("{"); lst(1, -1) { ref("whenOption") }; ref("whenOptionElse"); lit("}")
                    }
                    concatenation("whenOption") {
                        ref("expression"); lit("->"); ref("expression")
                    }
                    concatenation("whenOptionElse") {
                        lit("else"); lit("->"); ref("expression")
                    }
                    concatenation("cast") { ref("expression"); lit("as"); ref("typeReference") }
                    concatenation("typeTest") { ref("expression"); lit("is"); ref("typeReference") }
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
                    concatenation("lambda") { lit("{"); ref("expression"); lit("}") }
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
                    concatenation("typeReference") { ref("possiblyQualifiedName"); opt { ref("typeArgumentList") }; opt { lit("?") } }
                    concatenation("typeArgumentList") {
                        lit("<"); spLst(1, -1) { ref("typeReference"); lit(",") }; lit(">")
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
                    concatenation("STRING", isLeaf = true) { pat("'([^'\\\\]|\\\\.)*'") }
                    concatenation("POSITIVE_INTEGER", isLeaf = true) { pat("[0-9]+") } //TODO: move this into Base

                    // If we have an 'expression'
                    // ideally graft it into a 'whenOption'
                    // next best thing is to graft into an infix an end it if lh=='->'
                    preference("expression") {
                        optionRight(listOf("infixExpression"), ChoiceIndicator.ITEM, -1, listOf("INFIX_OPERATOR"))
                        // really want to match the 'ER' position ??
                        optionRight(listOf("infixExpression"), ChoiceIndicator.ITEM, -1, listOf("->"))
                        optionRight(listOf("whenOption"), ChoiceIndicator.NONE, -1, listOf("->"))
                    }
                }
            }
        }
    }

    override val typesModel by lazy {
        typeModel(NAME, true, AglBase.typesModel.namespace) {
            grammarTypeNamespace("net.akehurst.language.expressions.api", listOf("std", "net.akehurst.language.base.api")) {
               interface_("WithExpression") {
                    supertype("Expression")
                    propertyOf(setOf(VAL, CMP, STR), "expression", "Expression", false)
                    propertyOf(setOf(VAL, CMP, STR), "withContext", "Expression", false)
                }
                interface_("WhenOptionElse") {

                }
                interface_("WhenOption") {

                    propertyOf(setOf(VAL, CMP, STR), "condition", "Expression", false)
                    propertyOf(setOf(VAL, CMP, STR), "expression", "Expression", false)
                }
                interface_("WhenExpression") {
                    supertype("Expression")
                    propertyOf(setOf(VAR, CMP, STR), "options", "List", false){
                        typeArgument("WhenOption")
                    }
                }
                interface_("TypeTestExpression") {
                    supertype("Expression")
                }
                interface_("TypeReference") {

                }
                interface_("RootExpression") {
                    supertype("Expression")
                }
                interface_("PropertyCall") {
                    supertype("NavigationPart")
                }
                interface_("OnExpression") {
                    supertype("Expression")
                    propertyOf(setOf(VAL, CMP, STR), "expression", "Expression", false)
                }
                interface_("NavigationPart") {

                }
                interface_("NavigationExpression") {
                    supertype("Expression")
                    propertyOf(setOf(VAR, CMP, STR), "parts", "List", false){
                        typeArgument("NavigationPart")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "start", "Expression", false)
                }
                interface_("MethodCall") {
                    supertype("NavigationPart")
                    propertyOf(setOf(VAR, CMP, STR), "arguments", "List", false){
                        typeArgument("Expression")
                    }
                }
                interface_("LiteralExpression") {
                    supertype("Expression")
                }
                interface_("LambdaExpression") {
                    supertype("Expression")
                }
                interface_("InfixExpression") {
                    supertype("Expression")
                    propertyOf(setOf(VAR, CMP, STR), "expressions", "List", false){
                        typeArgument("Expression")
                    }
                }
                interface_("IndexOperation") {
                    supertype("NavigationPart")
                    propertyOf(setOf(VAR, CMP, STR), "indices", "List", false){
                        typeArgument("Expression")
                    }
                }
                interface_("GroupExpression") {
                    supertype("Expression")
                }
                interface_("Expression") {

                }
                interface_("CreateTupleExpression") {
                    supertype("Expression")
                    propertyOf(setOf(VAR, CMP, STR), "propertyAssignments", "List", false){
                        typeArgument("AssignmentStatement")
                    }
                }
                interface_("CreateObjectExpression") {
                    supertype("Expression")
                }
                interface_("CastExpression") {
                    supertype("Expression")
                }
                interface_("AssignmentStatement") {

                    propertyOf(setOf(VAL, CMP, STR), "rhs", "Expression", false)
                }
            }
            namespace("net.akehurst.language.expressions.asm", listOf("net.akehurst.language.expressions.api", "std", "net.akehurst.language.base.api")) {
                data("WithExpressionDefault") {
                    supertype("ExpressionAbstract")
                    supertype("WithExpression")
                    constructor_ {
                        parameter("withContext", "Expression", false)
                        parameter("expression", "Expression", false)
                    }
                    propertyOf(setOf(VAL, CMP, STR), "expression", "Expression", false)
                    propertyOf(setOf(VAL, CMP, STR), "withContext", "Expression", false)
                }
                data("WhenOptionElseDefault") {
                    supertype("WhenOptionElse")
                    constructor_ {
                        parameter("expression", "Expression", false)
                    }
                    propertyOf(setOf(VAL, REF, STR), "expression", "Expression", false)
                }
                data("WhenOptionDefault") {
                    supertype("WhenOption")
                    constructor_ {
                        parameter("condition", "Expression", false)
                        parameter("expression", "Expression", false)
                    }
                    propertyOf(setOf(VAL, CMP, STR), "condition", "Expression", false)
                    propertyOf(setOf(VAL, CMP, STR), "expression", "Expression", false)
                }
                data("WhenExpressionDefault") {
                    supertype("ExpressionAbstract")
                    supertype("WhenExpression")
                    constructor_ {
                        parameter("options", "List", false)
                        parameter("elseOption", "WhenOptionElse", false)
                    }
                    propertyOf(setOf(VAL, REF, STR), "elseOption", "WhenOptionElse", false)
                    propertyOf(setOf(VAR, CMP, STR), "options", "List", false){
                        typeArgument("WhenOption")
                    }
                }
                data("TypeTestExpressionDefault") {
                    supertype("TypeTestExpression")
                    constructor_ {
                        parameter("expression", "Expression", false)
                        parameter("targetType", "TypeReference", false)
                    }
                    propertyOf(setOf(VAL, REF, STR), "expression", "Expression", false)
                    propertyOf(setOf(VAL, REF, STR), "targetType", "TypeReference", false)
                }
                data("TypeReferenceDefault") {
                    supertype("TypeReference")
                    constructor_ {
                        parameter("possiblyQualifiedName", "PossiblyQualifiedName", false)
                        parameter("typeArguments", "List", false)
                        parameter("isNullable", "Boolean", false)
                    }
                    propertyOf(setOf(VAL, REF, STR), "isNullable", "Boolean", false)
                    propertyOf(setOf(VAL, REF, STR), "possiblyQualifiedName", "PossiblyQualifiedName", false)
                    propertyOf(setOf(VAR, REF, STR), "typeArguments", "List", false){
                        typeArgument("TypeReference")
                    }
                }
                data("RootExpressionDefault") {
                    supertype("ExpressionAbstract")
                    supertype("RootExpression")
                    constructor_ {
                        parameter("name", "String", false)
                    }
                    propertyOf(setOf(VAL, REF, STR), "name", "String", false)
                }
                data("PropertyCallDefault") {
                    supertype("PropertyCall")
                    constructor_ {
                        parameter("propertyName", "String", false)
                    }
                    propertyOf(setOf(VAL, REF, STR), "propertyName", "String", false)
                }
                data("OnExpressionDefault") {
                    supertype("ExpressionAbstract")
                    supertype("OnExpression")
                    constructor_ {
                        parameter("expression", "Expression", false)
                    }
                    propertyOf(setOf(VAL, CMP, STR), "expression", "Expression", false)
                    propertyOf(setOf(VAR, REF, STR), "propertyAssignments", "List", false){
                        typeArgument("AssignmentStatement")
                    }
                }
                data("NavigationExpressionDefault") {
                    supertype("ExpressionAbstract")
                    supertype("NavigationExpression")
                    constructor_ {
                        parameter("start", "Expression", false)
                        parameter("parts", "List", false)
                    }
                    propertyOf(setOf(VAR, CMP, STR), "parts", "List", false){
                        typeArgument("NavigationPart")
                    }
                    propertyOf(setOf(VAL, CMP, STR), "start", "Expression", false)
                }
                data("MethodCallDefault") {
                    supertype("MethodCall")
                    constructor_ {
                        parameter("methodName", "String", false)
                        parameter("arguments", "List", false)
                    }
                    propertyOf(setOf(VAR, CMP, STR), "arguments", "List", false){
                        typeArgument("Expression")
                    }
                    propertyOf(setOf(VAL, REF, STR), "methodName", "String", false)
                }
                data("LiteralExpressionDefault") {
                    supertype("ExpressionAbstract")
                    supertype("LiteralExpression")
                    constructor_ {
                        parameter("qualifiedTypeName", "QualifiedName", false)
                        parameter("value", "Any", false)
                    }
                    propertyOf(setOf(VAL, CMP, STR), "qualifiedTypeName", "QualifiedName", false)
                    propertyOf(setOf(VAL, REF, STR), "value", "Any", false)
                }
                data("LambdaExpressionDefault") {
                    supertype("LambdaExpression")
                    constructor_ {
                        parameter("expression", "Expression", false)
                    }
                    propertyOf(setOf(VAL, REF, STR), "expression", "Expression", false)
                }
                data("InfixExpressionDefault") {
                    supertype("InfixExpression")
                    constructor_ {
                        parameter("expressions", "List", false)
                        parameter("operators", "List", false)
                    }
                    propertyOf(setOf(VAR, CMP, STR), "expressions", "List", false){
                        typeArgument("Expression")
                    }
                    propertyOf(setOf(VAR, REF, STR), "operators", "List", false){
                        typeArgument("String")
                    }
                }
                data("IndexOperationDefault") {
                    supertype("IndexOperation")
                    constructor_ {
                        parameter("indices", "List", false)
                    }
                    propertyOf(setOf(VAR, CMP, STR), "indices", "List", false){
                        typeArgument("Expression")
                    }
                }
                data("GroupExpressionDefault") {
                    supertype("GroupExpression")
                    constructor_ {
                        parameter("expression", "Expression", false)
                    }
                    propertyOf(setOf(VAL, REF, STR), "expression", "Expression", false)
                }
                data("ExpressionAbstract") {
                    supertype("Expression")
                    constructor_ {}
                }
                data("CreateTupleExpressionDefault") {
                    supertype("ExpressionAbstract")
                    supertype("CreateTupleExpression")
                    constructor_ {
                        parameter("propertyAssignments", "List", false)
                    }
                    propertyOf(setOf(VAR, CMP, STR), "propertyAssignments", "List", false){
                        typeArgument("AssignmentStatement")
                    }
                }
                data("CreateObjectExpressionDefault") {
                    supertype("ExpressionAbstract")
                    supertype("CreateObjectExpression")
                    constructor_ {
                        parameter("possiblyQualifiedTypeName", "PossiblyQualifiedName", false)
                        parameter("constructorArguments", "List", false)
                    }
                    propertyOf(setOf(VAR, REF, STR), "constructorArguments", "List", false){
                        typeArgument("AssignmentStatement")
                    }
                    propertyOf(setOf(VAL, REF, STR), "possiblyQualifiedTypeName", "PossiblyQualifiedName", false)
                    propertyOf(setOf(VAR, REF, STR), "propertyAssignments", "List", false){
                        typeArgument("AssignmentStatement")
                    }
                }
                data("CastExpressionDefault") {
                    supertype("CastExpression")
                    constructor_ {
                        parameter("expression", "Expression", false)
                        parameter("targetType", "TypeReference", false)
                    }
                    propertyOf(setOf(VAL, REF, STR), "expression", "Expression", false)
                    propertyOf(setOf(VAL, REF, STR), "targetType", "TypeReference", false)
                }
                data("AssignmentStatementDefault") {
                    supertype("AssignmentStatement")
                    constructor_ {
                        parameter("lhsPropertyName", "String", false)
                        parameter("lhsGrammarRuleIndex", "Integer", false)
                        parameter("rhs", "Expression", false)
                    }
                    propertyOf(setOf(VAL, REF, STR), "lhsGrammarRuleIndex", "Integer", false)
                    propertyOf(setOf(VAL, REF, STR), "lhsPropertyName", "String", false)
                    propertyOf(setOf(VAL, CMP, STR), "rhs", "Expression", false)
                }
            }
        }
    }

    override val asmTransformModel by lazy {
        asmTransform(
            name = NAME,
            typeModel = typesModel,
            createTypes = false
        ) {
            namespace(qualifiedName = NAMESPACE_NAME) {
                transform(NAME) {
                    importTypes("net.akehurst.language.expressions.api")
                    createObject("expression","Expression") { /* custom syntax analyser */ }
                    createObject("rootExpression","RootExpression") { /* custom syntax analyser */ }
                    //TODO
                }
            }
        }
    }

    override val crossReferenceModel by lazy {
        crossReferenceModel(NAME) {
            //TODO

        }
    }

    override val formatModel by lazy {
        formatModel(NAME) {
//            TODO("not implemented")
        }
    }

    override val styleModel by lazy {
        styleModel(NAME) {
            namespace(NAMESPACE_NAME) {
                styles(NAME) {
                    metaRule("'[^']+'") {
                        declaration("foreground","darkgreen")
                        declaration("font-weight","bold")
                    }
                }
            }
        }
    }

    override val defaultTargetGrammar by lazy { grammarModel.findDefinitionByQualifiedNameOrNull(QualifiedName("${NAMESPACE_NAME}.${NAME}"))!! }
    override val defaultTargetGoalRule = "expression"

    override val syntaxAnalyser by lazy { ExpressionsSyntaxAnalyser() }
    override val semanticAnalyser by lazy { ExpressionsSemanticAnalyser() }
    override val completionProvider by lazy { ExpressionsCompletionProvider() }


    override fun toString() = "${AglGrammar.NAMESPACE_NAME}.${AglGrammar.NAME}"
}