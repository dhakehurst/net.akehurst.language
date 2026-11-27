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
import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.sentence.api.InputLocation
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * An option the asm-transform does not know is a warning (AT-Q6).
 */
class test_AsmTransformSyntaxAnalyser_unknownOption {

    private fun warnings(transform: String) =
        Agl.registry.agl.asmTransform.processor!!.process(transform.trimIndent()).allIssues.all
            .filter { it.kind == LanguageIssueKind.WARNING && it.message.startsWith("Unknown option") }
            .map { Pair(it.location, it.message) }

    @Test
    fun misspelt_file_option__warning() {
        assertEquals(
            listOf(Pair(InputLocation(0, 1, 1, 18, null), "Unknown option '#override-defaults', the asm-transform options are '#override-default-transform' and '#create-missing-types'")),
            warnings(
                """
                #override-defaults
                namespace example
                asm-transform Shapes {
                    circle: Circle() { id := child[1] }
                }
                """
            )
        )
    }

    @Test
    fun unknown_option_in_block__warning() {
        assertEquals(
            listOf("Unknown option '#foo', the asm-transform options are '#override-default-transform' and '#create-missing-types'"),
            warnings(
                """
                namespace example
                asm-transform Shapes {
                    #foo
                    circle: Circle() { id := child[1] }
                }
                """
            ).map { it.second }
        )
    }

    @Test
    fun known_options__no_warning() {
        assertEquals(
            emptyList(),
            warnings(
                """
                #override-default-transform
                namespace example
                  #create-missing-types
                asm-transform Shapes {
                    #override-default-transform
                    circle: Circle() { id := child[1] }
                }
                """
            )
        )
    }
}
