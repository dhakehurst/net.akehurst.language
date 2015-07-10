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
import net.akehurst.language.ogl.semanticStructure.Namespace;
import net.akehurst.transform.binary.Relation;
import net.akehurst.transform.binary.Transformer;

public class NamespaceBranch2Namespace implements Relation<IBranch, Namespace> {

	@Override
	public void configureLeft2Right(IBranch arg0, Namespace arg1, Transformer arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void configureRight2Left(IBranch arg0, Namespace arg1, Transformer arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Namespace constructLeft2Right(IBranch left, Transformer arg1) {
		String qualifiedName = left.getChild(1).getMatchedText();
		return new Namespace(qualifiedName);
	}

	@Override
	public IBranch constructRight2Left(Namespace arg0, Transformer arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isValidForLeft2Right(IBranch left) {
		// TODO Auto-generated method stub
		return "'namespace'".equals(left.getChild(0).getName());
	}

	@Override
	public boolean isValidForRight2Left(Namespace arg0) {
		// TODO Auto-generated method stub
		return false;
	}

}
