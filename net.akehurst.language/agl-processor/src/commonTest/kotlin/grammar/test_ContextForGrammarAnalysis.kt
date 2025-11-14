/**
 * Copyright (C) 2025 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.grammar.processor


import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.processor.contextFromGrammarRegistry
import net.akehurst.language.agl.simple.SentenceContextAny
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import kotlin.test.Test
import kotlin.test.assertEquals


class test_ContextForGrammarAnalysis {

    companion object {
        fun findGrammarOrNull(context: SentenceContextAny, localNamespace: QualifiedName, grammarNameOrQName: PossiblyQualifiedName) =
            context.findItemsByQualifiedNameConformingTo(grammarNameOrQName.asQualifiedName(localNamespace).parts.map { it.value }) { itemTypeName ->
                true
            }.firstOrNull()?.item
    }

    @Test
    fun findBy_SimpleName() {
        val context = SentenceContextAny(
            createScopedItem = { ref, item, location -> },
            resolveScopedItem = { item -> Any() }
        )

        // for grammar test.Test
        val scope = context.newScopeForSentence(1)
        val scope_test = scope.createOrGetChildScope("test", QualifiedName("GrammarNamespace"))
        scope_test.addToScope("Test", QualifiedName("Grammar"), null, "TestGrammar", false)

        val localNamespace = QualifiedName("test")
        val nameOrQName = SimpleName("Test")
        val actual = findGrammarOrNull(context, localNamespace, nameOrQName)

        assertEquals("TestGrammar",actual)
    }

    @Test
    fun findBy_QualifiedName() {
        val context = SentenceContextAny(
            createScopedItem = { ref, item, location -> },
            resolveScopedItem = { item -> Any() }
        )

        // for grammar test.Test
        val scope = context.newScopeForSentence(1)
        val scope_test = scope.createOrGetChildScope("test", QualifiedName("GrammarNamespace"))
        scope_test.addToScope("Test", QualifiedName("Grammar"), null, "TestGrammar", false)

        val localNamespace = QualifiedName("test")
        val nameOrQName = SimpleName("Test")
        val actual = findGrammarOrNull(context, localNamespace, nameOrQName)

        assertEquals("TestGrammar",actual)
    }

    @Test
    fun registry_findBy_QualifiedName() {

        // register the lazy items
        Agl.registry.agl.base
        Agl.registry.agl.expressions
        Agl.registry.agl.grammar

        val context = contextFromGrammarRegistry()

        val actual = AglGrammarSemanticAnalyser.findGrammarOrNull(context, QualifiedName("test"), AglGrammar.defaultTargetGrammar.qualifiedName)

        assertEquals(AglGrammar.defaultTargetGrammar,actual)
    }

}