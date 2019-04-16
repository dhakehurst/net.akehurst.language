package parser.scannerless.examples

import net.akehurst.language.agl.runtime.structure.RuntimeRuleItem
import net.akehurst.language.agl.runtime.structure.RuntimeRuleItemKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.parser.scannerless.test_ScannerlessParserAbstract
import kotlin.test.Test

class test_AhoSetiUlman_4_5_5 : test_ScannerlessParserAbstract() {

    // S = CC ;
    // C = cC | d ;
    private fun S(): RuntimeRuleSetBuilder {
        val b = RuntimeRuleSetBuilder()
        val r_c = b.literal("c")
        val r_d = b.literal("d")
        val r_C1 = b.rule("C1").build()
        val r_C = b.rule("C").choiceEqual(r_C1, r_d)
        b.rule(r_C1).concatenation(r_c, r_C)
        val r_S = b.rule("S").concatenation(r_C, r_C)
        return b
    }

    @Test
    fun c() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "c"

        val expected = """
            S { 'c' }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)
    }

    @Test
    fun d() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "d"

        val expected = """
            S { 'd' }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)
    }


    @Test
    fun dd() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "dd"

        val expected = """
            S { C { 'd' } C { 'd' } }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)
    }

}