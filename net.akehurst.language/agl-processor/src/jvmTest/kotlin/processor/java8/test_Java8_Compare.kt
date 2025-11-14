/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.processor.contextFromGrammarRegistry
import net.akehurst.language.agl.simple.SentenceContextAny
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.grammar.processor.AglGrammarSemanticAnalyser
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import std.extensions.capitalise
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_Java8_Compare {

    class Data(
        val title: String?,
        val grammarRule: String,
        val sentence: String
    ) {

        constructor(title: String, grammarRule: String, sentence: () -> String)
                : this(title, grammarRule, sentence.invoke()) {
        }

        // --- Object ---
        override fun toString(): String {
            return if (null == this.title || this.title.isEmpty()) {
                this.grammarRule + " : " + this.sentence
            } else {
                this.grammarRule + " : " + this.title
            }
        }
    }

    companion object {

        var aglSpecProcessor: LanguageProcessor<Asm, SentenceContextAny> = createJava8Processor("/Java/version_8/grammars/grammar_aglSpec.agl", true)
        var aglOptmProcessor: LanguageProcessor<Asm, SentenceContextAny> = createJava8Processor("/Java/version_8/grammars/grammar_aglOptm.agl", true)

        var antlrSpecProcessor: LanguageProcessor<Asm, SentenceContextAny> = createJava8Processor("/Java/version_8/grammars/grammar_antlrSpec.agl")
        var antlrOptmProcessor: LanguageProcessor<Asm, SentenceContextAny> = createJava8Processor("/Java/version_8/grammars/grammar_antlrOptm.agl")

        fun createJava8Processor(path: String, toUpper: Boolean = false): LanguageProcessor<Asm, SentenceContextAny> {
            println("Building $path")
            val grammarStr = this::class.java.getResource(path).readText()
            val proc = Agl.processorFromString<Asm, SentenceContextAny>(
                grammarDefinitionStr = grammarStr,
                aglOptions = Agl.options {
                    semanticAnalysis {
                        context(contextFromGrammarRegistry(Agl.registry))
                        // switch off ambiguity analysis for performance
                        option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, false)
                    }
                }
            ).processor!!
            val forRule = if (toUpper) "CompilationUnit" else "compilationUnit"
            //proc.buildFor(Agl.parseOptions { goalRuleName(forRule) })//TODO: use build
            println("Built $path")
            return proc
        }

        @JvmStatic
        fun data(): Collection<Array<Any>> {
            val col = ArrayList<Array<Any>>()

            col.add(arrayOf(Data("", "literal", "12")))
            col.add(arrayOf(Data("", "literal", "12345")))
            col.add(arrayOf(Data("", "compilationUnit", "")))
            col.add(arrayOf(Data("", "statement", "if(i==1) return 1;")))
            col.add(arrayOf(Data("", "annotation", "@AnAnnotation")))
            col.add(arrayOf(Data("", "annotation", "@AnAnnotation(1)")))
            col.add(arrayOf(Data("", "annotation", "@AnAnnotation(@AnAnnotation2)")))
            col.add(arrayOf(Data("", "annotation", "@CompilerAnnotationTest(@CompilerAnnotationTest2(name=\"test\",name2=\"test2\"))")))
            col.add(arrayOf(Data("", "typeDeclaration", "@CAT(@CAT2(name=\"test\",name2=\"test2\")) @interface CAT { }")))
            col.add(arrayOf(Data("", "typeDeclaration", "@CAT(@CAT2(name=\"test\",name2=\"test2\")) @interface CAT { CAT2[] value(); }")))
            col.add(arrayOf(Data("", "compilationUnit", "interface Intf {  }")))
            col.add(arrayOf(Data("", "typeDeclaration", "@An interface Intf {  }")))
            col.add(arrayOf(Data("", "compilationUnit", "@An interface Intf {  }")))
            col.add(arrayOf(Data("", "annotation", "@An()")))
            col.add(arrayOf(Data("", "typeDeclaration", "interface Intf {  }")))
            col.add(arrayOf(Data("", "typeDeclaration", "@An() interface Intf {  }")))
            col.add(arrayOf(Data("", "compilationUnit", "@An() interface Intf {  }")))
            col.add(arrayOf(Data("", "compilationUnit", "@An() class Cls {  }")))
            col.add(arrayOf(Data("", "compilationUnit", "import x; @An() interface Intf {  }")))
            col.add(arrayOf(Data("", "compilationUnit", "@An1(@An2) interface Intf {  }")))
            col.add(arrayOf(Data("", "compilationUnit", "package p; @An1(@An2) interface Intf {  }")))
            col.add(arrayOf(Data("", "compilationUnit", "interface Intf { An[] value(); }")))
            col.add(arrayOf(Data("", "compilationUnit", "@An1(@An2) interface Intf { An[] value(); }")))
            col.add(arrayOf(Data("", "compilationUnit", "@An1(@An2) @interface Intf { An[] value(); }")))
            col.add(arrayOf(Data("", "compilationUnit", """@An1(@An2(name="test",name2="test2")) @interface An { An[] value(); }""")))
            col.add(arrayOf(Data("", "compilationUnit", """package x; @CAT(@CAT2(name="test",name2="test2")) @interface CAT { CAT2[] value(); }""")))
            col.add(arrayOf(Data("", "compilationUnit", "package x; @interface CAT { CAT2[] value(); }")))
            col.add(arrayOf(Data("", "compilationUnit", "public class ConstructorAccess { class Inner { private Inner() { if (x.i != 42 || x.c != 'x') { } } } }")))
            col.add(arrayOf(Data("", "block", "{ Map.@An Entry<Object,Object> x; }")))
            col.add(arrayOf(Data("", "block", "{ (a)=(b)=1; }")))
            col.add(arrayOf(Data("", "block", "{ (a) = (b) = 1; }")))
            col.add(arrayOf(Data("", "block", "{ ls.add(\"Smalltalk rules!\"); }")))
            col.add(arrayOf(Data("", "block", "{ this.j = i; this.b = true; this.c = c; ConstructorAccess.this.i = i; }")))
            col.add(arrayOf(Data("", "expression", "args[0]")))

            col.add(arrayOf(Data("'many ifthen'", "compilationUnit") {
                var input = "class Test {"
                input += "void test() {"
                for (i in 0..9) {
                    input += "  if($i) return $i;"
                }
                input += "}"
                input += "}"
                input
            }))


            return col
        }
    }

    private fun clean(str: String): String {
        val eol: String = StringBuilder().appendLine().toString();
        var res = str.replace(eol, " ")
        res = res.trim { it <= ' ' }
        return res
    }

    private fun testParse(data: Data, proc: LanguageProcessor<Asm, SentenceContextAny>, toUpper: Boolean = false) {
        val queryStr = data.sentence
        val grammarRule = if (toUpper) data.grammarRule.capitalise else data.grammarRule
        val result = proc.parse(queryStr, Agl.parseOptions { goalRuleName(grammarRule) })
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        val resultStr = clean(result.sppt!!.asSentence)
        assertEquals(queryStr, resultStr)
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("data")
    @Timeout(5)
    fun aglSpec(data: Data) {
        this.testParse(data,aglSpecProcessor, true)
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("data")
    @Timeout(5)
    fun aglOptm(data: Data) {
        this.testParse(data,aglOptmProcessor, true)
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("data")
    @Timeout(5)
    fun antlrSpec(data: Data) {
        this.testParse(data,antlrSpecProcessor)
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("data")
    @Timeout(5)
    fun antlrOptm(data: Data) {
        this.testParse(data,antlrOptmProcessor)
    }
}