package net.akehurst.language.core.parser;

public interface IParseTree extends IParseTreeVisitable {

	INode getRoot();

	boolean getIsComplete();

	boolean getCanGrow();
}
