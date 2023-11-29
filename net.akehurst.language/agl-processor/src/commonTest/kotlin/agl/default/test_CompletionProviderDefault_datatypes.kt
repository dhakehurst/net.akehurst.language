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

package net.akehurst.language.agl.default

import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.agl.semanticAnalyser.ContextSimple
import net.akehurst.language.api.processor.CompletionItem
import net.akehurst.language.api.processor.CompletionItemKind
import net.akehurst.language.typemodel.api.TypeModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class test_CompletionProviderDefault_datatypes {

    private companion object {
        val grammarStr = """
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                skip leaf COMMENT_SINGLE_LINE = "//[^\r\n]*" ;
                skip leaf COMMENT_MULTI_LINE = "/\*[^*]*\*+([^*/][^*]*\*+)*/" ;
            
                unit = declaration* ;
                declaration = datatype | primitive | collection ;
                primitive = 'primitive' ID ;
                collection = 'collection' ID typeParameters? ;
                typeParameters = '<' typeParameterList '>' ;
                typeParameterList = [ID / ',']+ ;
                datatype = 'class' ID '{' property* '}' ;
                property = ID ':' typeReference ;
                typeReference = TYPE typeArguments? ;
                typeArguments = '<' typeArgumentList '>' ;
                typeArgumentList = [typeReference / ',']+ ;
            
                leaf ID = "[A-Za-z_][A-Za-z0-9_]*" ;
                leaf TYPE = ID;
            }
        """
        val crossReferencesStr = """
            namespace test.Test {
                identify Primitive by id
                identify Datatype by id
                identify Collection by id
            
                references {
                    in TypeReference {
                        property type refers-to Primitive|Datatype|Collection
                    }
                }
            }
        """.trimIndent()

        data class TestData(
            val additionalTypeModel: TypeModel? = null,
            val context: ContextSimple? = ContextSimple(),
            val sentence: String,
            val expected: List<CompletionItem>
        )

        fun test(data: TestData) {
            val res = Agl.processorFromStringDefault(grammarStr, crossReferencesStr)
            assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
            val proc = res.processor!!
            data.additionalTypeModel?.let {
                proc.typeModel.addAllNamespace(it.allNamespace)
            }
            proc.typeModel
            proc.crossReferenceModel
            assertTrue(proc.issues.errors.isEmpty(), proc.issues.toString())

            val actual = proc.expectedItemsAt(data.sentence, data.sentence.length, 0, Agl.options {
                completionProvider {
                    context(data.context)
                }
            })
            assertTrue(actual.issues.errors.isEmpty(), actual.issues.toString())
            assertEquals(data.expected.size, actual.items.size)
            assertEquals(data.expected.toSet(), actual.items.toSet())
        }

        val tests = listOf(
            TestData(
                sentence = "", expected = listOf(
                    CompletionItem(CompletionItemKind.LITERAL, "primitive", "'primitive'"),
                    CompletionItem(CompletionItemKind.LITERAL, "class", "'class'"),
                    CompletionItem(CompletionItemKind.LITERAL, "collection", "'collection'")
                )
            ),
            TestData(
                sentence = "class", expected = listOf(
                    CompletionItem(CompletionItemKind.PATTERN, "<ID>", "[A-Za-z_][A-Za-z0-9_]*"),
                )
            ),
            TestData(
                sentence = "class ", expected = listOf(
                    CompletionItem(CompletionItemKind.PATTERN, "<ID>", "[A-Za-z_][A-Za-z0-9_]*"),
                )
            ),
            TestData(
                sentence = "class A", expected = listOf(
                    CompletionItem(CompletionItemKind.LITERAL, "{", "'{'"),
                )
            ),
            TestData(
                sentence = "class A ", expected = listOf(
                    CompletionItem(CompletionItemKind.LITERAL, "{", "'{'"),
                )
            ),
            TestData(
                sentence = "class A {", expected = listOf(
                    CompletionItem(CompletionItemKind.LITERAL, "}", "'}'"),
                    CompletionItem(CompletionItemKind.PATTERN, "<ID>", "[A-Za-z_][A-Za-z0-9_]*"),
                )
            ),
            TestData(
                sentence = "class A { prop", expected = listOf(
                    CompletionItem(CompletionItemKind.LITERAL, ":", "':'"),
                )
            ),
            TestData(
                sentence = "class A { prop ", expected = listOf(
                    CompletionItem(CompletionItemKind.LITERAL, ":", "':'"),
                )
            ),
            TestData(
                sentence = "class A { prop :", expected = listOf(
                    CompletionItem(CompletionItemKind.PATTERN, "<TYPE>", "[A-Za-z_][A-Za-z0-9_]*"),
                )
            ),
            TestData(
                sentence = "class A { prop : ", expected = listOf(
                    CompletionItem(CompletionItemKind.PATTERN, "<TYPE>", "[A-Za-z_][A-Za-z0-9_]*"),
                )
            ),
            TestData(
                sentence = "class A { prop : B", expected = listOf(
                    CompletionItem(CompletionItemKind.LITERAL, "<", "'<'"),
                    CompletionItem(CompletionItemKind.PATTERN, "<ID>", "[A-Za-z_][A-Za-z0-9_]*"),
                    CompletionItem(CompletionItemKind.LITERAL, "}", "'}'"),
                )
            ),
            TestData(
                sentence = "class A { prop : B ", expected = listOf(
                    CompletionItem(CompletionItemKind.LITERAL, "<", "'<'"),
                    CompletionItem(CompletionItemKind.PATTERN, "<ID>", "[A-Za-z_][A-Za-z0-9_]*"),
                    CompletionItem(CompletionItemKind.LITERAL, "}", "'}'"),
                )
            ),
            TestData(
                sentence = "class A { prop : B<", expected = listOf(
                    CompletionItem(CompletionItemKind.PATTERN, "<TYPE>", "[A-Za-z_][A-Za-z0-9_]*"),
                )
            ),
            TestData(
                sentence = "class A { prop : B<C", expected = listOf(
                    CompletionItem(CompletionItemKind.LITERAL, "<", "'<'"),
                    CompletionItem(CompletionItemKind.LITERAL, ",", "','"),
                    CompletionItem(CompletionItemKind.LITERAL, ">", "'>'"),
                )
            ),
            TestData(
                sentence = "class A { prop : B<C>", expected = listOf(
                    CompletionItem(CompletionItemKind.PATTERN, "<ID>", "[A-Za-z_][A-Za-z0-9_]*"),
                    CompletionItem(CompletionItemKind.LITERAL, "}", "'}'"),
                )
            ),
            TestData(
                sentence = "class A { prop : B<C,", expected = listOf(
                    CompletionItem(CompletionItemKind.PATTERN, "<TYPE>", "[A-Za-z_][A-Za-z0-9_]*"),
                )
            ),
            TestData(
                sentence = "class A { prop : B<C,D", expected = listOf(
                    CompletionItem(CompletionItemKind.LITERAL, "<", "'<'"),
                    CompletionItem(CompletionItemKind.LITERAL, ",", "','"),
                    CompletionItem(CompletionItemKind.LITERAL, ">", "'>'"),
                )
            ),
            TestData(
                sentence = "class A { prop : B<C,D>", expected = listOf(
                    CompletionItem(CompletionItemKind.PATTERN, "<ID>", "[A-Za-z_][A-Za-z0-9_]*"),
                    CompletionItem(CompletionItemKind.LITERAL, "}", "'}'"),
                )
            ),
            TestData(
                sentence = "class A { prop : B<C,D> ", expected = listOf(
                    CompletionItem(CompletionItemKind.PATTERN, "<ID>", "[A-Za-z_][A-Za-z0-9_]*"),
                    CompletionItem(CompletionItemKind.LITERAL, "}", "'}'"),
                )
            ),
            TestData(
                sentence = "class A { prop : B<C,D> }", expected = listOf(
                    CompletionItem(CompletionItemKind.LITERAL, "primitive", "'primitive'"),
                    CompletionItem(CompletionItemKind.LITERAL, "class", "'class'"),
                    CompletionItem(CompletionItemKind.LITERAL, "collection", "'collection'")
                )
            ),
            TestData( //error in sentence before requested position gives nothing...TODO handle errors and keep parsing!
                sentence = "class A % prop", expected = listOf<CompletionItem>(

                )
            ),
        )
    }

    @Test
    fun all() {
        for (i in tests.indices) {
            println("Test[$i]: ${tests[i].sentence}")
            test(tests[i])
        }
    }

    @Test
    fun one() {
        val td = tests[2]
        println("Test: ${td.sentence}")
        test(td)

    }
}