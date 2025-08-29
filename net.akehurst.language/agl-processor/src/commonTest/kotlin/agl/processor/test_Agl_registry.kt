/**
 * Copyright (C) 2025 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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
import net.akehurst.language.api.processor.LanguageDefinition
import net.akehurst.language.api.processor.LanguageObject
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.base.api.Formatable
import net.akehurst.language.base.processor.AglBase
import net.akehurst.language.expressions.processor.AglExpressions
import net.akehurst.language.grammar.processor.AglGrammar
import net.akehurst.language.grammar.processor.contextFromGrammar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_Agl_registry_agl {

    companion object {
        fun <AsmType : Formatable, ContextType:Any> checkStringEqualsModel(processor: LanguageProcessor<AsmType,ContextType>, expected:String, actual: Formatable, context:ContextType ) {
            val exp = processor.process(expected,Agl.options { semanticAnalysis { context(context) } }).let {
                assertTrue(it.allIssues.errors.isEmpty(), it.allIssues.toString())
                it.asm!!
            }
            assertEquals(exp.asString(), actual.asString())
         }

        fun <AsmType : Formatable, ContextType:Any> checkModelEqualsString(processor: LanguageProcessor<AsmType,ContextType>, expected:Formatable, actual: String, context:ContextType ) {
            val act = processor.process(actual,Agl.options { semanticAnalysis { context(context) } }).let {
                assertTrue(it.allIssues.errors.isEmpty(), it.allIssues.toString())
                it.asm!!
            }
            assertEquals(expected.asString(), act.asString())
        }

        fun <AsmType : Any, ContextType : Any> checkLanguageDefinition(expected: LanguageObject<AsmType, ContextType>, actual: LanguageDefinition<AsmType, ContextType>) {
            assertEquals(expected.identity.value, actual.identity.value)
            checkStringEqualsModel(Agl.registry.agl.grammar.processor!!, expected.grammarString, actual.grammarDomain!!,contextFromGrammarRegistry(Agl.registry))
            checkModelEqualsString(Agl.registry.agl.grammar.processor!!, expected.grammarDomain, actual.grammarString!!.value,contextFromGrammarRegistry(Agl.registry))

            assertEquals(expected.allTypesString, actual.typesString?.value, "typesString doesn't match")

            assertEquals(expected.asmTransformString, actual.asmTransformString?.value, "asmTransformString doesn't match")

            assertEquals(expected.crossReferenceString, actual.crossReferenceString?.value, "crossReferenceString doesn't match")

            checkStringEqualsModel(Agl.registry.agl.style.processor!!, expected.styleString, actual.styleDomain!!, contextFromGrammar(expected.grammarDomain))
            checkModelEqualsString(Agl.registry.agl.style.processor!!, expected.styleDomain, actual.styleString!!.value, contextFromGrammar(actual.grammarDomain!!))

            assertEquals((expected.formatString), actual.formatString?.value)
            //TODO
        }

        fun checkProcessor(processor: LanguageProcessor<*, *>?) {
            assertNotNull(processor)
            assertNotNull(processor.configuration)
            assertTrue(processor.issues.isEmpty())
            assertNotNull(processor.grammarDomain)
            assertNotNull(processor.targetGrammar)
            assertNotNull(processor.targetRuleSet)
            assertNotNull(processor.scanner)
            assertNotNull(processor.spptParser)
            assertNotNull(processor.parser)
            assertNotNull(processor.baseTypesDomain)
            assertNotNull(processor.typesDomain)
            assertNotNull(processor.transformDomain)
            assertNotNull(processor.targetTransformRuleSet)
            assertNotNull(processor.crossReferenceDomain)
            assertNotNull(processor.formatDomain)
            assertNotNull(processor.syntaxAnalyser)
            assertNotNull(processor.semanticAnalyser)
            assertNotNull(processor.formatter)
            assertNotNull(processor.completionProvider)
        }
    }

    @Test
    fun test_Agl_registry_agl_base() {
        checkLanguageDefinition(AglBase, Agl.registry.agl.base)
        checkProcessor(Agl.registry.agl.base.processor)
    }

    @Test
    fun test_Agl_registry_agl_grammar() {
        Agl.registry.agl.base
        checkLanguageDefinition(AglGrammar, Agl.registry.agl.grammar)
        checkProcessor(Agl.registry.agl.grammar.processor)
    }

    @Test
    fun test_Agl_registry_agl_expressions() {
        checkLanguageDefinition(AglExpressions, Agl.registry.agl.expressions)
        checkProcessor(Agl.registry.agl.expressions.processor)
    }

    @Test
    fun test_Agl_registry_agl_types() {
        checkProcessor(Agl.registry.agl.types.processor)
    }

    @Test
    fun test_Agl_registry_agl_asmTransform() {
        checkProcessor(Agl.registry.agl.asmTransform.processor)
    }

    @Test
    fun test_Agl_registry_agl_m2mTransform() {
        checkProcessor(Agl.registry.agl.m2mTransform.processor)
    }

    @Test
    fun test_Agl_registry_agl_crossReference() {
        checkProcessor(Agl.registry.agl.crossReference.processor)
    }

    @Test
    fun test_Agl_registry_agl_style() {
        checkProcessor(Agl.registry.agl.style.processor)
    }

    @Test
    fun test_Agl_registry_agl_format() {
        checkProcessor(Agl.registry.agl.format.processor)
    }
}