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

package net.akehurst.language.agl.parser

import net.akehurst.language.agl.automaton.LookaheadSet
import net.akehurst.language.agl.automaton.Transition
import net.akehurst.language.agl.runtime.graph.GrowingNodeIndex

internal sealed class FailedParseReason(
    val position: Int,
    val transition: Transition,
    val gssSnapshot: Map<GrowingNodeIndex, Set<GrowingNodeIndex>>?
)

internal class FailedParseReasonLookahead(
    position: Int,
    transition: Transition,
    gssSnapshot: Map<GrowingNodeIndex, Set<GrowingNodeIndex>>?,
    val runtimeLhs: Set<LookaheadSet>,
    val possibleEndOfText: Set<LookaheadSet>
) : FailedParseReason(position, transition, gssSnapshot)

internal class FailedParseReasonWidthTo(
    position: Int,
    transition: Transition,
    gssSnapshot: Map<GrowingNodeIndex, Set<GrowingNodeIndex>>?
) : FailedParseReason(position, transition, gssSnapshot)

internal class FailedParseReasonGraftRTG(
    position: Int,
    transition: Transition,
    gssSnapshot: Map<GrowingNodeIndex, Set<GrowingNodeIndex>>?,
    val prevNumNonSkipChildren: Int
) : FailedParseReason(position, transition, gssSnapshot)

internal class FailedParseReasonEmbedded(
    position: Int,
    transition: Transition,
    gssSnapshot: Map<GrowingNodeIndex, Set<GrowingNodeIndex>>?,
    val embededFailedParseReasons: Map<Int, MutableList<FailedParseReason>>
) : FailedParseReason(position, transition, gssSnapshot)