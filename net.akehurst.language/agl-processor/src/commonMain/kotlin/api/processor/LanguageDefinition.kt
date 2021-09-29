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

package net.akehurst.language.api.processor

import net.akehurst.language.api.analyser.SemanticAnalyser
import net.akehurst.language.api.analyser.SyntaxAnalyser

interface LanguageDefinition {
    val identity: String
    var grammar: String?
    var defaultGoalRule: String?
    var style: String?
    var format: String?
    val syntaxAnalyser: SyntaxAnalyser<*,*>?
    val semanticAnalyser: SemanticAnalyser<*,*>?

    val processor: LanguageProcessor?

    val grammarObservers: MutableList<(String?, String?) -> Unit>
    val styleObservers: MutableList<(String?, String?) -> Unit>
    val formatObservers: MutableList<(String?, String?) -> Unit>

    val grammarIsModifiable: Boolean
}