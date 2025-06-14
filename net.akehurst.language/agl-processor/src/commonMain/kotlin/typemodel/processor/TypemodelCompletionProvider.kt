/*
 * Copyright (C) 2025 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.typemodel.processor

import net.akehurst.language.agl.completionProvider.CompletionProviderAbstract
import net.akehurst.language.agl.simple.ContextWithScope
import net.akehurst.language.api.processor.CompletionItem
import net.akehurst.language.api.processor.CompletionProviderOptions
import net.akehurst.language.api.processor.Spine
import net.akehurst.language.typemodel.api.TypeModel

internal class TypemodelCompletionProvider: CompletionProviderAbstract<TypeModel, ContextWithScope<Any,Any>>() {

    override fun provide(nextExpected: Set<Spine>, options: CompletionProviderOptions<ContextWithScope<Any,Any>>): List<CompletionItem> {
        return super.provide(nextExpected, options)
    }

}