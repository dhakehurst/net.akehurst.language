package net.akehurst.language.processor;

import net.akehurst.language.core.ILanguageProcessor;
import net.akehurst.language.core.analyser.ISemanticAnalyser;
import net.akehurst.language.core.lexicalAnalyser.ILexicalAnalyser;
import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.IParser;
import net.akehurst.language.ogl.semanticModel.Grammar;
import net.akehurst.language.ogl.semanticModel.RuleNotFoundException;
import net.akehurst.language.parser.ScannerLessParser;
import net.akehurst.language.parser.runtime.Factory;

public class LanguageProcessor implements ILanguageProcessor {

	public LanguageProcessor(Grammar grammar, String defaultGoalName, ISemanticAnalyser semanticAnalyser) {
		this.grammar = grammar;
		this.defaultGoalName = defaultGoalName;
//		this.lexicalAnalyser = new LexicalAnalyser(grammar.findTokenTypes());
		this.parseTreeFactory = new Factory();
		this.parser = new ScannerLessParser(this.parseTreeFactory, grammar);
		this.semanticAnalyser = semanticAnalyser;
	}

	Factory parseTreeFactory;
	
	Grammar grammar;
	public Grammar getGrammar() {
		return this.grammar;
	}
	
	String defaultGoalName;
	INodeType defaultGoal;
	@Override
	public INodeType getDefaultGoal() {
		if (null==this.defaultGoal) {
			try {
				this.defaultGoal = grammar.findNodeType(this.defaultGoalName);
			} catch (RuleNotFoundException e) {
				e.printStackTrace();
			}
		}
		return this.defaultGoal;
	}

	ILexicalAnalyser lexicalAnalyser;

	@Override
	public ILexicalAnalyser getLexicalAnaliser() {
		return lexicalAnalyser;
	}

	IParser parser;

	@Override
	public IParser getParser() {
		return this.parser;
	}

	ISemanticAnalyser semanticAnalyser;
	public ISemanticAnalyser getSemanticAnalyser() {
		return this.semanticAnalyser;
	}


}
