/*
 * Copyright (C) 2025 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.grammar.style

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.language.reference.test_LanguageObjectAbstract
import net.akehurst.language.agl.processor.contextFromGrammarRegistry
import net.akehurst.language.agl.processor.contextFromRegistryStyles
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.api.processor.ProcessOptions
import net.akehurst.language.base.processor.AglBase
import net.akehurst.language.grammar.processor.contextFromGrammar
import net.akehurst.language.style.api.AglStyleDomain
import net.akehurst.language.style.processor.AglStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class test_AglStyle : test_LanguageObjectAbstract() {

    override val sut = AglStyle
    override val processStyleOptions: ProcessOptions<AglStyleDomain, SentenceContext> = Agl.options {
        semanticAnalysis {
            sentenceContext(contextFromGrammar(AglStyle.grammarDomain).union(contextFromRegistryStyles()))
        }
    }

    @Test
    fun styles() {
        assertEquals(4,sut.styleDomain.allDefinitions.first().rules.size)
        assertEquals(8,sut.styleDomain.allDefinitions.first().allRules.size)
    }

    @Test
    fun grammarStr() {
        val combined = AglBase.grammarString +"\n"+AglStyle.grammarString
        val res = Agl.registry.agl.grammar.processor!!.process(combined, Agl.options { semanticAnalysis { sentenceContext(contextFromGrammarRegistry()) } })
        assertTrue(res.allIssues.errors.isEmpty(), res.allIssues.toString())
    }

    @Test
    fun styleStr() {
        val res = Agl.registry.agl.style.processor!!.process(AglStyle.styleString)
        assertTrue(res.allIssues.errors.isEmpty(), res.allIssues.toString())
    }

    @Test
    fun grammarModel() {

       // assertEquals(AglStyle.styleStr, AglStyle.st)

    }


}