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

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.api.runtime.RuleSet
import net.akehurst.language.agl.parser.LeftCornerParser
import net.akehurst.language.agl.regex.RegexEnginePlatform
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.scanner.ScannerClassic
import net.akehurst.language.agl.scanner.ScannerOnDemand
import net.akehurst.language.agl.sppt.SPPTParserDefault
import net.akehurst.language.api.language.base.QualifiedName
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.*
import net.akehurst.language.api.sppt.SharedPackedParseTree
import test.assertEqualsWarning
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.measureTimedValue

internal abstract class test_LeftCornerParserAbstract(val build: Boolean = false) {
    fun test(rrs: RuleSet, goal: String, sentence: String, expectedNumGSSHeads: Int, vararg expectedTrees: String): SharedPackedParseTree? {
        return this.test(
            rrs as RuntimeRuleSet,
            goal,
            sentence,
            expectedNumGSSHeads,
            false,
            *expectedTrees
        )
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
            Agl.options { parse { goalRuleName(goal) } },
            ScannerKind.OnDemand,
            *expectedTrees
        )

    fun testWithOptions(
        rrs: RuleSet,
        embeddedRuntimeRuleSets: Map<String, RuntimeRuleSet> = emptyMap(),
        sentence: String,
        expectedNumGSSHeads: Int,
        printAutomaton: Boolean = false,
        options: ProcessOptions<Any, Any> = Agl.options { },
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
        if (build) parser.buildFor(options.parse.goalRuleName!!)
        val (result, duration) = measureTimedValue {
            parser.parse(sentence, options.parse)
        }
        println("Duration: $duration")
        if (printAutomaton) println(rrs.usedAutomatonToString(options.parse.goalRuleName!!))
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString()) //TODO: check all, not error
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        val sppt = SPPTParserDefault(rrs, embeddedRuntimeRuleSets.mapKeys { QualifiedName(it.key) })
        expectedTrees.forEach { sppt.addTree(it) }
        val expected = sppt.tree
        assertEquals(expected.toStringAllWithIndent("  ", true).trim(), result.sppt!!.toStringAllWithIndent("  ", true).trim())
        //FIXME: add back this assert
//        assertEquals(expected, result.sppt)
        assertEqualsWarning(expectedNumGSSHeads, result.sppt!!.maxNumHeads, "Too many heads on GSS")
        assertTrue(parser.runtimeDataIsEmpty, "Runtime data not empty")
        return result.sppt
    }

    fun testFail(rrs: RuleSet, goal: String, sentence: String, expectedNumGSSHeads: Int): Pair<SharedPackedParseTree?, IssueCollection<LanguageIssue>> {
        val r = testFailWithOptions(rrs, sentence, expectedNumGSSHeads, Agl.options { parse { goalRuleName(goal) } }, ScannerKind.OnDemand)
        return Pair(r.sppt, r.issues)
    }

    fun testFailWithOptions(rrs: RuleSet, sentence: String, expectedNumGSSHeads: Int, options: ProcessOptions<Any, Any>, scannerKind: ScannerKind): ParseResult {
        val scanner = when (scannerKind) {
            ScannerKind.OnDemand -> ScannerOnDemand(RegexEnginePlatform, rrs.terminals)
            ScannerKind.Classic -> ScannerClassic(RegexEnginePlatform, rrs.terminals)
        }
        val parser = LeftCornerParser(scanner, rrs as RuntimeRuleSet)
        if (build) parser.buildFor(options.parse.goalRuleName!!)
        val r = parser.parse(sentence, options.parse)
        return r
    }

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
