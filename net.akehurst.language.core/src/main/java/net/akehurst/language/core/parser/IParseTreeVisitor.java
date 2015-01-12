package net.akehurst.language.core.parser;

public interface IParseTreeVisitor<T, A extends Object, E extends Throwable> {

	T visit(IParseTree target, A arg) throws E;
	T visit(ILeaf target, A arg) throws E;
	T visit(IBranch target, A arg) throws E;
	
}
