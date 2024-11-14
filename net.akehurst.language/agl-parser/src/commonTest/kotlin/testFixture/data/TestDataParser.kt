package testFixture.data

import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet

class TestDataParserSentence(
    val sentence:String,
    val expectedNumGSSHeads: Int,
    val expectedSppt:List<String>
)

class TestDataParser(
    val description:String,
    val rrs:RuntimeRuleSet,
    var goal:String,
    val sentences:List<TestDataParserSentence>
) {
}