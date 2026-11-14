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

package net.akehurst.language.types

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.GrammarString
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The types generated from each grammar in the language-repository and the test resources:
 * TypesDomain.asString() gives text that the AglTypes processor parses back to a TypesDomain with the same asString().
 * Grammars that do not give a processor are skipped.
 */
class test_TypesAsStringRoundTripFromGrammars {

    private val grammarFiles: List<File>
        get() {
            val repo = File("../language-repository/languages").walkTopDown().filter { it.isFile && it.name.startsWith("grammar", true) && it.name.endsWith(".agl") }
            val resources = File("src/jvmTest/resources").walkTopDown().filter { it.isFile && it.name.endsWith(".agl") }
            return (repo + resources).sortedBy { it.path }.toList()
        }

    @Test
    fun all_grammars() {
        val files = grammarFiles
        assertTrue(files.size > 10, "grammar files not found from '${File(".").absolutePath}'")
        var count = 0
        for (f in files) {
            val p = runCatching { Agl.processorFromStringSimple(GrammarString(f.readText())) }.getOrNull()
            if (null == p || p.issues.errors.isNotEmpty() || null == p.processor) continue // grammar has errors
            count++
            val expected = p.processor!!.typesDomain.asString()
            val res = Agl.registry.agl.types.processor!!.process(expected)
            assertTrue(res.allIssues.errors.isEmpty(), "${f.path}: ${res.allIssues}")
            assertEquals(expected, res.asm!!.asString(), f.path)
        }
        println("round-trip of types from $count grammars")
        assertTrue(count > 10)
    }
}
