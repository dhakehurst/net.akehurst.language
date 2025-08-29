/*
 * Copyright (C) 2024 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.types.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.processor.contextFromGrammarRegistry
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.processor.AglBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_AglTypes {

    @Test
    fun process_grammarString_EQ_grammarModel() {
        val res = Agl.registry.agl.grammar.processor!!.process(
            AglBase.grammarString + "\n" + AglTypes.grammarString,
            Agl.options {
                semanticAnalysis {
                    context(contextFromGrammarRegistry(Agl.registry))
                }
            }
        )
        assertTrue(res.allIssues.errors.isEmpty(), res.allIssues.toString())
        val actual = res.asm!!.asString()
        val expected = AglTypes.grammarDomain.asString()

        assertEquals(expected, actual)
    }

    @Test
    fun typemodel() {
        val actual = AglTypes.typesDomain

        assertNotNull(actual)
    }


    @Test
    fun domainTypes() {
        val td = AglTypes.typesDomain.findFirstDefinitionByNameOrNull(SimpleName("TypesDomain"))
        assertNotNull(td)
        assertEquals("TypesDomain", td.name.value)
        assertEquals("Domain", td.supertypes[0].typeName.value)
        assertEquals("TypesNamespace", td.supertypes[0].typeArguments[0].type.typeName.value)
        assertEquals("TypeDefinition", td.supertypes[0].typeArguments[1].type.typeName.value)
    }
}