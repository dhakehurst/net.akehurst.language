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
internal class LanguageDefinitionFromAsm<AsmType : Any, ContextType : Any>(
    override val identity: LanguageIdentity,
    grammarModel: GrammarModel,
    buildForDefaultGoal: Boolean,
    initialConfiguration: LanguageProcessorConfiguration<AsmType, ContextType>
) : LanguageDefinitionAbstract<AsmType, ContextType>(
    grammarModel,
    buildForDefaultGoal
) {
    private val _configuration = initialConfiguration
    override var configuration: LanguageProcessorConfiguration<AsmType, ContextType>
        get() = _configuration
        set(value) {
            error("Cannot set the configuration of a LanguageDefinitionFromAsm")
        }

    override val grammarString: GrammarString?
        get() = this.grammarModel?.asString()?.let { GrammarString(it) }

    override val isModifiable: Boolean = false

    override val typesString: TypesString?
        get() = this.typesModel?.asString()?.let { TypesString(it) }

    override val transformString: TransformString?
        get() = this.transformModel?.asString()?.let { TransformString(it) }

    override val crossReferenceString: CrossReferenceString?
        get() = this.crossReferenceModel?.asString()?.let { CrossReferenceString(it) }

    override val styleString: StyleString?
        get() = this.styleModel?.asString()?.let { StyleString(it) }

    override val formatString: FormatString?
        get() = FormatString(this.formatter?.formatModel?.asString() ?:"")

    override fun update(grammarStr: GrammarString?, typeModelStr: TypesString?, asmTransformStr: TransformString?, crossReferenceStr: CrossReferenceString?, styleStr: StyleString?,  formatStr: FormatString?) {
        error("Cannot update a LanguageDefinitionFromAsm")
    }
}