/**
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.syntaxAnalyser

import net.akehurst.language.api.processor.SentenceContext
import net.akehurst.language.api.typemodel.*

// used by other languages that reference rules  in a grammar

class ContextFromTypeModel(
) : SentenceContext<String> {
    constructor(typeModel: TypeModel) : this() {
        createScopeFrom(typeModel)
    }

    companion object {
        const val TYPE_NAME_FOR_TYPES = "\$Type"
        const val TYPE_NAME_FOR_PROPERTIES = "\$Property"
    }

    override var rootScope = ScopeSimple<String>(null, "", "")

    fun clear() {
        this.rootScope = ScopeSimple<String>(null, "", "")
    }

    fun createScopeFrom(typeModel: TypeModel) {
        val scope = ScopeSimple<String>(null, "", typeModel.name)
        typeModel.allRuleNameToType.forEach {
            scope.addToScope(it.value.type.name, TYPE_NAME_FOR_TYPES, it.value.type.name)
            val type = it.value.type
            when (type) {
                is NothingType -> Unit
                is AnyType -> Unit
                is StringType -> Unit
                is ListSimpleType -> Unit
                is ListSeparatedType -> Unit
                is UnnamedSuperTypeType -> Unit
                is TupleType -> {
                }

                is ElementType -> {
                    val chs = scope.createOrGetChildScope(type.name, type.name, type.name)
                    type.property.values.forEach {
                        chs.addToScope(it.name, TYPE_NAME_FOR_PROPERTIES, it.name)
                    }
                }
            }
        }
        this.rootScope = scope
    }

    override fun hashCode(): Int = rootScope.hashCode()

    override fun equals(other: Any?): Boolean = when {
        other !is ContextFromTypeModel -> false
        this.rootScope != other.rootScope -> false
        else -> true
    }

    override fun toString(): String = "ContextFromTypeModel"
}