package net.akehurst.language.parse.graph;

import java.util.Objects;

import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.IParseTreeVisitor;
import net.akehurst.language.grammar.parser.ParseTreeToString;
import net.akehurst.language.grammar.parser.ToStringVisitor;

public class ParseTreeFromGraph implements IParseTree {

	public ParseTreeFromGraph(final ICompleteNode gr) {
		this.root = (INode) gr;
	}

	@Override
	public <T, A, E extends Throwable> T accept(final IParseTreeVisitor<T, A, E> visitor, final A arg) throws E {
		return visitor.visit(this, arg);
	}

	INode root;

	@Override
	public INode getRoot() {
		return this.root;
	}

	@Override
	public String asString() {
		final ParseTreeToString visitor = new ParseTreeToString();
		final String s = this.accept(visitor, "");
		return s;
	}

	// --- Object ---
	static ToStringVisitor v = new ToStringVisitor();
	String toString_cache;

	@Override
	public String toString() {
		if (null == this.toString_cache) {
			this.toString_cache = this.accept(ParseTreeFromGraph.v, "");
		}
		return this.toString_cache;
	}

	@Override
	public int hashCode() {
		return this.getRoot().hashCode();
	}

	@Override
	public boolean equals(final Object arg) {
		if (arg instanceof IParseTree) {
			final IParseTree other = (IParseTree) arg;
			return Objects.equals(this.getRoot(), other.getRoot());
		} else {
			return false;
		}
	}

}
