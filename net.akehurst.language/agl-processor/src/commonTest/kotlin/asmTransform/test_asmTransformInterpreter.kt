/*
 * Copyright (C) 2024 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

/*
 * Copyright (C) 2024 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.asmTransform.processor

import testFixture.data.doTest
import testFixture.data.testSuit
import kotlin.test.Test

class test_asmTransformInterpreter {
    private companion object Companion {
        val testSuit = testSuit {
            testData("Default type of list rule") {
                grammarStr("""
                    namespace test
                    grammar Test {
                        skip leaf WS = "\s+" ;
                        S = 'block' name '{' listOfA '}' ;
                        listOfA = A* ;
                        A = 'a' ;
                        leaf name = "[a-zA-Z]+" ;
                    }
                """)
                sentencePass("block AA { a a a  }") {
                    expectedAsm {
                        element("S") {
                            propertyString("name","AA")
                            propertyListOfElement("listOfA") {
                                element("A") {}
                                element("A") {}
                                element("A") {}
                            }
                        }
                    }
                }
            }
            testData("Substitute default type of list rule") {
                grammarStr("""
                    namespace test
                    grammar Test {
                        skip leaf WS = "\s+" ;
                        S = 'block' name '{' listOfA '}' ;
                        listOfA = A* ;
                        A = 'a' ;
                        leaf name = "[a-zA-Z]+" ;
                    }
                """)
                transformStr("""
                    #create-missing-types
                    #override-default-transform
                    namespace test
                    asm-transform Test {
                        S: S() {   //TODO: would be nice not to have to override S !
                          name := child[1] as String
                          listOfA := child[3] as String
                        }
                        listOfA: §matchedText as String
                    }
                """.replace("§","$"))
                sentencePass("block AA { a a a  }") {
                    expectedAsm {
                        element("S") {
                            propertyString("name","AA")
                            propertyString("listOfA","a a a")
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testAll() {
        testSuit.testData.forEach {
            println("****** ${it.description} ******")
            doTest(it)
        }
    }

    @Test
    fun single() {
        doTest(testSuit["Substitute default type of list rule"])
    }
}