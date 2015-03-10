package net.akehurst.language.processor;

import net.akehurst.language.core.analyser.UnableToAnalyseExeception;
import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.ogl.semanticModel.RuleNotFoundException;

import org.junit.Assert;
import org.junit.Test;

public class OGLAnalyser_Test {

	class A {

	}

	<T> T process(String grammarText, Class<T> targetType) throws ParseFailedException, UnableToAnalyseExeception {
		try {
			OGLanguageProcessor proc = new OGLanguageProcessor();

			// List<IToken> tokens = proc.getLexicalAnaliser().lex(grammar);
			INodeType goal = proc.getGrammar().findRule("grammarDefinition").getNodeType();
			IParseTree tree = proc.getParser().parse(goal, grammarText);
			T t = proc.getSemanticAnalyser().analyse(targetType, tree);

			return t;
		} catch (RuleNotFoundException e) {
			Assert.fail(e.getMessage());
			return null;
		} catch (ParseTreeException e) {
			Assert.fail(e.getMessage());
			return null;
		}
	}

	@Test
	public void a_a_a_A() {
		// grammar, goal, input, target
		try {
			String grammar = "namespace test;" + System.lineSeparator();
			grammar += "grammar A {" + System.lineSeparator();
			grammar += " a = 'a' ;" + System.lineSeparator();
			grammar += "}";

			Class<A> targetType = A.class;

			A target = this.process(grammar, targetType);

			Assert.assertNotNull(target);

		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		} catch (UnableToAnalyseExeception e) {
			Assert.fail(e.getMessage());
		}
	}

}
