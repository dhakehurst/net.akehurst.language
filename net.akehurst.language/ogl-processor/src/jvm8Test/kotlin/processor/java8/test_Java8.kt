package new.akehurst.language.processor

import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.processor.Ogl
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(Parameterized::class)
class test_Java8(val data: Data) {

    companion object {

        var java8Processor: LanguageProcessor = createJava8Processor()

        fun createJava8Processor() : LanguageProcessor {
            val grammarStr = this::class.java.getResource("/Java8_all.ogl").readText()
            val proc = Ogl.processor(grammarStr)
            proc.build()
            return proc
        }

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> {
            val col = ArrayList<Array<Any>>()

            col.add(arrayOf(Data(4,"", "DecimalIntegerLiteral", "12")))
            col.add(arrayOf(Data(5, "", "IntegerLiteral", "12345")))
            col.add(arrayOf(Data(11, "", "compilationUnit", "")))
            col.add(arrayOf(Data(103, "", "ifThenStatement", "if(i==1) return 1;")))
            col.add(arrayOf(Data(9, "", "annotation", "@AnAnnotation")))
            col.add(arrayOf(Data(43, "", "annotation", "@AnAnnotation(1)")))
            col.add(arrayOf(Data(22, "", "annotation", "@AnAnnotation(@AnAnnotation2)")))
            col.add(arrayOf(Data(114, "", "annotation", "@CompilerAnnotationTest(@CompilerAnnotationTest2(name=\"test\",name2=\"test2\"))")))
            col.add(arrayOf(Data(115, "", "interfaceModifier", "@CompilerAnnotationTest(@CompilerAnnotationTest2(name=\"test\",name2=\"test2\"))")))
            col.add(arrayOf(Data(144, "", "annotationTypeDeclaration", "@CAT(@CAT2(name=\"test\",name2=\"test2\")) @interface CAT { }")))
            col.add(arrayOf(Data(187, "", "annotationTypeDeclaration", "@CAT(@CAT2(name=\"test\",name2=\"test2\")) @interface CAT { CAT2[] value(); }")))
            col.add(arrayOf(Data(188, "", "interfaceDeclaration", "@CAT(@CAT2(name=\"test\",name2=\"test2\")) @interface CAT { CAT2[] value(); }")))
            col.add(arrayOf(Data(189, "", "typeDeclaration", "@CAT(@CAT2(name=\"test\",name2=\"test2\")) @interface CAT { CAT2[] value(); }")))
            col.add(arrayOf(Data(45, "", "compilationUnit", "interface An {  }")))
            col.add(arrayOf(Data(45, "", "typeDeclaration", "@An interface An {  }")))
            col.add(arrayOf(Data(49, "", "compilationUnit", "@An interface An {  }")))
            col.add(arrayOf(Data(16, "", "annotation", "@An()")))
            col.add(arrayOf(Data(35, "", "typeDeclaration", "interface An {  }")))
            col.add(arrayOf(Data(52, "", "typeDeclaration", "@An() interface An {  }")))
            col.add(arrayOf(Data(62, "", "compilationUnit", "@An() interface An {  }")))
            col.add(arrayOf(Data(65, "", "compilationUnit", "@An() class An {  }")))
            col.add(arrayOf(Data(76, "", "compilationUnit", "import x; @An() interface An {  }")))
            col.add(arrayOf(Data(68, "", "compilationUnit", "@An(@An) interface An {  }")))
            col.add(arrayOf(Data(96, "", "compilationUnit", "interface An { An[] value(); }")))
            col.add(arrayOf(Data(119, "", "compilationUnit", "@An(@An) interface An { An[] value(); }")))
            col.add(arrayOf(Data(107, "", "compilationUnit", "@An(@An) @interface An { An[] value(); }")))
            col.add(arrayOf(Data(193, "", "compilationUnit", "@An(@An(name=\"test\",name2=\"test2\")) @interface An { An[] value(); }")))
            col.add(arrayOf(Data(215, "", "compilationUnit", "package x; @CAT(@CAT2(name=\"test\",name2=\"test2\")) @interface CAT { CAT2[] value(); }")))
            col.add(arrayOf(Data(100, "", "compilationUnit", "package x; @interface CAT { CAT2[] value(); }")))
            col.add(arrayOf(Data(305, "", "compilationUnit", "public class ConstructorAccess { class Inner { private Inner() { if (x.i != 42 || x.c != 'x') { } } } }")))
            col.add(arrayOf(Data(0, "", "block", "{ (a)=(b)=1; }")))
            col.add(arrayOf(Data(0, "", "block", "{ (a) = (b) = 1; }")))
            col.add(arrayOf(Data(83, "", "block", "{ ls.add(\"Smalltalk rules!\"); }")))
            col.add(arrayOf(Data(243, "", "block", "{ this.j = i; this.b = true; this.c = c; ConstructorAccess.this.i = i; }")))

            col.add(arrayOf(Data(966,"'many ifthen'", "compilationUnit", {
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

    @Test
    fun test() {
        try {
            val queryStr = this.data.sentence
            val grammarRule = this.data.grammarRule
            val tree = java8Processor.parse(grammarRule, queryStr)
            assertNotNull(tree)
            val resultStr = clean(tree.asString)
            assertEquals(queryStr, resultStr)

            assertEquals(this.data.expectedSeasons, tree.seasons)

        } catch (ex: ParseFailedException) {
            println(ex.message)
            println(ex.longestMatch)
            throw ex
        }

    }

}