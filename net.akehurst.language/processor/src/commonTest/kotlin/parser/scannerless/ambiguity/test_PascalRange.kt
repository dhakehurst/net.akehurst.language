package parser.scannerless.ambiguity

import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.ogl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.parser.scannerless.test_ScannerlessParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class test_PascalRange : test_ScannerlessParserAbstract() {

    /*
     * expr : range | real ;
     * range: integer '..' integer ;
     * integer : "[0-9]+" ;
     * real : "([0-9]+[.][0-9]*)|([.][0-9]+)" ;
     *
     */
    private fun S(): RuntimeRuleSetBuilder {
        val rrb = RuntimeRuleSetBuilder()
        val r_real = rrb.rule("real").concatenation(rrb.pattern("([0-9]+[.][0-9]*)|([.][0-9]+)"))
        val r_integer = rrb.rule("integer").concatenation(rrb.pattern("[0-9]+"))
        val r_range = rrb.rule("range").concatenation(r_integer, rrb.literal(".."), r_integer)
        val r_expr = rrb.rule("expr").choiceEqual(r_range, r_real)
        return rrb
    }

    @Test
    fun expr_empty() {
        val rrb = this.S()
        val goal = "expr"
        val sentence = ""

        val e = assertFailsWith(ParseFailedException::class) {
            super.test(rrb, goal, sentence)
        }
        assertEquals(1, e.location.line)
        assertEquals(0, e.location.column)
    }

    @Test
    fun expr_1() {
        val rrb = this.S()
        val goal = "expr"
        val sentence = "1."

        val expected1 = """
            expr {
              real { '([0-9]+[.][0-9]*)|([.][0-9]+)' : '1.' }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected1)

    }

    @Test
    fun expr_1_to_5() {
        val rrb = this.S()
        val goal = "expr"
        val sentence = "1..5"

        val expected1 = """
            expr {
              range {
                integer { '[0-9]+' : '1' }
                '..'
                integer { '[0-9]+' : '5' }
              }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected1)

    }
}