package net.akehurst.language.agl.grammar.style

import net.akehurst.language.api.style.AglStyleRule
import net.akehurst.language.agl.processor.Agl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class test_AglStyle {

    @Test
    fun single_line_comment() {

        val text = """
            // single line comment
        """.trimIndent()

        val p = Agl.styleProcessor.process<List<AglStyleRule>>("rules", text)

        assertNotNull(p)
        assertEquals(0, p.size)
    }

    @Test
    fun multi_line_comment() {

        val text = """
            /* multi
               line
               comment
            */
        """.trimIndent()

        val p = Agl.styleProcessor.process<List<AglStyleRule>>("rules", text)

        assertNotNull(p)
        assertEquals(0, p.size)
    }

    @Test
    fun emptyRule() {

        val text = """
            selector { }
        """.trimIndent()

        val actual = Agl.styleProcessor.process<List<AglStyleRule>>("rules", text)

        assertNotNull(actual)
        assertEquals(1, actual.size)
    }

    @Test
    fun oneRule() {

        val text = """
            class {
                -fx-fill: green;
                -fx-font-weight: bold;
            }
        """.trimIndent()

        val actual = Agl.styleProcessor.process<List<AglStyleRule>>("rules", text)

        assertNotNull(actual)
        assertEquals(1, actual.size)
        assertEquals("class", actual[0].selector)
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

        val actual = Agl.styleProcessor.process<List<AglStyleRule>>("rules", text)

        assertNotNull(actual)
        assertEquals(2, actual.size)
    }
    @Test
    fun regexWithQuotes() {
        val text = """
            "\\\"(?:\\\\?.)*?\\\"" {
              font-family: "Courier New";
              color: darkblue;
            }
        """.trimIndent()

        val actual = Agl.styleProcessor.process<List<AglStyleRule>>("rules", text)

        assertNotNull(actual)
        assertEquals(1, actual.size)
    }

}
