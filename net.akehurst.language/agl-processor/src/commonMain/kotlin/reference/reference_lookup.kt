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

package net.akehurst.language.reference.asm

import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.reference.api.*
import net.akehurst.language.types.api.TypeDefinition
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.api.TypesNamespace
import net.akehurst.language.types.api.UnionType
import net.akehurst.language.types.asm.StdLibDefault

/**
 * Looks up scope, identify and reference ('in') declarations for the type of an element.
 *
 * Declared type names are resolved once against the TypesDomain:
 *  - a qualified name is looked up as is,
 *  - a simple name in the types namespace with the same name as the declaring namespace block, then in the block's imports.
 *
 * A declaration for type T applies to every element whose type conforms to T (T itself, a supertype, or a union type that contains it).
 * The nearest declaration wins (the type itself has distance 0, a direct supertype or union 1, and so on).
 * Different declarations at the same distance are ambiguous.
 */
class CrossReferenceLookup(
    val crossReferenceDomain: CrossReferenceDomain,
    val typesDomain: TypesDomain
) {

    private class ResolvedScope(val type: TypeDefinition, val identifiables: List<ResolvedIdentifiable>, val definition: ScopeDefinition)
    private class ResolvedIdentifiable(val type: TypeDefinition, val identifiable: Identifiable)
    private class ResolvedReferenceDefinition(val type: TypeDefinition, val definition: ReferenceDefinition)
    private data class Candidate<T>(val key: List<Int>, val item: T)

    private val _scopes = mutableListOf<ResolvedScope>()
    private val _everywhere = mutableListOf<ResolvedIdentifiable>()
    private val _references = mutableListOf<ResolvedReferenceDefinition>()

    private val _distanceCache = mutableMapOf<Pair<TypeDefinition, TypeDefinition>, Int?>()
    private val _isScopeCache = mutableMapOf<QualifiedName, Boolean>()
    private val _identifyCache = mutableMapOf<Pair<QualifiedName, QualifiedName>, CrossReferenceMatch<Identifiable>>()
    private val _scopeImportsCache = mutableMapOf<QualifiedName, List<ScopeImport>>()
    private val _referencesCache = mutableMapOf<QualifiedName, List<CrossReferenceMatch<ReferenceExpression>>>()

    init {
        for (decl in crossReferenceDomain.declarationsForNamespace.values) {
            val typesNamespace = typesDomain.findNamespaceOrNull(decl.namespace.qualifiedName)
            val imported = decl.importedNamespaces.mapNotNull { typesDomain.findNamespaceOrNull(it.asQualifiedName) }
            for ((scopeTypeName, scopeDef) in decl.scopeDefinition) {
                val ids = scopeDef.identifiables.mapNotNull { id ->
                    resolveTypeOrNull(typesNamespace, imported, id.typeName)?.let { ResolvedIdentifiable(it, id) }
                }
                if (CrossReferenceDomainDefault.ROOT_SCOPE_TYPE_NAME.last == scopeTypeName) {
                    _everywhere.addAll(ids)
                } else {
                    resolveTypeOrNull(typesNamespace, imported, scopeTypeName)?.let { _scopes.add(ResolvedScope(it, ids, scopeDef)) }
                }
            }
            for (ref in decl.references) {
                resolveTypeOrNull(typesNamespace, imported, ref.inTypeName)?.let { _references.add(ResolvedReferenceDefinition(it, ref)) }
            }
        }
    }

    fun isScopeDefinedFor(typeQualifiedName: QualifiedName): Boolean = _isScopeCache.getOrPut(typeQualifiedName) {
        val type = typesDomain.findByQualifiedNameOrNull(typeQualifiedName)
        when (type) {
            null -> false
            else -> _scopes.any { null != distance(type, it.type) }
        }
    }

    fun identifyingExpressionFor(scopeForTypeQualifiedName: QualifiedName, typeQualifiedName: QualifiedName): CrossReferenceMatch<Identifiable> =
        _identifyCache.getOrPut(Pair(scopeForTypeQualifiedName, typeQualifiedName)) {
            val type = typesDomain.findByQualifiedNameOrNull(typeQualifiedName)
            when (type) {
                null -> CrossReferenceMatch.none()
                else -> {
                    val scopeType = when (scopeForTypeQualifiedName) {
                        CrossReferenceDomainDefault.ROOT_SCOPE_TYPE_NAME -> null
                        else -> typesDomain.findByQualifiedNameOrNull(scopeForTypeQualifiedName)
                    }
                    val candidates = mutableListOf<Candidate<Identifiable>>()
                    if (null != scopeType) {
                        for (scope in _scopes) {
                            val scopeDistance = distance(scopeType, scope.type) ?: continue
                            for (id in scope.identifiables) {
                                val typeDistance = distance(type, id.type) ?: continue
                                candidates.add(Candidate(listOf(0, scopeDistance, typeDistance), id.identifiable))
                            }
                        }
                    }
                    // top-level identify applies in every scope, after any scope block
                    for (id in _everywhere) {
                        val typeDistance = distance(type, id.type) ?: continue
                        candidates.add(Candidate(listOf(1, 0, typeDistance), id.identifiable))
                    }
                    CrossReferenceMatch(nearest(candidates).distinctBy { it.identifiedBy.asString() })
                }
            }
        }

    /**
     * the 'import' and 'inherit' declarations of the nearest scope definitions for the type (see class comment)
     */
    fun scopeImportsFor(typeQualifiedName: QualifiedName): List<ScopeImport> = _scopeImportsCache.getOrPut(typeQualifiedName) {
        val type = typesDomain.findByQualifiedNameOrNull(typeQualifiedName)
        when (type) {
            null -> emptyList()
            else -> {
                val candidates = _scopes.mapNotNull { sc -> distance(type, sc.type)?.let { Candidate(listOf(it), sc.definition) } }
                nearest(candidates).flatMap { it.scopeImports }
            }
        }
    }

    fun referencesFor(typeQualifiedName: QualifiedName): List<CrossReferenceMatch<ReferenceExpression>> = _referencesCache.getOrPut(typeQualifiedName) {
        val type = typesDomain.findByQualifiedNameOrNull(typeQualifiedName)
        when (type) {
            null -> emptyList()
            else -> {
                // key: what the reference expression is about, so a nearer 'in' block replaces a farther one for the same property
                val byKey = linkedMapOf<String, MutableList<Candidate<ReferenceExpression>>>()
                for (ref in _references) {
                    val d = distance(type, ref.type) ?: continue
                    for (re in ref.definition.referenceExpressionList) {
                        byKey.getOrPut(keyFor(re)) { mutableListOf() }.add(Candidate(listOf(d), re))
                    }
                }
                byKey.values.map { CrossReferenceMatch(nearest(it).distinctBy { re -> re.asString() }) }
            }
        }
    }

    private fun keyFor(re: ReferenceExpression): String = when (re) {
        is ReferenceExpressionProperty -> "property " + re.referringPropertyNavigation.asString()
        is ReferenceExpressionCollection -> "forall " + re.expression.asString() + " " + (re.ofType?.value ?: "")
        else -> re.asString()
    }

    private fun <T> nearest(candidates: List<Candidate<T>>): List<T> {
        val comparator = Comparator<List<Int>> { a, b ->
            a.zip(b).map { (x, y) -> x.compareTo(y) }.firstOrNull { it != 0 } ?: 0
        }
        val min = candidates.map { it.key }.minWithOrNull(comparator) ?: return emptyList()
        return candidates.filter { comparator.compare(it.key, min) == 0 }.map { it.item }
    }

    private fun resolveTypeOrNull(typesNamespace: TypesNamespace?, imported: List<TypesNamespace>, name: PossiblyQualifiedName): TypeDefinition? = when (name) {
        is QualifiedName -> typesDomain.findByQualifiedNameOrNull(name) ?: typesNamespace?.findTypeNamed(name)
        is SimpleName -> typesNamespace?.findTypeNamed(name) ?: imported.firstNotNullOfOrNull { it.findOwnedTypeNamed(name) }
    }

    /**
     * number of steps from 'type' to 'target' through supertypes and union types,
     * null if 'type' does not conform to 'target'
     */
    fun distance(type: TypeDefinition, target: TypeDefinition): Int? = distance(type, target, 0)

    private fun distance(type: TypeDefinition, target: TypeDefinition, depth: Int): Int? {
        val key = Pair(type, target)
        return if (_distanceCache.containsKey(key)) {
            _distanceCache[key]
        } else {
            val d = when {
                type == target -> 0
                depth > MAX_DEPTH -> null
                else -> {
                    val viaUnion = when (target) {
                        is UnionType -> target.alternatives.mapNotNull { distance(type, it.resolvedDefinition, depth + 1) }.minOrNull()?.plus(1)
                        else -> null
                    }
                    val viaSupertype = type.supertypes.mapNotNull { distance(it.resolvedDefinition, target, depth + 1) }.minOrNull()?.plus(1)
                    val viaAny = when (target) {
                        StdLibDefault.AnyType.resolvedDefinition -> ANY_DISTANCE
                        else -> null
                    }
                    listOfNotNull(viaUnion, viaSupertype, viaAny).minOrNull()
                }
            }
            _distanceCache[key] = d
            d
        }
    }

    private companion object {
        const val MAX_DEPTH = 100
        const val ANY_DISTANCE = 1000
    }
}
