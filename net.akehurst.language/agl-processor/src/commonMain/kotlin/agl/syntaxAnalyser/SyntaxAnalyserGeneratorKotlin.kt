/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.akehurst.language.agl.agl.syntaxAnalyser

import net.akehurst.language.api.language.grammar.Grammar
import net.akehurst.language.api.language.grammar.GrammarRule

class SyntaxAnalyserGeneratorKotlin {

    private val _sb = StringBuilder()

    fun generateFor(grammar: Grammar): String {
        val indent = "    "
        val lineSep = "\n"
        val register = grammar.grammarRule.joinToString(separator = lineSep) {
            val fName = functionNameFor(it)
            when {
                fName == it.name -> "super.register(this::${functionNameFor(it)})"
                else -> "super.registerFor(\"${it.name}\", this::${functionNameFor(it)})"
            }
        }
        val functions = grammar.grammarRule.joinToString(separator = "$lineSep$lineSep") {
            val fName = functionNameFor(it)
            val type = "Any"
            """
            $indent// ${it.toString()}
            ${indent}private fun $fName(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): $type = TODO()
            """.trimIndent()
        }

        return """
package ${grammar.namespace.qualifiedName}

import net.akehurst.language.api.sppt.Sentence
import net.akehurst.language.api.sppt.SpptDataNodeInfo
import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserByMethodRegistrationAbstract

class ${grammar.name}SyntaxAnalyser : SyntaxAnalyserByMethodRegistrationAbstract<AsmType>() {

    override fun registerHandlers() {
$register
    }
    
$functions
}
        """.trimIndent()
    }

    private fun functionNameFor(rule: GrammarRule): String {
        return rule.name.replace("[^a-zA-Z0-9_]", "_")
    }

}