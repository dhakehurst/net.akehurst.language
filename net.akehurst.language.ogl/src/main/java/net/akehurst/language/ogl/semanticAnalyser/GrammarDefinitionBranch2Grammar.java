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
			IBranch namespaceBranch = (IBranch)left.getChildren().get(0);
			IBranch grammarBranch = (IBranch)left.getChildren().get(1);
			IBranch grammarNameBranch = (IBranch)grammarBranch.getChildren().get(1);
			
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
