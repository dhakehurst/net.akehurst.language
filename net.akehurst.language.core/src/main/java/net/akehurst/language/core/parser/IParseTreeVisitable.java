package net.akehurst.language.core.parser;

public interface IParseTreeVisitable {

	<T, A extends Object, E extends Throwable> T accept(IParseTreeVisitor<T,A,E> visitor, A arg) throws E;
	
}
