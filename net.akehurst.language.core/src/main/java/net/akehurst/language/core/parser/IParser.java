package net.akehurst.language.core.parser;

import java.util.List;

public interface IParser {

	List<INodeType> getNodeTypes();

	//IParseTree parse(INodeType goal, List<IToken> tokens) throws ParseFailedException;
	IParseTree parse(INodeType goal, CharSequence text) throws ParseFailedException, ParseTreeException;
}
