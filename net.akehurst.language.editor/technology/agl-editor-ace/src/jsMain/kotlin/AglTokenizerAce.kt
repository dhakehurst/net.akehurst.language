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
import net.akehurst.language.editor.common.AglStyleHandler
import net.akehurst.language.editor.common.AglTokenizer

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

class AglTokenizerAce(
        val agl: AglComponents,
        val aglStyleHandler: AglStyleHandler
) : ace.Tokenizer {

    val aglTokenizer = AglTokenizer(agl)

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

    fun getLineTokensByScan(line: String, state: AglLineStateAce, row: Int): ace.LineTokens {
        TODO()
    }

    fun getLineTokensByParse(line: String, state: AglLineStateAce, row: Int): ace.LineTokens {
        TODO()
    }


}