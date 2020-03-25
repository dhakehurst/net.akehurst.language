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

package net.akehurst.language.editor.monaco

import monaco.languages
import net.akehurst.language.editor.common.AglComponents
import net.akehurst.language.editor.common.AglLineState
import net.akehurst.language.editor.common.AglToken
import net.akehurst.language.editor.common.AglTokenizer
import net.akehurst.language.editor.comon.AglWorkerClient

class AglTokenizerByWorkerMonaco(
        val agl:AglComponents
) : languages.TokensProvider {

    val aglTokenizer = AglTokenizer(agl)
    var acceptingTokens = false
    val tokensByLine = mutableMapOf<Int, List<AglToken>>()

    fun reset() {
        this.acceptingTokens = false
        this.tokensByLine.clear()
    }

    fun receiveTokens(tokens: Array<Array<AglToken>>) {
        if (this.acceptingTokens) {
            tokens.forEachIndexed { index, tokens ->
                this.tokensByLine[index] = tokens.toList()
            }
        }
    }

    // --- monaco.langugaes.Tokenizer ---

    override fun getInitialState(): languages.IState {
        return AglLineStateMonaco(0, "")
    }

    override fun tokenize(line: String, pState: languages.IState): languages.ILineTokens {
        val mcState = pState as AglLineStateMonaco
        val row = mcState.lineNumber
        val tokens = this.tokensByLine[row]
        return if (null == tokens) {
            // no tokens received from worker, try local scan
            val stateAgl = AglLineState(mcState.lineNumber, mcState.leftOverText, emptyList()) //not really emptyList, but its not needed as input so ok to use
            val ltokens = this.aglTokenizer.getLineTokensByScan(line, stateAgl, row)
            val lineTokens: List<AglTokenMonaco> = ltokens.tokens.map {
                AglTokenMonaco(
                        it.styles.first(),
                        it.column
                )
            }
            val lt: Array<languages.IToken> = lineTokens.toTypedArray()
            AglLineTokensMonaco(
                    AglLineStateMonaco(row, ""),
                    lt
            )
        } else {
            val lineTokens: List<AglTokenMonaco> = tokens.map {
                AglTokenMonaco(
                        it.styles.first(),
                        it.column
                )
            }
            val lt: Array<languages.IToken> = lineTokens.toTypedArray()
            AglLineTokensMonaco(
                    AglLineStateMonaco(row, ""),
                    lt
            )
        }
    }


}