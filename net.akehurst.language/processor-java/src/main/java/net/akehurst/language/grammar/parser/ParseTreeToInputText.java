package net.akehurst.language.grammar.parser;

import net.akehurst.language.api.sppt.SPPTBranch;
import net.akehurst.language.api.sppt.SPPTLeaf;
import net.akehurst.language.api.sppt.SPPTNode;
import net.akehurst.language.api.sppt.SharedPackedParseTree;
import net.akehurst.language.api.sppt.SharedPackedParseTreeVisitor;

public class ParseTreeToInputText implements SharedPackedParseTreeVisitor<String, String, RuntimeException> {

	@Override
	public String visit(final SharedPackedParseTree target, final String arg) throws RuntimeException {
		final SPPTNode root = target.getRoot();
		return root.accept(this, arg);
	}

	@Override
	public String visit(final SPPTLeaf target, final String arg) throws RuntimeException {
		return target.getMatchedText();
	}

	@Override
	public String visit(final SPPTBranch target, final String arg) throws RuntimeException {
		String result = "";
		for (final SPPTNode n : target.getChildren()) {
			result += n.accept(this, arg);
		}
		return result;
	}

}
