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

package net.akehurst.language.agl.semanticAnalyser

import net.akehurst.language.agl.simple.SentenceContextAny
import net.akehurst.language.api.processor.LanguageIdentity
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.base.api.asQualifiedName
import net.akehurst.language.types.api.TypesDomain

// used by other languages that reference rules  in a grammar
class ContextFromTypesDomainReference(
    val languageDefinitionId: LanguageIdentity
) : SentenceContext {

}

@Deprecated("use contextFromTypesDomain")
class ContextFromTypesDomain(
    val typesDomain: TypesDomain
) : SentenceContext {

    override fun hashCode(): Int = typesDomain.hashCode()

    override fun equals(other: Any?): Boolean = when {
        other !is ContextFromTypesDomain -> false
        this.typesDomain != other.typesDomain -> false
        else -> true
    }

    override fun toString(): String = "ContextFromTypesDomain($typesDomain)"
}

fun contextFromTypesDomain(typesDomain: TypesDomain) : SentenceContextAny {
    val context = SentenceContextAny()
    val sentenceIdentity = typesDomain.name.value
    typesDomain.allDefinitions.forEach { def ->
        val qualifiedName = def.qualifiedName.parts.map { it.value }
        val itemTypeName = def::class.simpleName!!.asQualifiedName //TODO: use qualified names when supported by kotlin
        val location = null
        context.addToScope(sentenceIdentity, qualifiedName, itemTypeName, location, def)
    }
    return context
}