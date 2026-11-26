/*
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.asmTransform.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.processor.contextFromGrammarAndTypesDomain
import net.akehurst.language.api.processor.AsmTransformString
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.asmTransform.asm.AsmTransformDomainDefault
import net.akehurst.language.sentence.api.InputLocation
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Without '#create-missing-types', a property set by 'T() { p := e }' must be a property of 'T' (AT-Q5).
 */
class test_AsmTransformSemanticAnalyser_createdObjectProperty {

    private companion object {
        val GRAMMAR = """
            namespace example
            grammar Shapes {
                skip leaf WS = "\s+" ;
                unit = item* ;
                item = circle | square ;
                circle = 'circle' ID ;
                square = 'square' ID ;
                leaf ID = "[a-z]+" ;
            }
        """.trimIndent()

        fun transformErrors(transform: String): List<Pair<InputLocation?, String>> {
            val proc = Agl.processorFromStringSimple(GrammarString(GRAMMAR)).processor!!
            val res = AsmTransformDomainDefault.fromString(
                contextFromGrammarAndTypesDomain(proc.grammarDomain!!, proc.baseTypesDomain),
                AsmTransformString(transform.trimIndent())
            )
            return res.allIssues.errors.map { Pair(it.location, it.message) }
        }
    }

    @Test
    fun unknown_property__error() {
        val errors = transformErrors(
            """
            #override-default-transform
            namespace example
            asm-transform Shapes {
                circle: Circle() { nosuchprop := child[1] }
            }
            """
        )
        assertEquals(
            listOf("Type 'example.Shapes.Circle' has no property 'nosuchprop' (set in the transformation rule for 'circle'), use '#create-missing-types' to create it"),
            errors.map { it.second }
        )
        assertEquals(InputLocation(92, 24, 4, 22, null), errors[0].first)
    }

    @Test
    fun known_property__no_error() {
        val errors = transformErrors(
            """
            #override-default-transform
            namespace example
            asm-transform Shapes {
                circle: Circle() { id := child[1] }
            }
            """
        )
        assertEquals(emptyList(), errors)
    }

    @Test
    fun unknown_property_with_create_missing_types__no_error() {
        val errors = transformErrors(
            """
            #override-default-transform
            #create-missing-types
            namespace example
            asm-transform Shapes {
                circle: Circle() { nosuchprop := child[1] }
            }
            """
        )
        assertEquals(emptyList(), errors)
    }
}
