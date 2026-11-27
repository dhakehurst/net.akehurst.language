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
import net.akehurst.language.types.api.TypeArgumentNamed
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.types.asm.TypeInstanceSimple

/**
 * Lookups on types, cached for one run of an accessor-mutator (see ObjectGraphAccessorMutator.beginTypeCache),
 * during which the types do not change.
 *
 * TypeInstances are often created anew for each value (e.g. TypeDefinition.type(), List<T>.type(..)),
 * so nothing is keyed by TypeInstance identity (that would add an entry per instance, all with the same hashCode).
 * TypeInstance.equals does not always mean 'resolves to the same definition', so keys are made from what a type resolves to:
 *  - the resolved definition of a TypeInstanceSimple is keyed by what it is resolved from (its namespace (identity) and name);
 *    other TypeInstances (tuple types, type parameter references) are not cached, their resolvedDefinitionOrNull is not a lookup,
 *  - resolved properties/methods are keyed by the resolved definition (identity), the keys of the type arguments and nullability;
 *    a type that (or whose type arguments) does not resolve is not cached.
 */
internal class TypeCache {

    /** equality by identity of [obj] (hashCode is that of [obj], which identical objects share) */
    private class Identity(val obj: Any) {
        override fun hashCode(): Int = obj.hashCode()
        override fun equals(other: Any?): Boolean = other is Identity && other.obj === this.obj
    }

    /** a resolved type: its definition (identity), the keys of its type arguments (with their names for named arguments) and nullability */
    private data class Key(val definition: Identity, val typeArguments: List<Any>, val isNullable: Boolean)

    /** TypeInstanceSimple.resolvedDefinitionOrNull is namespace.findTypeNamed(qualifiedOrImportedTypeName) */
    private data class NameKey(val namespace: Identity, val name: PossiblyQualifiedName)

    private val _resolvedDefinition = mutableMapOf<NameKey, TypeDefinition?>()

    fun resolvedDefinitionOrNull(type: TypeInstance): TypeDefinition? = when (type) {
        is TypeInstanceSimple -> {
            val k = NameKey(Identity(type.namespace), type.qualifiedOrImportedTypeName)
            when {
                _resolvedDefinition.containsKey(k) -> _resolvedDefinition[k]
                else -> type.resolvedDefinitionOrNull.also { _resolvedDefinition[k] = it }
            }
        }

        else -> type.resolvedDefinitionOrNull
    }

    private val _allResolvedProperty = mutableMapOf<Key, Map<PropertyName, PropertyDeclarationResolved>>()
    private val _allResolvedMethod = mutableMapOf<Key, Map<MethodName, MethodDefinitionResolved>>()

    /** null if the type, or one of its type arguments, does not resolve */
    private fun keyFor(type: TypeInstance): Key? {
        val def = resolvedDefinitionOrNull(type) ?: return null
        val args = type.typeArguments.map { ta ->
            val k = keyFor(ta.type) ?: return null
            when (ta) {
                is TypeArgumentNamed -> Pair(ta.name, k)
                else -> k
            }
        }
        return Key(Identity(def), args, type.isNullable)
    }

    fun allResolvedProperty(type: TypeInstance): Map<PropertyName, PropertyDeclarationResolved> =
        when (val k = keyFor(type)) {
            null -> type.allResolvedProperty
            else -> _allResolvedProperty.getOrPut(k) { type.allResolvedProperty }
        }

    fun allResolvedMethod(type: TypeInstance): Map<MethodName, MethodDefinitionResolved> =
        when (val k = keyFor(type)) {
            null -> type.allResolvedMethod
            else -> _allResolvedMethod.getOrPut(k) { type.allResolvedMethod }
        }
}
