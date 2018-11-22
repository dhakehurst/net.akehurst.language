package processor.java8

import net.akehurst.language.processor.processor
import kotlin.test.Test

class test_Java8_parts {



    @Test
    fun t1() {
        val grammarStr = """
compilationUnit
	=	packageDeclaration? importDeclaration* typeDeclaration*
	;

packageDeclaration
	=	packageModifier* 'package' packageName ';'
	;
packageModifier
	=	annotation
	;

importDeclaration
	=	singleTypeImportDeclaration
	|	typeImportOnDemandDeclaration
	|	singleStaticImportDeclaration
	|	staticImportOnDemandDeclaration
	;

        """.trimIndent()

        val sentence = "interface An { An[] value(); }"
        val goal = "compliationUnit"

        val p = processor(grammarStr)
        p.parse(goal, sentence)
    }

}