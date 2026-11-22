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

package net.akehurst.language.m2mTransform.processor

import net.akehurst.language.agl.processor.SemanticAnalysisResultDefault
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.api.processor.SemanticAnalysisOptions
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.syntaxAnalyser.LocationMap
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.m2mTransform.api.M2mTransformDomain
import net.akehurst.language.m2mTransform.api.M2mTransformRuleSet
import net.akehurst.language.m2mTransform.api.DomainReference
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.m2mTransform.api.M2mTransformPatternRule
import net.akehurst.language.m2mTransform.api.RuleCall
import net.akehurst.language.types.api.TypesDomain

class M2mTransformSemanticAnalyser : SemanticAnalyser<M2mTransformDomain, SentenceContext> {

    private val _issues = IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS)

    override fun clear() {
        _issues.clear()
    }

    override fun analyse(
        sentenceIdentity: Any?,
        asm: M2mTransformDomain,
        locationMap: LocationMap?,
        options: SemanticAnalysisOptions<SentenceContext>
    ): SemanticAnalysisResult {
        val sentenceContext = options.sentenceContext
        when {
            null == sentenceContext -> _issues.warn(null, "No sentence context provided, either provide one or switch off Semantic Analysis.")
            else -> {
                // transform extends: resolve first, the rules of a transform include the inherited ones (allRule)
                asm.allTransformRuleSet.forEach { def -> resolveTransformExtends(asm, def) }
                asm.allTransformRuleSet.forEach { def -> checkTransformExtends(def) }
                asm.allTransformRuleSet.forEach { def ->
                    def.domainParameters.entries.forEach { (k, v) ->
                        val td = (sentenceContext.findItemsNamedConformingTo(v.value) { true }).firstOrNull()?.item
                        when (td) {
                            null -> _issues.error(null, "TypesDomain '${v.value}' not found in sentence context for parameter '${k}'")
                            !is TypesDomain -> _issues.error(null, "TypesDomain '${v.value}' is not a TypesDomain in the sentence context, rather a '${td::class.simpleName}'")
                            else -> def.resolveDomainParameter(k, td)
                        }
                    }
                    def.rule.forEach { (k, v) ->
                        // resolve extends references
                        v.extends.forEach { e ->
                            when (val r = def.allRule[e.nameOrQName.simpleName]) { //TODO: support qnames !
                                null -> _issues.error(locationMap?.get(def), "Rule '${e.nameOrQName.value}' extended by rule '${k.value}' in transform '${def.name.value}' not found.")
                                else -> e.resolveAs(r)
                            }
                        }
                        v.domainSignature.forEach { (dr, dv) ->
                            val typesDomain = def.domainParameterResolved[dr]
                            if (null == typesDomain) {
                                _issues.error(null, "TypesDomain '$dr' not found in rule '${k}'")
                            } else {
                                val typeIssues = dv.variable.resolveType(typesDomain)
                                _issues.addAll(typeIssues)
                            }
                        }
                        // resolve where and when rule refs
                        when (v) {
                            is M2mTransformPatternRule -> {
                                v.domainTemplate.forEach { (dr,rhs) ->
                                    val typesDomain = def.domainParameterResolved[dr]
                                    if (null == typesDomain) {
                                        _issues.error(null, "TypesDomain '${dr.value}' not found in rule '${k.value}'")
                                    } else {
                                         // TODO: check that a template value conforms to its property type, e.g. an object template
                                         //  'key == Key {..}' for a collection property 'key: Set<Key>' is not reported
                                         val typeIssues = rhs.resolveTypes(typesDomain)
                                         _issues.addAll(typeIssues)
                                    }
                                }

                                when (val wn = v.when_) {
                                    is RuleCall -> resolveRuleCall(def, k, "when", wn, locationMap)
                                }
                                v.where.forEach { w ->
                                    resolveRuleCall(def, k, "where", w, locationMap)
                                }
                            }
                        }
                    }
                }
            }
        }

        return SemanticAnalysisResultDefault(emptyList(), _issues)
    }

    /**
     * resolves the rule called in a 'when' or 'where' clause of rule 'callerName',
     * and checks that the called rule has the domains and parameters named in the call
     */
    private fun resolveRuleCall(def: M2mTransformRuleSet, callerName: SimpleName, clause: String, call: RuleCall, locationMap: LocationMap?) {
        val location = locationMap?.get(def)
        val called = def.allRule[call.ruleName]
        when (called) {
            null -> _issues.error(location, "In '$clause' clause of rule '${callerName.value}' in transform '${def.name.value}', the called rule '${call.ruleName.value}' is not found.")
            else -> {
                call.resolveAs(called)
                call.domainArguments.keys.filter { it !in called.domainSignature.keys }.forEach { dr ->
                    _issues.error(location, "In '$clause' clause of rule '${callerName.value}' in transform '${def.name.value}', the called rule '${call.ruleName.value}' has no domain '${dr.value}'.")
                }
                val paramNames = called.parameters.map { it.name }
                call.ruleArguments.keys.filter { it !in paramNames }.forEach { an ->
                    _issues.error(location, "In '$clause' clause of rule '${callerName.value}' in transform '${def.name.value}', the called rule '${call.ruleName.value}' has no parameter '${an.value}'.")
                }
            }
        }
    }

    // TODO: transforms (for 'extends') are only looked up in this text; look them up also in the namespaces given by 'import ns' (MT20, docs m2m-transform.md)
    /** a transform named in 'extends' is looked up by qualified name, or by simple name in the same namespace first */
    private fun resolveTransformExtends(asm: M2mTransformDomain, def: M2mTransformRuleSet) {
        def.extends.forEach { ref ->
            val name = ref.nameOrQName
            val all = asm.allTransformRuleSet
            val found = when (name) {
                is QualifiedName -> all.firstOrNull { it.qualifiedName == name }
                else -> all.firstOrNull { it.namespace === def.namespace && it.name.value == name.value }
                    ?: all.firstOrNull { it.name.value == name.value }
            }
            when (found) {
                null -> _issues.error(null, "Transform '${name.value}' extended by transform '${def.name.value}' not found.")
                else -> ref.resolveAs(found)
            }
        }
    }

    private fun checkTransformExtends(def: M2mTransformRuleSet) {
        val supers = def.extends.mapNotNull { it.resolved }
        if (extendsTransitively(def, def, mutableListOf())) {
            _issues.error(null, "Transform '${def.name.value}' extends itself (directly or through other transforms).")
            return
        }
        supers.forEach { sup ->
            if (sup.domainParameters != def.domainParameters) {
                val fmt = { m: Map<DomainReference, SimpleName> -> m.entries.joinToString { "${it.key.value}:${it.value.value}" } }
                _issues.error(null, "Transform '${def.name.value}' extends '${sup.name.value}', but their domains differ: (${fmt(def.domainParameters)}) and (${fmt(sup.domainParameters)}).")
            }
        }
        val inherited = supers.flatMap { it.allRule.keys }.toSet()
        def.rule.values.forEach { r ->
            when {
                r.isOverride && r.name !in inherited ->
                    _issues.error(null, "Rule '${r.name.value}' in transform '${def.name.value}' is marked 'override', but no rule named '${r.name.value}' is inherited.")

                r.isOverride.not() && r.name in inherited ->
                    _issues.error(null, "Rule '${r.name.value}' in transform '${def.name.value}' replaces an inherited rule with the same name, it must be marked 'override'.")
            }
        }
    }

    private fun extendsTransitively(def: M2mTransformRuleSet, target: M2mTransformRuleSet, visited: MutableList<M2mTransformRuleSet>): Boolean =
        def.extends.mapNotNull { it.resolved }.any { sup ->
            when {
                sup === target -> true
                visited.any { it === sup } -> false
                else -> {
                    visited.add(sup)
                    extendsTransitively(sup, target, visited)
                }
            }
        }
}