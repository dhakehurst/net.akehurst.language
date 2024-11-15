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

package net.akehurst.language.parser.leftcorner

import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.scanner.api.ScannerKind
import net.akehurst.language.issues.api.IssueCollection
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.parser.api.ParseOptions
import net.akehurst.language.parser.api.ParseResult
import net.akehurst.language.parser.api.RuleSet
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.common.ScannerClassic
import net.akehurst.language.scanner.common.ScannerOnDemand
import net.akehurst.language.sentence.common.SentenceDefault
import net.akehurst.language.sppt.api.SharedPackedParseTree
import net.akehurst.language.sppt.treedata.SPPTParserDefault
import testFixture.data.TestDataParser
import testFixture.data.TestDataParserSentenceFail
import testFixture.data.TestDataParserSentencePass
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.measureTimedValue

abstract class test_LeftCornerParserAbstract(val build: Boolean = false) {

    fun test(data:TestDataParser, sentence:String) {
        val td = data.sentences.first{ it.sentence == sentence }
        when(td) {
            is TestDataParserSentencePass -> test_pass(data.rrs, data.goal, td.sentence, td.expectedNumGSSHeads, *td.expectedSppt.toTypedArray())
            is TestDataParserSentenceFail -> test_fail(data.rrs, data.goal, td.sentence, td.expected)
        }
    }

    fun test_pass(rrs: RuleSet, goal: String, sentence: String, expectedNumGSSHeads: Int, vararg expectedTrees: String): SharedPackedParseTree? {
        return this.test(
            rrs as RuntimeRuleSet,
            goal,
            sentence,
            expectedNumGSSHeads,
            false,
            *expectedTrees
        )
    }

    fun test_fail(rrs: RuleSet, goal: String, sentence: String, expected:Set<LanguageIssue>, options: ParseOptions = ParseOptionsDefault(goal), scannerKind: ScannerKind = ScannerKind.OnDemand) {
        val scanner = when (scannerKind) {
            ScannerKind.OnDemand -> ScannerOnDemand(RegexEnginePlatform, rrs.terminals)
            ScannerKind.Classic -> ScannerClassic(RegexEnginePlatform, rrs.terminals)
        }
        val parser = LeftCornerParser(scanner, rrs as RuntimeRuleSet)
        if (build) parser.buildFor(options.goalRuleName!!)
        val r = parser.parse(sentence, options)
        println(r.issues)
        assertTrue(r.issues.isNotEmpty(), "Expected parse to fail")
        assertEquals(expected, r.issues.all)
    }

    fun test(rrs: RuleSet, goal: String, sentence: String, expectedNumGSSHeads: Int, printAutomaton: Boolean, vararg expectedTrees: String): SharedPackedParseTree? {
        return this.test2(
            rrs as RuntimeRuleSet,
            emptyMap(),
            goal,
            sentence,
            expectedNumGSSHeads,
            printAutomaton,
            *expectedTrees
        )
    }

    fun test2(
        rrs: RuleSet,
        embeddedRuntimeRuleSets: Map<String, RuntimeRuleSet>,
        goal: String,
        sentence: String,
        expectedNumGSSHeads: Int,
        printAutomaton: Boolean = false,
        vararg expectedTrees: String
    ): SharedPackedParseTree? =
        testWithOptions(
            rrs, embeddedRuntimeRuleSets, sentence, expectedNumGSSHeads, printAutomaton,
            ParseOptionsDefault(goal),
            ScannerKind.OnDemand,
            *expectedTrees
        )

    fun testWithOptions(
        rrs: RuleSet,
        embeddedRuntimeRuleSets: Map<String, RuntimeRuleSet> = emptyMap(),
        sentence: String,
        expectedNumGSSHeads: Int,
        printAutomaton: Boolean = false,
        options: ParseOptions = ParseOptionsDefault(),
        scannerKind: ScannerKind,
        vararg expectedTrees: String
    ): SharedPackedParseTree? {
        println("${this::class.simpleName} - '$sentence'")
        val scanner = when (scannerKind) {
            ScannerKind.OnDemand -> ScannerOnDemand(RegexEnginePlatform, rrs.terminals)
            ScannerKind.Classic -> ScannerClassic(RegexEnginePlatform, rrs.terminals)
        }
        scanner.matchables
        val parser = LeftCornerParser(scanner, rrs as RuntimeRuleSet)
        if (build) parser.buildFor(options.goalRuleName!!)

        options.reportGrammarAmbiguities = true
        val (result, duration) = measureTimedValue {
            parser.parse(sentence, options)
        }
        println("Duration: $duration")
        if (printAutomaton) println(rrs.usedAutomatonToString(options.goalRuleName!!))
        if(result.issues.isNotEmpty()) println(result.issues)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString()) //TODO: check all, not error
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        val expectedSppt = SPPTParserDefault(rrs, embeddedRuntimeRuleSets.mapKeys { it.key })
        expectedTrees.forEach { expectedSppt.addTree(it) }
        val expected = expectedSppt.tree
        assertEquals(expected.toStringAllWithIndent("  ", true).trim(), result.sppt!!.toStringAllWithIndent("  ", true).trim())

        assertEquals(sentence, result.sppt!!.asSentence)

        //FIXME: add back this assert
//        assertEquals(expected, result.sppt)
//        assertEqualsWarning(expectedNumGSSHeads, result.sppt!!.maxNumHeads, "Too many heads on GSS")
        assertEquals(expectedNumGSSHeads, result.sppt!!.maxNumHeads, "Too many heads on GSS")
        assertTrue(parser.runtimeDataIsEmpty, "Runtime data not empty")
        return result.sppt
    }

    fun testFail(rrs: RuleSet, goal: String, sentence: String, expectedNumGSSHeads: Int): Pair<SharedPackedParseTree?, IssueCollection<LanguageIssue>> {
        val r = testFailWithOptions(rrs, sentence, expectedNumGSSHeads, ParseOptionsDefault(goal), ScannerKind.OnDemand)
        return Pair(r.sppt, r.issues)
    }

    fun testFailWithOptions(rrs: RuleSet, sentence: String, expectedNumGSSHeads: Int, options: ParseOptions, scannerKind: ScannerKind): ParseResult {
        val scanner = when (scannerKind) {
            ScannerKind.OnDemand -> ScannerOnDemand(RegexEnginePlatform, rrs.terminals)
            ScannerKind.Classic -> ScannerClassic(RegexEnginePlatform, rrs.terminals)
        }
        val parser = LeftCornerParser(scanner, rrs as RuntimeRuleSet)
        if (build) parser.buildFor(options.goalRuleName!!)
        val r = parser.parse(sentence, options)
        println(r.issues)
        return r
    }

    protected fun parseError(location: InputLocation, sentence: String, tryingFor:Set<String>, expected: Set<String>):LanguageIssue {
        val failed = tryingFor.sorted().joinToString(separator = " | ")
        val posIndication = SentenceDefault(sentence).contextInText(location.position)
        val message = "Failed to match {$failed} at: $posIndication"
        return LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.PARSE, location, message, expected)
    }

    @Deprecated("", ReplaceWith("parseError(InputLocation, sentence, tryingFor, expected)"))
    protected fun parseError(location: InputLocation, message: String, expected: Set<String>) =
        LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.PARSE, location, message, expected)

    /*
    fun test(rrsb:RuntimeRuleSetBuilder, goal:String, sentence:String, vararg expectedTrees:String) : SharedPackedParseTree {
        val parser = ScanOnDemandParser(rrsb.ruleSet())
        val actual = parser.parseForGoal(goal, sentence, AutomatonKind.LOOKAHEAD_1)

        val sppt = SPPTParserDefault(rrsb.ruleSet())
        expectedTrees.forEach { sppt.addTree(it) }
        val expected = sppt.tree
        assertEquals(expected, actual)
        return actual
    }

    fun testStringResult(rrsb:RuntimeRuleSetBuilder, goal:String, sentence:String, vararg expectedTrees:String) : SharedPackedParseTree {
        val parser = ScanOnDemandParser(rrsb.ruleSet())
        val actual = parser.parseForGoal(goal, sentence, AutomatonKind.LOOKAHEAD_1)

        val sppt = SPPTParserDefault(rrsb.ruleSet())
        expectedTrees.forEach { sppt.addTree(it) }
        val expected = sppt.tree
        assertEquals(expected.toStringAllWithIndent("  "), actual.toStringAllWithIndent("  "))
        assertEquals(expected, actual)
        return actual
    }
 */
}
