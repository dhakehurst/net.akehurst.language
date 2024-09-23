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

package net.akehurst.language.base.processor

import net.akehurst.language.grammar.asm.grammar

internal object AglBase {
    //: GrammarAbstract(NamespaceDefault("net.akehurst.language.agl"), "Base") {
    const val goalRuleName = "qualifiedName"

    //override val options = listOf(GrammarOptionDefault(AglGrammarGrammar.OPTION_defaultGoalRule, goalRuleName))
    //override val defaultGoalRule: GrammarRule get() = this.findAllResolvedGrammarRule(goalRuleName)!!

    val grammar = grammar(
        namespace = "net.akehurst.language.agl.language",
        name = "Base"
    ) {
        concatenation("WHITESPACE", isSkip = true, isLeaf = true) { pat("\\s+") }
        concatenation("MULTI_LINE_COMMENT", isSkip = true, isLeaf = true) { pat("/\\*[^*]*\\*+([^*/][^*]*\\*+)*/") }
        concatenation("SINGLE_LINE_COMMENT", isSkip = true, isLeaf = true) { pat("//[^\\n\\r]*") }

        concatenation("namespace") { lit("namespace"); ref("qualifiedName") }
        concatenation("import") { lit("import"); ref("qualifiedName") }
        separatedList("qualifiedName", 1, -1) { ref("IDENTIFIER"); lit(".") }
        concatenation("IDENTIFIER", isLeaf = true) { pat("[a-zA-Z_][a-zA-Z_0-9-]*") } //TODO: do not end with '-'
    }

    const val grammarStr = """namespace net.akehurst.language.agl.language

grammar Base {
    skip leaf WHITESPACE = "\s+" ;
    skip leaf MULTI_LINE_COMMENT = "/\*[^*]*\*+(?:[^*`/`][^*]*\*+)*`/`" ;
    skip leaf SINGLE_LINE_COMMENT = "//[\n\r]*?" ;

    namespace = 'namespace' qualifiedName ;
    import = 'import' qualifiedName ;
    qualifiedName = [IDENTIFIER / '.']+ ;
    leaf IDENTIFIER = "[a-zA-Z_][a-zA-Z_0-9-]*" ;
}
"""

    const val styleStr = """${"$"}keyword {
  foreground: darkgreen;
  font-style: bold;
}"""

    const val formatterStr = """
    """

    //TODO: gen this from the ASM
    override fun toString(): String = grammarStr.trimIndent()
}