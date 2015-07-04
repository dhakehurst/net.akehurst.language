package net.akehurst.language.core.parser;

public class ParseFailedException extends Exception {
	public ParseFailedException(String message, IParseTree longestMatch) {
		super(message+" ("+longestMatch.getRoot().getNumberOfLines()+")");
		this.longestMatch = longestMatch;
	}
	
	IParseTree longestMatch;
	public IParseTree getLongestMatch() {
		return longestMatch;
	}
	
	
}
