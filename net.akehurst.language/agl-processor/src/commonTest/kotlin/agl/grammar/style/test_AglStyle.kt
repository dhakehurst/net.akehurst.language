package net.akehurst.language.agl.grammar.style

import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.api.style.AglStyleRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class test_AglStyle {

    private companion object {
        val aglProc = Agl.registry.agl.style.processor!!
    }

    @Test
    fun single_line_comment() {

        val text = """
            // single line comment
        """.trimIndent()

        val result = aglProc.process(text)

        assertNotNull(result.asm)
        assertEquals(0, result.asm?.size)
    }

    @Test
    fun multi_line_comment() {

        val text = """
            /* multi
               line
               comment
            */
        """.trimIndent()

        val result = aglProc.process(text)

        assertNotNull(result.asm)
        assertEquals(0, result.asm?.size)
    }

    @Test
    fun emptyRule() {

        val text = """
            selector { }
        """.trimIndent()

        val result = aglProc.process(text)

        assertNotNull(result.asm)
        assertEquals(1, result.asm?.size)
    }

    @Test
    fun oneRule() {

        val text = """
            class {
                -fx-fill: green;
                -fx-font-weight: bold;
            }
        """.trimIndent()

        val result = aglProc.process(text)

        assertNotNull(result.asm)
        assertEquals(1, result.asm?.size)
        assertEquals("class", result.asm!![0].selector.first())
        assertEquals(2, result.asm!![0].styles.size)
    }

    @Test
    fun multiRules() {

        val text = """
            class {
                -fx-fill: green;
                -fx-font-weight: bold;
            }
            "[a-z]" {
                -fx-fill: green;
                -fx-font-weight: bold;
            }
        """.trimIndent()

        val result = aglProc.process(text)

        assertNotNull(result.asm)
        assertEquals(2, result.asm?.size)
    }

    @Test
    fun regexWithQuotes() {
        val text = """
            "\"(\\?.)*\"" {
              font-family: "Courier New";
              color: darkblue;
            }
        """.trimIndent()

        val result = aglProc.process(text)

        assertNotNull(result.asm)
        assertEquals(1, result.asm?.size)
    }

    @Test
    fun selectorAndComposition() {

        val text = """
            selector1,selector2,selector { }
        """.trimIndent()

        val result = aglProc.process(text)

        assertNotNull(result.asm)
        assertEquals(1, result.asm?.size)
    }
}
