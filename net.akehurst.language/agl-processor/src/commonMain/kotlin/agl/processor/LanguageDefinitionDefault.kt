/**
 * Copyright (C) 2021 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.processor

import net.akehurst.language.api.processor.LanguageDefinition
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.util.CachedValue
import net.akehurst.language.util.cached
import kotlin.properties.Delegates

//TODO: has to be public at present because otherwise JSNames are not correct for properties
class LanguageDefinitionDefault(
    override val identity: String,
    grammar: String?,
    override var defaultGoalRule: String?,
    style: String?,
    format: String?,
    override val syntaxAnalyser: SyntaxAnalyser?,
    override val semanticAnalyser: SemanticAnalyser?
) : LanguageDefinition {
    constructor(identity: String, grammar: String) : this(identity, grammar, null, null, null, null, null)

    private val _processor_cache: CachedValue<LanguageProcessor?> = cached {
        val g = this.grammar
        if (null==g) {
            null
        }else {
            val r = defaultGoalRule
            if (null == r) {
                Agl.processorFromString(g)
            } else {
                Agl.processorFromStringForGoal(g, r)
            }
        }
    }

    override val grammarObservers = mutableListOf<(String?, String?) -> Unit>()
    override val styleObservers = mutableListOf<(String?, String?) -> Unit>()
    override val formatObservers = mutableListOf<(String?, String?) -> Unit>()

    override var grammar: String? by Delegates.observable(grammar) { _, oldValue, newValue ->
        this._processor_cache.reset()
        grammarObservers.forEach { it(oldValue,newValue) }
    }

    override var style: String? by Delegates.observable(style) { _, oldValue, newValue ->
        styleObservers.forEach { it(oldValue,newValue) }
    }

    override var format: String? by Delegates.observable(format) { _, oldValue, newValue ->
        formatObservers.forEach { it(oldValue,newValue) }
    }

    override val processor: LanguageProcessor? get() = this._processor_cache.value
}