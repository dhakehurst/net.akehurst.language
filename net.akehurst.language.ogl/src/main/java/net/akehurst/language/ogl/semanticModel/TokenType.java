package net.akehurst.language.ogl.semanticModel;

import java.util.regex.Pattern;

import net.akehurst.language.core.lexicalAnalyser.ITokenType;

public class TokenType implements ITokenType {
	public TokenType(String identity, String pattern, boolean isRegex) {
		this.pattern = Pattern.compile(pattern, isRegex?Pattern.MULTILINE:Pattern.LITERAL);
		this.identity = identity;
		this.isRegEx = isRegex;
	}
	
	boolean isRegEx;
	@Override
	public boolean getIsRegEx() {
		return this.isRegEx;
	}
	
	@Override
	public String getPatternString() {
		return this.getPattern().pattern();
	}
	
	Pattern pattern;
	@Override
	public Pattern getPattern() {
		return pattern;
	}

	String identity;
	@Override
	public String getIdentity() {
		return identity;
	}
	
	@Override
	public boolean equals(Object arg) {
		if(arg instanceof ITokenType) {
			ITokenType other = (ITokenType)arg;
			return this.getPattern().equals(other.getPattern());
		} else {
			return false;
		}
	}
	@Override
	public int hashCode() {
		return this.getPattern().hashCode();
	}
}