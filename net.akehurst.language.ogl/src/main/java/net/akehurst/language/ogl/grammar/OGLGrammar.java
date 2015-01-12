package net.akehurst.language.ogl.grammar;

import java.util.List;

import net.akehurst.language.ogl.semanticModel.Choice;
import net.akehurst.language.ogl.semanticModel.Concatination;
import net.akehurst.language.ogl.semanticModel.Grammar;
import net.akehurst.language.ogl.semanticModel.GrammarBuilder;
import net.akehurst.language.ogl.semanticModel.Group;
import net.akehurst.language.ogl.semanticModel.Multi;
import net.akehurst.language.ogl.semanticModel.Namespace;
import net.akehurst.language.ogl.semanticModel.NonTerminal;
import net.akehurst.language.ogl.semanticModel.Rule;
import net.akehurst.language.ogl.semanticModel.SeparatedList;
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
		
		b.rule("grammarDefinition").concatination( new NonTerminal("namespace"), new NonTerminal("grammar") );
		b.rule("namespace").concatination( new TerminalLiteral("namespace"), new NonTerminal("qualifiedName"), new TerminalLiteral(";") );
		b.rule("grammar").concatination( new TerminalLiteral("grammar"), new NonTerminal("IDENTIFIER"), new TerminalLiteral("{"), new NonTerminal("rules"),new TerminalLiteral("}") );
		b.rule("rules").multi(1,-1,new NonTerminal("rule") );
		b.rule("rule").concatination( new NonTerminal("IDENTIFIER"), new TerminalLiteral("="), new NonTerminal("choice") );
		b.rule("choice").separatedList(1, new TerminalLiteral("|"), new NonTerminal("concatination") );
		b.rule("concatination").multi(1,-1,new NonTerminal("item") );
		b.rule("item").choice( new NonTerminal("LITERAL"),
				               new NonTerminal("nonTerminal"),
				               new NonTerminal("multi"),
				               new NonTerminal("group")
						);
		b.rule("multi").concatination( new NonTerminal("item"), new NonTerminal("multi.group1") );
		b.rule("multi.group1").choice(new TerminalLiteral("*"), new TerminalLiteral("+"));
		b.rule("group").concatination( new TerminalLiteral("("), new NonTerminal("choice"), new TerminalLiteral(")") );
		b.rule("separatedList").concatination( new TerminalLiteral("("), new NonTerminal("concatination"), new TerminalLiteral("/"), new NonTerminal("LITERAL"), new TerminalLiteral(")"), new NonTerminal("separatedList.group1") );
		b.rule("separatedList.group1").choice(new TerminalLiteral("*"), new TerminalLiteral("+"));
		b.rule("nonTerminal").choice(new NonTerminal("IDENTIFIER"));
		b.rule("qualifiedName").separatedList(1, new TerminalLiteral("::"), new NonTerminal("IDENTIFIER") );
		b.rule("LITERAL").concatination( new TerminalPattern("\\x27[^\\x27]*\\x27") );
		b.rule("PATTERN").concatination( new TerminalPattern("\\x22[^\\x22]*\\x22") );
		b.rule("IDENTIFIER").concatination( new TerminalPattern("[a-zA-Z_][a-zA-Z_0-9]*") );
		
		return b.get().getRule();
	}
	
	public OGLGrammar() {
		super( new Namespace("net::akehurst::language::ogl::grammar"), "OGL");
		this.setRule(createRules());
	}
	
}
