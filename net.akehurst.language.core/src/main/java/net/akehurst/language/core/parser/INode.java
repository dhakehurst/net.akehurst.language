package net.akehurst.language.core.parser;

public interface INode extends IParseTreeVisitable{

	INodeType getNodeType() throws ParseTreeException;

	String getName();
	
	int getStart();
	int getEnd();
	int getMatchedTextLength();
	
	INode deepClone();

	boolean getIsEmpty();
	
}
