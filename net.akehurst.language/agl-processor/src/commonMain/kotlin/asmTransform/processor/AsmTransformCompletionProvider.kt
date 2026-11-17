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

package net.akehurst.language.asmTransform.processor

import net.akehurst.language.agl.completionProvider.CompletionProviderAbstract
import net.akehurst.language.api.processor.CompletionItem
import net.akehurst.language.api.processor.CompletionProviderOptions
import net.akehurst.language.api.processor.Spine
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.asmTransform.api.AsmTransformDomain

/**
 * The default items, where a grammar rule name is expected (the rule a transform rule is for)
 * the rules in the sentence context (contextFromGrammarAndTypesDomain), from the references definition of AsmTransform.
 */
class AsmTransformCompletionProvider : CompletionProviderAbstract<AsmTransformDomain, SentenceContext>() {

    companion object {
        private val transformRuleSet by lazy {
            AsmTransform.asmTransformDomain.findDefinitionByQualifiedNameOrNull(AsmTransform.defaultTargetGrammar.qualifiedName)
                ?: error("Internal error: transform rule set for '${AsmTransform.defaultTargetGrammar.qualifiedName}' not found")
        }
    }

    override fun provide(nextExpected: Set<Spine>, options: CompletionProviderOptions<SentenceContext>): List<CompletionItem> {
        val items = nextExpected.flatMap { spine ->
            provideDefaultWithReferencesForSpine(spine, options, options.sentenceContext, transformRuleSet, AsmTransform.typesDomain, AsmTransform.crossReferenceDomain)
        }
        return defaultSortAndFilter(items)
    }
}
