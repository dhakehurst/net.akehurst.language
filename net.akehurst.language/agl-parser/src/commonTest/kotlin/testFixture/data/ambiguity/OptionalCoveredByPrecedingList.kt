package testFixture.data.ambiguity

import net.akehurst.language.agl.runtime.structure.ruleSet
import testFixture.data.*

object OptionalCoveredByPrecedingList : TestDataProviderAbstract() {

    val data = listOf(
        TestDataParser(
            "S = 'b' vs oa ; vs = v+ ; oa = 'a'? ;",
            ruleSet("test.Test") {
                concatenation("S") { literal("b"); ref("vs"); ref("oa") }
                optional("oa", "'a'")
                multi("vs", 1, -1, "v")
                concatenation("vvs") { ref("v"); ref("vs") }
                literal("a")
                pattern("v", "[a-z]")
            },
            "S",
            listOf(
                TestDataParserSentenceFail("", setOf(parseError(0,1,1,1,setOf("<GOAL>"),setOf("'b'")))),
                TestDataParserSentenceFail("b", setOf(parseError(1,2,1,1,setOf("<GOAL>"),setOf("v")))),
                TestDataParserSentencePass("ba", 1, listOf(" S { 'b' vs {v:'a'} oa{<EMPTY>} }")),
                TestDataParserSentencePass("baa", 2, listOf("S { 'b' vs { v:'a' } oa{ 'a' } }")),
                TestDataParserSentencePass("bxa", 2, listOf("S { 'b'  vs { v:'x' } oa{ 'a' } }")),
                TestDataParserSentencePass("baaa", 2, listOf("S { 'b' vs { v:'a' v:'a' } oa{ 'a' } }")),
                TestDataParserSentencePass("baaaa", 2, listOf("S { 'b'  vs { v:'a' v:'a' v:'a' } oa{ 'a' } }"))
            )
        ),
        TestDataParser(
            "S = 'b' vs oa ; vs = v+ ; oa = a? ; a = 'a' vs",
            ruleSet("test.Test") {
                concatenation("S") { literal("b"); ref("vs"); ref("oa") }
                optional("oa", "a")
                multi("vs", 1, -1, "v")
                concatenation("vvs") { ref("v"); ref("vs") }
                concatenation("a") { literal("a"); ref("vs") }
                literal("a")
                pattern("v", "[a-z]")
            },
            "S",
            listOf(
                TestDataParserSentenceFail("", setOf(parseError(0,1,1,1,setOf("<GOAL>"),setOf("'b'")))),
                TestDataParserSentenceFail("b", setOf(parseError(1,2,1,1,setOf("<GOAL>"),setOf("v")))),
                TestDataParserSentencePass("ba", 1, listOf("S { 'b' vs{v:'a'} oa{<EMPTY>} }")),
                TestDataParserSentencePass("bv", 1, listOf("S { 'b' vs{v:'v'} oa{<EMPTY>} }")),
                TestDataParserSentencePass("baa", 1, listOf("S { 'b' vs{ v:'a' v:'a'} oa{ <EMPTY> } }")),
                TestDataParserSentencePass("bav", 1, listOf("S { 'b' vs{ v:'a' v:'v'} oa{ <EMPTY> } }")),
                TestDataParserSentencePass("baav", 2, listOf("S { 'b' vs{ v:'a'} oa{ a{'a' vs { v : 'v' }} } }")),
                TestDataParserSentencePass("baaa", 2, listOf("S { 'b' vs{ v:'a'} oa{ a{'a' vs { v : 'a' }} } }")),
                TestDataParserSentencePass("baaaa", 2, listOf("S { 'b' vs{ v:'a' v:'a'} oa{ a{'a' v:'a'} } }")),
            )
        )
    )


}