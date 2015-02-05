package net.akehurst.language.parser.forrest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;

import net.akehurst.language.core.parser.ILeaf;
import net.akehurst.language.ogl.semanticModel.RuleNotFoundException;
import net.akehurst.language.ogl.semanticModel.Terminal;

public class Input {

	public Input(CharSequence text) {
		this.text = text;
	}
	public CharSequence text;
	
	public CharSequence get(int start, int end) {
		return this.text.subSequence(start, end);
	}

	public int getLength() {
		return text.length();
	}

	public List<ParseTreeBud> createNewBuds(Set<Terminal> allTerminal, int pos) throws RuleNotFoundException {
		String subString = this.text.toString().substring(pos);
		List<ParseTreeBud> buds = new ArrayList<>();
		buds.add(new ParseTreeEmptyBud(this,pos)); // always add empty bud as a new bud
		for (Terminal terminal : allTerminal) {
			try {
//				if (terminal.getNodeType() instanceof SkipNodeType) {
					ILeaf l = this.tryCreateBud(terminal, subString, pos);
					ParseTreeBud bud = new ParseTreeBud(this, l, new Stack<>() );
					buds.add(bud);
//				} else if ( terminal.getNodeType().equals( this.getNextExpectedItem().getNodeType() ) ) {
//					ILeaf l = this.tryCreateBud(terminal, subString, pos);
//					buds.add(l);
//				}
			} catch (CannotCreateNewBudException e) {
				
			}
		}
		return buds;
	}
	
	ILeaf tryCreateBud(Terminal terminal, String subString, int pos) throws CannotCreateNewBudException {		
		Matcher m = terminal.getPattern().matcher(subString);
		if (m.lookingAt()) {
			String matchedText = subString.substring(0, m.end());
			int start = pos + m.start();
			int end = pos + m.end();
			Leaf leaf = new Leaf(this, start, end, terminal);
			return leaf;
		}
		throw new CannotCreateNewBudException();
	}
	
}
