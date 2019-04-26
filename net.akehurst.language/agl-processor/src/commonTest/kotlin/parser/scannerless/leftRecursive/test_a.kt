package parser.scannerless.leftRecursive

import net.akehurst.language.agl.runtime.structure.RuntimeRuleItem
import net.akehurst.language.agl.runtime.structure.RuntimeRuleItemKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.parser.scannerless.test_ScannerlessParserAbstract
import kotlin.test.Test

class test_a : test_ScannerlessParserAbstract() {

    // S =  'a' | S1 ;
    // S1 = S 'a' ;
    private fun S(): RuntimeRuleSetBuilder {
        val b = RuntimeRuleSetBuilder()
        val r_a = b.literal("a")
        val r_S = b.rule("S").build()
        val r_S1 = b.rule("S1").concatenation(r_S, r_a)
        r_S.rhsOpt = RuntimeRuleItem(RuntimeRuleItemKind.CHOICE_EQUAL, -1, 0, arrayOf(r_a, r_S1))
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

        super.testStringResult(rrb, goal, sentence, expected)
    }


    @Test
    fun aa() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "aa"

        val expected = """
            S { S1 { S { 'a' } 'a' } }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)
    }

    @Test
    fun aaa() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "aaa"

        val expected = """
            S {
                S1 {
                    S {
                        S1 {
                            S { 'a' }
                            'a'
                        }
                    }
                    'a'
                }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun a50() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a".repeat(50)

        val expected = "S { S1 { ".repeat(49) + "S { 'a' }" + "'a' } }".repeat(49)


        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun a150() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a".repeat(150)

        val expected = "S { S1 { ".repeat(149) + "S { 'a' }" + "'a' } }".repeat(149)


        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun a500() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a".repeat(500)

        val expected = "S { S1 { ".repeat(499) + "S { 'a' }" + "'a' } }".repeat(499)


        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun a2000() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a".repeat(2000)

        val expected = "S { S1 { ".repeat(1999) + "S { 'a' }" + "'a' } }".repeat(1999)


        super.test(rrb, goal, sentence, expected)
    }
}