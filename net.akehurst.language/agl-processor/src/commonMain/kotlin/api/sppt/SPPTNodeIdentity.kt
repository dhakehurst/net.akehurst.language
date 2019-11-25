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

package net.akehurst.language.api.sppt

/**
 *
 * The identity of a node in a Shared Packed Parse Forest is composed from:
 * <ul>
 * <li>a unique rule number,
 * <li>a starting position indicating the index of a position in the input text of the parse at which this node starts,
 * <li>the length (number of characters) of the input text that is matched by this node
 * </ul>
 *
 * If a grammar is ambiguous, a parse result may contain multiple nodes with the same identity but with different children. An SPPF combines these nodes (to
 * avoid duplication) but supports the alternative lists of children.
 *
 */
interface SPPTNodeIdentity {

	/**
	 *  the number of the runtime rule used to create the node
	 */
	val runtimeRuleNumber: Int

	/**
	 *  the start position of the text matched by the node
	 */
	val startPosition: Int

	/**
	 * the length of the text matched by the node
	 */
	//val matchedTextLength: Int

}
