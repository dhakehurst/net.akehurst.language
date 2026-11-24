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

package net.akehurst.language.agl.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.grammar.api.GrammarRuleName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * issue #66: LanguageProcessor.targetTypeFor(goalRuleName) gives the asm type that process() gives for a goal rule
 */
class test_LanguageProcessor_targetTypeFor {

    private companion object {
        fun processorFor(grammarStr: String) = Agl.processorFromStringSimple(GrammarString(grammarStr.trimIndent())).let {
            assertTrue(it.issues.errors.isEmpty(), it.issues.toString())
            it.processor!!
        }

        val grammarStr = """
            namespace test
            grammar Base {
                skip leaf WS = "\s+" ;
                leaf ID = "[a-z]+" ;
                name = ID ;
            }
            grammar Test : Base {
                unit = decl* ;
                decl = 'decl' name ;
            }
        """
    }

    @Test
    fun default_goal__first_non_skip_rule() {
        val proc = processorFor(grammarStr)
        assertEquals("Unit", proc.targetTypeFor()?.typeName?.value)
        // the type resolves to a definition in the processor's types domain
        assertEquals("test.Test.Unit", proc.targetTypeFor()?.resolvedDefinitionOrNull?.qualifiedName?.value)
    }

    @Test
    fun default_goal__grammar_option() {
        val proc = processorFor(
            """
            namespace test
            grammar Test {
                #defaultGoalRule: decl
                skip leaf WS = "\s+" ;
                leaf ID = "[a-z]+" ;
                unit = decl* ;
                decl = 'decl' ID ;
            }
            """
        )
        assertEquals("Decl", proc.targetTypeFor()?.typeName?.value)
    }

    @Test
    fun named_goal() {
        val proc = processorFor(grammarStr)
        assertEquals("Decl", proc.targetTypeFor(GrammarRuleName("decl"))?.typeName?.value)
    }

    @Test
    fun named_goal__rule_of_extended_grammar() {
        val proc = processorFor(grammarStr)
        assertEquals("Name", proc.targetTypeFor(GrammarRuleName("name"))?.typeName?.value)
    }

    @Test
    fun named_goal__leaf() {
        val proc = processorFor(grammarStr)
        assertEquals("String", proc.targetTypeFor(GrammarRuleName("ID"))?.typeName?.value)
    }

    @Test
    fun named_goal__unknown_rule__null() {
        val proc = processorFor(grammarStr)
        assertNull(proc.targetTypeFor(GrammarRuleName("unknown")))
    }
}
