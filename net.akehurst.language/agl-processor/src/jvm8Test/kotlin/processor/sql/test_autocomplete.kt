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

import net.akehurst.language.agl.simple.ContextAsmSimple
import net.akehurst.language.agl.simple.contextAsmSimple
import net.akehurst.language.api.processor.CompletionItem
import net.akehurst.language.api.processor.CompletionItemKind
import org.junit.Test
import testFixture.data.doTest
import testFixture.data.executeTestSuit
import testFixture.data.testSuit

class test_autocomplete {

    private companion object {
        val grammarStr = test_autocomplete::class.java.getResource("/sql/version_/grammar.agl").readText()
        val referenceStr = test_autocomplete::class.java.getResource("/sql/version_/reference.agl").readText()

        val testSuit = testSuit {
            testData("With grammar only") {
                grammarStr(grammarStr)
                sentencePass("") {
                    context(ContextAsmSimple())
                    expectedCompletionItems(
                        listOf(
                            CompletionItem(CompletionItemKind.SEGMENT, "<statement> ;", "terminatedStatement"),
                            CompletionItem(CompletionItemKind.PATTERN, "<SELECT>", "SELECT"),
                            CompletionItem(CompletionItemKind.PATTERN, "<UPDATE>", "UPDATE"),
                            CompletionItem(CompletionItemKind.PATTERN, "<DELETE>", "DELETE"),
                            CompletionItem(CompletionItemKind.PATTERN, "<INSERT>", "INSERT"),
                            CompletionItem(CompletionItemKind.PATTERN, "<CREATE>", "CREATE"),
                        )
                    )
                }
                sentencePass("SELECT ") {
                    context(ContextAsmSimple())
                    expectedCompletionItems(
                        listOf(
                            CompletionItem(CompletionItemKind.LITERAL, "*", "*"),
                            CompletionItem(CompletionItemKind.PATTERN, "<REF>", "REF"),
                        )
                    )
                }
            }
            testData("With grammar & context") {
                grammarStr(grammarStr)
                sentencePass("SELECT * FROM ") {
                    context(contextAsmSimple {
                        item("table1", "net.akehurst.language.example.SQL.TableDefinition", "")
                    })
                    expectedCompletionItems(
                        listOf(
                            CompletionItem(CompletionItemKind.PATTERN, "<REF>", "REF"),
                        )
                    )
                }
                sentencePass("SELECT ") {
                    context(contextAsmSimple {
                        item("col1", "net.akehurst.language.sql.Column", "")
                    })
                    expectedCompletionItems(
                        listOf(
                            CompletionItem(CompletionItemKind.LITERAL, "*", "*"),
                            CompletionItem(CompletionItemKind.PATTERN, "<REF>", "REF"),
                        )
                    )
                }
            }
            testData("With grammar, reference and context") {
                grammarStr(grammarStr)
                referenceStr(referenceStr)
                sentencePass("SELECT * FROM ") {
                    context(contextAsmSimple {
                        item("table1", "net.akehurst.language.example.SQL.TableDefinition", "")
                        item("col1", "net.akehurst.language.example.SQL.ColumnDefinition", "")
                    })
                    expectedCompletionItems(
                        listOf(
                            CompletionItem(CompletionItemKind.REFERRED, "table1", "TableDefinition"),
                            CompletionItem(CompletionItemKind.PATTERN, "<REF>", "REF"),
                        )
                    )
                }
                sentencePass("SELECT ") {
                    context(contextAsmSimple {
                        item("col1", "net.akehurst.language.sql.Column", "")
                    })
                    expectedCompletionItems(
                        listOf(
                            CompletionItem(CompletionItemKind.REFERRED, "col1", "ColumnDefinition"),
                            CompletionItem(CompletionItemKind.PATTERN, "<REF>", "REF"),
                            CompletionItem(CompletionItemKind.LITERAL, "*", "*"),
                        )
                    )
                }
            }
            testData("With Reference & Autocomplete file") {
                grammarStr(grammarStr)
                referenceStr(referenceStr)
                sentencePass("") {
                    context(ContextAsmSimple())
                    expectedCompletionItems(
                        listOf(
                            CompletionItem(CompletionItemKind.SEGMENT, "SELECT <columns> FROM <table-id>;", "Select statement"),
                            CompletionItem(CompletionItemKind.SEGMENT, "UPDATE <table-id> SET <column-values>;", "Update statement"),
                            CompletionItem(CompletionItemKind.SEGMENT, "DELETE FROM <table-id>;", "Delete statement"),
                            CompletionItem(CompletionItemKind.SEGMENT, "INSERT INTO <table-id> ( <columns> ) VALUES ( <values> );", "Insert statement"),
                            CompletionItem(CompletionItemKind.LITERAL, "SELECT", "SELECT"),
                            CompletionItem(CompletionItemKind.LITERAL, "UPDATE", "UPDATE"),
                            CompletionItem(CompletionItemKind.LITERAL, "DELETE", "DELETE"),
                            CompletionItem(CompletionItemKind.LITERAL, "INSERT", "INSERT"),
                        )
                    )
                }
            }
        }

    }

    @Test
    fun testAll() = executeTestSuit(testSuit)

    @Test
    fun single() {
        val td = testSuit["With grammar, reference and context"]
        doTest(td, 0)
    }

}