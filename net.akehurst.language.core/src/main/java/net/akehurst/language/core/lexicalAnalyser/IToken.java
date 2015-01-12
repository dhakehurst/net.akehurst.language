package net.akehurst.language.core.lexicalAnalyser;

public interface IToken {

	ITokenType getType();
	String getText();
	int getStartPosition();
	int getLength();
	boolean getIsRegularExpresion();
	
}
