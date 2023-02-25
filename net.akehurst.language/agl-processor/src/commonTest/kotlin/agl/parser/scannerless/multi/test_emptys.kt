package agl.parser.scannerless.multi

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.scanondemand.multi.test_multi01
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test

internal class test_emptys : test_ScanOnDemandParserAbstract() {
    //TransitionReaction = StextTrigger? ('/' ReactionEffect)? ('#' TransitionProperty*)?;
    // S = 'a'?
    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("TR") { ref("optST"); ref("optRE"); ref("optTP")  }
            multi("optST",0,1,"ST")
            concatenation("ST") { ref("ID") }
            multi("optRE",0,1,"RE")
            concatenation("RE") { literal("/"); ref("EX") }
            multi("optTP",0,1,"TP")
            concatenation("TP") { literal("#"); ref("ID") }

            choice("EX",RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("ASS")
                ref("ID")
            }
            concatenation("ASS") { ref("ID"); literal("="); ref("ID")  }

            pattern("ID","[a-zA-Z]+")
        }
    }

    @Test
    fun empty() {
        val goal = "TR"
        val sentence = ""

        val expected = """
            TR {
              optST { §empty }
              optRE { §empty }
              optTP { §empty }
            }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun a() {
        val goal = "TR"
        val sentence = "a"

        val expected = """
            TR {
              optST { ST { ID:'a' } }
              optRE { §empty }
              optTP { §empty }
            }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun a_ass_b() {
        val goal = "TR"
        val sentence = "/a=b"

        val expected = """
            TR {
              optST { §empty }
              optRE { RE { '/' EX { ASS { ID:'a' '=' ID:'b' } } } }
              optTP { §empty }
            }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }
}