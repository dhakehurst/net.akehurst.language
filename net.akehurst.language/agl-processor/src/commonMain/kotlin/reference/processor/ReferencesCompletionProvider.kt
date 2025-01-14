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
import net.akehurst.language.grammar.api.Terminal
import net.akehurst.language.api.processor.CompletionItem
import net.akehurst.language.api.processor.Spine
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.reference.api.CrossReferenceModel

class ReferencesCompletionProvider : CompletionProviderAbstract<CrossReferenceModel, SentenceContext<String>>() {

    override fun provide(nextExpected: Set<Spine>, context: SentenceContext<String>?, options: Map<String, Any>): List<CompletionItem> {
        //TODO
        return nextExpected.flatMap { sp ->
            sp.expectedNextTerminals.flatMap { ri ->
                when (ri) {
                    is Terminal -> provideForTerminal(ri)
                    else -> emptyList()
                }
            }
        }
    }
}