package parser.scannerless.listSeparated

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import net.akehurst.language.sentence.api.InputLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class test_sList_followed_by_optionl_separator_literal : test_LeftCornerParserAbstract() {

    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("sList"); ref("optSemi") }
            optional("optSemi","';'")
            sList("sList", 0, -1, "'a'", "';'")
            literal("a")
            literal(";")
        }
        val goal = "S"
    }

    @Test
    fun empty() {
        val sentence = ""

        val expected = "S { sList{ <EMPTY_LIST> } optSemi { <EMPTY> } }"

        super.test_pass(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun semi() {
        val sentence = ";"

        val expected = "S { sList{<EMPTY_LIST>} optSemi { ';' } }"

        super.test_pass(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun a_semi() {
        val sentence = "a;"

        val expected = "S { sList{ 'a' } optSemi { ';' } }"

        super.test_pass(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun a_semi_semi__fails() {
        val sentence = "a;;"

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt, sppt?.toStringAll)
        assertEquals(
            listOf(
                parseError(InputLocation(2, 3, 1, 1, null), sentence, setOf("S", "sList"), setOf("'a'","<EOT>"))
            ), issues.errors
        )
    }

    @Test
    fun a_semi_a__fails() {
        val sentence = "a;a"

        val expected = "S { sList{'a' ';' 'a'} optSemi { <EMPTY> } }"

        super.test_pass(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun a_semi_a_semi() {
        val sentence = "a;a;"

        val expected = "S { sList{'a' ';' 'a'} optSemi { ';' } }"

        super.test_pass(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

}