package test

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.GrammarString
import net.akehurst.language.sppt.api.SpptDataNode
import net.akehurst.language.sppt.api.SpptDataNodeInfo
import net.akehurst.language.sppt.api.SpptWalker
import kotlin.test.Test
import kotlin.test.assertTrue

class test_ParserFromString {

    @Test
    fun parse() {
        val grammarStr = """
namespace CalculatorModelLanguage


grammar CalculatorModelGrammar {

// rules for "Calculator"
Calculator = 'Calculator' identifier
	 InputField*
	 OutputField* ;

InputField = 'input' identifier ;

OutputField = 'output' CalcExpression ;

InputFieldReference = __fre_reference ;

NumberLiteralExpression = stringLiteral ;

CalcExpression = InputFieldReference 
    | LiteralExpression 
    | __fre_binary_CalcExpression ;

LiteralExpression = NumberLiteralExpression  ;

__fre_binary_CalcExpression = [CalcExpression / __fre_binary_operator]2+ ;
leaf __fre_binary_operator = '*' | '+' | '-' | '/' ;

// common rules

__fre_reference = [ identifier / '.' ]+ ;

// white space and comments
skip WHITE_SPACE = "\s+" ;
skip SINGLE_LINE_COMMENT = "//[\n\r]*?" ;
skip MULTI_LINE_COMMENT = "/\*[^*]*\*+(?:[^*`/`][^*]*\*+)*`/`" ;

// the predefined basic types
leaf identifier          = "[a-zA-Z_][a-zA-Z0-9_]*" ;
/* see https://stackoverflow.com/questions/37032620/regex-for-matching-a-string-literal-in-java */
leaf stringLiteral       = '"' "([^'\\]|\\'|\\\\)*" '"' ;
leaf numberLiteral       = "[0-9]+";
leaf booleanLiteral      = 'false' | 'true';

}
        """.trimIndent()

        val res = Agl.processorFromStringSimple(GrammarString(grammarStr))
        println(res.issues.toString())
        assertTrue(res.issues.errors.isEmpty())

        val proc = res.processor ?: error("processor not found")

        val sentence = """
            Calculator a
              input i
              output o
            """.trimIndent()
        val pres = proc.parse(sentence)
        println(pres.issues.toString())
        assertTrue(pres.issues.isEmpty(), res.issues.toString())

        val sppt = pres.sppt ?: error("sppt not found")

        val walker = object : SpptWalker {
            override fun beginTree() {
                println("start of SPPT")
            }

            override fun endTree() {
                println("end of SPPT")
            }

            override fun skip(startPosition: Int, nextInputPosition: Int) {
                println("a skip node: ${startPosition}-${nextInputPosition}")
            }

            override fun leaf(nodeInfo: SpptDataNodeInfo) {
                println("leaf node: ${nodeInfo.node.rule.tag} ${nodeInfo.node.startPosition}-${nodeInfo.node.nextInputPosition}")
            }

            override fun beginBranch(nodeInfo: SpptDataNodeInfo) {
                println("start branch: ${nodeInfo.node.rule.tag}")
            }

            override fun endBranch(nodeInfo: SpptDataNodeInfo) {
                println("end branch: ${nodeInfo.node.rule.tag}")
            }

            override fun beginEmbedded(nodeInfo: SpptDataNodeInfo) {
                println("start embedded")
            }

            override fun endEmbedded(nodeInfo: SpptDataNodeInfo) {
                println("end embedded")
            }

            override fun error(msg: String, path: () -> List<SpptDataNode>) {
                println("error $msg")
            }

        }
        sppt.traverseTreeDepthFirst(walker, false)
    }

}