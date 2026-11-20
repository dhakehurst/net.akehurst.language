/*
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
 *
 */
package net.akehurst.language.parser.leftcorner

import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.parsermessages.IssueMessage
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.common.ScannerOnDemand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class test_ParserWontStop {

    @Test
    fun list_of_optional__reports_issue_instead_of_throwing() {
        // S = A* ; A = 'a'? ;  -- currently makes the parser loop on empty A
        val rrs = runtimeRuleSet {
            multi("S", 0, -1, "A")
            optional("A", "'a'")
            literal("a")
        } as RuntimeRuleSet
        val sut = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.terminals), rrs)

        val res = sut.parse("aa", ParseOptionsDefault(goalRuleName = "S"))

        assertNull(res.sppt)
        val issues = res.issues.all.toList()
        assertEquals(1, issues.size, issues.toString())
        assertEquals(LanguageIssueKind.ERROR, issues[0].kind)
        assertEquals(LanguageProcessorPhase.PARSE, issues[0].phase)
        assertEquals(IssueMessage.PARSER_WONT_STOP, issues[0].message)
    }
}
