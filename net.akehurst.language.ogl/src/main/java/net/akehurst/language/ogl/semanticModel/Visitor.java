package net.akehurst.language.ogl.semanticModel;

public interface Visitor<T, E extends Throwable> {

	T visit(Choice target, Object... arg) throws E;

	T visit(Concatination target, Object... arg) throws E;

	T visit(Multi target, Object... arg) throws E;

	T visit(NonTerminal target, Object... arg) throws E;

	T visit(SeparatedList target, Object... arg) throws E;

	T visit(TerminalPattern target, Object... arg) throws E;

	T visit(TerminalLiteral target, Object... arg) throws E;

}
