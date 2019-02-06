package parser.scannerless.whitespace

import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.parser.scannerless.test_ScannerlessParserAbstract
import kotlin.test.Test

class test_a : test_ScannerlessParserAbstract() {

    // skip WS = "\s+" ;
    // S = 'a' ;
    private fun S(): RuntimeRuleSetBuilder {
        val b = RuntimeRuleSetBuilder()
        val r_WS = b.rule("WS").skip(true).concatenation(b.pattern("\\s+"))
        b.rule("S").concatenation(b.literal("a"))
        return b
    }

    @Test
    fun S_S_a() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a"

        val expected = """
            S { 'a' }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)
    }

    @Test
    fun WSa() {
        val rrb = this.S()
        val goal = "S"
        val sentence = " a"

        val expected = """
            S { WS { '\s+' : ' ' } 'a' }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)
    }

    @Test
    fun aWS() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a "

        val expected = """
            S { 'a' WS { '\s+' : ' ' } }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)
    }

    @Test
    fun WSaWS() {
        val rrb = this.S()
        val goal = "S"
        val sentence = " a "

        val expected = """
            S { WS { '\s+' : ' ' } 'a' WS { '\s+' : ' ' } }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)
    }

}