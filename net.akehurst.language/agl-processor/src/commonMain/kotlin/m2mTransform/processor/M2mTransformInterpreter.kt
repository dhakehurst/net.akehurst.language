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

import net.akehurst.language.api.processor.EvaluationContext
import net.akehurst.language.base.api.Indent
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.expressions.api.CreateObjectExpression
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.api.RootExpression
import net.akehurst.language.expressions.processor.ExpressionsInterpreterOverTypedObject
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.m2mTransform.api.*
import net.akehurst.language.m2mTransform.processor.MappingRecord.Companion.merge
import net.akehurst.language.m2mTransform.processor.TemplateMatchResult.Companion.merge
import net.akehurst.language.objectgraph.api.ObjectGraphAccessorMutator
import net.akehurst.language.objectgraph.api.TypedObject
import net.akehurst.language.types.api.DataType
import net.akehurst.language.types.api.PropertyName
import net.akehurst.language.types.api.TypeInstance
import net.akehurst.language.types.api.ValueType
import net.akehurst.language.types.asm.StdLibDefault
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.plus

data class M2MTransformResult<OT : Any>(
    val issues: IssueHolder,
    val record: Map<M2mTransformRule, MappingRecord<OT>>,
    val targetDomainRef: DomainReference
) {
    val targets: List<TypedObject<OT>> get() = record.values.flatMap { it.alternatives.mapNotNull { it[targetDomainRef] } }

    fun asString(indent: Indent = Indent()): String {
        val sb = StringBuilder()
        sb.appendLine("${indent}M2M Transform Result:")
        for ((k, v) in record) {
            val recIndent = indent.inc
            sb.appendLine("${recIndent}top rule ${k.name}:")
            sb.appendLine(v.asString(recIndent.inc))
        }
        return sb.toString()
    }
}

data class TemplateMatchAlternatives<OT : Any>(
    val alternatives: List<TemplateMatchResult<OT>>
) {
    val isMatch: Boolean get() = alternatives.isNotEmpty()

    fun merge(): TemplateMatchResult<OT> = this.alternatives.merge()

    fun withVariable(name: String, value: TypedObject<OT>): TemplateMatchAlternatives<OT> {
        return TemplateMatchAlternatives(
            this.alternatives.map { it.withVariable(name, value) }
        )
    }
}

data class TemplateMatchResult<OT : Any>(
    val isMatch: Boolean,
    val matchedVariables: Map<String, TypedObject<OT>>
) {
    companion object {
        fun <OT : Any> EMPTY() = TemplateMatchResult<OT>(true, emptyMap())
        fun <OT : Any> Collection<TemplateMatchResult<OT>>.merge() = this.fold(TemplateMatchResult.EMPTY<OT>()) { acc, it -> acc.merge(it) }
    }

    fun getValueNamed(name: String): TypedObject<OT>? {
        return this.matchedVariables[name]
    }

    fun withVariable(name: String, value: TypedObject<OT>): TemplateMatchResult<OT> =
        TemplateMatchResult(
            this.isMatch,
            matchedVariables + Pair(name, value)
        )

    fun merge(other: TemplateMatchResult<OT>): TemplateMatchResult<OT> {
        return TemplateMatchResult(this.isMatch && other.isMatch, this.matchedVariables + other.matchedVariables)
    }
}

data class MappingRecord<OT : Any>(
    val rule: M2mTransformRule,
    /**
     * multiple source objects (or combinations of sources) could be matched by each top rule
     */
    val alternatives: List<Map<DomainReference, TypedObject<OT>>>
) {
    companion object {
        fun <OT : Any> EMPTY(rule: M2mTransformRule) = MappingRecord<OT>(rule, emptyList())
        fun <OT : Any> Collection<MappingRecord<OT>>.merge(rule: M2mTransformRule) = this.fold(MappingRecord.EMPTY<OT>(rule)) { acc, it -> acc.merge(it) }
    }

    fun merge(other: MappingRecord<OT>): MappingRecord<OT> {
        check(rule == other.rule) { "Can only merge MappingRecord if rules are the same." }
        return MappingRecord(rule, alternatives + other.alternatives)
    }

    fun asString(indent: Indent = Indent()): String {
        val sb = StringBuilder()
        sb.appendLine("${indent}MappingRecord:")
        for (i in alternatives.indices) {
            val altIndent = indent.inc
            val entryIndent = altIndent.inc
            sb.appendLine("${altIndent}$i:")
            sb.appendLine(alternatives[i].entries.joinToString("") { "${entryIndent}domain ${it.key.value} ${it.value.asString(entryIndent)}\n" })
        }
        return sb.toString()
    }

}

class M2mTransformExecution<OT : Any>(
    val targetTransform: M2mTransformRuleSet,
    val targetDomainRef: DomainReference,
    val domainAccessorMutator: Map<DomainReference, ObjectGraphAccessorMutator<OT>>,
) {
    val records: Map<M2mTransformRule, MappingRecord<OT>> = mutableMapOf()

    val targetAccessorMutator = domainAccessorMutator[targetDomainRef] ?: error("ObjectGraph not found for domain '$targetDomainRef'")

    fun addRecord(rule: M2mTransformRule, mapping: Map<DomainReference, TypedObject<OT>>) {
        val rec = records[rule]
        if (null == rec) {
            (records as MutableMap)[rule] = MappingRecord<OT>(rule, listOf(mapping))
        } else {
            rec.merge(MappingRecord<OT>(rule, listOf(mapping)))
        }
    }
}

class M2mTransformInterpreter<OT : Any>(
    val m2m: M2mTransformDomain,
    val domainAccessorMutatorByDomainName: Map<SimpleName, ObjectGraphAccessorMutator<OT>>,
    val _issues: IssueHolder = IssueHolder(LanguageProcessorPhase.INTERPRET)
) {

    companion object {

        data class CoverOption<B, R>(val b: B, val r: R)

        /**
         * Finds all subsets (combinations) R of type R, where R is generated by C elements
         * that collectively cover all elements in List1 (T).
         *
         * @param cover The elements T that MUST ALL be covered (the requirements).
         * @param bySubsetsOf The elements C to choose from (the options).
         * @param matches A function returning Pair<Boolean, R> for a match/transformed R element.
         * @return A Set of Lists, representing all valid covering combinations of type R.
         */
        fun <T, B, R> findCoveringSubsets2(
            cover: Collection<T>,
            bySubsetsOf: Collection<B>,
            matches: (c: T, b: B) -> Pair<Boolean, R>
        ): List<List<R>> {

            // T_to_CoverMap: Map<t_element, Set<CoverOption>>
            val tToCoverMap: Map<T, List<R>> = cover.associateWith { tElement ->
                bySubsetsOf.mapNotNull { cElement ->
                    val (isMatch, rValue) = matches(tElement, cElement)
                    if (isMatch) rValue else null
                }
            }

            val cp = tToCoverMap.values.cartesianProduct()
            return cp
        }

        /**
         * Finds all subsets (combinations) R of type R, where R is generated by C elements
         * that collectively cover all elements in List1 (T).
         *
         * @param cover The elements T that MUST ALL be covered (the requirements).
         * @param bySubsetsOf The elements C to choose from (the options).
         * @param matches A function returning Pair<Boolean, R> for a match/transformed R element.
         * @return A Set of Lists, representing all valid covering combinations of type R.
         */
        fun <T, B, R> findCoveringSubsets(
            cover: Collection<T>,
            bySubsetsOf: Collection<B>,
            matches: (c: T, b: B) -> Pair<Boolean, R>
        ): Set<List<R>> {

            // --- STEP 1: Pre-process Mapping and R-Generation ---

            // T_to_CoverMap: Map<t_element, Set<CoverOption>>
            val tToCoverMap: Map<T, Set<CoverOption<B, R>>> = cover.associateWith { tElement ->
                bySubsetsOf.mapNotNull { cElement ->
                    val (isMatch, rValue) = matches(tElement, cElement)
                    if (isMatch) CoverOption(cElement, rValue) else null
                }.toSet()
            }

            // --- IMPOSSIBLE CHECK ---
            if (tToCoverMap.values.any { it.isEmpty() }) {
                return emptySet()
            }

            // --- STEP 2: Recursive Search for Minimal Covers ---

            val resultsR = mutableSetOf<Set<R>>()

            // Start the recursive search
            searchCovers(
                remainingT = cover.toMutableList(),
                tToCoverMap = tToCoverMap,
                currentCoverC = mutableSetOf(),
                currentCoverR = mutableSetOf(),
                resultsR = resultsR
            )

            // --- STEP 3: Combine Minimal R-Covers with Optional R-Elements (Finalization) ---
            return finalizeRResults(bySubsetsOf, tToCoverMap, resultsR, matches)
        }

        /**
         * Recursive helper function based on the set-covering heuristic.
         */
        private fun <T, C, R> searchCovers(
            remainingT: MutableList<T>,
            tToCoverMap: Map<T, Set<CoverOption<C, R>>>,
            currentCoverC: MutableSet<C>,
            currentCoverR: MutableSet<R>,
            resultsR: MutableSet<Set<R>>
        ) {
            // BASE CASE: All T elements covered.
            if (remainingT.isEmpty()) {
                resultsR.add(currentCoverR.toSet())
                return
            }

            val tToCover = remainingT.removeAt(0)
            val coverOptions = tToCoverMap[tToCover] ?: emptySet()

            for (option in coverOptions) {
                val bElement = option.b
                val rElement = option.r

                // CHOOSE: Only proceed if the C element hasn't been used yet (unique source).
                if (currentCoverC.add(bElement)) {
                    currentCoverR.add(rElement)

                    // PRUNE: Identify and remove other T elements covered by this C element.
                    val newlyCoveredT = remainingT.filter { t ->
                        tToCoverMap[t]?.any { it.b == bElement } == true
                    }.toMutableList()

                    remainingT.removeAll(newlyCoveredT.toSet())

                    // RECURSE
                    searchCovers(remainingT, tToCoverMap, currentCoverC, currentCoverR, resultsR)

                    // UNCHOOSE (Backtrack):
                    remainingT.addAll(newlyCoveredT)
                    currentCoverC.remove(bElement)
                    currentCoverR.remove(rElement)
                }
            }

            remainingT.add(tToCover)
        }

        /**
         * Combines the minimal R-covers with all combinations of "optional" R-elements.
         */
        private fun <T, B, R> finalizeRResults(
            col2: Collection<B>,
            tToCoverMap: Map<T, Set<CoverOption<B, R>>>,
            minimalRCovers: Set<Set<R>>,
            matches: (t: T, b: B) -> Pair<Boolean, R>
        ): Set<List<R>> {
            if (minimalRCovers.isEmpty()) return emptySet()

            // 1. Identify "Optional" R-elements (those not required for T coverage)
            val requiredCElements = tToCoverMap.values.flatten().map { it.b }.toSet()
            val cOptional = col2.toSet() - requiredCElements

            // 2. Map optional C elements to their R values.
            val optionalROptions = cOptional.mapNotNull { b ->
                // We need a T element to call 'matches'. Since the C element is optional,
                // its generated R value should be independent of the specific T. We use the first T.
                // This assumes 'matches' for an optional C element consistently returns the same R.
                tToCoverMap.keys.firstOrNull()?.let { t ->
                    val (isMatch, r) = matches(t, b)
                    if (isMatch) r else null
                }
            }.toSet()

            // 3. Generate the power set of optional R elements.
            val optionalRSubsets = powerSet(optionalROptions.toList())

            // 4. Combine every minimal R cover with every optional R subset.
            val allValidCombinations = mutableSetOf<List<R>>()
            for (minRCover in minimalRCovers) {
                for (optionalRSubset in optionalRSubsets) {
                    allValidCombinations.add((minRCover + optionalRSubset).toList())
                }
            }
            return allValidCombinations
        }

        /**
         * Helper to recursively generate the power set of R elements.
         */
        private fun <R> powerSet(list: List<R>): Set<Set<R>> {
            if (list.isEmpty()) return setOf(emptySet())
            val head = list.first()
            val tail = list.drop(1)
            val subsetsOfTail = powerSet(tail)
            val subsetsWithHead = subsetsOfTail.map { subset -> subset + head }.toSet()
            return subsetsOfTail + subsetsWithHead
        }

        fun <OT : Any> List<TemplateMatchAlternatives<OT>>.cartesianProduct(): TemplateMatchAlternatives<OT> {
            val lists = this.map { it.alternatives }
            val cp = lists.cartesianProduct()
            val alts = cp.map { it.merge() }
            return TemplateMatchAlternatives(alts)
        }

        fun <K, V> Map<K, List<V>>.cartesianProduct(): List<Map<K, V>> {
            val keys = this.keys.toList()
            val values = this.values.toList()
            val alts = values.cartesianProduct()
            return alts.map { alt ->
                alt.mapIndexed { i, v -> Pair(keys[i], v) }.toMap()
            }
        }

        /**
         * Calculates the Cartesian Product of a list of lists using an iterative approach.
         * This implementation is robust against StackOverflowError for a very large number of lists.
         *
         * @param lists The top list (T) where each element is a list (L1, L2, ...).
         * @return A Set of Lists, where each result list contains exactly one element
         * from each of the input lists.
         */
        fun <E> Collection<List<E>>.cartesianProduct(): List<List<E>> {
            // Edge Case: If the input is empty or contains an empty list, the product is empty.
            if (this.isEmpty() || this.any { it.isEmpty() }) {
                return emptyList()
            }
            // Start with a set containing a single empty list.
            // This represents the "combination" of the lists processed so far (which is none).
            var result: List<List<E>> = listOf(emptyList())

            // Iterate through each list in the input (L1, L2, L3, ...)
            for (currentList in this) {

                // Create a temporary set to store the new, extended combinations
                val nextResult = mutableListOf<List<E>>()

                // Inner Loop 1: Iterate over the existing partial combinations
                for (combination in result) {

                    // Inner Loop 2: Iterate over the elements in the current input list
                    for (element in currentList) {

                        // Create a new, longer combination by appending the element
                        // to the existing partial combination.
                        nextResult.add(combination + element)
                    }
                }

                // Update the main result set to the newly generated combinations
                result = nextResult
            }

            return result
        }
    }


    /**
     * @param targetDomainRef reference to the domain that is the target of the transformation
     * @param domainGraphs the objects for each source domain, keyed by the domain reference
     */
    fun transform(targetTransform: M2mTransformRuleSet, targetDomainRef: DomainReference, domainGraphs: Map<DomainReference, List<TypedObject<OT>>>): M2MTransformResult<OT> {
        val domainAccessorMutator = targetTransform.domainParameters.entries.associate { (k, v) ->
            Pair(k, domainAccessorMutatorByDomainName[v] ?: error("Domain ObjectGraph not found for domain $k"))
        }
        val m2mExecution = M2mTransformExecution(targetTransform, targetDomainRef, domainAccessorMutator)

        val result = when {
            targetTransform.topRule.isEmpty() -> {
                _issues.error(null, "No conforming top rule found for target domain '${targetDomainRef.value}'")
                emptyList()
            }

            else -> {
                targetTransform.topRule.flatMap { topRule ->
                    executeRule(m2mExecution, topRule, domainGraphs)
                }
            }
        }
        val r = result.associateBy { it.rule }
        return M2MTransformResult(_issues, r, targetDomainRef)
    }

    private fun executeRule(
        m2mExecution: M2mTransformExecution<OT>,
        rule: M2mTransformRule,
        source: Map<DomainReference, List<TypedObject<OT>>>
    ): List<MappingRecord<OT>> = when (rule) {
        is M2mTransformAbstractRule -> executeAbstract(m2mExecution, rule, source)
        is M2MTransformRelation -> executeRelation(m2mExecution, rule, source)
        is M2MTransformMapping -> executeMapping(m2mExecution, rule, source)
        is M2MTransformTable -> executeTable(m2mExecution, rule, source)
        else -> error("Unknown rule type ${rule::class}")
    }

    private fun executeAbstract(
        m2mExecution: M2mTransformExecution<OT>,
        rule: M2mTransformAbstractRule,
        source: Map<DomainReference, List<TypedObject<OT>>>,
    ): List<MappingRecord<OT>> {
        val subrules = m2mExecution.targetTransform.rule.values.filter { r -> r !is M2mTransformAbstractRule && r.conformsTo(rule) }
        val records = subrules.flatMap { sr ->
            executeRule(m2mExecution, sr, source)
        }
        return records
    }

    private fun executeMapping(
        m2mExecution: M2mTransformExecution<OT>,
        rule: M2MTransformMapping,
        source: Map<DomainReference, List<TypedObject<OT>>>
    ): List<MappingRecord<OT>> {
        // for each domain reference get the match alternatives for each source object
        // Map of DomainReference -> List< TemplateMatchAlternatives per object >
        val domToListOfAlts = matchSourceVariables(m2mExecution, rule, source)

        val res = when {
            domToListOfAlts.isEmpty() -> {
                _issues.warn(null, "No matches found in source domains for rule '${rule.name}'.")
                emptyList()
            }

            else -> {
                val altSources = domToListOfAlts
                    .map { (k, v) -> Pair(k, v.alternatives) }
                    .toMap()
                    .cartesianProduct()
                when {
                    altSources.isEmpty() -> emptyList() // no match for this rule - a valid situation
//                    1 < altSources.size -> error("Cannot execute mapping if multiple alternative top mapping matches for a single root object")
                    else -> altSources.map { alt ->
                        val expression = rule.expression[m2mExecution.targetDomainRef]
                        when (expression) {
                            null -> {
                                _issues.error(null, "No expression found for target domain ref '$${m2mExecution.targetDomainRef.value}' of rule '${rule.name}'.")
                                emptyMap()
                            }

                            else -> {
                                val allVars = alt.values.merge()

                                val whenResult = rule.when_?.let {
                                    executeWhen(m2mExecution, rule, it, allVars.matchedVariables)
                                } ?: Pair(true, emptyMap())
                                if (whenResult.first) {
                                    val varsAfterWhen = whenResult.second + allVars.matchedVariables
                                    // construct
                                    val tgtExpr = rule.expression[m2mExecution.targetDomainRef]
                                        ?: error("No expression found for target domain '${m2mExecution.targetDomainRef.value}'") // should never happen
                                    val lhsType = rule.domainSignature[m2mExecution.targetDomainRef]?.variable?.type
                                        ?: error("No domainSignature found for target domain '${m2mExecution.targetDomainRef.value}'") // should never happen
                                    val tgtObj = createFromExpression(varsAfterWhen, lhsType,tgtExpr, m2mExecution.targetAccessorMutator)
                                    val mapping = rule.domainSignature.entries.associate { (dr, ds) ->
                                        val v = when (dr) {
                                            m2mExecution.targetDomainRef -> tgtObj
                                            else -> varsAfterWhen[ds.variable.name.value]
                                                ?: error("No variable found for '${ds.variable.name.value}' of domain '$dr'.") // should never be error!
                                        }
                                        Pair(dr, v)
                                    }
                                    m2mExecution.addRecord(rule, mapping)

                                    // where
                                    val varsAfterWhere = when {
                                        rule.where.isEmpty() -> varsAfterWhen
                                        else -> rule.where.map {
                                            executeWhere(m2mExecution, rule, it, varsAfterWhen)
                                        }.fold(mapOf()) { acc, it -> acc + it }
                                    }

                                    // setProperties
                                    setPropertiesFromExpression(tgtObj, varsAfterWhere, tgtExpr, m2mExecution.targetAccessorMutator)
                                    val srcs = alt.entries.associate { (srcDomainRef, v) ->
                                        val srcObjPat = rule.domainTemplate[srcDomainRef] ?: error("No object pattern found for domain '$srcDomainRef'")
                                        val srcId = srcObjPat.identifier?.value ?: error("No identifier found for matched object in domain '$srcDomainRef'")
                                        val src = v.getValueNamed(srcId) ?: error("No matched object found for identifier '$srcId'")
                                        Pair(srcDomainRef, src)
                                    }
                                    srcs + Pair(m2mExecution.targetDomainRef, tgtObj)

//                                    val varsAfterWhere = when {
//                                        rule.where.isEmpty() -> allVars.matchedVariables
//                                        else -> rule.where.map {
//                                            executeWhere(m2mExecution, rule, it, allVars.matchedVariables)
//                                        }.fold(mapOf<String, TypedObject<OT>>()) { acc, it -> acc + it }
//                                    }
//
//                                    val exprInterp = ExpressionsInterpreterOverTypedObject<OT>(m2mExecution.targetAccessorMutator, _issues)
//                                    val evc = EvaluationContext.of(varsAfterWhere)
//                                    val r = exprInterp.evaluateExpression(evc, expression)
//                                    val srcs = alt.entries.associate { (srcDomainRef, mr) ->
//                                        val srcObjPat = rule.domainTemplate[srcDomainRef] ?: error("No object pattern found for domain '$srcDomainRef'")
//                                        val srcId = srcObjPat.identifier?.value ?: error("No identifier found for matched object in domain '$srcDomainRef'")
//                                        val src = mr.getValueNamed(srcId) ?: error("No matched object found for identifier '$srcId'")
//                                        Pair(srcDomainRef, src)
//                                    }
//                                    srcs + Pair(m2mExecution.targetDomainRef, r)
                                } else {
                                    _issues.info(null, "when clause evaluated to false for target domain ref '${m2mExecution.targetDomainRef.value}' of rule '${rule.name}'.")
                                    emptyMap()
                                }
                            }
                        }
                    }
                }
            }
        }
        return listOf(MappingRecord(rule, res))
    }

    private fun executeRelation(
        m2mExecution: M2mTransformExecution<OT>,
        rule: M2MTransformRelation,
        source: Map<DomainReference, List<TypedObject<OT>>>
    ): List<MappingRecord<OT>> {
        val domToListOfAlts = matchSourceVariables(m2mExecution, rule, source)
        val res = when {
            domToListOfAlts.isEmpty() -> {
                _issues.warn(null, "No matches found in source domains for rule '${rule.name}'.")
                emptyList()
            }

            else -> {
                val altSources = domToListOfAlts
                    .map { (k, v) -> Pair(k, v.alternatives) }
                    .toMap()
                    .cartesianProduct()
                when {
                    altSources.isEmpty() -> emptyList() //no match for this rule - a valid situation
                    else -> altSources.map { alt ->
                        val allVars = alt.values.merge()
                        val whenResult = rule.when_?.let {
                            executeWhen(m2mExecution, rule, it, allVars.matchedVariables)
                        } ?: Pair(true, emptyMap())
                        if (whenResult.first) {
                            val varsAfterWhen = whenResult.second + allVars.matchedVariables
                            // construct
                            val objPat = rule.domainTemplate[m2mExecution.targetDomainRef]
                                ?: error("No domainTemplate found for domain '${m2mExecution.targetDomainRef.value}'") //should never happen
                            val lhsType = rule.domainSignature[m2mExecution.targetDomainRef]?.variable?.type
                                ?: error("No domainSignature found for domain '${m2mExecution.targetDomainRef.value}'") //should never happen
                            val tgtObj = createFromRhs(varsAfterWhen, lhsType, objPat, m2mExecution.targetAccessorMutator)
                            val mapping = rule.domainSignature.entries.associate { (dr, ds) ->
                                val v = when (dr) {
                                    m2mExecution.targetDomainRef -> tgtObj
                                    else -> varsAfterWhen[ds.variable.name.value] ?: error("No variable found for '${ds.variable.name.value}' of domain '$dr'.") // should never be error!
                                }
                                Pair(dr, v)
                            }
                            m2mExecution.addRecord(rule, mapping)

                            // where
                            val varsAfterWhere = when {
                                rule.where.isEmpty() -> varsAfterWhen
                                else -> rule.where.map {
                                    executeWhere(m2mExecution, rule, it, varsAfterWhen)
                                }.fold(mapOf<String, TypedObject<OT>>()) { acc, it -> acc + it }
                            }

                            // setProperties
                            setPropertiesFromRhs(tgtObj, varsAfterWhere, objPat, m2mExecution.targetAccessorMutator)
                            val srcs = alt.entries.associate { (srcDomainRef, v) ->
                                val srcObjPat = rule.domainTemplate[srcDomainRef] ?: error("No object pattern found for domain '$srcDomainRef'")
                                val srcId = srcObjPat.identifier?.value ?: error("No identifier found for matched object in domain '$srcDomainRef'")
                                val src = v.getValueNamed(srcId) ?: error("No matched object found for identifier '$srcId'")
                                Pair(srcDomainRef, src)
                            }
                            srcs + Pair(m2mExecution.targetDomainRef, tgtObj)
                        } else {
                            _issues.info(null, "when clause evaluated to false for target domain ref '${m2mExecution.targetDomainRef.value}' of rule '${rule.name}'.")
                            emptyMap()
                        }
                    }
                }
            }
        }
        return listOf(MappingRecord(rule, res))
    }

    private fun executeTable(m2mExecution: M2mTransformExecution<OT>, rule: M2MTransformTable, source: Map<DomainReference, List<TypedObject<OT>>>): List<MappingRecord<OT>> {
        val matchingValues = source.cartesianProduct().mapNotNull { srcAlt ->
            rule.values.firstNotNullOfOrNull { vs ->
                val srcValues = srcAlt.mapNotNull { (srcDomainRef, v) ->
                    val srcObjectGraph = m2mExecution.domainAccessorMutator[srcDomainRef] ?: error("ObjectGraph not found for domain '$srcDomainRef'")
                    val exprInterp = ExpressionsInterpreterOverTypedObject<OT>(srcObjectGraph, _issues)
                    val evc = EvaluationContext.of(emptyMap<String, TypedObject<OT>>()) //TODO: are there any variables !
                    val drExp = vs[srcDomainRef] ?: error("")
                    val value = exprInterp.evaluateExpression(evc, drExp)
                    val match = srcObjectGraph.equalTo(v, value)
                    if (match) {
                        Pair(srcDomainRef, value)
                    } else {
                        null
                    }
                }
                // if found a match, add the target value
                if (srcValues.isEmpty()) {
                    null
                } else {
                    val exprInterp = ExpressionsInterpreterOverTypedObject<OT>(m2mExecution.targetAccessorMutator, _issues)
                    val evc = EvaluationContext.of(emptyMap<String, TypedObject<OT>>()) //TODO: are there any variables !
                    val drExp = vs[m2mExecution.targetDomainRef] ?: error("")
                    val value = exprInterp.evaluateExpression(evc, drExp)
                    srcValues + Pair(m2mExecution.targetDomainRef, value)
                }
            }?.toMap()
        }
        return listOf(MappingRecord(rule, matchingValues))
    }

    private fun matchSourceVariables(
        m2mExecution: M2mTransformExecution<OT>,
        rule: M2mTransformPatternRule,
        source: Map<DomainReference, List<TypedObject<OT>>>,
    ): Map<DomainReference, TemplateMatchAlternatives<OT>> {
        val srcDomainRefs = rule.domainSignature.filterKeys { k -> k != m2mExecution.targetDomainRef }
        val results = srcDomainRefs.map { (srcDomainRef, srcDomainItem) ->
            val srcOg = m2mExecution.domainAccessorMutator[srcDomainRef] ?: error("ObjectGraph not found for domain '$srcDomainRef'")
            val srcObjPat = rule.domainTemplate[srcDomainRef] ?: error("No object pattern found for domain '$srcDomainRef'")
            // match variables from source domain
            val srcList = source[srcDomainRef] ?: error("No source object found for domain '$srcDomainRef'")
            val alts = srcList.map { src ->
                matchVariablesFromRhs(emptyMap(), srcOg, src, srcObjPat)
            }.filter { it.isMatch }
            Pair(srcDomainRef, TemplateMatchAlternatives(alts.flatMap { it.alternatives }))
        }
        // results contains, for each source domain, a list of alternative MatchResults
        return results.toMap()
    }

    fun matchVariablesFromRhs(
        variables: Map<String, TypedObject<OT>>,
        srcObjectGraph: ObjectGraphAccessorMutator<OT>,
        src: TypedObject<OT>,
        rhs: PropertyTemplateRhs
    ): TemplateMatchAlternatives<OT> =
        when (rhs) {
            is PropertyTemplateExpression -> {
                val mr = matchVariablesFromPropertyTemplateExpression(variables, srcObjectGraph, src, rhs)
                // only keep the result if it is a match
                mr.takeIf { it.isMatch }?.let { TemplateMatchAlternatives(listOf(it)) } ?: TemplateMatchAlternatives(emptyList())
            }

            is ObjectTemplate -> matchVariablesFromObjectTemplate(variables, rhs, srcObjectGraph, src)
            is CollectionTemplate -> matchVariablesFromCollectionTemplate(variables, rhs, srcObjectGraph, src)
            else -> error("Unknown rhs type ${rhs::class}")
        }.let { tma ->
            rhs.identifier?.let { id ->
                tma.withVariable(id.value, src)
            } ?: tma
        }

    /**
     * always returns isMatch==true
     */
    fun matchVariablesFromPropertyTemplateExpression(
        variables: Map<String, TypedObject<OT>>,
        srcObjectGraph: ObjectGraphAccessorMutator<OT>,
        lhs: TypedObject<OT>,
        rhs: PropertyTemplateExpression
    ): TemplateMatchResult<OT> {
        // either :
        // 1) rhs is a free-variable with no value set, in which case set it to the lhs
        // 2) rhs is an expression with a value that matches the lhs
        val expr = rhs.expression
        return when {
            expr is RootExpression && variables.contains(expr.name).not() -> {
                TemplateMatchResult(true, mapOf(expr.name to lhs))
            }

            else -> {
                val exprInterp = ExpressionsInterpreterOverTypedObject<OT>(srcObjectGraph, _issues)
                val evc = EvaluationContext.of(variables)
                val value = exprInterp.evaluateExpression(evc, expr)
                val isMatch = srcObjectGraph.equalTo(lhs, value)
                TemplateMatchResult(isMatch, emptyMap())
            }
        }
    }

    fun matchVariablesFromObjectTemplate(
        variables: Map<String, TypedObject<OT>>,
        objectTemplate: ObjectTemplate,
        srcObjectGraph: ObjectGraphAccessorMutator<OT>,
        src: TypedObject<OT>
    ): TemplateMatchAlternatives<OT> {
        return when {
            objectTemplate.propertyTemplate.isEmpty() -> TemplateMatchAlternatives(listOf(TemplateMatchResult.EMPTY()))
            else -> {
                val propTemplateMatches = objectTemplate.propertyTemplate.map { (k, v) ->
                    val rhsPat = v.rhs
                    val lhs = srcObjectGraph.getProperty(src, k.value)
                    matchVariablesFromRhs(variables, srcObjectGraph, lhs, rhsPat)
                } //TODO: determine no match before cartesianProduct
                val result = propTemplateMatches.map { it.alternatives }.cartesianProduct()
                val alts = result.map { l -> l.merge() }.filter { it.isMatch }
                TemplateMatchAlternatives(alts)
            }
        }
    }

    fun matchVariablesFromCollectionTemplate(
        variables: Map<String, TypedObject<OT>>,
        collectionTemplate: CollectionTemplate,
        srcObjectGraph: ObjectGraphAccessorMutator<OT>,
        src: TypedObject<OT>
    ): TemplateMatchAlternatives<OT> {
        val result = when {
            src.type.isCollection -> {
                // TODO: maybe a faster way to do it if NOT isSubset!
                val elements = mutableListOf<TypedObject<OT>>()
                srcObjectGraph.forEachIndexed(src) { idx, el -> elements.add(el) } //TODO: find a way not to 'collect' the list
                if (collectionTemplate.isSubset.not() && elements.size != collectionTemplate.elements.size) {
                    // TODO: should return a no match not error!
                    error("Collection size does not match template size: ${elements.size} != ${collectionTemplate.elements.size} ")
                }
                // get the different alternative combinations of objects from elements that (cover) match the defined templates
                // Set(  List(element-match per template)  )
                val options = findCoveringSubsets2(
                    cover = collectionTemplate.elements,
                    bySubsetsOf = elements
                ) { tp, el ->
                    val mr = matchVariablesFromRhs(variables, srcObjectGraph, el, tp)
                    Pair(mr.alternatives.isNotEmpty(), mr)
                }
                options.map { opt ->
                    opt.flatMap { it.alternatives }.merge()
                }
            }

            else -> error("src is not a collection")
        }
        return TemplateMatchAlternatives(result)
    }

    fun executeWhen(
        m2mExecution: M2mTransformExecution<OT>,
        owningRule: M2mTransformRule,
        when_: Expression,
        matchedVariables: Map<String, TypedObject<OT>>,
    ): Pair<Boolean, Map<String, TypedObject<OT>>> {
        // TODO: use an extended Expression evaluator that handles the RuleWhen options, so we can have compound when-expressions
        return when (when_) {
            is RuleWhenRelationHolds, is RuleWhenMappingHolds -> when_.resolved?.let { rule ->
                val source = getSource(m2mExecution, rule, when_.arguments, matchedVariables)
                val rec = m2mExecution.records[rule]
                val found = rec?.let {
                    rec.alternatives.firstOrNull { mapping ->
                        mapping.entries.all { (dr, v) ->
                            if (dr == m2mExecution.targetDomainRef) {
                                // do not match it
                                true
                            } else {
                                source[dr]?.any { //FIXME: this maybe not correct if multiple alternatives !
                                    m2mExecution.domainAccessorMutator[dr]!!.equalTo(v, it)
                                }
                                    ?: error("Domain reference '${dr.value}' not found!") //should not happen
                            }
                        }
                    }
                }
                found?.let {
                    val targetDomainRefIdx = rule.domainSignature.keys.indexOf(m2mExecution.targetDomainRef)
                    val targetArg = when_.arguments.getOrNull(targetDomainRefIdx)
                    val tgtValue = found[m2mExecution.targetDomainRef]
                    val vars: Map<String, TypedObject<OT>> = when(targetArg) {
                        null -> TODO()
                        is RootExpression -> when(tgtValue) {
                            null -> TODO()
                            else -> mapOf(targetArg.name to tgtValue)
                        }
                        else -> {
                            _issues.warn(
                                null,
                                "Argument for target domain '${m2mExecution.targetDomainRef.value}' must be a variable, in rule call '${when_.ruleName.value}' in 'when' clause of rule '${m2mExecution.targetTransform.name.value}.${owningRule.name.value}'."
                            )
                            matchedVariables
                        }
                    }
                    Pair(true, vars)
                } ?: Pair(false, emptyMap())
            } ?: run {
                _issues.error(null, "In 'when' clause of rule '${owningRule.name.value}', rule '${when_.ruleName.value}' is unresolved in '${m2mExecution.targetTransform.name.value}'.")
                Pair(false, emptyMap<String, TypedObject<OT>>())
            }

            is RuleWhenRelationHoldsForAll -> TODO()
            is RuleWhenMappingHoldsForAll -> TODO()
            else -> {
                val srcOg = m2mExecution.domainAccessorMutator.entries.filterNot { it.key == m2mExecution.targetDomainRef }.first().value //should never be null!
                val exprInterp = ExpressionsInterpreterOverTypedObject<OT>(srcOg, _issues) //FIXME: which og to use? all might be needed!
                val evc = EvaluationContext.of(matchedVariables)
                val res = exprInterp.evaluateExpression(evc, when_)
                val v: Boolean = when {
                    res.type.conformsTo(StdLibDefault.Boolean) -> {
                        srcOg.valueOf(res) as Boolean
                    }

                    else -> TODO()
                }
                Pair(v, emptyMap())
            }
        }
    }

    /*
     * returns matchedVariables + variables set by executing the where
     */
    fun executeWhere(
        m2mExecution: M2mTransformExecution<OT>,
        owningRule: M2mTransformRule,
        where: RuleWhere,
        matchedVariables: Map<String, TypedObject<OT>>,
    ): Map<String, TypedObject<OT>> {
        return when (where) { //TODO: support more complex expressions - override the expression interpreter to intercept function calls as rule-calls
            is RuleWhereCallRelation, is RuleWhereCallMapping -> where.resolved?.let { rule ->
                val source = getSource(m2mExecution, rule, where.arguments, matchedVariables)
                val tgtValue = executeRuleWhere(m2mExecution, owningRule, rule, source)
                val targetDomainRefIdx = rule.domainSignature.keys.indexOf(m2mExecution.targetDomainRef)
                val targetArg = where.arguments.getOrNull(targetDomainRefIdx)
                when (targetArg) {
                    null -> {
                        _issues.error(
                            null,
                            "Argument for target domain '${m2mExecution.targetDomainRef.value}' not found in rule call '${where.ruleName.value}' in 'where' clause of TransformRuleSet '${m2mExecution.targetTransform.name.value}'."
                        )
                        matchedVariables
                    }

                    is RootExpression -> when (tgtValue) {
                        null -> matchedVariables + Pair(targetArg.name, m2mExecution.targetAccessorMutator.nothing())
                        else -> matchedVariables + Pair(targetArg.name, tgtValue)
                    }

                    else -> {
                        _issues.warn(
                            null,
                            "Argument for target domain '${m2mExecution.targetDomainRef.value}' must be a variable, in rule call '${where.ruleName.value}' in 'where' clause of rule '${m2mExecution.targetTransform.name.value}.${owningRule.name.value}'."
                        )
                        matchedVariables
                    }
                }
            } ?: run {
                _issues.error(null, "In 'where' clause of rule '${owningRule.name.value}', rule '${where.ruleName.value}' is unresolved in '${m2mExecution.targetTransform.name.value}'.")
                matchedVariables
            }

            is RuleWhereCallRelationForAll, is RuleWhereCallMappingForAll -> where.resolved?.let { rule ->
                val source = getSource(m2mExecution, rule, where.arguments, matchedVariables)
                val tgtValue = when {
                    source.all { (k, v) -> v.all { it.type.isCollection } } -> {
                        val sourceElements = mutableListOf<Map<DomainReference, List<TypedObject<OT>>>>()
                        val sourceCollType = mutableListOf<TypeInstance>()
                        source.entries.forEach { (dr, alts) ->
                            alts.forEach { to ->
                                sourceCollType.add(to.type)
                                val og = m2mExecution.domainAccessorMutator[dr]!!
                                og.forEachIndexed(to) { idx, el ->
                                    if (idx >= sourceElements.size) {
                                        sourceElements.add(mutableMapOf())
                                    }
                                    if (null == sourceElements[idx][dr]) {
                                        (sourceElements[idx] as MutableMap)[dr] = mutableListOf()
                                    }
                                    (sourceElements[idx][dr] as MutableList).add(el)
                                }
                            }
                        }
                        val tgtList = sourceElements.mapNotNull { src ->
                            executeRuleWhere(m2mExecution, owningRule, rule, src)
                        }
                        //if(sourceCollType.all { it. }) //TODO: check all colls are type of tgt
                        m2mExecution.targetAccessorMutator!!.createCollection(sourceCollType.first().qualifiedTypeName, tgtList)
                    }

                    else -> {
                        _issues.error(
                            null,
                            "In 'where' clause of rule '${owningRule.name.value}' in '${m2mExecution.targetTransform.name.value}', the all call to rule '${where.ruleName.value}' is expecting a collection."
                        )
                        null
                    }
                }
                val targetDomainRefIdx = rule.domainSignature.keys.indexOf(m2mExecution.targetDomainRef)
                val targetArg = where.arguments.getOrNull(targetDomainRefIdx)
                when (targetArg) {
                    null -> {
                        _issues.error(
                            null,
                            "Argument for target domain '${m2mExecution.targetDomainRef.value}' not found in rule call '${where.ruleName.value}' in 'where' clause of TransformRuleSet '${m2mExecution.targetTransform.name.value}'."
                        )
                        matchedVariables
                    }

                    is RootExpression -> when (tgtValue) {
                        null -> matchedVariables + Pair(targetArg.name, m2mExecution.targetAccessorMutator.nothing())
                        else -> matchedVariables + Pair(targetArg.name, tgtValue)
                    }

                    else -> {
                        _issues.warn(
                            null,
                            "Argument for target domain '${m2mExecution.targetDomainRef.value}' must be a variable, in rule call '${where.ruleName.value}' in 'where' clause of rule '${m2mExecution.targetTransform.name.value}.${owningRule.name.value}'."
                        )
                        matchedVariables
                    }
                }
            } ?: run {
                _issues.error(null, "In 'where' clause of rule '${owningRule.name.value}', rule '${where.ruleName.value}' is unresolved in '${m2mExecution.targetTransform.name.value}'.")
                matchedVariables
            }

            else -> {
                _issues.error(null, "Cannot execute the 'where' clause.")
                matchedVariables
            }
        }
    }

    /*
     * returns null if the where-rule matches nothing
     */
    fun executeRuleWhere(
        m2mExecution: M2mTransformExecution<OT>,
        owningRule: M2mTransformRule,
        rule: M2mTransformRule,
        source: Map<DomainReference, List<TypedObject<OT>>>
    ): TypedObject<OT>? {
        val recList = executeRule(m2mExecution, rule, source)
        val merged = recList.merge(rule)
        return when (merged.alternatives.size) {
            0 -> {
                _issues.warn(null, "In rule '${owningRule.name.value}' the 'where' clause matched nothing.")
                null
            }

            1 -> merged.alternatives.first()[m2mExecution.targetDomainRef]

            else -> TODO("handle multiple matches from where")
        }
    }

    fun getSource(
        m2mExecution: M2mTransformExecution<OT>,
        rule: M2mTransformRule,
        arguments: List<Expression>,
        matchedVariables: Map<String, TypedObject<OT>>,
    ): Map<DomainReference, List<TypedObject<OT>>> {
        val argsValues = arguments.mapIndexed { idx, argExpr ->
            val domainRef = rule.domainSignature.keys.elementAt(idx)
            Pair(domainRef, argExpr)
        }.associate { it }
        return (argsValues - m2mExecution.targetDomainRef).mapValues { (k, v) ->
            val og = m2mExecution.domainAccessorMutator[k] ?: error("Cannot find ObjectGraph for domain reference '${k.value}'.")
            val exprInterp = ExpressionsInterpreterOverTypedObject<OT>(og, _issues)
            val evc = EvaluationContext.of(matchedVariables)
            val res = exprInterp.evaluateExpression(evc, v)
            listOf(res)
        }
    }

    //TODO: merge code with above efficiently
    fun evaluateArgs(
        m2mExecution: M2mTransformExecution<OT>,
        rule: M2mTransformRule,
        arguments: List<Expression>,
        matchedVariables: Map<String, TypedObject<OT>>,
    ): Map<DomainReference, List<TypedObject<OT>>> {
        val argsValues = arguments.mapIndexed { idx, argExpr ->
            val domainRef = rule.domainSignature.keys.elementAt(idx)
            Pair(domainRef, argExpr)
        }.associate { it }
        return argsValues.mapValues { (k, v) ->
            val og = m2mExecution.domainAccessorMutator[k] ?: error("Cannot find ObjectGraph for domain reference '${k.value}'.")
            val exprInterp = ExpressionsInterpreterOverTypedObject<OT>(og, _issues)
            val evc = EvaluationContext.of(matchedVariables)
            val res = exprInterp.evaluateExpression(evc, v)
            listOf(res)
        }
    }

    fun createFromRhs(variables: Map<String, TypedObject<OT>>, lhsType: TypeInstance, rhs: PropertyTemplateRhs, tgtObjectGraph: ObjectGraphAccessorMutator<OT>): TypedObject<OT> = when (rhs) {
        is PropertyTemplateExpression -> createFromExpression(variables, lhsType, rhs.expression, tgtObjectGraph)
        is ObjectTemplate -> createFromObjectTemplate(variables, lhsType, rhs, tgtObjectGraph)
        is CollectionTemplate -> createFromCollectionTemplate(variables, lhsType, rhs, tgtObjectGraph)
        else -> error("Unknown rhs type ${rhs::class}")
    }

    /**
     * returns value of expression evaluated in context of provided variables
     */
    fun createFromExpression(variables: Map<String, TypedObject<OT>>,lhsType: TypeInstance, expression: Expression, tgtObjectGraph: ObjectGraphAccessorMutator<OT>): TypedObject<OT> {
        val exprInterp = ExpressionsInterpreterOverTypedObject<OT>(tgtObjectGraph, _issues)
        return when (expression) {
            is CreateObjectExpression -> {
                val evc = EvaluationContext.of(variables)
                exprInterp.constructObject(evc, expression)
            }

            else -> {
                val evc = EvaluationContext.of(variables)
                val value = exprInterp.evaluateExpression(evc, expression)
                return value
            }
        }
    }

    fun createFromObjectTemplate(variables: Map<String, TypedObject<OT>>, lhsType: TypeInstance, objectTemplate: ObjectTemplate, tgtObjectGraph: ObjectGraphAccessorMutator<OT>): TypedObject<OT> {
//        objectTemplate.resolveType(tgtObjectGraph.typesDomain)
        val decl = objectTemplate.type.resolvedDeclaration
        return when (decl) {
            is DataType, is ValueType -> {
                val constructors = when (decl) {
                    is DataType -> decl.constructors
                    is ValueType -> decl.constructors
                    else -> error("Type '${decl.qualifiedName.value}' has no constructors")
                }
                val possibleConArgNames = constructors.flatMap { it -> it.parameters.map { it.name.value } } //FIXME: this is not really accurate!
                val conArgs = mutableMapOf<String, TypedObject<OT>>()
                objectTemplate.propertyTemplate.forEach { (k, v) ->
                    if (possibleConArgNames.contains(k.value)) {
                        val value = createFromRhs(variables, lhsType, v.rhs, tgtObjectGraph)
                        conArgs[k.value] = value
                    }
                }
                //.resolveType(tgtObjectGraph.typesDomain)
                tgtObjectGraph.createStructureValue(objectTemplate.type.qualifiedTypeName, conArgs)
            }

            else -> error("Cannot construct object of type ${decl.qualifiedName.value}")
        }
    }

    fun createFromCollectionTemplate(variables: Map<String, TypedObject<OT>>, lhsType: TypeInstance, collectionTemplate: CollectionTemplate, tgtObjectGraph: ObjectGraphAccessorMutator<OT>): TypedObject<OT> {
        val elements = collectionTemplate.elements.map {
            createFromRhs(variables, lhsType.typeArguments[0].type, it, tgtObjectGraph)
        }
        val col = tgtObjectGraph.createCollection(lhsType.qualifiedTypeName, elements)
        return col
    }

    fun setPropertiesFromRhs(obj: TypedObject<OT>, variables: Map<String, TypedObject<OT>>, rhs: PropertyTemplateRhs, tgtObjectGraph: ObjectGraphAccessorMutator<OT>): TypedObject<OT> = when (rhs) {
        is PropertyTemplateExpression -> setPropertiesFromExpression(obj, variables, rhs.expression, tgtObjectGraph)
        is ObjectTemplate -> setPropertiesFromObjectTemplate(obj, variables, rhs, tgtObjectGraph)
        else -> error("Unknown rhs type ${rhs::class}")
    }

    fun setPropertiesFromExpression(
        obj: TypedObject<OT>,
        variables: Map<String, TypedObject<OT>>,
        expression: Expression,
        tgtObjectGraph: ObjectGraphAccessorMutator<OT>
    ): TypedObject<OT> {
        val exprInterp = ExpressionsInterpreterOverTypedObject<OT>(tgtObjectGraph, _issues)
        when (expression) {
            is CreateObjectExpression -> {
                val evc = EvaluationContext.of(variables)
                exprInterp.propertyAssignmentBlock(evc, obj, expression.propertyAssignments)
            }

            else -> {
                // nothing to do, already done when createFromPropertyPatternExpression was called
            }
        }
        return obj
    }

    fun setPropertiesFromObjectTemplate(
        obj: TypedObject<OT>,
        variables: Map<String, TypedObject<OT>>,
        objectTemplate: ObjectTemplate,
        tgtObjectGraph: ObjectGraphAccessorMutator<OT>
    ): TypedObject<OT> {
        val lhsType =obj.type
        val propValues = mutableMapOf<String, TypedObject<OT>>()
        objectTemplate.propertyTemplate.forEach { (k, v) ->
            val propType = lhsType.allResolvedProperty[PropertyName( v.propertyName.value)]?.typeInstance ?: StdLibDefault.AnyType //TODO: maybe warn if not found?
            val value = createFromRhs(variables, propType, v.rhs, tgtObjectGraph)
            propValues[k.value] = value
        }
        propValues.forEach { (k, v) ->
            val pn = PropertyName(k)
            if (true == objectTemplate.type.allResolvedProperty[pn]?.isReadWrite) {
                tgtObjectGraph.setProperty(obj, k, v)
            }
        }
        return obj
    }

}