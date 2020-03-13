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
import net.akehurst.language.api.sppt.SPPTLeaf

class AglTokenProvider(
        val tokenPrefix: String,
        val agl: AglComponents
) : languages.TokensProvider {

    class ScanState(
            val lineNumber: Int,
            val leftOverText: String
    ) : languages.IState {
        override fun clone(): languages.IState {
            return ScanState(lineNumber, leftOverText)
        }

        override fun equals(other: Any?): Boolean {
            return when (other) {
                is ScanState -> other.lineNumber == this.lineNumber
                else -> false
            }
        }
    }

    companion object {
        fun NO_TOKENS(nextLineNumber: Int) = object : languages.ILineTokens {
            override val endState = ScanState(nextLineNumber, "")
            override val tokens = arrayOf<languages.IToken>(
                    object : languages.IToken {
                        override val scopes = ""
                        override val startIndex = 0
                    }
            )
        }
    }

    override fun getInitialState(): languages.IState {
        return ScanState(0, "")
    }

    override fun tokenize(line: String, state: languages.IState): languages.ILineTokens {
        try {
            if (null == this.agl.sppt) {
                return this.getLineTokensByScan(line, state)
            } else {
                return this.getLineTokensByParse(line, state)
            }
        } catch (t: Throwable) {
            console.error(t.message)
            return NO_TOKENS((state as ScanState).lineNumber + 1)
        }
    }

    private fun getLineTokensByScan(line: String, pState: languages.IState): languages.ILineTokens {
        val state = pState as ScanState
        val proc = this.agl.processor
        val nextLineNumber = state.lineNumber + 1
        if (null != proc) {
            val text = state.leftOverText + line
            val leafs = proc.scan(text);
            val tokens = leafs.map { leaf ->
                object : languages.IToken {
                    override val scopes = tokenPrefix+leaf.name //:FIXME: monaco doesn't support multiple classes on a token //mapToCssClasses(leaf).joinToString(separator = ".") { tokenPrefix+it }
                    override val startIndex = leaf.location.column - 1
                }
            }
            val endState = if (leafs.isEmpty()) {
                ScanState(nextLineNumber, text)
            } else {
                val lastLeaf = leafs.last()
                val endOfLastLeaf = lastLeaf.location.column + lastLeaf.location.length
                val leftOverText = line.substring(endOfLastLeaf, line.length)
                ScanState(nextLineNumber, leftOverText)
            }
            return object : languages.ILineTokens {
                override val endState = endState
                override val tokens: Array<languages.IToken> = tokens.toTypedArray()
            }
        } else {
            return NO_TOKENS(nextLineNumber)
        }
    }

    private fun getLineTokensByParse(line: String, pState: languages.IState): languages.ILineTokens {
        val state = pState as ScanState
        val nextLineNumber = state.lineNumber + 1
        val sppt = this.agl.sppt!!
        val leafs = sppt.tokensByLine(state.lineNumber)
        if (null != leafs) {
            val tokens = leafs.map { leaf ->
                object : languages.IToken {
                    override val scopes = tokenPrefix+leaf.name //:FIXME: monaco doesn't support multiple classes on a token //mapToCssClasses(leaf).joinToString(separator = ".") { tokenPrefix+it }
                    override val startIndex = leaf.location.column - 1
                }
            }
            return object : languages.ILineTokens {
                override val endState = ScanState(nextLineNumber, "")
                override val tokens: Array<languages.IToken> = tokens.toTypedArray()
            }
        } else {
            return return NO_TOKENS(nextLineNumber)
        }
    }

    private fun mapToCssClasses(leaf: SPPTLeaf): List<String> {
        val metaTagClasses = leaf.metaTags
        val otherClasses = if (!leaf.tagList.isEmpty()) {
            leaf.tagList
        } else {
            listOf(leaf.name)
        }
        val classes = metaTagClasses + otherClasses
        return classes;
    }
}
