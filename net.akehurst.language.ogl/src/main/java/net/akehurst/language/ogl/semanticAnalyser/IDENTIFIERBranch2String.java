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
package net.akehurst.language.ogl.semanticAnalyser;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.ILeaf;
import net.akehurst.language.core.parser.INode;
import net.akehurst.transform.binary.Relation;
import net.akehurst.transform.binary.Transformer;

public class IDENTIFIERBranch2String implements Relation<INode, String>{

	@Override
	public void configureLeft2Right(INode arg0, String arg1, Transformer arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void configureRight2Left(INode arg0, String arg1, Transformer arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String constructLeft2Right(INode left, Transformer transformer) {
		ILeaf leaf = (ILeaf)((IBranch)left).getChildren().get(0);
		String right = leaf.getMatchedText();
		return right;
	}

	@Override
	public IBranch constructRight2Left(String arg0, Transformer arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isValidForLeft2Right(INode left) {
		return left.getName().equals("IDENTIFIER");
	}

	@Override
	public boolean isValidForRight2Left(String arg0) {
		// TODO Auto-generated method stub
		return false;
	}

}
