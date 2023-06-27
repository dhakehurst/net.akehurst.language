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

package thridparty.projectIT

import net.akehurst.language.agl.processor.Agl
import kotlin.test.Test

class test_ExpressionLibraryGrammar {

    private companion object {

        val grammarStr = """
        namespace ExpressionLibraryLanguage
        grammar ExpressionLibraryGrammar {
                            
            // rules for "LibUnit"
            LibUnit = identifier 'expressions:' ( ExpressionBase ';' )* ;
            GroupedExpression = '(' ExpressionBase ')' ;
            BooleanLiteral = booleanLiteral ;
            NumberLiteral = numberLiteral ;
            StringLiteral = stringLiteral ;
            NotExpression = 'not' ExpressionBase ;
            AbsExpression = '|' ExpressionBase '|' ;
            PrefixMinusExpression = '-' ExpressionBase ;
            ExpressionBase =  
                GroupedExpression
                | NotExpression 
                | AbsExpression 
                | PrefixMinusExpression 
                | BooleanLiteral 
                | NumberLiteral 
                | StringLiteral 
                | __pi_binary_expression ; 
            
            __pi_binary_expression = [ExpressionBase / __pi_binary_operator]2+ ;
            leaf __pi_binary_operator = '==>' | '==' | '!=' | '<=' | '>=' | 'and' | 'or' | '<' | '>' | '*' | '+' | '-' | '/' ;
                  
            // white space and comments
            skip WHITE_SPACE = "\s+" ;
            skip SINGLE_LINE_COMMENT = "//[^\r\n]*" ;
            skip MULTI_LINE_COMMENT = "/\*[^*]*\*+([^*/][^*]*\*+)*/" ;
                    
            // the predefined basic types   
            leaf identifier          = "[a-zA-Z_][a-zA-Z0-9_]*" ;
            leaf stringLiteral       = "\"([^\"\\]|\\.)*\"";
            leaf numberLiteral       = "[0-9]+";
            leaf booleanLiteral      = 'false' | 'true';
        }
        """.trimIndent()

        val processor = Agl.processorFromString<Any,Any>(grammarStr).processor!!
        const val goal = "LibUnit"
    }

    @Test
    fun t1() {
        val sentence = """
            numerics
            expressions:
                12 + 23;
                23 * 34;
                34 / 45;
                45 - 56;
                (45 * 34) / 56;
                2 + (8-4);
                (90 * 34 - (45 + 789) * 34 /76 ) - 234;
                true and false;
                true + 12;
                false ==> 345;
                | 4567 | ;
                | ( true - false + "aap" ) | ;
        """.trimIndent()
        processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
    }

}