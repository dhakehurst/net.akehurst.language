package net.akehurst.language.grammar.parser;

import net.akehurst.language.core.sppf.ILeaf;
import net.akehurst.language.core.sppf.IParseTreeVisitor;
import net.akehurst.language.core.sppf.ISPPFBranch;
import net.akehurst.language.core.sppf.ISPPFNode;
import net.akehurst.language.core.sppf.ISharedPackedParseTree;

public class ParseTreeToInputText implements IParseTreeVisitor<String, String, RuntimeException> {

	@Override
	public String visit(final ISharedPackedParseTree target, final String arg) throws RuntimeException {
		final ISPPFNode root = target.getRoot();
		return root.accept(this, arg);
	}

	@Override
	public String visit(final ILeaf target, final String arg) throws RuntimeException {
		return target.getMatchedText();
	}

	@Override
	public String visit(final ISPPFBranch target, final String arg) throws RuntimeException {
		String result = "";
		for (final ISPPFNode n : target.getChildren()) {
			result += n.accept(this, arg);
		}
		return result;
	}

}
