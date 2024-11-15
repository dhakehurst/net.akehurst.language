package testFixture.data

import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.sentence.api.InputLocation

interface TestDataParserSentence {
    val sentence: String
}

fun parseError(pos:Int, col: Int, row: Int, len: Int, tryingFor: Set<String>, nextExpected: Set<String>) =
    TestDataParseIssue(InputLocation(pos, col, row, len),tryingFor, nextExpected)

data class TestDataParseIssue(
    val location: InputLocation,
    val tryingFor: Set<String>,
    val nextExpected: Set<String>
)

class TestDataParserSentencePass(
    override val sentence: String,
    val expectedNumGSSHeads: Int,
    val expectedSppt: List<String>
) : TestDataParserSentence

class TestDataParserSentenceFail(
    override val sentence: String,
    val expectedIssues: Set<TestDataParseIssue>
) : TestDataParserSentence {
    val expected: Set<LanguageIssue> = expectedIssues.map {
        testFixture.utils.parseError(it.location,sentence, it.tryingFor, it.nextExpected)
    }.toSet()
}


class TestDataParser(
    val description: String,
    val rrs: RuntimeRuleSet,
    var goal: String,
    val sentences: List<TestDataParserSentence>
) {
}