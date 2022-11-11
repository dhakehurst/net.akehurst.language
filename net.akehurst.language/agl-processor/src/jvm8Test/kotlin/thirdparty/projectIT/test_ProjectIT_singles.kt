/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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
package net.akehurst.language.agl.processor.thirdparty.projectIT

import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.agl.processor.dot.test_Dot_Singles
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.file.DirectoryStream
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull


class test_ProjectIT_singles {

    companion object {

        private val grammarStr = this::class.java.getResource("/projectIT/PiEditGrammar.agl")?.readText() ?: error("File not found")

        var processor: LanguageProcessor<Any, Any> = Agl.processorFromString(grammarStr)

    }

    @Test
    fun expression_constant_string() {
        val goal = "expression"
        val sentence = """
            "hell"
        """.trimIndent()
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { "$it" })
        assertEquals(emptyList(), result.issues)
    }

    @Test
    fun expression_constant_number() {
        val goal = "expression"
        val sentence = """
            12345
        """.trimIndent()
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { "$it" })
        assertEquals(emptyList(), result.issues)
    }

    @Test
    fun expression_var() {
        val goal = "expression"
        val sentence = """
            variable
        """.trimIndent()
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { "$it" })
        assertEquals(emptyList(), result.issues)
    }

    @Test
    fun expression_instanceExpression() {
        val goal = "expression"
        val sentence = """
            var1 : var2
        """.trimIndent()
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { "$it" })
        assertEquals(emptyList(), result.issues)
    }

    @Test
    fun expression_functionExpression() {
        val goal = "expression"
        val sentence = """
            func1(a,d,v)
        """.trimIndent()
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { "$it" })
        assertEquals(emptyList(), result.issues)
    }

    @Test
    fun expression_navigationExpression() {
        val goal = "expression"
        val sentence = """
            a.b.c.d
        """.trimIndent()
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { "$it" })
        assertEquals(emptyList(), result.issues)
    }

    @Test
    fun expression_listExpression() {
        val goal = "expression"
        val sentence = """
            list a.b.c horizontal separator [,]
        """.trimIndent()
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { "$it" })
        assertEquals(emptyList(), result.issues)
    }

    @Test
    fun expression_tableExpression() {
        val goal = "expression"
        val sentence = """
            table a.b.c rows
        """.trimIndent()
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { "$it" })
        assertEquals(emptyList(), result.issues)
    }

    @Test
    fun templateText_literal() {
        val goal = "templateText"
        val sentence = """
          Insurance Product
        """.trimIndent()
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { "$it" })
        assertEquals(emptyList(), result.issues)
    }

    @Test
    fun literal_literal_fails_close_bracket() {
        val goal = "literal"
        val sentence = """
          Insurance Product
        ]
        """.trimIndent()

        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })

        assertEquals(
            listOf(
                LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.PARSE, InputLocation(20, 1, 2, 1), "^]", setOf("<EOT>"))
            ),
            result.issues
        )
    }

    @Test
    fun templateText_literal_fails_close_bracket() {
        val goal = "templateText"
        val sentence = """
          Insurance Product
        ]
        """.trimIndent()
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertEquals(
            listOf(
                LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.PARSE, InputLocation(20, 1, 2, 1), "^]", setOf("<EOT>"))
            ),
            result.issues
        )
    }

    @Test
    fun projection_literal() {
        val goal = "projection"
        val sentence = """
        [
          Insurance Product
        ]
        """.trimIndent()
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { "$it" })
        assertEquals(emptyList(), result.issues)
    }

    @Test
    fun projection_lit_var_lit() {
        val goal = "projection"
        val sentence = """
        [
          Insurance Product ${"$"}{name} USES
        ]
        """.trimIndent()
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { "$it" })
        assertEquals(emptyList(), result.issues)
    }

    @Test
    fun projection_lit_list_lit() {
        val goal = "projection"
        val sentence = """
        [
          Insurance Product ${"$"}{name} USES
        ]
        """.trimIndent()
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { "$it" })
        assertEquals(emptyList(), result.issues)
    }

    @Test
    fun projection_1() {
        val goal = "projection"
        val sentence = """
        [
          ${"$"}{list a.b.c }
        ]
        """.trimIndent()
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { "$it" })
        assertEquals(emptyList(), result.issues)
    }

    @Test
    fun projection_() {
        val goal = "projection"
        val sentence = """
        [
          Insurance Product ${"$"}{name} ( public name: ${"$"}{productName} ) USES ${"$"}{list basedOn horizontal separator[, ]}
        ]
        """.trimIndent()
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { "$it" })
        assertEquals(emptyList(), result.issues)
    }

}
