package net.akehurst.language.examples.simple

import net.akehurst.language.api.processor.Formatter
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.api.processor.LanguageProcessorException
import net.akehurst.language.api.sppt.SPPTBranch
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

            unit = definition* ;
            definition = classDefinition ;
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

class SimpleExampleSyntaxAnalyser  : SyntaxAnalyserAbstract() {
    init {
        register("unit", this::unit as BranchHandler<SimpleExampleUnit>)
        register("definition", this::definition as BranchHandler<Definition>)
        register("classDefinition", this::classDefinition as BranchHandler<ClassDefinition>)
    }
    override fun clear() {
        // do nothing
    }

    override fun <T> transform(sppt: SharedPackedParseTree): T {
        return transform<T>(sppt.root.asBranch, "") as T
    }

    // unit = definition* ;
    fun unit(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): SimpleExampleUnit {
        val definitions = children[0].branchNonSkipChildren.map{
            transform<Definition>(it, arg)
        }
        return SimpleExampleUnit(definitions)
    }

    // definition = classDefinition ;
    fun definition(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): Definition {
        return transform(children[0], arg)
    }

    // classDefinition =
    //                'class' NAME '{'
    //                    propertyDefinition*
    //                    methodDefinition*
    //                '}'
    //            ;
    fun classDefinition(target: SPPTBranch, children: List<SPPTBranch>, arg: Any): ClassDefinition {
        val name = children[0].nonSkipMatchedText
        val classDefinition = ClassDefinition(name)
        //TODO
        return classDefinition
    }
}

class SimpleExampleFormatter : FormatterAbstract() {
    companion object {
        val EOL: String = "\n"
    }
    override fun <T> format(asm: T): String {
        return if (null == asm) {
            throw LanguageProcessorException("Cannot format null value", null)
        } else {
            when (asm) {
                is SimpleExampleUnit -> this.format("", asm)
                else -> throw LanguageProcessorException("Cannot format ${asm}", null)
            }
        }
    }
    fun format(indent: String, asm: SimpleExampleUnit): String {
        return asm.definition.map {
            format(indent, it)
        }.joinToString(separator = EOL, postfix = EOL)
    }
    fun format(indent: String, asm: Definition): String {
        return when(asm) {
            is ClassDefinition -> format(indent, asm)
            else -> throw RuntimeException("Unknown subtype of Definition")
        }
    }
    fun format(indent: String, asm: ClassDefinition): String {
        //TODO:
        return """
            class ${asm.name} {
            }
        """.trimIndent()
    }
}