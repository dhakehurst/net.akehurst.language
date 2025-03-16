/**
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.parser.leftcorner

import net.akehurst.language.agl.runtime.graph.GrowingNodeIndex
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsEmbedded
import net.akehurst.language.agl.runtime.structure.RuntimeSpineDefault
import net.akehurst.language.automaton.leftcorner.LookaheadSet
import net.akehurst.language.automaton.leftcorner.Transition

internal sealed class FailedParseReason(
    val fromSkipParser: Boolean,
    val failedAtPosition: Int,
    val head: GrowingNodeIndex,
    val transition: Transition,
    val gssSnapshot: Map<GrowingNodeIndex, Set<GrowingNodeIndex>>
) {
    val position get() = head.nextInputPositionAfterSkip
    val attemptedAction get() = transition.action
    open val skipFailure get() = fromSkipParser
    abstract val spine: RuntimeSpineDefault
}

// Lookahead failure, i.e. lookahead tokens not matched
internal class FailedParseReasonLookahead(
    fromSkipParser: Boolean,
    failedAtPosition: Int,
    head: GrowingNodeIndex,
    transition: Transition,
    gssSnapshot: Map<GrowingNodeIndex, Set<GrowingNodeIndex>>,
    val runtimeLhs: Set<LookaheadSet>,
    val possibleEndOfText: Set<LookaheadSet>
) : FailedParseReason(fromSkipParser, failedAtPosition, head, transition, gssSnapshot) {

    override val spine: RuntimeSpineDefault by lazy {
        val expected: Set<RuntimeRule> = possibleEndOfText.flatMap { eot ->
            runtimeLhs.flatMap { rt ->
                transition.lookahead.flatMap { lh ->
                    lh.guard.resolve(eot, rt).fullContent
                }
            }
        }.toSet()
        val terms = head.state.stateSet.usedTerminalRules
        val embeddedSkipTerms = head.state.stateSet.embeddedRuntimeRuleSet.flatMap { it.skipTerminals }.toSet()
        val exp = expected.minus(embeddedSkipTerms.minus(terms))

        RuntimeSpineDefault(head, gssSnapshot, exp, head.numNonSkipChildren + 1)
    }

    override val skipFailure get() = fromSkipParser
}

// transition.to token not found
internal class FailedParseReasonWidthTo(
    fromSkipParser: Boolean,
    failedAtPosition: Int,
    head: GrowingNodeIndex,
    transition: Transition,
    gssSnapshot: Map<GrowingNodeIndex, Set<GrowingNodeIndex>>
) : FailedParseReason(fromSkipParser, failedAtPosition, head, transition, gssSnapshot) {

    override val spine = RuntimeSpineDefault(head, gssSnapshot, setOf(transition.to.firstRule), head.numNonSkipChildren)

}

internal class FailedParseExpectedSkipAfter(
    fromSkipParser: Boolean,
    failedAtPosition: Int,
    head: GrowingNodeIndex,
    transition: Transition,
    gssSnapshot: Map<GrowingNodeIndex, Set<GrowingNodeIndex>>,
    firstTerminals: Set<RuntimeRule>
) : FailedParseReason(fromSkipParser, failedAtPosition, head, transition, gssSnapshot) {

    override val spine = RuntimeSpineDefault(head, gssSnapshot, firstTerminals, head.numNonSkipChildren)

}

// transition.runtimeGuard fails
internal class FailedParseReasonGraftRTG(
    fromSkipParser: Boolean,
    failedAtPosition: Int,
    head: GrowingNodeIndex,
    transition: Transition,
    gssSnapshot: Map<GrowingNodeIndex, Set<GrowingNodeIndex>>,
    val prevNumNonSkipChildren: Int
) : FailedParseReason(fromSkipParser, failedAtPosition, head, transition, gssSnapshot) {

    override val spine by lazy {
        val exp = transition.runtimeGuard.expectedWhenFailed(prevNumNonSkipChildren)
        RuntimeSpineDefault(head, gssSnapshot, exp, head.numNonSkipChildren)
    }

}

// embedded grammar failure
internal class FailedParseReasonEmbedded(
    fromSkipParser: Boolean,
    failedAtPosition: Int,
    head: GrowingNodeIndex,
    transition: Transition,
    gssSnapshot: Map<GrowingNodeIndex, Set<GrowingNodeIndex>>,
    val embededFailedParseReasons: List<FailedParseReason>
) : FailedParseReason(fromSkipParser, failedAtPosition, head, transition, gssSnapshot) {

    override val spine by lazy {
        // Outer skip terms are part of the 'possibleEndOfText' and thus could be in the expected terms
        // if these skip terms are not part of the embedded 'normal' terms...remove them
        val embeddedRhs = transition.to.runtimeRules.first().rhs as RuntimeRuleRhsEmbedded // should only ever be one
        val embeddedStateSet = embeddedRhs.embeddedRuntimeRuleSet.fetchStateSetFor(embeddedRhs.embeddedStartRule.tag, head.state.stateSet.automatonKind)
        //val x = findNextExpectedAfterError3(sentence, embededFailedParseReasons, head.state.stateSet.automatonKind, embeddedStateSet, failedAtPosition)
        val x = embededFailedParseReasons.map { it.spine }
        val embeddedRuntimeRuleSet = embeddedRhs.embeddedRuntimeRuleSet
        val embeddedTerms = embeddedRuntimeRuleSet.fetchStateSetFor(embeddedRhs.embeddedStartRule.tag, head.state.stateSet.automatonKind).usedTerminalRules
        val skipTerms = head.state.stateSet.runtimeRuleSet.skipParserStateSet?.usedTerminalRules ?: emptySet()
        val exp = x.flatMap { it.expectedNextTerminals }.minus(skipTerms.minus(embeddedTerms)).toSet()
        RuntimeSpineDefault(head, gssSnapshot, exp, head.numNonSkipChildren)
    }

}