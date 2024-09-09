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

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.CrossReferenceString
import net.akehurst.language.agl.GrammarString
import net.akehurst.language.agl.asm.AsmPathSimple
import net.akehurst.language.agl.language.typemodel.typeModel
import net.akehurst.language.api.language.base.QualifiedName
import net.akehurst.language.api.processor.CompletionItem
import net.akehurst.language.api.processor.CompletionItemKind
import net.akehurst.language.typemodel.api.TypeModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class test_CompletionProviderDefault {

    private companion object {

        data class TestData(
            val grammarStr: String,
            val crossReferencesStr: String = "",
            val additionalTypeModel: TypeModel? = null,
            val context: ContextAsmDefault? = ContextAsmDefault(),
            val sentence: String,
            val position: Int,
            val expected: List<CompletionItem>
        )

        fun test(data: TestData) {
            val res = Agl.processorFromStringDefault(grammarDefinitionStr = GrammarString(data.grammarStr), crossReferenceModelStr = CrossReferenceString(data.crossReferencesStr))
            assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
            val proc = res.processor!!
            data.additionalTypeModel?.let {
                proc.typeModel.addAllNamespaceAndResolveImports(it.allNamespace)
            }
            proc.typeModel
            proc.crossReferenceModel
            assertTrue(proc.issues.errors.isEmpty(), proc.issues.toString())

            val actual = proc.expectedItemsAt(data.sentence, data.position, 0, Agl.options {
                completionProvider {
                    context(data.context)
                }
            })
            assertTrue(actual.issues.errors.isEmpty(), actual.issues.toString())
            assertEquals(data.expected.size, actual.items.size)
            assertEquals(data.expected.toSet(), actual.items.toSet())
        }

    }

    @Test
    fun atStart_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' ;
            }
        """
        val sentence = ""
        val expected = listOf(
            CompletionItem(CompletionItemKind.LITERAL, "a", "'a'")
        )
        test(
            TestData(
                grammarStr = grammarStr,
                sentence = sentence,
                position = sentence.length,
                expected = expected
            )
        )
    }

    @Test
    fun atStart_pattern() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = "[a-z]" ;
            }
        """
        val sentence = ""
        val expected = listOf(
            CompletionItem(CompletionItemKind.PATTERN, "<\"[a-z]\">", "[a-z]")
        )
        test(
            TestData(
                grammarStr = grammarStr,
                sentence = sentence,
                position = sentence.length,
                expected = expected
            )
        )
    }

    @Test
    fun atStart_leaf_pattern() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = PAT;
                leaf PAT = "[a-z]" ;
            }
        """
        val sentence = ""
        val expected = listOf(
            CompletionItem(CompletionItemKind.PATTERN, "<PAT>", "[a-z]")
        )
        test(
            TestData(
                grammarStr = grammarStr,
                sentence = sentence,
                position = sentence.length,
                expected = expected
            )
        )
    }

    @Test
    fun FailedParseReasonLookahead__varDef_no_scope() {
        val grammarStr = """
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                S = varDef ;
                varDef = 'var' NAME ':' typeRef ;
                typeRef = NAME ;
                leaf NAME = "[a-zA-Z]+" ;
            }
        """

        val sentence = "var x:"

        val expected = listOf(
            CompletionItem(CompletionItemKind.PATTERN, "<NAME>", "[a-zA-Z]+")
        )

        test(
            TestData(
                grammarStr = grammarStr,
                sentence = sentence,
                position = sentence.length,
                expected = expected
            )
        )
    }

    @Test
    fun FailedParseReasonLookahead__external_from_scope() {
        val grammarStr = """
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                S = varDef ;
                varDef = 'var' NAME ':' typeRef ;
                typeRef = REF ;
                leaf REF = NAME ;
                leaf NAME = "[a-zA-Z]+" ;
            }
        """
        val externalNsName = "external"
        val crossReferencesStr = """
            namespace test.Test {
                import $externalNsName
                identify VarDef by name
                references {
                    in TypeRef {
                        property ref refers-to TypeDef
                    }
                }
            }
        """.trimIndent()


        val sentence = "var x:"
        val additionalTypeModel = typeModel("External", true) {
            namespace(externalNsName) {
                dataType("TypeDef")
            }
        }

        val context = ContextAsmDefault()
        context.rootScope.addToScope("int", QualifiedName("$externalNsName.TypeDef"), AsmPathSimple.EXTERNAL)


        val expected = listOf(
            CompletionItem(CompletionItemKind.REFERRED, "int", "TypeDef")
        )

        test(
            TestData(
                grammarStr = grammarStr,
                crossReferencesStr = crossReferencesStr,
                additionalTypeModel = additionalTypeModel,
                context = context,
                sentence = sentence,
                position = sentence.length,
                expected = expected
            )
        )
    }

}