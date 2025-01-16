package agl.simple

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.CompletionItem
import net.akehurst.language.api.processor.CompletionItemKind
import testFixture.data.doTest
import testFixture.data.executeTestSuit
import testFixture.data.testSuit
import kotlin.test.Test

class test_CompletionProviderSimple_with_depth {

    private companion object {

        val grammarStrConcat = """
            namespace test
            grammar Test {
                S = ABC DEF ;
                ABC = A '.' BC ;
                DEF = DE F '.' ;
                BC = B '.' C '.' ;
                DE = D '.' | E '.' ;
                A = 'a' ;
                B = 'b' ; 
                C = 'c' ;
                D = 'd' ;
                E = 'e' ;
                F = 'f' ;
            }
        """

        val grammarStrChoice = """
            namespace test
            grammar Test {
                S = X  ;
                X = ABCp | DEFp ;
                ABCp = ABC '.' ;
                DEFp = DEF '.' ;
                ABC = A | B | C ;
                DEF = D | EF ;
                EF = E F ;
                A = 'a' ;
                B = 'b' ; 
                C = 'c' ;
                D = 'd' ;
                E = 'e' ;
                F = 'f' ;                
            }
        """

        val grammarStrRecursive = """
            namespace test
            grammar Test {
                S = X ;
                X = ABCs | DEFs ;
                ABCs = A BCs ;
                BCs = B C | B C BCs ; 
                DEFs = DEs F ;
                DEs = D E | DEs D E ;
                A = 'a' ;
                B = 'b' ; 
                C = 'c' ;
                D = 'd' ;
                E = 'e' ;
                F = 'f' ;                
            }
        """


        val testSuit = testSuit {
            testData("Concats 0") {
                grammarStr(grammarStrConcat)
                sentencePass("") {
                    options(Agl.options { completionProvider { depth(0) } })
                    expectedCompletionItems(
                        listOf(
                            CompletionItem(CompletionItemKind.LITERAL, "a", "'a'"),
                        )
                    )
                }
            }
            testData("Concats 1") {
                grammarStr(grammarStrConcat)
                sentencePass("") {
                    options(Agl.options { completionProvider { depth(1) } })
                    expectedCompletionItems(
                        listOf(
                            CompletionItem(CompletionItemKind.SEGMENT, "<ABC> <DEF>", "S"),
                            CompletionItem(CompletionItemKind.LITERAL, "a", "'a'"),
                        )
                    )
                }
            }
            testData("Concats 2") {
                grammarStr(grammarStrConcat)
                sentencePass("") {
                    options(Agl.options { completionProvider { depth(2) } })
                    expectedCompletionItems(
                        listOf(
                            CompletionItem(CompletionItemKind.SEGMENT, "<A> . <BC> <DE> <F> .", "S"),
                            CompletionItem(CompletionItemKind.LITERAL, "a", "'a'"),
                        )
                    )
                }
            }
            testData("Concats 3") {
                grammarStr(grammarStrConcat)
                sentencePass("") {
                    options(Agl.options { completionProvider { depth(3) } })
                    expectedCompletionItems(
                        listOf(
                            CompletionItem(CompletionItemKind.SEGMENT, "a . <B> . <C> . <D> . f .", "S"),
                            CompletionItem(CompletionItemKind.SEGMENT, "a . <B> . <C> . <E> . f .", "S"),
                            CompletionItem(CompletionItemKind.LITERAL, "a", "'a'"),
                        )
                    )
                }
            }
            testData("Choice 1") {
                grammarStr(grammarStrChoice)
                sentencePass("") {
                    options(Agl.options { completionProvider { depth(1) } })
                    expectedCompletionItems(
                        listOf(
                            CompletionItem(CompletionItemKind.SEGMENT, "<ABC> .", "ABCp"),
                            CompletionItem(CompletionItemKind.SEGMENT, "<DEF> .", "DEFp"),
                            CompletionItem(CompletionItemKind.LITERAL, "a", "'a'"),
                            CompletionItem(CompletionItemKind.LITERAL, "b", "'b'"),
                            CompletionItem(CompletionItemKind.LITERAL, "c", "'c'"),
                            CompletionItem(CompletionItemKind.LITERAL, "d", "'d'"),
                            CompletionItem(CompletionItemKind.LITERAL, "e", "'e'"),
                        )
                    )
                }
            }
            testData("Choice 2") {
                grammarStr(grammarStrChoice)
                sentencePass("") {
                    options(Agl.options { completionProvider { depth(2) } })
                    expectedCompletionItems(
                        listOf(
                            CompletionItem(CompletionItemKind.SEGMENT, "<A> .", "ABCp"),
                            CompletionItem(CompletionItemKind.SEGMENT, "<B> .", "ABCp"),
                            CompletionItem(CompletionItemKind.SEGMENT, "<C> .", "ABCp"),
                            CompletionItem(CompletionItemKind.SEGMENT, "<D> .", "DEFp"),
                            CompletionItem(CompletionItemKind.SEGMENT, "<EF> .", "DEFp"),
                            CompletionItem(CompletionItemKind.LITERAL, "a", "'a'"),
                            CompletionItem(CompletionItemKind.LITERAL, "b", "'b'"),
                            CompletionItem(CompletionItemKind.LITERAL, "c", "'c'"),
                            CompletionItem(CompletionItemKind.LITERAL, "d", "'d'"),
                            CompletionItem(CompletionItemKind.LITERAL, "e", "'e'"),
                        )
                    )
                }
            }
            testData("Choice 3") {
                grammarStr(grammarStrChoice)
                sentencePass("") {
                    options(Agl.options { completionProvider { depth(3) } })
                    expectedCompletionItems(
                        listOf(
                            CompletionItem(CompletionItemKind.SEGMENT, "a .", "ABCp"),
                            CompletionItem(CompletionItemKind.SEGMENT, "b .", "ABCp"),
                            CompletionItem(CompletionItemKind.SEGMENT, "c .", "ABCp"),
                            CompletionItem(CompletionItemKind.SEGMENT, "d .", "DEFp"),
                            CompletionItem(CompletionItemKind.SEGMENT, "<E> <F> .", "DEFp"),
                            CompletionItem(CompletionItemKind.LITERAL, "a", "'a'"),
                            CompletionItem(CompletionItemKind.LITERAL, "b", "'b'"),
                            CompletionItem(CompletionItemKind.LITERAL, "c", "'c'"),
                            CompletionItem(CompletionItemKind.LITERAL, "d", "'d'"),
                            CompletionItem(CompletionItemKind.LITERAL, "e", "'e'"),
                        )
                    )
                }
            }
            testData("Recursive 1") {
                grammarStr(grammarStrRecursive)
               sentencePass("") {

               }
            }
        }

    }

    @Test
    fun all() = executeTestSuit(testSuit)

    @Test
    fun single() = doTest(testSuit["Choice 3"])
}