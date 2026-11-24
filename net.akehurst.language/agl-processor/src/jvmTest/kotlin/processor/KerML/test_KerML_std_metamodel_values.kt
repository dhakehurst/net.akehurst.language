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

package net.akehurst.language.agl.processor.KerML

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.AsmTransformString
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.api.processor.TypesString
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * values in the ASM of the 'std-metamodel' variants (standard grammar + asm-transform onto the metamodel types):
 * multiplicity bounds, Boolean literal values, and the filter package of an import with filters
 */
class test_KerML_std_metamodel_values {

    companion object {
        val SENTENCE = "package P { public import A::*[@F]; feature f [1..2]; feature b = true; }"

        fun asmText(version: String): String {
            val dir = File("../language-repository/languages/KerML/$version/languages/std-metamodel")
            val res = Agl.processorFromStringSimple(
                GrammarString(File(dir, "grammar.agl-grm").readText()),
                typeStr = TypesString(File(dir, "types.agl-typ").readText()),
                transformStr = AsmTransformString(File(dir, "transform.agl-trf").readText()),
            )
            assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
            val r = res.processor!!.process(SENTENCE, Agl.options { semanticAnalysis { enabled(false) } })
            assertTrue(r.allIssues.errors.isEmpty(), r.allIssues.toString())
            return r.asm!!.asString().lines().filter { !it.trim().startsWith("_identity") }.joinToString("\n")
        }

        fun check(version: String) {
            val asm = asmText(version)
            // multiplicity '[1..2]': both bounds
            assertTrue(asm.contains("value = 1") && asm.contains("value = 2"), "multiplicity bounds missing ($version):\n$asm")
            // 'true' is a Boolean, not the String 'true'
            assertTrue(asm.contains("value = true") && !asm.contains("value = 'true'"), "LiteralBoolean value is not a Boolean ($version):\n$asm")
            // 'import A::*[@F]': a NamespaceImport of a filter package, which owns the import of 'A' and the filter
            assertTrue(asm.contains("importedNamespace = [ 'A' ]") && asm.contains(":ElementFilterMembership") && asm.contains("type = [ 'F' ]"), "filtered import missing ($version):\n$asm")
        }
    }

    @Test
    fun v1_0_2026_04() = check("v1.0_2026-04")

    @Test
    fun v1_1_2026_09() = check("v1.1_2026-09")
}
