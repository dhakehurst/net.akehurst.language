package agl.simple

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.CompletionItem
import net.akehurst.language.api.processor.CompletionItemKind
import testFixture.data.doTest
import testFixture.data.executeTestSuit
import testFixture.data.testSuit
import kotlin.test.Test

class test_CompletionProviderSimple_Optional {

    private companion object {

        val grammarStrConcat = """
            namespace test
            grammar Test {
                S = ABC DEF ;
                ABC = A '.' BC? ;
                DEF = DE F '.' ;
                BC = B '.' C '.' ;
                DE = D '.' | E '.' ;
                A = 'a' ;
                B = 'b' ; 
                C = 'c' ;
                D = "[dxyz]" ;
                E = 'e' ;
                F = 'f' ;
            }
        """

        val testSuit = testSuit {
            testData("Optional 0") {
                grammarStr(grammarStrConcat)
                sentencePass("") {
                    options(Agl.options { completionProvider { depth(0) } })
                    expectedCompletionItems(
                        listOf(
                            CompletionItem(CompletionItemKind.SEGMENT, "S", "<ABC> <DEF>"),
                            CompletionItem(CompletionItemKind.LITERAL, "'a'", "a"),
                        )
                    )
                }
            }
            testData("Optional 1") {
                grammarStr(grammarStrConcat)
                sentencePass("") {
                    options(Agl.options { completionProvider { depth(1) } })
                    expectedCompletionItems(
                        listOf(
                            CompletionItem(CompletionItemKind.SEGMENT, "S", "<A> . <BC> <DE> <F> ."),
                            CompletionItem(CompletionItemKind.SEGMENT, "S", "<A> . <DE> <F> ."),
                            CompletionItem(CompletionItemKind.LITERAL, "'a'", "a"),
                        )
                    )
                }
            }
            testData("Optional 2") {
                grammarStr(grammarStrConcat)
                sentencePass("") {
                    options(Agl.options { completionProvider { depth(2) } })
                    expectedCompletionItems(
                        listOf(
                            CompletionItem(CompletionItemKind.SEGMENT, "S", "a . <B> . <C> . <E> . f ."),
                            CompletionItem(CompletionItemKind.SEGMENT, "S", "a . <B> . <C> . <D> . f ."),
                            CompletionItem(CompletionItemKind.SEGMENT, "S", "a . <E> . f ."),
                            CompletionItem(CompletionItemKind.SEGMENT, "S", "a . <D> . f ."),
                            CompletionItem(CompletionItemKind.LITERAL, "'a'", "a"),
                        )
                    )
                }
            }
            testData("Optional 3") {
                grammarStr(grammarStrConcat)
                sentencePass("") {
                    options(Agl.options { completionProvider { depth(3) } })
                    expectedCompletionItems(
                        listOf(
                            CompletionItem(CompletionItemKind.SEGMENT, "S", "a . b . c . <[dxyz]> . f ."),
                            CompletionItem(CompletionItemKind.SEGMENT, "S", "a . b . c . e . f ."),
                            CompletionItem(CompletionItemKind.SEGMENT, "S", "a . <[dxyz]> . f ."),
                            CompletionItem(CompletionItemKind.SEGMENT, "S", "a . e . f ."),
                            CompletionItem(CompletionItemKind.LITERAL, "'a'", "a"),
                        )
                    )
                }
                sentencePass("") {
                    options(Agl.options { completionProvider {
                        depth(3)
                        provideValuesForPatternTerminals(true)
                    } })
                    expectedCompletionItems(
                        listOf(
                            CompletionItem(CompletionItemKind.SEGMENT, "S", "a . b . c . e . f ."),
                            CompletionItem(CompletionItemKind.SEGMENT, "S", "a . b . c . d . f ."),
                            CompletionItem(CompletionItemKind.SEGMENT, "S", "a . e . f ."),
                            CompletionItem(CompletionItemKind.SEGMENT, "S", "a . d . f ."),
                            CompletionItem(CompletionItemKind.LITERAL, "'a'", "a"),
                        )
                    )
                }
                sentencePass("a.b.c.") {
                    options(Agl.options { completionProvider { depth(3) } })
                    expectedCompletionItems(
                        listOf(
                            CompletionItem(CompletionItemKind.SEGMENT, "DEF", "<[dxyz]> . f ."),
                            CompletionItem(CompletionItemKind.SEGMENT, "DEF", "e . f ."),
                            CompletionItem(CompletionItemKind.LITERAL, "'e'", "e"),
                            CompletionItem(CompletionItemKind.PATTERN, "[dxyz]", "<[dxyz]>"),
                        )
                    )
                }
                sentencePass("a.b.c.") {
                    options(Agl.options { completionProvider {
                        depth(3)
                        provideValuesForPatternTerminals(true)
                    } })
                    expectedCompletionItems(
                        listOf(
                            CompletionItem(CompletionItemKind.SEGMENT, "DEF", "e . f ."),
                            CompletionItem(CompletionItemKind.SEGMENT, "DEF", "d . f ."),
                            CompletionItem(CompletionItemKind.LITERAL, "'e'", "e"),
                            CompletionItem(CompletionItemKind.PATTERN, "[dxyz]", "d"),
                        )
                    )
                }
            }
        }

    }

    @Test
    fun all() = executeTestSuit(testSuit)

    @Test
    fun single() = doTest(testSuit["Optional 1"])
}