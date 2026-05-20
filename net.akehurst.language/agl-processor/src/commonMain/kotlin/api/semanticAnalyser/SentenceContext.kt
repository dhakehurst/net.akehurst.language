/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.api.semanticAnalyser

import net.akehurst.kotlinx.utils.Indent
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.scope.api.ItemInScope
import net.akehurst.language.scope.api.Scope
import net.akehurst.language.scope.asm.ScopeSimple
import net.akehurst.language.sentence.api.InputLocation

fun interface CreateScopedItem {
    fun invoke(qualifiedName: List<String>, item: Any, location: InputLocation?): Any
}

fun interface ResolveScopedItem {
    fun invoke(itemInScope: Any): Any?
}

interface SentenceContext{
    val resolveScopedItem: ResolveScopedItem
    val createScopedItem: CreateScopedItem
    val scopeForSentence: Map<Any, ScopeSimple>

    fun newScopeForSentence(sentenceIdentity: Any?): Scope
    fun getScopeForSentenceOrNull(sentenceIdentity: Any?): Scope?
    fun getOrCreateScopeForSentence(sentenceIdentity: Any?): Scope

    fun findItemsByQualifiedNameConformingTo(qname: List<String>, conformsToFunc: (itemTypeName: QualifiedName) -> Boolean): List<ItemInScope>
    fun findItemsNamedConformingTo(name: String, conformsToFunc: (itemTypeName: QualifiedName) -> Boolean): List<ItemInScope>
    fun findItemsConformingTo(conformsToFunc: (itemTypeName: QualifiedName) -> Boolean): List<ItemInScope>

    fun addToScope(sentenceIdentity: Any?, qualifiedName: List<String>, itemTypeName: QualifiedName, location: InputLocation?, item: Any)

    fun union(other: SentenceContext): SentenceContext

    fun asString(indent: Indent = Indent()): String
}

//interface ContextWithScope : SentenceContext {
//
//}