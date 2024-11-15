package testFixture.data.whitespace

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import testFixture.data.*


object ForceWhitespaceWhereNeeded : TestDataProviderAbstract() {

    val data = listOf(
        /*
         * skip leaf WS = "\s+" ;
         * leaf ID = "[a-z]+" ;
         * S = 'key' ID ;
         */
        TestDataParser(
            "S = 'key' ID ;",
            runtimeRuleSet {
                pattern("WS","\\s+", isSkip = true)
                concatenation("S") { literal("key"); ref("ID") }
                pattern("ID","[a-z]+")
            },
            "S",
            listOf(
                TestDataParserSentenceFail("", setOf(parseError(0,1,1,1, setOf("<GOAL>"), setOf("'key'")))),
                TestDataParserSentenceFail("v", setOf(parseError(0,1,1,1, setOf("<GOAL>"), setOf("'key'")))),
                TestDataParserSentenceFail("key", setOf(parseError(3,4,1,1, setOf("<GOAL>"), setOf("ID")))),
                TestDataParserSentenceFail("keyid", setOf(parseError(3,4,1,1, setOf("<GOAL>"), setOf("WS")))),
                TestDataParserSentencePass("key id", 1, listOf("S { 'key' WS:' ' ID:'id' }"))
            )
        )
    )


}