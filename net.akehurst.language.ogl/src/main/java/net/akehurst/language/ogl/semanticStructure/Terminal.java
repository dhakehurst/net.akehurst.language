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
package net.akehurst.language.ogl.semanticStructure;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public abstract class Terminal extends TangibleItem {

	public Terminal(String value) {
		this.value = value;
	}
	
	String value;
	public String getValue() {
		return this.value;
	}

	abstract public Pattern getPattern();
	
	public boolean matches(String value) {
		return this.getPattern().matcher(value).matches();
	}
	
//	public Set<TangibleItem> findFirstTangibleItem() {
//		Set<TangibleItem> result = new HashSet<>();
//		result.add( this );
//		return result;
//	}
//	
	
	@Override
	public Set<Terminal> findAllTerminal() {
		Set<Terminal> result = new HashSet<>();
		result.add(this);
		return result;
	}
	
	@Override
	public Set<NonTerminal> findAllNonTerminal() {
		Set<NonTerminal> result = new HashSet<>();
		return result;
	}
	
//	
//	@Override
//	public boolean isMatchedBy(INode node) throws RuleNotFoundException {
//		return node.getNodeType().equals(this.getNodeType());
//	}
}
