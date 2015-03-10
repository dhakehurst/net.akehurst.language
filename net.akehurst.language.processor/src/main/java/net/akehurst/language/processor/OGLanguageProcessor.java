package net.akehurst.language.processor;

import net.akehurst.language.ogl.grammar.OGLGrammar;
import net.akehurst.language.ogl.semanticAnalyser.SemanicAnalyser;


public class OGLanguageProcessor extends LanguageProcessor {

	public OGLanguageProcessor() {
		super(new OGLGrammar(), "grammarDefinition", new SemanicAnalyser());
	}
	
}
