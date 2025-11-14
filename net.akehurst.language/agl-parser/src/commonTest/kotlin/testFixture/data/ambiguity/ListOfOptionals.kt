package testFixture.data.ambiguity

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import testFixture.data.TestDataParser
import testFixture.data.TestDataParserSentencePass
import testFixture.data.TestDataProviderAbstract

object ListOfOptionals : TestDataProviderAbstract() {

    val data = listOf(
        TestDataParser(
            "S = as ; as = ao* ; ao = a? ;",
            runtimeRuleSet("test.Test") {
                concatenation("S") { ref("as") }
                multi("as", 0, -1, "ao")
                optional("ao", "a")
                literal("a", "a")
            },
            "S",
            listOf(
                TestDataParserSentencePass(
                    "",
                    1,
                    listOf("S { as { ao { <EMPTY> } } }")
                ),
                TestDataParserSentencePass(
                    "a",
                    1,
                    listOf("S { as { ao { a:'a' } } }")
                ),
                TestDataParserSentencePass(
                    "aa",
                    1,
                    listOf("S { as { ao { a:'a' } ao { a:'a' } } }")
                ),
                TestDataParserSentencePass(
                    "aaa",
                    1,
                    listOf("S { as { ao { a:'a' } ao { a:'a' } ao { a:'a' } } }")
                )
            )
        )
    )


}