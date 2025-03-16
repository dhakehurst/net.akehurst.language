package agl.simple

import net.akehurst.language.asm.builder.asmSimple
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
                    expectedAsm(asmSimple {
                        element("S") {
                            propertyString("a", "a")
                        }
                    })
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
                    expectedAsm(asmSimple {
                        element("S") {
                            propertyString("a", "a")
                        }
                    })
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
                      transform Test {
                        S : XXX() { yyy := child[0] }
                      }
                """.trimIndent()
                )
                sentencePass("a") {
                    expectedAsm(asmSimple {
                        element("XXX") {
                            propertyString("yyy", "a")
                        }
                    })
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
                      transform Test {
                        #override-default-transform
                        S : XXX() { yyy := child[0] }
                      }
                """.trimIndent()
                )
                sentencePass("aa") {
                    expectedAsm(asmSimple {
                        element("XXX") {
                            propertyElementExplicitType("yyy", "A") {
                                propertyString("a", "a")
                                propertyString("a2", "a")
                            }
                        }
                    })
                }
                sentencePass("bb") {
                    expectedAsm(asmSimple {
                        element("XXX") {
                            propertyElementExplicitType("yyy", "B") {
                                propertyString("b", "b")
                            }
                        }
                    })
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
                      transform Test {
                        S : XXX
                      }
                """.trimIndent()
                )
                sentencePass("a") {
                    expectedAsm(asmSimple {
                        element("XXX") {
                            propertyString("a", "a")
                        }
                    })
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
                      transform Test {
                        #override-default-transform
                        S : XXX() { yyy := child[0] }
                        A : child[0] as String
                      }
                """.trimIndent()
                )
                sentencePass("aa") {
                    expectedAsm(asmSimple {
                        element("XXX") {
                            propertyString("yyy", "a")
                        }
                    })
                }
                sentencePass("bb") {
                    expectedAsm(asmSimple {
                        element("XXX") {
                            propertyElementExplicitType("yyy", "B") {
                                propertyString("b", "b")
                            }
                        }
                    })
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
                      transform Test {
                        S : XXX() { yyy := child[0] }
                        A : child[0] as String
                      }
                """.trimIndent()
                )
                sentencePass("aa") {
                    expectedAsm(asmSimple {
                        element("XXX") {
                            propertyString("yyy", "a")
                        }
                    })
                }
                sentencePass("bb") {
                    expectedAsm(asmSimple {
                        element("XXX") {
                            propertyElementExplicitType("yyy", "B") {
                                propertyString("b", "b")
                            }
                        }
                    })
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
                        transform Outer {
                            B: BI() { inner := child[1] }
                        }
                """
                )
                sentencePass("d") {
                    expectedAsm(asmSimple() {
                        string("d")
                    })
                }
                sentencePass("babd") {
                    expectedAsm(asmSimple {
                        element("S1") {
                            propertyElementExplicitType("b", "BI") {
                                propertyString("inner", "a")
                            }
                            propertyString("s", "d")
                        }
                    })
                }
                sentencePass("cacd") {
                    expectedAsm(asmSimple() {
                        element("S1") {
                            propertyElementExplicitType("b", "BI") {
                                propertyString("inner", "a")
                            }
                            propertyString("s", "d")
                        }
                    })
                }
                sentencePass("baaaabd") {
                    expectedAsm(asmSimple {
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
                    })
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
                        transform Inner {
                            S: child[0] as String
                            S1: (child[0] + child[1]) as String
                        }
                        transform Outer {
                            B: BI() { inner := child[1] }
                        }
                """.replace("§", "$")
                )
                sentencePass("d") {
                    expectedAsm(asmSimple() {
                        string("d")
                    })
                }
                sentencePass("babd") {
                    expectedAsm(asmSimple {
                        element("S1") {
                            propertyElementExplicitType("b", "BI") {
                                propertyString("inner", "a")
                            }
                            propertyString("s", "d")
                        }
                    })
                }
                sentencePass("cacd") {
                    expectedAsm(asmSimple() {
                        element("S1") {
                            propertyElementExplicitType("b", "BI") {
                                propertyString("inner", "a")
                            }
                            propertyString("s", "d")
                        }
                    })
                }
                sentencePass("baaaabd") {
                    expectedAsm(asmSimple {
                        element("S1") {
                            propertyElementExplicitType("b", "BI") {
                                propertyString("inner", "aaaa")
                            }
                            propertyString("s", "d")
                        }
                    })
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