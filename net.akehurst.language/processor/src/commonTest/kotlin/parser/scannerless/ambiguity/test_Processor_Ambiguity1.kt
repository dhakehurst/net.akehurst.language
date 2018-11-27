package net.akehurst.language.processor.processor.ambiguity

import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.ogl.runtime.structure.RuntimeRuleItem
import net.akehurst.language.ogl.runtime.structure.RuntimeRuleItemKind
import net.akehurst.language.ogl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.parser.scannerless.test_ScannerlessParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class test_Processor_Ambiguity1 : test_ScannerlessParserAbstract() {
    /**
     * S : 'a' S B B | 'a' ;
     * B : 'b' ? ;
     */
    /**
     * S : S1 | 'a' ;
     * S1 = 'a' S B B ;
     * B : 'b' ? ;
     */
    private fun S(): RuntimeRuleSetBuilder {
        val b = RuntimeRuleSetBuilder()
        val r_a = b.literal("a")
        val r_B = b.rule("B").multi(0, 1, b.literal("b"))
        val r_S = b.rule("S").build()
        val r_S1 = b.rule("S1").concatenation(r_a, r_S, r_B, r_B)
        r_S.rhsOpt = RuntimeRuleItem(RuntimeRuleItemKind.CHOICE_EQUAL, -1, 0, arrayOf(r_S1, r_a))
        return b
    }

    @Test
    fun S_S_empty() {
        val rrb = this.S()
        val goal = "S"
        val sentence = ""

        val e = assertFailsWith(ParseFailedException::class) {
            super.test(rrb, goal, sentence)
        }
        assertEquals(1, e.location.line)
        assertEquals(0, e.location.column)
    }

    @Test
    fun S_S_a() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a"

        val expected1 = """
            S { 'a' }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected1)
    }

    @Test
    fun S_S_aa() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "aa"

        val expected1 = """
            S {
              S1 {
                'a'
                S { 'a' }
                B { §empty }
                B { §empty }
              }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected1)
    }

    @Test
    fun S_S_aab() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "aab"

        val expected1 = """
            S {
              S1 {
                'a'
                S { 'a' }
                B { 'b' }
                B { §empty }
              }
            }
        """.trimIndent()

        val expected2 = """
            S {
              S1 {
                'a'
                S { 'a' }
                B { §empty }
                B { 'b' }
              }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected1, expected2)
    }


}