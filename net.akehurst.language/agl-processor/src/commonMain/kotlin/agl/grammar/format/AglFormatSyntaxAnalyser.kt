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
package net.akehurst.language.agl.grammar.format

import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser

class AglFormatSyntaxAnalyser : SyntaxAnalyser {
    override val locationMap = mutableMapOf<Any, InputLocation>()

    override fun clear() {
        TODO("not implemented")
    }

    override fun <T> transform(sppt: SharedPackedParseTree): T {
        TODO("not implemented")
    }
}
