/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.runtime.structure

import net.akehurst.language.agl.runtime.graph.GrowingNodeIndex
import net.akehurst.language.api.parser.RuntimeSpine

internal class RuntimeSpineDefault(
    val head: GrowingNodeIndex,
    val gssSnapshot: Map<GrowingNodeIndex, Set<GrowingNodeIndex>>,
    override val expectedNextTerminals: Set<RuntimeRule>,
    val nextChildNumber: Int
) : RuntimeSpine {
    override val elements: List<RuntimeRule> by lazy {
        val list = mutableListOf(head.state.firstRule)
        var next = gssSnapshot[head]
        while (null != next && next.isNotEmpty()) {
            val nh = next.first()
            list.add(nh.state.firstRule)
            next = gssSnapshot[nh]
        }
        list
    }

    override fun toString(): String = "RTSpine ${head.state.firstRule.tag}[${head.numNonSkipChildren}]->${elements.joinToString(separator = "->") { it.tag }}"
}