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
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.api.FunctionCall
import net.akehurst.language.expressions.api.RootExpression
import net.akehurst.language.expressions.processor.ExpressionsInterpreterOverTypedObject
import net.akehurst.language.expressions.processor.ExpressionsInterpreterOverTypedObjectSuspending
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.m2mTransform.api.*
import net.akehurst.language.m2mTransform.processor.MappingRecord.Companion.merge
import net.akehurst.language.m2mTransform.processor.TemplateMatchResult.Companion.merge
import net.akehurst.language.objectgraph.api.ObjectGraphAccessorMutator
import net.akehurst.language.objectgraph.api.ObjectGraphAccessorMutatorSuspending
import net.akehurst.language.objectgraph.api.TypedObject
import net.akehurst.language.types.api.PropertyName
import net.akehurst.language.types.asm.StdLibDefault
import kotlin.collections.get

data class M2MTransformResult<OT : Any>(
    val issues: IssueHolder,
    val record: Map<M2mTransformRule, MappingRecord<OT>>,
    val targetDomainRef: DomainReference
) {
    val targets: List<TypedObject<OT>> get() = record.values.flatMap { it.alternatives.map { it[targetDomainRef]!! } }

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

class M2mTransformInterpreter<OT : Any>(
    val m2m: M2mTransformDomain,
    val domainObjectGraph: Map<SimpleName, ObjectGraphAccessorMutator<OT>>,
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

    val records = mutableMapOf<M2mTransformRule, List<MappingRecord<OT>>>()

    /**
     * @param targetDomainRef reference to the domain that is the target of the transformation
     * @param domainGraphs the objects for each source domain, keyed by the domain reference
     */
    fun transform(targetTransform: M2mTransformRuleSet, targetDomainRef: DomainReference, domainGraphs: Map<DomainReference, List<TypedObject<OT>>>): M2MTransformResult<OT> {
        val objectGraphHandler = targetTransform.domainParameters.entries.associate { (k, v) ->
            Pair(k, domainObjectGraph[v] ?: error("Domain ObjectGraph not found for domain $k"))
        }

        val result = when {
            targetTransform.topRule.isEmpty() -> {
                _issues.error(null, "No conforming top rule found for target domain '${targetDomainRef.value}'")
                emptyList()
            }

            else -> {
                targetTransform.topRule.flatMap { topRule ->
                    executeRule(targetTransform, topRule, targetDomainRef, domainGraphs, objectGraphHandler)
                }
            }
        }
        val r = result.associateBy { it.rule }
        return M2MTransformResult(_issues, r, targetDomainRef)
    }

    private fun executeRule(
        targetTransform: M2mTransformRuleSet,
        rule: M2mTransformRule,
        targetDomainRef: DomainReference,
        source: Map<DomainReference, List<TypedObject<OT>>>,
        sourceObjectGraph: Map<DomainReference, ObjectGraphAccessorMutator<OT>>,
    ): List<MappingRecord<OT>> = when (rule) {
        is M2mTransformAbstractRule -> executeAbstract(targetTransform, rule, targetDomainRef, source, sourceObjectGraph)
        is M2MTransformRelation -> executeRelation(targetTransform, rule, targetDomainRef, source, sourceObjectGraph)
        is M2MTransformMapping -> executeMapping(targetTransform, rule, targetDomainRef, source, sourceObjectGraph)
        is M2MTransformTable -> executeTable(targetTransform, rule, targetDomainRef, source, sourceObjectGraph)
        else -> error("Unknown rule type ${rule::class}")
    }.also { records[rule] = it }

    private fun executeAbstract(
        targetTransform: M2mTransformRuleSet,
        rule: M2mTransformAbstractRule,
        targetDomainRef: DomainReference,
        source: Map<DomainReference, List<TypedObject<OT>>>,
        objectGraph: Map<DomainReference, ObjectGraphAccessorMutator<OT>>,
    ): List<MappingRecord<OT>> {
        val subrules = targetTransform.rule.values.filter { r -> r !is M2mTransformAbstractRule && r.conformsTo(rule) }
        val records = subrules.flatMap { sr ->
            executeRule(targetTransform, sr, targetDomainRef, source, objectGraph)
        }
        return records
    }

    private fun executeMapping(
        targetTransform: M2mTransformRuleSet,
        rule: M2MTransformMapping,
        targetDomainRef: DomainReference,
        source: Map<DomainReference, List<TypedObject<OT>>>,
        objectGraph: Map<DomainReference, ObjectGraphAccessorMutator<OT>>,
    ): List<MappingRecord<OT>> {
        val tgtOg = objectGraph[targetDomainRef] ?: error("ObjectGraph not found for domain '$targetDomainRef'")
        // for each domain reference get the match alternatives for each source object
        // Map of DomainReference -> List< TemplateMatchAlternatives per object >
        val domToListOfAlts = matchSourceVariables(rule, targetDomainRef, source, objectGraph)

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
                        val expression = rule.expression[targetDomainRef]
                        when (expression) {
                            null -> {
                                _issues.error(null, "No expression found for target domain ref '$targetDomainRef' of rule '${rule.name}'.")
                                emptyMap()
                            }

                            else -> {
                                val allVars = alt.values.merge()

                                val whenResult = rule.when_?.let {
                                    executeWhen(targetTransform, rule, it, targetDomainRef, allVars.matchedVariables, objectGraph)
                                } ?: true

                                if (whenResult) {
                                    val varsAfterWhere = rule.where.map {
                                        executeWhere(targetTransform, rule, it, targetDomainRef, allVars.matchedVariables, objectGraph)
                                    }
                                    //TODO: add target of where to variables with correct name

                                    val exprInterp = ExpressionsInterpreterOverTypedObject<OT>(tgtOg, _issues)
                                    val evc = EvaluationContext.of(allVars.matchedVariables)
                                    val r = exprInterp.evaluateExpression(evc, expression)
                                    val srcs = alt.entries.associate { (srcDomainRef, mr) ->
                                        val srcObjPat = rule.domainTemplate[srcDomainRef] ?: error("No object pattern found for domain '$srcDomainRef'")
                                        val srcId = srcObjPat.identifier?.value ?: error("No identifier found for matched object in domain '$srcDomainRef'")
                                        val src = mr.getValueNamed(srcId) ?: error("No matched object found for identifier '$srcId'")
                                        Pair(srcDomainRef, src)
                                    }
                                    srcs + Pair(targetDomainRef, r)
                                } else {
                                    _issues.info(null, "when clause evaluated to false for target domain ref '$targetDomainRef' of rule '${rule.name}'.")
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
        targetTransform: M2mTransformRuleSet,
        rule: M2MTransformRelation,
        targetDomainRef: DomainReference,
        source: Map<DomainReference, List<TypedObject<OT>>>,
        objectGraph: Map<DomainReference, ObjectGraphAccessorMutator<OT>>,
    ): List<MappingRecord<OT>> {
        val tgtOg = objectGraph[targetDomainRef] ?: error("ObjectGraph not found for domain '$targetDomainRef'")
        val domToListOfAlts = matchSourceVariables(rule, targetDomainRef, source, objectGraph)
        val res = when {
            domToListOfAlts.isEmpty() -> {
                _issues.warn(null, "No matches found in source domains.")
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
                        val varsAfterWhere = rule.where.map {
                            executeWhere(targetTransform, rule, it, targetDomainRef, allVars.matchedVariables, objectGraph)
                        }
                        val objPat = rule.domainTemplate[targetDomainRef] ?: error("No object pattern found for domain '$targetDomainRef'")
                        val r = createFromRhs(allVars.matchedVariables, objPat, tgtOg)
                        val srcs = alt.entries.associate { (srcDomainRef, v) ->
                            val srcObjPat = rule.domainTemplate[srcDomainRef] ?: error("No object pattern found for domain '$srcDomainRef'")
                            val srcId = srcObjPat.identifier?.value ?: error("No identifier found for matched object in domain '$srcDomainRef'")
                            val src = v.getValueNamed(srcId) ?: error("No matched object found for identifier '$srcId'")
                            Pair(srcDomainRef, src)
                        }
                        srcs + Pair(targetDomainRef, r)
                    }
                }
            }
        }
        return listOf(MappingRecord(rule, res))
    }

    private fun executeTable(
        targetTransform: M2mTransformRuleSet,
        rule: M2MTransformTable,
        targetDomainRef: DomainReference,
        source: Map<DomainReference, List<TypedObject<OT>>>,
        objectGraph: Map<DomainReference, ObjectGraphAccessorMutator<OT>>,
    ): List<MappingRecord<OT>> {
        val matchingValues = source.cartesianProduct().mapNotNull { srcAlt ->
            rule.values.firstNotNullOfOrNull { vs ->
                val srcValues = srcAlt.mapNotNull { (srcDomainRef, v) ->
                    val srcObjectGraph = objectGraph[srcDomainRef] ?: error("ObjectGraph not found for domain '$srcDomainRef'")
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
                    val tgtObjectGraph = objectGraph[targetDomainRef] ?: error("ObjectGraph not found for domain '$targetDomainRef'")
                    val exprInterp = ExpressionsInterpreterOverTypedObject<OT>(tgtObjectGraph, _issues)
                    val evc = EvaluationContext.of(emptyMap<String, TypedObject<OT>>()) //TODO: are there any variables !
                    val drExp = vs[targetDomainRef] ?: error("")
                    val value = exprInterp.evaluateExpression(evc, drExp)
                    srcValues + Pair(targetDomainRef, value)
                }
            }?.toMap()
        }
        return listOf(MappingRecord(rule, matchingValues))
    }

    private fun matchSourceVariables(
        rule: M2mTransformPatternRule,
        targetDomainRef: DomainReference,
        source: Map<DomainReference, List<TypedObject<OT>>>,
        objectGraph: Map<DomainReference, ObjectGraphAccessorMutator<OT>>,
    ): Map<DomainReference, TemplateMatchAlternatives<OT>> {
        val srcDomainRefs = rule.domainSignature.filterKeys { k -> k != targetDomainRef }
        val results = srcDomainRefs.map { (srcDomainRef, srcDomainItem) ->
            val srcOg = objectGraph[srcDomainRef] ?: error("ObjectGraph not found for domain '$srcDomainRef'")
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
        targetTransform: M2mTransformRuleSet,
        owningRule: M2mTransformRule,
        when_: Expression,
        targetDomainRef: DomainReference,
        matchedVariables: Map<String, TypedObject<OT>>,
        objectGraph: Map<DomainReference, ObjectGraphAccessorMutator<OT>>,
    ): Boolean {
        // TODO: use an extended Expression evaluator that handles the RuleWhen options, so we can have compound when-expressions
        return when (when_) {
            is RuleWhenRelationHolds -> TODO()
            is RuleWhenRelationHoldsForAll -> TODO()
            is RuleWhenMappingHolds -> TODO()
            is RuleWhenMappingHoldsForAll -> TODO()
            else -> {
                val srcOg = objectGraph.entries.filterNot { it.key == targetDomainRef }.first().value //should never be null!
                val exprInterp = ExpressionsInterpreterOverTypedObject<OT>(srcOg, _issues) //FIXME: which og to use? all might be needed!
                val evc = EvaluationContext.of(matchedVariables)
                val res = exprInterp.evaluateExpression(evc, when_)
                when {
                    res.type.conformsTo(StdLibDefault.Boolean) -> {
                        srcOg.untyped(res) as Boolean
                    }

                    else -> TODO()
                }
            }
        }
    }

    /*
     * returns matchedVariables + variables set by executing the where
     */
    fun executeWhere(
        targetTransform: M2mTransformRuleSet,
        owningRule: M2mTransformRule,
        where: RuleWhere,
        targetDomainRef: DomainReference,
        matchedVariables: Map<String, TypedObject<OT>>,
        objectGraph: Map<DomainReference, ObjectGraphAccessorMutator<OT>>,
    ): Map<String, TypedObject<OT>> {
        return when (where) { //TODO: support more complex expressions - override the expression interpreter to intercept function calls as rule-calls
            is RuleWhereCallRelation, is RuleWhereCallMapping -> where.resolved?.let { rule ->
                val source = getSource(rule, targetDomainRef, where.arguments, matchedVariables, objectGraph)
                val recList = executeRule(targetTransform, rule, targetDomainRef, source, objectGraph)
                val merged = recList.merge(rule)
                when (merged.alternatives.size) {
                    0 -> {
                        _issues.warn(null, "In rule '${owningRule.name.value}' the 'where' clause matched nothing.")
                        matchedVariables
                    }

                    1 -> {
                        val rec = merged
                        val targetDomainRefIdx = rule.domainSignature.keys.indexOf(targetDomainRef)
                        val targetArg = where.arguments.getOrNull(targetDomainRefIdx)
                        when (targetArg) {
                            null -> {
                                _issues.error(
                                    null,
                                    "Argument for target domain '${targetDomainRef.value}' not found in rule call '${where.ruleName.value}' in 'where' clause of TransformRuleSet '${targetTransform.name}'."
                                )
                                matchedVariables
                            }

                            is RootExpression -> {
                                when (rec.alternatives.size) {
                                    0 -> TODO()
                                    1 -> {
                                        val tgtValue = rec.alternatives.first()[targetDomainRef]
                                        when (tgtValue) {
                                            null -> TODO()
                                            else -> matchedVariables + Pair(targetArg.name, tgtValue)
                                        }
                                    }

                                    else -> TODO()
                                }
                            }

                            else -> {
                                _issues.warn(
                                    null,
                                    "Argument for target domain '${targetDomainRef.value}' must be a variable, in rule call '${where.ruleName.value}' in 'where' clause of rule '${targetTransform.name.value}.${owningRule.name.value}'."
                                )
                                matchedVariables
                            }
                        }
                    }

                    else -> TODO("handle multiple matches from where")
                }
            } ?: run {
                _issues.error(null, "In 'where' clause of rule '${owningRule.name.value}', rule '${where.ruleName.value}' is unresolved in '${targetTransform.name}'.")
                matchedVariables
            }

            else -> {
                _issues.error(null, "Cannot execute the 'where' clause.")
                matchedVariables
            }
        }
    }

    fun getSource(
        rule: M2mTransformRule,
        targetDomainRef: DomainReference,
        arguments: List<Expression>,
        matchedVariables: Map<String, TypedObject<OT>>,
        objectGraph: Map<DomainReference, ObjectGraphAccessorMutator<OT>>,
    ): Map<DomainReference, List<TypedObject<OT>>> {
        val argsValues = arguments.mapIndexed { idx, argExpr ->
            val domainRef = rule.domainSignature.keys.elementAt(idx)
            Pair(domainRef, argExpr)
        }.associate { it }
        return (argsValues - targetDomainRef).mapValues { (k, v) ->
            val og = objectGraph[k] ?: error("Cannot find ObjectGraph for domain reference '${k.value}'.")
            val exprInterp = ExpressionsInterpreterOverTypedObject<OT>(og, _issues)
            val evc = EvaluationContext.of(matchedVariables)
            val res = exprInterp.evaluateExpression(evc, v)
            listOf(res)
        }
    }

    fun createFromRhs(variables: Map<String, TypedObject<OT>>, rhs: PropertyTemplateRhs, tgtObjectGraph: ObjectGraphAccessorMutator<OT>): TypedObject<OT> = when (rhs) {
        is PropertyTemplateExpression -> createFromPropertyPatternExpression(variables, rhs, tgtObjectGraph)
        is ObjectTemplate -> createFromObjectPattern(variables, rhs, tgtObjectGraph)
        else -> error("Unknown rhs type ${rhs::class}")
    }

    /**
     * returns value of expression evaluated in context of provided variables
     */
    fun createFromPropertyPatternExpression(variables: Map<String, TypedObject<OT>>, ppe: PropertyTemplateExpression, tgtObjectGraph: ObjectGraphAccessorMutator<OT>): TypedObject<OT> {
        val expr = ppe.expression
        val exprInterp = ExpressionsInterpreterOverTypedObject<OT>(tgtObjectGraph, _issues)
        val evc = EvaluationContext.of(variables)
        val value = exprInterp.evaluateExpression(evc, expr)
        return value
    }

    fun createFromObjectPattern(variables: Map<String, TypedObject<OT>>, objectTemplate: ObjectTemplate, tgtObjectGraph: ObjectGraphAccessorMutator<OT>): TypedObject<OT> {
        val propValues = mutableMapOf<String, TypedObject<OT>>()
        objectTemplate.propertyTemplate.forEach { (k, v) ->
            val value = createFromRhs(variables, v.rhs, tgtObjectGraph)
            propValues[k.value] = value
        }
        objectTemplate.resolveType(tgtObjectGraph.typesDomain)
        val obj = tgtObjectGraph.createStructureValue(objectTemplate.type.qualifiedTypeName, propValues)
        propValues.forEach { (k, v) ->
            val pn = PropertyName(k)
            if (true == objectTemplate.type.allResolvedProperty[pn]?.isReadWrite) {
                tgtObjectGraph.setProperty(obj, k, v)
            }
        }
        return obj
    }

}