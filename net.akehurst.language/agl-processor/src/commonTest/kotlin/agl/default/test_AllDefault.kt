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

package net.akehurst.language.agl.syntaxAnalyser

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.GrammarString
import net.akehurst.language.agl.grammarTypeModel.GrammarTypeModelTest
import net.akehurst.language.agl.grammarTypeModel.grammarTypeModel
import net.akehurst.language.agl.language.asmTransform.asmTransform
import net.akehurst.language.agl.language.asmTransform.test.AsmTransformModelTest
import net.akehurst.language.agl.processor.LanguageProcessorAbstract
import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetTest.matches
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.agl.semanticAnalyser.ContextSimple
import net.akehurst.language.api.asm.Asm
import net.akehurst.language.api.asm.asmSimple
import net.akehurst.language.api.language.asmTransform.AsmTransformModel
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.test.FixMethodOrder
import net.akehurst.language.test.MethodSorters
import net.akehurst.language.typemodel.api.TypeModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class test_AllDefault {

    private companion object {

        class TestDataForGeneratedParser(
            val grammarStr: String,
            val expectedRrs: RuntimeRuleSet,
            val expectedTm: TypeModel,
            val expectedTr: AsmTransformModel
        ) {
            val sentenceData = mutableListOf<TestDataForSentenceParse>()

            fun define(sentence: String, sppt: String, expected: () -> Asm) {
                sentenceData.add(TestDataForSentenceParse(sentence, sppt, expected()))
            }
        }

        class TestDataForSentenceParse(
            val sentence: String,
            val expectedSppt: String,
            val expected: Asm
        )

        fun processor(grammarStr: String) = Agl.processorFromStringDefault(
            grammarDefinitionStr = GrammarString(grammarStr)
        )

        fun testSentence(proc: LanguageProcessor<Asm, ContextSimple>, sd: TestDataForSentenceParse) {
            println("'${sd.sentence}'")
            val spptRes = proc.parse(sd.sentence)
            assertTrue(spptRes.issues.errors.isEmpty(), spptRes.issues.toString())
            val sppt = spptRes.sppt!!
            val expSppt = proc.spptParser.parse(sd.expectedSppt)
            assertEquals(expSppt.toStringAll, sppt.toStringAll, "Different SPPT")

            val asmRes = proc.process(sd.sentence)
            assertTrue(asmRes.issues.errors.isEmpty(), asmRes.issues.toString())
            val actual = asmRes.asm!!
            assertEquals(sd.expected.asString(indentIncrement = "  "), actual.asString(indentIncrement = "  "), "Different ASM")

        }

        fun test1(testData: TestDataForGeneratedParser, sentenceIndex: Int? = null) {
            val procRes = processor(testData.grammarStr)
            assertTrue(procRes.issues.isEmpty(), procRes.issues.toString())
            val proc = procRes.processor!!

            val rrs = (proc as LanguageProcessorAbstract).ruleSet as RuntimeRuleSet
            assertEquals(testData.expectedRrs.toString(), rrs.toString(), "Different RRS by string")
            assertTrue(testData.expectedRrs.matches(rrs), "Different RRS by match")

            assertEquals(testData.expectedTm.asString(), proc.typeModel.asString(), "Different TypeModel by string")
            GrammarTypeModelTest.tmAssertEquals(testData.expectedTm, proc.typeModel)

            assertEquals(testData.expectedTr.asString(), proc.asmTransformModel.asString(), "Different AsmTransform by string")
            AsmTransformModelTest.trAssertEquals(testData.expectedTr, proc.asmTransformModel)

            if (null == sentenceIndex) {
                for (sd in testData.sentenceData) {
                    testSentence(proc, sd)
                }
            } else {
                val sd = testData.sentenceData[sentenceIndex]
                testSentence(proc, sd)
            }
        }

        fun test(
            grammarStr: String,
            expectedRrs: RuntimeRuleSet,
            expectedTm: TypeModel,
            expectedTr: AsmTransformModel,
            sentenceIndex: Int? = null,
            sentenceData: TestDataForGeneratedParser.() -> Unit
        ) {
            val td = TestDataForGeneratedParser(
                grammarStr = grammarStr,
                expectedRrs = expectedRrs,
                expectedTm = expectedTm,
                expectedTr = expectedTr
            )
            td.sentenceData()
            test1(td, sentenceIndex)
        }
    }

    // --- Empty ---
    @Test // S =  ;
    fun _00_empty() {
        val grammarStr = """
                namespace test
                grammar Test {
                    S =  ;
                }
            """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { empty() }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "", sppt = "S { <EMPTY> }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }

    }

    // --- Literal ---
    @Test // S = 'a' ;
    fun _11_terminal_nonleaf_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test // S = a ; leaf a = 'a' ;
    fun _12_terminal_leaf_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a ;
                leaf a = 'a' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { ref("a") }
            literal("a", "a")

        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("a")
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            leafStringRule("a")
            createObject("S", "S") {
                assignment("a", "child[0]")
            }
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { a:'a' }") {
                asmSimple {
                    element("S") {
                        propertyString("a", "a")
                    }
                }
            }
        }
    }

    // --- Pattern ---
    @Test //  S = "[a-z]" ;
    fun _13_terminal_nonLeaf_pattern() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = "[a-z]" ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { pattern("[a-z]") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { \"[a-z]\":'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test // S = v ; leaf v = "[a-z]" ;
    fun _14_terminal_leaf_pattern() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = v ;
                leaf v = "[a-z]" ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { ref("v") }
            pattern("v", "[a-z]")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("v")
            dataType("S", "S") {
                propertyPrimitiveType("v", "String", false, 0)
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            leafStringRule("v")
            createObject("S", "S") {
                assignment("v", "child[0]")
            }
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { v:'a' }") {
                asmSimple {
                    element("S") {
                        propertyString("v", "a")
                    }
                }
            }
        }
    }

    // --- Concatenation ---
    @Test // S = A B C ;
    fun _21_concat_nonTerm_x3_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = A B C ;
                A = 'a' ;
                B = 'b' ;
                C = 'c' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { ref("A"); ref("B"); ref("C"); }
            concatenation("A") { literal("a") }
            concatenation("B") { literal("b") }
            concatenation("C") { literal("c") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyPrimitiveType("b", "String", false, 1)
                propertyPrimitiveType("c", "String", false, 2)
            }
            dataType("A", "A") {}
            dataType("B", "B") {}
            dataType("C", "C") {}
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S") {
                assignment("a", "child[0]")
                assignment("b", "child[1]")
                assignment("c", "child[2]")
            }
            createObject("A", "A")
            createObject("B", "B")
            createObject("C", "C")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "abc", sppt = "S { A {'a'} B{'b'} C{'c'} }") {
                asmSimple {
                    element("S") {
                        propertyElementExplicitType("a", "A") {}
                        propertyElementExplicitType("b", "B") {}
                        propertyElementExplicitType("c", "C") {}
                    }
                }
            }
        }
    }

    @Test // S = A ',' B ',' C ;
    fun _22_concat_nonTerm_x3_literal_with_separator() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = A ',' B ',' C ;
                A = 'a' ;
                B = 'b' ;
                C = 'c' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { ref("A"); literal(","); ref("B"); literal(","); ref("C"); }
            concatenation("A") { literal("a") }
            concatenation("B") { literal("b") }
            concatenation("C") { literal("c") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyPrimitiveType("b", "String", false, 2)
                propertyPrimitiveType("c", "String", false, 4)
            }
            dataType("A", "A") {}
            dataType("B", "B") {}
            dataType("C", "C") {}
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S") {
                assignment("a", "child[0]")
                assignment("b", "child[2]")
                assignment("c", "child[4]")
            }
            createObject("A", "A")
            createObject("B", "B")
            createObject("C", "C")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a,b,c", sppt = "S { A { 'a' } ',' B { 'b' } ',' C { 'c' } }") {
                asmSimple {
                    element("S") {
                        propertyElementExplicitType("a", "A") {}
                        propertyElementExplicitType("b", "B") {}
                        propertyElementExplicitType("c", "C") {}
                    }
                }
            }
        }
    }

    // --- Choice ---
    @Test // S = 'a' | 'b' | 'c' ;
    fun _31_choice_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' | 'b' | 'c' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("a")
                literal("b")
                literal("c")
            }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("S")
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            child0StringRule("S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    string("a")
                }
            }
        }
    }

    @Test // S = A | B | C ; A = a x; B = b x; C = c x;
    fun _32_choice_nonTerm() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = A | B | C ;
                A = a x;
                B = b x;
                C = c x;
                leaf a = 'a';
                leaf b = 'b';
                leaf c = 'c';
                leaf x = 'x';
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("A")
                ref("B")
                ref("C")
            }
            concatenation("A") { ref("a"); ref("x") }
            concatenation("B") { ref("b"); ref("x") }
            concatenation("C") { ref("c"); ref("x") }
            literal("a", "a")
            literal("b", "b")
            literal("c", "c")
            literal("x", "x")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
                subtypes("A", "B", "C")
            }
            dataType("A", "A") {
                supertypes("S")
                propertyPrimitiveType("a", "String", false, 0)
                propertyPrimitiveType("x", "String", false, 1)
            }
            dataType("B", "B") {
                supertypes("S")
                propertyPrimitiveType("b", "String", false, 0)
                propertyPrimitiveType("x", "String", false, 1)
            }
            dataType("C", "C") {
                supertypes("S")
                propertyPrimitiveType("c", "String", false, 0)
                propertyPrimitiveType("x", "String", false, 1)
            }
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("c")
            stringTypeFor("x")
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            subtypeRule("S", "S")
            createObject("A", "A") {
                assignment("a", "child[0]")
                assignment("x", "child[1]")
            }
            createObject("B", "B") {
                assignment("b", "child[0]")
                assignment("x", "child[1]")
            }
            createObject("C", "C") {
                assignment("c", "child[0]")
                assignment("x", "child[1]")
            }
            leafStringRule("a")
            leafStringRule("b")
            leafStringRule("c")
            leafStringRule("x")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "ax", sppt = "S{A{a:'a' x:'x'}}") {
                asmSimple {
                    element("A") {
                        propertyString("a", "a")
                        propertyString("x", "x")
                    }
                }
            }
            define(sentence = "bx", sppt = "S{B{b:'b' x:'x'}}") {
                asmSimple {
                    element("B") {
                        propertyString("b", "b")
                        propertyString("x", "x")
                    }
                }
            }
            define(sentence = "cx", sppt = "S{C{c:'c' x:'x'}}") {
                asmSimple {
                    element("C") {
                        propertyString("c", "c")
                        propertyString("x", "x")
                    }
                }
            }
        }
    }

    @Test // S = L | M ; L = 'a' | 'b' | 'c' ; M = 'x' | 'y' ;
    fun _33_choice_of_choice_all_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = L | M ;
                L = 'a' | 'b' | 'c' ;
                M = 'x' | 'y' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("L")
                ref("M")
            }
            choice("L", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("a")
                literal("b")
                literal("c")
            }
            choice("M", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("x")
                literal("y")
            }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("S")
            stringTypeFor("L")
            stringTypeFor("M")
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            child0StringRule("S")
            child0StringRule("L")
            child0StringRule("M")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S{L{'a'}}") {
                asmSimple {
                    string("a")
                }
            }
            define(sentence = "b", sppt = "S{L{'b'}}") {
                asmSimple {
                    string("b")
                }
            }
            define(sentence = "c", sppt = "S{L{'c'}}") {
                asmSimple {
                    string("c")
                }
            }
            define(sentence = "x", sppt = "S{M{'x'}}") {
                asmSimple {
                    string("x")
                }
            }
            define(sentence = "y", sppt = "S{M{'y'}}") {
                asmSimple {
                    string("y")
                }
            }
        }
    }

    @Test // S = L | M ; L = a | b | c ;  M = x | y ;
    fun _34_choice_of_choice_all_leaf() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = L | M ;
                L = a | b | c ;
                M = x | y ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf x = 'x' ;
                leaf y = 'y' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("L")
                ref("M")
            }
            choice("L", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("a")
                ref("b")
                ref("c")
            }
            choice("M", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("x")
                ref("y")
            }
            literal("a", "a")
            literal("b", "b")
            literal("c", "c")
            literal("x", "x")
            literal("y", "y")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("S")
            stringTypeFor("L")
            stringTypeFor("M")
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("c")
            stringTypeFor("x")
            stringTypeFor("y")
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            child0StringRule("S")
            child0StringRule("L")
            child0StringRule("M")
            leafStringRule("a")
            leafStringRule("b")
            leafStringRule("c")
            leafStringRule("x")
            leafStringRule("y")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { L{ a:'a' } }") {
                asmSimple {
                    string("a")
                }
            }
            define(sentence = "b", sppt = "S { L{ b:'b' } }") {
                asmSimple {
                    string("b")
                }
            }
            define(sentence = "c", sppt = "S { L{ c:'c' } }") {
                asmSimple {
                    string("c")
                }
            }
            define(sentence = "x", sppt = "S { M{ x:'x' } }") {
                asmSimple {
                    string("x")
                }
            }
            define(sentence = "y", sppt = "S { M{ y:'y' } }") {
                asmSimple {
                    string("y")
                }
            }
        }
    }

    @Test // S = A | B | C ; A = a x ; B = C | D ; C = c x; D = d x ;
    fun _35_choice_of_choice_all_concats() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = A | B | C ;
                A = a x ;
                B = C | D ;
                C = c x ;
                D = d x ;
                leaf a = 'a' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
                leaf x = 'x' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("A")
                ref("B")
                ref("C")
            }
            concatenation("A") { ref("a"); ref("x") }
            choice("B", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("C")
                ref("D")
            }
            concatenation("C") { ref("c"); ref("x") }
            concatenation("D") { ref("d"); ref("x") }
            literal("a", "a")
            literal("c", "c")
            literal("d", "d")
            literal("x", "x")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
                subtypes("A", "B", "C")
            }
            dataType("A", "A") {
                supertypes("S")
                propertyPrimitiveType("a", "String", false, 0)
                propertyPrimitiveType("x", "String", false, 1)
            }
            dataType("B", "B") {
                supertypes("S")
                subtypes("C", "D")
            }
            dataType("C", "C") {
                supertypes("S", "B")
                propertyPrimitiveType("c", "String", false, 0)
                propertyPrimitiveType("x", "String", false, 1)
            }
            dataType("D", "D") {
                supertypes("B")
                propertyPrimitiveType("d", "String", false, 0)
                propertyPrimitiveType("x", "String", false, 1)
            }
            stringTypeFor("a")
            stringTypeFor("c")
            stringTypeFor("d")
            stringTypeFor("x")
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            subtypeRule("S", "S")
            createObject("A", "A") {
                assignment("a", "child[0]")
                assignment("x", "child[1]")
            }
            subtypeRule("B", "B")
            createObject("C", "C") {
                assignment("c", "child[0]")
                assignment("x", "child[1]")
            }
            createObject("D", "D") {
                assignment("d", "child[0]")
                assignment("x", "child[1]")
            }
            leafStringRule("a")
            leafStringRule("c")
            leafStringRule("d")
            leafStringRule("x")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "ax", sppt = "S { A { a:'a' x:'x' } }") {
                asmSimple {
                    element("A") {
                        propertyString("a", "a")
                        propertyString("x", "x")
                    }
                }
            }
            define(sentence = "cx", sppt = "S { C { c:'c' x:'x' } }") {
                asmSimple {
                    element("C") {
                        propertyString("c", "c")
                        propertyString("x", "x")
                    }
                }
            }
            define(sentence = "dx", sppt = "S { B { D { d:'d' x:'x' } } }") {
                asmSimple {
                    element("D") {
                        propertyString("d", "d")
                        propertyString("x", "x")
                    }
                }
            }
        }
    }

    @Test // S = A | B | C ; A = a x ; B = c | D ; C = c ; D = d ;
    fun _36_choice_of_choice_mixed_literal_and_concats() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = A | B | C ;
                A = a x ;
                B = c | D ;
                C = c ;
                D = d ;
                leaf a = 'a';
                leaf c = 'c';
                leaf d = 'd';
                leaf x = 'x';
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) { ref("A"); ref("B"); ref("C") }
            concatenation("A") { ref("a"); ref("x") }
            choice("B", RuntimeRuleChoiceKind.LONGEST_PRIORITY) { ref("c"); ref("D") }
            concatenation("C") { ref("c") }
            concatenation("D") { ref("d") }
            literal("a", "a")
            literal("c", "c")
            literal("d", "d")
            literal("x", "x")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            val B = unnamedSuperTypeTypeOf("B", listOf(StringType, "D"))
            unnamedSuperTypeTypeOf("S", listOf("A", B, "C"))
            dataType("A", "A") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyPrimitiveType("x", "String", false, 1)
            }
            dataType("C", "C") {
                propertyPrimitiveType("c", "String", false, 0)
            }
            dataType("D", "D") {
                propertyPrimitiveType("d", "String", false, 0)
            }
            stringTypeFor("a")
            stringTypeFor("c")
            stringTypeFor("d")
            stringTypeFor("x")
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("A", "A") {
                assignment("a", "child[0]")
                assignment("x", "child[1]")
            }
            createObject("C", "C") {
                assignment("c", "child[0]")
            }
            createObject("D", "D") {
                assignment("d", "child[0]")
            }
            leafStringRule("a")
            leafStringRule("c")
            leafStringRule("d")
            leafStringRule("x")
            unnamedSubtypeRule("B", listOf("c", "D"))
            unnamedSubtypeRule("S", listOf("A", "B", "C"))
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "ax", sppt = "S { A { a:'a' x:'x' } }") {
                asmSimple {
                    element("A") {
                        propertyString("a", "a")
                        propertyString("x", "x")
                    }
                }
            }
            define("c", sppt = "S { C { c:'c' } }") {
                asmSimple {
                    element("C") {
                        propertyString("c", "c")
                    }
                }
            }
            define("d", sppt = "S { B { D { d:'d' } } }") {
                asmSimple {
                    element("D") {
                        propertyString("d", "d")
                    }
                }
            }
        }
    }

    // --- Optional ---
    @Test // S = 'a'? ;
    fun _41_optional_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a'? ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            optional("S", "'a'")
            literal("a")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "", sppt = "S { <EMPTY> }") {
                asmSimple {
                    element("S") { }
                }
            }
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") { }
                }
            }
        }
    }

    @Test //S = a? ;
    fun _42_optional_literal_leaf() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a? ;
                leaf a = 'a';
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            optional("S", "a")
            literal("a", "a")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", true, 0)
            }
            stringTypeFor("a")
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S") {
                assignment("a", "child[0]")
            }
            leafStringRule("a")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "", sppt = "S { <EMPTY> }") {
                asmSimple {
                    element("S") {
                        propertyNothing("a")
                    }
                }
            }
            define(sentence = "a", sppt = "S { a:'a' }") {
                asmSimple {
                    element("S") {
                        propertyString("a", "a")
                    }
                }
            }
        }
    }

    @Test // S = a 'b'? c ;
    fun _43_concat_optional_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a 'b'? c ;
                leaf a = 'a' ;
                leaf c = 'c' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { ref("a"); ref("§S§opt1"); ref("c") }
            optional("§S§opt1", "'b'")
            literal("a", "a")
            literal("b")
            literal("c", "c")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyPrimitiveType("c", "String", false, 2)
            }
            stringTypeFor("a")
            stringTypeFor("c")
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S") {
                assignment("a", "child[0]")
                assignment("c", "child[2]")
            }
            leafStringRule("a")
            leafStringRule("c")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "ac", sppt = "S { a:'a' §S§opt1 {<EMPTY>} c:'c' }") {
                asmSimple {
                    element("S") {
                        propertyString("a", "a")
                        propertyString("c", "c")
                    }
                }
            }
            define(sentence = "abc", sppt = "S { a:'a' §S§opt1 {'b'} c:'c' }") {
                asmSimple {
                    element("S") {
                        propertyString("a", "a")
                        propertyString("c", "c")
                    }
                }
            }
        }
    }

    @Test // S = a b? c ;
    fun _44_concat_optional_literal_leaf() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a b? c ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { ref("a"); ref("§S§opt1"); ref("c") }
            optional("§S§opt1", "b")
            literal("a", "a")
            literal("b", "b")
            literal("c", "c")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyPrimitiveType("b", "String", true, 1)
                propertyPrimitiveType("c", "String", false, 2)
            }
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("c")
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S") {
                assignment("a", "child[0]")
                assignment("b", "child[1]")
                assignment("c", "child[2]")
            }
            leafStringRule("a")
            leafStringRule("b")
            leafStringRule("c")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "ac", sppt = "S { a:'a' §S§opt1 {<EMPTY>} c:'c' }") {
                asmSimple {
                    element("S") {
                        propertyString("a", "a")
                        propertyString("b", null)
                        propertyString("c", "c")
                    }
                }
            }
            define(sentence = "abc", sppt = "S { a:'a' §S§opt1 {b:'b'} c:'c' }") {
                asmSimple {
                    element("S") {
                        propertyString("a", "a")
                        propertyString("b", "b")
                        propertyString("c", "c")
                    }
                }
            }
        }
    }

    @Test // S = A?; A = a; leaf a = 'a';
    fun _45_optional_nonTerm() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = A? ;
                A = a ;
                leaf a = 'a';
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            optional("S", "A")
            concatenation("A") { ref("a") }
            literal("a", "a")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
                propertyDataTypeOf("a", "A", true, 0)
            }
            dataType("A", "A") {
                propertyPrimitiveType("a", "String", false, 0)
            }
            stringTypeFor("a")
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S") {
                assignment("a", "child[0]")
            }
            createObject("A", "A") {
                assignment("a", "child[0]")
            }
            leafStringRule("a")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "", sppt = "S { <EMPTY> }") {
                asmSimple {
                    element("S") {
                        propertyNothing("a")
                    }
                }
            }
            define(sentence = "a", sppt = "S { A { a:'a' } }") {
                asmSimple {
                    element("S") {
                        propertyElementExplicitType("a", "A") {
                            propertyString("a", "a")
                        }
                    }
                }
            }
        }
    }

    @Test // S = b A?; A = a; leaf a = 'a';
    fun _46_concat_optional_nonTerm() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = b A? ;
                A = a ;
                leaf a = 'a';
                leaf b = 'b';
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { ref("b"); ref("§S§opt1") }
            optional("§S§opt1", "A")
            concatenation("A") { ref("a") }
            literal("a", "a")
            literal("b", "b")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
                propertyPrimitiveType("b", "String", false, 0)
                propertyDataTypeOf("a", "A", true, 1)
            }
            dataType("A", "A") {
                propertyPrimitiveType("a", "String", false, 0)
            }
            stringTypeFor("a")
            stringTypeFor("b")
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S") {
                assignment("b", "child[0]")
                assignment("a", "child[1]")
            }
            createObject("A", "A") {
                assignment("a", "child[0]")
            }
            leafStringRule("a")
            leafStringRule("b")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "b", sppt = "S { b:'b' §S§opt1 { <EMPTY> } }") {
                asmSimple {
                    element("S") {
                        propertyString("b", "b")
                        propertyNothing("a")
                    }
                }
            }
            define(sentence = "ba", sppt = "S { b:'b' §S§opt1 { A { a:'a' } } }") {
                asmSimple {
                    element("S") {
                        propertyString("b", "b")
                        propertyElementExplicitType("a", "A") {
                            propertyString("a", "a")
                        }
                    }
                }
            }
        }
    }

    @Test // S = oA; oA=A?; A = a; leaf a = 'a';
    fun _47_nonTerm_optional_nonTerm() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = oA ;
                oA = A? ;
                A = a ;
                leaf a = 'a';
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { ref("oA") }
            optional("oA", "A")
            concatenation("A") { ref("a") }
            literal("a", "a")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
                propertyDataTypeOf("oA", "OA", false, 0)
            }
            dataType("oA", "OA") {
                propertyDataTypeOf("a", "A", true, 0)
            }
            dataType("A", "A") {
                propertyPrimitiveType("a", "String", false, 0)
            }
            stringTypeFor("a")
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S") {
                assignment("oA", "child[0]")
            }
            createObject("oA", "OA") {
                assignment("a", "child[0]")
            }
            createObject("A", "A") {
                assignment("a", "child[0]")
            }
            leafStringRule("a")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "", sppt = "S { oA { <EMPTY> } }") {
                asmSimple {
                    element("S") {
                        propertyElementExplicitType("oA", "OA") {
                            propertyNothing("a")
                        }
                    }
                }
            }
            define(sentence = "a", sppt = "S { oA { A { a:'a' } } }") {
                asmSimple {
                    element("S") {
                        propertyElementExplicitType("oA", "OA") {
                            propertyElementExplicitType("a", "A") {
                                propertyString("a", "a")
                            }
                        }
                    }
                }
            }
        }
    }

    // --- ListSimple ---
    @Test //  S = 'a'* ;
    fun _501_list_literal_nonLeaf() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a'* ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            multi("S", 0, -1, "'a'")
            literal("a")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "", sppt = "S { <EMPTY_LIST> }") {
                asmSimple {
                    element("S") {}
                }
            }
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {}
                }
            }
            define(sentence = "aa", sppt = "S { 'a' 'a' }") {
                asmSimple {
                    element("S") {}
                }
            }
            define(sentence = "aaa", sppt = "S { 'a' 'a' 'a'}") {
                asmSimple {
                    element("S") {}
                }
            }
        }
    }

    @Test // S = a 'b'* c ;
    fun _502_concat_list_literal_nonLeaf() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a 'b'* c ;
                leaf a = 'a' ;
                leaf c = 'c' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { ref("a"); ref("§S§multi1"); ref("c") }
            multi("§S§multi1", 0, -1, "'b'")
            literal("a", "a")
            literal("b")
            literal("c", "c")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyPrimitiveType("c", "String", false, 2)
            }
            stringTypeFor("a")
            stringTypeFor("c")
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S") {
                assignment("a", "child[0]")
                assignment("c", "child[2]")
            }
            leafStringRule("a")
            leafStringRule("c")
        }

        // list of 'b' is ignored for ASM because they are all non-leaf literals
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "ac", sppt = "S { a:'a' §S§multi1 { <EMPTY_LIST> } c:'c' }") {
                asmSimple {
                    element("S") {
                        propertyString("a", "a")
                        propertyString("c", "c")
                    }
                }
            }
            define(sentence = "abc", sppt = "S { a:'a' §S§multi1 { 'b' } c:'c' }") {
                asmSimple {
                    element("S") {
                        propertyString("a", "a")
                        propertyString("c", "c")
                    }
                }
            }
            define(sentence = "abbc", sppt = "S { a:'a' §S§multi1 { 'b' 'b' } c:'c' }") {
                asmSimple {
                    element("S") {
                        propertyString("a", "a")
                        propertyString("c", "c")
                    }
                }
            }
            define(sentence = "abbbc", sppt = "S { a:'a' §S§multi1 { 'b' 'b' 'b' } c:'c' }") {
                asmSimple {
                    element("S") {
                        propertyString("a", "a")
                        propertyString("c", "c")
                    }
                }
            }
        }
    }

    @Test //  S = a* ;
    fun _503_list_literal_leaf() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a* ;
                leaf a = 'a' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            multi("S", 0, -1, "a")
            literal("a", "a")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
                propertyListType("a", false, 0) { primitiveRef("String") }
            }
            stringTypeFor("a")
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S") {
                assignment("a", "children")
            }
            leafStringRule("a")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "", sppt = "S { <EMPTY_LIST> }") {
                asmSimple {
                    element("S") {
                        propertyListOfString("a", listOf())
                    }
                }
            }
            define(sentence = "a", sppt = "S { a:'a' }") {
                asmSimple {
                    element("S") {
                        propertyListOfString("a", listOf("a"))
                    }
                }
            }
            define(sentence = "aa", sppt = "S { a:'a' a:'a' }") {
                asmSimple {
                    element("S") {
                        propertyListOfString("a", listOf("a", "a"))
                    }
                }
            }
            define(sentence = "aaa", sppt = "S { a:'a' a:'a' a:'a' }") {
                asmSimple {
                    element("S") {
                        propertyListOfString("a", listOf("a", "a", "a"))
                    }
                }
            }
        }
    }

    @Test // S = a b* c ;
    fun _504_concat_list_literal_leaf() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a b* c ;
                leaf a = 'a';
                leaf b = 'b';
                leaf c = 'c';
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { ref("a"); ref("§S§multi1"); ref("c") }
            multi("§S§multi1", 0, -1, "b")
            literal("a", "a")
            literal("b", "b")
            literal("c", "c")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyListType("b", false, 1) { primitiveRef("String") }
                propertyPrimitiveType("c", "String", false, 2)
            }
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("c")
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S") {
                assignment("a", "child[0]")
                assignment("b", "child[1]")
                assignment("c", "child[2]")
            }
            leafStringRule("a")
            leafStringRule("b")
            leafStringRule("c")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "ac", sppt = "S { a:'a' §S§multi1 { <EMPTY_LIST> } c:'c' }") {
                asmSimple {
                    element("S") {
                        propertyString("a", "a")
                        propertyListOfString("b", emptyList())
                        propertyString("c", "c")
                    }
                }
            }
            define(sentence = "abc", sppt = "S { a:'a' §S§multi1 { b:'b' } c:'c' }") {
                asmSimple {
                    element("S") {
                        propertyString("a", "a")
                        propertyListOfString("b", listOf("b"))
                        propertyString("c", "c")
                    }
                }
            }
            define(sentence = "abbc", sppt = "S { a:'a' §S§multi1 { b:'b' b:'b' } c:'c' }") {
                asmSimple {
                    element("S") {
                        propertyString("a", "a")
                        propertyListOfString("b", listOf("b", "b"))
                        propertyString("c", "c")
                    }
                }
            }
            define(sentence = "abbbc", sppt = "S { a:'a' §S§multi1 { b:'b' b:'b' b:'b' } c:'c' }") {
                asmSimple {
                    element("S") {
                        propertyString("a", "a")
                        propertyListOfString("b", listOf("b", "b", "b"))
                        propertyString("c", "c")
                    }
                }
            }
        }
    }

    @Test // S = A*; A = a; leaf a = 'a';
    fun _505_list_nonTerm() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = A* ;
                A = a ;
                leaf a = 'a';
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            multi("S", 0, -1, "A")
            concatenation("A") { ref("a") }
            literal("a", "a")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
                propertyListTypeOf("a", "A", false, 0)
            }
            dataType("A", "A") {
                propertyPrimitiveType("a", "String", false, 0)
            }
            stringTypeFor("a")
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S") {
                assignment("a", "children")
            }
            createObject("A", "A") {
                assignment("a", "child[0]")
            }
            leafStringRule("a")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "", sppt = "S { <EMPTY_LIST> }") {
                asmSimple {
                    element("S") {
                        propertyListOfElement("a") {}
                    }
                }
            }
            define(sentence = "a", sppt = "S { A{ a:'a' } }") {
                asmSimple {
                    element("S") {
                        propertyListOfElement("a") {
                            element("A") {
                                propertyString("a", "a")
                            }
                        }
                    }
                }
            }
            define(sentence = "aa", sppt = "S { A{ a:'a' } A{ a:'a' } }") {
                asmSimple {
                    element("S") {
                        propertyListOfElement("a") {
                            element("A") {
                                propertyString("a", "a")
                            }
                            element("A") {
                                propertyString("a", "a")
                            }
                        }
                    }
                }
            }
        }
    }

    @Test // S = b A* c; A = a; leaf a = 'a';
    fun _506_concat_list_nonTerm() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = b A* c;
                A = a ;
                leaf a = 'a';
                leaf b = 'b';
                leaf c = 'c';
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { ref("b"); ref("§S§multi1"); ref("c") }
            multi("§S§multi1", 0, -1, "A")
            concatenation("A") { ref("a") }
            literal("a", "a")
            literal("b", "b")
            literal("c", "c")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
                propertyPrimitiveType("b", "String", false, 0)
                propertyListTypeOf("a", "A", false, 1)
                propertyPrimitiveType("c", "String", false, 2)
            }
            dataType("A", "A") {
                propertyPrimitiveType("a", "String", false, 0)
            }
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("c")
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S") {
                assignment("b", "child[0]")
                assignment("a", "child[1]")
                assignment("c", "child[2]")
            }
            createObject("A", "A") {
                assignment("a", "child[0]")
            }
            leafStringRule("a")
            leafStringRule("b")
            leafStringRule("c")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define("bc", sppt = "S { b:'b' §S§multi1 { <EMPTY_LIST> } c:'c' }") {
                asmSimple {
                    element("S") {
                        propertyString("b", "b")
                        propertyListOfElement("a") {}
                        propertyString("c", "c")
                    }
                }
            }
            define("bac", sppt = "S { b:'b' §S§multi1 { A { a:'a' } } c:'c' }") {
                asmSimple {
                    element("S") {
                        propertyString("b", "b")
                        propertyListOfElement("a") {
                            element("A") {
                                propertyString("a", "a")
                            }
                        }
                        propertyString("c", "c")
                    }
                }
            }
            define("baac", sppt = "S { b:'b' §S§multi1 { A { a:'a' } A { a:'a' } } c:'c' }") {
                asmSimple {
                    element("S") {
                        propertyString("b", "b")
                        propertyListOfElement("a") {
                            element("A") {
                                propertyString("a", "a")
                            }
                            element("A") {
                                propertyString("a", "a")
                            }
                        }
                        propertyString("c", "c")
                    }
                }
            }
        }
    }

    @Test //S = as ; as = a* ;
    fun _507_nonTerm_list_literal_leaf() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = as ;
                as = a* ;
                leaf a = 'a' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { ref("as") }
            multi("as", 0, -1, "a")
            literal("a", "a")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
                propertyListType("as", false, 0) { primitiveRef("String") }
            }
            //listTypeFor("as", StringType)
            dataType("as", "As") {
                propertyListType("a", false, 0) { primitiveRef("String") }
            }
            stringTypeFor("a")
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S") {
                assignment("as", "child[0]")
            }
            createObject("as", "As") {
                assignment("a", "children")
            }
            leafStringRule("a")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "", sppt = "S { as { <EMPTY_LIST> } }") {
                asmSimple {
                    element("S") {
                        propertyListOfString("as", listOf())
                    }
                }
            }
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                        propertyListOfString("as", listOf("a"))
                    }
                }
            }
            define(sentence = "aa", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                        propertyListOfString("as", listOf("a", "a"))
                    }
                }
            }
            define(sentence = "aaa", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                        propertyListOfString("as", listOf("a", "a", "a"))
                    }
                }
            }
        }
    }

    @Test //S = as ; as = ao* ; ao= a? ;
    fun _508_nonTerm_list_literal_leaf() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = as ;
                as = ao* ;
                ao = a? ;
                leaf a = 'a' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test //S = abs; abs = ab*; ab = A | B;
    fun _509_list_of_supertype() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = abs ;
                abs = AB* ;
                AB = A | B ;
                A = a ;
                B = b ;
                leaf a = 'a' ;
                leaf b = 'b' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test
    fun _510_supertype_of_list() {
        val grammarStr = """
            namespace test
            grammar Test {
                skip WS = "\s+" ;
                S = E ;
                E = V | A ;
                A = E{2+} ;
                V = NAME ;
                leaf NAME = "[a-zA-Z]+" ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    // --- ListSeparated ---
    @Test // S = as ; as = ['a' / ',']* ;
    fun _61_rhs_nonTerm_sepList_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = as ;
                as = ['a' / ',']* ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test // S = a bs c ; bs = ['b' / ',']* ;
    fun _6_concat_nonTerm_sepList_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a bs c;
                bs = ['b' / ',']* ;
                leaf a = 'a' ;
                leaf c = 'c' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test // S = as ; as = [a / ',']* ;
    fun _6_rhs_nonTerm_sepList_literal_leaf() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = as ;
                as = [a / ',']* ;
                leaf a = 'a' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test //S = abs; abs = [ab / ',']*; ab = A | B;
    fun _6_sepList_of_supertype() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = abs ;
                abs = [AB / ',']* ;
                AB = A | B ;
                A = a ;
                B = b ;
                leaf a = 'a' ;
                leaf b = 'b' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test //S = A B? ; A = ['a'/'.']+ ; B = 'b' ;
    fun _6_sepList_followed_by_empty() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = A B? ;
                A = ['a'/'.']+ ;
                B = 'b' ;
            }
        """
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test //S = As B? ; As = [A/'.']+ ; leaf A = 'a' ; leaf B = 'b' ;
    fun _6_sepList_followed_by_empty_leafs() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = As B? ;
                As = [A/'.']+ ;
                leaf A = 'a' ;
                leaf B = 'b' ;
            }
        """
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    // --- Group ---
    @Test // S = a ('b' 'c' 'd') e ;
    fun _7_concat_group_concat_literal_nonLeaf() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a ('b' 'c' 'd') e ;
                leaf a = 'a' ;
                leaf e = 'e' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test // S = a (b c d) e ;
    fun _7_concat_group_concat_literal_leaf() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a (b c d) e ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
                leaf e = 'e' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test // S = a (b c d) (b a c) e ;
    fun _7_concat_group_concat_leaf_literal_2() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a (b c d) (b a c) e ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
                leaf e = 'e' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test // S = a (b) e ;
    fun _7_concat_group_1_leaf_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a (b) e ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf e = 'e' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test // S = a (b (c) d) e ;
    fun _7_group_concat_group_group_leaf_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a (b (c) d) e ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
                leaf e = 'e' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test // S = a (b | c | d) e ;
    fun _7_concat_group_choice_leaf_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a (b | c | d) e ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
                leaf e = 'e' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test // S = a (b c | d) e ;
    fun _7_concat_group_choice_concat_leaf_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a (b c | d) e ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
                leaf e = 'e' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test // S = a (b c | d e) f ;
    fun _7_concat_group_choice_concat_leaf_literal_2() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a (b c | d e) f ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
                leaf e = 'e' ;
                leaf f = 'f' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test // S = a ( ('x' | 'y') b c | d e) f ;
    fun _7_concat_group_choice_concat_leaf_literal_3() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a ( ('x'|'y') b c | d e) f ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
                leaf e = 'e' ;
                leaf f = 'f' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test // S = a (b c | d e)? f ;
    fun _7_concat_group_choice_concat_leaf_literal_4() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a (b c | d e)? f ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
                leaf e = 'e' ;
                leaf f = 'f' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test //  S = a (b? c) e ;
    fun _7_concat_group_concat_optional() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a (b? c) e ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
                leaf e = 'e' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test // S = a ( (b | c) (d?) e ) f ;
    fun _7_concat_group_choice_group_concat_optional() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a ( (b | c) (d?) e ) f ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
                leaf e = 'e' ;
                leaf f = 'f' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test // S = (BC | d+) ;
    fun _7_rhs_group_choice_concat_nonTerm_list() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = (BC | d+) ;
                BC = b c ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test // S = a (BC | d+) e ;
    fun _7_concat_group_choice_concat_nonTerm_list() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a (BC | d+) e ;
                BC = b c ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
                leaf e = 'e' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test // S = a (BC | d+)? e ;
    fun _7_concat_group_choice_concat_nonTerm_list_2() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a (BC | d+)? e ;
                BC = b c ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
                leaf e = 'e' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test // S = a b | c d e ;
    fun _7_1a_rhs_choice_concat_nonTerm() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a b | c d e ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
                leaf e = 'e' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test // S = (a b) | (c d e) ;
    fun _7_1b_rhs_choice_concat_nonTerm() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = (a b) | (c d e) ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
                leaf e = 'e' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test // S = (a b | c d e) ;
    fun _7_1c_rhs_choice_concat_nonTerm() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = (a b | c d e) ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
                leaf e = 'e' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test // S = ((a b) | (c d e)) ;
    fun _7_2_rhs_choice_concat_nonTerm() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = (a b | c d e) ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
                leaf e = 'e' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test // S = x (a b | c d e) y ;
    fun _7_3_rhs_choice_concat_nonTerm() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = x (a b | c d e) y ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
                leaf e = 'e' ;
                leaf x = 'x' ;
                leaf y = 'y' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test // S = CH; CH = a b | c d e ;
    fun _7_4_rhs_choice_concat_nonTerm() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = CH ;
                CH = a b | c d e ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
                leaf e = 'e' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test // S = CH; CH = (a b | c d e) ;
    fun _7_5_rhs_choice_concat_nonTerm() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = CH ;
                CH = (a b | c d e) ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
                leaf e = 'e' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test // S = x CH y ; CH = a b | c d e
    fun _7_6_rhs_choice_concat_nonTerm() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = x CH y ;
                CH = a b | c d e ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
                leaf e = 'e' ;
                leaf x = 'x' ;
                leaf y = 'y' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test // S = x C y ; C = (a b | c d e)
    fun _7_7_rhs_choice_concat_nonTerm() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = x CH y ;
                CH = (a b | c d e) ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
                leaf e = 'e' ;
                leaf x = 'x' ;
                leaf y = 'y' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }


    @Test
    fun _7_where_root_is_UnnamedSuperType() {
        TODO()
    }

    @Test
    fun _7_group_where_tuple_property_is_UnnamedSuperType() {
        TODO()
    }

    @Test
    fun _7_UnnamedSuperType_of_UnnamedSuperType() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a X? e ;
                X = R | D ;
                R = ( ( b c ) | (c d) ) ;
                D = ( 'x' | 'y' );
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
                leaf e = 'e' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    // Embedded
    @Test
    fun _8_e() {
        //  S = <e> | S a
        // S = d | B S
        // B = b I::S b | c I::S c
        val grammarStr = """
            namespace test
            grammar I {
                S = a | S a ;
                leaf a = 'a' ;
            }
            grammar O {
                S = d | B S ;
                B = b I::S b | c I::S c ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test
    fun _8_e_2() {
        val grammarStr = """
            namespace test
            grammar I {
                S = A | SA ;
                SA = S A ;
                A = a ;
                leaf a = 'a' ;
            }
            grammar O {
               S = B | SBC ;
               SBC = S BC ;
               BC = B | C ;
               B = 'b' I::S 'b' ;
               C = 'c' I::S 'c' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test
    fun _8_e_3() {
        // S = 'a' | S 'a' ;
        // S = B | S B;
        // B = 'b' Inner::S 'b' | 'c' Inner::S 'c' ;
        val grammarStr = """
            namespace test
            grammar I {
                S = 'a' | S 'a' ;
            }
            grammar O {
               S = B | S B;
               B = 'b' I::S 'b' | 'c' I::S 'c' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test
    fun _8_e_4() {
        val grammarStr = """
            namespace test
            grammar I {
                S = A | SA ;
                SA = S A ;
                A = a ;
                leaf a = 'a' ;
            }
            grammar O {
               S = B | S BC ;
               BC = B | C ;
               B = 'b' I::S 'b' ;
               C = 'c' I::S 'c' ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    // --- Misc ---
    @Test
    fun _9_nesting() {
        val grammarStr = """
            namespace test
            grammar Test {
                skip WS = "\s+" ;
                S = type ;
                type = NAME typeArgs? ;
                typeArgs = '<' typeArgList '>' ;
                typeArgList = [type / ',']+ ;
                leaf NAME = "[a-zA-Z][a-zA-Z0-9]*" ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test
    fun _9_patternChoice() {
        val grammarStr = """
            namespace test
            grammar Test {
                skip WS = "\s+" ;
                S = ID ':' type ;
                type = 'int' | 'bool' | "[A-Z][a-z]*" ;
                leaf ID = "[a-z]" ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test
    fun _9_concatenation() {
        val grammarStr = """
            namespace test
            grammar Test {
                skip WS = "\s+" ;
                S = ID NUMBER NAME ;
                leaf ID = "[a-z]" ;
                leaf NUMBER = "[0-9]+" ;
                leaf NAME = "[a-zA-Z][a-zA-Z0-9]+" ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test
    fun _9_choice() {
        val grammarStr = """
            namespace test
            grammar Test {
                skip WS = "\s+" ;
                S = ID item ;
                item = A | B | C ;
                A = NUMBER ;
                B = NAME ;
                C = NAME NUMBER ;
                leaf ID = "[a-z]" ;
                leaf NUMBER = "[0-9]+" ;
                leaf NAME = "[a-zA-Z][a-zA-Z0-9]+" ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test
    fun _9_optional_full() {
        val grammarStr = """
            namespace test
            grammar Test {
                skip WS = "\s+" ;
                S = ID NUMBER? NAME ;
                leaf ID = "[a-z]" ;
                leaf NUMBER = "[0-9]+" ;
                leaf NAME = "[a-zA-Z][a-zA-Z0-9]+" ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test
    fun _9_optional_empty() {
        val grammarStr = """
            namespace test
            grammar Test {
                skip WS = "\s+" ;
                S = ID NUMBER? NAME ;
                leaf ID = "[a-z]" ;
                leaf NUMBER = "[0-9]+" ;
                leaf NAME = "[a-zA-Z][a-zA-Z0-9]+" ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test
    fun _9_list_empty() {
        val grammarStr = """
            namespace test
            grammar Test {
                skip WS = "\s+" ;
                S = ID NAME* ;
                leaf ID = "[a-z]" ;
                leaf NUMBER = "[0-9]+" ;
                leaf NAME = "[a-zA-Z][a-zA-Z0-9]+" ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test
    fun _9_list() {
        val grammarStr = """
            namespace test
            grammar Test {
                skip WS = "\s+" ;
                S = ID NAME* ;
                leaf ID = "[a-z]" ;
                leaf NUMBER = "[0-9]+" ;
                leaf NAME = "[a-zA-Z][a-zA-Z0-9]+" ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test
    fun _9_list_of_group() {
        val grammarStr = """
            namespace test
            grammar Test {
                skip WS = "\s+" ;
                S = ID (NAME | NUMBER)* ;
                leaf ID = "[a-z]" ;
                leaf NUMBER = "[0-9]+" ;
                leaf NAME = "[a-zA-Z][a-zA-Z0-9]+" ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test
    fun _9_sepList() {
        val grammarStr = """
            namespace test
            grammar Test {
                skip WS = "\s+" ;
                addressBook = ID contacts;
                contacts = [person / ',']* ;
                person = NAME NAME NUMBER ;
                leaf ID = "[a-zA-Z0-9]+" ;
                leaf NUMBER = "[0-9]+" ;
                leaf NAME = "[a-zA-Z][a-zA-Z0-9]+" ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test
    fun _9_sepList2() {
        val grammarStr = """
            namespace test
            grammar Test {
                skip leaf WHITESPACE = "\s+" ;
                attr_stmt = attr_type attr_lists ;
                attr_type = 'graph' | 'node' | 'edge' ;
                attr_lists = attr_list+ ;
                attr_list = '[' attr_list_content ']' ;
                attr_list_content = [ attr / a_list_sep ]* ;
                attr = ID '=' ID ;
                leaf a_list_sep = (';' | ',')? ;
                leaf ID = "[a-zA-Z_][a-zA-Z_0-9]+" ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test
    fun _9_expressions_infix() {
        val grammarStr = """
            namespace test
            grammar Test {
                skip WS = "\s+" ;
                S = exprList ;
                exprList = expr (';' expr)* ;
                expr = root | mul | add ;
                root = var | literal ;
                mul = expr '*' expr ;
                add = expr '+' expr ;
                var = NAME ;
                literal = NUMBER ;
                leaf NUMBER = "[0-9]+" ;
                leaf NAME = "[a-zA-Z]+" ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test
    fun _9_expressions_sepList() {
        val grammarStr = """
            namespace test
            grammar Test {
                skip WS = "\s+" ;
                S = exprList ;
                exprList = expr (';' expr)* ;
                expr = root | mul | add ;
                root = var | literal ;
                mul = [expr / '*']2+ ;
                add = [expr / '+']2+ ;
                var = NAME ;
                literal = NUMBER ;
                leaf NUMBER = "[0-9]+" ;
                leaf NAME = "[a-zA-Z]+" ;
            }
        """.trimIndent()
        val expectedRrs = runtimeRuleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple {
                    element("S") {
                    }
                }
            }
        }
    }
}