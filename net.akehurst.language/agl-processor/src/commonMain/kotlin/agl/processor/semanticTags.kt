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

package net.akehurst.language.agl.processor

import net.akehurst.language.api.processor.SemanticTag
import net.akehurst.language.sppt.api.LeafData

/**
 * Adds the semantic tags (from semantic analysis) to the tag list of each token they cover,
 * so that style rules with semantic selectors ('type T', 'identify T', 'refers-to T') can match the token.
 * A semantic tag covers a token when the token lies within the tag's location.
 * Tags of larger (outer) locations come before tags of smaller (inner) ones; the existing tags of a token stay first.
 */
fun tokensWithSemanticTags(tokensByLine: List<List<LeafData>>, semanticTags: List<SemanticTag>): List<List<LeafData>> {
    if (semanticTags.isEmpty()) return tokensByLine
    val sorted = semanticTags.sortedWith(compareBy<SemanticTag> { it.location.position }.thenByDescending { it.location.length })
    var next = 0
    val active = mutableListOf<SemanticTag>()
    // tokens are visited in position order
    return tokensByLine.map { line ->
        line.map { leaf ->
            val start = leaf.position
            val end = leaf.position + leaf.length
            while (next < sorted.size && sorted[next].location.position <= start) {
                active.add(sorted[next])
                next++
            }
            active.removeAll { it.location.position + it.location.length <= start }
            val covering = active.filter { it.location.position <= start && end <= it.location.position + it.location.length }
            if (covering.isEmpty()) {
                leaf
            } else {
                val extra = covering.sortedByDescending { it.location.length }.flatMap { it.tags }
                leaf.copy(tagList = leaf.tagList + extra)
            }
        }
    }
}
