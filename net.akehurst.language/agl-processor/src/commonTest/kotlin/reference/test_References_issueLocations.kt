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

package net.akehurst.language.agl.language.reference

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.CrossReferenceString
import net.akehurst.language.api.processor.GrammarString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * issue #20: errors in a references definition point at the part of the definition that is wrong.
 */
class test_References_issueLocations {

    private companion object {
        val grammarStr = """
            namespace test
            grammar Refs {
                skip leaf WS = "\s+" ;
                unit = decl* ;
                decl = def | use ;
                def = 'def' ID ;
                use = 'use' ref ;
                leaf ref = ID ;
                leaf ID = "[a-zA-Z]+" ;
            }
        """.trimIndent()

        fun errorsFor(refs: String) = run {
            val pr = Agl.processorFromStringSimple(GrammarString(grammarStr), referenceStr = CrossReferenceString(refs))
            assertTrue(pr.issues.errors.isEmpty(), pr.issues.toString())
            val proc = pr.processor!!
            proc.crossReferenceDomain
            proc.allIssues.errors
        }
    }

    @Test
    fun referring_property_not_found__location_of_property() {
        val refs = """
            namespace test.Refs
                identify Def by id
                references {
                    in Use { property reff refers-to Def }
                }
        """.trimIndent()
        val errs = errorsFor(refs)
        assertEquals(1, errs.size, errs.joinToString("\n"))
        assertTrue(errs[0].message.contains("referring property 'reff' not found"), errs[0].message)
        val loc = assertNotNull(errs[0].location, errs[0].toString())
        assertEquals(4, loc.line)
        assertEquals(refs.indexOf("reff"), loc.position)
    }

    @Test
    fun referring_property_not_found_in_when__location_of_property() {
        val refs = """
            namespace test.Refs
                identify Def by id
                references {
                    in Use { property reff when { else -> refers-to Def } }
                }
        """.trimIndent()
        val errs = errorsFor(refs)
        assertEquals(1, errs.size, errs.joinToString("\n"))
        val loc = assertNotNull(errs[0].location, errs[0].toString())
        assertEquals(refs.indexOf("reff"), loc.position)
    }

    @Test
    fun namespace_not_found__location_of_namespace_name() {
        val refs = """
            namespace test.Ref
                identify Def by id
        """.trimIndent()
        val errs = errorsFor(refs)
        assertEquals(1, errs.size, errs.joinToString("\n"))
        assertEquals("Namespace 'test.Ref' not found in the types domain.", errs[0].message)
        val loc = assertNotNull(errs[0].location, errs[0].toString())
        assertEquals(refs.indexOf("test.Ref"), loc.position)
        assertEquals("test.Ref".length, loc.length)
    }

}
