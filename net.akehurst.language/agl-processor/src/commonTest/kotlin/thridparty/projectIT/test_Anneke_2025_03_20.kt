package thridparty.projectIT

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.GrammarString
import kotlin.test.Test

class test_Anneke_2025_03_20 {

    private companion object {
        val grammarStr = $$"""
            namespace test
            grammar Test {
                skip WS = "\s+" ;
                WithDirectRefs =
                 'WithDirectRefs' '{'
                    'ref' __fre_reference
                     ('optRef' __fre_reference)?
                    'refList' refList
                     ('optRefList' refList)?
                    '}'
                 ;
                 refList = __fre_reference* ';' ;
                 
                 __fre_reference = [ identifier / '.' ]+ ;
                 leaf identifier = "[a-zA-Z0-9]+" ;
            }
        """
        val proc = Agl.processorFromStringSimple(
            GrammarString(grammarStr),
        ).let {
            check(it.issues.errors.isEmpty())
            it.processor!!
        }
    }

    @Test
    fun parse1() {
        val sentence = """
            WithDirectRefs { ref someName refList name1 }            
        """.trimIndent()

        val actual = proc.parse(sentence).let {
            check(it.issues.errors.isEmpty())
            it.sppt!!
        }

        println(actual.toStringAll)
    }

    @Test
    fun parse2() {
        val sentence = """
            WithDirectRefs {
                ref someName
                refList name1 ;
                optRefList name2 ;
            }            
        """.trimIndent()

        val actual = proc.parse(sentence).let {
            check(it.issues.errors.isEmpty())
            it.sppt!!
        }

        println(actual.toStringAll)
    }

    @Test
    fun parse() {
        val sentence = """
            WithDirectRefs {
                ref someName
                optRef nameAB
                refList
                    name1
                    name2
                    name3
                    name4
                optRefList nameZ
                nameY
                nameX
            }            
        """.trimIndent()

        val actual = proc.parse(sentence).let {
            check(it.issues.errors.isEmpty())
            it.sppt!!
        }

        println(actual.toStringAll)
    }

    @Test
    fun process1() {
        val sentence = """
            WithDirectRefs {
                ref someName
                refList name1
            }            
        """

        val actual = proc.process(sentence).let {
            check(it.issues.errors.isEmpty())
            it.asm!!
        }

        println(actual.asString())
    }

    @Test
    fun process() {
        val sentence = """
            WithDirectRefs {
                ref someName
                optRef nameAB
                refList
                    name1
                    name2
                    name3
                    name4
                optRefList nameZ
                nameY
                nameX
            }            
        """

        val actual = proc.process(sentence).let {
            check(it.issues.errors.isEmpty())
            it.asm!!
        }

        println(actual.asString())
    }

}