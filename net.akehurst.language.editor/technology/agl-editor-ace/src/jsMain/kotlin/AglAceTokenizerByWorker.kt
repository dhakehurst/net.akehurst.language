/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.editor.ace

import net.akehurst.language.editor.common.AglToken
import net.akehurst.language.editor.comon.AglWorkerClient

class AglAceTokenizerByWorker(
) : ace.Tokenizer {

    val tokensByLine = mutableMapOf<Int, List<AglToken>>()

    // --- ace.Ace.Tokenizer ---
    override fun getLineTokens(line: String, pState: ace.LineState?, row: Int): ace.LineTokens {
        val tokens = this.tokensByLine[row]
        return if (null == tokens) {
            // no tokens received from worker
            AglLineTokensAce(AglLineStateAce(row, ""), arrayOf(AglTokenAce(emptyArray(), line, row, 0)))
        } else {
            val lineTokens:List<AglTokenAce> = tokens.map {
                AglTokenAce(
                        it.styles,
                        it.value,
                        it.line,
                        it.column
                )
            }
            val lt:Array<ace.Token> = lineTokens.toTypedArray()
            AglLineTokensAce(
                    AglLineStateAce(row, ""),
                    lt
            )
        }
    }


}