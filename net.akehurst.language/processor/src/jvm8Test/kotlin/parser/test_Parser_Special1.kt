package parser

import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.ogl.ast.GrammarBuilderDefault
import net.akehurst.language.ogl.ast.NamespaceDefault
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class test_Parser_Special1 : test_ParserAbstract() {
    /**
     * `
     * S : 'a' S B B | 'a' ;
     * B : 'b' ? ;
    ` *
     */
    private fun S(): Grammar {
        val b = GrammarBuilderDefault(NamespaceDefault("test"), "Test")
        b.rule("S").choiceEqual(b.nonTerminal("S1"), b.terminalLiteral("a"));
        b.rule("S1").concatenation(b.terminalLiteral("a"), b.nonTerminal("S"), b.nonTerminal("B"), b.nonTerminal("B"))
        b.rule("B").multi(0, 1, b.terminalLiteral("b"))
        return b.grammar
    }

    @Test
    fun S_S_empty() {
        val grammar = this.S()
        val goal = "S"
        val sentence = ""

        val e = assertFailsWith(ParseFailedException::class) {
            super.test(grammar, goal, sentence)
        }
        assertEquals(1, e.location.line)
        assertEquals(0, e.location.column)
    }

    @Test
    fun S_S_a() {
        val grammar = this.S()
        val goal = "S"
        val sentence = "a"

        val expected1 = """
            S { 'a' }
        """.trimIndent()

        super.test(grammar, goal, sentence, expected1)
    }

    @Test
    fun S_S_aa() {
        val grammar = this.S()
        val goal = "S"
        val sentence = "aa"

        val expected1 = """
            S {
              S1 {
                'a'
                S { 'a' }
                B { §multi0 { §empty } }
                B { §multi0 { §empty } }
              }
            }
        """.trimIndent()

        super.test(grammar, goal, sentence, expected1)
    }

    @Test
    fun S_S_aab() {
        val grammar = this.S()
        val goal = "S"
        val sentence = "aab"

        val expected1 = """
            S {
              S1 {
                'a'
                S { 'a' }
                B { §multi0 {'b'} }
                B { §multi0 { §empty } }
              }
            }
        """.trimIndent()

        val expected2 = """
            S {
              S1 {
                'a'
                S { 'a' }
                B { §multi0 { §empty} }
                B { §multi0 {'b'} }
              }
            }
        """.trimIndent()

        super.test(grammar, goal, sentence, expected1, expected2)
    }


}