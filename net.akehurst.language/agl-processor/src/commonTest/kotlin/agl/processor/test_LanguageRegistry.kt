/**
 * Copyright (C) 2021 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.api.processor.LanguageIdentity
import net.akehurst.language.grammar.processor.AglGrammarSemanticAnalyser
import net.akehurst.language.grammar.processor.ContextFromGrammarRegistry
import kotlin.test.*

class test_LanguageRegistry {

    private companion object {
        val identity = LanguageIdentity("test.lang")
    }

    @BeforeTest
    fun before() {
        Agl.registry.unregister(identity)
    }

    @Test
    fun register_empty() {
        val languageDefinition = Agl.registry.register(
            identity = identity,
            buildForDefaultGoal = false,
            aglOptions = Agl.options {
                semanticAnalysis {
                    context(ContextFromGrammarRegistry(Agl.registry))
                    option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, false)
                }
            },
            configuration = Agl.configurationSimple()
        )

        assertEquals(identity, languageDefinition.identity)
        assertTrue(languageDefinition.issues.isNotEmpty(), languageDefinition.issues.toString())
        assertNull(languageDefinition.processor)
    }

    @Test
    fun register_valid() {
        val languageDefinition = Agl.registry.register(
            identity = identity,
            buildForDefaultGoal = false,
            aglOptions = Agl.options {
                semanticAnalysis {
                    context(ContextFromGrammarRegistry(Agl.registry))
                    option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, false)
                }
            },
            configuration =Agl.configuration(base= Agl.configurationSimple()) {
                grammarString(GrammarString("namespace ns grammar Test { S = 'b'; }"))
            }
        )

        assertEquals(identity, languageDefinition.identity)
        assertTrue(languageDefinition.issues.isEmpty())
        assertNotNull(languageDefinition.processor)
    }

    @Test
    fun register_update_empty_to_valid() {
        val languageDefinition = Agl.registry.register(
            identity = identity,
            buildForDefaultGoal = false,
            aglOptions = Agl.options {
                semanticAnalysis {
                    context(ContextFromGrammarRegistry(Agl.registry))
                    option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, false)
                }
            },
            configuration =Agl.configuration(base= Agl.configurationSimple()) {
                grammarString(GrammarString(""))
            }
        )

        assertEquals(identity, languageDefinition.identity)
        assertTrue(languageDefinition.issues.isNotEmpty())
        assertNull(languageDefinition.processor)

        languageDefinition.update(GrammarString("namespace ns grammar Test { S = 'b'; }"), null, null, null, null)
        assertEquals(identity, languageDefinition.identity)
        assertTrue(languageDefinition.issues.isEmpty())
        assertNotNull(languageDefinition.processor)
    }

}