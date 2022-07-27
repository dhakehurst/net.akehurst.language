package thridparty.projectIT

import net.akehurst.language.agl.processor.Agl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_demo {

    private companion object {

        val grammarStr = """

        """.trimIndent()

        val processor = Agl.processorFromStringDefault(grammarStr)
        const val goal = ""
    }

    @Test
    fun sentence1() {
        val sentence = """

        """.trimIndent()
        val (sppt,issues) =  processor.parse(sentence, processor.parserOptions { goalRuleName(goal) })
        assertNotNull(sppt)
        assertEquals(emptyList(),issues)
    }
}