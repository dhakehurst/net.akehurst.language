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

package net.akehurst.language.agl.simple

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.AsmTransformString
import net.akehurst.language.api.processor.CrossReferenceString
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.api.processor.TypesString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 'inherit members-of' an unnamed (composite) element: its anonymous scope is inherited.
 * Here an Alias inherits the members of its unnamed Chain, and the Chain inherits the members of the target of its last step,
 * given by a navigation with a method call ('steps.filter(...).last.target'), whose references are resolved first.
 * (Like a KerML feature chain 'a.c', which is an unnamed owned Feature with the members of its last chaining feature.)
 */
class test_ReferenceResolverSimple_inheritFromUnnamed {

    private companion object {
        val grammarStr = GrammarString(
            """
            namespace test
            grammar Test {
              skip leaf WS = "\s+" ;
              unit = item* ;
              item = def | alias ;
              def = 'def' ID '{' def* '}' ;
              alias = 'alias' ID '=' chain ;
              chain = [step / '.']+ ;
              step = ID ;
              leaf ID = "[a-z]+" ;
            }
            """.trimIndent()
        )
        val typesStr = TypesString(
            """
            namespace test.Test
              import std
              interface Item { }
              data Unit { cmp var items: List<Item> }
              data Def : Item { var name: String  cmp var defs: List<Def> }
              data Alias : Item { var name: String  cmp var chain: Chain }
              data Chain { cmp var steps: OrderedSet<Step> }
              data Step { ref var target: Item }
            """.trimIndent()
        )
        val transformStr = AsmTransformString(
            """
            namespace test
            asm-transform Test {
              unit: Unit() { items := children }
              item: child[0] as Item
              def: Def() { name := child[1]  defs := child[3] }
              alias: Alias() { name := child[1]  chain := child[3] }
              chain: Chain() { steps := children.items }
              step: Step() { target := child[0] }
            }
            """.trimIndent()
        )
        val referenceStr = CrossReferenceString(
            """
            namespace test.Test
              identify Def by name
              identify Alias by name
              scope Def { identify Def by name }
              scope Alias { inherit members-of chain }
              scope Chain { inherit members-of steps.filter({ s -> true }).last.target when steps.size > 0 }
              references {
                in Chain { forall steps of-type Step { property target refers-to Item from ${'$'}previous.target } }
              }
            """.trimIndent()
        )
        val processor = Agl.processorFromStringSimple(grammarStr, typesStr, transformStr, referenceStr).let {
            assertTrue(it.issues.errors.isEmpty(), it.issues.toString())
            it.processor!!
        }

        fun test(sentence: String, expectedErrors: List<String>) {
            val result = processor.process(sentence, Agl.options { semanticAnalysis { sentenceContext(contextAsmSimple()) } })
            assertTrue(result.allIssues.errors.none { it.phase.name != "SEMANTIC_ANALYSIS" }, result.allIssues.toString())
            assertEquals(expectedErrors, result.allIssues.errors.map { it.message })
        }
    }

    @Test
    fun chain_of_defs() {
        test("def a { def c { def d { } } } alias b = a.c", emptyList())
    }

    @Test
    fun chain_through_an_alias__members_of_the_last_step() {
        test("def a { def c { def d { } } } alias b = a.c alias e = b.d", emptyList())
    }

    @Test
    fun chain_through_an_alias__not_a_member_of_the_last_step() {
        test("def a { def c { def d { } } } alias b = a.c alias e = b.c", listOf("Reference 'c' not resolved, to type(s) [Item] in scope '/b'"))
    }
}
