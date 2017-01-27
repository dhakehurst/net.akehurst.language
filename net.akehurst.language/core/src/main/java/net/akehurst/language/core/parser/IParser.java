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
package net.akehurst.language.core.parser;

import java.io.Reader;
import java.util.Set;

public interface IParser {

	/**
	 * It is not necessary to call this method, but doing so will speed up future calls to parse as it will build the internal caches for the parser,
	 */
	void build();

	Set<INodeType> getNodeTypes();

	IParseTree parse(String goalRuleName, Reader inputText) throws ParseFailedException, ParseTreeException, RuleNotFoundException;
	// IParseTree parse(INodeType goal, CharSequence text) throws ParseFailedException, ParseTreeException;
}
