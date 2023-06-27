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
package net.akehurst.language.agl.grammar.style

import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.api.grammar.GrammarItem
import net.akehurst.language.api.processor.ProcessResult
import net.akehurst.language.api.processor.SentenceContext
import net.akehurst.language.api.style.AglStyleModel
import net.akehurst.language.api.style.AglStyleRule

internal class AglStyleModelDefault(
    override val rules: List<AglStyleRule>
) : AglStyleModel {
    companion object {
        fun fromString(context: SentenceContext<String>, aglStyleModelSentence:String): ProcessResult<AglStyleModel> {
            val proc = Agl.registry.agl.style.processor ?: error("Scopes language not found!")
            return proc.process(
                sentence = aglStyleModelSentence,
                Agl.options {
                    semanticAnalysis { context(context) }
                }
            )
        }
    }

    override fun toString(): String {
        return rules.joinToString(separator = "\n") {
            val stylesStr = it.styles.values.joinToString(separator = "\n  ") { "${it.name}: ${it.value};" }
            """
${it.selector.joinToString { it }} {
  $stylesStr
}
""".trimIndent()
        }
    }
}