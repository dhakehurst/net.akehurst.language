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

import net.akehurst.language.expressions.asm.ExpressionExt.freeVariables
import net.akehurst.language.m2mTransform.api.RuleCall
import net.akehurst.language.m2mTransform.api.RuleWhere
import net.akehurst.language.m2mTransform.processor.TemplateMatchResult.Companion.merge

/**
 * helper functions of the m2m interpreter, moved out of the (now commented out) M2mTransformInterpreter companion object
 */
object M2mTransformInterpreterHelpers {
    const val RESULT = "§result"

    /** one way the members of a collection template match the elements of a collection: a result per member, the element indices in each gap (null if no gap) */
    data class CollectionMatch<R>(val memberResults: List<R>, val gapElements: List<List<Int>?>)

    /**
     * all ways that 'memberCount' member templates match 'elementCount' elements, each member a different element.
     * matches[i][k] are the results of member i matching element k (empty if it does not match).
     * ordered: members match in order, and elements between them are only allowed where gapPresent is true (gapPresent[i] is the gap before member i, gapPresent[memberCount] after the last).
     * unordered: elements not matched by a member are allowed only if there is a gap, they are put in the gap at leftoverGapIndex.
     */
    fun <R> collectionTemplateMatches(
        memberCount: Int,
        elementCount: Int,
        ordered: Boolean,
        gapPresent: List<Boolean>,
        leftoverGapIndex: Int?,
        matches: List<List<List<R>>>
    ): List<CollectionMatch<R>> {
        val result = mutableListOf<CollectionMatch<R>>()
        val chosen = ArrayList<R>(memberCount)
        if (ordered) {
            val gapEls = MutableList<List<Int>?>(memberCount + 1) { null }
            fun next(i: Int, j: Int) {
                if (i == memberCount) {
                    when {
                        gapPresent[i] -> {
                            gapEls[i] = (j until elementCount).toList()
                            result.add(CollectionMatch(chosen.toList(), gapEls.toList()))
                        }

                        j == elementCount -> result.add(CollectionMatch(chosen.toList(), gapEls.toList()))
                    }
                } else {
                    val last = if (gapPresent[i]) elementCount - 1 else minOf(j, elementCount - 1)
                    for (k in j..last) {
                        gapEls[i] = if (gapPresent[i]) (j until k).toList() else null
                        for (r in matches[i][k]) {
                            chosen.add(r)
                            next(i + 1, k + 1)
                            chosen.removeAt(chosen.size - 1)
                        }
                    }
                    gapEls[i] = null
                }
            }
            next(0, 0)
        } else {
            val used = BooleanArray(elementCount)
            fun next(i: Int) {
                if (i == memberCount) {
                    val leftover = (0 until elementCount).filter { used[it].not() }
                    when {
                        leftover.isEmpty() -> result.add(CollectionMatch(chosen.toList(), gapPresent.map { if (it) emptyList<Int>() else null }))
                        null != leftoverGapIndex -> result.add(CollectionMatch(chosen.toList(), gapPresent.mapIndexed { gi, p ->
                            when {
                                gi == leftoverGapIndex -> leftover
                                p -> emptyList()
                                else -> null
                            }
                        }))
                        // elements not matched and no gap: no match
                    }
                } else {
                    for (k in 0 until elementCount) {
                        if (used[k]) continue
                        used[k] = true
                        for (r in matches[i][k]) {
                            chosen.add(r)
                            next(i + 1)
                            chosen.removeAt(chosen.size - 1)
                        }
                        used[k] = false
                    }
                }
            }
            next(0)
        }
        return result
    }

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
        val alts = cp.map { it.merge() }.filter { it.isMatch }
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

    val RuleWhere.domainArgumentNames: List<String> get() = this.domainArguments.flatMap { (_, v) -> v.freeVariables } //TODO: does not handle RuleWhen freeVars !
    val RuleCall.ruleArgumentNames: List<String> get() = this.ruleArguments.flatMap { (_, v) -> v.freeVariables } //TODO: does not handle RuleWhen freeVars !
}
