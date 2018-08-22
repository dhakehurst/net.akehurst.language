/**
 * Copyright (C) 2015 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.akehurst.language.parser;

import org.junit.Assert;
import org.junit.Test;

import net.akehurst.language.api.parser.ParseFailedException;
import net.akehurst.language.api.sppt.SPPTBranch;
import net.akehurst.language.api.sppt.SharedPackedParseTree;
import net.akehurst.language.grammar.parser.forrest.ParseTreeBuilder;
import net.akehurst.language.ogl.semanticStructure.GrammarDefault;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilderDefault;
import net.akehurst.language.ogl.semanticStructure.NamespaceDefault;
import net.akehurst.language.ogl.semanticStructure.NonTerminalDefault;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteralDefault;

public class Parser_RightRecursion_Test extends AbstractParser_Test {

	GrammarDefault as() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		b.rule("as").choice(new NonTerminalDefault("as$group1"), new NonTerminalDefault("a"));
		b.rule("as$group1").concatenation(new NonTerminalDefault("a"), new NonTerminalDefault("as"));
		b.rule("a").concatenation(new TerminalLiteralDefault("a"));
		return b.get();
	}

	@Test
	public void as_as_a() throws ParseFailedException {
		// grammar, goal, input

		final GrammarDefault g = this.as();
		final String goal = "as";
		final String text = "a";

		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		;
		final SPPTBranch expected = b.branch("as", b.branch("a", b.leaf("a", "a")));
		Assert.assertEquals(expected, tree.getRoot());

	}

	@Test
	public void as_as_aa() throws ParseFailedException {
		// grammar, goal, input

		final GrammarDefault g = this.as();
		final String goal = "as";
		final String text = "aa";

		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		;
		final SPPTBranch expected = b.branch("as", b.branch("as$group1", b.branch("a", b.leaf("a", "a")), b.branch("as", b.branch("a", b.leaf("a", "a")))));
		Assert.assertEquals(expected, tree.getRoot());

	}

	@Test
	public void as_as_aaa() throws ParseFailedException {
		// grammar, goal, input

		final GrammarDefault g = this.as();
		final String goal = "as";
		final String text = "aaa";

		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		;
		final SPPTBranch expected = b.branch("as", b.branch("as$group1", b.branch("a", b.leaf("a", "a")),
				b.branch("as", b.branch("as$group1", b.branch("a", b.leaf("a", "a")), b.branch("as", b.branch("a", b.leaf("a", "a")))))));
		Assert.assertEquals(expected, tree.getRoot());

	}

	// E = 'a' | E '+a' Bm ;
	// Bm = 'b'?
	GrammarDefault hidden1() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		b.rule("S").choice(new NonTerminalDefault("E"));
		b.rule("E").choice(new TerminalLiteralDefault("a"), new NonTerminalDefault("E2"));
		b.rule("E2").concatenation(new NonTerminalDefault("E"), new TerminalLiteralDefault("+a"), new NonTerminalDefault("Bm"));
		b.rule("Bm").multi(0, 1, new TerminalLiteralDefault("b"));
		return b.get();
	}

	@Test
	public void hidden1_S_a() throws ParseFailedException {
		// grammar, goal, input
		final GrammarDefault g = this.hidden1();
		final String goal = "S";
		final String text = "a";

		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		b.define("S {");
		b.define("  E{");
		b.define("    'a'");
		b.define("  }");
		b.define("}");
		final SharedPackedParseTree expected = b.buildAndAdd();
		Assert.assertEquals(expected, tree);
	}

	@Test
	public void hidden1_S_apa() throws ParseFailedException {
		// grammar, goal, input
		final GrammarDefault g = this.hidden1();
		final String goal = "S";
		final String text = "a+a";

		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		b.define("S {");
		b.define("  E{");
		b.define("    E2{");
		b.define("      E {'a'}");
		b.define("      '+a'");
		b.define("      Bm { $empty }");
		b.define("    }");
		b.define("  }");
		b.define("}");
		final SharedPackedParseTree expected = b.buildAndAdd();
		Assert.assertEquals(expected, tree);
	}

	@Test
	public void hidden1_S_apapa() throws ParseFailedException {
		// grammar, goal, input
		final GrammarDefault g = this.hidden1();
		final String goal = "S";
		final String text = "a+a+a";

		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		b.define("S {");
		b.define("  E{");
		b.define("    E2{");
		b.define("      E {");
		b.define("        E2{");
		b.define("          E {'a'}");
		b.define("          '+a'");
		b.define("          Bm { $empty }");
		b.define("        }");
		b.define("      }");
		b.define("      '+a'");
		b.define("      Bm { $empty }");
		b.define("    }");
		b.define("  }");
		b.define("}");
		final SharedPackedParseTree expected = b.buildAndAdd();
		Assert.assertEquals(expected, tree);
	}

	@Test
	public void hidden1_S_bapa() throws ParseFailedException {
		// grammar, goal, input
		final GrammarDefault g = this.hidden1();
		final String goal = "S";
		final String text = "a+ab";

		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		b.define("S {");
		b.define("  E{");
		b.define("    E2{");
		b.define("      E {'a'}");
		b.define("      '+a'");
		b.define("      Bm { 'b' }");
		b.define("    }");
		b.define("  }");
		b.define("}");
		final SharedPackedParseTree expected = b.buildAndAdd();
		Assert.assertEquals(expected, tree);
	}

	// E = 'a' Fm Bm;
	// Fm = F? ;
	// F = '+' E ;
	// Bm = 'b'?
	GrammarDefault trailingEmpty() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		b.rule("S").choice(new NonTerminalDefault("E"));
		b.rule("E").concatenation(new TerminalLiteralDefault("a"), new NonTerminalDefault("Fm"), new NonTerminalDefault("Bm"));
		b.rule("Fm").multi(0, 1, new NonTerminalDefault("F"));
		b.rule("F").concatenation(new TerminalLiteralDefault("+"), new NonTerminalDefault("E"));
		b.rule("Bm").multi(0, 1, new TerminalLiteralDefault("b"));
		return b.get();
	}

	@Test
	public void trailingEmpty_S_a() throws ParseFailedException {
		// grammar, goal, input
		final GrammarDefault g = this.trailingEmpty();
		final String goal = "S";
		final String text = "a";

		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		b.define("S {");
		b.define("  E{");
		b.define("    'a'");
		b.define("    Fm { $empty }");
		b.define("    Bm { $empty }");
		b.define("  }");
		b.define("}");
		final SharedPackedParseTree expected = b.buildAndAdd();
		Assert.assertEquals(expected, tree);
	}

	@Test
	public void trailingEmpty_S_apa() throws ParseFailedException {
		// grammar, goal, input
		final GrammarDefault g = this.trailingEmpty();
		final String goal = "S";
		final String text = "a+a";

		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		b.define("S {");
		b.define("  E{");
		b.define("    'a'");
		b.define("    Fm {");
		b.define("      F {");
		b.define("        '+'");
		b.define("        E{ 'a' Fm{$empty} Bm{$empty} }");
		b.define("      }");
		b.define("    }");
		b.define("    Bm { $empty }");
		b.define("  }");
		b.define("}");
		final SharedPackedParseTree expected = b.buildAndAdd();
		Assert.assertEquals(expected, tree);
	}
}