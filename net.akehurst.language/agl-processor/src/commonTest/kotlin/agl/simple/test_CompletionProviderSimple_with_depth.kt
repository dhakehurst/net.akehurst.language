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
                            CompletionItem(CompletionItemKind.LITERAL, "'a'", "a"),
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
                            CompletionItem(CompletionItemKind.SEGMENT, "S", "<ABC> <DEF>"),
                            CompletionItem(CompletionItemKind.LITERAL, "'a'", "a"),
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
                            CompletionItem(CompletionItemKind.SEGMENT, "S", "<A> . <BC> <DE> <F> ."),
                            CompletionItem(CompletionItemKind.LITERAL, "'a'", "a"),
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
                            CompletionItem(CompletionItemKind.SEGMENT, "S", "a . <B> . <C> . <D> . f ."),
                            CompletionItem(CompletionItemKind.SEGMENT, "S", "a . <B> . <C> . <E> . f ."),
                            CompletionItem(CompletionItemKind.LITERAL, "'a'", "a"),
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
                            CompletionItem(CompletionItemKind.SEGMENT, "X", "<X>"),
                            CompletionItem(CompletionItemKind.LITERAL, "'a'", "a"),
                            CompletionItem(CompletionItemKind.LITERAL, "'b'", "b"),
                            CompletionItem(CompletionItemKind.LITERAL, "'c'", "c"),
                            CompletionItem(CompletionItemKind.LITERAL, "'d'", "d"),
                            CompletionItem(CompletionItemKind.LITERAL, "'e'", "e"),
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
                            CompletionItem(CompletionItemKind.SEGMENT, "ABCp", "<ABCp>"),
                            CompletionItem(CompletionItemKind.SEGMENT, "DEFp", "<DEFp>"),
                            CompletionItem(CompletionItemKind.LITERAL, "'a'", "a"),
                            CompletionItem(CompletionItemKind.LITERAL, "'b'", "b"),
                            CompletionItem(CompletionItemKind.LITERAL, "'c'", "c"),
                            CompletionItem(CompletionItemKind.LITERAL, "'d'", "d"),
                            CompletionItem(CompletionItemKind.LITERAL, "'e'", "e"),
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
                            CompletionItem(CompletionItemKind.SEGMENT, "ABCp", "<ABC> ."),
                            CompletionItem(CompletionItemKind.SEGMENT, "DEFp", "<DEF> ."),
                            CompletionItem(CompletionItemKind.LITERAL, "'a'", "a"),
                            CompletionItem(CompletionItemKind.LITERAL, "'b'", "b"),
                            CompletionItem(CompletionItemKind.LITERAL, "'c'", "c"),
                            CompletionItem(CompletionItemKind.LITERAL, "'d'", "d"),
                            CompletionItem(CompletionItemKind.LITERAL, "'e'", "e"),
                        )
                    )
                }
            }
            testData("Choice 4") {
                grammarStr(grammarStrChoice)
                sentencePass("") {
                    options(Agl.options { completionProvider { depth(4) } })
                    expectedCompletionItems(
                        listOf(
                            CompletionItem(CompletionItemKind.SEGMENT, "A", "<A> ."),
                            CompletionItem(CompletionItemKind.SEGMENT, "B", "<B> ."),
                            CompletionItem(CompletionItemKind.SEGMENT, "C", "<C> ."),
                            CompletionItem(CompletionItemKind.SEGMENT, "D", "<D> ."),
                            CompletionItem(CompletionItemKind.SEGMENT, "EF", "<EF> ."),
                            CompletionItem(CompletionItemKind.LITERAL, "'a'", "a"),
                            CompletionItem(CompletionItemKind.LITERAL, "'b'", "b"),
                            CompletionItem(CompletionItemKind.LITERAL, "'c'", "c"),
                            CompletionItem(CompletionItemKind.LITERAL, "'d'", "d"),
                            CompletionItem(CompletionItemKind.LITERAL, "'e'", "e"),
                        )
                    )
                }
            }
            testData("Choice 5") {
                grammarStr(grammarStrChoice)
                sentencePass("") {
                    options(Agl.options { completionProvider { depth(5) } })
                    expectedCompletionItems(
                        listOf(
                            CompletionItem(CompletionItemKind.SEGMENT, "A", "a ."),
                            CompletionItem(CompletionItemKind.SEGMENT, "B", "b ."),
                            CompletionItem(CompletionItemKind.SEGMENT, "C", "c ."),
                            CompletionItem(CompletionItemKind.SEGMENT, "D", "d ."),
                            CompletionItem(CompletionItemKind.SEGMENT, "EF", "<E> <F> ."),
                            CompletionItem(CompletionItemKind.LITERAL, "'a'", "a"),
                            CompletionItem(CompletionItemKind.LITERAL, "'b'", "b"),
                            CompletionItem(CompletionItemKind.LITERAL, "'c'", "c"),
                            CompletionItem(CompletionItemKind.LITERAL, "'d'", "d"),
                            CompletionItem(CompletionItemKind.LITERAL, "'e'", "e"),
                        )
                    )
                }
            }
            testData("Recursive 1") {
                grammarStr(grammarStrRecursive)
                sentencePass("") {
                    options(Agl.options { completionProvider { depth(1) } })
                    expectedCompletionItems(
                        listOf(
                            CompletionItem(CompletionItemKind.SEGMENT, "X", "<X>"),
                            CompletionItem(CompletionItemKind.LITERAL, "'a'", "a"),
                            CompletionItem(CompletionItemKind.LITERAL, "'d'", "d"),
                        )
                    )
                }
            }
            testData("Recursive 2") {
                grammarStr(grammarStrRecursive)
                sentencePass("") {
                    options(Agl.options { completionProvider { depth(2) } })
                    expectedCompletionItems(
                        listOf(
                            CompletionItem(CompletionItemKind.SEGMENT, "ABCs", "<ABCs>"),
                            CompletionItem(CompletionItemKind.SEGMENT, "DEFs", "<DEFs>"),
                            CompletionItem(CompletionItemKind.LITERAL, "'a'", "a"),
                            CompletionItem(CompletionItemKind.LITERAL, "'d'", "d"),
                        )
                    )
                }
            }
            testData("Recursive 3") {
                grammarStr(grammarStrRecursive)
                sentencePass("") {
                    options(Agl.options { completionProvider { depth(3) } })
                    expectedCompletionItems(
                        listOf(
                            CompletionItem(CompletionItemKind.SEGMENT, "ABCs", "<A> <BCs>"),
                            CompletionItem(CompletionItemKind.SEGMENT, "DEFs", "<DEs> <F>"),
                            CompletionItem(CompletionItemKind.LITERAL, "'a'", "a"),
                            CompletionItem(CompletionItemKind.LITERAL, "'d'", "d"),
                        )
                    )
                }
            }
            testData("Recursive 4") {
                grammarStr(grammarStrRecursive)
                sentencePass("") {
                    options(Agl.options { completionProvider { depth(4) } })
                    expectedCompletionItems(
                        listOf(
                            CompletionItem(CompletionItemKind.SEGMENT, "ABCs", "a <B> <C>"),
                            CompletionItem(CompletionItemKind.SEGMENT, "ABCs", "a <B> <C> <BCs>"),
                            CompletionItem(CompletionItemKind.SEGMENT, "DEFs", "<D> <E> f"),
                            CompletionItem(CompletionItemKind.SEGMENT, "DEFs", "<DEs> <D> <E> f"),
                            CompletionItem(CompletionItemKind.LITERAL, "'a'", "a"),
                            CompletionItem(CompletionItemKind.LITERAL, "'d'", "d"),
                        )
                    )
                }
            }
        }

    }

    @Test
    fun all() = executeTestSuit(testSuit)

    @Test
    fun single() = doTest(testSuit["Recursive 1"])
}