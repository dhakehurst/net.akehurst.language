package net.akehurst.language.processor.processor.ambiguity

import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.ogl.ast.GrammarBuilderDefault
import net.akehurst.language.ogl.ast.NamespaceDefault
import net.akehurst.language.ogl.runtime.structure.RuntimeRuleItem
import net.akehurst.language.ogl.runtime.structure.RuntimeRuleItemKind
import net.akehurst.language.ogl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.ogl.runtime.structure.RuntimeRuleSetBuilder
import processor.test_ParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class test_Processor_Ambiguity2 : test_ParserAbstract() {
    /**
     * S : S S | 'a' ;
     */
    private fun S(): RuntimeRuleSetBuilder {
        val rrb = RuntimeRuleSetBuilder()
        val ra = rrb.literal("a")
        val rS = rrb.rule("S").build()
        val rS1 = rrb.rule("S1").concatenation(rS, rS)
        rS.rhsOpt = RuntimeRuleItem(RuntimeRuleItemKind.CHOICE_EQUAL,-1,0,arrayOf(rS1, ra))
        return rrb
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
                S { 'a' }
                S { 'a' }
              }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected1)
    }

    @Test
    fun S_S_aaa() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "aaa"

        val expected1 = """
            S {
              S {
                S1 {
                  S { 'a' }
                  S { 'a' }
                }
              }
              S { 'a' }
            }
        """.trimIndent()

        val expected2 = """
            S {
              S { 'a' }
              S {
                S1 {
                  S { 'a' }
                  S { 'a' }
                }
              }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected1, expected2)
    }


}