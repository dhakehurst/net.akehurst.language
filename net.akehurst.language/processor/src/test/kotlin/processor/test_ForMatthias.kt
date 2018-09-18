/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.processor

import net.akehurst.language.api.parser.ParseFailedException
import kotlin.test.*

internal class test_ForMatthias {

    @Test
    fun parser_grammarDefinitionStr() {
        val grammarStr = """
            namespace test
            grammar Matthias {
              skip WHITESPACE = "\\s+";
              conceptDefinition = 'concept' conceptName '{' properties '}';
              properties = 'properties' '{' propertyDefinition* '}' ;
              propertyDefinition = propertyName ':' typeName quantifier ;
              conceptName = IDENTIFIER ;
              propertyName = IDENTIFIER ;
              typeName = IDENTIFIER ;
              quantifier = '[0..n]' | '[1]' | '[0..1]' ;
              IDENTIFIER = "[a-zA-Z_][a-zA-Z_0-9]*" ;
            }
        """.trimIndent()
        val p = parser(grammarStr)
        p.parse("conceptDefinition", """
            concept Test {
              properties {
                 p1 : Int [1]
                 p2 : Int [1]
              }
            }
        """.trimIndent())
    }

}