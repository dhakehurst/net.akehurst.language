package net.akehurst.language.core.parser;


public interface ILeaf extends INode, IParseTreeVisitable {

	String getMatchedText();
	
	@Override
	ILeaf deepClone();
}
