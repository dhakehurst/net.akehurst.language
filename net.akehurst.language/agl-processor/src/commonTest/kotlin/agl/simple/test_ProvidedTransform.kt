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

package net.akehurst.language.agl.simple

import testFixture.data.doTest
import testFixture.data.executeTestSuit
import testFixture.data.testSuit
import kotlin.test.Test

class test_ProvidedTransform {

    private companion object {
        val testSuit = testSuit {
            // TODO: test where not creating missing types
            testData("S = a; leaf a = 'a'; no provided transform") {
                grammarStr(
                    """
                    namespace test
                      grammar Test {
                        S = a ;
                        leaf a = 'a';
                      }
                """.trimIndent()
                )
                sentencePass("a") {
                    expectedAsm {
                        element("S") {
                            propertyString("a", "a")
                        }
                    }
                }
            }
            testData("S = a; leaf a = 'a'; nothing rewritten, use default") {
                grammarStr(
                    """
                    namespace test
                      grammar Test {
                        S = a ;
                        leaf a = 'a';
                      }
                """.trimIndent()
                )
                transformStr(
                    """
                    #override-default-transform
                    #create-missing-types
                    namespace test
                """.trimIndent()
                )
                sentencePass("a") {
                    expectedAsm {
                        element("S") {
                            propertyString("a", "a")
                        }
                    }
                }
            }
            testData("S = a; leaf a = 'a'; rewrite root rule") {
                grammarStr(
                    """
                    namespace test
                      grammar Test {
                        S = a ;
                        leaf a = 'a';
                      }
                """.trimIndent()
                )
                transformStr(
                    """
                    #create-missing-types
                    namespace test
                      asm-transform Test {
                        S : XXX() { yyy := child[0] }
                      }
                """.trimIndent()
                )
                sentencePass("a") {
                    expectedAsm {
                        element("XXX") {
                            propertyString("yyy", "a")
                        }
                    }
                }
            }
            testData("S = A | B; A = a a; B = 'b' b; rewrite root only") {
                grammarStr(
                    """
                    namespace test
                      grammar Test {
                        S = A | B ;
                        A = a a ;
                        B = 'b' b ;
                        leaf a = 'a';
                        leaf b = 'b';
                      }
                """.trimIndent()
                )
                transformStr(
                    """
                    #create-missing-types
                    namespace test
                      asm-transform Test {
                        #override-default-transform
                        S : XXX() { yyy := child[0] }
                      }
                """.trimIndent()
                )
                sentencePass("aa") {
                    expectedAsm {
                        element("XXX") {
                            propertyElementExplicitType("yyy", "A") {
                                propertyString("a", "a")
                                propertyString("a2", "a")
                            }
                        }
                    }
                }
                sentencePass("bb") {
                    expectedAsm {
                        element("XXX") {
                            propertyElementExplicitType("yyy", "B") {
                                propertyString("b", "b")
                            }
                        }
                    }
                }
            }
            testData("S = a; leaf a = 'a'; override-default in unit, change only type of root rule") {
                grammarStr(
                    """
                    namespace test
                      grammar Test {
                        S = a ;
                        leaf a = 'a';
                      }
                """.trimIndent()
                )
                transformStr(
                    """
                    #create-missing-types
                    #override-default-transform
                    namespace test
                      asm-transform Test {
                        S : XXX
                      }
                """.trimIndent()
                )
                sentencePass("a") {
                    expectedAsm {
                        element("XXX") {
                            propertyString("a", "a")
                        }
                    }
                }
            }
            testData("S = A | B; A = a a; B = 'b' b; override-default in transform, rewrite Root and one choice") {
                grammarStr(
                    """
                    namespace test
                      grammar Test {
                        S = A | B ;
                        A = a a ;
                        B = 'b' b ;
                        leaf a = 'a';
                        leaf b = 'b';
                      }
                """.trimIndent()
                )
                transformStr(
                    """
                    #create-missing-types
                    namespace test
                      asm-transform Test {
                        #override-default-transform
                        S : XXX() { yyy := child[0] }
                        A : child[0] as String
                      }
                """.trimIndent()
                )
                sentencePass("aa") {
                    expectedAsm {
                        element("XXX") {
                            propertyString("yyy", "a")
                        }
                    }
                }
                sentencePass("bb") {
                    expectedAsm {
                        element("XXX") {
                            propertyElementExplicitType("yyy", "B") {
                                propertyString("b", "b")
                            }
                        }
                    }
                }
            }
            testData("S = A | B; A = a a; B = 'b' b; override-default in unit, rewrite Root and one choice") {
                grammarStr(
                    """
                    namespace test
                      grammar Test {
                        S = A | B ;
                        A = a a ;
                        B = 'b' b ;
                        leaf a = 'a';
                        leaf b = 'b';
                      }
                """.trimIndent()
                )
                transformStr(
                    """
                    #create-missing-types
                    #override-default-transform
                    namespace test
                      asm-transform Test {
                        S : XXX() { yyy := child[0] }
                        A : child[0] as String
                      }
                """.trimIndent()
                )
                sentencePass("aa") {
                    expectedAsm {
                        element("XXX") {
                            propertyString("yyy", "a")
                        }
                    }
                }
                sentencePass("bb") {
                    expectedAsm {
                        element("XXX") {
                            propertyElementExplicitType("yyy", "B") {
                                propertyString("b", "b")
                            }
                        }
                    }
                }
            }
            testData("Embedded grammars, override-default in unit, rewrite outer") {
                grammarStr(
                    """
                    namespace test
                        grammar Inner {
                            S = a | S1 ;
                            S1 = S a ;
                            leaf a = 'a' ;
                        }
                        grammar Outer {
                            S = d | S1 ;
                            S1 = B S ;
                            B = b Inner::S b | c Inner::S c ;
                            leaf b = 'b' ;
                            leaf c = 'c' ;
                            leaf d = 'd' ;
                        }                    
                """
                )
                transformStr(
                    """
                    #create-missing-types
                    #override-default-transform
                    namespace test
                        asm-transform Outer {
                            B: BI() { inner := child[1] }
                        }
                """
                )
                sentencePass("d") {
                    expectedAsm {
                        string("d")
                    }
                }
                sentencePass("babd") {
                    expectedAsm {
                        element("S1") {
                            propertyElementExplicitType("b", "BI") {
                                propertyString("inner", "a")
                            }
                            propertyString("s", "d")
                        }
                    }
                }
                sentencePass("cacd") {
                    expectedAsm {
                        element("S1") {
                            propertyElementExplicitType("b", "BI") {
                                propertyString("inner", "a")
                            }
                            propertyString("s", "d")
                        }
                    }
                }
                sentencePass("baaaabd") {
                    expectedAsm {
                        element("S1") {
                            propertyElementExplicitType("b", "BI") {
                                propertyElementExplicitType("inner", "S1") {
                                    propertyElementExplicitType("s", "S1") {
                                        propertyElementExplicitType("s", "S1") {
                                            propertyString("s", "a")
                                            propertyString("a", "a")
                                        }
                                        propertyString("a", "a")
                                    }
                                    propertyString("a", "a")
                                }
                            }
                            propertyString("s", "d")
                        }
                    }
                }
            }
            testData("Embedded grammars, override-default in unit, rewrite inner & outer") {
                grammarStr(
                    """
                    namespace test
                        grammar Inner {
                            S = a | S1 ;
                            S1 = S a ;
                            leaf a = 'a' ;
                        }
                        grammar Outer {
                            S = d | S1 ;
                            S1 = B S ;
                            B = b Inner::S b | c Inner::S c ;
                            leaf b = 'b' ;
                            leaf c = 'c' ;
                            leaf d = 'd' ;
                        }                    
                """
                )
                transformStr(
                    """
                    #create-missing-types
                    #override-default-transform
                    namespace test
                        asm-transform Inner {
                            S: child[0] as String
                            S1: (child[0] + child[1]) as String
                        }
                        asm-transform Outer {
                            B: BI() { inner := child[1] }
                        }
                """.replace("§", "$")
                )
                sentencePass("d") {
                    expectedAsm {
                        string("d")
                    }
                }
                sentencePass("babd") {
                    expectedAsm {
                        element("S1") {
                            propertyElementExplicitType("b", "BI") {
                                propertyString("inner", "a")
                            }
                            propertyString("s", "d")
                        }
                    }
                }
                sentencePass("cacd") {
                    expectedAsm {
                        element("S1") {
                            propertyElementExplicitType("b", "BI") {
                                propertyString("inner", "a")
                            }
                            propertyString("s", "d")
                        }
                    }
                }
                sentencePass("baaaabd") {
                    expectedAsm {
                        element("S1") {
                            propertyElementExplicitType("b", "BI") {
                                propertyString("inner", "aaaa")
                            }
                            propertyString("s", "d")
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testAll() = executeTestSuit(testSuit)

    @Test
    fun single() {
        doTest(testSuit["S = a; leaf a = 'a'; override-default in unit, change only type of root rule"])
    }
}