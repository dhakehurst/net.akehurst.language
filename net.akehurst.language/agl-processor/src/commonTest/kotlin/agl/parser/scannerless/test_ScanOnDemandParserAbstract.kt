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
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.agl.sppt.SPPTParser
import net.akehurst.language.api.processor.AutomatonKind
import net.akehurst.language.api.sppt.SharedPackedParseTree
import kotlin.test.assertEquals

internal abstract class test_ScanOnDemandParserAbstract {

    fun test(rrs:RuntimeRuleSet, goal:String, sentence:String, expectedNumGSSHeads:Int, vararg expectedTrees:String) : SharedPackedParseTree {
        val parser = ScanOnDemandParser(rrs)
        val actual = parser.parse(goal, sentence, AutomatonKind.LOOKAHEAD_1)

        val sppt = SPPTParser(rrs)
        expectedTrees.forEach { sppt.addTree(it) }
        val expected = sppt.tree
        assertEquals(expected.toStringAllWithIndent("  "), actual.toStringAllWithIndent("  "))
        assertEquals(expected, actual)
        //FIXME: add back this assert
        assertEquals(expectedNumGSSHeads, actual.maxNumHeads,"Too many heads on GSS")
        return actual
    }

    fun test(rrsb:RuntimeRuleSetBuilder, goal:String, sentence:String, vararg expectedTrees:String) : SharedPackedParseTree {
        val parser = ScanOnDemandParser(rrsb.ruleSet())
        val actual = parser.parse(goal, sentence, AutomatonKind.LOOKAHEAD_1)

        val sppt = SPPTParser(rrsb.ruleSet())
        expectedTrees.forEach { sppt.addTree(it) }
        val expected = sppt.tree
        assertEquals(expected, actual)
        return actual
    }

    fun testStringResult(rrsb:RuntimeRuleSetBuilder, goal:String, sentence:String, vararg expectedTrees:String) : SharedPackedParseTree {
        val parser = ScanOnDemandParser(rrsb.ruleSet())
        val actual = parser.parse(goal, sentence, AutomatonKind.LOOKAHEAD_1)

        val sppt = SPPTParser(rrsb.ruleSet())
        expectedTrees.forEach { sppt.addTree(it) }
        val expected = sppt.tree
        assertEquals(expected.toStringAllWithIndent("  "), actual.toStringAllWithIndent("  "))
        assertEquals(expected, actual)
        return actual
    }
}