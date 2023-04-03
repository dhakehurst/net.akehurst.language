import net.akehurst.language.agl.processor.Agl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_GeneratedGrammar_Simple {

    @Test
    fun build_for_generated_should_fail() {
        val sentence = "a"

        val p = GeneratedGrammar_Simple.processor.buildFor(Agl.parseOptions {
            goalRuleName("S")
        })

    }

    @Test
    fun parse() {
        val sentence = "a"
        val result = GeneratedGrammar_Simple.parse(sentence)

        assertNotNull(result.sppt)
    }

}