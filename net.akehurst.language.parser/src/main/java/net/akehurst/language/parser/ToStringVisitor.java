package net.akehurst.language.parser;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.ILeaf;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.IParseTreeVisitor;

public class ToStringVisitor implements IParseTreeVisitor<String, String, RuntimeException> {

	public ToStringVisitor() {
		this(System.lineSeparator(), "  ");
	}

	public ToStringVisitor(String lineSeparator, String indentIncrement) {
		this.lineSeparator = lineSeparator;
		this.indentIncrement = indentIncrement;
	}

	String lineSeparator;
	String indentIncrement;

	@Override
	public String visit(IParseTree target, String indent) throws RuntimeException {
		String s = indent;
		s += target.getRoot().accept(this, indent);
		return s;
	}

	@Override
	public String visit(ILeaf target, String indent) throws RuntimeException {
		String s = indent + target.getName() + " : \"" + target.getMatchedText().replace("\n", new String(Character.toChars(0x23CE))) + "\"";
		return s;
	}

	@Override
	public String visit(IBranch target, String indent) throws RuntimeException {
		String s = indent;
		s += target.getName() + " : [";
		if (0 < target.getChildren().size()) {
			s += this.lineSeparator;
			s += target.getChildren().get(0).accept(this, indent + indentIncrement);
			for (int i=1;i<target.getChildren().size(); ++i) {
				s += ", " + this.lineSeparator;
				s += target.getChildren().get(i).accept(this, indent + indentIncrement);
			}
			s += this.lineSeparator + indent;
		}
		s += "]";
		return s;
	}

}
