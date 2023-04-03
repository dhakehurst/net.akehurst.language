package agl.processor

import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.agl.processor.test_ProcessorAbstract
import kotlin.test.Test

class test_Hannes : test_ProcessorAbstract() {

    companion object {
        val grammarStr = """
            namespace hannes
            grammar Hannes {

                S = IDCHAR_SEQ | INT ;

                leaf IDCHAR_SEQ = (('0' | DECIMAL) IDCHAR_FIRST_SEQ)+ ;
                leaf IDCHAR_FIRST_SEQ = IDCHAR_FIRST (IDCHAR_FIRST | '0' | DECIMAL | IDCHAR_ESCAPED)*;
                leaf IDCHAR_FIRST = '#' | ';' | '@' | "[A-Z]" | '_' | "[a-z]" | "[\u0080-\uffff]" ;
                leaf DECIMAL = "[1-9]" "[0-9]"* ;
                leaf IDCHAR_ESCAPED = '\\' ('\\' | '{' | '}' | '"' | "[a-z]") ;
    
                leaf INT
                    = (DECIMAL // decimal without leading 0 
                       | ('0' ("[0-7]")*) // octal
                       | ('0' ('x' | 'X') (("[0-9]") | ("[a-f]") | ("[A-F]"))+) // hexadecimal
                       | ('0' ('b' | 'B') ('0' | '1')+) // binary
                       )
                       INT_SUFFIX?
                    ;
                leaf INT_SUFFIX = ((('u' | 'U') ('l' | 'L')?) | (('l' | 'L') ('u' | 'U')?))?;
            }
            
        """.trimIndent()

        val processor = Agl.processorFromString<Any,Any>(grammarStr).processor!!
    }

    @Test
    fun f() {
        val text = "123"

        val expected = """
             S|1 { INT : '123' }
        """.trimIndent()

        super.test(processor,"S", text, expected)
    }

}