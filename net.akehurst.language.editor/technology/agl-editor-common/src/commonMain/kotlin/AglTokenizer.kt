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
package net.akehurst.language.editor.common

import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.api.sppt.SPPTLeaf
import net.akehurst.language.api.sppt.SharedPackedParseTree

open class AglComponents(
        val languageId: String
) {
    val styleHandler = AglStyleHandler(languageId)
    var processor: LanguageProcessor? = null
    var goalRule: String? = null
    var sppt: SharedPackedParseTree? = null
    var asm: Any? = null
}

class AglLineState(
        val lineNumber: Int,
        val leftOverText: String,
        val tokens: List<AglToken>
) {
}

class AglToken(
        val styles: Array<String>,
        val value: String,
        val line: Int,
        val column: Int
)

class AglTokenizer(
        val agl: AglComponents
) {

    fun getLineTokens(lineText: String, pState: AglLineState, line: Int): AglLineState {
        val sppt = this.agl.sppt
        return if (null == sppt) {
            this.getLineTokensByScan(lineText, pState, line)
        } else {
            this.getLineTokensByParse(lineText, pState, line)
        }
    }

    private fun mapTokenTypeToClass(tokenType: String): String? {
        var cssClass = this.agl.styleHandler.tokenToClassMap.get(tokenType)
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

    fun transformToTokens(leafs: List<SPPTLeaf>): List<AglToken> {
        return leafs.map { leaf ->
            val cssClasses = this.mapToCssClasses(leaf)
            var beforeEOL = leaf.matchedText
            val eolIndex = leaf.matchedText.indexOf('\n');
            if (-1 !== eolIndex) {
                beforeEOL = leaf.matchedText.substring(0, eolIndex);
            }
            AglToken(
                    cssClasses.toSet().toTypedArray(),
                    beforeEOL,
                    leaf.location.line, //ace first line is 0
                    leaf.location.column
            )
        }
    }

    fun getLineTokensByScan(lineText: String, state: AglLineState, row: Int): AglLineState {
        val proc = this.agl.processor
        return if (null != proc) {
            val text = state.leftOverText + lineText
            val leafs = proc.scan(text);
            val tokens = transformToTokens(leafs)
            val endState = if (leafs.isEmpty()) {
                AglLineState(row, "", emptyList())
            } else {
                val lastLeaf = leafs.last()
                val endOfLastLeaf = lastLeaf.location.column + lastLeaf.location.length
                val leftOverText = lineText.substring(endOfLastLeaf, lineText.length)
                AglLineState(row, leftOverText, tokens)
            }
            return endState
        } else {
            AglLineState(row, "", listOf(AglToken(emptyArray(), lineText, row, 0)))
        }
    }

    fun getLineTokensByParse(lineText: String, state: AglLineState, row: Int): AglLineState {
        val sppt = this.agl.sppt!!
        val leafs = sppt.tokensByLine(row)
        return if (null != leafs) {
            val tokens = transformToTokens(leafs)
            val endState = AglLineState(row, "", tokens)
            return endState
        } else {
            AglLineState(row, "", emptyList())
        }
    }
}