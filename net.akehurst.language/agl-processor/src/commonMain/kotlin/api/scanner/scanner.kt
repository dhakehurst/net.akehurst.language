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

package net.akehurst.language.api.scanner

import net.akehurst.language.agl.api.runtime.Rule
import net.akehurst.language.agl.regex.RegexEngine
import net.akehurst.language.agl.scanner.Matchable
import net.akehurst.language.agl.sppt.CompleteTreeDataNode
import net.akehurst.language.api.processor.ScanResult
import net.akehurst.language.api.processor.ScannerKind
import net.akehurst.language.api.sppt.Sentence


interface Scanner {
    //val sentence: Sentence

    val kind: ScannerKind
    val regexEngine: RegexEngine
    val matchables: List<Matchable>

    fun reset()
    fun isEnd(sentence: Sentence, position: Int): Boolean
    fun isLookingAt(sentence: Sentence, position: Int, terminalRule: Rule): Boolean
    fun findOrTryCreateLeaf(sentence: Sentence, position: Int, terminalRule: Rule): CompleteTreeDataNode?

    fun scan(sentence: Sentence, startAtPosition: Int = 0, offsetPosition: Int = 0): ScanResult
}