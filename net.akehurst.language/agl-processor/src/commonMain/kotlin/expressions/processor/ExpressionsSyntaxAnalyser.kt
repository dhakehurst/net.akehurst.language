/**
 * Copyright (C) 2021 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.expressions.processor

import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserByMethodRegistrationAbstract
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.base.api.Import
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.api.asSimpleName
import net.akehurst.language.base.asm.OptionHolderDefault
import net.akehurst.language.base.processor.BaseSyntaxAnalyser
import net.akehurst.language.collections.toSeparatedList
import net.akehurst.language.expressions.api.*
import net.akehurst.language.expressions.asm.*
import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sppt.api.SpptDataNodeInfo
import net.akehurst.language.types.asm.StdLibDefault

class ExpressionsSyntaxAnalyser : SyntaxAnalyserByMethodRegistrationAbstract<Expression>() {

    override val extendsSyntaxAnalyser: Map<QualifiedName, SyntaxAnalyser<*>> = mapOf(
        QualifiedName("Base") to BaseSyntaxAnalyser()
    )

    override fun registerHandlers() {
        super.register(this::unit)
        super.register(this::namespace)
        super.register(this::definition)
        super.register(this::function)
        super.register(this::parameter)
        super.register(this::expression)
        super.register(this::rootExpression)
        super.register(this::literalExpression)
        super.register(this::navigationExpression)
        super.register(this::navigationRoot)
        super.register(this::navigationPartList)
        super.register(this::navigationPart)
        super.register(this::ternaryConditionExpression)
        super.register(this::infixExpression)
        super.registerFor("object", this::object_)
        super.register(this::functionCall)
        super.register(this::constructorArguments)
        super.register(this::tuple)
        super.register(this::propertyAssignmentBlock)
        super.register(this::propertyAssignment)
        super.register(this::propertyName)
        super.register(this::grammarRuleIndex)
        super.register(this::with)
        super.registerFor("when", this::when_)
        super.register(this::whenOption)
        super.register(this::whenOptionElse)
        super.register(this::cast)
        super.register(this::typeTest)
        super.register(this::group)
        super.register(this::propertyCall)
        super.register(this::methodCall)
        super.register(this::argumentList)
        super.register(this::lambda)
        super.register(this::block)
        super.register(this::variableAssignment)
        super.register(this::variableDefinition)
        super.register(this::propertyReference)
        super.register(this::methodReference)
        super.register(this::indexOperation)
        super.register(this::indexList)
        super.register(this::typeReference)
        super.register(this::typeArgumentList)
        super.register(this::literal)
    }

    data class PropertyValue(
        val asmObject: Any,
        val value: String
    )

    // override unit from BaseSyntaxAnalyser
    // unit = option* namespace* ;
    fun unit(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): ExpressionsDomain {
        val options = children[0] as List<Pair<String, String>>
        val namespace = children[1] as List<ExpressionsNamespace>
        val optHolder = OptionHolderDefault(null, options.toMap())
        namespace.forEach { (it.options as OptionHolderDefault).parent = optHolder }
        val result = ExpressionsDomainDefault(SimpleName("Unit"), optHolder, namespace)
        return result
    }

    // override namespace from BaseSyntaxAnalyser
    fun namespace(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): ExpressionsNamespace {
        val pqn = children[1] as PossiblyQualifiedName
        val options = children[2] as List<Pair<String, String>>
        val import = children[3] as List<Import>
        val definition = children[4] as List<(ns: ExpressionsNamespace) -> FunctionDefinition>

        val optHolder = OptionHolderDefault(null, options.toMap())
        val ns = ExpressionsNamespaceDefault(pqn.asQualifiedName(null), optHolder, import)
        definition.forEach {
            val def = it.invoke(ns)
            ns.addDefinition(def)
        }
        return ns
    }

    // override definition = function ;
    private fun definition(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (ns: ExpressionsNamespace) -> FunctionDefinition =
        children[0] as ((ns: ExpressionsNamespace) -> FunctionDefinition)

    // function := 'fun' IDENTIFIER '(' [parameter sep ',']* ')' (':' typeReference)? '=' expression ;
    private fun function(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (ns: ExpressionsNamespace) -> FunctionDefinition {
        val name = children[1] as String
        val parameters = (children[3] as List<Any>).toSeparatedList<Any, FunctionParameter, String>().items
        val retType = (children[5]  as? List<*>)?.let{ it[1] as TypeReference}
        val body = children[7] as Expression
        return { namespace ->
            FunctionDefinitionDefault(namespace, SimpleName(name), parameters, retType, body)
        }
    }

    // parameter := IDENTIFIER ': typeReference ('=' expression)? ;
    private fun parameter(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): FunctionParameter {
        val name = children[0] as String
        val typeRef = children[2] as TypeReference
        val defaultExpr = children[3] as? Expression
        return FunctionParameterDefault(name, typeRef, defaultExpr)
    }

    // expression = root | literal | navigation | tuple  | object | with | when | cast | typeTest | group | block
    private fun expression(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Expression =
        children[0] as Expression

    // root = propertyReference ;
    private fun rootExpression(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): RootExpression {
        val v = children[0] as String
        return when {
            v.startsWith("\$") -> when (v) {
                RootExpressionDefault.NOTHING.name -> RootExpressionDefault.NOTHING
                RootExpressionDefault.SELF.name -> RootExpressionDefault.SELF
                else -> RootExpressionDefault(v)
            }

            else -> RootExpressionDefault(v)
        }
    }

    // literalExpression = literal ;
    private fun literalExpression(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Expression =
        children[0] as Expression

    // override literal = BOOLEAN | INTEGER | REAL | STRING ;
    private fun literal(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): LiteralExpression = when (nodeInfo.alt.option.asIndex) {
        0 -> LiteralExpressionDefault(StdLibDefault.Boolean.qualifiedTypeName, (children[0] as String).toBoolean())
        1 -> LiteralExpressionDefault(StdLibDefault.Integer.qualifiedTypeName, (children[0] as String).toLong())
        2 -> LiteralExpressionDefault(StdLibDefault.Real.qualifiedTypeName, (children[0] as String).toDouble())
        3 -> LiteralExpressionDefault(StdLibDefault.String.qualifiedTypeName, (children[0] as String).trim('\''))
        else -> error("Internal error: alternative ${nodeInfo.alt.option} not handled for 'literal'")
    }

    // navigation = navigationRoot navigationPartList ;
    private fun navigationExpression(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): NavigationExpressionDefault {
        val navigationRoot = children[0] as Expression
        val parts = children[1] as List<NavigationPart>
        return NavigationExpressionDefault(navigationRoot, parts)
    }

    // navigationRoot = root | literal | functionCall | group;
    private fun navigationRoot(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Expression =
        children[0] as Expression

    // navigationPartList = navigationPart+ ;
    private fun navigationPartList(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<NavigationPart> =
        children as List<NavigationPart>

    // navigationPart = propertyCall | methodCall | indexOperation ;
    private fun navigationPart(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence) =
        children[0]

    // ternaryConditionExpression := expression '?' expression ':' expression ;
    private fun ternaryConditionExpression(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): TernaryConditionExpression {
        val expression = children[0] as Expression
        val trueExpression = children[2] as Expression
        val falseExpression = children[4] as Expression
        return TernaryConditionExpressionDefault(expression, trueExpression, falseExpression)
    }

    // infix = expression INFIX_OPERATOR expression ;
    // infix = [expression / INFIX_OPERATOR]2+ ;
    private fun infixExpression(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): InfixExpression {
        val expressions = children.filterIsInstance<Expression>()
        val operators = children.filterIsInstance<String>()
        return InfixExpressionDefault(expressions, operators)
    }

    // functionCall = IDENTIFIER '(' argumentList ')' ;
    private fun functionCall(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): FunctionCall {
        val n = children[0] as String
        val args = children[2] as List<Expression>
        return FunctionCallDefault(n.asSimpleName, args)
    }

    // object = possiblyQualifiedName constructorArguments assignmentBlock ;
    private fun object_(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): CreateObjectExpression {
        val pqn = children[0] as PossiblyQualifiedName
        val args = children[1] as List<VariableAssignmentStatement>
        val propertyAssignments = children[2] as List<VariableAssignmentStatement>
        val exp = CreateObjectExpressionDefault(pqn, args)
        exp.propertyAssignments = propertyAssignments
        return exp
    }

    // constructorArguments = '(' [ assignment / ',' ]* ')' ;
    private fun constructorArguments(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<VariableAssignmentStatement> =
        (children[1] as List<Any>).toSeparatedList<Any, VariableAssignmentStatement, String>().items

    // tuple = 'tuple' assignmentBlock ;
    private fun tuple(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): CreateTupleExpression {
        val propertyAssignments = children[1] as List<VariableAssignmentStatement>
        return CreateTupleExpressionDefault(propertyAssignments)
    }

    // propertyAssignmentBlock = '{' propertyAssignment*  '}' ;
    private fun propertyAssignmentBlock(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<VariableAssignmentStatement> =
        children[1] as List<VariableAssignmentStatement>

    // propertyAssignment = propertyName  grammarRuleIndex? ':=' expression ;
    private fun propertyAssignment(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): VariableAssignmentStatement {
        val lhsPropertyName = children[0] as String
        val lhsGrammarRuleIndex = children[1] as Int?
        val rhs = children[3] as Expression
        val variable = VariableDefinitionDefault(lhsPropertyName, null) //TODO: should this be reused ?
        return VariableAssignmentStatementDefault(variable, lhsGrammarRuleIndex, rhs) //TODO: should this be reused !?
    }

    // propertyName = IDENTIFIER | SPECIAL
    private fun propertyName(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence) =
        children[0] as String

    // grammarRuleIndex = '$' POSITIVE_INTEGER ;
    private fun grammarRuleIndex(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Int =
        (children[1] as String).toInt()

    // with = 'with' '(' expression ')' expression ;
    private fun with(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): WithExpression {
        val withContext = children[2] as Expression
        val expression = children[4] as Expression
        return WithExpressionDefault(withContext, expression)
    }

    // when = 'when' '{' whenOption+ whenOptionElse '}' ;
    private fun when_(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): WhenExpression {
        val optionList = children[2] as List<WhenOption>
        val whenOptionElse = children[3] as WhenOptionElse
        return WhenExpressionDefault(optionList, whenOptionElse)
    }

    // whenOption = expression '->' expression ;
    private fun whenOption(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): WhenOption {
        val condition = children[0] as Expression
        val expression = children[2] as Expression
        return WhenOptionDefault(condition, expression)
    }

    // whenOption = 'else' '->' expression ;
    private fun whenOptionElse(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): WhenOptionElse {
        val expression = children[2] as Expression
        return WhenOptionElseDefault(expression)
    }

    // cast = expression 'as' typeReference ;
    private fun cast(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): CastExpression {
        val expression = children[0] as Expression
        val pqn = children[2] as TypeReference
        return CastExpressionDefault(expression, pqn)
    }

    // typeTest = expression 'is' typeReference ;
    private fun typeTest(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): TypeTestExpression {
        val expression = children[0] as Expression
        val pqn = children[2] as TypeReference
        return TypeTestExpressionDefault(expression, pqn)
    }

    // group = '(' expression ')' ;
    private fun group(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): GroupExpression {
        val expression = children[1] as Expression
        return GroupExpressionDefault(expression)
    }

    // propertyCall = '.' propertyReference ;
    private fun propertyCall(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): PropertyCall {
        val id = children[1] as String
        return PropertyCallDefault(id)
    }

    // methodCall = '.' methodReference '(' argumentList ')'  ;
    private fun methodCall(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): MethodCall {
        val methodReference = children[1] as String
        val argumentList = children[3] as List<Expression>
        return MethodCallDefault(methodReference, argumentList)
    }

    // argumentList = [expression / ',']* ;
    private fun argumentList(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<Expression> {
        return (children as List<Any>).toSeparatedList<Any, Expression, String>().items
    }

    // lambda = '{' [IDENTIFIER / ',']+ '->' expression '}' ;
    private fun lambda(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): LambdaExpression {
        val idList = (children[1] as List<String>).toSeparatedList<String, String, String>().items
        val expression = children[3] as Expression
        return LambdaExpressionDefault(idList, expression)
    }

    // block = '{' variableAssignment* expression '}' ;
    private fun block(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): StatementBlockExpression {
        val assignments = children[1] as List<VariableAssignmentStatement>
        val expression = children[2] as Expression
        return StatementBlockExpressionDefault(assignments, expression)
    }

    // variableAssignment = variableDefinition ':=' expression ;
    private fun variableAssignment(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): VariableAssignmentStatement {
        val variable = children[0] as VariableDefinition
        val rhs = children[2] as Expression
        return VariableAssignmentStatementDefault(variable, -1, rhs)
    }

    // variableDefinition = IDENTIFIER (':' typeReference)? ;
    private fun variableDefinition(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): VariableDefinition {
        val name = children[0] as String
        val typeRef = (children[1] as? List<*>)?.let { it[1] as TypeReference }
        return VariableDefinitionDefault(name, typeRef)
    }

    // indexOperation = '[' indexList ']' ;
    private fun indexOperation(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): IndexOperation {
        val expr = children[1] as List<Expression>
        return IndexOperationDefault(expr)
    }

    // indexList = [expression / ',']+ ;
    private fun indexList(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<Expression> =
        children.toSeparatedList<Any?, Expression, String>().items

    // propertyReference = IDENTIFIER | SPECIAL
    private fun propertyReference(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence) =
        children[0] as String

    //methodReference = IDENTIFIER ;
    private fun methodReference(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence) =
        children[0] as String

    // typeReference = possiblyQualifiedName typeArgumentList? '?'?;
    private fun typeReference(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): TypeReference {
        val pqn = children[0] as PossiblyQualifiedName
        val targs = (children[1] as List<TypeReference>?) ?: emptyList()
        val isNullable = children[2] != null
        return TypeReferenceDefault(pqn, targs, isNullable)
    }

    // typeArgumentList = '<' [ typeReference / ',']+ '>' ;
    private fun typeArgumentList(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<TypeReference> {
        val list = children[1] as List<Any>
        val slist = list.toSeparatedList<Any, TypeReference, String>()
        return slist.items
    }

}