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

package net.akehurst.language.agl.simple

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.CrossReferenceString
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.agl.semanticAnalyser.contextFromTypesDomain
import net.akehurst.language.reference.asm.CrossReferenceDomainDefault
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 'global-prefix '$'': a referring name whose first part is the prefix is looked up from the root scopes only, without that part
 * (e.g. KerML/SysML '$::Objects::Object', C# 'global::System').
 */
class test_ReferenceResolverSimple_globalPrefix {

    private companion object {
        val grammarStr = GrammarString(
            """
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                unit = element* ;
                element = package | def | use ;
                package = 'package' ID '{' element* '}' ;
                def = 'def' ID ;
                use = 'use' ID ':' qname ;
                qname = [part / '::']+ ;
                part = ID | '$' ;
                leaf ID = "[A-Za-z_][A-Za-z0-9_]*" ;
            }
            """.trimIndent()
        )

        fun referenceStr(prefixLine: String) = CrossReferenceString(
            """
            namespace test.Test
                $prefixLine
                identify Package by id
                identify Def by id
                identify Use by id
                scope Package { }
                references {
                    in Use { property qname refers-to Def }
                }
            """.trimIndent()
        )

        // the root package Objects has Root, the nested package P.Objects has Inner
        const val packages = "package Objects { def Root } package P { package Objects { def Inner } "

        fun errors(prefixLine: String, uses: String): List<String> {
            val processor = Agl.processorFromStringSimple(grammarDefinitionStr = grammarStr, referenceStr = referenceStr(prefixLine)).let {
                assertTrue(it.issues.errors.isEmpty(), it.issues.toString())
                it.processor!!
            }
            val result = processor.process("$packages $uses }", Agl.options { semanticAnalysis { sentenceContext(contextAsmSimple()) } })
            assertTrue(result.allIssues.errors.none { it.phase.name != "SEMANTIC_ANALYSIS" }, result.allIssues.toString())
            return result.allIssues.errors.map { it.message }
        }

        fun definitionErrors(references: String): List<String> {
            val p = Agl.processorFromStringSimple(grammarDefinitionStr = grammarStr).processor!!
            val res = CrossReferenceDomainDefault.fromString(contextFromTypesDomain(p.typesDomain), CrossReferenceString(references.trimIndent()))
            return res.allIssues.errors.map { it.message }
        }
    }

    @Test
    fun without_prefix__nearest_is_used() {
        assertEquals(emptyList(), errors("global-prefix '$'", "use a : Objects::Inner"))
        // the nearer package Objects hides the root one
        assertEquals(1, errors("global-prefix '$'", "use a : Objects::Root").size)
    }

    @Test
    fun with_prefix__from_the_root() {
        assertEquals(emptyList(), errors("global-prefix '$'", "use b : $::Objects::Root"))
        val e = errors("global-prefix '$'", "use b : $::Objects::Inner")
        assertEquals(1, e.size, e.toString())
        assertTrue(e[0].startsWith("Reference '$.Objects.Inner' not resolved"), e.toString())
    }

    @Test
    fun no_global_prefix_line__prefix_is_a_name_part() {
        // '$' is then looked up as a name, and there is none
        assertEquals(1, errors("", "use b : $::Objects::Root").size)
    }

    @Test
    fun prefix_alone_is_a_name() {
        // a name of one part is never global
        assertEquals(1, errors("global-prefix '$'", "use b : $").size)
    }

    @Test
    fun asString_round_trip() {
        val processor = Agl.processorFromStringSimple(grammarDefinitionStr = grammarStr, referenceStr = referenceStr("global-prefix '$'")).processor!!
        val str = processor.crossReferenceDomain.asString()
        assertTrue(str.contains("global-prefix '$'"), str)
        val again = Agl.processorFromStringSimple(grammarDefinitionStr = grammarStr, referenceStr = CrossReferenceString(str))
        assertTrue(again.issues.errors.isEmpty(), "$str\n${again.issues}")
        assertEquals(str, again.processor!!.crossReferenceDomain.asString())
    }

    @Test
    fun two_prefixes__error() {
        val e = definitionErrors(
            """
            namespace test.Test
                global-prefix '$'
                global-prefix 'global'
                identify Def by id
            """
        )
        assertEquals(1, e.size, e.toString())
        assertTrue(e[0].contains("more than one 'global-prefix'"), e.toString())
    }
}
