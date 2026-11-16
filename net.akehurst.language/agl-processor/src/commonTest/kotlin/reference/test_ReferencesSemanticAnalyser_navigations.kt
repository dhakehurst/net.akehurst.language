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
import net.akehurst.language.agl.semanticAnalyser.contextFromTypesDomain
import net.akehurst.language.api.processor.CrossReferenceString
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.reference.asm.CrossReferenceDomainDefault
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Checking (semantic analysis) of the navigations in a references definition other than 'identify ... by' and the referring property:
 * 'when', 'from', 'else', 'visibility', 'alias-for', and the navigations of 'import', 'inherit' and 'import-into-scope'.
 * A property that does not exist is an error. Special values ('$owner', '$container', '$root', '$previous') are not checked.
 */
class test_ReferencesSemanticAnalyser_navigations {

    private companion object {
        val grammarStr = GrammarString(
            """
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                unit = member* ;
                member = block | use | imp | alias ;
                block = vis? 'block' ID '{' member* '}' ;
                vis = 'private' | 'public' ;
                use = 'use' ID ;
                imp = 'import' ID ;
                alias = 'alias' ID 'for' target ;
                target = ID ;
                leaf ID = "[a-z]+" ;
            }
            """.trimIndent()
        )

        val processor = Agl.processorFromStringSimple(grammarDefinitionStr = grammarStr).let {
            assertTrue(it.issues.errors.isEmpty(), it.issues.toString())
            it.processor!!
        }

        fun errorsFor(references: String): List<String> {
            val res = CrossReferenceDomainDefault.fromString(contextFromTypesDomain(processor.typesDomain), CrossReferenceString(references))
            return res.allIssues.errors.map { it.message }
        }

        fun refs(lines: String, refLines: String = "") = """
            namespace test.Test
                identify Block by id
                $lines
                references {
                    in Use { property id refers-to Block $refLines }
                    in Imp { property id refers-to Block }
                    in Target { property id refers-to Block }
                }
        """.trimIndent()
    }

    @Test
    fun all_ok() {
        val errors = errorsFor(
            refs(
                """
                identify Alias by id alias-for target.id
                import-into-scope Imp element id recursive id == 'r' when ${'$'}owner != ${'$'}nothing
                scope Block {
                    identify Block by id visibility vis
                    import forall member of-type Imp element id visibility 'public' when id != 'x'
                    inherit forall ${'$'}owner members-of ${'$'}container.id
                }
                """.trimIndent(),
                "from ${'$'}root when id == 'a'"
            )
        )
        assertEquals(emptyList(), errors)
    }

    @Test
    fun when_unknown_property() {
        val errors = errorsFor(refs("", "when nope == 'a'"))
        assertEquals(listOf("For references in 'Use', property 'id', when, 'nope': type 'Use' has no property 'nope'"), errors)
    }

    @Test
    fun from_unknown_property() {
        val errors = errorsFor(refs("", "from nope"))
        assertEquals(listOf("For references in 'Use', property 'id', from, 'nope': type 'Use' has no property 'nope'"), errors)
    }

    @Test
    fun else_without_previous() {
        val errors = errorsFor(refs("", "from ${'$'}root else id"))
        assertEquals(listOf("For references in 'Use', property 'id': 'else' is only used with 'from \$previous...', it is ignored here"), errors)
    }

    @Test
    fun visibility_unknown_property() {
        val errors = errorsFor(refs("scope Block { identify Block by id visibility nope }"))
        assertEquals(listOf("In scope for type 'Block', visibility of 'Block', 'nope': type 'Block' has no property 'nope'"), errors)
    }

    @Test
    fun alias_for_unknown_property() {
        val errors = errorsFor(refs("identify Alias by id alias-for target.nope"))
        assertEquals(listOf("In root scope, alias-for of 'Alias', 'target.nope': type 'Target' has no property 'nope'"), errors)
    }

    @Test
    fun import_elements_unknown_property() {
        val errors = errorsFor(refs("scope Block { import forall nope element id }"))
        assertEquals(listOf("In scope for type 'Block', import, forall, 'nope': type 'Block' has no property 'nope'"), errors)
    }

    @Test
    fun import_target_unknown_property() {
        val errors = errorsFor(refs("scope Block { import forall member of-type Imp element nope }"))
        assertEquals(listOf("In scope for type 'Block', import, 'nope': type 'Imp' has no property 'nope'"), errors)
    }

    @Test
    fun import_into_scope_condition_unknown_property() {
        val errors = errorsFor(refs("import-into-scope Imp element id when nope"))
        assertEquals(listOf("For 'import-into-scope Imp', when, 'nope': type 'Imp' has no property 'nope'"), errors)
    }
}
