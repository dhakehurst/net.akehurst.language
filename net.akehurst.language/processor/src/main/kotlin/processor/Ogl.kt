/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

//@file:JvmName("Ogl")

package net.akehurst.language.processor

import net.akehurst.language.api.analyser.SemanticAnalyser
import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.ogl.grammar.OglGrammar

private val oglProcessor: LanguageProcessor by lazy {
    val grammar = OglGrammar()
    val semanticAnalyser: SemanticAnalyser? = null //TODO:
    processor(grammar)//, semanticAnalyser)
}

fun processor(grammar: Grammar): LanguageProcessor {
    return LanguageProcessorDefault(grammar, null)
}

fun processor(grammar: Grammar, semanticAnalyser: SemanticAnalyser): LanguageProcessor {
    return LanguageProcessorDefault(grammar, semanticAnalyser)
}

fun processor(grammarStr: String): LanguageProcessor {
    val grammar = oglProcessor.process<Grammar>(grammarStr, "grammar")
    return processor(grammar)
}

fun processor(grammarStr: String, semanticAnalyser: SemanticAnalyser): LanguageProcessor {
    val grammar = oglProcessor.process<Grammar>(grammarStr, "grammar")
    return processor(grammar, semanticAnalyser)
}

fun parser(rules: List<String>): LanguageProcessor {
    val grammarStr = "namespace temp; grammar Temp { ${rules.joinToString(";")} }"
    val grammar = oglProcessor.process<Grammar>(grammarStr, "grammar")
    return LanguageProcessorDefault(grammar, null)
}

fun parser(rules: String): LanguageProcessor {
    val grammarStr = "namespace temp; grammar Temp { ${rules} }"
    val grammar = oglProcessor.process<Grammar>(grammarStr, "grammar")
    return LanguageProcessorDefault(grammar, null)
}
