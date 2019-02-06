package parser.scannerless.whitespace

import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.parser.scannerless.test_ScannerlessParserAbstract
import kotlin.test.Test
import kotlin.test.fail

class test_multi_a_dot : test_ScannerlessParserAbstract() {

    // skip WS = "\s+" ;
    // S = ad* ;
    // ad = a '.' ;
    // a = 'a' ;
    private fun S(): RuntimeRuleSetBuilder {
        val b = RuntimeRuleSetBuilder()
        val r_WS = b.rule("WS").skip(true).concatenation(b.pattern("\\s+"))
        val r_a = b.rule("a").concatenation(b.literal("a"))
        val r_ad = b.rule("ad").concatenation(r_a, b.literal("."))
        b.rule("S").multi(0,-1,r_ad)
        return b
    }
    @Test
    fun a() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a."

        val expected = """
            S {
                ad {
                    a { 'a' }
                    '.'
                }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun aaa() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a.a.a."

        val expected = """
            S {
                ad { a { 'a' } '.' }
                ad { a { 'a' } '.' }
                ad { a { 'a' } '.' }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun asDot_as_aWS() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a. "

        val expected = """
            S {
                ad { a { 'a' } '.' WS { '\s+' : ' ' } }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }

}