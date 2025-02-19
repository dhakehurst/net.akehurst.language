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

import net.akehurst.language.api.processor.*
import net.akehurst.language.grammar.api.GrammarModel

//TODO: has to be public at present because otherwise JSNames are not correct for properties
internal class LanguageDefinitionFromAsm<AsmType:Any, ContextType : Any>(
    override val identity: LanguageIdentity,
    grammarModel: GrammarModel,
    buildForDefaultGoal: Boolean,
    initialConfiguration: LanguageProcessorConfiguration<AsmType, ContextType>
) : LanguageDefinitionAbstract<AsmType, ContextType>(
    grammarModel,
    buildForDefaultGoal,
    initialConfiguration
) {
    private val _configuration = initialConfiguration
    override var configuration: LanguageProcessorConfiguration<AsmType, ContextType>
        get() = _configuration
        set(value) {
            error("Cannot set the configuration of a LanguageDefinitionFromAsm")
        }

    override var grammarStr: GrammarString?
        get() = this.grammarModel.asString().let { GrammarString(it) }
        set(value) {
            error("Cannot set the grammar of a LanguageDefinitionFromAsm using a String")
        }
    override val isModifiable: Boolean = false

    override var typeModelStr: TypeModelString?
        get() = this.typeModel?.asString()?.let { TypeModelString(it) }
        set(value) {
            error("Cannot set the typeModelStr of a LanguageDefinitionFromAsm using a String")
        }

    override var asmTransformStr: TransformString?
        get() = this.asmTransformModel?.asString()?.let { TransformString(it) }
        set(value) {
            error("Cannot set the asmTransformStr of a LanguageDefinitionFromAsm using a String")
        }

    override var crossReferenceStr: CrossReferenceString?
        get() = this.crossReferenceModel?.asString()?.let { CrossReferenceString(it) }
        set(value) {
            error("Cannot set the crossReferenceModelStr of a LanguageDefinitionFromAsm using a String")
        }

    override var styleStr: StyleString?
        get() = this.style?.asString() ?.let { StyleString(it) }
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
    override fun update(grammarStr: GrammarString?, typeModelStr: TypeModelString?, asmTransformStr: TransformString?, crossReferenceStr: CrossReferenceString?, styleStr: StyleString?) {
        error("Cannot update a LanguageDefinitionFromAsm using Strings")
    }
}