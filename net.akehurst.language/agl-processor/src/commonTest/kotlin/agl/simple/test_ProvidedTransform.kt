package agl.simple

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.GrammarString
import net.akehurst.language.agl.TransformString
import net.akehurst.language.agl.simple.ContextAsmSimple
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.asm.builder.asmSimple
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.grammar.processor.ContextFromGrammarRegistry
import testFixture.data.TestDataParserSentence
import testFixture.data.TestDataProcessor
import testFixture.data.TestDataProcessorSentencePass
import testFixture.data.testSuit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class test_ProvidedTransform {

    companion object {
        fun processor(grammarStr: String, transformStr: String?) = Agl.processorFromStringSimple(
            grammarDefinitionStr = GrammarString(grammarStr),
            transformStr = transformStr?.let { TransformString(it) },
            grammarAglOptions = Agl.options {
                semanticAnalysis {
                    context(ContextFromGrammarRegistry(Agl.registry))
//TODO:                    option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, true)
                }
            }
        )

        fun testSentence(proc: LanguageProcessor<Asm, ContextAsmSimple>, sd: TestDataParserSentence) {
            println("'${sd.sentence}'")
            when (sd) {
                is TestDataProcessorSentencePass -> {
                    val asmRes = proc.process(sd.sentence, Agl.options {
                        this.parse { goalRuleName(sd.goal) }
                    })
                    assertTrue(asmRes.issues.errors.isEmpty(), asmRes.issues.toString())
                    val actual = asmRes.asm!!
                    assertEquals(sd.expectedAsm.asString(indentIncrement = "  "), actual.asString(indentIncrement = "  "), "Different ASM")
                }

                //is TestDataParserSentenceFail -> {}
                else -> error("Unsupported")
            }

        }

        fun doTest(testData: TestDataProcessor, sentenceIndex: Int? = null) {
            val procRes = processor(testData.grammarStr, testData.transformStr)
            assertTrue(procRes.issues.isEmpty(), procRes.issues.toString())
            val proc = procRes.processor!!

            println("--- TypeDomain ---")
            println(proc.typeModel.asString())
            println("--- Asm Transform ---")
            println(proc.asmTransformModel.asString())

            if (null == sentenceIndex) {
                for (sd in testData.sentences) {
                    testSentence(proc, sd)
                }
            } else {
                val sd = testData.sentences[sentenceIndex]
                testSentence(proc, sd)
            }
        }

        val testData = testSuit {
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
                sentencePass("a", "S") {
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
                sentencePass("a", "S") {
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
                sentencePass("a", "S") {
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
                sentencePass("aa", "S") {
                    expectedAsm(asmSimple {
                        element("XXX") {
                            propertyElementExplicitType("yyy", "A") {
                                propertyString("a", "a")
                                propertyString("a2", "a")
                            }
                        }
                    })
                }
                sentencePass("bb", "S") {
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
                sentencePass("a", "S") {
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
                sentencePass("aa", "S") {
                    expectedAsm(asmSimple {
                        element("XXX") {
                            propertyString("yyy", "a")
                        }
                    })
                }
                sentencePass("bb", "S") {
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
                sentencePass("aa", "S") {
                    expectedAsm(asmSimple {
                        element("XXX") {
                            propertyString("yyy", "a")
                        }
                    })
                }
                sentencePass("bb", "S") {
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
                sentencePass("d", "S") {
                    expectedAsm(asmSimple() {
                        string("d")
                    })
                }
                sentencePass("babd", "S") {
                    expectedAsm(asmSimple {
                        element("S1") {
                            propertyElementExplicitType("b", "BI") {
                                propertyString("inner", "a")
                            }
                            propertyString("s", "d")
                        }
                    })
                }
                sentencePass("cacd", "S") {
                    expectedAsm(asmSimple() {
                        element("S1") {
                            propertyElementExplicitType("b", "BI") {
                                propertyString("inner", "a")
                            }
                            propertyString("s", "d")
                        }
                    })
                }
                sentencePass("baaaabd", "S") {
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
                sentencePass("d", "S") {
                    expectedAsm(asmSimple() {
                        string("d")
                    })
                }
                sentencePass("babd", "S") {
                    expectedAsm(asmSimple {
                        element("S1") {
                            propertyElementExplicitType("b", "BI") {
                                propertyString("inner", "a")
                            }
                            propertyString("s", "d")
                        }
                    })
                }
                sentencePass("cacd", "S") {
                    expectedAsm(asmSimple() {
                        element("S1") {
                            propertyElementExplicitType("b", "BI") {
                                propertyString("inner", "a")
                            }
                            propertyString("s", "d")
                        }
                    })
                }
                sentencePass("baaaabd", "S") {
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
    fun testAll() {
        testData.testData.forEach {
            println("****** ${it.description} ******")
            doTest(it)
        }
    }

    @Test
    fun single() {
        doTest(testData["S = a; leaf a = 'a'; override-default in unit, change only type of root rule"])
    }
}