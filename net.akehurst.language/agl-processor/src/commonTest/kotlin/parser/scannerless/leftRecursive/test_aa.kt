package parser.scannerless.leftRecursive

import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import kotlin.test.Test

class test_aa {

    // S = P | 'a' ;
    // P = S | P S ;  // S*; try right recursive also
    private fun S(): RuntimeRuleSetBuilder {
        val b = RuntimeRuleSetBuilder()
        val r_a = b.literal("a")
        val r_S = b.rule("S").build()
        val r_P = b.rule("P").choiceEqual(r_S, r_P1)
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

}