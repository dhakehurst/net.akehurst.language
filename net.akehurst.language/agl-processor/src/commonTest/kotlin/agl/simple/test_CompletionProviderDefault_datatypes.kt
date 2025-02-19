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

package net.akehurst.language.agl.simple

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.CompletionItem
import net.akehurst.language.api.processor.CompletionItemKind
import net.akehurst.language.api.processor.CrossReferenceString
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.typemodel.api.TypeModel
import testFixture.data.doTest
import testFixture.data.executeTestSuit
import testFixture.data.testSuit
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
            namespace test.Test
                identify Primitive by id
                identify Datatype by id
                identify Collection by id
            
                references {
                    in TypeReference {
                        property type refers-to Primitive|Datatype|Collection
                    }
                }
        """.trimIndent()

        data class TestData(
            val additionalTypeModel: TypeModel? = null,
            val context: ContextAsmSimple? = ContextAsmSimple(),
            val sentence: String,
            val expected: List<CompletionItem>
        )

        fun test(data: TestData) {
            val res = Agl.processorFromStringSimple(grammarDefinitionStr = GrammarString(grammarStr), referenceStr = CrossReferenceString(crossReferencesStr))

            assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
            val proc = res.processor!!
            data.additionalTypeModel?.let {
                proc.typeModel.addAllNamespaceAndResolveImports(it.namespace)
            }
            proc.typeModel
            proc.crossReferenceModel
            assertTrue(proc.issues.errors.isEmpty(), proc.issues.toString())

            val actual = proc.expectedItemsAt(data.sentence, data.sentence.length, Agl.options {
                completionProvider {
                    context(data.context)
                }
            })
            assertTrue(actual.issues.errors.isEmpty(), actual.issues.toString())
            assertEquals(data.expected.size, actual.items.size,actual.items.joinToString(separator = "\n"))
            assertEquals(data.expected.toSet(), actual.items.toSet())
        }

        val testSuit = testSuit {
            testData("no reference model, depth==1") {
                grammarStr(grammarStr)
                processOptions(Agl.options { completionProvider { depth(1) } })
                sentencePass("") {
                    context(contextAsmSimple {  })
                    expectedCompletionItems(listOf(
                        CompletionItem(CompletionItemKind.SEGMENT, "datatype", "class <ID> { <property> }"),
                        CompletionItem(CompletionItemKind.SEGMENT, "primitive", "primitive <ID>"),
                        CompletionItem(CompletionItemKind.SEGMENT, "collection", "collection <ID> <typeParameters>"),
                        CompletionItem(CompletionItemKind.LITERAL, "'class'", "class"),
                        CompletionItem(CompletionItemKind.LITERAL, "'primitive'", "primitive"),
                        CompletionItem(CompletionItemKind.LITERAL, "'collection'", "collection")
                    ))
                }
                sentencePass("class") {
                    expectedCompletionItems(listOf(
                        CompletionItem(CompletionItemKind.PATTERN, "[A-Za-z_][A-Za-z0-9_]*", "<ID>")
                    ))
                }
                sentencePass("class ") {
                    expectedCompletionItems(listOf(
                        CompletionItem(CompletionItemKind.PATTERN, "[A-Za-z_][A-Za-z0-9_]*", "<ID>")
                    ))
                }
                sentencePass("class A") {
                    expectedCompletionItems(listOf(
                        CompletionItem(CompletionItemKind.LITERAL, "'{'", "{")
                    ))
                }
                sentencePass("class A ") {
                    expectedCompletionItems(listOf(
                        CompletionItem(CompletionItemKind.LITERAL, "'{'", "{")
                    ))
                }
                sentencePass("class A {") {
                    expectedCompletionItems(listOf(
                        CompletionItem(CompletionItemKind.SEGMENT, "property", "<ID> : <typeReference>"),
                        CompletionItem(CompletionItemKind.LITERAL, "'}'", "}"),
                        CompletionItem(CompletionItemKind.PATTERN, "[A-Za-z_][A-Za-z0-9_]*", "<ID>"),
                    ))
                }
                sentencePass("class A { ") {
                    expectedCompletionItems(listOf(
                        CompletionItem(CompletionItemKind.SEGMENT, "property", "<ID> : <typeReference>"),
                        CompletionItem(CompletionItemKind.LITERAL, "'}'", "}"),
                        CompletionItem(CompletionItemKind.PATTERN, "[A-Za-z_][A-Za-z0-9_]*", "<ID>"),
                    ))
                }
                sentencePass("class A { prop") {
                    expectedCompletionItems(listOf(
                        CompletionItem(CompletionItemKind.LITERAL, "':'", ":"),
                    ))
                }
                sentencePass("class A { prop ") {
                    expectedCompletionItems(listOf(
                        CompletionItem(CompletionItemKind.LITERAL, "':'", ":"),
                    ))
                }
                sentencePass("class A { prop :") {
                    expectedCompletionItems(listOf(
                        CompletionItem(CompletionItemKind.SEGMENT, "typeReference", "<TYPE> <typeArguments>"),
                        CompletionItem(CompletionItemKind.PATTERN, "[A-Za-z_][A-Za-z0-9_]*", "<TYPE>"),
                    ))
                }
                sentencePass("class A { prop : ") {
                    expectedCompletionItems(listOf(
                        CompletionItem(CompletionItemKind.SEGMENT, "typeReference", "<TYPE> <typeArguments>"),
                        CompletionItem(CompletionItemKind.PATTERN, "[A-Za-z_][A-Za-z0-9_]*", "<TYPE>"),
                    ))
                }
                sentencePass("class A { prop : B") {
                    expectedCompletionItems(listOf(
                        CompletionItem(CompletionItemKind.SEGMENT, "typeArguments", "< <typeArgumentList> >"),
                        CompletionItem(CompletionItemKind.SEGMENT, "property", "<ID> : <typeReference>"), //FIXME: should not really be valid without WS
                        CompletionItem(CompletionItemKind.LITERAL, "'<'", "<"),
                        CompletionItem(CompletionItemKind.PATTERN, "[A-Za-z_][A-Za-z0-9_]*", "<ID>"),  //FIXME: should not really be valid without WS
                        CompletionItem(CompletionItemKind.LITERAL, "'}'", "}"),
                    ))
                }
                sentencePass("class A { prop : B ") {
                    expectedCompletionItems(listOf(
                        CompletionItem(CompletionItemKind.SEGMENT, "typeArguments", "< <typeArgumentList> >"),
                        CompletionItem(CompletionItemKind.SEGMENT, "property", "<ID> : <typeReference>"), //FIXME: should not really be valid without WS
                        CompletionItem(CompletionItemKind.LITERAL, "'<'", "<"),
                        CompletionItem(CompletionItemKind.PATTERN, "[A-Za-z_][A-Za-z0-9_]*", "<ID>"),  //FIXME: should not really be valid without WS
                        CompletionItem(CompletionItemKind.LITERAL, "'}'", "}"),
                    ))
                }
                sentencePass("class A { prop : B<") {
                    expectedCompletionItems(listOf(
                        CompletionItem(CompletionItemKind.SEGMENT, "typeReference", "<TYPE> <typeArguments>"),
                        CompletionItem(CompletionItemKind.PATTERN, "[A-Za-z_][A-Za-z0-9_]*", "<TYPE>"),
                    ))
                }
                sentencePass("class A { prop : B<C") {
                    expectedCompletionItems(listOf(
                        CompletionItem(CompletionItemKind.SEGMENT, "typeArguments", "< <typeArgumentList> >"),
                        CompletionItem(CompletionItemKind.LITERAL, "'<'", "<"),
                        CompletionItem(CompletionItemKind.LITERAL, "','", ","),
                        CompletionItem(CompletionItemKind.LITERAL, "'>'", ">"),
                    ))
                }
                sentencePass("class A { prop : B<C>") {
                    expectedCompletionItems(listOf(
                        CompletionItem(CompletionItemKind.SEGMENT, "property", "<ID> : <typeReference>"),
                        CompletionItem(CompletionItemKind.LITERAL, "'}'", "}"),
                        CompletionItem(CompletionItemKind.PATTERN, "[A-Za-z_][A-Za-z0-9_]*", "<ID>"),
                    ))
                }
                sentencePass("class A { prop : B<C,") {
                    expectedCompletionItems(listOf(
                        CompletionItem(CompletionItemKind.SEGMENT, "typeReference", "<TYPE> <typeArguments>"),
                        CompletionItem(CompletionItemKind.PATTERN, "[A-Za-z_][A-Za-z0-9_]*", "<TYPE>"),
                    ))
                }
                sentencePass("class A { prop : B<C,D") {
                    expectedCompletionItems(listOf(
                        CompletionItem(CompletionItemKind.SEGMENT, "typeArguments", "< <typeArgumentList> >"),
                        CompletionItem(CompletionItemKind.LITERAL, "'<'", "<"),
                        CompletionItem(CompletionItemKind.LITERAL, "','", ","),
                        CompletionItem(CompletionItemKind.LITERAL, "'>'", ">"),
                    ))
                }
                sentencePass("class A { prop : B<C,D>") {
                    expectedCompletionItems(listOf(
                        CompletionItem(CompletionItemKind.SEGMENT, "property", "<ID> : <typeReference>"),
                        CompletionItem(CompletionItemKind.LITERAL, "'}'", "}"),
                        CompletionItem(CompletionItemKind.PATTERN, "[A-Za-z_][A-Za-z0-9_]*", "<ID>"),
                    ))
                }
                sentencePass("class A { prop : B<C,D> ") {
                    expectedCompletionItems(listOf(
                        CompletionItem(CompletionItemKind.SEGMENT, "property", "<ID> : <typeReference>"),
                        CompletionItem(CompletionItemKind.LITERAL, "'}'", "}"),
                        CompletionItem(CompletionItemKind.PATTERN, "[A-Za-z_][A-Za-z0-9_]*", "<ID>"),
                    ))
                }
                sentencePass("class A { prop : B<C,D> }") {
                    expectedCompletionItems(listOf(
                        CompletionItem(CompletionItemKind.SEGMENT, "datatype", "class <ID> { <property> }"),
                        CompletionItem(CompletionItemKind.SEGMENT, "primitive", "primitive <ID>"),
                        CompletionItem(CompletionItemKind.SEGMENT, "collection", "collection <ID> <typeParameters>"),
                        CompletionItem(CompletionItemKind.LITERAL, "'class'", "class"),
                        CompletionItem(CompletionItemKind.LITERAL, "'primitive'", "primitive"),
                        CompletionItem(CompletionItemKind.LITERAL, "'collection'", "collection")
                    ))
                }
            }
            testData("no reference model, depth==2") {
                grammarStr(grammarStr)
                processOptions(Agl.options { completionProvider { depth(2) } })
                sentencePass("") {
                    context(contextAsmSimple {  })
                    expectedCompletionItems(listOf(
                        CompletionItem(CompletionItemKind.SEGMENT, "datatype", "class <ID> { <ID> : <typeReference> }"),
                        CompletionItem(CompletionItemKind.SEGMENT, "primitive", "primitive <ID>"),
                        CompletionItem(CompletionItemKind.SEGMENT, "collection", "collection <ID> < <typeParameterList> >"),
                        CompletionItem(CompletionItemKind.LITERAL, "'class'", "class"),
                        CompletionItem(CompletionItemKind.LITERAL, "'primitive'", "primitive"),
                        CompletionItem(CompletionItemKind.LITERAL, "'collection'", "collection")
                    ))
                }
                sentencePass("class A {") {
                    expectedCompletionItems(listOf(
                        CompletionItem(CompletionItemKind.SEGMENT, "property", "<ID> : <TYPE> <typeArguments>"),
                        CompletionItem(CompletionItemKind.LITERAL, "'}'", "}"),
                        CompletionItem(CompletionItemKind.PATTERN, "[A-Za-z_][A-Za-z0-9_]*", "<ID>"),
                    ))
                }
                sentencePass("class A { prop :") {
                    expectedCompletionItems(listOf(
                        CompletionItem(CompletionItemKind.SEGMENT, "typeReference", "<TYPE> < <typeArgumentList> >"),
                        CompletionItem(CompletionItemKind.PATTERN, "[A-Za-z_][A-Za-z0-9_]*", "<TYPE>"),
                    ))
                }
                sentencePass("class A { prop : B") {
                    expectedCompletionItems(listOf(
                        CompletionItem(CompletionItemKind.SEGMENT, "typeArguments", "< <typeReference> >"),
                        CompletionItem(CompletionItemKind.SEGMENT, "property", "<ID> : <TYPE> <typeArguments>"), //FIXME: should not really be valid without WS
                        CompletionItem(CompletionItemKind.LITERAL, "'<'", "<"),
                        CompletionItem(CompletionItemKind.PATTERN, "[A-Za-z_][A-Za-z0-9_]*", "<ID>"),  //FIXME: should not really be valid without WS
                        CompletionItem(CompletionItemKind.LITERAL, "'}'", "}"),
                    ))
                }
                sentencePass("class A { prop : B<") {
                    expectedCompletionItems(listOf(
                        CompletionItem(CompletionItemKind.SEGMENT, "typeReference", "<TYPE> < <typeArgumentList> >"),
                        CompletionItem(CompletionItemKind.PATTERN, "[A-Za-z_][A-Za-z0-9_]*", "<TYPE>"),
                    ))
                }
                sentencePass("class A { prop : B<C") {
                    expectedCompletionItems(listOf(
                        CompletionItem(CompletionItemKind.SEGMENT, "typeArguments", "< <typeReference> >"),
                        CompletionItem(CompletionItemKind.LITERAL, "'<'", "<"),
                        CompletionItem(CompletionItemKind.LITERAL, "','", ","),
                        CompletionItem(CompletionItemKind.LITERAL, "'>'", ">"),
                    ))
                }
                sentencePass("class A { prop : B<C>") {
                    expectedCompletionItems(listOf(
                        CompletionItem(CompletionItemKind.SEGMENT, "property", "<ID> : <TYPE> <typeArguments>"),
                        CompletionItem(CompletionItemKind.LITERAL, "'}'", "}"),
                        CompletionItem(CompletionItemKind.PATTERN, "[A-Za-z_][A-Za-z0-9_]*", "<ID>"),
                    ))
                }
                sentencePass("class A { prop : B<C,") {
                    expectedCompletionItems(listOf(
                        CompletionItem(CompletionItemKind.SEGMENT, "typeReference", "<TYPE> < <typeArgumentList> >"),
                        CompletionItem(CompletionItemKind.PATTERN, "[A-Za-z_][A-Za-z0-9_]*", "<TYPE>"),
                    ))
                }
                sentencePass("class A { prop : B<C,D") {
                    expectedCompletionItems(listOf(
                        CompletionItem(CompletionItemKind.SEGMENT, "typeArguments", "< <typeReference> >"),
                        CompletionItem(CompletionItemKind.LITERAL, "'<'", "<"),
                        CompletionItem(CompletionItemKind.LITERAL, "','", ","),
                        CompletionItem(CompletionItemKind.LITERAL, "'>'", ">"),
                    ))
                }
                sentencePass("class A { prop : B<C,D>") {
                    expectedCompletionItems(listOf(
                        CompletionItem(CompletionItemKind.SEGMENT, "property", "<ID> : <TYPE> <typeArguments>"),
                        CompletionItem(CompletionItemKind.LITERAL, "'}'", "}"),
                        CompletionItem(CompletionItemKind.PATTERN, "[A-Za-z_][A-Za-z0-9_]*", "<ID>"),
                    ))
                }
                sentencePass("class A { prop : B<C,D> ") {
                    expectedCompletionItems(listOf(
                        CompletionItem(CompletionItemKind.SEGMENT, "property", "<ID> : <TYPE> <typeArguments>"),
                        CompletionItem(CompletionItemKind.LITERAL, "'}'", "}"),
                        CompletionItem(CompletionItemKind.PATTERN, "[A-Za-z_][A-Za-z0-9_]*", "<ID>"),
                    ))
                }
                sentencePass("class A { prop : B<C,D> }") {
                    expectedCompletionItems(listOf(
                        CompletionItem(CompletionItemKind.SEGMENT, "datatype", "class <ID> { <ID> : <typeReference> }"),
                        CompletionItem(CompletionItemKind.SEGMENT, "primitive", "primitive <ID>"),
                        CompletionItem(CompletionItemKind.SEGMENT, "collection", "collection <ID> < <typeParameterList> >"),
                        CompletionItem(CompletionItemKind.LITERAL, "'class'", "class"),
                        CompletionItem(CompletionItemKind.LITERAL, "'primitive'", "primitive"),
                        CompletionItem(CompletionItemKind.LITERAL, "'collection'", "collection")
                    ))
                }
            }
        }

        val tests = listOf(
            TestData( //error in sentence before requested position gives nothing...TODO handle errors and keep parsing!
                sentence = "class A % prop", expected = listOf<CompletionItem>(

                )
            ),
        )
    }

    @Test
    fun testAll() = executeTestSuit(testSuit)

    @Test
    fun single() = doTest(testSuit["no reference model, depth==2"],2)

}