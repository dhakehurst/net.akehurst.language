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
package net.akehurst.language.grammar.parse.tree;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;

abstract
public class Node implements INode {

	public Node(final RuntimeRule runtimeRule) {
		this.runtimeRule = runtimeRule;
	}
		
	IBranch parent;
	public IBranch getParent() {
		return parent;
	}
	public void setParent(IBranch value) {
		this.parent = value;
	}
	
	RuntimeRule runtimeRule;
	public RuntimeRule getRuntimeRule() {
		return this.runtimeRule;
	}

	public boolean getIsSkip() {
		return this.runtimeRule.getIsSkipRule();
	}
	
	@Override
	public int getNumberOfLines() {
		String str = this.getMatchedText();
		Pattern p = Pattern.compile(System.lineSeparator());
	    Matcher m = p.matcher(str);
	    int count = 0;
	    while (m.find()){
	    	count +=1;
	    }
	    return count;
	}
}
