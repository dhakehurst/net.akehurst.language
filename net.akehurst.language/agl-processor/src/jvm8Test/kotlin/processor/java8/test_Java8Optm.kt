package net.akehurst.language.processor

import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.processor.Agl
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(Parameterized::class)
class test_Java8Optm(val data: Data) {

    companion object {

        var standardProcessor: LanguageProcessor = createJava8Processor("/java8/Java8_all.agl")
        var antlrOptmProcessor: LanguageProcessor = createJava8Processor("/java8/Java8OptmAntlr.agl")
        var aglOptmProcessor: LanguageProcessor = createJava8Processor("/java8/Java8OptmAgl.agl")

        fun createJava8Processor(path: String): LanguageProcessor {
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

            col.add(arrayOf(Data("", "literal", "12")))
            col.add(arrayOf(Data("", "literal", "12345")))
            col.add(arrayOf(Data("", "compilationUnit", "")))
            col.add(arrayOf(Data("", "statement", "if(i==1) return 1;")))
            col.add(arrayOf(Data("", "annotation", "@AnAnnotation")))
            col.add(arrayOf(Data("", "annotation", "@AnAnnotation(1)")))
            col.add(arrayOf(Data("", "annotation", "@AnAnnotation(@AnAnnotation2)")))
            col.add(arrayOf(Data("", "annotation", "@CompilerAnnotationTest(@CompilerAnnotationTest2(name=\"test\",name2=\"test2\"))")))
            col.add(arrayOf(Data("", "typeDeclaration", "@CAT(@CAT2(name=\"test\",name2=\"test2\")) @interface CAT { CAT2[] value(); }")))
            col.add(arrayOf(Data("", "compilationUnit", "interface An {  }")))
            col.add(arrayOf(Data("", "typeDeclaration", "@An interface An {  }")))
            col.add(arrayOf(Data("", "compilationUnit", "@An interface An {  }")))
            col.add(arrayOf(Data("", "annotation", "@An()")))
            col.add(arrayOf(Data("", "typeDeclaration", "interface An {  }")))
            col.add(arrayOf(Data("", "typeDeclaration", "@An() interface An {  }")))
            col.add(arrayOf(Data("", "compilationUnit", "@An() interface An {  }")))
            col.add(arrayOf(Data("", "compilationUnit", "@An() class An {  }")))
            col.add(arrayOf(Data("", "compilationUnit", "import x; @An() interface An {  }")))
            col.add(arrayOf(Data("", "compilationUnit", "@An(@An) interface An {  }")))
            col.add(arrayOf(Data("", "compilationUnit", "interface An { An[] value(); }")))
            col.add(arrayOf(Data("", "compilationUnit", "@An(@An) interface An { An[] value(); }")))
            col.add(arrayOf(Data("", "compilationUnit", "@An(@An) @interface An { An[] value(); }")))
            col.add(arrayOf(Data("", "compilationUnit", "@An(@An(name=\"test\",name2=\"test2\")) @interface An { An[] value(); }")))
            col.add(arrayOf(Data("", "compilationUnit", "package x; @CAT(@CAT2(name=\"test\",name2=\"test2\")) @interface CAT { CAT2[] value(); }")))
            col.add(arrayOf(Data("", "compilationUnit", "package x; @interface CAT { CAT2[] value(); }")))
            col.add(arrayOf(Data("", "compilationUnit", "public class ConstructorAccess { class Inner { private Inner() { if (x.i != 42 || x.c != 'x') { } } } }")))
            col.add(arrayOf(Data("", "block", "{ (a)=(b)=1; }")))
            col.add(arrayOf(Data("", "block", "{ (a) = (b) = 1; }")))
            col.add(arrayOf(Data("", "block", "{ ls.add(\"Smalltalk rules!\"); }")))
            col.add(arrayOf(Data("", "block", "{ this.j = i; this.b = true; this.c = c; ConstructorAccess.this.i = i; }")))

            col.add(arrayOf(Data( "'many ifthen'", "compilationUnit", {
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
            val title: String?,
            val grammarRule: String,
            val sentence: String
    ) {

        constructor( title: String, grammarRule: String, sentence: () -> String)
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

    @Test(timeout = 5000)
    fun standard() {
        try {
            val queryStr = this.data.sentence
            val grammarRule = this.data.grammarRule
            val tree = standardProcessor.parse(grammarRule, queryStr)
            assertNotNull(tree)
            val resultStr = clean(tree.asString)
            assertEquals(queryStr, resultStr)
        } catch (ex: ParseFailedException) {
            println(ex.message)
            println(ex.longestMatch)
            throw ex
        }

    }

    @Test(timeout = 5000)
    fun antlr() {
        try {
            val queryStr = this.data.sentence
            val grammarRule = this.data.grammarRule
            val tree = antlrOptmProcessor.parse(grammarRule, queryStr)
            assertNotNull(tree)
            val resultStr = clean(tree.asString)
            assertEquals(queryStr, resultStr)
        } catch (ex: ParseFailedException) {
            println(ex.message)
            println(ex.longestMatch)
            throw ex
        }

    }

    @Test(timeout = 5000)
    fun agl() {
        try {
            val queryStr = this.data.sentence
            val grammarRule = this.data.grammarRule
            val tree = aglOptmProcessor.parse(grammarRule, queryStr)
            assertNotNull(tree)
            val resultStr = clean(tree.asString)
            assertEquals(queryStr, resultStr)
        } catch (ex: ParseFailedException) {
            println(ex.message)
            println(ex.longestMatch)
            throw ex
        }

    }

}