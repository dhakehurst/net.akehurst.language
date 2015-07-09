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
package net.akehurst.language.ogl.abstractSyntax;

import java.util.Arrays;
import java.util.List;

public class GrammarZ {

	public Grammar(Namespace namespace, String name, Grammar[] extends_, Rule... rule) {
		this.namespace = namespace;
		this.name = name;
		this.extends_ = Arrays.asList(extends_);
		this.rule = Arrays.asList(rule);
	}
	
	Namespace namespace;
	public Namespace getNamespace() {
		return this.namespace;
	}
	
	String name;
	public String getName() {
		return this.name;
	}
	
	List<Grammar> extends_;
	public List<Grammar> getExtends() {
		return this.extends_;
	}
	
	List<Rule> rule;
	public List<Rule> getRule() {
		return this.rule;
	}
	
	//--- Object ---
	@Override
	public String toString() {
		String r = this.getNamespace() + System.lineSeparator();
		r += "grammar "+this.getName() + "{" + System.lineSeparator();
		for(Rule i : this.getRule()) {
			r += i.toString() + System.lineSeparator();
		}
		r+="}";
		return r;
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof Grammar) {
			Grammar other = (Grammar)arg;
			return this.toString().equals(other.toString());
		} else {
			return false;
		}
	}
}
