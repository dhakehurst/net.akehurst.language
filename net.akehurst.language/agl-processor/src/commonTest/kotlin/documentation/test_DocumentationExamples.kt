/*
 * Copyright (C) 20253 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.documentation

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.GrammarString
import kotlin.test.Test

class test_DocumentationExamples {

    @Test
    fun simpleExample() {

        /**
         * Create a processor from a grammar
         */
       val processor = Agl.processorFromStringSimple(GrammarString("""
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                skip leaf COMMENT = "//[^\n]*(\n)" ;
            
                unit = declaration* ;
                declaration = datatype | primitive | collection ;
                primitive = 'primitive' ID ;
                collection = 'collection' ID typeParameters? ;
                typeParameters = '<' typeParameterList '>' ;
                typeParameterList = [ID / ',']+ ;
                datatype = 'datatype' ID '{' property* '}' ;
                property = ID ':' typeReference ;
                typeReference = type typeArguments? ;
                typeArguments = '<' typeArgumentList '>' ;
                typeArgumentList = [typeReference / ',']+ ;
            
                leaf ID = "[A-Za-z_][A-Za-z0-9_]*" ;
                leaf type = ID;
            }
        """.trimIndent())).let {
            check(it.issues.errors.isEmpty()) { it.issues.toString() }
            it.processor!!
       }

        /**
         * Parse a sentence to see if it is valid
         */
        val sentence = """
            primitive String
            collection List<E>
            datatype A {
                prop : String
                prop2 : List<String>
            }
        """.trimIndent()
        val result1 = processor.parse(sentence)
        check(result1.issues.errors.isEmpty()) { result1.issues.toString() }

        /**
         * Or,
         * Process the sentence to get an Abstract Syntax Model (tree) of the sentence
         */
        val result2 = processor.process(sentence)
        check(result2.allIssues.errors.isEmpty()) { result2.allIssues.toString() }
        println(result2.asm?.asString())
    }

}