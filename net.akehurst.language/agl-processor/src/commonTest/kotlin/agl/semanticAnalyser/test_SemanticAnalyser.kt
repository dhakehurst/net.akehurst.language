package agl.semanticAnalyser

import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.agl.processor.SemanticAnalysisResultDefault
import net.akehurst.language.api.analyser.SemanticAnalyser
import net.akehurst.language.api.grammar.GrammarItem
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.*
import kotlin.test.Test

class test_SemanticAnalyser {

    class TestSemanticAnalyser : SemanticAnalyser<Any,Any> {
        override fun clear() {
            TODO("not implemented")
        }

        override fun configure(configurationContext: SentenceContext<GrammarItem>, configuration: Map<String, Any>): List<LanguageIssue> {
            TODO("not implemented")
        }

        override fun analyse(asm: Any, locationMap: Map<Any, InputLocation>?, context: Any?, options: Map<String, Any>): SemanticAnalysisResult {
            val ih = IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS)
            when (asm) {
                "error" -> ih.error(null,"error")
                "warning" -> ih.warn(null,"warning")
                else -> throw RuntimeException("Test Error")
            }
            return SemanticAnalysisResultDefault(ih)
        }
    }

    @Test
    fun error() {
        val asm = Any()
        val sut = TestSemanticAnalyser()
        val actual = sut.analyse("error")
        TODO()
    }
}