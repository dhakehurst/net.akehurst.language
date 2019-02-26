package parser.scannerless.rightRecursive

import net.akehurst.language.agl.runtime.structure.RuntimeRuleItem
import net.akehurst.language.agl.runtime.structure.RuntimeRuleItemKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.parser.scannerless.test_ScannerlessParserAbstract
import kotlin.test.Test

class test_expession_ambiguous : test_ScannerlessParserAbstract() {

    // S =  I < P < n ;      //  infix < propertyCall < name
    // n = 'a' ;             // "[a-z]+"
    // P = S 'p' n ;         // S '.' name
    // I = S 'o' S ;         // S '+' S
    private fun S(): RuntimeRuleSetBuilder {
        val b = RuntimeRuleSetBuilder()
        val r_n = b.literal("a")
        val r_S = b.rule("S").build()
        val r_P = b.rule("P").concatenation(r_S, b.literal("p"), r_n)
        val r_I = b.rule("I").concatenation(r_S,b.literal("o"), r_S)
        r_S.rhsOpt = RuntimeRuleItem(RuntimeRuleItemKind.CHOICE_PRIORITY, -1, 0, arrayOf(r_n, r_P, r_I))
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
    fun apa() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "apa"

        val expected = """
            S { P { S { 'a' } 'p' 'a' } }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun aoa() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "aoa"

        val expected = """
            S { I { S{'a'} 'o' S{'a'} } }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun aoaoa() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "aoaoa"

        val expected = """
            S {
              I {
                S{'a'}
                'o'
                S{'a'}
              }
            }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)
    }


    @Test
    fun apaoa() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "apaoa"

        val expected = """
             S { I {
                S { P {
                    S { 'a' }
                    'p'
                    'a'
                  } }
                'o'
                S { 'a' }
              } }
        """.trimIndent()


        super.testStringResult(rrb, goal, sentence, expected)
    }

    @Test
    fun aoapa() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "aoapa"

        val expected = """
             S { I {
                  S { 'a' }
                  'o'
                  S { P {
                    S { 'a' }
                    'p'
                    'a'
                  } }
              } }
        """.trimIndent()


        super.testStringResult(rrb, goal, sentence, expected)
    }

    @Test
    fun apaoapaoapa() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "apaoapaoapa"

        val expected = """
             S { I {
                  S { 'a' }
                  'o'
                  S { P {
                    S { 'a' }
                    'p'
                    'a'
                  } }
              } }
        """.trimIndent()


        super.testStringResult(rrb, goal, sentence, expected)
    }
}