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

package net.akehurst.language.api.syntaxAnalyser

import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.sppt.SharedPackedParseTree

class SyntaxAnalyserException(message: String, cause: Throwable?) : Exception(message, cause)

/**
 *
 * A Syntax Analyser converts a Parse Tree (in this case a SharedPackedParseTree) into a "Syntax Tree/Model".
 * i.e. it will map the parse tree to some other data structure that abstracts away unwanted concrete syntax information
 * e.g. as whitesapce
 *
 */
interface SyntaxAnalyser {

	val locationMap: Map<Any, InputLocation>

	/**
	 * reset the sppt2ast, clearing any cached values
	 */
	fun clear()

	/**
	 * map the tree into an instance of the targetType
	 *
	 */
	fun <T> transform(sppt: SharedPackedParseTree): T
}
