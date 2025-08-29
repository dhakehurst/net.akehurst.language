/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.expressions.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.processor.contextFromGrammarRegistry
import net.akehurst.language.agl.simple.contextAsmSimpleWithAsmPath
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.processor.AglBase
import net.akehurst.language.types.api.PropertyName
import net.akehurst.language.types.asm.StdLibDefault
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_AglExpressions {

        @Test
    fun identity() {
            assertEquals("net.akehurst.language.Expressions", AglExpressions.identity.value)
    }

    @Test
    fun process_grammarString_EQ_grammarModel() {
        val res = Agl.registry.agl.grammar.processor!!.process(
            AglBase.grammarString + "\n" + AglExpressions.grammarString,
            Agl.options {
                semanticAnalysis {
                    context(contextFromGrammarRegistry(Agl.registry))
                }
            }
        )
        assertTrue(res.allIssues.errors.isEmpty(), res.allIssues.toString())
        val actual = res.asm!!.asString()
        val expected = AglExpressions.grammarDomain.asString()

        assertEquals(expected, actual)
    }

    @Test
    fun process_typesString_EQ_typesModel() {
        val res = Agl.registry.agl.types.processor!!.process(
           AglBase.typesString + "\n" +
                   AglExpressions.typesString,
            Agl.options {
                semanticAnalysis {
                    context(contextAsmSimpleWithAsmPath())
                }
            }
        )
        assertTrue(res.allIssues.errors.isEmpty(), res.allIssues.toString())
        res.asm!!.addNamespace(StdLibDefault)
        res.asm!!.resolveImports()
        val actual = res.asm!!.asString()
        val expected = AglExpressions.typesDomain.asString()

        assertEquals(expected, actual)
    }

    @Test
    fun process_transformString_EQ_transformModel() {
        val res = Agl.registry.agl.asmTransform.processor!!.process(
            // AglBase.typesString + "\n" +
            AglExpressions.asmTransformString, // this is created from asString on the model, thus base namespace is already included!
            Agl.options {
                semanticAnalysis {
                    //context(ContextAsmSimpleWithScopePath())
                }
            }
        )
        assertTrue(res.allIssues.errors.isEmpty(), res.allIssues.toString())
        val actual = res.asm!!.asString()
        val expected = AglExpressions.asmTransformDomain.asString()

        assertEquals(expected, actual)
    }

    @Test
    fun grammarModel_EQ_grammarString() {
        val actual = AglExpressions.grammarDomain.asString()
        val expected = AglExpressions.grammarString

        assertEquals(expected, actual)
    }

    @Test
    fun typeModel() {
        val actual = AglExpressions.typesDomain
        assertNotNull(actual)
    }

    @Test
    fun transformModel_EQ_transformString() {
        val actual = AglExpressions.asmTransformDomain.asString()
        val expected = AglExpressions.asmTransformString

        assertEquals(expected, actual)
    }

    @Test
    fun styleModel_EQ_styleString() {
        val actual = AglExpressions.styleDomain.asString()
        val expected = AglExpressions.styleString

        assertEquals(expected, actual)
    }

}