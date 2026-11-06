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

import kotlin.test.Test
import kotlin.test.assertEquals

class test_ParseOptionsDefault {

    @Test
    fun clone_copies_all_fields() {
        val orig = ParseOptionsDefault(
            enabled = false,
            goalRuleName = "S",
            reportErrors = false,
            reportGrammarAmbiguities = true,
            cacheSkip = false,
            reverseFindWordStartRegex = "[a-z]+",
            reverseFindWordStartBySkipRules = false,
            expectedAtFromAllTokenStarts = true,
            streamListener = object : net.akehurst.language.parser.api.ParseStreamListener {
                override fun leaf(node: net.akehurst.language.sppt.api.SpptDataNode) {}
                override fun skip(startPosition: Int, nextInputPosition: Int) {}
                override fun branch(node: net.akehurst.language.sppt.api.SpptDataNode, numChildren: Int) {}
                override fun finished(issues: net.akehurst.language.issues.api.IssueCollection<net.akehurst.language.issues.api.LanguageIssue>) {}
                override fun failed(issues: net.akehurst.language.issues.api.IssueCollection<net.akehurst.language.issues.api.LanguageIssue>, committedUpTo: Int) {}
            },
            streamLagWarningLimit = 42
        )
        val actual = orig.clone()
        assertEquals(orig.enabled, actual.enabled)
        assertEquals(orig.goalRuleName, actual.goalRuleName)
        assertEquals(orig.sentenceIdentity, actual.sentenceIdentity)
        assertEquals(orig.reportErrors, actual.reportErrors)
        assertEquals(orig.reportGrammarAmbiguities, actual.reportGrammarAmbiguities)
        assertEquals(orig.cacheSkip, actual.cacheSkip)
        assertEquals(orig.reverseFindWordStartRegex, actual.reverseFindWordStartRegex)
        assertEquals(orig.reverseFindWordStartBySkipRules, actual.reverseFindWordStartBySkipRules)
        assertEquals(orig.expectedAtFromAllTokenStarts, actual.expectedAtFromAllTokenStarts)
        assertEquals(orig.streamListener, actual.streamListener)
        assertEquals(orig.streamLagWarningLimit, actual.streamLagWarningLimit)
    }
}
