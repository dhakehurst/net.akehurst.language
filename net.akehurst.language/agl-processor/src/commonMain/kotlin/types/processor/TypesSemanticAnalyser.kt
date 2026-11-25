/*
 * Copyright (C) 2025 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.types.processor

import net.akehurst.language.agl.processor.SemanticAnalysisResultDefault
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.api.processor.ResolvedReference
import net.akehurst.language.api.processor.SemanticAnalysisOptions
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.syntaxAnalyser.LocationMap
import net.akehurst.language.base.api.OptionHolder
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.api.TypeInstance
import net.akehurst.language.types.api.TypeDefinition
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.types.api.UnionType
import net.akehurst.language.types.api.DataType
import net.akehurst.language.types.api.ValueType
import net.akehurst.language.types.asm.TypeInstanceSimple
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.types.asm.StdLibDefault

/** a type named in a types definition: [item] is the property, parameter or type it is used in (for the location) */
internal class TypeReferenceToCheck(val namespace: QualifiedName, val owner: TypeDefinition, val item: Any, val typeInstance: TypeInstance, val usedAs: String)

internal class TypesSemanticAnalyser : SemanticAnalyser<TypesDomain, SentenceContext> {

    companion object {
        const val OPTION_INCLUDE_STD = "include-std"

        /**
         * The type references written in [definition] (property, constructor parameter, supertype and union alternative types).
         * Collected before the transform domain adds the types created from the grammar to the same domain object,
         * so that only what the types definition says is checked. (issue #20)
         */
        fun typeReferencesIn(definition: TypesDomain): List<TypeReferenceToCheck> {
            val refs = mutableListOf<TypeReferenceToCheck>()
            definition.namespace.filter { it !is StdLibDefault }.forEach { ns ->
                ns.ownedTypes.forEach { td ->
                    fun add(item: Any, ti: TypeInstance, usedAs: String) = refs.add(TypeReferenceToCheck(ns.qualifiedName, td, item, ti, usedAs))
                    td.supertypes.forEach { add(td, it, "(a supertype of '${td.name.value}')") }
                    when (td) {
                        is UnionType -> td.alternatives.forEach { add(td, it, "(an alternative of union '${td.name.value}')") }
                        else -> Unit
                    }
                    td.property.forEach { p -> add(p, p.typeInstance, "(of property '${p.name.value}' of '${td.name.value}')") }
                    val constructors = when (td) {
                        is DataType -> td.constructors
                        is ValueType -> td.constructors
                        else -> emptyList()
                    }
                    constructors.forEach { c ->
                        // a parameter that sets a property is already checked with the property
                        c.parameters.filter { null == it.property }.forEach { pd ->
                            add(pd, pd.typeInstance, "(of constructor parameter '${pd.name.value}' of '${td.name.value}')")
                        }
                    }
                }
            }
            return refs
        }

        /**
         * Each type named in [refs] (and in its type arguments) must be found in [complete], the types domain once std,
         * the grammar namespaces and the types created from the grammar are added; looked up as documented in types.md:
         * own types, imports, then the other namespaces of the domain (std last).
         * A name that is a type parameter of the type it is used in is not a type reference.
         * The [locationMap] is that of the types definition. (issue #20)
         */
        fun checkTypeReferencesFound(refs: List<TypeReferenceToCheck>, locationMap: LocationMap?, complete: TypesDomain): IssueHolder {
            val issues = IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS)
            refs.forEach { ref ->
                val completeNs = complete.findNamespaceOrNull(ref.namespace) ?: return@forEach
                val typeParams = ref.owner.typeParameters.map { it.name }.toSet()
                fun notFound(ti: TypeInstance): List<PossiblyQualifiedName> = when (ti) {
                    is TypeInstanceSimple -> {
                        val n = ti.qualifiedOrImportedTypeName
                        val self = when {
                            n is SimpleName && typeParams.contains(n) -> emptyList()
                            null == (completeNs.findTypeNamed(n) ?: complete.findFirstDefinitionByPossiblyQualifiedNameOrNull(n)) -> listOf(n)
                            else -> emptyList()
                        }
                        self + ti.typeArguments.flatMap { notFound(it.type) }
                    }

                    else -> ti.typeArguments.flatMap { notFound(it.type) }
                }
                notFound(ref.typeInstance).forEach { n ->
                    issues.error(
                        locationMap?.get(ref.item) ?: locationMap?.get(ref.owner),
                        "Type '${n.value}' ${ref.usedAs} is not found in namespace '${ref.namespace.value}', its imports, or the other namespaces of the types domain. Is the name misspelled?"
                    )
                }
            }
            return issues
        }

        private val OptionHolder.includeStd:Boolean get() = this[OPTION_INCLUDE_STD] ?: false
    }

    private val _issues = IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS)
    private val _resolvedReferences = mutableListOf<ResolvedReference>()


    override fun clear() {
        _issues.clear()
        _resolvedReferences.clear()
    }

    // a type reference that is not found (e.g. 'id: Strin') can't be checked here: std, the grammar namespaces and the types
    // created from the grammar are added to the domain after this analysis, see [checkTypeReferencesFound], called by the language processor.
    override fun analyse(sentenceIdentity: Any?, asm: TypesDomain, locationMap: LocationMap?, options: SemanticAnalysisOptions<SentenceContext>): SemanticAnalysisResult {
        if (asm.options.includeStd) {
            asm.addNamespace(StdLibDefault)
        }
        return SemanticAnalysisResultDefault(_resolvedReferences, _issues)
    }

}