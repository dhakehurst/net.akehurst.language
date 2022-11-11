package net.akehurst.language.agl.grammar.style

import net.akehurst.language.agl.grammar.grammar.ContextFromGrammar
import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.agl.syntaxAnalyser.ContextSimple
import net.akehurst.language.api.style.AglStyleRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class test_AglStyle {

    private companion object {
        val aglProc = Agl.registry.agl.style.processor!!
        val testGrammar = Agl.registry.agl.grammar.processor?.process("""
            namespace test
            
            grammar Test {
                skip WS = "\s+" ;
            
                unit = declaration* ;
                declaration = datatype | primitive ;
                primitive = 'primitive' ID ;
                datatype = 'datatype' ID '{' property* '}' ;
                property = ID ':' typeReference ;
                typeReference = type typeArguments? ;
                typeArguments = '<' [typeReference / ',']+ '>' ;
            
                leaf ID = "[A-Za-z_][A-Za-z0-9_]*" ;
                leaf type = ID;
            
                // not marked as leaf so can use to test patterns
                COMMENT = "\"(\\?.)*\"" ;
                INT = "[0-9]+" ;
            }
        """.trimIndent())?.asm?.first()!!
    }

    private fun process(sentence:String) =aglProc.process(sentence, Agl.options { syntaxAnalysis { context(ContextFromGrammar(testGrammar)) } })


    @Test
    fun single_line_comment() {

        val text = """
            // single line comment
        """.trimIndent()

        val result = process(text)

        assertNotNull(result.asm)
        assertEquals(0, result.asm?.size)

        assertEquals(0, result.issues.size, result.issues.joinToString("\n") { "$it" })
    }

    @Test
    fun multi_line_comment() {

        val text = """
            /* multi
               line
               comment
            */
        """.trimIndent()

        val result = process(text)

        assertNotNull(result.asm)
        assertEquals(0, result.asm?.size)
        assertEquals(0, result.issues.size, result.issues.joinToString("\n") { "$it" })
    }

    @Test
    fun emptyRule() {

        val text = """
            declaration { }
        """.trimIndent()

        val result = process(text)

        assertNotNull(result.asm)
        assertEquals(1, result.asm?.size)
        assertEquals(0, result.issues.size, result.issues.joinToString("\n") { "$it" })
    }

    @Test
    fun oneLeafRule() {

        val text = """
            ID {
                -fx-fill: green;
                -fx-font-weight: bold;
            }
        """.trimIndent()

        val result =process(text)

        assertNotNull(result.asm)
        assertEquals(1, result.asm?.size)
        assertEquals("ID", result.asm!![0].selector.first())
        assertEquals(2, result.asm!![0].styles.size)
        assertEquals(0, result.issues.size, result.issues.joinToString("\n") { "$it" })
    }

    @Test
    fun multiLeafRules() {

        val text = """
            ID {
                -fx-fill: green;
                -fx-font-weight: bold;
            }
            "[0-9]+" {
                -fx-fill: green;
                -fx-font-weight: bold;
            }
        """.trimIndent()

        val result = process(text)

        assertNotNull(result.asm)
        assertEquals(2, result.asm?.size)
        assertEquals(0, result.issues.size, result.issues.joinToString("\n") { "$it" })
    }

    @Test
    fun regexWithQuotes() {
        val text = """
            "\"(\\?.)*\"" {
              font-family: "Courier New";
              color: darkblue;
            }
        """.trimIndent()

        val result = process(text)

        assertNotNull(result.asm)
        assertEquals(1, result.asm?.size)
        assertEquals(0, result.issues.size, result.issues.joinToString("\n") { "$it" })
    }

    @Test
    fun selectorAndComposition() {

        val text = """
            property,typeReference,typeArguments { }
        """.trimIndent()

        val result = process(text)

        assertNotNull(result.asm)
        assertEquals(1, result.asm?.size)
        assertEquals(0, result.issues.size, result.issues.joinToString("\n") { "$it" })
    }
    //TODO more tests
}
