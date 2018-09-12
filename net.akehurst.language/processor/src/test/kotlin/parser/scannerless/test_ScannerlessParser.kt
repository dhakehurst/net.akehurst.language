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

package net.akehurst.language.parser.scannerless

import net.akehurst.language.ogl.runtime.converter.Converter
import net.akehurst.language.ogl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.ogl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.ogl.semanticStructure.GrammarBuilderDefault
import net.akehurst.language.ogl.semanticStructure.NamespaceDefault
import net.akehurst.language.parser.scannerless.ScannerlessParser
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.fail

class test_ScannerlessParser {

    @Test
    fun construct() {
        val rrb = RuntimeRuleSetBuilder()
        val sp = ScannerlessParser(rrb.ruleSet())

        assertNotNull(sp)
    }

    @Test
    fun build() {
        val rrb = RuntimeRuleSetBuilder()
        val sp = ScannerlessParser(rrb.ruleSet())
        sp.build()

        fail("must test if build worked!")
    }

    @Test
    fun parse() {
        val rrb = RuntimeRuleSetBuilder()
        val r0 = rrb.literal("a")
        val r1 = rrb.rule("a").concatenation(r0).build()
        val sp = ScannerlessParser(rrb.ruleSet())

        sp.parse("a","a")
    }

    @Test
    fun expectedAt() {
        val rrb = RuntimeRuleSetBuilder()
        val sp = ScannerlessParser(rrb.ruleSet())

        sp.expectedAt("","",0)
    }

}