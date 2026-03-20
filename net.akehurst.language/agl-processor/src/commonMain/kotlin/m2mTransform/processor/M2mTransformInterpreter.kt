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

import net.akehurst.language.agl.m2mTransform.processor.interpreter.M2mPatternExecution
import net.akehurst.language.agl.m2mTransform.processor.interpreter.M2mPatternExecutor
import net.akehurst.language.agl.m2mTransform.processor.interpreter.M2mPatternExecutor.Companion.RESULT
import net.akehurst.language.base.api.Indent
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.expressions.api.CreateObjectExpression
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.api.RootExpression
import net.akehurst.language.expressions.processor.ExpressionsInterpreterOverTypedObject
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.m2mTransform.api.*
import net.akehurst.language.m2mTransform.processor.TemplateMatchResult.Companion.merge
import net.akehurst.language.objectgraph.api.EvaluationContext
import net.akehurst.language.objectgraph.api.ObjectGraphAccessorMutator
import net.akehurst.language.objectgraph.api.TypedObject
import net.akehurst.language.types.api.DataType
import net.akehurst.language.types.api.PropertyName
import net.akehurst.language.types.api.TypeInstance
import net.akehurst.language.types.api.ValueType
import net.akehurst.language.types.asm.StdLibDefault
import kotlin.collections.component1
import kotlin.collections.component2

data class M2MTransformResult(
    val issues: IssueHolder,
    val record: Map<M2mTransformRule, MappingRecord>,
    val targetDomainRef: DomainReference
) {
    val targets: List<TypedObject>
        get() = record.values.flatMap {
            if (it.rule.isTop) {
                it.alternatives.mapNotNull { it[targetDomainRef] }
            } else {
                emptyList()
            }
        }

    fun asString(indent: Indent = Indent()): String {
        val sb = StringBuilder()
        sb.appendLine("${indent}M2M Transform Result:")
        for ((k, v) in record) {
            val recIndent = indent.inc
            sb.appendLine("${recIndent}rule ${k.name}:")
            sb.appendLine(v.asString(recIndent.inc))
        }
        return sb.toString()
    }
}

data class TemplateMatchAlternatives(
    val alternatives: List<TemplateMatchResult>
) {
    val isMatch: Boolean get() = alternatives.isNotEmpty()

    fun merge(): TemplateMatchResult = this.alternatives.merge()

    fun withVariable(name: String, value: TypedObject): TemplateMatchAlternatives {
        return TemplateMatchAlternatives(
            this.alternatives.map { it.withVariable(name, value) }
        )
    }
}

data class TemplateMatchResult(
    val isMatch: Boolean,
    val matchedVariables: Map<String, TypedObject>
) {
    companion object {
        fun EMPTY() = TemplateMatchResult(true, emptyMap())
        fun Collection<TemplateMatchResult>.merge() = this.fold(TemplateMatchResult.EMPTY()) { acc, it -> acc.merge(it) }
    }

    fun getValueNamed(name: String): TypedObject? {
        return this.matchedVariables[name]
    }

    fun withVariable(name: String, value: TypedObject): TemplateMatchResult =
        TemplateMatchResult(
            this.isMatch,
            matchedVariables + Pair(name, value)
        )

    fun merge(other: TemplateMatchResult): TemplateMatchResult {
        return TemplateMatchResult(this.isMatch && other.isMatch, this.matchedVariables + other.matchedVariables)
    }
}

data class MappingRecord(
    val rule: M2mTransformRule,
    /**
     * multiple source objects (or combinations of sources) could be matched by each top rule
     */
    val alternatives: List<Map<DomainReference, TypedObject>>
) {
    companion object {
        fun EMPTY(rule: M2mTransformRule) = MappingRecord(rule, emptyList())
        fun Collection<MappingRecord>.merge(rule: M2mTransformRule) = this.fold(MappingRecord.EMPTY(rule)) { acc, it -> acc.merge(it) }
    }

    fun merge(other: MappingRecord): MappingRecord {
        check(rule == other.rule) { "Can only merge MappingRecord if rules are the same." }
        return MappingRecord(rule, alternatives + other.alternatives)
    }

    fun bySource(source: Map<DomainReference, List<TypedObject>>): List<Map<DomainReference, TypedObject>> {
        return alternatives.filter { rec ->
            source.all { (k, v) ->
                v.contains(rec[k]!!)
            }
        }

    }

    fun asString(indent: Indent = Indent()): String {
        val sb = StringBuilder()
        sb.appendLine("${indent}MappingRecord:")
        for (i in alternatives.indices) {
            val altIndent = indent.inc
            val entryIndent = altIndent.inc
            sb.appendLine("${altIndent}$i:")
            sb.append(alternatives[i].entries.joinToString("") { "${entryIndent}domain ${it.key.value} ${it.value}\n" })
        }
        return sb.toString()
    }

}

class M2mTransformExecutionContext(

) {
    //val evaluationContext: EvaluationContext


}

class M2mTransformExecution(
    val targetTransform: M2mTransformRuleSet,
    val targetDomainRef: DomainReference,
    val domainAccessorMutator: Map<DomainReference, ObjectGraphAccessorMutator>,
    val issues: IssueHolder
) {

    val executionContext = M2mTransformExecutionContext()

    val records: Map<M2mTransformRule, MappingRecord> = mutableMapOf()
    val targetAccessorMutator = domainAccessorMutator[targetDomainRef] ?: error("ObjectGraph not found for domain '$targetDomainRef'")

    fun addRecord(rule: M2mTransformRule, alternatives: List<Map<DomainReference, TypedObject>>) {
        val rec = records[rule]
        if (null == rec) {
            (records as MutableMap)[rule] = MappingRecord(rule, alternatives)
        } else {
            (records as MutableMap)[rule] = rec.merge(MappingRecord(rule, alternatives))
        }
    }

    fun addRecord(rule: M2mTransformRule, mapping: Map<DomainReference, TypedObject>) {
        val rec = records[rule]
        if (null == rec) {
            (records as MutableMap)[rule] = MappingRecord(rule, listOf(mapping))
        } else {
            (records as MutableMap)[rule] = rec.merge(MappingRecord(rule, listOf(mapping)))
        }
    }

    fun errorIssue(msg: String) {
        issues.error(null, msg)
    }

    fun warnIssue(msg: String) {
        issues.warn(null, msg)
    }

    fun infoIssue(msg: String) {
        issues.info(null, msg)
    }

    /**
     * for recording main execution steps
     */
    fun evaluationStep(msg: String) {}

    /**
     * for recording minor execution steps
     */
    fun evaluationTrace(msg: String) {}
}

class M2mTransformInterpreter(
    val m2m: M2mTransformDomain,
    val domainAccessorMutatorByDomainName: Map<SimpleName, ObjectGraphAccessorMutator>,
    val issues: IssueHolder = IssueHolder(LanguageProcessorPhase.INTERPRET)
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

        fun <OT : Any> List<TemplateMatchAlternatives>.cartesianProduct(): TemplateMatchAlternatives {
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
    fun transform(targetTransform: M2mTransformRuleSet, targetDomainRef: DomainReference, domainGraphs: Map<DomainReference, List<TypedObject>>): M2MTransformResult {
        val domainAccessorMutator = targetTransform.domainParameters.entries.associate { (k, v) ->
            Pair(k, domainAccessorMutatorByDomainName[v] ?: error("Domain ObjectGraph not found for domain $k"))
        }
        val m2mExecution = M2mTransformExecution(targetTransform, targetDomainRef, domainAccessorMutator, issues)
        m2mExecution.evaluationStep("Executing transform rule set '${targetTransform.qualifiedName}' with target domain '${targetDomainRef.value}'.")
        when {
            targetTransform.topRule.isEmpty() -> m2mExecution.errorIssue("No conforming top rule found for target domain '${targetDomainRef.value}'")
            else -> targetTransform.topRule.forEach { topRule ->
                executeRule(m2mExecution, topRule, domainGraphs)
            }
        }
        return M2MTransformResult(issues, m2mExecution.records, targetDomainRef)
    }

    /**
     * executes a rule and all the when and whare clauses transitively there in.
     * mapping records from all rule executions are recorded in the m2mExecution.
     * returns the MappingRecord made specifically by executing this rule
     */
    private fun executeRule(
        m2mExecution: M2mTransformExecution,
        rule: M2mTransformRule,
        source: Map<DomainReference, List<TypedObject>>
    ) = when (rule) {
        is M2mTransformAbstractRule -> executeAbstract(m2mExecution, rule, source)
        is M2MTransformRelation -> executeRelation(m2mExecution, rule, source)
        is M2MTransformMapping -> executeMapping(m2mExecution, rule, source)
        is M2MTransformTable -> executeTable(m2mExecution, rule, source)
        else -> error("Unknown rule type ${rule::class}")
    }

    private fun executeAbstract(
        m2mExecution: M2mTransformExecution,
        rule: M2mTransformAbstractRule,
        source: Map<DomainReference, List<TypedObject>>,
    ) {
        val subrules = m2mExecution.targetTransform.rule.values.filter { r -> r !is M2mTransformAbstractRule && r.conformsTo(rule) }
        subrules.forEach { sr ->
            executeRule(m2mExecution, sr, source)
            val rec = m2mExecution.records[sr]
            rec?.bySource(source)?.let {
                m2mExecution.addRecord(rule, it)
            }
        }
    }

    private fun executeMapping(
        m2mExecution: M2mTransformExecution,
        rule: M2MTransformMapping,
        source: Map<DomainReference, List<TypedObject>>
    ) {
        m2mExecution.evaluationStep("Executing mapping rule '${rule.name.value}'.")
        // for each domain reference get the match alternatives for each source object
        // Map of DomainReference -> List< TemplateMatchAlternatives per object >
        val domToListOfAlts = matchSourceVariables(m2mExecution, rule, source)
        when {
            domToListOfAlts.isEmpty() -> m2mExecution.warnIssue("No matches found in source domains for rule '${rule.name}'.")
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
                            null -> m2mExecution.errorIssue("No expression found for target domain ref '$${m2mExecution.targetDomainRef.value}' of rule '${rule.name}'.")
                            else -> {
                                val allVars = alt.values.merge()

                                val whenResult = rule.when_?.let {
                                    executeWhen(m2mExecution, rule, it, EvaluationContext.of(allVars.matchedVariables))
                                } ?: Pair(true, EvaluationContext.of(allVars.matchedVariables))
                                if (whenResult.first) {
                                    val varsAfterWhen = whenResult.second// + allVars.matchedVariables
                                    // construct
                                    val tgtExpr = rule.expression[m2mExecution.targetDomainRef]
                                        ?: error("No expression found for target domain '${m2mExecution.targetDomainRef.value}'") // should never happen
                                    val lhsType = rule.domainSignature[m2mExecution.targetDomainRef]?.variable?.type
                                        ?: error("No domainSignature found for target domain '${m2mExecution.targetDomainRef.value}'") // should never happen
                                    val tgtObj = createFromExpression(m2mExecution, varsAfterWhen, lhsType, tgtExpr)
                                    val mapping = rule.domainSignature.entries.associate { (dr, ds) ->
                                        val v = when (dr) {
                                            m2mExecution.targetDomainRef -> tgtObj
                                            else -> varsAfterWhen.getOrInParent(ds.variable.name.value)
                                                ?: error("No variable found for '${ds.variable.name.value}' of domain '$dr'.") // should never be error!
                                        }
                                        Pair(dr, v)
                                    }
                                    m2mExecution.addRecord(rule, mapping)

                                    // where
                                    val varsAfterWhere = when {
                                        rule.where.isEmpty() -> varsAfterWhen
                                        else -> rule.where.fold(varsAfterWhen) { acc, it ->
                                            executeWhere(m2mExecution, rule, it, acc)
                                        }
                                    }

                                    // setProperties
                                    setPropertiesFromExpression(m2mExecution, tgtObj, varsAfterWhere, tgtExpr)

                                } else {
                                    m2mExecution.infoIssue("when clause evaluated to false for target domain ref '${m2mExecution.targetDomainRef.value}' of rule '${rule.name}'.")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun executeRelation(
        m2mExecution: M2mTransformExecution,
        rule: M2MTransformRelation,
        source: Map<DomainReference, List<TypedObject>>
    ) {
        m2mExecution.evaluationStep("Executing relation rule '${rule.name.value}'.")
        val domToListOfAlts = matchSourceVariables(m2mExecution, rule, source)
        when {
            domToListOfAlts.isEmpty() -> m2mExecution.warnIssue("No matches found in source domains for rule '${rule.name}'.")
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
                            executeWhen(m2mExecution, rule, it, EvaluationContext.of(allVars.matchedVariables))
                        } ?: Pair(true, EvaluationContext.of(allVars.matchedVariables))
                        if (whenResult.first) {
                            val varsAfterWhen = whenResult.second //+ allVars.matchedVariables
                            // construct
                            val template = rule.domainTemplate[m2mExecution.targetDomainRef]
                                ?: error("No domainTemplate found for domain '${m2mExecution.targetDomainRef.value}'") //should never happen
                            val lhsType = rule.domainSignature[m2mExecution.targetDomainRef]?.variable?.type
                                ?: error("No domainSignature found for domain '${m2mExecution.targetDomainRef.value}'") //should never happen

                            val srcs = alt.entries.associate { (srcDomainRef, v) ->
                                val srcObjPat = rule.domainTemplate[srcDomainRef] ?: error("No object pattern found for domain '$srcDomainRef'")
                                val srcId = srcObjPat.identifier?.value ?: error("No identifier found for matched object in domain '$srcDomainRef'")
                                val src = v.getValueNamed(srcId) ?: error("No matched object found for identifier '$srcId'")
                                Pair(srcDomainRef, src)
                            }

                            val matchedVars = M2mPatternExecution("Matched variables after when clause", emptyList(), varsAfterWhen.namedValues.map { it.key }) {
                                varsAfterWhen
                            }

                            val mappedVars = rule.domainTemplate.map { (k, v) -> v.identifier?.value ?: error("No identifier found for domain '$k'") }
                            val tgtVarName = rule.domainSignature[m2mExecution.targetDomainRef]!!.variable.name.value
                            val recordMapping = M2mPatternExecution("Record Mapping", mappedVars, emptyList()) { evc ->
                                val tgtObj = evc.getOrInParent(tgtVarName) ?: m2mExecution.targetAccessorMutator.nothing()
                                val mapping = srcs + Pair(m2mExecution.targetDomainRef, tgtObj)
                                m2mExecution.addRecord(rule, mapping)
                                evc
                            }
                            val whereExes = rule.where.map { rw ->
                                val whereRule = rw.resolved!! //FIXME: issue when not resolved
                                val whereOutputs = listOf(rw.domainArguments[m2mExecution.targetDomainRef]).map {
                                    when (it) {
                                        is RootExpression -> it.name
                                        else -> error("not handled") //FIXME:
                                    }
                                }
                                val whereInputs = rw.domainArguments.map { (k, v) ->
                                    when (v) {
                                        is RootExpression -> v.name
                                        else -> error("not handled") //FIXME:
                                    }
                                } - whereOutputs
                                M2mPatternExecution("where ${rw}", whereInputs, whereOutputs) { evc ->
                                    val mv = executeWhere(m2mExecution, rule, rw, evc)
                                    //evc.child(mv)
                                    mv
                                }
                            }
                            recordMapping.doMeBefore.addAll(whereExes) // called rules may refer to the mapping
                            val initExes = listOf(matchedVars, recordMapping) + whereExes
                            val executor = M2mPatternExecutor(m2mExecution.issues, m2mExecution.targetAccessorMutator, initExes) //TODO: construct and build this during semantic analysis
                            val tgtName = template.identifier?.value ?: RESULT
                            executor.build(tgtName, template, lhsType)
                            executor.execute(varsAfterWhen, tgtName)
                        } else {
                            m2mExecution.infoIssue("when clause evaluated to false for target domain ref '${m2mExecution.targetDomainRef.value}' of rule '${rule.name}'.")
                        }
                    }
                }
            }
        }
    }

    private fun executeTable(m2mExecution: M2mTransformExecution, rule: M2MTransformTable, source: Map<DomainReference, List<TypedObject>>) {
        m2mExecution.evaluationStep("Executing table rule '${rule.name.value}'.")
        source.cartesianProduct().mapNotNull { srcAlt ->
            val mapping = rule.values.firstNotNullOfOrNull { vs ->
                val srcValues = srcAlt.mapNotNull { (srcDomainRef, v) ->
                    val srcObjectGraph = m2mExecution.domainAccessorMutator[srcDomainRef] ?: error("ObjectGraph not found for domain '$srcDomainRef'")
                    val exprInterp = ExpressionsInterpreterOverTypedObject(srcObjectGraph, m2mExecution.issues)
                    val evc = EvaluationContext.of(emptyMap<String, TypedObject>()) //TODO: are there any variables !
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
                    val exprInterp = ExpressionsInterpreterOverTypedObject(m2mExecution.targetAccessorMutator, m2mExecution.issues)
                    val evc = EvaluationContext.of(emptyMap<String, TypedObject>()) //TODO: are there any variables !
                    val drExp = vs[m2mExecution.targetDomainRef] ?: error("")
                    val value = exprInterp.evaluateExpression(evc, drExp)
                    srcValues + Pair(m2mExecution.targetDomainRef, value)
                }
            }?.toMap()
            mapping?.let {
                m2mExecution.addRecord(rule, mapping)
            }
        }
    }

    private fun matchSourceVariables(
        m2mExecution: M2mTransformExecution, //FIXME: only needed for 'issues' - just pass issues!
        rule: M2mTransformPatternRule,
        source: Map<DomainReference, List<TypedObject>>,
    ): Map<DomainReference, TemplateMatchAlternatives> {
        val srcDomainRefs = rule.domainSignature.filterKeys { k -> k != m2mExecution.targetDomainRef }
        val results = srcDomainRefs.map { (srcDomainRef, srcDomainItem) ->
            val srcOg = m2mExecution.domainAccessorMutator[srcDomainRef] ?: error("ObjectGraph not found for domain '$srcDomainRef'")
            val srcObjPat = rule.domainTemplate[srcDomainRef] ?: error("No object pattern found for domain '$srcDomainRef'")
            // match variables from source domain
            val srcList = source[srcDomainRef] ?: error("No source object found for domain '$srcDomainRef'")
            val alts = srcList.map { src ->
                matchVariablesFromRhs(m2mExecution, EvaluationContext.of(emptyMap()), srcOg, src, srcObjPat)
            }.filter { it.isMatch }
            Pair(srcDomainRef, TemplateMatchAlternatives(alts.flatMap { it.alternatives }))
        }
        // results contains, for each source domain, a list of alternative MatchResults
        return results.toMap()
    }

    fun matchVariablesFromRhs(
        m2mExecution: M2mTransformExecution,
        evc: EvaluationContext,
        accessorMutator: ObjectGraphAccessorMutator,
        src: TypedObject,
        rhs: PropertyTemplateRhs
    ): TemplateMatchAlternatives =
        when (rhs) {
            is PropertyTemplateExpression -> {
                val mr = matchVariablesFromPropertyTemplateExpression(m2mExecution, evc, accessorMutator, src, rhs)
                // only keep the result if it is a match
                mr.takeIf { it.isMatch }?.let { TemplateMatchAlternatives(listOf(it)) } ?: TemplateMatchAlternatives(emptyList())
            }

            is ObjectTemplate -> matchVariablesFromObjectTemplate(m2mExecution, evc, rhs, accessorMutator, src)
            is CollectionTemplate -> matchVariablesFromCollectionTemplate(m2mExecution, evc, rhs, accessorMutator, src)
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
        m2mExecution: M2mTransformExecution,
        evc: EvaluationContext,
        accessorMutator: ObjectGraphAccessorMutator,
        lhs: TypedObject,
        rhs: PropertyTemplateExpression
    ): TemplateMatchResult {
        // either :
        // 1) rhs is a free-variable with no value set, in which case set it to the lhs
        // 2) rhs is an expression with a value that matches the lhs
        val expr = rhs.expression
        return when {
            expr is RootExpression && null == evc.getOrInParent(expr.name) -> {
                TemplateMatchResult(true, mapOf(expr.name to lhs))
            }

            else -> {
                val exprInterp = ExpressionsInterpreterOverTypedObject(accessorMutator, m2mExecution.issues)
                val value = exprInterp.evaluateExpression(evc, expr)
                val isMatch = accessorMutator.equalTo(lhs, value)
                TemplateMatchResult(isMatch, emptyMap())
            }
        }
    }

    fun matchVariablesFromObjectTemplate(
        m2mExecution: M2mTransformExecution,
        evc: EvaluationContext,
        objectTemplate: ObjectTemplate,
        accessorMutator: ObjectGraphAccessorMutator,
        src: TypedObject
    ): TemplateMatchAlternatives {
        return when {
            objectTemplate.propertyTemplate.isEmpty() -> TemplateMatchAlternatives(listOf(TemplateMatchResult.EMPTY()))
            else -> {
                val propTemplateMatches = objectTemplate.propertyTemplate.map { (k, v) ->
                    val rhsPat = v.rhs
                    val lhs = src.getProperty(k.value)
                    matchVariablesFromRhs(m2mExecution, evc, accessorMutator, lhs, rhsPat)
                } //TODO: determine no match before cartesianProduct
                val result = propTemplateMatches.map { it.alternatives }.cartesianProduct()
                val alts = result.map { l -> l.merge() }.filter { it.isMatch }
                TemplateMatchAlternatives(alts)
            }
        }
    }

    fun matchVariablesFromCollectionTemplate(
        m2mExecution: M2mTransformExecution,
        evc: EvaluationContext,
        collectionTemplate: CollectionTemplate,
        accessorMutator: ObjectGraphAccessorMutator,
        src: TypedObject
    ): TemplateMatchAlternatives {
        val result = when {
            src.type.isCollection -> {
                // TODO: maybe a faster way to do it if NOT isSubset!
                val elements = mutableListOf<TypedObject>()
                accessorMutator.forEachIndexed(src) { idx, el -> elements.add(el) } //TODO: find a way not to 'collect' the list
                if (collectionTemplate.isSubset.not() && elements.size != collectionTemplate.elements.size) {
                    // TODO: should return a no match not error!
                    error("Collection size does not match template size: ${elements.size} != ${collectionTemplate.elements.size} ")
                }
                matchVariablesFromCollectionTemplateToElementsIn(m2mExecution, evc, collectionTemplate, accessorMutator, elements)
            }

            else -> error("src is not a collection")
        }
        return TemplateMatchAlternatives(result)
    }

    // get the different alternative combinations of objects from elements that (cover) match the defined templates
    // Set(  List(element-match per template)  )
    fun matchVariablesFromCollectionTemplateToElementsIn(
        m2mExecution: M2mTransformExecution,
        evc: EvaluationContext,
        collectionTemplate: CollectionTemplate,
        accessorMutator: ObjectGraphAccessorMutator,
        elements: List<TypedObject>
    ): List<TemplateMatchResult> {
        val options = findCoveringSubsets2(
            cover = collectionTemplate.elements,
            bySubsetsOf = elements
        ) { tp, el ->
            val mr = matchVariablesFromRhs(m2mExecution, evc, accessorMutator, el, tp)
            Pair(mr.alternatives.isNotEmpty(), mr)
        }
        val res = options.map { opt ->
            opt.flatMap { it.alternatives }.merge()
        }
        return res
    }

    fun executeWhen(
        m2mExecution: M2mTransformExecution,
        owningRule: M2mTransformRule,
        when_: Expression,
        evc: EvaluationContext
        //matchedVariables: Map<String, TypedObject>,
    ): Pair<Boolean, EvaluationContext> {
        // TODO: use an extended Expression evaluator that handles the RuleWhen options, so we can have compound when-expressions
        return when (when_) {
            is RuleWhenRelationHolds, is RuleWhenMappingHolds -> when_.resolved?.let { rule ->
                val source = getSource(m2mExecution, rule, when_.domainArguments, evc)
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
                    val targetArg = when_.domainArguments[m2mExecution.targetDomainRef]
                    val tgtValue = found[m2mExecution.targetDomainRef]
                    val vars: EvaluationContext = when (targetArg) {
                        null -> TODO()
                        is RootExpression -> when (tgtValue) {
                            null -> TODO()
                            else -> evc.child(mapOf(targetArg.name to tgtValue))
                        }

                        else -> {
                            m2mExecution.warnIssue(
                                "Argument for target domain '${m2mExecution.targetDomainRef.value}' must be a variable, in rule call '${when_.ruleName.value}' in 'when' clause of rule '${m2mExecution.targetTransform.name.value}.${owningRule.name.value}'."
                            )
                            evc
                        }
                    }
                    Pair(true, vars)
                } ?: Pair(false, evc)
            } ?: run {
                m2mExecution.errorIssue("In 'when' clause of rule '${owningRule.name.value}', rule '${when_.ruleName.value}' is unresolved in '${m2mExecution.targetTransform.name.value}'.")
                Pair(false, evc)
            }

            is RuleWhenRelationHoldsForAll -> TODO()
            is RuleWhenMappingHoldsForAll -> TODO()
            else -> {
                val srcOg = m2mExecution.domainAccessorMutator.entries.filterNot { it.key == m2mExecution.targetDomainRef }.first().value //should never be null!
                val exprInterp = ExpressionsInterpreterOverTypedObject(srcOg, m2mExecution.issues) //FIXME: which og to use? all might be needed!
                //val evc = EvaluationContext.of(matchedVariables)
                val res = exprInterp.evaluateExpression(evc, when_)
                val v: Boolean = when {
                    res.type.conformsTo(StdLibDefault.Boolean) -> {
                        srcOg.valueOf(res) as Boolean
                    }

                    else -> TODO()
                }
                Pair(v, evc)
            }
        }
    }

    /*
     * returns matchedVariables + variables set by executing the where
     */
    fun executeWhere(
        m2mExecution: M2mTransformExecution,
        owningRule: M2mTransformRule,
        where: RuleWhere,
        evc: EvaluationContext
//        matchedVariables: Map<String, TypedObject>,
    ): EvaluationContext {
        return when (where) { //TODO: support more complex expressions - override the expression interpreter to intercept function calls as rule-calls
            is RuleWhereCallRelation, is RuleWhereCallMapping -> where.resolved?.let { rule ->
                val source = getSource(m2mExecution, rule, where.domainArguments, evc)
                val tgtValue = executeRuleWhere(m2mExecution, owningRule, rule, source)
                val targetArg = where.domainArguments[m2mExecution.targetDomainRef]
                when (targetArg) {
                    null -> {
                        m2mExecution.errorIssue(
                            "Argument for target domain '${m2mExecution.targetDomainRef.value}' not found in rule call '${where.ruleName.value}' in 'where' clause of TransformRuleSet '${m2mExecution.targetTransform.name.value}'."
                        )
                        evc
                    }

                    is RootExpression -> when (tgtValue) {
                        null -> evc.setNamedValue(targetArg.name, m2mExecution.targetAccessorMutator.nothing())
                        else -> evc.setNamedValue(targetArg.name, tgtValue)
                    }

                    else -> {
                        m2mExecution.warnIssue(
                            "Argument for target domain '${m2mExecution.targetDomainRef.value}' must be a variable, in rule call '${where.ruleName.value}' in 'where' clause of rule '${m2mExecution.targetTransform.name.value}.${owningRule.name.value}'."
                        )
                        evc
                    }
                }
            } ?: run {
                m2mExecution.errorIssue("In 'where' clause of rule '${owningRule.name.value}', rule '${where.ruleName.value}' is unresolved in '${m2mExecution.targetTransform.name.value}'.")
                evc
            }

            is RuleWhereCallRelationForAll, is RuleWhereCallMappingForAll -> where.resolved?.let { rule ->
                val source = getSource(m2mExecution, rule, where.domainArguments, evc)
                val tgtValue = when {
                    source.all { (k, v) -> v.all { it.type.isCollection } } -> {
                        val sourceElements = mutableListOf<Map<DomainReference, List<TypedObject>>>()
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
                        m2mExecution.targetAccessorMutator!!.createCollection(sourceCollType.first(), tgtList)
                    }

                    else -> {
                        m2mExecution.errorIssue(
                            "In 'where' clause of rule '${owningRule.name.value}' in '${m2mExecution.targetTransform.name.value}', the all call to rule '${where.ruleName.value}' is expecting a collection."
                        )
                        null
                    }
                }
                val targetArg = where.domainArguments[m2mExecution.targetDomainRef]
                when (targetArg) {
                    null -> {
                        m2mExecution.errorIssue(
                            "Argument for target domain '${m2mExecution.targetDomainRef.value}' not found in rule call '${where.ruleName.value}' in 'where' clause of TransformRuleSet '${m2mExecution.targetTransform.name.value}'."
                        )
                        evc
                    }

                    is RootExpression -> when (tgtValue) {
                        null -> evc.setNamedValue(targetArg.name, m2mExecution.targetAccessorMutator.nothing())
                        else -> evc.setNamedValue(targetArg.name, tgtValue)
                    }

                    else -> {
                        m2mExecution.warnIssue(
                            "Argument for target domain '${m2mExecution.targetDomainRef.value}' must be a variable, in rule call '${where.ruleName.value}' in 'where' clause of rule '${m2mExecution.targetTransform.name.value}.${owningRule.name.value}'."
                        )
                        evc
                    }
                }
            } ?: run {
                m2mExecution.errorIssue("In 'where' clause of rule '${owningRule.name.value}', rule '${where.ruleName.value}' is unresolved in '${m2mExecution.targetTransform.name.value}'.")
                evc
            }

            else -> {
                m2mExecution.errorIssue("Cannot execute the 'where' clause.")
                evc
            }
        }
    }

    /*
     * returns null if the where-rule matches nothing
     */
    fun executeRuleWhere(
        m2mExecution: M2mTransformExecution,
        owningRule: M2mTransformRule,
        rule: M2mTransformRule,
        source: Map<DomainReference, List<TypedObject>>
    ): TypedObject? {
        var mappingRecord = m2mExecution.records[rule]
        if (null == mappingRecord) {
            executeRule(m2mExecution, rule, source)
            mappingRecord = m2mExecution.records[rule]
        }
        val alts = mappingRecord?.let {
            it.alternatives.filter { rec ->
                source.all { (k, v) ->
                    v.contains(rec[k]!!)
                }
            }
        } ?: emptyList()
        return when (alts.size) {
            0 -> {
                m2mExecution.warnIssue("In rule '${owningRule.name.value}' the 'where' clause matched nothing.")
                null
            }

            1 -> alts.first()[m2mExecution.targetDomainRef]

            else -> TODO("handle multiple matches from where")
        }
    }

    fun getSource(
        m2mExecution: M2mTransformExecution,
        rule: M2mTransformRule,
        arguments: Map<DomainReference, Expression>,
        evc: EvaluationContext
//        matchedVariables: Map<String, TypedObject>,
    ): Map<DomainReference, List<TypedObject>> {
        return (arguments - m2mExecution.targetDomainRef).mapValues { (k, v) ->
            val og = m2mExecution.domainAccessorMutator[k] ?: error("Cannot find ObjectGraph for domain reference '${k.value}'.")
            val exprInterp = ExpressionsInterpreterOverTypedObject(og, m2mExecution.issues)
            //val evc = EvaluationContext.of(matchedVariables)
            val res = exprInterp.evaluateExpression(evc, v)
            listOf(res)
        }
    }

    //TODO: merge code with above efficiently
    fun evaluateArgs(
        m2mExecution: M2mTransformExecution,
        rule: M2mTransformRule,
        arguments: List<Expression>,
        matchedVariables: Map<String, TypedObject>,
    ): Map<DomainReference, List<TypedObject>> {
        val argsValues = arguments.mapIndexed { idx, argExpr ->
            val domainRef = rule.domainSignature.keys.elementAt(idx)
            Pair(domainRef, argExpr)
        }.associate { it }
        return argsValues.mapValues { (k, v) ->
            val og = m2mExecution.domainAccessorMutator[k] ?: error("Cannot find ObjectGraph for domain reference '${k.value}'.")
            val exprInterp = ExpressionsInterpreterOverTypedObject(og, m2mExecution.issues)
            val evc = EvaluationContext.of(matchedVariables)
            val res = exprInterp.evaluateExpression(evc, v)
            listOf(res)
        }
    }

    fun createFromRhs(
        m2mExecution: M2mTransformExecution,
        evc: EvaluationContext,
        lhsType: TypeInstance,
        rhs: PropertyTemplateRhs
    ): Pair<TypedObject, Map<String, TypedObject>> = when (rhs) {
        is PropertyTemplateExpression -> createFromPropertyTemplateExpression(m2mExecution, evc, lhsType, rhs)
        is ObjectTemplate -> createFromObjectTemplate(m2mExecution, evc, lhsType, rhs)
        is CollectionTemplate -> createFromCollectionTemplate(m2mExecution, evc, lhsType, rhs)
        else -> error("Unknown rhs type ${rhs::class}")
    }

    fun createFromPropertyTemplateExpression(
        m2mExecution: M2mTransformExecution,
        evc: EvaluationContext,
        lhsType: TypeInstance,
        rhs: PropertyTemplateExpression
    ): Pair<TypedObject, Map<String, TypedObject>> {
        val o = createFromExpression(m2mExecution, evc, lhsType, rhs.expression)
        val mv = rhs.identifier?.let { mapOf(it.value to o) } ?: emptyMap()
        return Pair(o, mv)
    }

    /**
     * returns value of expression evaluated in context of provided variables
     */
    fun createFromExpression(
        m2mExecution: M2mTransformExecution,
        evc: EvaluationContext,
        lhsType: TypeInstance,
        expression: Expression
    ): TypedObject {
        val exprInterp = ExpressionsInterpreterOverTypedObject(m2mExecution.targetAccessorMutator, m2mExecution.issues)
        return when (expression) {
            is CreateObjectExpression -> {
                //val evc = EvaluationContext.of(variables)
                exprInterp.constructObject(evc, expression)
            }

            else -> {
                //val evc = EvaluationContext.of(variables)
                val value = exprInterp.evaluateExpression(evc, expression)
                value
            }
        }
    }

    fun createFromObjectTemplate(
        m2mExecution: M2mTransformExecution,
        evc: EvaluationContext,
        lhsType: TypeInstance,
        objectTemplate: ObjectTemplate
    ): Pair<TypedObject, Map<String, TypedObject>> {
        val decl = objectTemplate.type.resolvedDeclaration
        return when (decl) {
            is DataType, is ValueType -> {
                val matchedVars = mutableMapOf<String, TypedObject>()
                val constructors = when (decl) {
                    is DataType -> decl.constructors
                    is ValueType -> decl.constructors
                    else -> error("Type '${decl.qualifiedName.value}' has no constructors")
                }
                val possibleConArgNames = constructors.flatMap { it -> it.parameters.map { it.name.value } } //FIXME: this is not really accurate!
                val conArgs = mutableMapOf<String, TypedObject>()
                objectTemplate.propertyTemplate.forEach { (k, v) ->
                    if (possibleConArgNames.contains(k.value)) {
                        val propType = lhsType.allResolvedProperty[PropertyName(k.value)]?.typeInstance ?: StdLibDefault.AnyType
                        val (value, mv) = createFromRhs(m2mExecution, evc, propType, v.rhs)
                        matchedVars.putAll(mv)
                        conArgs[k.value] = value
                    }
                }
                val o = m2mExecution.targetAccessorMutator.createStructureValue(objectTemplate.type.qualifiedTypeName, conArgs)
                val mv = objectTemplate.identifier?.let { matchedVars + Pair(it.value, o) } ?: matchedVars
                Pair(o, mv)
            }

            else -> error("Cannot construct object of type ${decl.qualifiedName.value}")
        }
    }

    fun createFromCollectionTemplate(
        m2mExecution: M2mTransformExecution,
        evc: EvaluationContext,
        lhsType: TypeInstance,
        collectionTemplate: CollectionTemplate
    ): Pair<TypedObject, Map<String, TypedObject>> {
        //collection may already have been created, (via when/where/etc) and be a captured variable
        val existing = collectionTemplate.identifier?.let { evc.getOrInParent(it.value) }
        return when {
            null == existing -> {
                // create new collection from template elements
                val matchedVars = mutableMapOf<String, TypedObject>()
                val elements = collectionTemplate.elements.map {
                    val (o, mv) = createFromRhs(m2mExecution, evc, lhsType.typeArguments[0].type, it)
                    matchedVars.putAll(mv)
                    o
                }
                val col = m2mExecution.targetAccessorMutator.createCollection(lhsType, elements)
                val mv = collectionTemplate.identifier?.let { matchedVars + Pair(it.value, col) } ?: matchedVars
                Pair(col, mv)
            }

            else -> {
                // try to match template elements against existing collection elements, if not matched then create them.
                val elements = mutableListOf<TypedObject>()
                m2mExecution.targetAccessorMutator.forEachIndexed(existing) { idx, el -> elements.add(el) } //TODO: find a way not to 'collect' the list
                val matches = matchVariablesFromCollectionTemplateToElementsIn(m2mExecution, evc, collectionTemplate, m2mExecution.targetAccessorMutator, elements)

                when {
                    matches.isEmpty() -> {
                        // create new collection from template elements
                        val matchedVars = mutableMapOf<String, TypedObject>()
                        val elements = collectionTemplate.elements.map {
                            val (o, mv) = createFromRhs(m2mExecution, evc, lhsType.typeArguments[0].type, it)
                            matchedVars.putAll(mv)
                            o
                        }
                        val col = m2mExecution.targetAccessorMutator.createCollection(lhsType, elements)
                        val mv = collectionTemplate.identifier?.let { matchedVars + Pair(it.value, col) } ?: matchedVars
                        Pair(col, mv)
                    }

                    1 == matches.size -> {
                        val matched = matches.first()
                        matched.matchedVariables
                        TODO()
                    }

                    else -> TODO("not sue what to do here yet")
                }
            }
        }
    }

    fun setPropertiesFromRhs(
        m2mExecution: M2mTransformExecution,
        obj: TypedObject,
        evc: EvaluationContext,
        rhs: PropertyTemplateRhs
    ) = when (rhs) {
        is PropertyTemplateExpression -> setPropertiesFromExpression(m2mExecution, obj, evc, rhs.expression)
        is ObjectTemplate -> setPropertiesFromObjectTemplate(m2mExecution, obj, evc, rhs)
        is CollectionTemplate -> setPropertiesFromCollectionTemplate(m2mExecution, obj, evc, rhs)
        else -> error("Unknown rhs type ${rhs::class}")
    }

    fun setPropertiesFromExpression(
        m2mExecution: M2mTransformExecution,
        obj: TypedObject,
        evc: EvaluationContext,
        expression: Expression
    ) {
        val exprInterp = ExpressionsInterpreterOverTypedObject(m2mExecution.targetAccessorMutator, m2mExecution.issues)
        when (expression) {
            is CreateObjectExpression -> {
                exprInterp.propertyAssignmentBlock(evc, obj, expression.propertyAssignments)
            }

            else -> {
                // nothing to do, already done when createFromPropertyPatternExpression was called
            }
        }
    }

    fun setPropertiesFromObjectTemplate(
        m2mExecution: M2mTransformExecution,
        obj: TypedObject,
        evc: EvaluationContext,
        objectTemplate: ObjectTemplate
    ) {
        val decl = objectTemplate.type.resolvedDeclaration
        return when (decl) {
            // only DataTypes have properties that can be set
            is DataType -> {
                val propValues = mutableMapOf<String, TypedObject>()
                objectTemplate.propertyTemplate.forEach { (k, v) ->
                    val propType = obj.type.allResolvedProperty[PropertyName(k.value)]?.typeInstance ?: StdLibDefault.AnyType

                    // TODO: if property is a constructor arg, it is already set, else it will have no value yet
                    // can we deduce this rather than getting all properties and just checking for nothing

                    val possiblePv = obj.getProperty(k.value)
                    val pv = when {
                        m2mExecution.targetAccessorMutator.isNothing(possiblePv) -> {
                            val (o, vars) = createFromRhs(m2mExecution, evc, propType, v.rhs)
                            setPropertiesFromRhs(m2mExecution, o, evc, v.rhs)
                            o
                        }

                        else -> {
                            setPropertiesFromRhs(m2mExecution, possiblePv, evc, v.rhs)
                            possiblePv
                        }
                    }
                    propValues[k.value] = pv
                }
            }

            else -> Unit
        }
    }

    fun setPropertiesFromCollectionTemplate(
        m2mExecution: M2mTransformExecution,
        obj: TypedObject,
        evc: EvaluationContext,
        collectionTemplate: CollectionTemplate
    ) {
        val elements = mutableListOf<TypedObject>()
        m2mExecution.targetAccessorMutator.forEachIndexed(obj) { i, el ->
            elements.add(el)
        }
        collectionTemplate.elements.map { elTemplate ->
            when {
                // if elTemplate is named, then it should be a variable we can et it
                null != elTemplate.identifier -> {
                    val el = evc.getOrInParent(elTemplate.identifier!!.value)
                    el?.let {
                        check(elements.contains(el)) //maybe not needed
                        setPropertiesFromRhs(m2mExecution, el, evc, elTemplate)
                    }
                }

                else -> TODO("How to identify a collection template element?")
            }
        }
    }

}