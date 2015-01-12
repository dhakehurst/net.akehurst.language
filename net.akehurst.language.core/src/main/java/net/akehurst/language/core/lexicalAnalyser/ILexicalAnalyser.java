package net.akehurst.language.core.lexicalAnalyser;

import java.util.List;


public interface ILexicalAnalyser {

	List<IToken> lex(CharSequence text);
	List<IToken> lex(int offset, CharSequence text);
	
}
