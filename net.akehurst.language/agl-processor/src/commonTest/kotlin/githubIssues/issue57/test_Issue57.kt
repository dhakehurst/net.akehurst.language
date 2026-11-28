/**
 * Copyright (C) 2025 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.processor.githubIssues.issue57

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.CompletionItem
import net.akehurst.language.api.processor.CompletionItemKind
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.api.processor.ProcessOptions
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class test_Issue57 {

    private companion object {
        val grammarString = """
            namespace org.example.brackets.agl

            grammar Brackets {
            	sentence = '[' 'x' ']';
            	
            	skip WHITESPACE = "\s+";
            }
        """.trimIndent()

        fun doTest(sentence: String, position: Int, expected: Set<String>) {
            println("=== sentence: '$sentence' position: $position ===")
            val p = Agl.processorFromStringSimple(
                GrammarString(grammarString)
            ).let {
                check(it.issues.errors.isEmpty()) { it.issues.toString() }
                it.processor!!
            }
            val res = p.expectedItemsAt(sentence, position)
            assertTrue(res.issues.errors.isEmpty(), res.issues.toString())

            res.items.forEach {
                println("${it.kind}: '${it.text}' (${it.label})")
            }
            val actual = res.items.map { it.text }.toSet()

            assertEquals(expected, actual)
        }

        data class TestData(
            val sentence: String,
            val position: Int,
            val expected: Set<String>
        )

        val testData = listOf(
            TestData("", 0, setOf("[ x ]", "[")),
            TestData("[", 1, setOf("x")),
            TestData("[ ", 2, setOf("x")),
            TestData("[x", 2, setOf("]")),
            TestData("[ ", 1, setOf("x")),
            TestData("[]", 1, setOf("x")),
            TestData("[  ]", 3, setOf("x")),
            TestData("[x]", 3, setOf()),
            TestData("[x] ", 4, setOf()),
            TestData("[ x", 3, setOf("]")),
            TestData("[ x ", 4, setOf("]")),
            TestData("[ x ]", 5, setOf()),
            TestData("[ x ] ", 6, setOf()),
        )

        val grammarBracketsComplex = """
            namespace org.example.bracketscomplex.agl

            grammar BracketsComplex {
                sentence = word | bracketed;
                bracketed = '[' word* ']';
                word = NAME;

                leaf NAME = NAME_CHAR+;
                leaf NAME_CHAR = "[-.0-9]";

                skip WHITESPACE = "\s+";
            }
        """.trimIndent()

        val grammarDoubleBrackets = """
            namespace org.example.doublebrackets.agl

            grammar DoubleBrackets {
                sentence = word | bracketed | '(' sentence ')';
                bracketed = '[' word* ']' | '[' '(' word+ ')' ']';
                word = NAME;

                leaf NAME = NAME_CHAR+;
                leaf NAME_CHAR = "[0-9]";

                skip WHITESPACE = "\s+";
            }
        """.trimIndent()

        fun processor(grammar: String) = Agl.processorFromStringSimple(GrammarString(grammar)).let {
            check(it.issues.errors.isEmpty()) { it.issues.toString() }
            it.processor!!
        }

        /** the text of each item, with the name of the rule that owns the grammar item of a literal, e.g. ']_bracketed' */
        fun textsWithOwner(items: List<CompletionItem>) = items.map {
            when (it.kind) {
                CompletionItemKind.LITERAL -> "${it.text}_${it.detail?.owningRule?.name}"
                else -> it.text
            }
        }.toSet()

        // the sentences (and positions) of the issue
        val issueSentences = listOf(
            "" to 0, "[" to 1, "[ " to 2, "[x" to 2, "[x " to 3, "[( " to 3, "[(x " to 4, "[(x) " to 5,
            "[ " to 1, "[]" to 1, "[  ]" to 3, "[x]" to 3, "[x] " to 4
        )
    }

    @Test
    fun test() {
        for (data in testData) {
            doTest(data.sentence, data.position, data.expected)
        }
    }

    @Test
    fun test2() {
        val p = Agl.processorFromStringSimple(
            GrammarString("""
                namespace org.example.bracketscomplex.agl
                
                grammar BracketsComplex {
                    sentence = word | bracketed;
                    bracketed = '[' word* ']';
                    word = NAME;
                    
                    leaf NAME = NAME_CHAR+; //'12345'; //NAME_CHAR+;
                    leaf NAME_CHAR = "[-.0-9]";
                    
                    skip WHITESPACE = "\s+";
                }
                """.trimIndent())
        ).let {
            check(it.issues.errors.isEmpty()) { it.issues.toString() }
            it.processor!!
        }

        val sentence = "[ 12345 ] "
        for(pos in 4 until 11) {
            println("=== sentence: '$sentence' position: $pos ===")
            val res = p.expectedItemsAt(sentence, pos)
            assertTrue(res.issues.errors.isEmpty(), res.issues.toString())

            res.items.forEach {
                println("${it.kind}: '${it.text}' (${it.label})")
            }
        }

    }

    @Test
    fun test_answer_1() {
        val p = Agl.processorFromStringSimple(
            GrammarString("""
                    namespace org.example.brackets.agl
        
                    grammar Brackets {
                        sentence = '[' 'uvwxyz' ']';
                        
                        skip WHITESPACE = "\s+";
                    }
                """.trimIndent())
        ).let {
            check(it.issues.errors.isEmpty()) { it.issues.toString() }
            it.processor!!
        }
        val res = p.expectedItemsAt("[ uvwxyz ", 8)
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())

        res.items.forEach {
            println("${it.kind}: '${it.text}' (${it.label})")
        }


    }

    @Test
    fun BracketsComplex_no_exception_for_issue_sentences() {
        // issue #57: a ClassCastException (SimpleListDefault to TangibleItem) was thrown in SpineDefault.expectedNextLeafNonTerminalOrTerminal
        val p = processor(grammarBracketsComplex)
        for ((sentence, position) in issueSentences) {
            val res = p.expectedItemsAt(sentence, position)
            assertTrue(res.issues.errors.isEmpty(), "'$sentence'@$position: ${res.issues}")
        }
    }

    @Test
    fun BracketsComplex() {
        val p = processor(grammarBracketsComplex)
        val testData = listOf(
            TestData("", 0, setOf("<bracketed>", "<word>", "[", "<NAME>")),
            TestData("[", 1, setOf("<word>", "]", "<NAME>")),
            TestData("[ ", 2, setOf("<word>", "]", "<NAME>")),
            TestData("[ ", 1, setOf("<word>", "]", "<NAME>")),
            TestData("[  ]", 3, setOf("<word>", "]", "<NAME>")),
            TestData("[1 ", 3, setOf("<word>", "]", "<NAME>")),
            TestData("[1]", 3, setOf()),
            TestData("[1] ", 4, setOf()),
        )
        for (data in testData) {
            val res = p.expectedItemsAt(data.sentence, data.position)
            assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
            assertEquals(data.expected, res.items.map { it.text }.toSet(), "'${data.sentence}'@${data.position}")
        }
    }

    @Test
    fun DoubleBrackets_no_exception_for_issue_sentences() {
        val p = processor(grammarDoubleBrackets)
        for ((sentence, position) in issueSentences) {
            val res = p.expectedItemsAt(sentence, position)
            assertTrue(res.issues.errors.isEmpty(), "'$sentence'@$position: ${res.issues}")
        }
    }

    @Test
    fun DoubleBrackets() {
        // issue #57: the owning rule of a literal is the rule it is expected in ('(' of sentence at the start, '(' of bracketed after '['),
        // and only the items of the alternative being parsed are expected (after '[' a <word> of '[' word* ']')
        val p = processor(grammarDoubleBrackets)
        val testData = listOf(
            TestData("", 0, setOf("<bracketed>", "( <sentence> )", "<word>", "(_sentence", "[_bracketed", "<NAME>")),
            TestData("[", 1, setOf("<word>", "(_bracketed", "]_bracketed", "<NAME>")),
            TestData("[ ", 2, setOf("<word>", "(_bracketed", "]_bracketed", "<NAME>")),
            TestData("[ ", 1, setOf("<word>", "(_bracketed", "]_bracketed", "<NAME>")),
            TestData("[1 ", 3, setOf("<word>", "]_bracketed", "<NAME>")),
            TestData("[( ", 3, setOf("<word>", "<NAME>")),
            TestData("[(1 ", 4, setOf("<word>", ")_bracketed", "<NAME>")),
            TestData("[(1) ", 5, setOf("]_bracketed")),
            TestData("[1]", 3, setOf()),
            TestData("[1] ", 4, setOf()),
        )
        for (data in testData) {
            val res = p.expectedItemsAt(data.sentence, data.position)
            assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
            assertEquals(data.expected, textsWithOwner(res.items), "'${data.sentence}'@${data.position}")
        }
    }

    @Test
    fun BracketsComplex_pattern_continued() {
        // issue #57 (IC1): a pattern whose match the typed word could be the start of is offered from the start of the word
        val p = processor(grammarBracketsComplex)
        val testData = listOf(
            TestData("1", 1, setOf("<NAME>@0")),
            TestData("[1", 2, setOf("<NAME>@1", "<word>@2", "]@2", "<NAME>@2")),
            TestData("[12", 3, setOf("<NAME>@1", "<word>@3", "]@3", "<NAME>@3")),
            TestData("[1 2", 4, setOf("<NAME>@3", "<word>@4", "]@4", "<NAME>@4")),
            TestData("[1 ", 3, setOf("<word>@3", "]@3", "<NAME>@3")),
        )
        for (data in testData) {
            val res = p.expectedItemsAt(data.sentence, data.position)
            assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
            assertEquals(data.expected, res.items.map { "${it.text}@${it.offeredPosition}" }.toSet(), "'${data.sentence}'@${data.position}")
        }
    }

    @Test
    fun pattern_partly_typed_continued() {
        // issue #57 (IC1, IC2): a partly typed word that is not yet a match of the pattern, but can be continued to one
        val p = processor("""
            namespace test
            grammar Test {
                sentence = '[' NUMBER* ']';
                leaf NUMBER = "[0-9]+\.[0-9]+";
                skip WHITESPACE = "\s+";
            }
        """.trimIndent())
        val testData = listOf(
            TestData("[1", 2, setOf("<NUMBER>@1")),
            TestData("[1.", 3, setOf("<NUMBER>@1")),
            TestData("[1.5", 4, setOf("<NUMBER>@1", "<NUMBER>@4", "]@4")),
        )
        for (data in testData) {
            val res = p.expectedItemsAt(data.sentence, data.position)
            assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
            assertEquals(data.expected, res.items.map { "${it.text}@${it.offeredPosition}" }.toSet(), "'${data.sentence}'@${data.position}")
        }
    }

    @Test
    fun omitLiteralsAlreadyAfterPosition() {
        // issue #57 (IC3a): option to leave out a literal that is already the text after the position (after skip text)
        val options = Agl.options<Asm, SentenceContext> { completionProvider { omitLiteralsAlreadyAfterPosition(true) } }
        fun texts(p: LanguageProcessor<Asm, SentenceContext>, sentence: String, position: Int, opts: ProcessOptions<Asm, SentenceContext>?) =
            p.expectedItemsAt(sentence, position, opts).let {
                assertTrue(it.issues.errors.isEmpty(), it.issues.toString())
                it.items.map { it.text }.toSet()
            }

        val pBrackets = processor(grammarString)
        assertEquals(setOf("x"), texts(pBrackets, "[]", 1, options))
        assertEquals(setOf("]"), texts(pBrackets, "[x", 2, options), "nothing after the position")

        val pComplex = processor(grammarBracketsComplex)
        // the default keeps ']'
        assertEquals(setOf("<word>", "]", "<NAME>"), texts(pComplex, "[]", 1, null))
        assertEquals(setOf("<word>", "<NAME>"), texts(pComplex, "[]", 1, options))
        assertEquals(setOf("<word>", "<NAME>"), texts(pComplex, "[  ]", 2, options), "skip text after the position")
        assertEquals(setOf("<word>", "]", "<NAME>"), texts(pComplex, "[ 1 ]", 2, options), "a different text after the position")

        val pIf = processor("""
            namespace test
            grammar Test {
                sentence = 'if' NAME? ;
                leaf NAME = "[a-z]+";
                skip WHITESPACE = "\s+";
            }
        """.trimIndent())
        assertEquals(setOf(), texts(pIf, "if x", 0, options).filter { it == "if" }.toSet())
        assertEquals(setOf("if"), texts(pIf, "ifx", 0, options).filter { it == "if" }.toSet(), "'if' is only the start of the word 'ifx'")
    }

    @Test
    fun BracketsComplex_invalid_text_before_position() {
        // issue #57 (IC4, q21): 'x' is not a NAME_CHAR, the text is invalid from position 1;
        // the items expected at the first error position are offered (from that position), wherever the position is after it
        val p = processor(grammarBracketsComplex)
        val expected = setOf("<word>@1", "]@1", "<NAME>@1")
        for ((sentence, position) in listOf("[x" to 2, "[x " to 3, "[x]" to 3, "[x] " to 4)) {
            val res = p.expectedItemsAt(sentence, position)
            assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
            assertEquals(expected, res.items.map { "${it.text}@${it.offeredPosition}" }.toSet(), "'$sentence'@$position")
            assertEquals(1, res.offeredPosition, "'$sentence'@$position")
        }
    }
}
