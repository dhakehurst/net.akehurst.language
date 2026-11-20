/**
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.processor.ansiC

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.processor.contextFromGrammarRegistry
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.api.processor.StyleString
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.scanner.api.ScannerKind
import net.akehurst.language.sppt.api.SharedPackedParseTree
import net.akehurst.language.sppt.api.SpptDataNode
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests the C11 grammar (language-repository/languages/ansiC/version_11)
 * with the sentences in 'valid' and 'invalid', and the grouping of expressions and statements.
 */
class test_AnsiC_11 {

    private companion object {
        val languageDir = File("../language-repository/languages/ansiC/version_11")
        val grammarStr by lazy { File(languageDir, "grammar.agl-grm").readText() }
        val styleStr by lazy { File(languageDir, "style.agl-sty").readText() }

        val processor: LanguageProcessor<Asm, SentenceContext> by lazy {
            val res = Agl.processorFromStringSimple(GrammarString(grammarStr), styleStr = StyleString(styleStr))
            assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
            res.processor!!
        }
    }

    private fun sentenceFiles(kind: String) = File(languageDir, kind).walkTopDown()
        .filter { it.isFile && it.name.endsWith(".c") }
        .sortedBy { it.path }
        .toList()

    /** the sentence with the expressions grouped in parentheses, using the first alternative of each node */
    private fun grouped(sppt: SharedPackedParseTree, text: String): String {
        val td = sppt.treeData
        fun render(n: SpptDataNode): String {
            val alts = td.childrenFor(n)
            if (alts.isEmpty()) return text.substring(n.startPosition, n.nextInputNoSkip).trim()
            val children = alts.first().second.map { render(it) }.filter { it.isNotEmpty() }
            val group = (n.rule.tag.endsWith("Expression") || n.rule.tag.endsWith("Statement")) && children.size > 1
            return if (group) "(" + children.joinToString(" ") + ")" else children.joinToString(" ")
        }
        return td.root?.let { render(it) } ?: ""
    }

    private fun assertGrouping(body: String, expected: String) {
        val sentence = "void f(void) { $body }"
        val res = processor.parse(sentence)
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        val actual = grouped(res.sppt!!, sentence).removePrefix("void f ( void ) ({ ").removeSuffix(" })")
        assertEquals(expected, actual)
    }

    @Test
    fun process_grammar() {
        val res = Agl.registry.agl.grammar.processor!!.process(grammarStr, Agl.options { semanticAnalysis { sentenceContext(contextFromGrammarRegistry(Agl.registry)) } })
        assertTrue(res.allIssues.errors.isEmpty(), res.allIssues.toString())
    }

    @Test
    fun processor_with_style() {
        assertTrue(processor.typesDomain.allDefinitions.isNotEmpty())
        assertEquals(ScannerKind.Classic, processor.scanner!!.kind)
    }

    @Test
    fun process_valid() {
        val files = sentenceFiles("valid")
        assertTrue(files.isNotEmpty(), "sentences not found in '${languageDir.absolutePath}'")
        val failed = files.associate { it.name to processor.process(it.readText()).allIssues.errors }.filterValues { it.isNotEmpty() }
        assertEquals(emptyMap(), failed, "valid sentences with errors")
    }

    @Test
    fun parse_invalid() {
        val files = sentenceFiles("invalid")
        assertTrue(files.isNotEmpty())
        val parsed = files.filter { processor.parse(it.readText()).issues.errors.isEmpty() }.map { it.name }
        assertEquals(emptyList(), parsed, "invalid sentences that parse")
    }

    @Test
    fun precedence_unary_postfix_multiplicative_additive() =
        assertGrouping("return *p++ + -a * b - c;", "(return (((* (p ++)) + ((- a) * b)) - c) ;)")

    @Test
    fun precedence_sizeof() = assertGrouping("return sizeof x + 1;", "(return ((sizeof x) + 1) ;)")

    @Test
    fun precedence_relational_equality_logical() =
        assertGrouping("return a < b == c && d || e;", "(return ((((a < b) == c) && d) || e) ;)")

    @Test
    fun precedence_bitwise_shift() = assertGrouping("return a & b ^ c | d << 1;", "(return (((a & b) ^ c) | (d << 1)) ;)")

    @Test
    fun conditional_is_right_associative() = assertGrouping("return a ? b : c ? d : e;", "(return (a ? b : (c ? d : e)) ;)")

    @Test
    fun assignment_is_right_associative() = assertGrouping("a = b += c;", "((a = (b += c)) ;)")

    @Test
    fun additive_is_left_associative() = assertGrouping("return a - b - c;", "(return ((a - b) - c) ;)")

    @Test
    fun cast_binds_tighter_than_multiplicative() = assertGrouping("return (long) -x * y;", "(return ((( long ) (- x)) * y) ;)")

    @Test
    fun postfix_chain() = assertGrouping("return !a->b[1].c(2);", "(return (! ((((a -> b) [ 1 ]) . c) ( 2 ))) ;)")

    @Test
    fun else_belongs_to_nearest_if() =
        assertGrouping("if (a) if (b) x(); else y();", "(if ( a ) (if ( b ) ((x ( )) ;) else ((y ( )) ;)))")
}
