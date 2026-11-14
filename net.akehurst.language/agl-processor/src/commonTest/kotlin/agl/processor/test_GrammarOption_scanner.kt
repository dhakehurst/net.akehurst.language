/**
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.akehurst.language.agl.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.regex.agl.RegexEngineAgl
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.regex.api.RegexEngineKind
import net.akehurst.language.scanner.api.ScannerKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** grammar options '#scanner' and '#regexEngine' */
class test_GrammarOption_scanner {

    private companion object {
        fun grammarWithOptions(options: String) = """
            namespace test
            grammar Test {
                $options
                skip leaf WS = "\s+" ;
                unit = decl* ;
                decl = 'var' ID ';' ;
                leaf ID = "[a-z]+" ;
            }
        """.trimIndent()

        fun processorFor(options: String) = Agl.processorFromStringSimple(GrammarString(grammarWithOptions(options))).also {
            assertTrue(it.issues.errors.isEmpty(), it.issues.toString())
        }.processor!!
    }

    @Test
    fun no_option__OnDemand() {
        val p = processorFor("")
        assertEquals(ScannerKind.OnDemand, p.scanner!!.kind)
        // with OnDemand a keyword can be a name
        assertTrue(p.parse("var var;").issues.errors.isEmpty())
    }

    @Test
    fun scanner_Classic() {
        val p = processorFor("#scanner: Classic")
        assertEquals(ScannerKind.Classic, p.scanner!!.kind)
        assertTrue(p.parse("var x;").issues.errors.isEmpty())
        // with Classic a keyword is reserved
        assertTrue(p.parse("var var;").issues.errors.isNotEmpty())
    }

    @Test
    fun scanner_Classic_as_string() {
        val p = processorFor("#scanner: 'Classic'")
        assertEquals(ScannerKind.Classic, p.scanner!!.kind)
    }

    @Test
    fun scanner_OnDemand() {
        val p = processorFor("#scanner: OnDemand")
        assertEquals(ScannerKind.OnDemand, p.scanner!!.kind)
    }

    @Test
    fun scanner_option_on_namespace() {
        val g = grammarWithOptions("").replace("namespace test", "namespace test\n#scanner: Classic")
        val p = Agl.processorFromStringSimple(GrammarString(g)).processor!!
        assertEquals(ScannerKind.Classic, p.scanner!!.kind)
    }

    @Test
    fun scanner_unknown_value__warning_and_default() {
        val res = Agl.processorFromStringSimple(GrammarString(grammarWithOptions("#scanner: Clasic")))
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        assertTrue(res.issues.warnings.any { it.message.contains("'scanner'") && it.message.contains("Clasic") }, res.issues.toString())
        assertEquals(ScannerKind.OnDemand, res.processor!!.scanner!!.kind)
    }

    @Test
    fun explicit_configuration_wins_over_grammar_option() {
        val p = Agl.processorFromStringSimple(
            GrammarString(grammarWithOptions("#scanner: Classic")),
            configurationBase = Agl.configuration(Agl.configurationSimple()) { scanner(ScannerKind.OnDemand, RegexEngineKind.PLATFORM) }
        ).processor!!
        assertEquals(ScannerKind.OnDemand, p.scanner!!.kind)
    }

    @Test
    fun regexEngine_AGL() {
        val p = processorFor("#regexEngine: AGL")
        assertEquals(RegexEngineAgl, p.scanner!!.regexEngine)
        assertTrue(p.parse("var x;").issues.errors.isEmpty())
    }

    @Test
    fun regexEngine_default_PLATFORM() {
        val p = processorFor("")
        assertEquals(RegexEnginePlatform, p.scanner!!.regexEngine)
    }

    @Test
    fun processorFromString_uses_option() {
        val p = Agl.processorFromString<Any, Any>(grammarWithOptions("#scanner: Classic")).processor!!
        assertEquals(ScannerKind.Classic, p.scanner!!.kind)
    }
}
