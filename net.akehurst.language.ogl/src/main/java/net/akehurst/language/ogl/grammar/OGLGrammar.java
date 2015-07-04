package net.akehurst.language.ogl.grammar;

import java.util.List;

import net.akehurst.language.ogl.semanticModel.Grammar;
import net.akehurst.language.ogl.semanticModel.GrammarBuilder;
import net.akehurst.language.ogl.semanticModel.Namespace;
import net.akehurst.language.ogl.semanticModel.NonTerminal;
import net.akehurst.language.ogl.semanticModel.Rule;
import net.akehurst.language.ogl.semanticModel.TerminalLiteral;
import net.akehurst.language.ogl.semanticModel.TerminalPattern;

/**
 * 
 * namespace net::akehurst::language::ogl::grammar;
 * 
 * grammar OGL {
 * 
 *   grammarDefinition = namespace grammar ;
 *   namespace = 'namespace' qualifiedName ;
 *   grammar = 'grammar' IDENTIFIER '{' rules '}' ;
 *   rules = rule+ ;
 *   rule = IDENTIFIER '=' choice ';' ;
 *   choice = (concatination / '|')+ ;
 *   concatination = item+ ;
 *   item = LITERAL | nonTerminal | multi | group | separatedList ;
 *   multi = item ('*' | '+' | '?') ;
 *   group = '(' choice ')' ;
 *   separatedList = '(' concatination '/' LITERAL ')' ('*' | '+') ;
 *   nonTerminal = IDENTIFIER ;
 *   qualifiedName = (IDENTIFIER / '::')+ ;
 *   LITERAL = "\x27[^\x27]*\x27" ;
 *   PATTERN = "\x22[^\x22]*\x22" ;
 *   IDENTIFIER = "[a-zA-Z_][a-zA-Z_0-9]*";
 *   
 * }
 * 
 * 
 * @author akehurst
 *
 */
public class OGLGrammar extends Grammar {
	
	static List<Rule> createRules() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("net::akehurst::language::ogl::grammar"), "OGL");
		b.skip("WHITESPACE").concatination( new TerminalPattern("\\s+") );
		b.skip("COMMENT").concatination( new TerminalPattern("(?s)/\\*.*?\\*/") );
		//b.skip("COMMENT").concatination( new TerminalPattern("/\\*(?:.|[\\n\\r])*?\\*/") );
		
		b.rule("grammarDefinition").concatenation( new NonTerminal("namespace"), new NonTerminal("grammar") );
		b.rule("namespace").concatenation( new TerminalLiteral("namespace"), new NonTerminal("qualifiedName"), new TerminalLiteral(";") );
		b.rule("grammar").concatenation( new TerminalLiteral("grammar"), new NonTerminal("IDENTIFIER"), new TerminalLiteral("{"), new NonTerminal("rules"), new TerminalLiteral("}") );
		b.rule("rules").multi(1,-1,new NonTerminal("anyRule") );
		b.rule("anyRule").choice(new NonTerminal("normalRule"), new NonTerminal("skipRule") );
		b.rule("skipRule").concatenation( new NonTerminal("IDENTIFIER"), new TerminalLiteral("?="), new NonTerminal("choice"), new TerminalLiteral(";") );
		b.rule("normalRule").concatenation( new NonTerminal("IDENTIFIER"), new TerminalLiteral(":"), new NonTerminal("choice"), new TerminalLiteral(";") );
		b.rule("choice").separatedList(1, new TerminalLiteral("|"), new NonTerminal("concatination") );
		b.rule("concatination").multi(1,-1,new NonTerminal("item") );
		b.rule("item").choice( new NonTerminal("LITERAL"),
							   new NonTerminal("PATTERN"),
				               new NonTerminal("nonTerminal"),
				               new NonTerminal("multi"),
				               new NonTerminal("group"),
				               new NonTerminal("separatedList")
						);
		b.rule("multi").concatenation( new NonTerminal("item"), new NonTerminal("multiplicity") );
		b.rule("multiplicity").choice(new TerminalLiteral("*"), new TerminalLiteral("+"), new TerminalLiteral("?"));
		b.rule("group").concatenation( new TerminalLiteral("("), new NonTerminal("choice"), new TerminalLiteral(")") );
		b.rule("separatedList").concatenation( new TerminalLiteral("("), new NonTerminal("concatination"), new TerminalLiteral("/"), new NonTerminal("LITERAL"), new TerminalLiteral(")"), new NonTerminal("multiplicity") );
		b.rule("nonTerminal").choice(new NonTerminal("IDENTIFIER"));
		b.rule("qualifiedName").separatedList(1, new TerminalLiteral("::"), new NonTerminal("IDENTIFIER") );
//		b.rule("LITERAL").concatenation( new TerminalPattern("\\x27([^\\x27])*\\x27") );
		b.rule("LITERAL").concatenation( new TerminalPattern("'(?:\\\\?.)*?'") );
//		b.rule("PATTERN").concatenation( new TerminalPattern("\\x22[^\\x22]*\\x22") );
		b.rule("PATTERN").concatenation( new TerminalPattern("\"(?:\\\\?.)*?\"") );
		b.rule("IDENTIFIER").concatenation( new TerminalPattern("[a-zA-Z_][a-zA-Z_0-9]*") );
		
		return b.get().getRule();
	}
	
	public OGLGrammar() {
		super( new Namespace("net::akehurst::language::ogl::grammar"), "OGL");
		this.setRule(createRules());
	}
	
}
