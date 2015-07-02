package net.akehurst.language.ogl.semanticAnalyser;

import net.akehurst.language.core.analyser.ISemanticAnalyser;
import net.akehurst.language.core.analyser.UnableToAnalyseExeception;
import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.ogl.semanticModel.Grammar;
import net.akehurst.transform.binary.AbstractTransformer;
import net.akehurst.transform.binary.RelationNotFoundException;

public class SemanicAnalyser extends AbstractTransformer implements ISemanticAnalyser {

	public SemanicAnalyser() {
		super.registerRule(GrammarDefinitionBranch2Grammar.class);
		super.registerRule(IDENTIFIERBranch2String.class);
		super.registerRule(NamespaceBranch2Namespace.class);
		super.registerRule(TerminalLiteralNode2Terminal.class);
		super.registerRule(TerminalPatternNode2Terminal.class);
	}
	
	
	Grammar analyse(IParseTree parseTree) throws UnableToAnalyseExeception {
		try {
			Grammar grammar = this.transformLeft2Right(GrammarDefinitionBranch2Grammar.class, (IBranch)parseTree.getRoot());
			return grammar;
		} catch (RelationNotFoundException e) {
			throw new UnableToAnalyseExeception("Cannot Analyse ParseTree",e);
		}
		
	}


	@Override
	public <T> T analyse(Class<T> targetType, IParseTree tree) throws UnableToAnalyseExeception {
		//this.transformLeft2Right(Relation.class, (IBranch)tree.getRoot());
		return (T)this.analyse(tree);
	}
	
}
