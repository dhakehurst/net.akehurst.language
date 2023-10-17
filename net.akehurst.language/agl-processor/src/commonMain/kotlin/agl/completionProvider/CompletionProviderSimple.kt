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

package net.akehurst.language.agl.completionProvider

import net.akehurst.language.agl.semanticAnalyser.ContextSimple
import net.akehurst.language.api.asm.AsmSimple
import net.akehurst.language.api.grammar.RuleItem
import net.akehurst.language.api.processor.CompletionItem

class CompletionProviderSimple : CompletionProviderAbstract<AsmSimple, ContextSimple>() {

    override fun provide(nextExpected: Set<RuleItem>, context: ContextSimple?, options: Map<String, Any>): List<CompletionItem> {
        TODO("not implemented")
    }

}