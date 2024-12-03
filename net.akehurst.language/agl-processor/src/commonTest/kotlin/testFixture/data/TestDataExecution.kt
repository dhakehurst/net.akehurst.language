package testFixture.data

import net.akehurst.language.agl.*
import net.akehurst.language.agl.simple.ContextAsmSimple
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.grammar.processor.ContextFromGrammarRegistry
import kotlin.test.assertEquals
import kotlin.test.assertTrue


fun testSentence(proc: LanguageProcessor<Asm, ContextAsmSimple>, sd: TestDataParserSentence) {
    println("'${sd.sentence}'")
    when (sd) {
        is TestDataProcessorSentencePass -> {
            val asmRes = proc.process(sd.sentence, Agl.options {
                this.parse { goalRuleName(sd.goal) }
            })
            assertTrue(asmRes.issues.errors.isEmpty(), asmRes.issues.toString())
            val actual = asmRes.asm!!
            assertEquals(sd.expectedAsm.asString(indentIncrement = "  "), actual.asString(indentIncrement = "  "), "Different ASM")
        }

        //is TestDataParserSentenceFail -> {}
        else -> error("Unsupported")
    }

}

fun doTest(testData: TestDataProcessor, sentenceIndex: Int? = null) {
    val procRes = Agl.processorFromStringSimple(
        grammarDefinitionStr = GrammarString(testData.grammarStr),
        typeStr = testData.typeStr?.let { TypeModelString(it) },
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
    println(proc.typeModel.asString())
    println("--- Asm Transform ---")
    println(proc.asmTransformModel.asString())

    if (null == sentenceIndex) {
        for (sd in testData.sentences) {
            testSentence(proc, sd)
        }
    } else {
        val sd = testData.sentences[sentenceIndex]
        testSentence(proc, sd)
    }
}
