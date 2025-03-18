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
import net.akehurst.language.api.processor.CrossReferenceString
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.api.processor.ProcessOptions
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.asm.builder.asmSimple
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.sentence.api.InputLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_SemanticAnalyserSimple {

    private companion object {

        fun test(grammarStr: String, crossReferenceModelStr: String, sentence: String, options: ProcessOptions<Asm, ContextAsmSimple>, expected: ContextAsmSimple) {
            val processor = Agl.processorFromStringSimple(
                grammarDefinitionStr = GrammarString(grammarStr),
                referenceStr = CrossReferenceString(crossReferenceModelStr)
            ).let {
                it.processor!!.crossReferenceModel
                check(it.issues.isEmpty()) { it.issues.toString() }
                it.processor!!
            }
            assertEquals(processor.targetGrammar!!.qualifiedName, processor.crossReferenceModel.declarationsForNamespace.keys.first())
            val result = processor.process(sentence, options)
            assertTrue(result.issues.isEmpty(), result.issues.toString())
            assertNotNull(result.asm)
            assertEquals(expected.asString(), options.semanticAnalysis.context!!.asString())
        }

        fun test_issues(grammarStr: String, crossReferenceModelStr: String, sentence: String, options: ProcessOptions<Asm, ContextAsmSimple
                >, expected: List<LanguageIssue>) {
            val processor = Agl.processorFromStringSimple(
                grammarDefinitionStr = GrammarString(grammarStr),
                referenceStr = CrossReferenceString(crossReferenceModelStr)
            ).processor!!
            val result = processor.process(sentence, options)

            assertEquals(expected, result.issues.all.toList())
        }
    }

    @Test
    fun no_context_provided() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' ;
            }
        """
        val referenceModelStr = """
            namespace test.Test {
            }
        """
        val sentence = "a"
        val context: ContextAsmSimple? = null
        val options = Agl.options<Asm, ContextAsmSimple> {
            semanticAnalysis {
                context(context)
            }
        }

        val expected = listOf(
            LanguageIssue(
                LanguageIssueKind.INFORMATION,
                LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                null,
                "No context provided, references not built, checked or resolved, switch off semanticAnalysis or provide a context."
            )
        )
        test_issues(grammarStr, referenceModelStr, sentence, options, expected)
    }

    @Test
    fun context_provided() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' ;
            }
        """
        val referenceModelStr = """
            namespace test.Test 
        """
        val sentence = "a"
        val context = contextAsmSimple {  }
        val options = Agl.options<Asm, ContextAsmSimple> {
            semanticAnalysis {
                context(context)
            }
        }

        val expected = contextAsmSimple {

        }
        test(grammarStr, referenceModelStr, sentence, options, expected)
    }

    @Test
    fun checkReferences_off() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' ;
            }
        """
        val referenceModelStr = """
            namespace test.Test 
        """
        val sentence = "a"
        val context = ContextAsmSimple()
        val options = Agl.options<Asm, ContextAsmSimple> {
            semanticAnalysis {
                checkReferences(false)
                context(context)
            }
        }

        val expected = listOf(
            LanguageIssue(
                LanguageIssueKind.INFORMATION,
                LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                null,
                "Semantic Analysis option 'checkReferences' is off, references not checked."
            )
        )
        test_issues(grammarStr, referenceModelStr, sentence, options, expected)
    }

    @Test
    fun checkReferences_on() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' ;
            }
        """
        val referenceModelStr = """
            namespace test.Test
        """
        val sentence = "a"
        val context = ContextAsmSimple()
        val options = Agl.options<Asm, ContextAsmSimple> {
            semanticAnalysis {
                checkReferences(true)
                context(context)
            }
        }

        val expected = contextAsmSimple {

        }
        test(grammarStr, referenceModelStr, sentence, options, expected)
    }

    @Test
    fun crossReferenceModel_empty() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' ;
            }
        """
        val referenceModelStr = """
        """
        val sentence = "a"
        val context = ContextAsmSimple()
        val options = Agl.options<Asm, ContextAsmSimple> {
            semanticAnalysis {
                checkReferences(true)
                context(context)
            }
        }

        val expected = listOf(
            LanguageIssue(
                LanguageIssueKind.WARNING,
                LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                null,
                "Empty CrossReferenceModel"
            )
        )
        test_issues(grammarStr, referenceModelStr, sentence, options, expected)
    }

    @Test
    fun crossReferenceModel_notEmpty() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' ;
            }
        """
        val referenceModelStr = """
            namespace test.Test 
        """
        val sentence = "a"
        val context = ContextAsmSimple()
        val options = Agl.options<Asm, ContextAsmSimple> {
            semanticAnalysis {
                checkReferences(true)
                context(context)
            }
        }

        val expected = contextAsmSimple {

        }
        test(grammarStr, referenceModelStr, sentence, options, expected)
    }

    @Test
    fun identifyingExpression_no_identifyingExpression_defined() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = A B? C?;
                leaf A = 'a' ;
                leaf B = 'b' ;
                C = A ;
            }
        """
        val referenceModelStr = """
            namespace test.Test 
                identify C by a
        """
        val sentence = "a"
        val context = ContextAsmSimple()
        val options = Agl.options<Asm, ContextAsmSimple> {
            semanticAnalysis {
                checkReferences(true)
                context(context)
            }
        }

        val expected = contextAsmSimple { }
        test(grammarStr, referenceModelStr, sentence, options, expected)
    }

    @Test
    fun identifyingExpression_by_value_Nothing() {
        // will identify by typename if id by evaluates to Nothing - //TODO: I think we don't want this !
        val grammarStr = """
            namespace test
            grammar Test {
                S = A B? ;
                leaf A = 'a' ;
                leaf B = 'b' ;
            }
        """
        val referenceModelStr = """
            namespace test.Test 
                identify S by b
        """
        val sentence = "a"
        val context = ContextAsmSimple()
        val options = Agl.options<Asm, ContextAsmSimple> {
            semanticAnalysis {
                checkReferences(true)
                context(context)
            }
        }

        val expected = contextAsmSimple {
            item("test.Test.S", "test.Test.S", "/0")
        }
        test(grammarStr, referenceModelStr, sentence, options, expected)
    }

    @Test
    fun identifyingExpression_string() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = A B? ;
                leaf A = 'a' ;
                leaf B = 'b' ;
            }
        """
        val referenceModelStr = """
            namespace test.Test 
                identify S by a
        """
        val sentence = "a"
        val context = ContextAsmSimpleWithAsmPath()
        val options = Agl.options<Asm, ContextAsmSimple> {
            semanticAnalysis {
                checkReferences(true)
                context(context)
            }
        }

        val expected = contextAsmSimple {
            item("a", "test.Test.S", "/0")
        }
        test(grammarStr, referenceModelStr, sentence, options, expected)
    }

    @Test
    fun identifyingExpression_integer() {
        TODO()
    }

    @Test
    fun identifyingExpression_listOfString_no_scope() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = As B? ;
                As = [A/'.']+ ;
                leaf A = 'a' ;
                leaf B = 'b' ;
            }
        """
        val referenceModelStr = """
            namespace test.Test
                identify S by as
        """
        val sentence = "a.a.a"
        val context = ContextAsmSimpleWithAsmPath()
        val options = Agl.options<Asm, ContextAsmSimple> {
            semanticAnalysis {
                checkReferences(true)
                context(context)
            }
        }

        val expected = listOf(
            LanguageIssue(
                LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(0, 1, 1, 5, null),
                "Cannot create a local reference in '//' for item with type 'test.Test.S' because there is no scope defined for the type, although its identifying expression evaluates to a List<String>"
            )
        )
        test_issues(grammarStr, referenceModelStr, sentence, options, expected)
    }

    @Test
    fun identifyingExpression_listOfString_with_scope_but_not_identified_in_scope() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = As B? ;
                As = [A/'.']+ ;
                leaf A = 'a' ;
                leaf B = 'b' ;
            }
        """
        val referenceModelStr = """
            namespace test.Test
                identify S by as
                scope S {
                
                }
        """
        val sentence = "a.a.a"
        val context = ContextAsmSimpleWithAsmPath()
        val options = Agl.options<Asm, ContextAsmSimple> {
            semanticAnalysis {
                checkReferences(true)
                context(context)
            }
        }

        val expected = listOf(
            LanguageIssue(
                LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(0, 1, 1, 5, null),
                "Cannot create a local reference in '//' for item with type 'test.Test.S' because the type has no identifying expression in the scope (which should evaluate to a List<String>)"
            )
        )
        test_issues(grammarStr, referenceModelStr, sentence, options, expected)
    }

    @Test
    fun identifyingExpression_listOfString_with_scope_but_identified_different_in_scope() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = As B? ;
                As = [A/'.']+ ;
                leaf A = 'a' ;
                leaf B = 'b' ;
            }
        """
        val referenceModelStr = """
            namespace test.Test 
                identify S by as
                scope S {
                    identify S by b
                }
        """
        val sentence = "a.a.a"
        val context = ContextAsmSimpleWithAsmPath()
        val options = Agl.options<Asm, ContextAsmSimple> {
            semanticAnalysis {
                checkReferences(true)
                context(context)
            }
        }

        val expected = listOf(
            LanguageIssue(
                LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(0, 1, 1, 5, null),
                "Cannot create a local reference in '//' for item with type 'test.Test.S' because the identifying expression is different in the scope and the parent scope"
            )
        )
        test_issues(grammarStr, referenceModelStr, sentence, options, expected)
    }

    @Test
    fun identifyingExpression_listOfString_with_scope_and_identified_in_scope() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = As B? ;
                As = [A/'.']+ ;
                leaf A = 'a' ;
                leaf B = 'b' ;
            }
        """
        val referenceModelStr = """
            namespace test.Test
                identify S by as
                scope S {
                    identify S by as
                }
        """
        val sentence = "a.a.a"
        val context = ContextAsmSimpleWithAsmPath()
        val options = Agl.options<Asm, ContextAsmSimple> {
            semanticAnalysis {
                checkReferences(true)
                context(context)
            }
        }

        val expected = contextAsmSimple {
            scopedItem("a", "test.Test.S", "/0") {
                scopedItem("a", "test.Test.S", "/0") {
                    scopedItem("a", "test.Test.S", "/0") {
                    }
                }
            }
        }
        test(grammarStr, referenceModelStr, sentence, options, expected)
    }

    @Test
    fun identifyingExpression_structure() {
        TODO()
    }

    @Test
    fun resolveReferences_off() {
        TODO()
    }

    @Test
    fun resolveReferences_on() {
        TODO()
    }


    @Test
    fun listOfItems() {
        val grammarStr = """
            namespace test
            grammar test {
                S = L;
                L = I* ;
                I = 'a' | 'b' ;
            }
        """.trimIndent()
        val scopeModelStr = """
        """.trimIndent()
        val sentence = """
            aabba
        """.trimIndent()
        TODO()
        /*        val expected = scope {
                    element("S") {
                        propertyListOfString("l", listOf("a", "a", "b", "b", "a"))
                    }
                }

                test(grammarStr, scopeModelStr, sentence, expected)*/
    }

    @Test
    fun listOfEmpty() {
        val grammarStr = """
            namespace test
            grammar test {
                S = L;
                L = I* ;
                I =  ;
            }
        """.trimIndent()
        val sentence = ""

        val expected = asmSimple {
            element("S") {
                propertyListOfString("l", listOf())
            }
        }
        TODO()
//        test(grammarStr, sentence, expected)
    }

}