/**
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.akehurst.language.parser.leftcorner

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parsermessages.IssueMessage
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.common.ScannerOnDemand
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class test_interrupt {

    // S = S S | 'a' ; is very ambiguous, so parsing a long sentence takes a long time
    private val rrs = runtimeRuleSet {
        choice("S", RuntimeRuleChoiceKind.AMBIGUOUS) {
            concatenation { ref("S"); ref("S") }
            literal("a")
        }
    } as RuntimeRuleSet

    @Test
    fun interrupt_gives_result_with_issue_not_exception() {
        val sut = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.terminals), rrs)
        val sentence = "a".repeat(500)

        var done = false
        val interrupter = thread {
            while (!done) {
                sut.interrupt("test stop")
                Thread.sleep(5)
            }
        }
        val result = sut.parse(sentence, ParseOptionsDefault(goalRuleName = "S"))
        done = true
        interrupter.join()

        assertNull(result.sppt)
        assertEquals(1, result.issues.errors.size, result.issues.toString())
        assertEquals(IssueMessage.parserTerminated("test stop"), result.issues.errors.first().message)
    }

    @Test
    fun parse_after_interrupt_is_not_interrupted() {
        val sut = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.terminals), rrs)
        sut.interrupt("before parse")
        val result = sut.parse("aa", ParseOptionsDefault(goalRuleName = "S"))
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
    }
}
