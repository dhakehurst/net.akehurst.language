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

import net.akehurst.language.api.sppt.SPPTLeaf
import net.akehurst.language.editor.common.AglComponents

class AglBackgroundTokenizer(
        tok: ace.Tokenizer,
        ed: ace.Editor
) : ace.BackgroundTokenizer(tok, ed) {
}

class AglLineStateAce(
        val lineNumber: Int,
        val leftOverText: String
) : ace.LineState {
}

class AglTokenAce(
        styles: Array<String>,
        override val value: String,
        override val line: Int,
        column: Int
) : ace.Token {
    override val type = styles.joinToString(".")
    override var start = column
}

class AglLineTokensAce(
        override val state: ace.LineState,
        override val tokens: Array<ace.Token>
) : ace.LineTokens {}

class AglTokenizer(
        val agl: AglComponents
) : ace.Tokenizer {

    // --- ace.Ace.Tokenizer ---
    override fun getLineTokens(line: String, pState: ace.LineState?, row: Int): ace.LineTokens {
        val sppt = this.agl.sppt
        val state = if (null == pState) AglLineStateAce(0, "") else pState as AglLineStateAce
        return if (null == sppt) {
            this.getLineTokensByScan(line, state, row)
        } else {
            this.getLineTokensByParse(line, state, row)
        }
    }

    private fun mapTokenTypeToClass(tokenType: String): String? {
        var cssClass = this.agl.tokenToClassMap.get(tokenType)
        return cssClass
    }

    private fun mapToCssClasses(leaf: SPPTLeaf): List<String> {
        val metaTagClasses = leaf.metaTags.mapNotNull { this.mapTokenTypeToClass(it) }
        val otherClasses = if (!leaf.tagList.isEmpty()) {
            leaf.tagList.mapNotNull { this.mapTokenTypeToClass(it) }
        } else {
            listOf(this.mapTokenTypeToClass(leaf.name)).mapNotNull { it }
        }
        val classes = metaTagClasses + otherClasses
        return if (classes.isEmpty()) {
            listOf("nostyle")
        } else {
            classes.toSet().toList()
        }
    }

    private fun transformToAceTokens(leafs: List<SPPTLeaf>): List<AglTokenAce> {
        return leafs.map { leaf ->
            val tokenType = leaf.name; //(it.isPattern) ? '"' + it.name + '"' : "'" + it.name + "'";
            val cssClasses = this.mapToCssClasses(leaf).toTypedArray()
            var beforeEOL = leaf.matchedText
            val eolIndex = leaf.matchedText.indexOf('\n');
            if (-1 !== eolIndex) {
                beforeEOL = leaf.matchedText.substring(0, eolIndex);
            }
            AglTokenAce(
                    cssClasses,
                    beforeEOL,
                    leaf.location.line, //ace first line is 0
                    leaf.location.column
            )
        }
    }

    private fun getLineTokensByScan(line: String, state: AglLineStateAce, row:Int): ace.LineTokens {
        val proc = this.agl.processor
        return if (null != proc) {
            val text = state.leftOverText + line
            val leafs = proc.scan(text);
            val tokens = transformToAceTokens(leafs)
            val endState = if (leafs.isEmpty()) {
                AglLineStateAce(row, "")
            } else {
                val lastLeaf = leafs.last()
                val endOfLastLeaf = lastLeaf.location.column + lastLeaf.location.length
                val leftOverText = line.substring(endOfLastLeaf, line.length)
                AglLineStateAce(row, leftOverText)
            }
            AglLineTokensAce(
                    endState,
                    tokens.toTypedArray()
            )
        } else {
            AglLineTokensAce(AglLineStateAce(row, ""), arrayOf(AglTokenAce(emptyArray(), line, row,0)))
        }
    }

    private fun getLineTokensByParse(line: String, state: AglLineStateAce, row:Int): ace.LineTokens {
        val sppt = this.agl.sppt!!
        val leafs = sppt.tokensByLine(row)
        return if (null != leafs) {
            val tokens = transformToAceTokens(leafs)
            val endState = AglLineStateAce(row, "")
            return AglLineTokensAce(
                    endState,
                    tokens.toTypedArray()
            )
        } else {
            AglLineTokensAce(AglLineStateAce(row, ""), emptyArray())
        }
    }
}