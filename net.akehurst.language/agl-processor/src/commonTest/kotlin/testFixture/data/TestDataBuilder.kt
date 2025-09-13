package testFixture.data

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.processor.ProcessOptionsDefault
import net.akehurst.language.agl.simple.ContextWithScope
import net.akehurst.language.api.processor.CompletionItem
import net.akehurst.language.api.processor.ProcessOptions
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.asm.builder.AsmSimpleBuilder
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.sentence.api.InputLocation

//copied from parser
interface TestDataParserSentence {
    val sentence: String
    val options: ProcessOptions<Asm, ContextWithScope<Any, Any>>
}

fun parseError(pos: Int, col: Int, row: Int, len: Int, tryingFor: Set<String>, nextExpected: Set<String>) =
    TestDataParseIssue(InputLocation(pos, col, row, len, null), tryingFor, nextExpected)

data class TestDataParseIssue(
    val location: InputLocation,
    val tryingFor: Set<String>,
    val nextExpected: Set<String>
)

/*
open class TestDataParserSentencePass(
    override val sentence: String,
    override val options:ProcessOptions<*,*>,
    val expectedNumGSSHeads: Int,
    val expectedSppt: List<String>
) : TestDataParserSentence

class TestDataParserSentenceFail(
    override val sentence: String,
    override val options:ProcessOptions<*,*>,
    val expectedIssues: Set<TestDataParseIssue>
) : TestDataParserSentence {
    val expected: Set<LanguageIssue> = expectedIssues.map {
        testFixture.utils.parseError(it.location,sentence, it.tryingFor, it.nextExpected)
    }.toSet()
}

class TestDataParser(
    val description: String,
    val rrs: RuleSet,
    var goal: String,
    val sentences: List<TestDataParserSentence>
) {
}
*/

class TestSuit(
    val testData: List<TestDataProcessor>
) {
    operator fun get(index: String) = testData.first { it.description == index }
}

// new
class TestDataProcessor(
    val description: String,
    val grammarStr: String,
    val typeStr: String?,
    val transformStr: String?,
    val referenceStr: String?,
    val sentences: List<TestDataProcessorSentence>
) {
}

interface TestDataProcessorSentence : TestDataParserSentence {
}

class TestDataProcessorSentencePass(
    override val sentence: String,
    val description:String?,
    override val options: ProcessOptions<Asm, ContextWithScope<Any, Any>>,
    val expectedAsm: (AsmSimpleBuilder.() -> Unit)?,
    val expectedCompletionItem: List<CompletionItem>?
) : TestDataProcessorSentence {
    override fun toString(): String = "Pass: ${description ?:""} $sentence"
}

class TestDataProcessorSentenceFail(
    override val sentence: String,
    val description:String?,
    override val options: ProcessOptions<Asm, ContextWithScope<Any, Any>>,
    val expected: List<LanguageIssue>
) : TestDataProcessorSentence {

    override fun toString(): String = "Fail: ${description ?:""} $sentence"
}

fun testSuit(init: TestSuitBuilder.() -> Unit): TestSuit {
    val b = TestSuitBuilder()
    b.init()
    return b.build()
}

@DslMarker
annotation class AsmTestDataBuilderMarker

@AsmTestDataBuilderMarker
class TestSuitBuilder() {
    private val _testData = mutableListOf<TestDataProcessor>()

    fun testData(description: String, init: TestDataBuilder.() -> Unit) {
        val b = TestDataBuilder(description)
        b.init()
        val td = b.build()
        _testData.add(td)
    }

    fun build(): TestSuit = TestSuit(_testData)
}

@AsmTestDataBuilderMarker
class TestDataBuilder(
    private val _description: String
) {
    private lateinit var _grammarStr: String
    private var _typeStr: String? = null
    private var _transformStr: String? = null
    private var _referenceStr: String? = null
    private var _options: ProcessOptions<Asm, ContextWithScope<Any, Any>> = ProcessOptionsDefault()
    private val _sentences = mutableListOf<TestDataProcessorSentence>()

    fun grammarStr(value: String) {
        _grammarStr = value
    }

    fun typeStr(value: String) {
        _typeStr = value
    }

    fun transformStr(value: String) {
        _transformStr = value
    }

    fun referenceStr(value: String) {
        _referenceStr = value
    }

    fun processOptions(value: ProcessOptions<Asm, ContextWithScope<Any, Any>>) {
        _options = value
    }

    fun sentencePass(sentence: String, description:String? = null, init: TestDataSentenceBuilder.() -> Unit) {
        val b = TestDataSentenceBuilder(true, _options, sentence, description)
        b.init()
        val td = b.build()
        _sentences.add(td)
    }

    fun build() = TestDataProcessor(
        _description,
        _grammarStr,
        _typeStr,
        _transformStr,
        _referenceStr,
        _sentences
    )
}

@AsmTestDataBuilderMarker
class TestDataSentenceBuilder(
    val pass: Boolean,
    val baseOptions: ProcessOptions<Asm, ContextWithScope<Any, Any>>,
    val sentence: String,
    val description:String?
) {
    private var _options: ProcessOptions<Asm, ContextWithScope<Any, Any>> = Agl.options(baseOptions){}
    private var _context: ContextWithScope<Any,Any>? = null
    private var _expectedAsm: (AsmSimpleBuilder.() -> Unit)? = null
    private var _expectedCompletionItems: List<CompletionItem>? = null
    private val _expectedIssues = mutableListOf<LanguageIssue>()

    fun options(value: ProcessOptions<Asm, ContextWithScope<Any, Any>>) {
        _options = value
    }

    fun context(value: ContextWithScope<Any,Any>) {
        _options.semanticAnalysis.context = value
        _options.completionProvider.context = value
    }

    fun expectedIssues(value: List<LanguageIssue>) {
        _expectedIssues.addAll(value)
    }

    fun expectedAsm(init: AsmSimpleBuilder.() -> Unit) {
        _expectedAsm = init
    }

    fun expectedCompletionItems(value: List<CompletionItem>) {
        _expectedCompletionItems = value
    }

    fun build() = when (pass) {
        true -> TestDataProcessorSentencePass(
            sentence,
            description,
            _options,
            _expectedAsm,
            _expectedCompletionItems
        )

        false -> TestDataProcessorSentenceFail(
            sentence,
            description,
            _options,
            _expectedIssues
        )
    }
}