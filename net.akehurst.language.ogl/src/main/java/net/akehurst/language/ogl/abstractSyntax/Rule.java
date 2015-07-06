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

public class Rule {

	public Rule(String name, Choice rhs) {
		this.name = name;
		this.rhs = rhs;
	}
	
	String name;
	public String getName(){
		return name;
	}

	Choice rhs;
	public Choice getRhs(){
		return rhs;
	}
	
	//--- Object ---
	@Override
	public String toString() {
		return this.getName()+" = "+this.getRhs();
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof Rule) {
			Rule other = (Rule)arg;
			return this.toString().equals(other.toString());
		} else {
			return false;
		}
	}
}
