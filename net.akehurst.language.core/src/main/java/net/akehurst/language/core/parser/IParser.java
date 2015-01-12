package net.akehurst.language.core.parser;

import java.util.List;

import net.akehurst.language.core.lexicalAnalyser.IToken;

public interface IParser {

	List<INodeType> getNodeTypes();

	//IParseTree parse(INodeType goal, List<IToken> tokens) throws ParseFailedException;
	IParseTree parse(INodeType goal, CharSequence text) throws ParseFailedException, ParseTreeException;
}
