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

package net.akehurst.language.parser.expectedTerminalsAt

import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.parser.leftcorner.LeftCornerParser
import net.akehurst.language.parser.leftcorner.ParseOptionsDefault
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.common.ScannerOnDemand
import kotlin.test.assertEquals

internal abstract class test_ExpectedTerminasAtAbstract {

    protected data class TestData(val sentence: String, val position: Int, val expected: Set<String>)

    protected fun test(rrs: RuntimeRuleSet, goal: String, data: TestData) {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.terminals), rrs)
        val result = parser.expectedTerminalsAt(data.sentence, data.position, ParseOptionsDefault(goal))

        val actual = result.filter { it.isEmptyTerminal.not() && it.isEmptyListTerminal.not() && it.isSkip.not() }.map { (it as RuntimeRule).rhs.toString() }.toSet()
        val expected = data.expected
        assertEquals(expected, actual, data.toString())
    }

}