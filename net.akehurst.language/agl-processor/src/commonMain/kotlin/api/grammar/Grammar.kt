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

package net.akehurst.language.api.grammar

interface GrammarItem {

}

/**
 *
 * The definition of a Grammar. A grammar defines a list of rules and may be defined to extend a number of other Grammars.
 *
 */
interface Grammar {

	/**
	 *
	 * the namespace of this grammar;
	 */
	val namespace: Namespace

	/**
	 *
	 * the name of this grammar
	 */
	val name: String

	/**
	 * the List of grammars extended by this one
	 */
	val extends: List<Grammar>

	val rule: List<GrammarRule>

	/**
	 * the List of rules defined by this grammar and those that this grammar extends
	 * the order of the rules is the order they are defined in with the top of the grammar extension
	 * hierarchy coming first (in extension order where more than one grammar is extended)
	 */
	val allRule : List<GrammarRule>

	/**
	 * the Set of all non-terminal rules in this grammar and those that this grammar extends
	 */
	val allNonTerminalRule: Set<GrammarRule>

	/**
	 * the Set of all terminals in this grammar and those that this grammar extends
     */
	val allTerminal: Set<Terminal>

	val allEmbeddedRules: Set<Embedded>

	val allEmbeddedGrammars: Set<Grammar>

	fun findNonTerminalRule(ruleName: String): GrammarRule?

	fun findTerminalRule(terminalPattern: String): Terminal
}
