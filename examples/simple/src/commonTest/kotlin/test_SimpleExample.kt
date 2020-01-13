package net.akehurst.language.examples.simple

import kotlin.test.Test
import kotlin.test.assertEquals


class test_SimpleExample {

    val text1 = """
            class class {
               property : String
               method(param: Integer) {
                    property := 1 + 2 * param / 3.14
               }
            }
        """.trimIndent()

    @Test
    fun scan() {
        val result = SimpleExample.processor.scan(text1)
        println(result)

        val expected = listOf(
                Triple(0, "class", "class"),
                Triple(5, "\\s+", " "),
                Triple(6, "class", "class"),
                Triple(11, "\\s+", " "),
                Triple(12, "{", "{"),
                Triple(13, "\\s+", "\n   "),
                Triple(17, "[a-zA-Z_][a-zA-Z0-9_]*", "property"),
                Triple(25, "\\s+", " "),
                Triple(26, ":", ":"),
                Triple(27, "\\s+", " "),
                Triple(28, "[a-zA-Z_][a-zA-Z0-9_]*", "String"),
                Triple(34, "\\s+", "\n   "),
                Triple(38, "[a-zA-Z_][a-zA-Z0-9_]*", "method"),
                Triple(44, "(", "(")
        )

        //assertEquals(result.size, expected.size)
        result.take(14).forEachIndexed { i, leaf ->
            assertEquals(expected[i].first, leaf.startPosition)
            assertEquals(expected[i].second, leaf.name)
            assertEquals(expected[i].third, leaf.matchedText)
        }
    }

    @Test
    fun parse() {
        val result = SimpleExample.processor.parse("unit", text1)

        println("--- original text, from the parse tree ---")
        println(result.asString)

        println("--- the parse tree ---")
        println(result.toStringIndented("  "))

        val expected =
    }

    @Test
    fun process() {
        val result: SimpleExampleUnit = SimpleExample.processor.process("unit", text1)
        println(result)
    }

    @Test
    fun format_ASM() {
        val result: SimpleExampleUnit = SimpleExample.processor.process("unit", text1)
        val formatted = SimpleExample.processor.format(result)
        print(formatted)
        assertEquals(text1, formatted)
    }

    @Test
    fun format_Text() {
        val unformattedText = "class class { property:String method(param:Type) { property := 1 + 2 * param / 3.14 } }"
        val formatted = SimpleExample.processor.format<SimpleExampleUnit>("unit", unformattedText)
        print(formatted)
        assertEquals(text1, formatted)
    }
}