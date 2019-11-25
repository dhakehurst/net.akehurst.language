
import java.time.Duration;
import java.time.Instant;

import org.junit.Assert;

import net.akehurst.language.api.grammar.GrammarRuleNotFoundException;
import net.akehurst.language.api.parser.ParseFailedException;
import net.akehurst.language.api.parser.ParseTreeException;
import net.akehurst.language.api.parser.Parser;
import net.akehurst.language.api.sppt.SharedPackedParseTree;
import net.akehurst.language.grammar.parser.ScannerLessParser3;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSetBuilder;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilderDefault;
import net.akehurst.language.ogl.semanticStructure.GrammarDefault;
import net.akehurst.language.ogl.semanticStructure.NamespaceDefault;
import net.akehurst.language.ogl.semanticStructure.NonTerminalDefault;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteralDefault;

public class Main {

	static SharedPackedParseTree process(final GrammarDefault grammar, final String text, final String goalName) throws ParseFailedException {
		try {
			final RuntimeRuleSetBuilder runtimeRules = new RuntimeRuleSetBuilder();
			final Parser parser = new ScannerLessParser3(runtimeRules, grammar);
			final SharedPackedParseTree tree = parser.parse(goalName, text);
			return tree;
		} catch (final GrammarRuleNotFoundException e) {
			Assert.fail(e.getMessage());
			return null;
		} catch (final ParseTreeException e) {
			Assert.fail(e.getMessage());
			return null;
		}
	}

	// S = b | SSA
	// A = S | <empty>
	static GrammarDefault SA() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		b.rule("S").choice(new NonTerminalDefault("S1"), new NonTerminalDefault("S2"));
		b.rule("S1").choice(new TerminalLiteralDefault("b"));
		b.rule("S2").concatenation(new NonTerminalDefault("S"), new NonTerminalDefault("S"), new NonTerminalDefault("A"));
		b.rule("A").choice(new NonTerminalDefault("A1"), new NonTerminalDefault("A2"));
		b.rule("A1").concatenation(new NonTerminalDefault("S"));
		b.rule("A2").choice();
		// TODO:
		return b.get();
	}

	public static void main(final String[] args) {
		try {
			for (int len = 1; len < 30; ++len) {
				final GrammarDefault g = Main.SA();
				final String goal = "S";
				String text = "";

				for (int i = 0; i < len; i++) {
					text += "b";
				}

				final Instant b = Instant.now();
				final SharedPackedParseTree tree = Main.process(g, text, goal);
				final Instant a = Instant.now();
				final Duration d = Duration.between(b, a);
				System.out.println("length=" + len + "  time =" + d);
				//System.out.println(tree.toStringAll());
			}
		} catch (final Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
