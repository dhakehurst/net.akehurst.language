package format

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.format.processor.AglFormat
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The parse result of the format processor must not depend on what the same processor parsed before,
 * nor on whether the automaton was built up front (buildFor) or lazily.
 * Found 2026-11-20 with the omg Reflective.agl-fmt / ClassesRam.agl-fmt templates.
 */
class test_AglFormat_parseHistory {
    private companion object {
        fun fresh(): LanguageProcessor<Any, Any> = Agl.processorFromGrammar(
            AglFormat.grammarDomain,
            Agl.configuration { targetGrammarName("Format"); defaultGoalRuleName("unit") }
        )

        const val P = "namespace n\nformat U{y ->{s:=when{d()->''else->''}\"\"}o ->{m:=e?e.v:u when{i ->{\"\"}else->\"\"}}}"
        const val T = "namespace e\nformat M{l ->{s:=r({t ->false==s()})\"\"}}"

        fun assertParses(proc: LanguageProcessor<Any, Any>, sentence: String, context: String) {
            val res = proc.parse(sentence)
            assertTrue(res.issues.errors.isEmpty(), "$context: ${res.issues}")
        }
    }

    @Test
    fun T_alone() = assertParses(fresh(), T, "T")

    @Test
    fun P_alone() = assertParses(fresh(), P, "P")

    @Test
    fun T_then_T() {
        val proc = fresh()
        assertParses(proc, T, "T first")
        assertParses(proc, T, "T second")
    }

    @Test
    fun P_then_T() {
        val proc = fresh()
        assertParses(proc, P, "P first")
        assertParses(proc, T, "T after P")
    }

    @Test
    fun T_then_P() {
        val proc = fresh()
        assertParses(proc, T, "T first")
        assertParses(proc, P, "P after T")
    }

    @Test
    fun buildFor_then_T() {
        val proc = fresh()
        proc.buildFor()
        assertParses(proc, T, "T after buildFor")
    }

    @Test
    fun buildFor_then_P() {
        val proc = fresh()
        proc.buildFor()
        assertParses(proc, P, "P after buildFor")
    }
}
