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

import ace.LineState
import ace.Token
import net.akehurst.language.api.sppt.SPPTLeaf
import net.akehurst.language.editor.api.AglComponents
import kotlin.js.RegExp

class AglBackgroundTokenizer(
        tok: AglTokenizer,
        ed: ace.Editor
) : ace.BackgroundTokenizer(tok, ed) {
}

class AglLineState(
        val lineNumber: Int,
        val leftOverText: String
) : ace.LineState {
}

class AglToken(
        styles: Array<String>,
        override val value: String,
        override val line: Int,
        column: Int
) : ace.Token {
    override val type = styles.joinToString(".")
    override var start = column
}

class AglLineTokens(
        override val endState: LineState,
        override val tokens: Array<Token>
) : ace.LineTokens {}

class AglTokenizer(
        val agl: AglComponentsAce
) : ace.Tokenizer {

    // --- ace.Ace.Tokenizer ---
    override fun getLineTokens(line: String, pState: ace.LineState?, row: Int): ace.LineTokens {
        val sppt = this.agl.sppt
        val state = if (null == pState) AglLineState(0, "") else pState as AglLineState
        return if (null == sppt) {
            this.getLineTokensByScan(line, state)
        } else {
            this.getLineTokensByParse(line, state)
        }
    }

    private fun mapTokenTypeToClass(tokenType: String): String {
        var cssClass = this.agl.tokenToClassMap.get(tokenType);
        if (null == cssClass) {
            cssClass = "nostyle"
        }
        return cssClass;
    }

    private fun mapToCssClasses(leaf: SPPTLeaf): List<String> {
        val metaTagClasses = leaf.metaTags.map { this.mapTokenTypeToClass(it) }
        val otherClasses = if (!leaf.tagList.isEmpty()) {
            leaf.tagList.map { this.mapTokenTypeToClass(it) }
        } else {
            listOf(this.mapTokenTypeToClass(leaf.name))
        }
        val classes = metaTagClasses + otherClasses
        return classes;
    }

    private fun transformToAceTokens(leafs: List<SPPTLeaf>): List<AglToken> {
        return leafs.map { leaf ->
            val tokenType = leaf.name; //(it.isPattern) ? '"' + it.name + '"' : "'" + it.name + "'";
            val cssClasses = this.mapToCssClasses(leaf).toTypedArray()
            var beforeEOL = leaf.matchedText
            val eolIndex = leaf.matchedText.indexOf('\n');
            if (-1 !== eolIndex) {
                beforeEOL = leaf.matchedText.substring(0, eolIndex);
            }
            AglToken(
                    cssClasses,
                    beforeEOL,
                    leaf.location.column,
                    leaf.location.line
            )
        }
    }

    private fun getLineTokensByScan(line: String, state: AglLineState): ace.LineTokens {
        val proc = this.agl.processor
        val nextLineNumber = state.lineNumber + 1
        return if(null != proc) {
            val text = state.leftOverText + line
            val leafs = proc.scan(text);
            val tokens = transformToAceTokens(leafs)
            val endState = if (leafs.isEmpty()) {
                AglLineState(nextLineNumber, "")
            } else {
                val lastLeaf = leafs.last()
                val endOfLastLeaf = lastLeaf.location.column + lastLeaf.location.length
                val leftOverText = line.substring(endOfLastLeaf, line.length)
                AglLineState(nextLineNumber, leftOverText)
            }
            AglLineTokens(
                    endState,
                    tokens.toTypedArray()
            )
        } else {
            AglLineTokens(AglLineState(nextLineNumber, ""), emptyArray())
        }
    }

    private fun getLineTokensByParse(line: String, state: AglLineState): ace.LineTokens {
        val nextLineNumber = state.lineNumber + 1
        val sppt = this.agl.sppt!!
        val leafs = sppt.tokensByLine(state.lineNumber)
        return if (null != leafs) {
            val tokens = transformToAceTokens(leafs)
            val endState = AglLineState(nextLineNumber, "")
            return AglLineTokens(
                    endState,
                    tokens.toTypedArray()
            )
        } else {
            AglLineTokens(AglLineState(nextLineNumber, ""), emptyArray())
        }
    }
}