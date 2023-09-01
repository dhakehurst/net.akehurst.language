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

package net.akehurst.language.agl.agl.parser

import net.akehurst.language.agl.parser.InputFromString
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.sppt.Sentence
import net.akehurst.language.api.sppt.SpptDataNode

class SentenceDefault(
    override val text: String
) : Sentence {

    // --- Sentence ---
    override fun matchedTextNoSkip(node: SpptDataNode): String = text.substring(node.startPosition, node.nextInputNoSkip)

    override fun locationFor(position: Int, length: Int): InputLocation {
        return when {
            0 == position -> InputLocation(position, 1, 1, length)
            _eolPositions.isEmpty() -> InputLocation(position, position + 1, 1, length)
            else -> {
                val line = _eolPositions.filter { it <= position }.size
                val col = when {
                    0 == line -> position + 1
                    else -> position - _eolPositions[line - 1]
                }
                InputLocation(position, col, line + 1, length)
            }
        }
    }

    override fun locationFor(node: SpptDataNode): InputLocation = locationFor(node.startPosition, node.nextInputNoSkip - node.startPosition)

    // --- implementation ---
    private val _eolPositions: List<Int> by lazy { InputFromString.eolPositions(text) }

}