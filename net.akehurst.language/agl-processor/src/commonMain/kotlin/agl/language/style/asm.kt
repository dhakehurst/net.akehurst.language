/**
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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
package net.akehurst.language.agl.language.style.asm

import net.akehurst.language.agl.language.grammar.ContextFromGrammar
import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.api.processor.ProcessResult
import net.akehurst.language.api.style.*

class AglStyleModelDefault(
    _rules: List<AglStyleRule>
) : AglStyleModel {

    companion object {
        //not sure if this should be here or in grammar object
        const val KEYWORD_STYLE_ID = "\$keyword"
        const val NO_STYLE_ID = "\$nostyle"

        val DEFAULT_NO_STYLE = AglStyleRule(listOf(AglStyleSelector(NO_STYLE_ID, AglStyleSelectorKind.META))).also {
            it.styles["foreground"] = AglStyle("foreground", "black")
            it.styles["background"] = AglStyle("background", "white")
            it.styles["font-style"] = AglStyle("font-style", "normal")
        }

        fun fromString(context: ContextFromGrammar, aglStyleModelSentence: String): ProcessResult<AglStyleModel> {
            val proc = Agl.registry.agl.style.processor ?: error("Scopes language not found!")
            return proc.process(
                sentence = aglStyleModelSentence,
                options = Agl.options { semanticAnalysis { context(context) } }
            )
        }
    }

    override val rules: List<AglStyleRule>

    init {
        rules = if (_rules.any { it.selector.any { it.value == NO_STYLE_ID } }) {
            // NO_STYLE defined
            _rules
        } else {
            listOf(DEFAULT_NO_STYLE) + _rules
        }
    }

    override fun toString(): String {
        return rules.joinToString(separator = "\n") {
            val stylesStr = it.styles.values.joinToString(separator = "\n  ") { "${it.name}: ${it.value};" }
            """
${it.selector.joinToString { it.value }} {
  $stylesStr
}
""".trimIndent()
        }
    }
}