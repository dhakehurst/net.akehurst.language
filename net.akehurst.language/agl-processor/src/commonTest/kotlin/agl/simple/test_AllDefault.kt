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

package net.akehurst.language.agl.simple

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.GrammarString
import net.akehurst.language.agl.grammarTypeModel.GrammarTypeModelTest
import net.akehurst.language.agl.grammarTypeModel.grammarTypeModel
import net.akehurst.language.agl.processor.LanguageProcessorAbstract
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetTest.matches
import net.akehurst.language.agl.runtime.structure.ruleSet
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.asm.simple.asmSimple
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.grammar.processor.ContextFromGrammarRegistry
import net.akehurst.language.parser.api.RuleSet
import net.akehurst.language.test.FixMethodOrder
import net.akehurst.language.test.MethodSorters
import net.akehurst.language.transform.api.TransformModel
import net.akehurst.language.transform.asm.AsmTransformRuleSetBuilder
import net.akehurst.language.transform.asm.asmTransform
import net.akehurst.language.transform.test.AsmTransformModelTest
import net.akehurst.language.typemodel.api.TypeModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class test_AllDefault {

    private companion object {
        /**
         * TrModel.name = qualifiedName.last
         * TrNamespace.name = qualifiedName.front
         * TrRuleSet.name = qualifiedName.last
         */
        fun asmGrammarTransform(qualifiedName: String, typeModel: TypeModel, createTypes: Boolean, init: AsmTransformRuleSetBuilder.() -> Unit): TransformModel {
            val qn = QualifiedName(qualifiedName)
            return asmTransform(qn.last.value, typeModel, createTypes) {
                namespace(qn.front.value) {
                    transform(qn.last.value, init)
                }
            }
        }

        class TestDataForGeneratedParser(
            val grammarStr: String,
            val expectedRrs: RuleSet,
            val expectedTm: TypeModel,
            val expectedTr: TransformModel
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

        fun processor(grammarStr: String) = Agl.processorFromStringSimple(
            grammarDefinitionStr = GrammarString(grammarStr),
            grammarAglOptions = Agl.options {
                semanticAnalysis {
                    context(ContextFromGrammarRegistry(Agl.registry))
//TODO:                    option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, true)
                }
            }
        )

        fun testSentence(proc: LanguageProcessor<Asm, ContextAsmSimple>, sd: TestDataForSentenceParse) {
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
            expectedRrs: RuleSet,
            expectedTm: TypeModel,
            expectedTr: TransformModel,
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { empty() }
        }
        /*
          namespace test.Test
          S -> datatype S
         */
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
            }
        }
        /*
          namespace test
          grammar-transform Test {
             S: S
          }
         */
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "", sppt = "S { <EMPTY> }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("test.Test.S") {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { ref("a") }
            literal("a", "a")

        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            stringTypeFor("a")
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
            }
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
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
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { pattern("[a-z]") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { \"[a-z]\":'a' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { ref("v") }
            pattern("v", "[a-z]")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            stringTypeFor("v")
            dataType("S", "S") {
                propertyPrimitiveType("v", "String", false, 0)
            }
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
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
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { ref("A"); ref("B"); ref("C"); }
            concatenation("A") { literal("a") }
            concatenation("B") { literal("b") }
            concatenation("C") { literal("c") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyPrimitiveType("b", "String", false, 1)
                propertyPrimitiveType("c", "String", false, 2)
            }
            dataType("A", "A") {}
            dataType("B", "B") {}
            dataType("C", "C") {}
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
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
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { ref("A"); literal(","); ref("B"); literal(","); ref("C"); }
            concatenation("A") { literal("a") }
            concatenation("B") { literal("b") }
            concatenation("C") { literal("c") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyPrimitiveType("b", "String", false, 2)
                propertyPrimitiveType("c", "String", false, 4)
            }
            dataType("A", "A") {}
            dataType("B", "B") {}
            dataType("C", "C") {}
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
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
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
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
        val expectedRrs = ruleSet("test.Test") {
            choiceLongest("S") {
                literal("a")
                literal("b")
                literal("c")
            }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            stringTypeFor("S")
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            transRule("S", "String", "child[0]")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
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
        val expectedRrs = ruleSet("test.Test") {
            choiceLongest("S") {
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
        val expectedTm = grammarTypeModel("test.Test", "Test") {
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
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
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
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("A") {
                        propertyString("a", "a")
                        propertyString("x", "x")
                    }
                }
            }
            define(sentence = "bx", sppt = "S{B{b:'b' x:'x'}}") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("B") {
                        propertyString("b", "b")
                        propertyString("x", "x")
                    }
                }
            }
            define(sentence = "cx", sppt = "S{C{c:'c' x:'x'}}") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
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
        val expectedRrs = ruleSet("test.Test") {
            choiceLongest("S") {
                ref("L")
                ref("M")
            }
            choiceLongest("L") {
                literal("a")
                literal("b")
                literal("c")
            }
            choiceLongest("M") {
                literal("x")
                literal("y")
            }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            stringTypeFor("S")
            stringTypeFor("L")
            stringTypeFor("M")
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
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
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    string("a")
                }
            }
            define(sentence = "b", sppt = "S{L{'b'}}") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    string("b")
                }
            }
            define(sentence = "c", sppt = "S{L{'c'}}") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    string("c")
                }
            }
            define(sentence = "x", sppt = "S{M{'x'}}") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    string("x")
                }
            }
            define(sentence = "y", sppt = "S{M{'y'}}") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
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
        val expectedRrs = ruleSet("test.Test") {
            choiceLongest("S") {
                ref("L")
                ref("M")
            }
            choiceLongest("L") {
                ref("a")
                ref("b")
                ref("c")
            }
            choiceLongest("M") {
                ref("x")
                ref("y")
            }
            literal("a", "a")
            literal("b", "b")
            literal("c", "c")
            literal("x", "x")
            literal("y", "y")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            stringTypeFor("S")
            stringTypeFor("L")
            stringTypeFor("M")
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("c")
            stringTypeFor("x")
            stringTypeFor("y")
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
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
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    string("a")
                }
            }
            define(sentence = "b", sppt = "S { L{ b:'b' } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    string("b")
                }
            }
            define(sentence = "c", sppt = "S { L{ c:'c' } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    string("c")
                }
            }
            define(sentence = "x", sppt = "S { M{ x:'x' } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    string("x")
                }
            }
            define(sentence = "y", sppt = "S { M{ y:'y' } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
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
        val expectedRrs = ruleSet("test.Test") {
            choiceLongest("S") {
                ref("A")
                ref("B")
                ref("C")
            }
            concatenation("A") { ref("a"); ref("x") }
            choiceLongest("B") {
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
        val expectedTm = grammarTypeModel("test.Test", "Test") {
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
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
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
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("A") {
                        propertyString("a", "a")
                        propertyString("x", "x")
                    }
                }
            }
            define(sentence = "cx", sppt = "S { C { c:'c' x:'x' } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("C") {
                        propertyString("c", "c")
                        propertyString("x", "x")
                    }
                }
            }
            define(sentence = "dx", sppt = "S { B { D { d:'d' x:'x' } } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
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
        val expectedRrs = ruleSet("test.Test") {
            choiceLongest("S") { ref("A"); ref("B"); ref("C") }
            concatenation("A") { ref("a"); ref("x") }
            choiceLongest("B") { ref("c"); ref("D") }
            concatenation("C") { ref("c") }
            concatenation("D") { ref("d") }
            literal("a", "a")
            literal("c", "c")
            literal("d", "d")
            literal("x", "x")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
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
        expectedTm.resolveImports()
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
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
            unnamedSubtypeRule("B") {
                primitiveRef("String")
                elementRef("D")
            }
            unnamedSubtypeRule("S") {
                elementRef("A")
                unnamedSuperType {
                    primitiveRef("String")
                    elementRef("D")
                }
                elementRef("C")
            }
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "ax", sppt = "S { A { a:'a' x:'x' } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("A") {
                        propertyString("a", "a")
                        propertyString("x", "x")
                    }
                }
            }
            define("c", sppt = "S { C { c:'c' } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("C") {
                        propertyString("c", "c")
                    }
                }
            }
            define("d", sppt = "S { B { D { d:'d' } } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("D") {
                        propertyString("d", "d")
                    }
                }
            }
        }
    }

    @Test // S = BC | d+ ;
    fun _36_choice_concat_term_list() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = BC | d+ ;
                BC = b c ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
            }
        """.trimIndent()
        val expectedRrs = ruleSet("test.Test") {
            choiceLongest("S") {
                concatenation { ref("BC") }
                concatenation { ref("§S§multi1") }
            }
            concatenation("BC") { ref("b"); ref("c") }
            multi("§S§multi1", 1, -1, "d", isPseudo = true)
            literal("b", "b")
            literal("c", "c")
            literal("d", "d")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("BC", "BC") {
                propertyPrimitiveType("b", "String", false, 0)
                propertyPrimitiveType("c", "String", false, 1)
            }
            unnamedSuperTypeType("S") {
                elementRef("BC")
                listType(false) {
                    primitiveRef("String")
                }
            }
            stringTypeFor("b")
            stringTypeFor("c")
            stringTypeFor("d")
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            unnamedSubtypeRule("S") {
                elementRef("BC")
                listType(false) {
                    primitiveRef("String")
                }
            }
            createObject("BC", "BC") {
                assignment("b", "child[0]")
                assignment("c", "child[1]")
            }
            leafStringRule("b")
            leafStringRule("c")
            leafStringRule("d")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "bc", sppt = "S { BC { b:'b' c:'c' } }") {
                asmSimple {
                    element("BC") {
                        propertyString("b", "b")
                        propertyString("c", "c")
                    }
                }
            }
            define(sentence = "d", sppt = "S { §S§multi1 { d : 'd' } }") {
                asmSimple {
                    listOfString("d")
                }
            }
            define(sentence = "dd", sppt = "S { §S§multi1 { d:'d' d:'d' } }") {
                asmSimple {
                    listOfString("d", "d")
                }
            }
            define(sentence = "ddd", sppt = "S { §S§multi1 { d:'d' d:'d' d:'d' } }") {
                asmSimple {
                    listOfString("d", "d", "d")
                }
            }
        }
    }

    @Test // S = BC | D* ;
    fun _37_choice_concat_nonTerm_list() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = (BC | D*) ;
                BC = b c ;
                D = d ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
            }
        """.trimIndent()
        val expectedRrs = ruleSet("test.Test") {
            choiceLongest("S") {
                concatenation { ref("BC") }
                concatenation { ref("§S§multi1") }
            }
            concatenation("BC") { ref("b"); ref("c") }
            multi("§S§multi1", 0, -1, "D", isPseudo = true)
            concatenation("D") { ref("d") }
            literal("b", "b")
            literal("c", "c")
            literal("d", "d")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            unnamedSuperTypeType("S") {
                elementRef("BC")
                listType(false) {
                    elementRef("D")
                }
            }
            dataType("BC", "BC") {
                propertyPrimitiveType("b", "String", false, 0)
                propertyPrimitiveType("c", "String", false, 1)
            }
            dataType("D", "D") {
                propertyPrimitiveType("d", "String", false, 0)
            }
            stringTypeFor("b")
            stringTypeFor("c")
            stringTypeFor("d")
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            unnamedSubtypeRule("S") {
                elementRef("BC")
                listType(false) {
                    elementRef("D")
                }
            }
            createObject("BC", "BC") {
                assignment("b", "child[0]")
                assignment("c", "child[1]")
            }
            createObject("D", "D") {
                assignment("d", "child[0]")
            }
            leafStringRule("b")
            leafStringRule("c")
            leafStringRule("d")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "bc", sppt = "S { BC { b:'b' c:'c' } }") {
                asmSimple {
                    element("BC") {
                        propertyString("b", "b")
                        propertyString("c", "c")
                    }
                }
            }
            define(sentence = "", sppt = "S { §S§multi1 { <EMPTY_LIST> } }") {
                asmSimple {
                    listOfString()
                }
            }
            define(sentence = "d", sppt = "S { §S§multi1 { D { d : 'd' } } }") {
                asmSimple {
                    list {
                        element("D") {
                            propertyString("d","d")
                        }
                    }
                }
            }
            define(sentence = "dd", sppt = "S { §S§multi1 { D { d:'d' } D { d:'d' } } }") {
                asmSimple {
                    list {
                        element("D") {
                            propertyString("d","d")
                        }
                        element("D") {
                            propertyString("d","d")
                        }
                    }
                }
            }
            define(sentence = "ddd", sppt = "S { §S§multi1 { D { d:'d' } D { d:'d' } D { d:'d' } } }") {
                asmSimple {
                    list {
                        element("D") {
                            propertyString("d","d")
                        }
                        element("D") {
                            propertyString("d","d")
                        }
                        element("D") {
                            propertyString("d","d")
                        }
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
        val expectedRrs = ruleSet("test.Test") {
            optional("S", "'a'")
            literal("a")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "", sppt = "S { <EMPTY> }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") { }
                }
            }
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
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
        val expectedRrs = ruleSet("test.Test") {
            optional("S", "a")
            literal("a", "a")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", true, 0)
            }
            stringTypeFor("a")
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
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
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyNothing("a")
                    }
                }
            }
            define(sentence = "a", sppt = "S { a:'a' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { ref("a"); ref("§S§opt1"); ref("c") }
            optional("§S§opt1", "'b'", isPseudo = true)
            literal("a", "a")
            literal("b")
            literal("c", "c")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyPrimitiveType("c", "String", false, 2)
            }
            stringTypeFor("a")
            stringTypeFor("c")
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
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
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyString("a", "a")
                        propertyString("c", "c")
                    }
                }
            }
            define(sentence = "abc", sppt = "S { a:'a' §S§opt1 {'b'} c:'c' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { ref("a"); ref("§S§opt1"); ref("c") }
            optional("§S§opt1", "b", isPseudo = true)
            literal("a", "a")
            literal("b", "b")
            literal("c", "c")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyPrimitiveType("b", "String", true, 1)
                propertyPrimitiveType("c", "String", false, 2)
            }
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("c")
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S") {
                assignment("a", "child[0]")
                assignment("b", "with(child[1]) child[0]")
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
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyString("a", "a")
                        propertyString("b", null)
                        propertyString("c", "c")
                    }
                }
            }
            define(sentence = "abc", sppt = "S { a:'a' §S§opt1 {b:'b'} c:'c' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
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
        val expectedRrs = ruleSet("test.Test") {
            optional("S", "A")
            concatenation("A") { ref("a") }
            literal("a", "a")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
                propertyDataTypeOf("a", "A", true, 0)
            }
            dataType("A", "A") {
                propertyPrimitiveType("a", "String", false, 0)
            }
            stringTypeFor("a")
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
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
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyNothing("a")
                    }
                }
            }
            define(sentence = "a", sppt = "S { A { a:'a' } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { ref("b"); ref("§S§opt1") }
            optional("§S§opt1", "A", isPseudo = true)
            concatenation("A") { ref("a") }
            literal("a", "a")
            literal("b", "b")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
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
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S") {
                assignment("b", "child[0]")
                assignment("a", "with(child[1]) child[0]")
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
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyString("b", "b")
                        propertyNothing("a")
                    }
                }
            }
            define(sentence = "ba", sppt = "S { b:'b' §S§opt1 { A { a:'a' } } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { ref("oA") }
            optional("oA", "A")
            concatenation("A") { ref("a") }
            literal("a", "a")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
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
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
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
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyElementExplicitType("oA", "OA") {
                            propertyNothing("a")
                        }
                    }
                }
            }
            define(sentence = "a", sppt = "S { oA { A { a:'a' } } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
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
        val expectedRrs = ruleSet("test.Test") {
            multi("S", 0, -1, "'a'")
            literal("a")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "", sppt = "S { <EMPTY_LIST> }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {}
                }
            }
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {}
                }
            }
            define(sentence = "aa", sppt = "S { 'a' 'a' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {}
                }
            }
            define(sentence = "aaa", sppt = "S { 'a' 'a' 'a'}") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { ref("a"); ref("§S§multi1"); ref("c") }
            multi("§S§multi1", 0, -1, "'b'", isPseudo = true)
            literal("a", "a")
            literal("b")
            literal("c", "c")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyPrimitiveType("c", "String", false, 2)
            }
            stringTypeFor("a")
            stringTypeFor("c")
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
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
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyString("a", "a")
                        propertyString("c", "c")
                    }
                }
            }
            define(sentence = "abc", sppt = "S { a:'a' §S§multi1 { 'b' } c:'c' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyString("a", "a")
                        propertyString("c", "c")
                    }
                }
            }
            define(sentence = "abbc", sppt = "S { a:'a' §S§multi1 { 'b' 'b' } c:'c' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyString("a", "a")
                        propertyString("c", "c")
                    }
                }
            }
            define(sentence = "abbbc", sppt = "S { a:'a' §S§multi1 { 'b' 'b' 'b' } c:'c' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
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
        val expectedRrs = ruleSet("test.Test") {
            multi("S", 0, -1, "a")
            literal("a", "a")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
                propertyListType("a", false, 0) { primitiveRef("String") }
            }
            stringTypeFor("a")
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
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
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyListOfString("a", listOf())
                    }
                }
            }
            define(sentence = "a", sppt = "S { a:'a' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyListOfString("a", listOf("a"))
                    }
                }
            }
            define(sentence = "aa", sppt = "S { a:'a' a:'a' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyListOfString("a", listOf("a", "a"))
                    }
                }
            }
            define(sentence = "aaa", sppt = "S { a:'a' a:'a' a:'a' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { ref("a"); ref("§S§multi1"); ref("c") }
            multi("§S§multi1", 0, -1, "b", isPseudo = true)
            literal("a", "a")
            literal("b", "b")
            literal("c", "c")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyListType("b", false, 1) { primitiveRef("String") }
                propertyPrimitiveType("c", "String", false, 2)
            }
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("c")
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
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
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyString("a", "a")
                        propertyListOfString("b", emptyList())
                        propertyString("c", "c")
                    }
                }
            }
            define(sentence = "abc", sppt = "S { a:'a' §S§multi1 { b:'b' } c:'c' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyString("a", "a")
                        propertyListOfString("b", listOf("b"))
                        propertyString("c", "c")
                    }
                }
            }
            define(sentence = "abbc", sppt = "S { a:'a' §S§multi1 { b:'b' b:'b' } c:'c' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyString("a", "a")
                        propertyListOfString("b", listOf("b", "b"))
                        propertyString("c", "c")
                    }
                }
            }
            define(sentence = "abbbc", sppt = "S { a:'a' §S§multi1 { b:'b' b:'b' b:'b' } c:'c' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
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
        val expectedRrs = ruleSet("test.Test") {
            multi("S", 0, -1, "A")
            concatenation("A") { ref("a") }
            literal("a", "a")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
                propertyListTypeOf("a", "A", false, 0)
            }
            dataType("A", "A") {
                propertyPrimitiveType("a", "String", false, 0)
            }
            stringTypeFor("a")
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
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
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyListOfElement("a") {}
                    }
                }
            }
            define(sentence = "a", sppt = "S { A{ a:'a' } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
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
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { ref("b"); ref("§S§multi1"); ref("c") }
            multi("§S§multi1", 0, -1, "A", isPseudo = true)
            concatenation("A") { ref("a") }
            literal("a", "a")
            literal("b", "b")
            literal("c", "c")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
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
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
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
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyString("b", "b")
                        propertyListOfElement("a") {}
                        propertyString("c", "c")
                    }
                }
            }
            define("bac", sppt = "S { b:'b' §S§multi1 { A { a:'a' } } c:'c' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
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
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
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

    @Test //S = as ; as = 'a'* ;
    fun _507_nonTerm_list_literal_leaf() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = as ;
                as = 'a'* ;
            }
        """.trimIndent()
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { ref("as") }
            multi("as", 0, -1, "'a'")
            literal("a")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
            }
            dataType("as", "As") {
            }
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S") {
            }
            createObject("as", "As") {
            }
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "", sppt = "S { as { <EMPTY_LIST> } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                    }
                }
            }
            define(sentence = "a", sppt = "S { as { 'a' } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                    }
                }
            }
            define(sentence = "aa", sppt = "S { as { 'a' 'a' } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                    }
                }
            }
            define(sentence = "aaa", sppt = "S { as { 'a' 'a' 'a' } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test //S = as ; as = a* ;
    fun _508_nonTerm_list_literal_leaf() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = as ;
                as = a* ;
                leaf a = 'a' ;
            }
        """.trimIndent()
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { ref("as") }
            multi("as", 0, -1, "a")
            literal("a", "a")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
                propertyListType("as", false, 0) { primitiveRef("String") }
            }
            //listTypeFor("as", StringType)
            dataType("as", "As") {
                propertyListType("a", false, 0) { primitiveRef("String") }
            }
            stringTypeFor("a")
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S") {
                assignment("as", "child[0].a")
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
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyListOfString("as", listOf())
                    }
                }
            }
            define(sentence = "a", sppt = "S { as { a:'a' } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyListOfString("as", listOf("a"))
                    }
                }
            }
            define(sentence = "aa", sppt = "S { as { a:'a' a:'a' } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyListOfString("as", listOf("a", "a"))
                    }
                }
            }
            define(sentence = "aaa", sppt = "S { as { a:'a' a:'a' a:'a' } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyListOfString("as", listOf("a", "a", "a"))
                    }
                }
            }
        }
    }

    @Test //S = as ; as = ao* ; ao= a? ;
    fun _509_nonTerm_list_literal_leaf() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = as ;
                as = ao* ;
                ao = a? ;
                leaf a = 'a' ;
            }
        """.trimIndent()
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { ref("as") }
            multi("as", 0, -1, "ao")
            optional("ao", "a")
            literal("a", "a")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
                propertyListTypeOf("as", "Ao", false, 0)
            }
            dataType("as", "As") {
                propertyListTypeOf("ao", "Ao", false, 0)
            }
            dataType("ao", "Ao") {
                propertyPrimitiveType("a", "String", true, 0)
            }
            stringTypeFor("a")
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S") {
                assignment("as", "child[0].ao")
            }
            createObject("as", "As") {
                assignment("ao", "children")
            }
            createObject("ao", "Ao") {
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
            define(sentence = "", sppt = "S { as { ao { <EMPTY> } } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyListOfElement("as") {
                            element("Ao") {
                                propertyNothing("a")
                            }
                        }
                    }
                }
            }
            define(sentence = "a", sppt = "S { as { ao { a:'a' } } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyListOfString("as", listOf("a"))
                    }
                }
            }
            define(sentence = "aa", sppt = "S { 'a' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyListOfString("as", listOf("a", "a"))
                    }
                }
            }
            define(sentence = "aaa", sppt = "S { 'a' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyListOfString("as", listOf("a", "a", "a"))
                    }
                }
            }
        }
    }

    @Test //S = abs; abs = ab*; ab = A | B;
    fun _510_list_of_supertype() {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { ref("abs") }
            multi("abs", 0, -1, "AB")
            choiceLongest("AB") {
                ref("A")
                ref("B")
            }
            concatenation("A") { ref("a") }
            concatenation("B") { ref("b") }
            literal("a", "a")
            literal("b", "b")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
                propertyListTypeOf("abs", "AB", false, 0)
            }
            dataType("abs", "Abs") {
                propertyListTypeOf("ab", "AB", false, 0)
            }
            dataType("A", "A") {
                propertyPrimitiveType("a", "String", false, 0)
            }
            dataType("B", "B") {
                propertyPrimitiveType("b", "String", false, 0)
            }
            dataType("AB", "AB") {
                subtypes("A", "B")
            }
            stringTypeFor("a")
            stringTypeFor("b")
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S") {
                assignment("abs", "child[0].ab")
            }
            createObject("abs", "Abs") {
                assignment("ab", "children")
            }
            subtypeRule("AB", "AB")
            createObject("A", "A") {
                assignment("a", "child[0]")
            }
            createObject("B", "B") {
                assignment("b", "child[0]")
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
            define(sentence = "", sppt = "S { abs { <EMPTY_LIST> } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyListOfElement("abs") {

                        }
                    }
                }
            }
            define(sentence = "a", sppt = "S { abs { AB{ A { a:'a' } } } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyListOfElement("abs") {
                            element("A") {
                                propertyString("a", "a")
                            }
                        }
                    }
                }
            }
            define(sentence = "b", sppt = "S { abs { AB{ B { b:'b' } } } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyListOfElement("abs") {
                            element("B") {
                                propertyString("b", "b")
                            }
                        }
                    }
                }
            }
            define(sentence = "aa", sppt = "S { abs { AB{ A { a:'a' } } AB{ A { a:'a' } } } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyListOfElement("abs") {
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
            define(sentence = "bb", sppt = "S { abs { AB{ B { b:'b' } } AB{ B { b:'b' } } } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyListOfElement("abs") {
                            element("B") {
                                propertyString("b", "b")
                            }
                            element("B") {
                                propertyString("b", "b")
                            }
                        }
                    }
                }
            }
            define(sentence = "ab", sppt = "S { abs { AB{ A { a:'a' } } AB{ B { b:'b' } } } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyListOfElement("abs") {
                            element("A") {
                                propertyString("a", "a")
                            }
                            element("B") {
                                propertyString("b", "b")
                            }
                        }
                    }
                }
            }
            define(sentence = "ababab", sppt = "S{ abs{ AB{ A{ a:'a' } } AB{ B{ b:'b' } } AB{ A{ a:'a' } } AB{ B{ b:'b' } } AB{ A{ a:'a' } } AB{ B{ b:'b' } } } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyListOfElement("abs") {
                            element("A") {
                                propertyString("a", "a")
                            }
                            element("B") {
                                propertyString("b", "b")
                            }
                            element("A") {
                                propertyString("a", "a")
                            }
                            element("B") {
                                propertyString("b", "b")
                            }
                            element("A") {
                                propertyString("a", "a")
                            }
                            element("B") {
                                propertyString("b", "b")
                            }
                        }
                    }
                }
            }
        }
    }

    @Test // S = E; E = V | A ; A = E{2+} ; V = NAME ;
    fun _511_supertype_of_list() {
        val grammarStr = """
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                S = E ;
                E = V | A ;
                A = E{2+} ;
                V = N ;
                leaf N = "[a-zA-Z]+" ;
            }
        """.trimIndent()
        val expectedRrs = ruleSet("test.Test") {
            pattern("WS", "\\s+", true)
            concatenation("S") { ref("E") }
            choiceLongest("E") {
                ref("V")
                ref("A")
            }
            multi("A", 2, -1, "E")
            concatenation("V") { ref("N") }
            pattern("N", "[a-zA-Z]+")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
                propertyDataTypeOf("e", "E", false, 0)
            }
            dataType("E", "E") {
                subtypes("V", "A")
            }
            dataType("A", "A") {
                supertypes("E")
                propertyListTypeOf("e", "E", false, 0)
            }
            dataType("V", "V") {
                supertypes("E")
                propertyPrimitiveType("n", "String", false, 0)
            }
            stringTypeFor("N")
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S") {
                assignment("e", "child[0]")
            }
            subtypeRule("E", "E")
            createObject("A", "A") {
                assignment("e", "children")
            }
            createObject("V", "V") {
                assignment("n", "child[0]")
            }
            leafStringRule("N")
        }
        test(grammarStr = grammarStr, expectedRrs = expectedRrs, expectedTm = expectedTm, expectedTr = expectedTr) {
            define(sentence = "v", sppt = "S{ E{ V{ N:'v' } } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyElementExplicitType("e", "V") {
                            propertyString("n", "v")
                        }
                    }
                }
            }
            define(sentence = "v w", sppt = "S{ E{ A{ E{V{N:'v' WS:' '}} E{V{N:'w'}} } } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyElementExplicitType("e", "A") {
                            propertyListOfElement("e") {
                                element("V") {
                                    propertyString("n", "v")
                                }
                                element("V") {
                                    propertyString("n", "w")
                                }
                            }
                        }
                    }
                }
            }
            define(sentence = "v w x y z", sppt = "S{ E{ A{ E{V{N:'v' WS:' '}} E{V{N:'w' WS:' '}} E{V{N:'x' WS:' '}} E{V{N:'y' WS:' '}} E{V{N:'z'}} } } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyElementExplicitType("e", "A") {
                            propertyListOfElement("e") {
                                element("V") {
                                    propertyString("n", "v")
                                }
                                element("V") {
                                    propertyString("n", "w")
                                }
                                element("V") {
                                    propertyString("n", "x")
                                }
                                element("V") {
                                    propertyString("n", "y")
                                }
                                element("V") {
                                    propertyString("n", "z")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- ListSeparated ---
    @Test // S = as ; as = ['a' / ',']* ;
    fun _61_nonTerm_sepList_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = as ;
                as = ['a' / ',']* ;
            }
        """.trimIndent()
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { ref("as") }
            sList("as", 0, -1, "'a'", "','")
            literal("a")
            literal(",")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
            }
            dataType("as", "As") {
            }
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S")
            createObject("as", "As")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "", sppt = "S { as { <EMPTY_LIST> } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                    }
                }
            }
            define(sentence = "a", sppt = "S { as { 'a' } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                    }
                }
            }
            define(sentence = "a,a", sppt = "S { as { 'a' ',' 'a' } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                    }
                }
            }
            define(sentence = "a,a,a", sppt = "S { as { 'a' ',' 'a' ',' 'a' } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                    }
                }
            }
            define(sentence = "a,a,a,a", sppt = "S { as { 'a' ',' 'a' ',' 'a' ',' 'a' } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test // S = a bs c ; bs = ['b' / ',']* ;
    fun _62_concat_nonTerm_sepList_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a bs c;
                bs = ['b' / ',']* ;
                leaf a = 'a' ;
                leaf c = 'c' ;
            }
        """.trimIndent()
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { ref("a"); ref("bs"); ref("c") }
            sList("bs", 0, -1, "'b'", "','")
            literal("a", "a")
            literal("b")
            literal(",")
            literal("c", "c")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyPrimitiveType("c", "String", false, 2)
            }
            dataType("bs", "Bs") {

            }
            stringTypeFor("a")
            stringTypeFor("c")
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S") {
                assignment("a", "child[0]")
                assignment("c", "child[2]")
            }
            createObject("bs", "Bs")
            leafStringRule("a")
            leafStringRule("c")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "ac", sppt = "S { a:'a' bs { <EMPTY_LIST> } c:'c' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyString("a", "a")
                        propertyString("c", "c")
                    }
                }
            }
            define(sentence = "abc", sppt = "S { a:'a' bs { 'b' } c:'c' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyString("a", "a")
                        propertyString("c", "c")
                    }
                }
            }
            define(sentence = "ab,bc", sppt = "S { a:'a' bs { 'b' ',' 'b' } c:'c' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyString("a", "a")
                        propertyString("c", "c")
                    }
                }
            }
            define(sentence = "ab,b,bc", sppt = "S { a:'a' bs { 'b' ',' 'b' ',' 'b' } c:'c' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyString("a", "a")
                        propertyString("c", "c")
                    }
                }
            }
            define(sentence = "ab,b,b,bc", sppt = "S { a:'a' bs { 'b' ',' 'b' ',' 'b' ',' 'b' } c:'c' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyString("a", "a")
                        propertyString("c", "c")
                    }
                }
            }
        }
    }

    @Test // S = as ; as = [a / ',']* ;
    fun _63_nonTerm_sepList_literal_leaf() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = as ;
                as = [a / ',']* ;
                leaf a = 'a' ;
            }
        """.trimIndent()
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { ref("as") }
            sList("as", 0, -1, "a", "','")
            literal(",")
            literal("a", "a")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
                propertyListTypeOf("as", "String", false, 0) // of String
            }
            dataType("as", "As") {
                propertyListTypeOf("a", "String", false, 0) // of String
            }
            stringTypeFor("a")
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S") {
                assignment("as", "child[0].a")
            }
            createObject("as", "As") {
                assignment("a", "children.items")
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
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyListOfString("as", listOf())
                    }
                }
            }
            define(sentence = "a", sppt = "S { as { a:'a' } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyListOfString("as", listOf("a"))
                    }
                }
            }
            define(sentence = "a,a", sppt = "S { as { a:'a' ',' a:'a' } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyListOfString("as", listOf("a", "a"))
                    }
                }
            }
            define(sentence = "a,a,a", sppt = "S { as { a:'a' ',' a:'a' ',' a:'a' } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyListOfString("as", listOf("a", "a", "a"))
                    }
                }
            }
            define(sentence = "a,a,a,a", sppt = "S { as { a:'a' ',' a:'a' ',' a:'a' ',' a:'a' } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyListOfString("as", listOf("a", "a", "a", "a"))
                    }
                }
            }
        }
    }

    @Test //S = abs; abs = [ab / ',']*; ab = A | B;
    fun _64_sepList_of_supertype() {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { ref("abs") }
            sList("abs", 0, -1, "AB", "','")
            choiceLongest("AB") {
                ref("A")
                ref("B")
            }
            concatenation("A") { ref("a") }
            concatenation("B") { ref("b") }
            literal(",")
            literal("a", "a")
            literal("b", "b")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
                propertyListTypeOf("abs", "AB", false, 0)
            }
            dataType("abs", "Abs") {
                propertyListTypeOf("ab", "AB", false, 0)
            }
            dataType("A", "A") {
                propertyPrimitiveType("a", "String", false, 0)
            }
            dataType("B", "B") {
                propertyPrimitiveType("b", "String", false, 0)
            }
            dataType("AB", "AB") {
                subtypes("A", "B")
            }
            stringTypeFor("a")
            stringTypeFor("b")
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S") {
                assignment("abs", "child[0].ab")
            }
            createObject("abs", "Abs") {
                assignment("ab", "children.items")
            }
            createObject("A", "A") {
                assignment("a", "child[0]")
            }
            createObject("B", "B") {
                assignment("b", "child[0]")
            }
            subtypeRule("AB", "AB")
            leafStringRule("a")
            leafStringRule("b")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "", sppt = "S{ abs{ <EMPTY_LIST> } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyListOfElement("abs") {

                        }
                    }
                }
            }
            define(sentence = "a", sppt = "S{ abs{ AB{ A{a:'a'} } } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyListOfElement("abs") {
                            element("A") {
                                propertyString("a", "a")
                            }
                        }
                    }
                }
            }
            define(sentence = "b", sppt = "S{ abs{ AB{ B{b:'b'} } } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyListOfElement("abs") {
                            element("B") {
                                propertyString("b", "b")
                            }
                        }
                    }
                }
            }
            define(sentence = "a,a", sppt = "S{ abs{ AB{ A{a:'a'} } ',' AB{ A{a:'a'} } } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyListOfElement("abs") {
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
            define(sentence = "b,b", sppt = "S{ abs{ AB{ B{b:'b'} } ',' AB{ B{b:'b'} } } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyListOfElement("abs") {
                            element("B") {
                                propertyString("b", "b")
                            }
                            element("B") {
                                propertyString("b", "b")
                            }
                        }
                    }
                }
            }
            define(sentence = "a,b", sppt = "S{ abs{ AB{ A{a:'a'} } ',' AB{ B{b:'b'} } } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyListOfElement("abs") {
                            element("A") {
                                propertyString("a", "a")
                            }
                            element("B") {
                                propertyString("b", "b")
                            }
                        }
                    }
                }
            }
            define(sentence = "a,b,a,b,a,b", sppt = "S{ abs{ AB{ A{a:'a'} } ',' AB{ B{b:'b'} } ',' AB { A{a:'a'} } ',' AB{ B{b:'b'} } ',' AB { A{a:'a'} } ',' AB{ B{b:'b'} } } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyListOfElement("abs") {
                            element("A") {
                                propertyString("a", "a")
                            }
                            element("B") {
                                propertyString("b", "b")
                            }
                            element("A") {
                                propertyString("a", "a")
                            }
                            element("B") {
                                propertyString("b", "b")
                            }
                            element("A") {
                                propertyString("a", "a")
                            }
                            element("B") {
                                propertyString("b", "b")
                            }
                        }
                    }
                }
            }
        }
    }

    @Test //S = A B? ; A = ['a'/'.']+ ; B = 'b' ;
    fun _65_sepList_followed_by_empty() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = A B? ;
                A = ['a'/'.']+ ;
                B = 'b' ;
            }
        """
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { ref("A"); ref("§S§opt1") }
            optional("§S§opt1", "B", isPseudo = true)
            sList("A", 1, -1, "'a'", "'.'")
            concatenation("B") { literal("b") }
            literal("a")
            literal(".")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
                // no property 'a' because it is list of non-leaf literals
                propertyDataTypeOf("b", "B", true, 1)
            }
            dataType("A", "A") {
                // no properties because rule list contains only literals
            }
            dataType("B", "B") {
                // no properties because rule contains only literals
            }
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S") {
                assignment("b", "with(child[1]) child[0]")
            }
            createObject("A", "A") {}
            createObject("B", "B") {}
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { A { 'a' } §S§opt1 { <EMPTY> } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyNothing("b")
                    }
                }
            }
            define(sentence = "a.a", sppt = "S { A { 'a' '.' 'a' } §S§opt1 { <EMPTY> } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyNothing("b")
                    }
                }
            }
            define(sentence = "a.a.a", sppt = "S { A { 'a' '.' 'a' '.' 'a' } §S§opt1 { <EMPTY> } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyNothing("b")
                    }
                }
            }
            define(sentence = "ab", sppt = "S { A { 'a' } §S§opt1 { B{'b'} } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyElementExplicitType("b", "B") {}
                    }
                }
            }
            define(sentence = "a.ab", sppt = "S { A { 'a' '.' 'a' } §S§opt1 { B{'b'} } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyElementExplicitType("b", "B") {}
                    }
                }
            }
            define(sentence = "a.a.ab", sppt = "S { A { 'a' '.' 'a' '.' 'a' } §S§opt1 { B{'b'} } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyElementExplicitType("b", "B") {}
                    }
                }
            }
        }
    }

    @Test //S = As B? ; As = [A/'.']+ ; leaf A = 'a' ; leaf B = 'b' ;
    fun _66_sepList_followed_by_empty_leafs() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = As B? ;
                As = [A/'.']+ ;
                leaf A = 'a' ;
                leaf B = 'b' ;
            }
        """
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { ref("As"); ref("§S§opt1") }
            optional("§S§opt1", "B", isPseudo = true)
            sList("As", 1, -1, "A", "'.'")
            literal(".")
            literal("A", "a")
            literal("B", "b")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
                propertyListType("as", false, 0) { primitiveRef("String") }
                propertyPrimitiveType("b", "String", true, 1)
            }
            dataType("As", "As") {
                propertyListType("a", false, 0) { primitiveRef("String") }
            }
            stringTypeFor("A")
            stringTypeFor("B")
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S") {
                assignment("as", "child[0].a")
                assignment("b", "with(child[1]) child[0]")
            }
            createObject("As", "As") {
                assignment("a", "children.items")
            }
            leafStringRule("A")
            leafStringRule("B")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { As { A:'a' } §S§opt1 { <EMPTY> } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyListOfString("as", listOf("a"))
                        propertyNothing("b")
                    }
                }
            }
            define(sentence = "a.a", sppt = "S { As { A:'a' '.' A:'a' } §S§opt1 { <EMPTY> } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyListOfString("as", listOf("a", "a"))
                        propertyNothing("b")
                    }
                }
            }
            define(sentence = "a.a.a", sppt = "S { As { A:'a' '.' A:'a' '.' A:'a' } §S§opt1 { <EMPTY> } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyListOfString("as", listOf("a", "a", "a"))
                        propertyNothing("b")
                    }
                }
            }
            define(sentence = "ab", sppt = "S { As { A:'a' } §S§opt1 { B:'b' } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyListOfString("as", listOf("a"))
                        propertyString("b", "b")
                    }
                }
            }
            define(sentence = "a.ab", sppt = "S { As { A:'a' '.' A:'a' } §S§opt1 { B:'b' } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyListOfString("as", listOf("a", "a"))
                        propertyString("b", "b")
                    }
                }
            }
            define(sentence = "a.a.ab", sppt = "S { As { A:'a' '.' A:'a' '.' A:'a' } §S§opt1 { B:'b' } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyListOfString("as", listOf("a", "a", "a"))
                        propertyString("b", "b")
                    }
                }
            }
        }
    }

    @Test // S = ass ; ass = [as / ',']* ; as = a* ;
    fun _67_sepList_multi_nonTerm() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = ass ;
                ass = [as / ',']* ;
                as = a* ;
                a = 'a' ;
            }
        """.trimIndent()
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { ref("ass") }
            sList("ass", 0, -1, "as", "','")
            multi("as", 0, -1, "a")
            concatenation("a") { literal("a") }
            literal(",")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
                propertyListTypeOf("ass", "As", false, 0) // of String
            }
            dataType("ass", "Ass") {
                propertyListTypeOf("as", "As", false, 0)
            }
            dataType("as", "As") {
                propertyListTypeOf("a", "A", false, 0)
            }
            dataType("a", "A") {
            }
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S") {
                assignment("ass", "child[0].as")
            }
            createObject("ass", "Ass") {
                assignment("as", "children.items")
            }
            createObject("as", "As") {
                assignment("a", "children")
            }
            createObject("a", "A")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "", sppt = "S { ass { <EMPTY_LIST> } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyListOfElement("ass") {}
                    }
                }
            }
            define(sentence = "a", sppt = "S { ass { as { a { 'a' } } } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyListOfElement("ass") {
                            element("As") {
                                propertyElementExplicitType("a", "A") {

                                }
                            }
                        }
                    }
                }
            }
            define(sentence = "aa", sppt = "S { ass { as { a { 'a' } a { 'a' } } } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyListOfElement("ass") {
                            element("As") {
                                propertyListOfElement("a") {
                                    element("A") {}
                                    element("A") {}
                                }
                            }
                        }
                    }
                }
            }
            define(sentence = "a,a", sppt = "S{ ass{ as{ a{ 'a' } } ',' as{ a{ 'a' } } } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyListOfElement("ass") {
                            element("As") {
                                propertyListOfElement("a") {
                                    element("A") {}
                                }
                            }
                            element("As") {
                                propertyListOfElement("a") {
                                    element("A") {}
                                }
                            }
                        }
                    }
                }
            }
            define(sentence = "aa,aa", sppt = "S{ ass{ as{ a{ 'a' } a{ 'a' } } ',' as{ a{ 'a' } a{ 'a' } } }  }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyListOfElement("ass") {
                            element("As") {
                                propertyListOfElement("a") {
                                    element("A") {}
                                    element("A") {}
                                }
                            }
                            element("As") {
                                propertyListOfElement("a") {
                                    element("A") {}
                                    element("A") {}
                                }
                            }
                        }
                    }
                }
            }
            define(sentence = "a,a,a", sppt = "S{ ass{ as{ a{ 'a' } } ',' as{ a{ 'a' } } ',' as{ a{ 'a' } } } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyListOfElement("ass") {
                            element("As") {
                                propertyListOfElement("a") {
                                    element("A") {}
                                }
                            }
                            element("As") {
                                propertyListOfElement("a") {
                                    element("A") {}
                                }
                            }
                            element("As") {
                                propertyListOfElement("a") {
                                    element("A") {}
                                }
                            }
                        }
                    }
                }
            }
            define(sentence = "aaa,a,aa", sppt = "S{ ass{ as{ a{ 'a' } a{ 'a' } a{ 'a' } } ',' as{ a{ 'a' } } ',' as{ a{ 'a' } a{ 'a' } } } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyListOfElement("ass") {
                            element("As") {
                                propertyListOfElement("a") {
                                    element("A") {}
                                    element("A") {}
                                    element("A") {}
                                }
                            }
                            element("As") {
                                propertyListOfElement("a") {
                                    element("A") {}
                                }
                            }
                            element("As") {
                                propertyListOfElement("a") {
                                    element("A") {}
                                    element("A") {}
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Group ---
    @Test // S = ('b' 'c' 'd') ;
    fun _700_group_concat_literal_nonLeaf() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = ('b' 'c' 'd') ;
            }
        """.trimIndent()
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { ref("§S§group1") }
            concatenation("§S§group1", isPseudo = true) { literal("b"); literal("c"); literal("d") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S") {
            }
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "bcd", sppt = "S{ §S§group1 { 'b' 'c' 'd' } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test // S = (b c d) ;
    fun _701_group_concat_literal_nonLeaf() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = (b c d);
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
            }
        """.trimIndent()
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { ref("§S§group1") }
            concatenation("§S§group1", isPseudo = true) { ref("b"); ref("c"); ref("d") }
            literal("b", "b")
            literal("c", "c")
            literal("d", "d")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
                propertyTupleType("\$group", false, 0) {
                    primitive("b", "String", false)
                    primitive("c", "String", false)
                    primitive("d", "String", false)
                }
            }
            stringTypeFor("b")
            stringTypeFor("c")
            stringTypeFor("d")
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S") {
                assignment("\$group", "with(child[0]) tuple{ b:=child[0] c:=child[1] d:=child[2] }")
            }
            leafStringRule("b")
            leafStringRule("c")
            leafStringRule("d")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "bcd", sppt = "S{ §S§group1 { b:'b' c:'c' d:'d' } }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyTuple("\$group", 0) {
                            propertyString("b", "b")
                            propertyString("c", "c")
                            propertyString("d", "d")
                        }
                    }
                }
            }
        }
    }

    @Test // S = a ('b' 'c' 'd') e ;
    fun _711_concat_group_concat_literal_nonLeaf() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a ('b' 'c' 'd') e ;
                leaf a = 'a' ;
                leaf e = 'e' ;
            }
        """.trimIndent()
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { ref("a"); ref("§S§group1"); ref("e") }
            concatenation("§S§group1", isPseudo = true) { literal("b"); literal("c"); literal("d") }
            literal("a", "a")
            literal("e", "e")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyPrimitiveType("e", "String", false, 2)
            }
            stringTypeFor("a")
            stringTypeFor("e")
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S") {
                assignment("a", "child[0]")
                assignment("e", "child[2]")
            }
            leafStringRule("a")
            leafStringRule("e")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "abcde", sppt = "S{ a:'a' §S§group1 { 'b' 'c' 'd' } e:'e' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyString("a", "a")
                        propertyString("e", "e")
                    }
                }
            }
        }
    }

    @Test // S = a (b c d) e ;
    fun _712_concat_group_concat_literal_leaf() {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { ref("a"); ref("§S§group1"); ref("e") }
            concatenation("§S§group1", isPseudo = true) { ref("b"); ref("c"); ref("d") }
            literal("a", "a")
            literal("b", "b")
            literal("c", "c")
            literal("d", "d")
            literal("e", "e")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyTupleType("\$group", false, 1) {
                    primitive("b", "String", false)
                    primitive("c", "String", false)
                    primitive("d", "String", false)
                }
                propertyPrimitiveType("e", "String", false, 2)
            }
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("c")
            stringTypeFor("d")
            stringTypeFor("e")
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S") {
                assignment("a", "child[0]")
                assignment("\$group", "with(child[1]) tuple { b:=child[0] c:=child[1] d:=child[2] }")
                assignment("e", "child[2]")
            }
            leafStringRule("a")
            leafStringRule("b")
            leafStringRule("c")
            leafStringRule("d")
            leafStringRule("e")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "abcde", sppt = "S { a:'a' §S§group1 {b:'b' c:'c' d:'d'} e:'e' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyString("a", "a")
                        propertyTuple("\$group") {
                            propertyString("b", "b")
                            propertyString("c", "c")
                            propertyString("d", "d")
                        }
                        propertyString("e", "e")
                    }
                }
            }
        }
    }

    @Test // S = a (b c d) (b a c) e ;
    fun _713_concat_group_concat_leaf_literal_2() {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { ref("a"); ref("§S§group1"); ref("§S§group2"); ref("e") }
            concatenation("§S§group1", isPseudo = true) { ref("b"); ref("c"); ref("d") }
            concatenation("§S§group2", isPseudo = true) { ref("b"); ref("a"); ref("c") }
            literal("a", "a")
            literal("b", "b")
            literal("c", "c")
            literal("d", "d")
            literal("e", "e")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyTupleType("\$group", false, 1) {
                    primitive("b", "String", false)
                    primitive("c", "String", false)
                    primitive("d", "String", false)
                }
                propertyTupleType("\$group2", false, 2) {
                    primitive("b", "String", false)
                    primitive("a", "String", false)
                    primitive("c", "String", false)
                }
                propertyPrimitiveType("e", "String", false, 3)
            }
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("c")
            stringTypeFor("d")
            stringTypeFor("e")
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S") {
                assignment("a", "child[0]")
                assignment("\$group", "with(child[1]) tuple { b:=child[0] c:=child[1] d:=child[2] }")
                assignment("\$group2", "with(child[2]) tuple { b:=child[0] a:=child[1] c:=child[2] }")
                assignment("e", "child[3]")
            }
            leafStringRule("a")
            leafStringRule("b")
            leafStringRule("c")
            leafStringRule("d")
            leafStringRule("e")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "abcdbace", sppt = "S { a:'a' §S§group1 {b:'b' c:'c' d:'d'} §S§group2 {b:'b' a:'a' c:'c'} e:'e' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyString("a", "a")
                        propertyTuple("\$group") {
                            propertyString("b", "b")
                            propertyString("c", "c")
                            propertyString("d", "d")
                        }
                        propertyTuple("\$group2") {
                            propertyString("b", "b")
                            propertyString("a", "a")
                            propertyString("c", "c")
                        }
                        propertyString("e", "e")
                    }
                }
            }
        }
    }

    @Test // S = a (b) e ;
    fun _714_concat_group_1_leaf_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a (b) e ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf e = 'e' ;
            }
        """.trimIndent()
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { ref("a"); ref("§S§group1"); ref("e") }
            concatenation("§S§group1", isPseudo = true) { ref("b") }
            literal("a", "a")
            literal("b", "b")
            literal("e", "e")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyTupleType("\$group", false, 1) {
                    primitive("b", "String", false)
                }
                propertyPrimitiveType("e", "String", false, 2)
            }
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("e")
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S") {
                assignment("a", "child[0]")
                assignment("\$group", "with(child[1]) tuple{ b:=child[0] }")
                assignment("e", "child[2]")
            }
            leafStringRule("a")
            leafStringRule("b")
            leafStringRule("e")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "abe", sppt = "S { a:'a' §S§group1 { b:'b' }  e:'e' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyString("a", "a")
                        propertyTuple("\$group") {
                            propertyString("b", "b")
                        }
                        propertyString("e", "e")
                    }
                }
            }
        }
    }

    @Test // S = a (b (c) d) e ;
    fun _715_group_concat_group_group_leaf_literal() {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { ref("a"); ref("§S§group2"); ref("e") }
            concatenation("§S§group2", isPseudo = true) { ref("b"); ref("§S§group1"); ref("d") }
            concatenation("§S§group1", isPseudo = true) { ref("c") }
            literal("a", "a")
            literal("b", "b")
            literal("c", "c")
            literal("d", "d")
            literal("e", "e")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyTupleType("\$group", false, 1) {
                    primitive("b", "String", false)
                    tuple("\$group", false) {
                        primitive("c", "String", false)
                    }
                    primitive("d", "String", false)
                }
                propertyPrimitiveType("e", "String", false, 2)
            }
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("c")
            stringTypeFor("d")
            stringTypeFor("e")
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S") {
                assignment("a", "child[0]")
                assignment(
                    "\$group", """
                    with(child[1])
                      tuple {
                        b:=child[0]
                        ${'$'}group:=with(child[1]) tuple { c:=child[0] }
                        d:=child[2]
                      }
                    
                """.trimMargin()
                )
                assignment("e", "child[2]")
            }
            leafStringRule("a")
            leafStringRule("b")
            leafStringRule("c")
            leafStringRule("d")
            leafStringRule("e")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "abcde", sppt = "S { a:'a' §S§group2{ b:'b' §S§group1{c:'c'} d:'d' }  e:'e' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyString("a", "a")
                        propertyTuple("\$group") {
                            propertyString("b", "b")
                            propertyTuple("\$group") {
                                propertyString("c", "c")
                            }
                            propertyString("d", "d")
                        }
                        propertyString("e", "e")
                    }
                }
            }
        }
    }

    @Test // S = a (b | c | d) e ;
    fun _721_concat_group_choice_leaf_literal() {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { ref("a"); ref("§S§choice1"); ref("e") }
            choiceLongest("§S§choice1", isPseudo = true) {
                ref("b")
                ref("c")
                ref("d")
            }
            literal("a", "a")
            literal("b", "b")
            literal("c", "c")
            literal("d", "d")
            literal("e", "e")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyPrimitiveType("\$choice", "String", false, 1)
                propertyPrimitiveType("e", "String", false, 2)
            }
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("c")
            stringTypeFor("d")
            stringTypeFor("e")
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S") {
                assignment("a", "child[0]")
                assignment("\$choice", "with(child[1]) child[0]")
                assignment("e", "child[2]")
            }
            leafStringRule("a")
            leafStringRule("b")
            leafStringRule("c")
            leafStringRule("d")
            leafStringRule("e")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "abe", sppt = "S { a:'a' §S§choice1 { b:'b' } e:'e' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyString("a", "a")
                        propertyString("\$choice", "b")
                        propertyString("e", "e")
                    }
                }
            }
            define(sentence = "ace", sppt = "S { a:'a' §S§choice1 { c:'c' } e:'e' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyString("a", "a")
                        propertyString("\$choice", "c")
                        propertyString("e", "e")
                    }
                }
            }
            define(sentence = "ade", sppt = "S { a:'a' §S§choice1 { d:'d' } e:'e' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyString("a", "a")
                        propertyString("\$choice", "d")
                        propertyString("e", "e")
                    }
                }
            }
        }
    }

    @Test // S = a (b c | d) e ;
    fun _722_concat_group_choice_concat_leaf_literal() {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { ref("a"); ref("§S§choice1"); ref("e") }
            choiceLongest("§S§choice1", isPseudo = true) {
                concatenation { ref("b"); ref("c") }
                ref("d")
            }
            literal("a", "a")
            literal("b", "b")
            literal("c", "c")
            literal("d", "d")
            literal("e", "e")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyUnnamedSuperType("\$choice", false, 1) {
                    tupleType {
                        primitive("b", "String", false)
                        primitive("c", "String", false)
                    }
                    primitiveRef("String")
                }
                propertyPrimitiveType("e", "String", false, 2)
            }
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("c")
            stringTypeFor("d")
            stringTypeFor("e")
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S") {
                assignment("a", "child[0]")
                assignment("\$choice", "with(child[1]) when { 0==\$alternative -> tuple { b:=child[0] c:=child[1] } 1==\$alternative -> child[0] }")
                assignment("e", "child[2]")
            }
            leafStringRule("a")
            leafStringRule("b")
            leafStringRule("c")
            leafStringRule("d")
            leafStringRule("e")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "abce", sppt = "S { a:'a' §S§choice1 { b:'b' c:'c' } e:'e' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyString("a", "a")
                        propertyTuple("\$choice") {
                            propertyString("b", "b")
                            propertyString("c", "c")
                        }
                        propertyString("e", "e")
                    }
                }
            }
            define(sentence = "ade", sppt = "S { a:'a' §S§choice1 { d:'d' } e:'e' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyString("a", "a")
                        propertyString("\$choice", "d")
                        propertyString("e", "e")
                    }
                }
            }
        }
    }

    @Test // S = a (b c | d e) f ;
    fun _723_concat_group_choice_concat_leaf_literal_2() {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { ref("a"); ref("§S§choice1"); ref("f") }
            choiceLongest("§S§choice1", isPseudo = true) {
                concatenation { ref("b"); ref("c") }
                concatenation { ref("d"); ref("e") }
            }
            literal("a", "a")
            literal("b", "b")
            literal("c", "c")
            literal("d", "d")
            literal("e", "e")
            literal("f", "f")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyUnnamedSuperType("\$choice", false, 1) {
                    tupleType {
                        primitive("b", "String", false)
                        primitive("c", "String", false)
                    }
                    tupleType {
                        primitive("d", "String", false)
                        primitive("e", "String", false)
                    }
                }
                propertyPrimitiveType("f", "String", false, 2)
            }
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("c")
            stringTypeFor("d")
            stringTypeFor("e")
            stringTypeFor("f")
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S") {
                assignment("a", "child[0]")
                assignment("\$choice", "with(child[1]) when { 0==\$alternative -> tuple { b:=child[0] c:=child[1] } 1==\$alternative -> tuple { d:=child[0] e:=child[1] } }")
                assignment("f", "child[2]")
            }
            leafStringRule("a")
            leafStringRule("b")
            leafStringRule("c")
            leafStringRule("d")
            leafStringRule("e")
            leafStringRule("f")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "abcf", sppt = "S { a:'a' §S§choice1 { b:'b' c:'c' } f:'f' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyString("a", "a")
                        propertyTuple("\$choice") {
                            propertyString("b", "b")
                            propertyString("c", "c")
                        }
                        propertyString("f", "f")
                    }
                }
            }
            define(sentence = "adef", sppt = "S { a:'a' §S§choice1 { d:'d' e:'e' } f:'f' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyString("a", "a")
                        propertyTuple("\$choice") {
                            propertyString("d", "d")
                            propertyString("e", "e")
                        }
                        propertyString("f", "f")
                    }
                }
            }
        }
    }

    @Test // S = a ( ('x' | 'y') b c | d e) f ;
    fun _724_concat_group_choice_concat_leaf_literal_3() {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { ref("a"); ref("§S§choice2"); ref("f") }
            choiceLongest("§S§choice2", isPseudo = true) {
                concatenation { ref("§S§choice1"); ref("b"); ref("c") }
                concatenation { ref("d"); ref("e") }
            }
            choiceLongest("§S§choice1", isPseudo = true) {
                literal("x")
                literal("y")
            }
            literal("a", "a")
            literal("b", "b")
            literal("c", "c")
            literal("d", "d")
            literal("e", "e")
            literal("f", "f")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyUnnamedSuperType("\$choice", false, 1) {
                    tupleType {
                        primitive("b", "String", false)
                        primitive("c", "String", false)
                    }
                    tupleType {
                        primitive("d", "String", false)
                        primitive("e", "String", false)
                    }
                }
                propertyPrimitiveType("f", "String", false, 2)
            }
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("c")
            stringTypeFor("d")
            stringTypeFor("e")
            stringTypeFor("f")
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S") {
                assignment("a", "child[0]")
                assignment(
                    "\$choice", """
                    with(child[1]) when {
                      0==§alternative -> tuple { b:=child[1] c:=child[2] }
                      1==§alternative -> tuple { d:=child[0] e:=child[1] }
                    }
                """.trimMargin().replace("§", "$")
                )
                assignment("f", "child[2]")
            }
            leafStringRule("a")
            leafStringRule("b")
            leafStringRule("c")
            leafStringRule("d")
            leafStringRule("e")
            leafStringRule("f")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "axbcf", sppt = "S { a:'a' §S§choice2 { §S§choice1 { 'x' } b:'b' c:'c' } f:'f' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyString("a", "a")
                        propertyTuple("\$choice") {
                            propertyString("b", "b")
                            propertyString("c", "c")
                        }
                        propertyString("f", "f")
                    }
                }
            }
            define(sentence = "adef", sppt = "S { a:'a' §S§choice2 { d:'d' e:'e' } f:'f' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyString("a", "a")
                        propertyTuple("\$choice") {
                            propertyString("d", "d")
                            propertyString("e", "e")
                        }
                        propertyString("f", "f")
                    }
                }
            }
        }
    }

    @Test // S = a (b c | d e)? f ;
    fun _725_concat_group_choice_concat_leaf_literal_4() {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { ref("a"); ref("§S§opt1"); ref("f") }
            optional("§S§opt1", "§S§choice1", isPseudo = true)
            choiceLongest("§S§choice1", isPseudo = true) {
                concatenation { ref("b"); ref("c") }
                concatenation { ref("d"); ref("e") }
            }
            literal("a", "a")
            literal("b", "b")
            literal("c", "c")
            literal("d", "d")
            literal("e", "e")
            literal("f", "f")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyUnnamedSuperType("\$choice", true, 1) {
                    tupleType {
                        primitive("b", "String", false)
                        primitive("c", "String", false)
                    }
                    tupleType {
                        primitive("d", "String", false)
                        primitive("e", "String", false)
                    }
                }
                propertyPrimitiveType("f", "String", false, 2)
            }
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("c")
            stringTypeFor("d")
            stringTypeFor("e")
            stringTypeFor("f")
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S") {
                assignment("a", "child[0]")
                assignment(
                    "\$choice", """
                    with(child[1]) with(child[0]) when {
                       0==§alternative -> tuple { b:=child[0] c:=child[1] }
                       1==§alternative -> tuple { d:=child[0] e:=child[1] }
                    }
                """.trimIndent().replace("§", "$")
                )
                assignment("f", "child[2]")
            }
            leafStringRule("a")
            leafStringRule("b")
            leafStringRule("c")
            leafStringRule("d")
            leafStringRule("e")
            leafStringRule("f")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "af", sppt = "S { a:'a' §S§opt1 { <EMPTY> } f:'f' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyString("a", "a")
                        propertyNothing("\$choice")
                        propertyString("f", "f")
                    }
                }
            }
            define(sentence = "abcf", sppt = "S { a:'a' §S§opt1 { §S§choice1 { b:'b' c:'c' } } f:'f' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyString("a", "a")
                        propertyTuple("\$choice") {
                            propertyString("b", "b")
                            propertyString("c", "c")
                        }
                        propertyString("f", "f")
                    }
                }
            }
            define(sentence = "adef", sppt = "S { a:'a' §S§opt1 { §S§choice1 { d:'d' e:'e' } } f:'f' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyString("a", "a")
                        propertyTuple("\$choice") {
                            propertyString("d", "d")
                            propertyString("e", "e")
                        }
                        propertyString("f", "f")
                    }
                }
            }
        }
    }

    @Test //  S = a (b? c) e ;
    fun _726_concat_group_concat_optional() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a (b? c) e ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf e = 'e' ;
            }
        """.trimIndent()
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { ref("a"); ref("§S§group1"); ref("e") }
            concatenation("§S§group1", isPseudo = true) { ref("§S§opt1"); ref("c") }
            optional("§S§opt1", isPseudo = true, itemRef = "b")
            literal("a", "a")
            literal("b", "b")
            literal("c", "c")
            literal("e", "e")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyTupleType("\$group", false, 1) {
                    primitive("b", "String", true)
                    primitive("c", "String", false)
                }
                propertyPrimitiveType("e", "String", false, 2)
            }
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("c")
            stringTypeFor("e")
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S") {
                assignment("a", "child[0]")
                assignment(
                    "\$group", """
                    with(child[1]) tuple {
                      b:=with(child[0]) child[0]
                      c:=child[1]
                    }
                """.trimMargin().replace("§", "$")
                )
                assignment("e", "child[2]")
            }
            leafStringRule("a")
            leafStringRule("b")
            leafStringRule("c")
            leafStringRule("e")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "abce", sppt = "S { a:'a' §S§group1 { §S§opt1 { b:'b' } c:'c' } e:'e' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyString("a", "a")
                        propertyTuple("\$group") {
                            propertyString("b", "b")
                            propertyString("c", "c")
                        }
                        propertyString("e", "e")
                    }
                }
            }
            define(sentence = "ace", sppt = "S { a:'a' §S§group1 { §S§opt1 { <EMPTY> } c:'c' } e:'e' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyString("a", "a")
                        propertyTuple("\$group") {
                            propertyString("b", null)
                            propertyString("c", "c")
                        }
                        propertyString("e", "e")
                    }
                }
            }
        }
    }

    @Test // S = a ( (b | c) (d?) e ) f ;
    fun _727_concat_group_choice_group_concat_optional() {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { ref("a"); ref("§S§group2"); ref("f") }
            concatenation("§S§group2", isPseudo = true) { ref("§S§choice1"); ref("§S§group1"); ref("e") }
            choiceLongest("§S§choice1", isPseudo = true) {
                ref("b")
                ref("c")
            }
            optional("§S§group1", isPseudo = true, itemRef = "d")
            literal("a", "a")
            literal("b", "b")
            literal("c", "c")
            literal("d", "d")
            literal("e", "e")
            literal("f", "f")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyTupleType("\$group", false, 1) {
                    primitive("\$choice", "String", false)
                    tuple("\$group", false) {
                        primitive("d", "String", true)
                    }
                    primitive("e", "String", false)
                }
                propertyPrimitiveType("f", "String", false, 2)
            }
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("c")
            stringTypeFor("d")
            stringTypeFor("e")
            stringTypeFor("f")
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S") {
                assignment("a", "child[0]")
                assignment(
                    "\$group", """
                    with(child[1]) tuple {
                       §choice := with(child[0]) child[0]
                       §group := with(child[1]) tuple { d := child[0] }
                       e := child[2]
                    }
                """.trimMargin().replace("§", "$")
                )
                assignment("f", "child[2]")
            }
            leafStringRule("a")
            leafStringRule("b")
            leafStringRule("c")
            leafStringRule("d")
            leafStringRule("e")
            leafStringRule("f")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "abef", sppt = "S { a:'a' §S§group2 { §S§choice1 { b:'b' } §S§group1 { <EMPTY> } e:'e' } f:'f' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyString("a", "a")
                        propertyTuple("\$group") {
                            propertyString("\$choice", "b")
                            propertyTuple("\$group") {
                                propertyNothing("d")
                            }
                            propertyString("e", "e")
                        }
                        propertyString("f", "f")
                    }
                }
            }
            define(sentence = "acef", sppt = "S { a:'a' §S§group2 { §S§choice1 { c:'c' } §S§group1 { <EMPTY> } e:'e' } f:'f' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyString("a", "a")
                        propertyTuple("\$group") {
                            propertyString("\$choice", "c")
                            propertyTuple("\$group") {
                                propertyNothing("d")
                            }
                            propertyString("e", "e")
                        }
                        propertyString("f", "f")
                    }
                }
            }
            define(sentence = "abdef", sppt = "S { a:'a' §S§group2 { §S§choice1 { b:'b' } §S§group1 { d:'d' } e:'e' } f:'f' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyString("a", "a")
                        propertyTuple("\$group") {
                            propertyString("\$choice", "b")
                            propertyTuple("\$group") {
                                propertyString("d", "d")
                            }
                            propertyString("e", "e")
                        }
                        propertyString("f", "f")
                    }
                }
            }
            define(sentence = "acdef", sppt = "S { a:'a' §S§group2 { §S§choice1 { c:'c' } §S§group1 { d:'d' } e:'e' } f:'f' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                        propertyString("a", "a")
                        propertyTuple("\$group") {
                            propertyString("\$choice", "c")
                            propertyTuple("\$group") {
                                propertyString("d", "d")
                            }
                            propertyString("e", "e")
                        }
                        propertyString("f", "f")
                    }
                }
            }
        }
    }

    @Test // S = a (BC | d+) e ;
    fun _728_concat_group_choice_concat_nonTerm_list() {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { ref("a"); ref("§S§choice1"); ref("e") }
            choiceLongest("§S§choice1", isPseudo = true) {
                ref("BC")
                ref("§S§multi1")
            }
            concatenation("BC") { ref("b"); ref("c") }
            multi("§S§multi1",1,-1,"d", isPseudo = true)
            literal("a", "a")
            literal("b", "b")
            literal("c", "c")
            literal("d", "d")
            literal("e", "e")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
                propertyPrimitiveType("a","String",false,0)
                propertyUnnamedSuperType("\$choice",false,1) {
                    elementRef("BC")
                    listType { primitiveRef("String") }
                }
                propertyPrimitiveType("e","String",false,2)
            }
            dataType("BC", "BC") {
                propertyPrimitiveType("b","String",false,0)
                propertyPrimitiveType("c","String",false,1)
            }
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("c")
            stringTypeFor("d")
            stringTypeFor("e")
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S") {
                assignment("a", "child[0]")
                assignment(
                    "\$choice", """
                    with(child[1]) when {
                       0 == §alternative -> child[0]
                       1 == §alternative -> children
                    }
                """.trimMargin().replace("§", "$")
                )
                assignment("e", "child[2]")
            }
            createObject("BC","BC") {
                assignment("b", "child[0]")
                assignment("c", "child[1]")
            }
            leafStringRule("a")
            leafStringRule("b")
            leafStringRule("c")
            leafStringRule("d")
            leafStringRule("e")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "abce", sppt = "S { a:'a' §S§choice1 { BC { b:'b' c:'c' } } e:'e' }") {
                asmSimple {
                    element("S") {
                        propertyString("a", "a")
                        propertyElementExplicitType("\$choice", "BC") {
                            propertyString("b", "b")
                            propertyString("c", "c")
                        }
                        propertyString("e", "e")
                    }
                }
            }
            define(sentence = "ade", sppt = "S { a:'a' §S§choice1 { §S§multi1 { d:'d' } } e:'e' }") {
                asmSimple {
                    element("S") {
                        propertyString("a", "a")
                        propertyListOfString("\$choice", listOf("d"))
                        propertyString("e", "e")
                    }
                }
            }
            define(sentence = "adde", sppt = "S { a:'a' §S§choice1 { §S§multi1 { d:'d' d:'d' } } e:'e' }") {
                asmSimple {
                    element("S") {
                        propertyString("a", "a")
                        propertyListOfString("\$choice", listOf("d", "d"))
                        propertyString("e", "e")
                    }
                }
            }
            define(sentence = "addde", sppt = "S { a:'a' §S§choice1 { §S§multi1 { d:'d' d:'d' d:'d' } } e:'e' }") {
                asmSimple {
                    element("S") {
                        propertyString("a", "a")
                        propertyListOfString("\$choice", listOf("d","d","d"))
                        propertyString("e", "e")
                    }
                }
            }
        }
    }

    @Test // S = a (BC | d+)? e ;
    fun _729_concat_group_choice_concat_nonTerm_list_2() {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { ref("a"); ref("§S§opt1"); ref("e") }
            optional("§S§opt1", "§S§choice1", isPseudo = true)
            choiceLongest("§S§choice1", isPseudo = true) {
                ref("BC")
                ref("§S§multi1")
            }
            concatenation("BC") { ref("b"); ref("c") }
            multi("§S§multi1",1,-1,"d", isPseudo = true)
            literal("a", "a")
            literal("b", "b")
            literal("c", "c")
            literal("d", "d")
            literal("e", "e")
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
                propertyPrimitiveType("a","String",false,0)
                propertyUnnamedSuperType("\$choice",true,1) {
                    elementRef("BC")
                    listType { primitiveRef("String") }
                }
                propertyPrimitiveType("e","String",false,2)
            }
            dataType("BC", "BC") {
                propertyPrimitiveType("b","String",false,0)
                propertyPrimitiveType("c","String",false,1)
            }
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("c")
            stringTypeFor("d")
            stringTypeFor("e")
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S") {
                assignment("a", "child[0]")
                assignment(
                    "\$choice", """
                    with(child[1]) with(child[0]) when {
                       0 == §alternative -> child[0]
                       1 == §alternative -> children
                    }
                """.trimMargin().replace("§", "$")
                )
                assignment("e", "child[2]")
            }
            createObject("BC","BC") {
                assignment("b", "child[0]")
                assignment("c", "child[1]")
            }
            leafStringRule("a")
            leafStringRule("b")
            leafStringRule("c")
            leafStringRule("d")
            leafStringRule("e")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "ae", sppt = "S { a:'a' §S§opt1 { <EMPTY> } e:'e' }") {
                asmSimple {
                    element("S") {
                        propertyString("a", "a")
                        propertyNothing("\$choice")
                        propertyString("e", "e")
                    }
                }
            }
            define(sentence = "abce", sppt = "S { a:'a' §S§opt1 { §S§choice1 { BC { b:'b' c:'c' } } } e:'e' }") {
                asmSimple {
                    element("S") {
                        propertyString("a", "a")
                        propertyElementExplicitType("\$choice", "BC") {
                            propertyString("b", "b")
                            propertyString("c", "c")
                        }
                        propertyString("e", "e")
                    }
                }
            }
            define(sentence = "ade", sppt = "S { a:'a' §S§opt1 { §S§choice1 { §S§multi1 { d:'d' } } } e:'e' }") {
                asmSimple {
                    element("S") {
                        propertyString("a", "a")
                        propertyListOfString("\$choice", listOf("d"))
                        propertyString("e", "e")
                    }
                }
            }
            define(sentence = "adde", sppt = "S { a:'a' §S§opt1 { §S§choice1 { §S§multi1 { d:'d' d:'d' } } } e:'e' }") {
                asmSimple {
                    element("S") {
                        propertyString("a", "a")
                        propertyListOfString("\$choice", listOf("d", "d"))
                        propertyString("e", "e")
                    }
                }
            }
            define(sentence = "addde", sppt = "S { a:'a' §S§opt1 { §S§choice1 { §S§multi1 { d:'d' d:'d' d:'d' } } } e:'e' }") {
                asmSimple {
                    element("S") {
                        propertyString("a", "a")
                        propertyListOfString("\$choice", listOf("d","d","d"))
                        propertyString("e", "e")
                    }
                }
            }
        }
    }

    @Test // S = a b | c d e ;
    fun _740_choice_concat_nonTerm() {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test // S = (a b) | (c d e) ;
    fun _741_rhs_choice_concat_nonTerm() {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test // S = (a b | c d e) ;
    fun _742_rhs_choice_concat_nonTerm() {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test // S = ((a b) | (c d e)) ;
    fun _743_rhs_choice_concat_nonTerm() {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test // S = x (a b | c d e) y ;
    fun _744_rhs_choice_concat_nonTerm() {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test // S = CH; CH = a b | c d e ;
    fun _745_rhs_choice_concat_nonTerm() {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test // S = CH; CH = (a b | c d e) ;
    fun _746_rhs_choice_concat_nonTerm() {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test // S = x CH y ; CH = a b | c d e
    fun _7_47_rhs_choice_concat_nonTerm() {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                    }
                }
            }
        }
    }

    @Test // S = x C y ; C = (a b | c d e)
    fun _748_rhs_choice_concat_nonTerm() {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                    }
                }
            }
        }
    }


    @Test
    fun _760_where_root_is_UnnamedSuperType() {
        TODO()
    }

    @Test
    fun _761_group_where_tuple_property_is_UnnamedSuperType() {
        TODO()
    }

    @Test
    fun _762_UnnamedSuperType_of_UnnamedSuperType() {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
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
        val expectedRrs = ruleSet("test.Test") {
            concatenation("S") { literal("a") }
        }
        val expectedTm = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmGrammarTransform(
            "test.Test",
            typeModel = grammarTypeModel("test.Test", "Test") {}.also { it.resolveImports() },
            true
        ) {
            createObject("S", "S")
        }
        test(
            grammarStr = grammarStr,
            expectedRrs = expectedRrs,
            expectedTm = expectedTm,
            expectedTr = expectedTr
        ) {
            define(sentence = "a", sppt = "S { 'a' }") {
                asmSimple(typeModel = expectedTm, defaultNamespace = QualifiedName("test.Test")) {
                    element("S") {
                    }
                }
            }
        }
    }
}