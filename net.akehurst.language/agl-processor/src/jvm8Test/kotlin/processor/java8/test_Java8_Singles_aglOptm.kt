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
import net.akehurst.language.agl.simple.ContextAsmSimple
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.grammar.processor.AglGrammarSemanticAnalyser
import net.akehurst.language.grammar.processor.ContextFromGrammarRegistry
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.parser.leftcorner.ParseOptionsDefault
import net.akehurst.language.processor.test.utils.notWidth
import net.akehurst.language.sentence.api.InputLocation
import kotlin.test.*

class test_Java8_Singles_aglOptm {

    companion object {
        val grammarFile = "/Java/version_8/grammars/grammar_aglOptm.agl"
        val proc: LanguageProcessor<Asm, ContextAsmSimple> = createJava8Processor(grammarFile, true)

        fun createJava8Processor(path: String, toUpper: Boolean = false): LanguageProcessor<Asm, ContextAsmSimple> {
            val grammarStr = this::class.java.getResource(path)?.readText() ?: error("file not found '$path'")
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
            //proc.buildFor(forRule)//TODO: use build
            return proc
        }

    }

    @Test
    fun Literal__0() {
        val sentence = "0"
        val goal = "Literal"

        val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun Expression__true() {
        val sentence = "true"
        val goal = "Expression"

        val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        // expect WIDTH ambiguity as 'true' could be a type name
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(2, result.sppt!!.maxNumHeads)
    }

    @Test//(timeout = 5000)
    fun Types_Type__int() {

        val grammarStr = this::class.java.getResource(grammarFile).readText()
        val goal = "Type"
        val p = Agl.processorFromString(
            grammarDefinitionStr = grammarStr,
            configuration = Agl.configuration() {
                targetGrammarName(("Types"))
                defaultGoalRuleName(goal)
            },
            aglOptions = Agl.options {
                semanticAnalysis {
                    // switch off ambiguity analysis for performance
                    option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, false)
                    context(ContextFromGrammarRegistry(Agl.registry))
                }
            }).processor!!

        val sentence = "int"
        val result = p.parse(sentence, Agl.parseOptions { goalRuleName("TypeReference") })//TODO: use build
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test(timeout = 5000)
    fun UType__int() {
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
        val result = p.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test(timeout = 5000)
    fun CompilationUnit__import_annotation_interface() {
        //val sentence = "import x; @An() interface An {  }"
        val sentence = "import x; @An() interface An {  }"
        val goal = "CompilationUnit"

        val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test(timeout = 5000)
    fun Expression_arrayIndex() {
        val sentence = "a[0]"
        val goal = "Expression"
        val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test(timeout = 5000)
    fun ArrayAccess__arrayIndex() {
        val sentence = "a[0]"
        val goal = "ArrayAccess"
        val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test(timeout = 5000)
    fun subgrammar__ArrayAccess__arrayIndex() {
        val grammarStr = """
            namespace test

grammar Expressions {

    leaf IDENTIFIER = "[A-Za-z]+" ;

    Expression = Primary Nav? ;
    Nav = '.' IDENTIFIER ;

    Primary
      = IDENTIFIER
      | MethodReference
      | ArrayAccess
      | ClassInstanceCreationExpression
      ;

    MethodReference
      = ArrayType '::' IDENTIFIER
      | Primary '::' IDENTIFIER
      ;

    ArrayType = QualifiedTypeReference '[' ']' ;
    QualifiedTypeReference = UnannTypeReference ;
    UnannTypeReference = IDENTIFIER TypeArguments? ;
    TypeArguments = '<'  '>' ;
    
    ClassInstanceCreationExpression  = Primary '.'  'new' ;

    ArrayAccess = Expression '[' Expression ']' ;
}
        """.trimIndent()
        val p = Agl.processorFromString<Any, Any>(grammarStr).processor!!

        val sentence = "a[b]"
        val goal = "ArrayAccess"
        val result = p.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test(timeout = 5000)
    fun Expression__arrayIndex_navigationToField() {
        val sentence = "a[0].b"
        val goal = "Expression"
        val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test(timeout = 5000)
    fun FieldDeclaration__int_valid_eq_0b0() {
        val sentence = "int valid = 0b0;"
        val goal = "FieldDeclaration"
        //proc.parse(goal, sentence)
        val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test(timeout = 5000)
    fun TypeDeclaration__class_fieldDeclaration() {
        val sentence = "class A { int valid = 0b0; }"
        val goal = "TypeDeclaration"
        val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test(timeout = 5000)
    fun ClassBody__fieldDeclaration() {
        val sentence = "{ int valid = 0b0; }"
        val goal = "ClassBody"
        val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test(timeout = 5000)
    fun CompilationUnit__fieldDeclaration() {
        val sentence = "class A { int valid = 0b0; }"
        val goal = "CompilationUnit"
        val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test(timeout = 5000)
    fun VariableInitializer__badBinaryLiteral() {
        val sentence = "0b012"
        val goal = "VariableInitializer"

        val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertNull(result.sppt)
        assertEquals(
            listOf(
                LanguageIssue(
                    LanguageIssueKind.ERROR, LanguageProcessorPhase.PARSE, InputLocation(4, 5, 1, 1, null),
                    "0b01^2",
                    setOf("'.'", "ASSIGNMENT_OPERATOR", "'::'", "'?'", "INFIX_OPERATOR", "POSTFIX_OPERATOR", "'['", "<EOT>")
                )
            ), result.issues.errors
        )

    }

    @Test(timeout = 5000)
    fun CompilationUnit__badLiterals() {
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
        val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(1, result.sppt!!.maxNumHeads)

    }

    @Test(timeout = 5000)
    fun BlockStatement__UnannQualifiedTypeReference1() {
        val sentence = "Map.Entry<Object,Object> x;"
        val goal = "BlockStatement"
        val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test(timeout = 5000)
    fun BlockStatement__UnannQualifiedTypeReference2() {
        val sentence = "Map.Entry<Object,Object> x;"
        val goal = "BlockStatement"
        val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test(timeout = 5000)
    fun Block__UnannQualifiedTypeReference() {
        val sentence = "{ Map.@An Entry<Object,Object> x; }"
        val goal = "Block"
        val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test(timeout = 5000)
    fun ClassDeclaration__enumDecl() {
        val sentence = "enum E { A, B, C }"
        val goal = "ClassDeclaration"
        val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(1, result.sppt!!.maxNumHeads)

        val resultStr = result.sppt!!.asSentence
        assertEquals(sentence, resultStr)
    }

    @Test(timeout = 5000)
    fun CompilationUnit__interfaceDecl() {
        val sentence = "interface An { An[] value(); }"
        val goal = "CompilationUnit"
        val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val resultStr = result.sppt!!.asSentence
        assertEquals(sentence, resultStr)
    }

    @Test(timeout = 5000)
    fun CompilationUnit__class_with_constructor() {
        val sentence = "class B {  B() {  } }"
        val goal = "CompilationUnit"
        val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val resultStr = result.sppt!!.asSentence
        assertEquals(sentence, resultStr)
    }

    @Test(timeout = 5000)
    fun FormalParameterList__A_a() {
        val sentence = "A a"
        val goal = "FormalParameterList"
        val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(1, result.sppt!!.maxNumHeads)

        val resultStr = result.sppt!!.asSentence
        assertEquals(sentence, resultStr)
    }

    @Test(timeout = 5000)
    fun FormalParameterList__A_this() {
        val sentence = "A this"
        val goal = "FormalParameterList"
        val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(1, result.sppt!!.maxNumHeads)

        val resultStr = result.sppt!!.asSentence
        assertEquals(sentence, resultStr)
    }

    @Test(timeout = 5000)
    fun FormalParameterList__varargsParameter() {
        val sentence = "A... this"
        val goal = "FormalParameterList"
        val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(1, result.sppt!!.maxNumHeads)

        val resultStr = result.sppt!!.asSentence
        assertEquals(sentence, resultStr)
    }

    @Test(timeout = 5000)
    fun FormalParameterList__2parameters() {
        val sentence = "A a, B b"
        val goal = "FormalParameterList"
        val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(1, result.sppt!!.maxNumHeads)

        val resultStr = result.sppt!!.asSentence
        assertEquals(sentence, resultStr)
    }

    @Test(timeout = 5000)
    fun FormalParameterList__3parameters() {
        val sentence = "A a, B b, C c"
        val goal = "FormalParameterList"
        val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(1, result.sppt!!.maxNumHeads)

        val resultStr = result.sppt!!.asSentence
        assertEquals(sentence, resultStr)
    }

    @Test(timeout = 5000)
    fun ConstructorDeclaration__head_body() {
        val sentence = "B() {  }"
        val goal = "ConstructorDeclaration"
        val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(1, result.sppt!!.maxNumHeads)

        val resultStr = result.sppt!!.asSentence
        assertEquals(sentence, resultStr)
    }

    @Test(timeout = 5000)
    fun ConstructorDeclarator__head() {
        val sentence = "B()"
        val goal = "ConstructorDeclarator"
        val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(1, result.sppt!!.maxNumHeads)

        val resultStr = result.sppt!!.asSentence
        assertEquals(sentence, resultStr)
    }

    @Test(timeout = 5000)
    fun ConstructorBody__body() {
        val sentence = "{  }"
        val goal = "ConstructorBody"
        val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(1, result.sppt!!.maxNumHeads)

        val resultStr = result.sppt!!.asSentence
        assertEquals(sentence, resultStr)
    }

    @Test(timeout = 5000)//(timeout = 10000)
    fun Block__long_concatenation() {

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

        val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(1, result.sppt!!.maxNumHeads)

        val resultStr = result.sppt!!.asSentence
        assertEquals(sentence, resultStr)
    }

    @Test(timeout = 20000)
    fun CompilationUnit__fromStdLib_CharBufferSpliterator() {

        val sentence = """
/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.nio;

import java.util.Comparator;
import java.util.Spliterator;
import java.util.function.IntConsumer;

/**
 * A Spliterator.OfInt for sources that traverse and split elements
 * maintained in a CharBuffer.
 *
 * @implNote
 * The implementation is based on the code for the Array-based spliterators.
 */
class CharBufferSpliterator implements Spliterator.OfInt {
    private final CharBuffer buffer;
    private int index;   // current index, modified on advance/split
    private final int limit;

    CharBufferSpliterator(CharBuffer buffer) {
        this(buffer, buffer.position(), buffer.limit());
    }

    CharBufferSpliterator(CharBuffer buffer, int origin, int limit) {
        assert origin <= limit;
        this.buffer = buffer;
        this.index = (origin <= limit) ? origin : limit;
        this.limit = limit;
    }

    @Override
    public OfInt trySplit() {
        int lo = index, mid = (lo + limit) >>> 1;
        return (lo >= mid)
               ? null
               : new CharBufferSpliterator(buffer, lo, index = mid);
    }

    @Override
    public void forEachRemaining(IntConsumer action) {
        if (action == null)
            throw new NullPointerException();
        CharBuffer cb = buffer;
        int i = index;
        int hi = limit;
        index = hi;
        while (i < hi) {
            action.accept(cb.getUnchecked(i++));
        }
    }

    @Override
    public boolean tryAdvance(IntConsumer action) {
        if (action == null)
            throw new NullPointerException();
        if (index >= 0 && index < limit) {
            action.accept(buffer.getUnchecked(index++));
            return true;
        }
        return false;
    }

    @Override
    public long estimateSize() {
        return (long)(limit - index);
    }

    @Override
    public int characteristics() {
        return Buffer.SPLITERATOR_CHARACTERISTICS;
    }
}
        """.trimIndent()
        val goal = "CompilationUnit"

        val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(1, result.sppt!!.maxNumHeads)

        val resultStr = result.sppt!!.asSentence
        assertEquals(sentence, resultStr)
    }

    @Test(timeout = 5000)
    fun Statement__ifthenelse() {

        val sentence = """
        if (action == null)
            throw new NullPointerException();
        """.trimIndent()
        val goal = "Statement"

        val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(2, result.sppt!!.maxNumHeads)

        val resultStr = result.sppt!!.asSentence
        assertEquals(sentence, resultStr)
    }

    @Test(timeout = 20000)
    fun CompilationUnit__fromStdLib_CharBufferSpliterator_part() {

        val sentence = """
class A {
    public void tryAdvance() {
        if (index >= 0 && index < limit) {
        }
    }
}
        """.trimIndent()
        val goal = "CompilationUnit"

        val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertTrue(result.issues.errors.isEmpty(), result.issues.errors.toString())
        assertNotNull(result.sppt)
        assertEquals(3, result.sppt!!.maxNumHeads)

        val resultStr = result.sppt!!.asSentence
        assertEquals(sentence, resultStr)
    }

    @Test(timeout = 5000)
    fun BlockStatements__ifthenelse() {

        val sentence = """
            {
        if (action == null)
            throw new NullPointerException();
        CharBuffer cb = buffer;
        int i = index;
        int hi = limit;
            }
        """.trimIndent()
        val goal = "Statement"

        val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(2, result.sppt!!.maxNumHeads)

        val resultStr = result.sppt!!.asSentence
        assertEquals(sentence, resultStr)
    }

    @Test(timeout = 5000)
    fun Navigations__fromStdLib_CharBufferSpliterator_1() {

        val sentence = """
            action.accept(cb.getUnchecked(i++))
        """.trimIndent()
        val goal = "Navigations"

        val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(1, result.sppt!!.maxNumHeads)

        val resultStr = result.sppt!!.asSentence
        assertEquals(sentence, resultStr)
    }

    @Test(timeout = 5000)
    fun MethodInvocation__fromStdLib_CharBufferSpliterator_2() {

        val sentence = """
            accept(cb.getUnchecked(i++))
        """.trimIndent()
        val goal = "MethodInvocation"

        val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(1, result.sppt!!.maxNumHeads)

        val resultStr = result.sppt!!.asSentence
        assertEquals(sentence, resultStr)
    }

    @Test(timeout = 5000)
    fun Navigations__fromStdLib_CharBufferSpliterator_3() {

        val sentence = """
            cb.getUnchecked(i++)
        """.trimIndent()
        val goal = "Navigations"

        val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(1, result.sppt!!.maxNumHeads)

        val resultStr = result.sppt!!.asSentence
        assertEquals(sentence, resultStr)
    }

    @Test(timeout = 5000)
    fun Navigations__fromStdLib_CharBufferSpliterator_3b() {

        val sentence = """
            cb.getUnchecked(i)
        """.trimIndent()
        val goal = "Navigations"

        val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(1, result.sppt!!.maxNumHeads)

        val resultStr = result.sppt!!.asSentence
        assertEquals(sentence, resultStr)
    }

    @Test(timeout = 5000)
    fun NavigableExpression__fromStdLib_CharBufferSpliterator_4() {

        val sentence = """
            getUnchecked(i++)
        """.trimIndent()
        val goal = "NavigableExpression"

        val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(1, result.sppt!!.maxNumHeads)

        val resultStr = result.sppt!!.asSentence
        assertEquals(sentence, resultStr)
    }

    @Test(timeout = 5000)
    fun NavigableExpression__fromStdLib_CharBufferSpliterator_4b() {

        val sentence = """
            getUnchecked(i)
        """.trimIndent()
        val goal = "NavigableExpression"

        val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(1, result.sppt!!.maxNumHeads)

        val resultStr = result.sppt!!.asSentence
        assertEquals(sentence, resultStr)
    }

    @Test(timeout = 5000)
    fun GenericMethodInvocation__fromStdLib_CharBufferSpliterator_5() {

        val sentence = """
            getUnchecked(i++)
        """.trimIndent()
        val goal = "MethodInvocation"

        val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(1, result.sppt!!.maxNumHeads)

        val resultStr = result.sppt!!.asSentence
        assertEquals(sentence, resultStr)
    }

    @Test
    fun withNoWhitespace() {
        val sentence = "classclass{voidvoid(){}}"
        val result = proc.parse(sentence)
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun Type() {
        val sentences = listOf(
            "int", "int[]", "int@An[]", "int@An[]@An[]@An[]",
            "A", "A.B.C", "A", "A.@An B.@An C",
            "A<>", "A<?>", "A<? extends B>"
        )
        val goal = "TypeReference"
        for (sentence in sentences) {
            println("sentence = '$sentence'")
            val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
            assertTrue(result.issues.notWidth.isEmpty(), result.issues.toString())
            assertNotNull(result.sppt)
            assertTrue(2 >= result.sppt!!.maxNumHeads)
        }
    }

    @Test(timeout = 5000)
    fun Expression__InfixAdditive() {
        val sentence = "1+2+3+4"
        val goal = "Expression"
        val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test(timeout = 5000)
    fun Expression__InfixMultiplicative() {
        val sentences = listOf("1/2", "1/2/3/4", "1*2*3*4", "1/2*3/4*5", "1%2/3*4")
        val goal = "Expression"
        for (sentence in sentences) {
            println("sentence = '$sentence'")
            val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
            assertTrue(result.issues.isEmpty(), result.issues.toString())
            assertNotNull(result.sppt)
            assertTrue(1 >= result.sppt!!.maxNumHeads, "number of heads = ${result.sppt!!.maxNumHeads}")
        }
    }

    @Test(timeout = 5000)
    fun Expression__precedence() {
        val sentences = listOf("a*b+c")
        val goal = "Expression"
        for (sentence in sentences) {
            println("sentence = '$sentence'")
            val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
            assertTrue(result.issues.isEmpty(), result.issues.toString())
            assertNotNull(result.sppt)
            println(result.sppt!!.toStringAll)
            assertTrue(1 >= result.sppt!!.maxNumHeads, "number of heads = ${result.sppt!!.maxNumHeads}")
        }
    }

    @Test(timeout = 5000)
    fun Expression__Prefix() {
        val sentences = listOf("+1", "-1", "++1", "--1")
        val goal = "Expression"
        for (sentence in sentences) {
            val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
            assertTrue(result.issues.isEmpty(), result.issues.toString())
            assertNotNull(result.sppt)
            assertEquals(1, result.sppt!!.maxNumHeads)
        }
    }

    @Test
    fun Expression__Primary() {
        val sentences = listOf("(A)", "A.class", "A", "int", "a", "1", "this", "super", "(1)")
        val goal = "Expression"
        for (sentence in sentences) {
            println("sentence = '$sentence'")
            val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
            assertNotNull(result.sppt)
            println(result.sppt!!.toStringAll)
            assertTrue(result.issues.notWidth.isEmpty(), result.issues.toString())
            assertTrue(2 >= result.sppt!!.maxNumHeads, "number of heads = ${result.sppt!!.maxNumHeads}")
        }
    }

    @Test(timeout = 5000)
    fun CompilationUnit__constructor() {
        val sentence = """
package com.test;

public class Test${"$"}Test {

    public Test${"$"}Test(double value) {
    }

}
        """
        val goal = "CompilationUnit"
        val result = proc.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val resultStr = result.sppt!!.asSentence
        assertEquals(sentence, resultStr)
    }

}