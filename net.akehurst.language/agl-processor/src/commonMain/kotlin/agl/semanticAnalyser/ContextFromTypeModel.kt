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

package net.akehurst.language.agl.semanticAnalyser

import net.akehurst.language.api.processor.LanguageIdentity
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.typemodel.api.TypeModel

// used by other languages that reference rules  in a grammar

class ContextFromTypeModelReference(
    val languageDefinitionId: LanguageIdentity
) : SentenceContext<String> {
    //val rootScope = ScopeSimple<String>(null, ScopeSimple.ROOT_ID, CrossReferenceModelDefault.ROOT_SCOPE_TYPE_NAME)
    /*
        fun dereference(reg: LanguageRegistry): ContextFromTypeModel? {
            val langDef = reg.findOrNull<Any, Any>(this.languageDefinitionId)
            return langDef?.let {
                val tm = GrammarNamespaceAndAsmTransformBuilderFromGrammar.createFromGrammarList(it.grammarList)
                ContextFromTypeModel(tm)
            }
        }

     */
}

class ContextFromTypeModel(
    val typeModel: TypeModel
) : SentenceContext<String> {

    override fun hashCode(): Int = typeModel.hashCode()

    override fun equals(other: Any?): Boolean = when {
        other !is ContextFromTypeModel -> false
        this.typeModel != other.typeModel -> false
        else -> true
    }

    override fun toString(): String = "ContextFromTypeModel($typeModel)"
}