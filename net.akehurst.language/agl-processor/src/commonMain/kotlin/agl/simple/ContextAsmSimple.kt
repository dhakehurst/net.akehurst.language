/*
 * Copyright (C) 2024 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

import net.akehurst.kotlinx.utils.Indent
import net.akehurst.language.api.semanticAnalyser.CreateScopedItem
import net.akehurst.language.api.semanticAnalyser.ResolveScopedItem
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.reference.asm.CrossReferenceDomainDefault
import net.akehurst.language.scope.api.ItemInScope
import net.akehurst.language.scope.api.Scope
import net.akehurst.language.scope.asm.ScopeSimple
import net.akehurst.language.sentence.api.InputLocation


class CreateScopedItemDefault: CreateScopedItem {
    override fun invoke(qualifiedName: List<String>, item: Any, location: InputLocation?): Any = item
}

class ResolveScopedItemDefault : ResolveScopedItem {
    override fun invoke(itemInScope: Any): Any? = itemInScope as Any?
}

class SentenceContextAny(
    override val createScopedItem: CreateScopedItem = CreateScopedItemDefault(),
    override val resolveScopedItem: ResolveScopedItem = ResolveScopedItemDefault()
) : SentenceContext {

    companion object {
        object NULL_SENTENCE_IDENTIFIER {
            override fun toString() = "NULL_SENTENCE_IDENTIFIER"
        }
    }

    /**
     * The items in the scope contain a ScopePath to an element in an AsmSimple model
     */
    //var rootScope = ScopeSimple<ItemInScopeType>(null, ScopeSimple.ROOT_ID, CrossReferenceModelDefault.ROOT_SCOPE_TYPE_NAME)
    override val scopeForSentence = mutableMapOf<Any, ScopeSimple>()

    val isEmpty: Boolean get() = scopeForSentence.all { (k, v) -> v.isEmpty }

    fun clear() {
        scopeForSentence.clear()
    }

    override fun newScopeForSentence(sentenceIdentity: Any?): Scope {
        val newScope = ScopeSimple(null, ScopeSimple.ROOT_ID, CrossReferenceDomainDefault.ROOT_SCOPE_TYPE_NAME)
        scopeForSentence[sentenceIdentity ?: NULL_SENTENCE_IDENTIFIER] = newScope
        return newScope
    }

    override fun getScopeForSentenceOrNull(sentenceIdentity: Any?): Scope? {
        return if (null == sentenceIdentity) {
            scopeForSentence[NULL_SENTENCE_IDENTIFIER]
                ?: newScopeForSentence(NULL_SENTENCE_IDENTIFIER)
        } else {
            scopeForSentence[sentenceIdentity]
        }
    }

    override fun getOrCreateScopeForSentence(sentenceIdentity: Any?): Scope {
        return if (null == sentenceIdentity) {
            scopeForSentence[NULL_SENTENCE_IDENTIFIER]
                ?: newScopeForSentence(NULL_SENTENCE_IDENTIFIER)
        } else {
            scopeForSentence[sentenceIdentity] ?: newScopeForSentence(sentenceIdentity)
        }
    }

    //TODO: location carries sentenceIdentity ! remove duplication
    override fun addToScope(sentenceIdentity: Any?, qualifiedName: List<String>, itemTypeName: QualifiedName, location: InputLocation?, item: Any) {
        val rootScope = getScopeForSentenceOrNull(sentenceIdentity) ?: newScopeForSentence(sentenceIdentity)
        var scope = rootScope
        for (n in qualifiedName.dropLast(1)) {
            scope = scope.createOrGetChildScope(n, QualifiedName("<unknown>"))
        }
        scope.addToScope(qualifiedName.last(), itemTypeName, location, item, false)
    }

    fun merge(sentenceIdentity:Any?, other:Scope) {
        val scope = getScopeForSentenceOrNull(sentenceIdentity) ?: newScopeForSentence(sentenceIdentity)
        scope.merge(other)
    }

    override  fun findItemsConformingTo(conformsToFunc: (itemTypeName: QualifiedName) -> Boolean): List<ItemInScope> {
        return scopeForSentence.flatMap { it.value.findItemsConformingTo(conformsToFunc) }
    }

    override  fun findItemsNamedConformingTo(name: String, conformsToFunc: (itemTypeName: QualifiedName) -> Boolean): List<ItemInScope> {
        return scopeForSentence.flatMap { it.value.findItemsNamedConformingTo(name, conformsToFunc) }
    }

    override fun findItemsByQualifiedNameConformingTo(qname: List<String>, conformsToFunc: (itemTypeName: QualifiedName) -> Boolean): List<ItemInScope> {
        return scopeForSentence.flatMap { it.value.findItemsByQualifiedNameConformingTo(qname, conformsToFunc) }
    }

    override fun union(other: SentenceContext): SentenceContext {
        return SentenceContextAny().also { result ->
            this.scopeForSentence.forEach { (k, v) -> result.merge(k,v) }
            other.scopeForSentence.forEach { (k, v) -> result.merge(k,v) }
        }
    }

    override fun asString(indent: Indent): String {
        val scopeIndent = indent.inc
        val scopes = scopeForSentence.entries.joinToString("\n") { (k, v) -> "${scopeIndent}sentence $k = ${v.asString(scopeIndent)}" }
        return when {
            scopeForSentence.isEmpty() -> "context { }"
            else -> "context {\n$scopes\n}"
        }
    }

    override fun hashCode(): Int = 0 //scopeForSentence.hashCode()

    override fun equals(other: Any?): Boolean = when {
        other !is SentenceContext -> false
        this.scopeForSentence != other.scopeForSentence -> false
        else -> true
    }

    override fun toString(): String = "SentenceContext"
}