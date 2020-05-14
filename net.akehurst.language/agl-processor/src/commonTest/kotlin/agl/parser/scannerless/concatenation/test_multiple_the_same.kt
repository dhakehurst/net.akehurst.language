package parser.scannerless.concatenation

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class test_multiple_the_same : test_ScanOnDemandParserAbstract() {

    val S = runtimeRuleSet {
        skip("WS") { literal("\\s+") }
        concatenation("S") { ref("X"); ref("Ls") }
        multi("Ls",0,-1,"L")
        concatenation("L") { ref("A"); ref("A"); ref("B"); }
        literal("A", "a")
        literal("B", "b")
        literal("X", "x")
    }

    @Test
    fun empty_fails() {
        val rrb = this.S
        val goal = "S"
        val sentence = ""

        val ex = assertFailsWith(ParseFailedException::class) {
            super.test(rrb, goal, sentence)
        }
        assertEquals(1, ex.location.line)
        assertEquals(1, ex.location.column)
    }

    @Test
    fun id_only() {
        val rrb = this.S
        val goal = "S"
        val sentence = "x"

        val expected = """
            S { X:'x' Ls|1 { §empty } }
        """.trimIndent()
        val actual = super.test(rrb, goal, sentence, expected)

        assertNotNull(actual)
    }

    @Test
    fun xaab() {
        val rrb = this.S
        val goal = "S"
        val sentence = "xaab"

        val expected = """
            S { X:'x' Ls { L {
                A:'a' A:'a' B:'b'
            } } }
        """.trimIndent()
        val actual = super.test(rrb, goal, sentence, expected)

        assertNotNull(actual)
    }

}