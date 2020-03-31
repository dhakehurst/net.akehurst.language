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

import monaco.IRange
import monaco.editor
import monaco.languages
import net.akehurst.language.editor.common.AglComponents
import net.akehurst.language.editor.common.AglLineState
import net.akehurst.language.editor.common.AglToken
import net.akehurst.language.editor.common.AglTokenizer
import net.akehurst.language.editor.comon.AglWorkerClient

class ModelDecorationOptions(
        override val afterContentClassName: String? = null,
        override val beforeContentClassName: String? = null,
        override val className: String? = null,
        override val glyphMarginClassName: String? = null,
        override val glyphMarginHoverMessage: dynamic = null,
        override val hoverMessage: dynamic = null,
        override val inlineClassName: String? = null,
        override val inlineClassNameAffectsLetterSpacing: Boolean? = null,
        override val isWholeLine: Boolean? = null,
        override val linesDecorationsClassName: String? = null,
        override val marginClassName: String? = null,
        override val minimap: dynamic = null,
        override val overviewRuler: dynamic = null,
        override val stickiness: dynamic = null,
        override val zindex: dynamic = null
) : editor.IModelDecorationOptions

class AglTokenizerByWorkerMonaco(
        val aglEditor: AglEditorMonaco,
        val agl: AglComponents
) : languages.TokensProvider {

    val aglTokenizer = AglTokenizer(agl)
    var acceptingTokens = false
    val tokensByLine = mutableMapOf<Int, List<AglToken>>()
    val decs = mutableMapOf<Int, Array<String>>()

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
        val row = mcState.lineNumber+1
        val tokens = this.tokensByLine[row-1]
        return if (null == tokens) {
            // no tokens received from worker, try local scan
            val stateAgl = AglLineState(mcState.lineNumber, mcState.leftOverText, emptyList()) //not really emptyList, but its not needed as input so ok to use
            val ltokens = this.aglTokenizer.getLineTokensByScan(line, stateAgl, row)
            this.decorateLine(row,ltokens.tokens)
            val lineTokens: List<AglTokenMonaco> = ltokens.tokens.map {
                AglTokenMonaco(
                        it.styles.firstOrNull() ?: "",
                        it.column
                )
            }
            val lt: Array<languages.IToken> = lineTokens.toTypedArray()
            AglLineTokensMonaco(
                    AglLineStateMonaco(row, ""),
                    lt
            )
        } else {
            this.decorateLine(row,tokens)
            val lineTokens: List<AglTokenMonaco> = tokens.map {
                AglTokenMonaco(
                        it.styles.firstOrNull() ?: "",
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

    fun decorateLine(lineNum:Int, tokens: List<AglToken>) {
        val decs: Array<editor.IModelDeltaDecoration> = tokens.map { aglTok ->
            object : editor.IModelDeltaDecoration {
                override val range = object : IRange {
                    override val startColumn = aglTok.column
                    override val endColumn = aglTok.column + aglTok.value.length
                    override val startLineNumber = aglTok.line
                    override val endLineNumber = aglTok.line
                }
                override val options = ModelDecorationOptions(
                        inlineClassName = aglTok.styles.joinToString(separator = " ") { "monaco_${it}" }
                )
            }
        }.toTypedArray()
        val curDes = this.decs[lineNum] ?: emptyArray()
        val d = aglEditor.monacoEditor.deltaDecorations(curDes, decs)
        this.decs[lineNum] = d
    }

}