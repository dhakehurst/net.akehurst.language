package net.akehurst.language.grammar.parser;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.ILeaf;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.IParseTreeVisitor;

public class ParseTreeToSingleLineTreeString implements IParseTreeVisitor<String, String, RuntimeException> {

	@Override
	public String visit(final IParseTree target, final String arg) throws RuntimeException {
		return target.getRoot().accept(this, arg);
	}

	@Override
	public String visit(final ILeaf target, final String arg) throws RuntimeException {
		return "'" + target.getMatchedText() + "'";
	}

	@Override
	public String visit(final IBranch target, final String arg) throws RuntimeException {
		String result = target.getName() + "{";
		for (final INode n : target.getChildren()) {
			result += n.accept(this, arg);
		}
		result += "}";
		return result;
	}

}
