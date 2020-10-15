package agl.parser.scannerless.choiceEqual

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals

class test_choice : test_ScanOnDemandParserAbstract() {

    companion object {
        // S =  'a' | S1 ;
        // S1 = 'a' S2 ;
        // S2 = ',' S ;
        val S = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("a")
                ref("S1")
            }
            concatenation("S1") { literal("a"); ref("S2") }
            concatenation("S2") { literal(","); ref("S") }
        }
    }

    @Test
    fun a() {
        val rrs = S
        val goal = "S"
        val sentence = "a"

        val expected = """
            S { 'a' }
        """.trimIndent()

        val actual = super.test(rrs, goal, sentence, expected)
        assertEquals(1, actual.maxNumHeads)
    }

}