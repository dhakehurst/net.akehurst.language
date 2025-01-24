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

package net.akehurst.language.agl.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.GrammarString
import net.akehurst.language.parser.leftcorner.LeftCornerParser
import kotlin.test.Test
import kotlin.test.assertTrue

 class test_ObjectSerialisation {

    private companion object {
        val grammarStr = """
        namespace test
        grammar Test {
        
            skip leaf WS = "\s+" ;
        
            content = object ;
            object = '{'
              property+
            '}' ;

            property = NAME ':' value ;
            value = object | STRING | list ;
            list = '[' value* ']' ;
            //item = object | STRING ;

            // other chars NOT : or whitespace
            leaf NAME = "([^: \t\n\x0B\f\r]|\\.)+" ;
            leaf STRING = "'([^']|\\.)*'" ;
        }
        """.trimIndent()

        val processor = Agl.processorFromStringSimple(GrammarString(grammarStr)).processor!!

        fun testParse(sentence: String) {
            val res = processor.parse(sentence)
            assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
            assertTrue(((processor as LanguageProcessorDefault).parser as LeftCornerParser).runtimeDataIsEmpty)
        }
    }

    @Test
    fun one_property_string() {
        val sentence = """
            {
              obj: {
               prop: ''
              }
            }
        """.trimIndent()
        testParse(sentence)
    }

    @Test
    fun one_property_list_string() {
        val sentence = """
            {
              obj: {
               list: ['' '' '' '']
              }
            }
        """.trimIndent()
        testParse(sentence)
    }

    @Test
    fun one_property_list_obj() {
        val sentence = """
            {
              obj: {
               list: [{
                 prop:''
               } {
                 prop:''
               }]
              }
            }
        """.trimIndent()
        testParse(sentence)
    }

    @Test
    fun f() {
        val sentence = """
        {
            d: {
                r: '...'
                p: [{
                    r: '...'
                    n:'p'
                    f:'...p'
                    d:''
                    path:'/././p'
                    q:'p'
                    bsd: [{
                        n:''
                        d:''
                        bs: [{
                            v:'bls'
                            m:''
                            s:''
                            a:''
                            u:''
                            d:'...'
                        }]
                    }]
                    m: [{
                      r:''
                      n:''
                      f:''
                      d:''
                      p:''
                      q:''
                      v:{
                        n:''
                        f:''
                        v:'v'
                      }
                      oa: ['oa']
                      ma: ['ma']
                      os: [{
                        r:''
                        i:''
                        n:''
                        l:''
                        a:''
                        f:''
                        p:{
                             n:''
                             f:''
                             v:'v'
                        }
                        a: []
                        e: []
                      }]
                      l: []
                    }]
                }]
            }
            m: {
                d:''
                t:''
                m:''
                r:''
                o:''
                lm:''
                ls:''
                l:''
            }
        }
""".trimIndent()
        testParse(sentence)
    }
}