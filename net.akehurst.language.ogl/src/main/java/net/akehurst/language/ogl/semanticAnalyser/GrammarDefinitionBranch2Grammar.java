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
import net.akehurst.language.ogl.semanticModel.Grammar;
import net.akehurst.language.ogl.semanticModel.Namespace;
import net.akehurst.transform.binary.Relation;
import net.akehurst.transform.binary.RelationNotFoundException;
import net.akehurst.transform.binary.Transformer;

public class GrammarDefinitionBranch2Grammar implements Relation<IBranch, Grammar>{

	@Override
	public void configureLeft2Right(IBranch arg0, Grammar arg1, Transformer arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void configureRight2Left(IBranch arg0, Grammar arg1, Transformer arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Grammar constructLeft2Right(IBranch left, Transformer transformer) {
		try {
			IBranch namespaceBranch = (IBranch)left.getChild(0);
			IBranch grammarBranch = (IBranch)left.getChild(1);
			IBranch grammarNameBranch = (IBranch)grammarBranch.getChild(1);
			
			Namespace namespace = transformer.transformLeft2Right(NamespaceBranch2Namespace.class, namespaceBranch);
			String name = transformer.transformLeft2Right(IDENTIFIERBranch2String.class, grammarNameBranch);

			Grammar right = new Grammar(namespace, name);
			
			return right;
		} catch (RelationNotFoundException e) {
			throw new RuntimeException("Unable to complete semantic analysis", e);
		}
	}

	@Override
	public IBranch constructRight2Left(Grammar arg0, Transformer arg1) {
		// TODO Auto-generated method stub, handle extends !!
		return null;
	}

	@Override
	public boolean isValidForLeft2Right(IBranch left) {
		return left.getName().equals("grammarDefinition");
	}

	@Override
	public boolean isValidForRight2Left(Grammar right) {
		// TODO Auto-generated method stub
		return false;
	}

}
