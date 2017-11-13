package net.akehurst.language.grammar.parser;

import net.akehurst.language.core.sppt.ISPLeaf;
import net.akehurst.language.core.sppt.IParseTreeVisitor;
import net.akehurst.language.core.sppt.ISPBranch;
import net.akehurst.language.core.sppt.ISPNode;
import net.akehurst.language.core.sppt.ISharedPackedParseTree;

public class ParseTreeToSingleLineTreeString implements IParseTreeVisitor<String, String, RuntimeException> {

	@Override
	public String visit(final ISharedPackedParseTree target, final String arg) throws RuntimeException {
		final ISPNode root = target.getRoot();
		return root.accept(this, arg);
	}

	@Override
	public String visit(final ISPLeaf target, final String arg) throws RuntimeException {
		if (target.isEmptyLeaf()) {
			return "$empty";
		} else {
			return "'" + target.getMatchedText() + "'";
		}
	}

	@Override
	public String visit(final ISPBranch target, final String arg) throws RuntimeException {
		String result = target.getName() + "{";
		if (target.getChildrenAlternatives().size() > 1) {
			result += "*";
		}
		for (final ISPNode n : target.getChildren()) {
			result += n.accept(this, arg);
		}
		result += "}";
		return result;
	}

}
