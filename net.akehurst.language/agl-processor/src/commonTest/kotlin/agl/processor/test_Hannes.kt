/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package agl.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.processor.test_ProcessorAbstract
import net.akehurst.language.api.processor.GrammarString
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

        val processor = Agl.processorFromStringSimple(GrammarString(grammarStr)).processor!!
    }

    @Test
    fun f() {
        val text = "123"

        val expected = """
             S { INT : '123' }
        """.trimIndent()

        super.test(processor, "S", text, expected)
    }

}