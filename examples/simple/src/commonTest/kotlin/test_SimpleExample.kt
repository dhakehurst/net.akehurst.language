package net.akehurst.language.examples.simple

import kotlin.test.Test


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
    }

    @Test
    fun parse() {
        val result = SimpleExample.processor.parse("unit", text1)

        println("--- original text, from the parse tree ---")
        println(result.asString)

        println("--- the parse tree ---")
        println(result.toStringIndented("  "))
    }

    @Test
    fun process() {

        val result: SimpleExampleUnit = SimpleExample.processor.process("unit", text1)
        println(result)
    }

    @Test
    fun format() {
        val result: SimpleExampleUnit = SimpleExample.processor.process("unit", text1)
        val formatted = SimpleExample.processor.format(result)
        print(formatted)
    }
}