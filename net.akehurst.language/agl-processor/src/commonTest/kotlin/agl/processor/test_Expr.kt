package agl.processor

import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.agl.processor.test_ProcessorAbstract
import kotlin.test.Test

class test_Expr : test_ProcessorAbstract() {

    companion object {
        val grammarStr = """
namespace test.d
grammar Test {
    skip WS = "\s+" ;
    expr
      = value
      | var
      ;

    var = NAME;
    value = INT ;

    leaf INT = "[0-9]+";
    leaf NAME = "[a-z]+";
}
        """.trimIndent()

        val processor = Agl.processorFromString(grammarStr)
    }

    @Test
    fun f() {
        val text = "a"

        val expected = """
             expr|1 { var { NAME : 'a' } }
        """.trimIndent()

        super.test(processor,"expr", text, expected)
    }

}