package agl.simple

import net.akehurst.language.api.processor.CompletionItem
import net.akehurst.language.api.processor.CompletionItemKind
import testFixture.data.executeTestSuit
import testFixture.data.testSuit
import kotlin.test.Test

class test_CompletionProviderSimple_with_depth {

    private companion object {

        val grammarStr = """
            namespace test
            grammar Test {
                S = ABC DEF ;
                ABC = A '.' BC ;
                DEF = DE F '.' ;
                BC = B '.' C '.' ;
                DE = D '.' E '.' ;
                A = 'a' ;
                B = 'b' ; 
                C = 'c' ;
                D = 'd' ;
                E = 'e' ;
                F = 'f' ;
            }
        """

        val testSuit = testSuit {
            testData("") {
                grammarStr(grammarStr)
                sentencePass("") {
                    expectedCompletionItems(listOf(
                        CompletionItem(CompletionItemKind.SEGMENT,"<ABC> <DEF>","S"),
                        CompletionItem(CompletionItemKind.LITERAL,"a","'a'"),
                    ))
                }
            }
        }

    }

    @Test
    fun all() = executeTestSuit(testSuit)

}