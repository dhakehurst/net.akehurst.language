/*
 * Copyright (C) 2025 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.scanner.common

import net.akehurst.language.parser.api.Rule
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.regex.api.RegexEngine
import net.akehurst.language.scanner.api.Matchable
import net.akehurst.language.scanner.api.ScannerKind
import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sppt.treedata.CompleteTreeDataNode

class ScannerFromMatchables(
    regexEngine: RegexEngine = RegexEnginePlatform,
    private val _matchables: ()->List<Matchable>
): ScannerAbstract(regexEngine) {

    override val matchables: List<Matchable> get() = _matchables.invoke()
    override val kind: ScannerKind get() = error("Not used")
    override val validTerminals: List<Rule> get() = error("Not used")
    override fun reset() {}
    override fun isLookingAt(sentence: Sentence, position: Int, terminalRule: Rule): Boolean = error("Not used")
    override fun findOrTryCreateLeaf(sentence: Sentence, position: Int, terminalRule: Rule): CompleteTreeDataNode = error("Not used")

}