package agl.semanticAnalyser

import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyserItem
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyserItemKind
import kotlin.test.Test

class test_SemanticAnalyser {

    class TestSemanticAnalyser : SemanticAnalyser<Any> {
        override fun clear() {
            TODO("not implemented")
        }

        override fun analyse(asm: Any, locationMap: Map<Any, InputLocation>): List<SemanticAnalyserItem> {
            return when (asm) {
                "error" -> listOf(SemanticAnalyserItem(SemanticAnalyserItemKind.ERROR, null,"error"))
                "warning" -> listOf(SemanticAnalyserItem(SemanticAnalyserItemKind.WARNING, null,"error"))
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