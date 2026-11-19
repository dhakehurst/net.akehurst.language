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


package net.akehurst.language.reference

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.simple.contextAsmSimple
import net.akehurst.language.api.processor.CrossReferenceString
import net.akehurst.language.api.processor.GrammarString
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A property used in the references definition for a type that does not have it (e.g. the type of a grammar choice, whose supertype has no properties)
 * is not an error when a subtype of that type has it.
 */
class test_References_subtypeProperties {

    private companion object {
        val grammarStr = GrammarString(
            """
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                unit = item* ;
                item = def | ext | use ;
                ext = sub | sub2 ;
                def = 'def' ID body? ;
                sub = 'sub' ID 'uses' ID body? ;
                sub2 = 'sub2' ID 'uses' ID body? ;
                body = '{' item* '}' ;
                use = 'use' ID ;
                leaf ID = "[A-Za-z_][A-Za-z0-9_]*" ;
            }
            """.trimIndent()
        )

        fun errors(references: String, sentence: String): List<String> {
            val processor = Agl.processorFromStringSimple(grammarDefinitionStr = grammarStr, referenceStr = CrossReferenceString(references.trimIndent())).processor!!
            return processor.process(sentence, Agl.options { semanticAnalysis { sentenceContext(contextAsmSimple()) } }).allIssues.errors.map { it.message }
        }
    }

    @Test
    fun identify_supertype_by_property_of_subtypes() {
        val refs = """
            namespace test.Test
                identify Def by id
                identify Ext by id
                references {
                    in Use { property id refers-to Ext }
                }
        """
        assertEquals(emptyList(), errors(refs, "sub B uses A sub2 C uses A use B use C"))
        assertEquals(1, errors(refs, "sub B uses A use X").size)
    }

    @Test
    fun in_supertype_property_of_subtypes() {
        val refs = """
            namespace test.Test
                identify Def by id
                references {
                    in Ext { property id2 refers-to Def }
                }
        """
        assertEquals(emptyList(), errors(refs, "def A sub B uses A sub2 C uses A"))
        assertEquals(2, errors(refs, "def A sub B uses X sub2 C uses Y").size)
    }

    @Test
    fun scope_import_of_supertype_property_of_subtypes() {
        val refs = """
            namespace test.Test
                identify Def by id
                identify Ext by id
                scope Def { identify Def by id }
                scope Ext { import members-of id2 }
                references {
                    in Ext { property id2 refers-to Def }
                    in Use { property id refers-to Def }
                }
        """
        assertEquals(emptyList(), errors(refs, "def A { def M } sub B uses A { use M } sub2 C uses A { use M }"))
        assertEquals(1, errors(refs, "def A { def M } def D { def N } sub B uses A { use N }").size)
    }

    @Test
    fun property_of_no_subtype__error() {
        val refs = """
            namespace test.Test
                identify Def by id
                identify Ext by name
                scope Ext { import members-of other }
                references {
                    in Ext { property nope refers-to Def }
                }
        """
        val e = errors(refs, "def A")
        // identify gives two errors (the reason from the type resolver, then the identify error)
        assertEquals(4, e.size, e.toString())
        assertEquals(1, e.count { it.contains("'other': type 'Ext' has no property 'other'") }, e.toString())
        assertEquals(1, e.count { it.contains("referring property 'nope' not found") }, e.toString())
    }
}
