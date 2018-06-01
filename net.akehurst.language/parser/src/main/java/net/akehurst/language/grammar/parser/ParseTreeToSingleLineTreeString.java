package net.akehurst.language.grammar.parser;

import net.akehurst.language.core.sppt.SPPTLeaf;
import net.akehurst.language.core.sppt.SharedPackedParseTreeVisitor;
import net.akehurst.language.core.sppt.SPPTBranch;
import net.akehurst.language.core.sppt.SPPTNode;
import net.akehurst.language.core.sppt.SharedPackedParseTree;

public class ParseTreeToSingleLineTreeString implements SharedPackedParseTreeVisitor<String, String, RuntimeException> {

	@Override
	public String visit(final SharedPackedParseTree target, final String arg) throws RuntimeException {
		final SPPTNode root = target.getRoot();
		return root.accept(this, arg);
	}

	@Override
	public String visit(final SPPTLeaf target, final String arg) throws RuntimeException {
		if (target.isEmptyLeaf()) {
			return "$empty";
		} else {
			return "'" + target.getMatchedText() + "'";
		}
	}

	@Override
	public String visit(final SPPTBranch target, final String arg) throws RuntimeException {
		String result = target.getName() + "{";
		if (target.getChildrenAlternatives().size() > 1) {
			result += "*";
		}
		for (final SPPTNode n : target.getChildren()) {
			result += n.accept(this, arg);
		}
		result += "}";
		return result;
	}

}
