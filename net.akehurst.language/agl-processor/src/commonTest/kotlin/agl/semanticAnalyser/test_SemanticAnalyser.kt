package agl.semanticAnalyser

import net.akehurst.language.agl.processor.SemanticAnalysisResultDefault
import net.akehurst.language.api.analyser.SemanticAnalyser
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.LanguageIssueKind
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.api.processor.SemanticAnalysisResult
import kotlin.test.Test

class test_SemanticAnalyser {

    class TestSemanticAnalyser : SemanticAnalyser<Any,Any> {
        override fun clear() {
            TODO("not implemented")
        }

        override fun analyse(asm: Any, locationMap: Map<*, InputLocation>?, arg: Any?): SemanticAnalysisResult {
            val issues =  when (asm) {
                "error" -> listOf(LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.PARSE,null,"error"))
                "warning" -> listOf(LanguageIssue(LanguageIssueKind.WARNING, LanguageProcessorPhase.PARSE,null,"error"))
                else -> throw RuntimeException("Test Error")
            }
            return SemanticAnalysisResultDefault(issues)
        }
    }

    @Test
    fun error() {
        val asm = Any()
        val sut = TestSemanticAnalyser()
        val actual = sut.analyse("error")
    }
}