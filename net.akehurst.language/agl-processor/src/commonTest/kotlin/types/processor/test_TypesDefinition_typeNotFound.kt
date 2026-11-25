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

package net.akehurst.language.types.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.api.processor.TypesString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * issue #20: a type named in a types definition of a language must be found,
 * once std, the grammar namespaces and the types created from the grammar are part of the types domain.
 */
class test_TypesDefinition_typeNotFound {

    private companion object {
        val grammarStr = """
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                S = 'a' NAME ;
                leaf NAME = "[a-z]+" ;
            }
        """.trimIndent()

        fun typeErrors(typesStr: String): List<String> {
            val pr = Agl.processorFromStringSimple(GrammarString(grammarStr), typeStr = TypesString(typesStr))
            assertTrue(pr.issues.errors.isEmpty(), pr.issues.toString())
            val proc = pr.processor!!
            proc.typesDomain // builds the types (and transform) domain
            return proc.allIssues.errors.map { it.message }
        }
    }

    @Test
    fun all_found__no_errors() {
        val errs = typeErrors(
            """
            namespace test.Test
              import std
              data Person { name: String  friend: List<Person>  s: S }
              data Box<T> { content: T  others: List<T> }
              union U { Person | Integer }
            """.trimIndent()
        )
        assertEquals(emptyList(), errs)
    }

    @Test
    fun property_type_misspelled() {
        val errs = typeErrors(
            """
            namespace test.Test
              import std
              data Person { name: Strin }
            """.trimIndent()
        )
        assertEquals(1, errs.size, errs.joinToString("\n"))
        assertTrue(errs[0].contains("'Strin'") && errs[0].contains("property 'name' of 'Person'"), errs[0])
    }

    @Test
    fun type_argument_supertype_and_alternative_not_found() {
        val errs = typeErrors(
            """
            namespace test.Test
              import std
              data Person : Persn { friend: List<Persn> }
              union U { Person | Intger }
            """.trimIndent()
        )
        assertEquals(3, errs.size, errs.joinToString("\n"))
        assertTrue(errs.any { it.contains("supertype of 'Person'") }, errs.joinToString("\n"))
        assertTrue(errs.any { it.contains("'Persn'") && it.contains("property 'friend'") }, errs.joinToString("\n"))
        assertTrue(errs.any { it.contains("'Intger'") && it.contains("union 'U'") }, errs.joinToString("\n"))
    }

    @Test
    fun error_has_location_and_reaches_process_result() {
        val pr = Agl.processorFromStringSimple(
            GrammarString(grammarStr), typeStr = TypesString(
                """
                namespace test.Test
                  import std
                  data Person { name: Strin }
                """.trimIndent()
            )
        )
        val proc = pr.processor!!
        val res = proc.process("a bob")
        val err = res.allIssues.errors.firstOrNull { it.message.contains("'Strin'") }
        assertNotNull(err, res.allIssues.toString())
        val loc = proc.allIssues.errors.first { it.message.contains("'Strin'") }.location
        assertNotNull(loc)
        assertEquals(3, loc.line)
    }

}
