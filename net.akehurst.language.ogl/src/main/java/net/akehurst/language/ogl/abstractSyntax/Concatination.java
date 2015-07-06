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

public class Concatination {

	public Concatination(Item... item) {
		this.item = Arrays.asList(item);
	}
	
	List<Item> item;
	public List<Item> getItem() {
		return this.item;
	}
	
	//--- Object ---
	@Override
	public String toString() {
		String r = "";
		for(Item i : this.getItem()) {
			r += i.toString() + " ";
		}
		return r;
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof Concatination) {
			Concatination other = (Concatination)arg;
			return this.toString().equals(other.toString());
		} else {
			return false;
		}
	}
}
