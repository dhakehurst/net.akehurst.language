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

import net.akehurst.language.grammar.asm.asGrammarModel
import net.akehurst.language.grammar.api.Grammar
import net.akehurst.language.api.processor.LanguageIdentity

import net.akehurst.language.api.processor.LanguageProcessorConfiguration

//TODO: has to be public at present because otherwise JSNames are not correct for properties
internal class LanguageDefinitionFromAsm<AsmType : Any, ContextType : Any>(
    override val identity: LanguageIdentity,
    grammar: Grammar,
    buildForDefaultGoal: Boolean,
    initialConfiguration: LanguageProcessorConfiguration<AsmType, ContextType>
) : LanguageDefinitionAbstract<AsmType, ContextType>(
    grammar.asGrammarModel(),
    buildForDefaultGoal,
    initialConfiguration
) {
    private val _configuration = initialConfiguration
    override var configuration: LanguageProcessorConfiguration<AsmType, ContextType>
        get() = _configuration
        set(value) {
            error("Cannot set the configuration of a LanguageDefinitionFromAsm")
        }

    override var grammarStr: String?
        get() = this.grammarList.asString()
        set(value) {
            error("Cannot set the grammar of a LanguageDefinitionFromAsm using a String")
        }
    override val isModifiable: Boolean = false

    override var crossReferenceModelStr: String?
        get() = this.crossReferenceModel.toString() //TODO:
        set(value) {
            error("Cannot set the scopeModel of a LanguageDefinitionFromAsm using a String")
        }

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

    override fun update(grammarStr: String?, crossReferenceModelStr: String?, styleStr: String?) {
        error("Cannot update a LanguageDefinitionFromAsm using Strings")
    }
}