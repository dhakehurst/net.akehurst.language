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

import net.akehurst.language.agl.grammar.grammar.GrammarContext
import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.processor.*
import kotlin.properties.Delegates

//TODO: has to be public at present because otherwise JSNames are not correct for properties
internal class LanguageDefinitionDefault<AsmType : Any, ContextType : Any>(
    override val identity: String,
    grammarStrArg: String?,
    buildForDefaultGoal: Boolean,
    configuration: LanguageProcessorConfiguration<AsmType, ContextType>
) : LanguageDefinitionAbstract<AsmType, ContextType>(
    buildForDefaultGoal,
    configuration,
) {

    override var grammarStr: String? by Delegates.observable(grammarStrArg) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            super._grammarResolver = {
               Agl.grammarFromString<List<Grammar>, GrammarContext>(newValue)
            }
            grammarStrObservers.forEach { it.invoke(oldValue,newValue) }
        }
    }

    override val grammarIsModifiable: Boolean = true

    override var scopeModelStr: String? by Delegates.observable(null) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            super._scopeModelResolver = {
                if (null==newValue) {
                    ProcessResultDefault(null, emptyList())
                } else {
                    Agl.registry.agl.scopes.processor!!.process(newValue)
                }
            }
        }
    }

    override var formatStr: String? by Delegates.observable(null) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            super._formatterResolver = {
                if (null==newValue) {
                    ProcessResultDefault(null, emptyList())
                } else {
                    Agl.registry.agl.formatter.processor!!.process(newValue)
                }
            }
        }
    }

    override var styleStr: String? by Delegates.observable(null) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            super._styleResolver = {
                if (null==newValue) {
                    ProcessResultDefault(null, emptyList())
                } else {
                    Agl.registry.agl.style.processor!!.process(newValue)
                }
            }
        }
    }
}