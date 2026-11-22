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

package net.akehurst.language.reference.processor

import net.akehurst.language.agl.completionProvider.CompletionProviderAbstract
import net.akehurst.language.api.processor.CompletionItem
import net.akehurst.language.api.processor.CompletionProviderOptions
import net.akehurst.language.api.processor.Spine
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.reference.api.CrossReferenceDomain

/**
 * The default items, where a type name is expected ('scope', 'identify', 'in', 'refers-to', 'of-type', 'import-into-scope')
 * the types in the sentence context (e.g. contextFromTypesDomain), from the references definition of AglCrossReference.
 * Simple names are offered, with an additional edit that imports the namespace when needed, qualified names on a clash (CR1).
 */
class ReferencesCompletionProvider : CompletionProviderAbstract<CrossReferenceDomain, SentenceContext>() {

    companion object {
        private val transformRuleSet by lazy {
            AglCrossReference.asmTransformDomain.findDefinitionByQualifiedNameOrNull(AglCrossReference.defaultTargetGrammar.qualifiedName)
                ?: error("Internal error: transform rule set for '${AglCrossReference.defaultTargetGrammar.qualifiedName}' not found")
        }
    }

    override fun provide(nextExpected: Set<Spine>, options: CompletionProviderOptions<SentenceContext>): List<CompletionItem> {
        val items = nextExpected.flatMap { spine ->
            provideDefaultWithReferencesForSpine(spine, options, options.sentenceContext, transformRuleSet, AglCrossReference.typesDomain, AglCrossReference.crossReferenceDomain)
        }
        return defaultSortAndFilter(items)
    }

}
