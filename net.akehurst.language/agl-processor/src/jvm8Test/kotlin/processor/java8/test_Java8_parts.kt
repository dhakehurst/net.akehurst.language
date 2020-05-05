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

import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.agl.processor.Agl
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(Parameterized::class)
class test_Java8_parts(val data: Data) {

    companion object {

        var standardProcessor: LanguageProcessor = createJava8Processor("/java8/Java8_all.agl")

        fun createJava8Processor(path:String) : LanguageProcessor {
//            val grammarStr = this::class.java.getResource("/java8/Java8_all.agl").readText()
            val grammarStr = this::class.java.getResource(path).readText()
            val proc = Agl.processor(grammarStr)
            proc.build()
            return proc
        }

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> {
            val col = ArrayList<Array<Any>>()

            col.add(arrayOf(Data(7,"", "DecimalIntegerLiteral", "12")))
            col.add(arrayOf(Data(8, "", "IntegerLiteral", "12345")))
            col.add(arrayOf(Data(14, "", "compilationUnit", "")))
            col.add(arrayOf(Data(116, "", "ifThenStatement", "if(i==1) return 1;")))
            col.add(arrayOf(Data(12, "", "annotation", "@AnAnnotation")))
            col.add(arrayOf(Data(50, "", "annotation", "@AnAnnotation(1)")))
            col.add(arrayOf(Data(25, "", "annotation", "@AnAnnotation(@AnAnnotation2)")))
            col.add(arrayOf(Data(126, "", "annotation", "@CompilerAnnotationTest(@CompilerAnnotationTest2(name=\"test\",name2=\"test2\"))")))
            col.add(arrayOf(Data(127, "", "interfaceModifier", "@CompilerAnnotationTest(@CompilerAnnotationTest2(name=\"test\",name2=\"test2\"))")))
            col.add(arrayOf(Data(156, "", "annotationTypeDeclaration", "@CAT(@CAT2(name=\"test\",name2=\"test2\")) @interface CAT { }")))
            col.add(arrayOf(Data(199, "", "annotationTypeDeclaration", "@CAT(@CAT2(name=\"test\",name2=\"test2\")) @interface CAT { CAT2[] value(); }")))
            col.add(arrayOf(Data(200, "", "interfaceDeclaration", "@CAT(@CAT2(name=\"test\",name2=\"test2\")) @interface CAT { CAT2[] value(); }")))
            col.add(arrayOf(Data(201, "", "typeDeclaration", "@CAT(@CAT2(name=\"test\",name2=\"test2\")) @interface CAT { CAT2[] value(); }")))
            col.add(arrayOf(Data(46, "", "compilationUnit", "interface An {  }")))
            col.add(arrayOf(Data(48, "", "typeDeclaration", "@An interface An {  }")))
            col.add(arrayOf(Data(50, "", "compilationUnit", "@An interface An {  }")))
            col.add(arrayOf(Data(19, "", "annotation", "@An()")))
            col.add(arrayOf(Data(38, "", "typeDeclaration", "interface An {  }")))
            col.add(arrayOf(Data(55, "", "typeDeclaration", "@An() interface An {  }")))
            col.add(arrayOf(Data(63, "", "compilationUnit", "@An() interface An {  }")))
            col.add(arrayOf(Data(66, "", "compilationUnit", "@An() class An {  }")))
            col.add(arrayOf(Data(77, "", "compilationUnit", "import x; @An() interface An {  }")))
            col.add(arrayOf(Data(63, "", "compilationUnit", "@An(@An) interface An {  }")))
            col.add(arrayOf(Data(97, "", "compilationUnit", "interface An { An[] value(); }")))
            col.add(arrayOf(Data(114, "", "compilationUnit", "@An(@An) interface An { An[] value(); }")))
            col.add(arrayOf(Data(102, "", "compilationUnit", "@An(@An) @interface An { An[] value(); }")))
            col.add(arrayOf(Data(203, "", "compilationUnit", "@An(@An(name=\"test\",name2=\"test2\")) @interface An { An[] value(); }")))
            col.add(arrayOf(Data(225, "", "compilationUnit", "package x; @CAT(@CAT2(name=\"test\",name2=\"test2\")) @interface CAT { CAT2[] value(); }")))
            col.add(arrayOf(Data(101, "", "compilationUnit", "package x; @interface CAT { CAT2[] value(); }")))
            col.add(arrayOf(Data(305, "", "compilationUnit", "public class ConstructorAccess { class Inner { private Inner() { if (x.i != 42 || x.c != 'x') { } } } }")))
            col.add(arrayOf(Data(0, "", "block", "{ (a)=(b)=1; }")))
            col.add(arrayOf(Data(0, "", "block", "{ (a) = (b) = 1; }")))
            col.add(arrayOf(Data(90, "", "block", "{ ls.add(\"Smalltalk rules!\"); }")))
            col.add(arrayOf(Data(254, "", "block", "{ this.j = i; this.b = true; this.c = c; ConstructorAccess.this.i = i; }")))

            col.add(arrayOf(Data(1047,"'many ifthen'", "compilationUnit", {
                var input = "class Test {"
                input += "void test() {"
                for (i in 0..9) {
                    input += "  if($i) return $i;"
                }
                input += "}"
                input += "}"
                input
            })))

            return col
        }
    }

    private fun clean(str: String): String {
        val eol: String = StringBuilder().appendln().toString();
        var res = str.replace(eol, " ")
        res = res.trim { it <= ' ' }
        return res
    }

    class Data(
            val expectedSeasons: Int,
            val title: String?,
            val grammarRule: String,
            val sentence: String
    ) {

        constructor(expectedSeasons: Int, title: String, grammarRule: String, sentence: ()->String)
        : this(expectedSeasons, title, grammarRule, sentence.invoke())
        {
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

    @Test(timeout=5000)
    fun standard() {
        try {
            val queryStr = this.data.sentence
            val grammarRule = this.data.grammarRule
            val tree = standardProcessor.parse(grammarRule, queryStr)
            assertNotNull(tree)
            val resultStr = clean(tree.asString)
            assertEquals(queryStr, resultStr)

            assertEquals(this.data.expectedSeasons, tree.seasons)

        } catch (ex: ParseFailedException) {
            println(ex.message)
            println(ex.longestMatch?.toStringAll)
            throw ex
        }

    }


}