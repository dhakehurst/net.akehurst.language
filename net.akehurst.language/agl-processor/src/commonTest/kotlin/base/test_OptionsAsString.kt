/*
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.akehurst.language.base.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.base.api.Domain
import net.akehurst.language.base.api.OptionHolder
import net.akehurst.language.base.asm.OptionHolderDefault
import net.akehurst.language.base.asm.optionsAsString
import net.akehurst.kotlinx.utils.Indent
import net.akehurst.language.issues.api.LanguageIssueKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * asString of each agl DSL writes the options (`#name: value`) of the unit, of each namespace and of each definition,
 * so that processing the result again gives the same options.
 */
class test_OptionsAsString {

    private fun ownOptionsOf(domain: Domain<*, *>): List<Map<String, Any>> =
        listOf(domain.options.ownOptions) +
                domain.namespace.flatMap { ns -> listOf(ns.options.ownOptions) + ns.definition.map { it.options.ownOptions } }

    private fun <A : Domain<*, *>> test(proc: LanguageProcessor<A, *>, sentence: String, expected: String) {
        val res1 = proc.process(sentence)
        assertTrue(res1.allIssues.none { it.kind == LanguageIssueKind.ERROR }, res1.allIssues.toString())
        val asm1 = assertNotNull(res1.asm)
        val actual = asm1.asString()
        assertEquals(expected, actual)

        val res2 = proc.process(actual)
        assertTrue(res2.allIssues.none { it.kind == LanguageIssueKind.ERROR }, res2.allIssues.toString())
        val asm2 = assertNotNull(res2.asm)
        assertEquals(ownOptionsOf(asm1), ownOptionsOf(asm2))
        assertEquals(actual, asm2.asString())
    }

    private fun holder(vararg options: Pair<String, Any>): OptionHolder = OptionHolderDefault(null, mapOf(*options))

    @Test
    fun optionsAsString_empty() {
        assertEquals("", optionsAsString(holder(), Indent("", "  ")))
    }

    @Test
    fun optionsAsString_values() {
        val h = holder(
            "flag" to true,
            "off" to false,
            "id" to "abc",
            "str" to "hello world",
            "strTrue" to "true",
            "int" to 3L,
            "real" to 1.5
        )
        val expected = """
            |  #flag
            |  #off: false
            |  #id: abc
            |  #str: 'hello world'
            |  #strTrue: 'true'
            |  #int: 3
            |  #real: 1.5
            |""".trimMargin()
        assertEquals(expected, optionsAsString(h, Indent("", "  ").inc))
    }

    @Test
    fun optionsAsString_parentNotWritten() {
        val parent = holder("p" to true)
        val h = OptionHolderDefault(parent, mapOf("c" to true))
        assertEquals(true, h.get<Boolean>("p"))
        assertEquals("#c\n", optionsAsString(h, Indent("", "  ")))
    }

    @Test
    fun grammar() = test(
        Agl.registry.agl.grammar.processor!!,
        """
            #u1
            #u2: false
            namespace test.g
              #n1: abc
              #n2: 'hello world'
              #n3: 'true'
              #n4: 2
              #n5: 1.5
              grammar G {
                #defaultGoalRule: a
                a = 'a' ;
              }
        """.trimIndent(),
        """
            #u1
            #u2: false
            namespace test.g
              #n1: abc
              #n2: 'hello world'
              #n3: 'true'
              #n4: 2
              #n5: 1.5
              grammar G {
                #defaultGoalRule: a
                a = 'a' ;
              }
        """.trimIndent()
    )

    @Test
    fun grammar_noOptions() = test(
        Agl.registry.agl.grammar.processor!!,
        """
            namespace test.g
              grammar G {
                a = 'a' ;
              }
        """.trimIndent(),
        """
            namespace test.g
              grammar G {
                a = 'a' ;
              }
        """.trimIndent()
    )

    @Test
    fun asmTransform() = test(
        Agl.registry.agl.asmTransform.processor!!,
        """
            namespace test.a
              #create-missing-types
              asm-transform T {
                #override-default-transform
              }
        """.trimIndent(),
        "namespace test.a\n  #create-missing-types\n  asm-transform T {\n    #override-default-transform\n\n  }"
    )

    @Test
    fun completion() = test(
        Agl.registry.agl.completion.processor!!,
        """
            #u1
            namespace test.c
              #n1: abc
              completion C {
                #c1
              }
        """.trimIndent(),
        """
            #u1
            namespace test.c
              #n1: abc
              completion C {
                #c1
              }
        """.trimIndent()
    )

    @Test
    fun style() = test(
        Agl.registry.agl.style.processor!!,
        """
            namespace test.s
              #n1: abc
              styles S {
              }
        """.trimIndent(),
        "namespace test.s\n  #n1: abc\n  styles S {\n\n  }"
    )

    @Test
    fun types() = test(
        Agl.registry.agl.types.processor!!,
        """
            #u1
            namespace test.t
              #n1: abc
              data A {}
        """.trimIndent(),
        "#u1\nnamespace test.t\n  #n1: abc\n  \n  data A {\n    \n  }"
    )

    @Test
    fun references() = test(
        Agl.registry.agl.crossReference.processor!!,
        """
            #u1
            namespace test.r
              identify A by id
        """.trimIndent(),
        """
            #u1
            namespace test.r
              identify A by id
        """.trimIndent()
    )
}
