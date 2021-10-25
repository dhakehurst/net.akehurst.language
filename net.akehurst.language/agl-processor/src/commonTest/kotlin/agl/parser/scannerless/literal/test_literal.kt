/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.parser.scanondemand

import net.akehurst.language.agl.parser.ScanOnDemandParser
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.api.processor.AutomatonKind
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.LanguageIssueKind
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.api.sppt.SharedPackedParseTree
import kotlin.test.*

internal class test_literal : test_ScanOnDemandParserAbstract() {

    //  S = 'a'
    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("a") }
        }
        val goal = "S"
    }

    @Test
    fun a() {
        val sentence = "a"

        val expected = """
        """.trimIndent()

        super.test(
            rrs = rrs,
            goal=goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expected
        )

    }

    @Test
    fun b_fails() {
        val sentence = "b"

        val (sppt, issues) = super.testFail(rrs, goal, sentence, expectedNumGSSHeads = 1)
        assertNull(sppt)
        assertEquals(listOf(
            LanguageIssue(LanguageIssueKind.ERROR,LanguageProcessorPhase.PARSE, InputLocation(0,1,1,1),"")
        ),issues)
    }

}