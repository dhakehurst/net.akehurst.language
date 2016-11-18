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
package net.akehurst.language.grammar.parser.forrest;

import java.util.Collections;
import java.util.Objects;

import net.akehurst.language.core.parser.INode;
import net.akehurst.language.grammar.parse.tree.Branch;
import net.akehurst.language.grammar.parse.tree.Node;

//Identifies a node as (rule,start,end)
// comparing identifiers does not mean duplicate trees
// as children can be different
public class NodeIdentifier {

	public NodeIdentifier(int ruleNumber, int start, int length) {//, int end, int nextItemIndex) {
//		this.node = node;
		this.ruleNumber = ruleNumber;
		this.start = start;
		this.length = length;
//		this.end = end;
//		this.nextItemIndex = nextItemIndex;
//		this.childrenHash_cache = 0;
//		if (node instanceof Branch) {
//			this.childrenHash_cache = Objects.hash(((Branch)node).getChildren().toArray(new Object[0]));
//		}
		this.hashCode_cache = Objects.hash(ruleNumber, start, length);//, end, nextItemIndex);//, childrenHash_cache);
	}
//	Node node;
	int ruleNumber;
	int start;
	int length;
	public int getStart() {
		return this.start;
	}
//	int end;
//	int nextItemIndex;
//	int childrenHash_cache;
	
	@Override
	public boolean equals(Object arg) {
		if (!(arg instanceof NodeIdentifier)) {
			return false;
		} else {
			NodeIdentifier other = (NodeIdentifier)arg;
			
			boolean f = ( this.start == other.start
//				&& this.end == other.end
				&& this.ruleNumber==other.ruleNumber
				&& this.length==other.length
//				&& this.nextItemIndex==other.nextItemIndex
			);
//				&& this.childrenHash_cache==other.childrenHash_cache);
			if (!f) {
				return false;
			} else {
				return true; //this.node.equals(other.node);
			}
		}
	}
	
	int hashCode_cache;
	@Override
	public int hashCode() {
		return this.hashCode_cache;
	}

	@Override
	public String toString() {
//		return "("+this.ruleNumber+","+this.start+","+this.end+","+this.nextItemIndex+")";//","+this.childrenHash_cache+")";
		return "("+this.ruleNumber+","+this.start+","+this.length+")";//","+this.childrenHash_cache+")";
	}
	
}
