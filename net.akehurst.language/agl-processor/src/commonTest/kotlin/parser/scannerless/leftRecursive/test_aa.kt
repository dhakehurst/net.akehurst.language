package parser.scannerless.leftRecursive

import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.parser.scannerless.test_ScannerlessParserAbstract
import kotlin.test.Test

class test_aa : test_ScannerlessParserAbstract() {

    // S  = P | 'a' ;
    // P  = S | P1 ;  // S*
    // P1 = P S ;    // S*; try right recursive also
    private fun S(): RuntimeRuleSetBuilder {
        val b = RuntimeRuleSetBuilder()
        val r_a = b.literal("a")
        val r_S = b.rule("S").build()
        val r_P1 = b.rule("P1").build()
        val r_P = b.rule("P").choiceEqual(r_S, r_P1)
        b.rule(r_P1).concatenation(r_P, r_S)
        b.rule(r_S).choiceEqual(r_P, r_a)
        return b
    }

    @Test
    fun a() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a"

        val expected = """
            S { 'a' }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun aa() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "aa"

        val expected = """
            S { 'a' }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)
    }

}