package testFixture.data

import net.akehurst.language.agl.*
import net.akehurst.language.agl.simple.ContextWithScope
import net.akehurst.language.api.processor.*
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.grammar.processor.ContextFromGrammarRegistry
import kotlin.test.assertEquals
import kotlin.test.assertTrue

fun testSentence(proc: LanguageProcessor<Asm, ContextWithScope<Any, Any>>, sd: TestDataParserSentence) {
    println("Testing - $sd")
    when (sd) {
        is TestDataProcessorSentencePass -> when {
            null != sd.expectedAsm && null != sd.expectedCompletionItem -> error("Currently only supports testing either process or autocomplete, not both")
            null != sd.expectedAsm -> {
                val asmRes = proc.process(sd.sentence, sd.options)
                assertTrue(asmRes.allIssues.errors.isEmpty(), asmRes.allIssues.toString())
                val actual = asmRes.asm!!
                assertEquals(sd.expectedAsm.asString(indentIncrement = "  "), actual.asString(indentIncrement = "  "), "Different ASM")
            }

            null != sd.expectedCompletionItem -> {
                val actual = proc.expectedItemsAt(sd.sentence, sd.sentence.length, sd.options)
                assertTrue(actual.issues.errors.isEmpty(), actual.issues.toString())
                //assertEquals(sd.expectedCompletionItem.size, actual.items.size,actual.items.joinToString(separator = "\n"))
                assertEquals(sd.expectedCompletionItem.joinToString(separator = "\n"), actual.items.joinToString(separator = "\n"))
            }
            else -> error("Must provide either an expectedAsm or expectedCompletionItems")
        }

        //is TestDataParserSentenceFail -> {}
        else -> error("Unsupported")
    }

}

fun doTest(testData: TestDataProcessor, sentenceIndex: Int? = null) {
    println("****** ${testData.description} ******")
    val procRes = Agl.processorFromStringSimple(
        grammarDefinitionStr = GrammarString(testData.grammarStr),
        typeStr = testData.typeStr?.let { TypesString(it) },
        transformStr = testData.transformStr?.let { TransformString(it) },
        referenceStr = testData.referenceStr?.let { CrossReferenceString(it) },
        grammarAglOptions = Agl.options {
            semanticAnalysis {
                context(ContextFromGrammarRegistry(Agl.registry))
//TODO:                    option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, true)
            }
        }
    )
    assertTrue(procRes.issues.isEmpty(), procRes.issues.toString())
    val proc = procRes.processor!!

    println("--- TypeDomain ---")
    println(proc.typesModel.asString())
    println("--- Asm Transform ---")
    println(proc.transformModel.asString())

    println("****** ${testData.description} Sentences ******")
    if (null == sentenceIndex) {
        for (sd in testData.sentences) {
            testSentence(proc, sd)
        }
    } else {
        val sd = testData.sentences[sentenceIndex]
        testSentence(proc, sd)
    }
}

fun executeTestSuit(testSuit:TestSuit) {
    testSuit.testData.forEach {
        doTest(it)
    }
}