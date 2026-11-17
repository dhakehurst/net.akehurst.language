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

package net.akehurst.language.format.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.semanticAnalyser.contextFromTypesDomain
import net.akehurst.language.api.processor.CompletionItem
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class test_AglFormatCompletionProvider {

    private companion object {
        val aglProc = Agl.registry.agl.format.processor!!

        val types = typesDomain("Test", true) {
            namespace("my.lang") {
                data("Person") {}
                data("Company") {}
            }
            namespace("other.ns") {
                data("Thing") {}
            }
            namespace("other.ns2") {
                data("Gadget") {}
                data("Company") {}
            }
        }

        fun itemString(it: CompletionItem) =
            "${it.kind} ${it.label} '${it.text}' (${it.description})" + it.additionalEdits.joinToString("") { e -> " +edit@${e.position}'${e.text.replace("\n", "\\n")}'" }

        fun test(sentence: String, expected: List<String>, withContext: Boolean = true) {
            val actual = aglProc.expectedItemsAt(sentence, sentence.length, Agl.options {
                completionProvider {
                    sentenceContext(if (withContext) contextFromTypesDomain(types) else null)
                }
            })
            assertTrue(actual.issues.errors.isEmpty(), actual.issues.toString())
            // the std types are left out, to keep the lists short
            val refs = actual.items.filter { it.kind == net.akehurst.language.api.processor.CompletionItemKind.REFERRED && it.description.startsWith("std.").not() }
            assertEquals(expected.joinToString("\n"), refs.joinToString("\n") { itemString(it) }, actual.items.joinToString("\n") { itemString(it) })
        }
    }

    @Test
    fun format_rule_type() {
        val sentence = "namespace my.lang\n  format F {\n    "
        test(sentence, listOf(
            "REFERRED TypeDefinition 'Person' (my.lang.Person)",
            "REFERRED TypeDefinition 'Company' (my.lang.Company)",
            "REFERRED TypeDefinition 'Thing' (other.ns.Thing) +edit@17'\\nimport other.ns'",
            "REFERRED TypeDefinition 'Gadget' (other.ns2.Gadget) +edit@17'\\nimport other.ns2'",
            "REFERRED TypeDefinition 'other.ns2.Company' (other.ns2.Company)",
        ))
    }

    @Test
    fun format_rule_type_imported() {
        val sentence = "namespace my.lang\n  import other.ns2\n  format F {\n    "
        test(sentence, listOf(
            "REFERRED TypeDefinition 'Person' (my.lang.Person)",
            "REFERRED TypeDefinition 'my.lang.Company' (my.lang.Company)",
            "REFERRED TypeDefinition 'Thing' (other.ns.Thing) +edit@36'\\nimport other.ns'",
            "REFERRED TypeDefinition 'Gadget' (other.ns2.Gadget)",
            "REFERRED TypeDefinition 'other.ns2.Company' (other.ns2.Company)",
        ))
    }

    @Test
    fun no_context() {
        val sentence = "namespace my.lang\n  format F {\n    "
        test(sentence, listOf(), false)
    }
}
