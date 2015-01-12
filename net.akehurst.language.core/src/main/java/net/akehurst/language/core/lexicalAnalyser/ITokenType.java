package net.akehurst.language.core.lexicalAnalyser;

import java.util.regex.Pattern;

public interface ITokenType {

	String getIdentity();
	String getPatternString();
	Pattern getPattern();
	boolean getIsRegEx();

}
