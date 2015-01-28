package net.akehurst.language.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.akehurst.language.core.lexicalAnalyser.IToken;
import net.akehurst.language.core.lexicalAnalyser.ITokenType;
import net.akehurst.language.core.parser.INodeIdentity;
import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.IParser;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.ogl.semanticModel.Grammar;
import net.akehurst.language.ogl.semanticModel.Rule;
import net.akehurst.language.ogl.semanticModel.RuleNotFoundException;
import net.akehurst.language.ogl.semanticModel.Terminal;
import net.akehurst.language.parser.forrest.Forrest;
import net.akehurst.language.parser.forrest.Input;
import net.akehurst.language.parser.forrest.ParseTreeStartBud;

public class ScannerLessParser implements IParser {

	public ScannerLessParser(Grammar grammar) {
		this.grammar = grammar;
		this.findTerminal_cache = new HashMap<ITokenType, Terminal>();
	}

	Grammar grammar;

	public Grammar getGrammar() {
		return this.grammar;
	}

	@Override
	public List<INodeType> getNodeTypes() {
		List<INodeType> result = new ArrayList<INodeType>();
		for (Rule r : this.grammar.getRule()) {
			result.add(new INodeType() {
				@Override
				public INodeIdentity getIdentity() {
					return new INodeIdentity() {
						@Override
						public String asPrimitive() {
							return r.getName();
						}
					};
				}
			});
		}
		return result;
	}

	@Override
	public IParseTree parse(INodeType goal, CharSequence text) throws ParseFailedException, ParseTreeException {
		try {
			return this.doParse2(goal, text);
		} catch (RuleNotFoundException e) {
			// Should never happen!
			throw new RuntimeException("Should never happen");
		}
	}

	/**
	 * <code>
	 * starting tree is an empty node
	 * while(something can grow) { //i.e. a possible tree has not reached the end of the input text
	 *   for all trees in forrest
	 *     try and grow the tree
	 *       pos = length of tree match
	 *       grab all possible next tokens from pos forwards
	 *       for each possible next token
	 *         try and grow the tree up or extend outwards
	 * }
	 * 
	 * find the longest match of the goal
	 * </code>
	 * 
	 * @param goal
	 * @param text
	 * @return
	 * @throws ParseFailedException
	 * @throws RuleNotFoundException
	 * @throws ParseTreeException
	 */
	IParseTree doParse2(INodeType goal, CharSequence text) throws ParseFailedException, RuleNotFoundException, ParseTreeException {
		Input input = new Input(text);
		Forrest newForrest = new Forrest(goal, this.grammar, input);
		Forrest oldForrest = null;
		newForrest.add(new ParseTreeStartBud(input));
		oldForrest=newForrest.shallowClone();
		newForrest = oldForrest.grow();
		do {
			oldForrest=newForrest.shallowClone();
			newForrest = oldForrest.grow();
		} while (newForrest.getCanGrow() && !oldForrest.equals(newForrest));

		IParseTree match = newForrest.getLongestMatch(text);

		return match;
	}

	Map<ITokenType, Terminal> findTerminal_cache;

	Terminal findTerminal(IToken token) {
		Terminal terminal = this.findTerminal_cache.get(token.getType());
		if (null == terminal) {
			for (Terminal term : this.getGrammar().getAllTerminal()) {
				if ((!token.getType().getIsRegEx() && term.getValue().equals(token.getType().getIdentity()))
						|| (token.getType().getIsRegEx() && term.getValue().equals(token.getType().getPatternString()))) {
					this.findTerminal_cache.put(token.getType(), term);
					return term;
				}
			}
		}
		return terminal;
	}

}
