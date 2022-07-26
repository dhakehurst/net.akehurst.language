package thridparty.projectIT

import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.api.processor.parserOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_demo {

    private companion object {

        val grammarStr = """

        """.trimIndent()

        val processor = Agl.processorFromString(grammarStr)
        const val goal = ""
    }

    @Test
    fun sentence1() {
        val sentence = """

        """.trimIndent()
        val (sppt,issues) =  processor.parse(sentence, parserOptions { goalRule(goal) })
        assertNotNull(sppt)
        assertEquals(emptyList(),issues)
    }
}