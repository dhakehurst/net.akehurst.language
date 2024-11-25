package agl.simple

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.GrammarString
import net.akehurst.language.agl.TransformString
import net.akehurst.language.agl.simple.ContextAsmSimple
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.asm.builder.asmSimple
import net.akehurst.language.grammar.processor.ContextFromGrammarRegistry
import testFixture.data.TestDataParserSentence
import testFixture.data.TestDataProcessor
import testFixture.data.TestDataProcessorSentencePass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class test_ProvidedTransform {

    companion object {
        fun processor(grammarStr: String, transformStr: String) = Agl.processorFromStringSimple(
            grammarDefinitionStr = GrammarString(grammarStr),
            transformStr = TransformString(transformStr),
            grammarAglOptions = Agl.options {
                semanticAnalysis {
                    context(ContextFromGrammarRegistry(Agl.registry))
//TODO:                    option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, true)
                }
            }
        )

        fun testSentence(proc: LanguageProcessor<Asm, ContextAsmSimple>, sd: TestDataParserSentence) {
            println("'${sd.sentence}'")
            val res = proc.process(sd.sentence)

            when (sd) {
                is TestDataProcessorSentencePass -> {
                    val asmRes = proc.process(sd.sentence)
                    assertTrue(asmRes.issues.errors.isEmpty(), asmRes.issues.toString())
                    val actual = asmRes.asm!!
                    assertEquals(sd.expectedAsm.asString(indentIncrement = "  "), actual.asString(indentIncrement = "  "), "Different ASM")
                }

                //is TestDataParserSentenceFail -> {}
                else -> error("Unsupported")
            }

        }

        fun testPass(testData: TestDataProcessor, sentenceIndex: Int? = null) {
            val procRes = processor(testData.grammarStr, testData.transformStr)
            assertTrue(procRes.issues.isEmpty(), procRes.issues.toString())
            val proc = procRes.processor!!

            if (null == sentenceIndex) {
                for (sd in testData.sentences) {
                    testSentence(proc, sd)
                }
            } else {
                val sd = testData.sentences[sentenceIndex]
                testSentence(proc, sd)
            }
        }

        val testData = listOf(
            TestDataProcessor(
                "",
                grammarStr = """
                    namespace test
                      grammar Test {
                        S = a ;
                        leaf a = 'a';
                      }
                """.trimIndent(),
                transformStr = """
                    #create-missing-types
                    namespace test
                      transform Test {
                        S : XXX() { yyy := child[0] }
                      }
                """.trimIndent(),
                "",
                "S",
                listOf(
                    TestDataProcessorSentencePass(
                        "a",
                        asmSimple {
                            element("XXX") {
                                propertyString("yyy","a")
                            }
                        }
                    )
                )
            ),
            TestDataProcessor(
                "",
                grammarStr = """
                    namespace test
                      grammar Test {
                        S = A | B ;
                        A = a a ;
                        B = 'b' b ;
                        leaf a = 'a';
                        leaf b = 'b';
                      }
                """.trimIndent(),
                transformStr = """
                    #create-missing-types
                    namespace test
                      transform Test {
                        #override-default-transform
                        S : XXX() { yyy := child[0] }
                      }
                """.trimIndent(),
                "",
                "S",
                listOf(
                    TestDataProcessorSentencePass(
                        "aa",
                        asmSimple {
                            element("XXX") {
                                propertyElementExplicitType("yyy","A") {
                                    propertyString("a","a")
                                    propertyString("a2","a")
                                }
                            }
                        }
                    ),
                    TestDataProcessorSentencePass(
                        "bb",
                        asmSimple {
                            element("XXX") {
                                propertyElementExplicitType("yyy","B") {
                                    propertyString("b","b")
                                }
                            }
                        }
                    )
                )
            ),
            TestDataProcessor(
                "",
                grammarStr = """
                    namespace test
                      grammar Test {
                        S = A | B ;
                        A = a a ;
                        B = 'b' b ;
                        leaf a = 'a';
                        leaf b = 'b';
                      }
                """.trimIndent(),
                transformStr = """
                    #create-missing-types
                    namespace test
                      transform Test {
                        #override-default-transform
                        S : XXX() { yyy := child[0] }
                        A : child[0] as String
                      }
                """.trimIndent(),
                "",
                "S",
                listOf(
                    TestDataProcessorSentencePass(
                        "aa",
                        asmSimple {
                            element("XXX") {
                                propertyString("yyy","a")
                            }
                        }
                    ),
                    TestDataProcessorSentencePass(
                        "bb",
                        asmSimple {
                            element("XXX") {
                                propertyElementExplicitType("yyy","B") {
                                    propertyString("b","b")
                                }
                            }
                        }
                    )
                )
            )
        )

    }

    @Test
    fun transform_for_user_goal_one_rule_one_leaf() {
        testPass(testData[0])
    }

    @Test
    fun transform_for_user_goal_three_rule_two_leaf() {
        testPass(testData[1])
    }

    @Test
    fun t3() {
        testPass(testData[2])
    }

}