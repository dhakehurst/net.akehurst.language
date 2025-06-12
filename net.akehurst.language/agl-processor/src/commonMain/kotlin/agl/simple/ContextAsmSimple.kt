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

import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.asm.api.AsmStructure
import net.akehurst.language.base.api.Indent
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.reference.asm.CrossReferenceModelDefault
import net.akehurst.language.scope.api.ItemInScope
import net.akehurst.language.scope.api.Scope
import net.akehurst.language.scope.asm.ScopeSimple
import net.akehurst.language.sentence.api.InputLocation
import kotlin.collections.set

fun interface CreateScopedItem<ItemType, ItemInScopeType> {
    fun invoke(qualifiedName: List<String>, item: ItemType, location: InputLocation?): ItemInScopeType
}

fun interface ResolveScopedItem<ItemType, ItemInScopeType> {
    fun invoke(itemInScope: ItemInScopeType): ItemType?
}

object NULL_SENTENCE_IDENTIFIER {
    override fun toString() = "NULL_SENTENCE_IDENTIFIER"
}

open class ContextWithScope<ItemType : Any, ItemInScopeType : Any>(
    val createScopedItem: CreateScopedItem<ItemType, ItemInScopeType> = CreateScopedItem { ref, item, location -> item as ItemInScopeType },
    val resolveScopedItem: ResolveScopedItem<ItemType, ItemInScopeType> = ResolveScopedItem { itemInScope -> itemInScope as ItemType }
) : SentenceContext {


    /**
     * The items in the scope contain a ScopePath to an element in an AsmSimple model
     */
    //var rootScope = ScopeSimple<ItemInScopeType>(null, ScopeSimple.ROOT_ID, CrossReferenceModelDefault.ROOT_SCOPE_TYPE_NAME)
    val scopeForSentence = mutableMapOf<Any, ScopeSimple<ItemInScopeType>>()

    val isEmpty: Boolean get() = scopeForSentence.all { (k, v) -> v.isEmpty }

    fun clear() {
        scopeForSentence.clear()
    }

    fun newScopeForSentence(sentenceIdentity: Any?): Scope<ItemInScopeType> {
        val newScope = ScopeSimple<ItemInScopeType>(null, ScopeSimple.ROOT_ID, CrossReferenceModelDefault.ROOT_SCOPE_TYPE_NAME)
        scopeForSentence[sentenceIdentity ?: NULL_SENTENCE_IDENTIFIER] = newScope
        return newScope
    }

    fun getScopeForSentenceOrNull(sentenceIdentity: Any?): Scope<ItemInScopeType>? {
        return if (null == sentenceIdentity) {
            scopeForSentence[NULL_SENTENCE_IDENTIFIER]
                ?: newScopeForSentence(NULL_SENTENCE_IDENTIFIER)
        } else {
            scopeForSentence[sentenceIdentity]
        }
    }

    //TODO: location carries sentenceIdentity ! remove duplication
    fun addToScope(sentenceIdentity: Any?, qualifiedName: List<String>, itemTypeName: QualifiedName, location: InputLocation?, item: ItemInScopeType) {
        val rootScope = getScopeForSentenceOrNull(sentenceIdentity) ?: newScopeForSentence(sentenceIdentity)
        var scope = rootScope
        for (n in qualifiedName.dropLast(1)) {
            scope = scope.createOrGetChildScope(n, QualifiedName("<unknown>"))
        }
        scope.addToScope(qualifiedName.last(), itemTypeName, location, item, false)
    }

    fun findItemsConformingTo(conformsToFunc: (itemTypeName: QualifiedName) -> Boolean): List<ItemInScope<ItemInScopeType>> {
        return scopeForSentence.flatMap { it.value.findItemsConformingTo(conformsToFunc) }
    }

    fun findItemsNamedConformingTo(name: String, conformsToFunc: (itemTypeName: QualifiedName) -> Boolean): List<ItemInScope<ItemInScopeType>> {
        return scopeForSentence.flatMap { it.value.findItemsNamedConformingTo(name, conformsToFunc) }
    }

    fun findItemsByQualifiedNameConformingTo(qname: List<String>, conformsToFunc: (itemTypeName: QualifiedName) -> Boolean): List<ItemInScope<ItemInScopeType>> {
        return scopeForSentence.flatMap { it.value.findItemsByQualifiedNameConformingTo(qname, conformsToFunc) }
    }

    fun asString(indent: Indent = Indent()): String {
        val scopeIndent = indent.inc
        val scopes = scopeForSentence.entries.joinToString("\n") { (k, v) -> "${scopeIndent}sentence $k = ${v.asString(scopeIndent)}" }
        return when {
            scopeForSentence.isEmpty() -> "context { }"
            else -> "context {\n$scopes\n}"
        }
    }

    override fun hashCode(): Int = 0 //scopeForSentence.hashCode()

    override fun equals(other: Any?): Boolean = when {
        other !is ContextWithScope<*, *> -> false
        this.scopeForSentence != other.scopeForSentence -> false
        else -> true
    }

    override fun toString(): String = "ContextWithScope"
}
