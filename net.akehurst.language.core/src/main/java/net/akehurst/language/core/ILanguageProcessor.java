package net.akehurst.language.core;

import net.akehurst.language.core.lexicalAnalyser.ILexicalAnalyser;
import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.IParser;

public interface ILanguageProcessor {

	ILexicalAnalyser getLexicalAnaliser();
	
	IParser getParser();

	INodeType getDefaultGoal();
}
