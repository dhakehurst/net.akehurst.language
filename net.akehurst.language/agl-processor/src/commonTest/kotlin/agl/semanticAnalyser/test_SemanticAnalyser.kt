package agl.semanticAnalyser

import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.analyser.SemanticAnalyser
import net.akehurst.language.api.analyser.AnalyserIssue
import net.akehurst.language.api.analyser.AnalyserIssueKind
import kotlin.test.Test

class test_SemanticAnalyser {

    class TestSemanticAnalyser : SemanticAnalyser<Any,Any> {
        override fun clear() {
            TODO("not implemented")
        }

        override fun analyse(asm: Any, locationMap: Map<*, InputLocation>?, arg: Any?): List<AnalyserIssue> {
            return when (asm) {
                "error" -> listOf(AnalyserIssue(AnalyserIssueKind.ERROR, null,"error"))
                "warning" -> listOf(AnalyserIssue(AnalyserIssueKind.WARNING, null,"error"))
                else -> throw RuntimeException("Test Error")
            }
        }
    }

    @Test
    fun error() {
        val asm = Any()
        val sut = TestSemanticAnalyser()
        val actual = sut.analyse("error")
    }
}