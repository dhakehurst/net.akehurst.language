/**
 * Copyright (C) 2015 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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
package net.akehurst.language.api.sppt;

/**
 * Visitor interface for a SharedPackedParse
 *
 * @param <T> result type of visit methods
 * @param <A> type of arg to visit methods
 */
interface SharedPackedParseTreeVisitor<T, A> {

	fun visitTree(target: SharedPackedParseTree, arg: A): T

	fun visitLeaf(target: SPPTLeaf, arg: A): T

	fun visitBranch(target: SPPTBranch, arg: A): T

	fun visitNode(target: SPPTNode, arg: A) = when (target) {
		is SPPTLeaf -> visitLeaf(target, arg)
		is SPPTBranch -> visitBranch(target, arg)
		else -> error("Unknown subtype of SPPTNode ${target::class.simpleName}")

	}
}
