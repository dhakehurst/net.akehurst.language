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
package net.akehurst.language.agl.grammar.scopes

class ScopeModel {
    val scopes = mutableListOf<Scope>()
    val references = mutableListOf<ReferenceDefinition>()

    fun isScope(scopeFor: String): Boolean {
        return scopes.any { it.scopeFor == scopeFor }
    }

    fun isReference(typeName: String, propertyName: String): Boolean {
        return references.any {
            it.inTypeName == typeName
                    && it.referringPropertyName == propertyName
        }
    }

    fun getReferablePropertyNameFor(scopeFor: String, typeName: String): String? {
        val scope = scopes.firstOrNull { it.scopeFor == scopeFor }
        val identifiable = scope?.identifiables?.firstOrNull { it.typeName == typeName }
        return identifiable?.propertyName
    }
    fun getReferredToTypeNameFor(inTypeName: String, referringPropertyName: String): List<String> {
        val def = references.firstOrNull { it.inTypeName == inTypeName && it.referringPropertyName==referringPropertyName }
        return def?.refersToTypeName ?: emptyList()
    }
}

data class Scope(
    val scopeFor: String
) {
    val identifiables = mutableListOf<Identifiable>()
}

data class Identifiable(
    val typeName: String,
    val propertyName: String
)

data class ReferenceDefinition(
    val inTypeName: String,
    val referringPropertyName: String,
    val refersToTypeName: List<String>
)