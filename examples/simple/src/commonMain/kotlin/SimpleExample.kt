package net.akehurst.language.examples.simple

import net.akehurst.language.api.processor.Formatter
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.api.sppt2ast.SyntaxAnalyser
import net.akehurst.language.processor.*
import kotlin.js.JsName

object SimpleExample {

    val grammarStr = """
        namespace net.akehurst.language.examples.simple
        grammar SimpleExample {
        
            skip WHITE_SPACE = "\s+" ;
	        skip MULTI_LINE_COMMENT = "/\*[^*]*\*+(?:[^*/][^*]*\*+)*/" ;
	        skip SINGLE_LINE_COMMENT = "//.*?${'$'}" ;

            unit = classDefinition ;
            classDefinition =
                'class' NAME '{'
                    propertyDefinition*
                    methodDefinition*
                '}'
            ;
            
            propertyDefinition = NAME ':' NAME ;
            methodDefinition = NAME '(' parameterList ')' body ;
            parameterList = [ parameterDefinition / ',']* ;
            parameterDefinition = NAME ':' NAME ;
            body = '{' statement* '}' ;
            statement =

                 statementAssignment
                ;
            statementAssignment = NAME ':=' expression ;
            expression =
                  expresionLiteral
                | expressionVariableReference
                | expressionInfix
                | expressionGroup
                ;
            expresionLiteral = BOOLEAN | NUMBER | STRING ;
            expressionVariableReference = NAME ;
            expressionInfix = [ expression / OPERATOR ]2+ ;
            OPERATOR = '+' | '-' | '*' | '/' ;
            expressionGroup = '(' expression ')' ;
            
            NAME = "[a-zA-Z_][a-zA-Z0-9_]*" ;
            BOOLEAN = "true | false" ;
            NUMBER = "[0-9]+([.][0-9]+)?" ;
            STRING = "'(?:\\?.)*?'" ;
                              
        }
    """

    @JsName("processor")
    val processor: LanguageProcessor by lazy {
        Agl.processor(
                grammarStr,
                SimpleExampleSyntaxAnalyser(),
                SimpleExampleFormatter()
        )
    }

}

class SimpleExampleSyntaxAnalyser : SyntaxAnalyser {
    override fun clear() {
        TODO("not implemented")
    }

    override fun <T> transform(sppt: SharedPackedParseTree): T {
        TODO("not implemented")
    }

}

class SimpleExampleFormatter : Formatter {

    override fun <T> format(asm: T): String {
        TODO("not implemented")
    }
}