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

package net.akehurst.language.test.processor.sql

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.GrammarString
import net.akehurst.language.agl.simple.ContextAsmSimple
import net.akehurst.language.api.processor.CompletionItem
import net.akehurst.language.api.processor.CompletionItemKind
import net.akehurst.language.api.processor.ProcessOptions
import net.akehurst.language.asm.api.Asm
import org.junit.Test
import kotlin.test.assertEquals

class test_autocomplete {

    private companion object {
        val grammarStr = test_autocomplete::class.java.getResource("/sql/version_/grammar.agl").readText()
        val processor by lazy {
            Agl.processorFromStringSimple(GrammarString(grammarStr)).processor!!
        }

        fun testPass(sentence: String, position: Int, options: ProcessOptions<Asm, ContextAsmSimple>? = null, expected: List<CompletionItem>) {
            val result = processor.expectedItemsAt(sentence, position, -1, options)
            assertEquals(expected, result.items)
        }
    }

    @Test
    fun empty() {
        val sentence = ""
        val position = sentence.length

        val expected = listOf(
            CompletionItem(CompletionItemKind.SEGMENT, "SELECT <columns> FROM <table-id>;", "Select"),
            CompletionItem(CompletionItemKind.SEGMENT, "UPDATE <table-id> SET <column-values>;", "Update"),
            CompletionItem(CompletionItemKind.SEGMENT, "DELETE FROM <table-id>;", "Delete"),
            CompletionItem(CompletionItemKind.SEGMENT, "INSERT INTO <table-id> ( <columns> ) VALUES ( <values> );", "Insert")
        )
        testPass(sentence, position, null, expected)
    }

    @Test
    fun SELECT_() {
        val sentence = "SELECT "
        val position = sentence.length

        val expected = listOf(
            CompletionItem(CompletionItemKind.SEGMENT, "<column-id>", "column-id"),

        )
        testPass(sentence, position, Agl.options {
            completionProvider {
                context(ContextAsmSimple())
            }
        }, expected)
    }

}