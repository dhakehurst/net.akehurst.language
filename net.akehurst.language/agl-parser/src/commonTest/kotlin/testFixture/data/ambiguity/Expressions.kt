package testFixture.data.ambiguity

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.api.RulePosition
import testFixture.data.TestDataParser
import testFixture.data.TestDataParserSentence

object Expressions {

    val LI = RulePosition.OPTION_MULTI_ITEM

    val data = listOf(
        TestDataParser(
            "S = p E; E = l+ | [l / 'o']2+",
            runtimeRuleSet {
                concatenation("S") { literal("p"); ref("Exp") }
                choiceLongest("Exp") {
                    ref("Var")
                    ref("Ifx")
                }
                multi("Var", 1, -1, "ltr")
                sList("Ifx", 2, -1, "Exp", "op")
                pattern("ltr", "[a-z]")
                literal("op","o")
                preferenceFor("ltr") {
                    rightOption(listOf("Var"), LI, setOf("op"))
                }
            },
            "S",
            listOf(
                TestDataParserSentence(
                    "pa",
                    1,
                    listOf("S{ 'p' Exp { Var { ltr:'a' } } }")
                ),
                TestDataParserSentence(
                    "paob",
                    1,
                    listOf(
                        """
                            S { 'p' Exp { Ifx {
                              Exp { Var { ltr : 'a' } }
                              op:'o'
                              Exp { Var { ltr : 'b' } }
                            } } }
                        """.trimIndent()
                    )
                ),
                TestDataParserSentence(
                    "paoboc",
                    1,
                    listOf(
                        """
                            S { 'p' Exp { Ifx {
                              Exp { Var { ltr : 'a' } }
                              op : 'o'
                              Exp { Var { ltr : 'b' } }
                              op : 'o'
                              Exp { Var { ltr : 'c' } }
                            } } }                
                        """
                    )
                ),
                TestDataParserSentence(
                    "aobocod",
                    1,
                    listOf(
                        """
                            S { Exp { Ifx {
                              Exp { Var { ltr : 'a' } }
                              op : 'o'
                              Exp { Var { ltr : 'b' } }
                              op : 'o'
                              Exp { Var { ltr : 'c' } }
                              op : 'o'
                              Exp { Var { ltr : 'd' } }
                            } } }                
                        """
                    )
                )
            )
        )
    )


}