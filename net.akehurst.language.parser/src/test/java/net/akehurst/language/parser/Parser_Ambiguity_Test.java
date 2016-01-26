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

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.grammar.parser.ToStringVisitor;
import net.akehurst.language.grammar.parser.forrest.ParseTreeBuilder;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilder;
import net.akehurst.language.ogl.semanticStructure.Namespace;
import net.akehurst.language.ogl.semanticStructure.NonTerminal;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteral;
import net.akehurst.language.ogl.semanticStructure.TerminalPattern;

public class Parser_Ambiguity_Test extends AbstractParser_Test {
	
	Grammar am() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("S").multi(0, -1, new TerminalLiteral("a"));
		return b.get();
	}
	
	Grammar aq() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("S").multi(0, 1, new TerminalLiteral("a"));
		return b.get();
	}
	
	Grammar aab() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("S").choice(new TerminalLiteral("a"), new NonTerminal("ab"));
		b.rule("ab").concatenation(new TerminalLiteral("a"), new TerminalLiteral("b"));
		return b.get();
	}
	
	Grammar ae() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("S").choice(new TerminalLiteral("a"), new NonTerminal("nothing"));
		b.rule("nothing").choice();
		return b.get();
	}
	
	Grammar amq() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("S").multi(0, 1, new NonTerminal("am"));
		b.rule("am").multi(0, -1, new TerminalLiteral("a"));
		return b.get();
	}
	
	Grammar x() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("S").multi(0, 1, new NonTerminal("aaa"));
		b.rule("aaa").choice(new NonTerminal("a1"),new NonTerminal("a2"),new NonTerminal("a3"));
		b.rule("a1").multi(0, 1, new TerminalLiteral("a"));
		b.rule("a2").multi(0, 2, new TerminalLiteral("a"));
		b.rule("a3").multi(0, 3, new TerminalLiteral("a"));
		return b.get();
	}
	
	Grammar tg() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.skip("WS").concatination( new TerminalPattern("\\s+") );
		b.rule("fps").choice(new NonTerminal("fps.choice1"), new NonTerminal("fps.choice2"));
		b.rule("fps.choice1").concatenation(new NonTerminal("fp"), new NonTerminal("fps.choice1.group.multi"));
		b.rule("fps.choice1.group.multi").multi(0, -1, new NonTerminal("fps.choice1.group"));
		b.rule("fps.choice1.group").concatenation(new TerminalLiteral(","), new NonTerminal("fp"));
		b.rule("fps.choice2").concatenation(new NonTerminal("rp"), new NonTerminal("fps.choice1.group.multi"));
		b.rule("fp").concatenation(new NonTerminal("t"), new NonTerminal("name"));
		b.rule("rp").concatenation(new NonTerminal("name"), new NonTerminal("rp.multi"), new TerminalLiteral("this"));
		b.rule("rp.multi").multi(0, 1, new NonTerminal("rp.multi.group"));
		b.rule("rp.multi.group").concatenation(new NonTerminal("name"), new TerminalLiteral("."));
		b.rule("t").choice(new NonTerminal("bt"),new NonTerminal("gt"));
		b.rule("bt").concatenation(new NonTerminal("name"));
		b.rule("gt").concatenation(new NonTerminal("name"), new TerminalLiteral("("),new NonTerminal("name"), new TerminalLiteral(")"));
		b.rule("name").choice(new TerminalPattern("[a-zA-Z]+"));
		return b.get();
	}
	
	@Test
	public void am_S_empty() {
		// grammar, goal, input
		try {
			Grammar g = am();
			String goal = "S";
			String text = "";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("", "");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*S 1, 1}",st);
			
			ParseTreeBuilder b = this.builder(g, text, goal);
			IBranch expected = 
					b.branch("S",
						b.emptyLeaf("S")
					);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void am_S_a() {
		// grammar, goal, input
		try {
			Grammar g = am();
			String goal = "S";
			String text = "a";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("", "");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*S 1, 2}",st);
			
			ParseTreeBuilder b = this.builder(g, text, goal);
			IBranch expected = 
					b.branch("S",
						b.leaf("a")
					);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void am_S_aa() {
		// grammar, goal, input
		try {
			Grammar g = am();
			String goal = "S";
			String text = "aa";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("", "");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*S 1, 3}",st);
			
			ParseTreeBuilder b = this.builder(g, text, goal);
			IBranch expected = 
					b.branch("S",
							b.leaf("a"),
							b.leaf("a")
					);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void x_S_aa() {
		// grammar, goal, input
		try {
			Grammar g = x();
			String goal = "S";
			String text = "aa";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("", "");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*S 1, 3}",st);
			
			ParseTreeBuilder b = this.builder(g, text, goal);
			IBranch expected = 
					b.branch("S",
						b.branch("aaa", 
							b.branch("a2",
								b.leaf("a"),
								b.leaf("a")
							)
						)
					);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void tg_fp_V() {
		// grammar, goal, input
		try {
			Grammar g = tg();
			String goal = "fp";
			String text = "V v";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("", "");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*fp 1, 4}",st);
			
			ParseTreeBuilder b = this.builder(g, text, goal);
			IBranch expected = 
					b.branch("fp",
						b.branch("t", 
							b.branch("bt",
								b.branch("name",
									b.leaf("[a-zA-Z]+","V")
								)
							)
						),
						b.branch("WS",
							b.leaf("\\s+", " ")
						),
						b.branch("name",
							b.leaf("[a-zA-Z]+","v")
						)
					);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void tg_fp_VE() {
		// grammar, goal, input
		try {
			Grammar g = tg();
			String goal = "fp";
			String text = "V(E) v";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("", "");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*fp 1, 7}",st);
			
			ParseTreeBuilder b = this.builder(g, text, goal);
			IBranch expected = 
					b.branch("fp",
						b.branch("t", 
							b.branch("gt",
								b.branch("name",
									b.leaf("[a-zA-Z]+","V")
								),
								b.leaf("("),
								b.branch("name",
									b.leaf("[a-zA-Z]+","E")
								),				
								b.leaf(")")
							)
						),
						b.branch("WS",
							b.leaf("\\s+", " ")
						),
						b.branch("name",
							b.leaf("[a-zA-Z]+","v")
						)
					);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void tg_fps_choice1_VE() {
		// grammar, goal, input
		try {
			Grammar g = tg();
			String goal = "fps.choice1";
			String text = "V(E) v";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("", "");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*fps.choice1 1, 7}",st);
			
			ParseTreeBuilder b = this.builder(g, text, goal);
			IBranch expected = 
				b.branch("fps.choice1",
					b.branch("fp",
						b.branch("t", 
							b.branch("gt",
								b.branch("name",
									b.leaf("[a-zA-Z]+","V")
								),
								b.leaf("("),
								b.branch("name",
									b.leaf("[a-zA-Z]+","E")
								),				
								b.leaf(")")
							)
						),
						b.branch("WS",
							b.leaf("\\s+", " ")
						),
						b.branch("name",
							b.leaf("[a-zA-Z]+","v")
						)
					),
					b.branch("fps.choice1.group.multi",
						b.emptyLeaf("fps.choice1.group.multi")
					)
				);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void tg_fps_VE() {
		// grammar, goal, input
		try {
			Grammar g = tg();
			String goal = "fps";
			String text = "V(E) v";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("", "");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*fps 1, 7}",st);
			
			ParseTreeBuilder b = this.builder(g, text, goal);
			IBranch expected = 
				b.branch("fps",
					b.branch("fps.choice1",
						b.branch("fp",
							b.branch("t", 
								b.branch("gt",
									b.branch("name",
										b.leaf("[a-zA-Z]+","V")
									),
									b.leaf("("),
									b.branch("name",
										b.leaf("[a-zA-Z]+","E")
									),				
									b.leaf(")")
								)
							),
							b.branch("WS",
								b.leaf("\\s+", " ")
							),
							b.branch("name",
								b.leaf("[a-zA-Z]+","v")
							)
						),
						b.branch("fps.choice1.group.multi",
							b.emptyLeaf("fps.choice1.group.multi")
						)
					)
				);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void tg_fps_V_this() {
		// grammar, goal, input
		try {
			Grammar g = tg();
			String goal = "fps";
			String text = "V A.this";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("", "");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*fps 1, 9}",st);
			
			ParseTreeBuilder b = this.builder(g, text, goal);
			IBranch expected = 
				b.branch("fps",
					b.branch("fps.choice2",
						b.branch("rp",
							b.branch("name",
								b.leaf("[a-zA-Z]+","V")
							),
							b.branch("WS",
								b.leaf("\\s+", " ")
							),
							b.branch("rp.multi",
								b.branch("rp.multi.group",
									b.branch("name",
										b.leaf("[a-zA-Z]+","A")
									),
									b.leaf(".")
								)
							),
							b.leaf("this")
						),
						b.branch("fps.choice1.group.multi",
							b.emptyLeaf("fps.choice1.group.multi")
						)
					)
				);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
}
