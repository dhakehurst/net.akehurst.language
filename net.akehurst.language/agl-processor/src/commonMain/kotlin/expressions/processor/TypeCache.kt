/*
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.expressions.processor

import net.akehurst.language.types.api.MethodDefinitionResolved
import net.akehurst.language.types.api.MethodName
import net.akehurst.language.types.api.PropertyDeclarationResolved
import net.akehurst.language.types.api.PropertyName
import net.akehurst.language.types.api.TypeDefinition
import net.akehurst.language.types.api.TypeInstance
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.types.asm.TypeInstanceSimple

/**
 * Lookups on types, cached for one run of an accessor-mutator (see ObjectGraphAccessorMutator.beginTypeCache),
 * during which the types do not change.
 *
 * TypeInstances are often created anew for each value (e.g. TypeDefinition.type()), and TypeInstance.equals does not
 * always mean 'resolves to the same definition', so:
 *  - the resolved definition of a TypeInstanceSimple is keyed by what it is resolved from (its namespace (identity) and name),
 *    of any other TypeInstance by the TypeInstance itself (identity),
 *  - resolved properties/methods of a TypeInstance without type arguments are keyed by its resolved definition (identity) and nullability,
 *    of any other TypeInstance by the TypeInstance itself (identity).
 */
internal class TypeCache {

    /** equality by identity of [obj] (hashCode is that of [obj], which identical objects share) */
    private class Identity(val obj: Any) {
        override fun hashCode(): Int = obj.hashCode()
        override fun equals(other: Any?): Boolean = other is Identity && other.obj === this.obj
    }

    private data class Key(val definitionOrInstance: Identity, val isNullable: Boolean)

    /** TypeInstanceSimple.resolvedDefinitionOrNull is namespace.findTypeNamed(qualifiedOrImportedTypeName) */
    private data class NameKey(val namespace: Identity, val name: PossiblyQualifiedName)

    private val _resolvedDefinition = mutableMapOf<Any, TypeDefinition?>()

    fun resolvedDefinitionOrNull(type: TypeInstance): TypeDefinition? {
        val k: Any = when (type) {
            is TypeInstanceSimple -> NameKey(Identity(type.namespace), type.qualifiedOrImportedTypeName)
            else -> Identity(type)
        }
        return when {
            _resolvedDefinition.containsKey(k) -> _resolvedDefinition[k]
            else -> type.resolvedDefinitionOrNull.also { _resolvedDefinition[k] = it }
        }
    }

    private val _allResolvedProperty = mutableMapOf<Key, Map<PropertyName, PropertyDeclarationResolved>>()
    private val _allResolvedMethod = mutableMapOf<Key, Map<MethodName, MethodDefinitionResolved>>()

    private fun keyFor(type: TypeInstance): Key {
        val def: TypeDefinition? = when {
            type.typeArguments.isEmpty() -> resolvedDefinitionOrNull(type)
            else -> null
        }
        return Key(Identity(def ?: type), type.isNullable)
    }

    fun allResolvedProperty(type: TypeInstance): Map<PropertyName, PropertyDeclarationResolved> =
        _allResolvedProperty.getOrPut(keyFor(type)) { type.allResolvedProperty }

    fun allResolvedMethod(type: TypeInstance): Map<MethodName, MethodDefinitionResolved> =
        _allResolvedMethod.getOrPut(keyFor(type)) { type.allResolvedMethod }
}
