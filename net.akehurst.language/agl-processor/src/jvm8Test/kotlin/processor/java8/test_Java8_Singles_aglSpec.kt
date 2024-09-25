/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.processor.java8

//import com.soywiz.korio.async.runBlockingNoSuspensions
//import com.soywiz.korio.file.std.resourcesVfs
import net.akehurst.language.agl.Agl
import net.akehurst.language.grammar.processor.AglGrammarSemanticAnalyser
import net.akehurst.language.grammar.processor.ContextFromGrammarRegistry
import net.akehurst.language.agl.simple.ContextAsmSimple
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.parser.leftcorner.ParseOptionsDefault
import test.assertEqualsWarning
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class test_Java8_Singles_aglSpec {

    private companion object {
        val grammarFile = "/Java/version_8/grammars/grammar_aglSpec.agl"
        val aglSpecProcessor: LanguageProcessor<Asm, ContextAsmSimple> by lazy { createJava8Processor(grammarFile, true) }

        val proc = aglSpecProcessor

        fun createJava8Processor(path: String, toUpper: Boolean = false): LanguageProcessor<Asm, ContextAsmSimple> {
            val grammarStr = this::class.java.getResource(path).readText()
            val proc = Agl.processorFromString<Asm, ContextAsmSimple>(
                grammarDefinitionStr = grammarStr,
                aglOptions = Agl.options {
                    semanticAnalysis {
                        context(ContextFromGrammarRegistry(Agl.registry))
                        // switch off ambiguity analysis for performance
                        option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, false)
                    }
                }
            ).processor!!
            val forRule = if (toUpper) "CompilationUnit" else "compilationUnit"
            //proc.buildFor(Agl.parseOptions { goalRuleName(forRule) }) //TODO: use build
            return proc
        }
    }

    @Test
    fun literal() {
        val sentence = "0"
        val goal = "Literal"

        val result = proc.parse(sentence, ParseOptionsDefault(goal))
        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues.errors)
    }

    @Test
    fun Types_Type__int() {

        val grammarStr = this::class.java.getResource(grammarFile).readText()
        val goal = "Type"
        val p = Agl.processorFromString<Asm, ContextAsmSimple>(
            grammarDefinitionStr = grammarStr,
            aglOptions = Agl.options {
                semanticAnalysis {
                    active(false) // switch off for performance
                }
            },
            configuration = Agl.configuration {
                defaultGoalRuleName(goal)
            }
        ).processor!!

        val sentence = "int"
        val result = p.parse(sentence, ParseOptionsDefault(goal))
        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues.errors)
        assertEqualsWarning(2, result.sppt!!.maxNumHeads)
    }

    @Test
    fun Expressions_Type__int() {
        val grammarStr = """
            namespace test

            grammar Expressions {
                leaf ID = "[A-Za-z]"+ ;
                leaf UPT = 'int' ;

                UType = UTypeNonArray Dims? ;
                UTypeNonArray = UTypeReference | UPT ;
                UTypeReference = ID TypeArguments? ;
                TypeArguments = '<' ReferenceType '>' ;
                ReferenceType = Annotation* UTypeReference ;
                Annotation = ID  TypeArguments?  ;
                Dims = Annotation '[' ']' ;
            }
        """.trimIndent()
        val goal = "UType"
        val p = Agl.processorFromString(grammarStr, Agl.configuration { defaultGoalRuleName(goal) }).processor!!

        val sentence = "int"
        val result = p.parse(sentence, ParseOptionsDefault(goal))
        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues.errors)

        assertEqualsWarning(2, result.sppt!!.maxNumHeads)
    }

    @Test
    fun t1() {
        //val sentence = "import x; @An() interface An {  }"
        val sentence = "import x; @An() interface An {  }"
        val goal = "CompilationUnit"

        val result = proc.parse(sentence, ParseOptionsDefault(goal))
        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues.errors)
    }

    @Test
    fun arrayIndex() {
        val sentence = "a[0]"
        val goal = "Expression"
        val result = proc.parse(sentence, ParseOptionsDefault(goal))
        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues.errors)
    }

    @Test
    fun t() {
        val sentence = "a[0].b"
        val goal = "Expression"
        val result = proc.parse(sentence, ParseOptionsDefault(goal))
        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues.errors)
    }

    @Test
    fun bad_Binary_Literal() {
        val sentence = "0b012"
        val goal = "VariableInitializer"

        val result = proc.parse(sentence, ParseOptionsDefault(goal))
        assertNull(result.sppt)
        assertEquals(
            listOf(
                LanguageIssue(
                    LanguageIssueKind.ERROR, LanguageProcessorPhase.PARSE, InputLocation(4, 5, 1, 1),
                    "0b01^2",
                    setOf(
                        "<EOT>",
                        "'.'",
                        "'['",
                        "'::'",
                        "'++'",
                        "'--'",
                        "'*'",
                        "'/'",
                        "'%'",
                        "'+'",
                        "'-'",
                        "'<<'",
                        "'>>'",
                        "'>>>'",
                        "'<'",
                        "'>'",
                        "'<='",
                        "'>='",
                        "'instanceof'",
                        "'=='",
                        "'!='",
                        "'&'",
                        "'^'",
                        "'|'",
                        "'&&'",
                        "'||'",
                        "'?'"
                    )
                )
            ), result.issues.errors
        )

    }

    @Test
    fun BadLiterals() {
        val sentence = """
/*
 * @test /nodynamiccopyright/
 * @bug 6860965
 * @summary Project Coin: binary literals
 * @compile/fail/ref=BadBinaryLiterals.6.out -XDrawDiagnostics -source 6 -Xlint:-options BadBinaryLiterals.java
 * @compile/fail/ref=BadBinaryLiterals.7.out -XDrawDiagnostics BadBinaryLiterals.java
 */

public class BadBinaryLiterals {
    int valid = 0b0;            // valid literal, illegal in source 6
    int baddigit = 0b011;       // bad digit
                    //aaaabbbbccccddddeeeeffffgggghhhh
    int overflow1 = 0b111111111111111111111111111111111; // too long for int
                    //aaaabbbbccccddddeeeeffffgggghhhhiiiijjjjkkkkllllmmmmnnnnoooopppp
    int overflow2 = 0b11111111111111111111111111111111111111111111111111111111111111111L; // too long for long
    float badfloat1 = 0b01.01;  // no binary floats
    float badfloat2 = 0b01e01;  // no binary floats
}
            """.trimIndent()
        val goal = "CompilationUnit"
        val result = proc.parse(sentence, ParseOptionsDefault(goal))
        assertNull(result.sppt)
        assertEquals(
            listOf(
                LanguageIssue(
                    LanguageIssueKind.ERROR, LanguageProcessorPhase.PARSE, InputLocation(799, 28, 16, 1),
                    "...t1 = 0b01.^01;  // no...",
                    setOf("IDENTIFIER", "'new'", "'<'")
                )
            ), result.issues.errors
        )

    }

    @Test
    fun UnannQualifiedTypeReference1() {
        val sentence = "Map.Entry<Object,Object> x;"
        val goal = "BlockStatement"
        val result = proc.parse(sentence, ParseOptionsDefault(goal))
        assertEquals(emptyList(), result.issues.errors)
        assertNotNull(result.sppt)
    }

    @Test
    fun UnannQualifiedTypeReference2() {
        val sentence = "Map.Entry<Object,Object> x;"
        val goal = "BlockStatement"
        val result = proc.parse(sentence, ParseOptionsDefault(goal))
        assertEquals(emptyList(), result.issues.errors)
        assertNotNull(result.sppt)
    }

    @Test
    fun UnannQualifiedTypeReference() {
        val sentence = "{ Map.@An Entry<Object,Object> x; }"
        val goal = "Block"
        val result = proc.parse(sentence, ParseOptionsDefault(goal))
        assertEquals(emptyList(), result.issues.errors)
        assertNotNull(result.sppt)
    }

    @Test
    fun Enum() {
        val sentence = "enum E { A, B, C }"
        val goal = "ClassDeclaration"
        val result = proc.parse(sentence, ParseOptionsDefault(goal))
        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues.errors)
        val actual = result.sppt!!.toStringAll
        val resultStr = result.sppt!!.asSentence
        assertEquals(sentence, resultStr)
    }

    @Test
    fun xx() {
        val sentence = "interface An { An[] value(); }"
        val goal = "CompilationUnit"
        val result = proc.parse(sentence, ParseOptionsDefault(goal))
        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues.errors)
        val actual = result.sppt!!.toStringAll
        val resultStr = result.sppt!!.asSentence
        assertEquals(sentence, resultStr)
    }

    @Test
    fun long_expression() {

        val sentence = """
          "a" + "b" + "c" + "d" + "e" + "f" + "g" + "h" + "i" + "j" + "k"
        """.trimIndent()
        val goal = "Expression"

        val result = proc.parse(sentence, ParseOptionsDefault(goal))
        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues.errors)

        // println( t.toStringAll )
        val resultStr = result.sppt!!.asSentence
        assertEquals(sentence, resultStr)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test(timeout = 10000)
    fun long_concatenation() {

        val sentence = """
          {
            concat =  "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e"
                    + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" + "a" + "b" + "c" + "d" + "e" ;
          }
        """.trimIndent()
        val goal = "Block"

        val result = proc.parse(sentence, ParseOptionsDefault(goal))
        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues.errors)

        // println( t.toStringAll )
        val resultStr = result.sppt!!.asSentence
        assertEquals(sentence, resultStr)
    }

}