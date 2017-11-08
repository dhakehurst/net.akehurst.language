package net.akehurst.language.grammar.parser;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.ILeaf;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.IParseTreeVisitor;

public class ParseTreeToInputText implements IParseTreeVisitor<String, String, RuntimeException> {

	@Override
	public String visit(IParseTree target, String arg) throws RuntimeException {
		return target.getRoot().accept(this, arg);
	}

	@Override
	public String visit(ILeaf target, String arg) throws RuntimeException {
		return target.getMatchedText();
	}

	@Override
	public String visit(IBranch target, String arg) throws RuntimeException {
		String result = "";
		for(INode n: target.getChildren()) {
			result += n.accept(this, arg);
		}
		return result;
	}

}
