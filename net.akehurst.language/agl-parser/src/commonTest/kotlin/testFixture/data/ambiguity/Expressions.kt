package testFixture.data.ambiguity

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import testFixture.data.TestDataParser
import testFixture.data.TestDataParserSentence
import testFixture.data.TestDataParserSentencePass
import testFixture.data.TestDataProviderAbstract

object Expressions : TestDataProviderAbstract() {

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
                TestDataParserSentencePass(
                    "pa",
                    1,
                    listOf("S{ 'p' Exp { Var { ltr:'a' } } }")
                ),
                TestDataParserSentencePass(
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
                TestDataParserSentencePass(
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
                TestDataParserSentencePass(
                    "paobocod",
                    1,
                    listOf(
                        """
                            S { 'p' Exp { Ifx {
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