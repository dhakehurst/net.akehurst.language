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
import net.akehurst.language.api.analyser.ScopeModel
import net.akehurst.language.api.analyser.SemanticAnalyser
import net.akehurst.language.api.analyser.SyntaxAnalyser
import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.processor.*
import net.akehurst.language.util.CachedValue
import net.akehurst.language.util.cached
import kotlin.properties.Delegates

//TODO: has to be public at present because otherwise JSNames are not correct for properties
internal class LanguageDefinitionFromAsm<AsmType : Any, ContextType : Any>(
    override val identity: String,
    grammar: Grammar,
    buildForDefaultGoal: Boolean,
    configuration: LanguageProcessorConfiguration<AsmType, ContextType>
) : LanguageDefinitionAbstract<AsmType, ContextType>(
    grammar,
    buildForDefaultGoal,
    configuration
) {
    override var grammarStr: String?
        get() = this.grammar.toString() //TODO:
        set(value) {
            error("Cannot set the grammar of a LanguageDefinitionFromAsm using a String")
        }
    override val isModifiable: Boolean = false

    override var scopeModelStr: String?
        get() = this.scopeModel.toString() //TODO:
        set(value) {
            error("Cannot set the scopeModel of a LanguageDefinitionFromAsm using a String")
        }
/*
    override var aglOptions: ProcessOptions<List<Grammar>, GrammarContext>?
        get() = error("Cannot get the aglOptions of a LanguageDefinitionFromAsm")
        set(value) {
            error("Cannot set the aglOptions of a LanguageDefinitionFromAsm")
        }
*/

    override var styleStr: String?
        get() = this.style.toString() //TODO:
        set(value) {
            error("Cannot set the styleStr of a LanguageDefinitionFromAsm using a String")
        }
/*
    override var formatStr: String?
        get() = this.grammar.toString() //TODO:
        set(value) {
            error("Cannot set the formatStr of a LanguageDefinitionFromAsm using a String")
        }

 */
}