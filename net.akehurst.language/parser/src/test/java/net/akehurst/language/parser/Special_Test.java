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
import org.junit.Before;
import org.junit.Test;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.grammar.parser.ToStringVisitor;
import net.akehurst.language.grammar.parser.forrest.ParseTreeBuilder;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSetBuilder;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilder;
import net.akehurst.language.ogl.semanticStructure.Namespace;
import net.akehurst.language.ogl.semanticStructure.NonTerminal;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteral;

public class Special_Test extends AbstractParser_Test {
	
	Grammar S() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		//b.rule("S").choice(new NonTerminal("S1"), new NonTerminal("S2"));
		b.rule("S$group1").concatenation(new TerminalLiteral("a"), new NonTerminal("S"), new NonTerminal("B"), new NonTerminal("B"));
		b.rule("S").choice(new NonTerminal("S$group1"), new TerminalLiteral("a"));
		//b.rule("B").choice(new NonTerminal("B1"), new NonTerminal("B2"));
		b.rule("B").multi(0,1,new TerminalLiteral("b"));
		return b.get();
	}
	
	@Test
	public void S_S_aab() {
		// grammar, goal, input
		try {
			Grammar g = S();
			String goal = "S";
			String text = "aab";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			ParseTreeBuilder b = this.builder(g, text, goal);;
			IBranch expected =
				b.branch("S",
					b.branch("S$group1",
						b.leaf("a", "a"),
						b.branch("S",
							b.leaf("a", "a")
						),

						b.branch("B",
							b.leaf("b", "b")
						),
						b.branch("B",
								b.emptyLeaf("B")
							)
					)
				);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	Grammar parametersG() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");

		b.rule("S").priorityChoice(new NonTerminal("fpfps"), new NonTerminal("rpfps"));
		b.rule("fpfps").concatenation(new NonTerminal("fp"), new NonTerminal("fpList1"));
		b.rule("rpfps").concatenation(new NonTerminal("rp"), new NonTerminal("fpList2"));
		b.rule("fpList1").multi(0,-1,new NonTerminal("cmrFp1"));
		b.rule("cmrFp1").concatenation(new TerminalLiteral(","), new NonTerminal("fp"));
		b.rule("fpList2").multi(0,-1,new NonTerminal("cmrFp2"));
		b.rule("cmrFp2").concatenation(new TerminalLiteral(","), new NonTerminal("fp"));
		b.rule("fp").concatenation(new NonTerminal("vms"), new NonTerminal("unannType"));
		b.rule("rp").concatenation(new NonTerminal("anns"), new NonTerminal("unannType"), new TerminalLiteral("this"));
		b.rule("unannType").choice(new NonTerminal("unannReferenceType"));
		b.rule("unannReferenceType").choice(new NonTerminal("unannClassOrInterfaceType"));
		b.rule("unannClassOrInterfaceType").choice(new NonTerminal("unannClassType_lfno_unannClassOrInterfaceType"));
		b.rule("unannClassType_lfno_unannClassOrInterfaceType").concatenation(new NonTerminal("Id"),new NonTerminal("typeArgs"));
		b.rule("vms").multi(0,-1,new NonTerminal("vm"));
		b.rule("vm").choice(new TerminalLiteral("final"), new NonTerminal("ann"));
		b.rule("anns").multi(0,-1,new NonTerminal("ann"));
		b.rule("ann").choice(new TerminalLiteral("@"), new NonTerminal("Id"));
		b.rule("typeArgs").multi(0,1,new NonTerminal("typeArgList"));
		b.rule("typeArgList").concatenation(new TerminalLiteral("<"), new TerminalLiteral(">"));
		b.rule("Id").choice(new TerminalLiteral("a"));
		
		return b.get();
	}
	
	@Test
	public void parameters() {
		//FIXME: This test repeats work.
		// the fp and rp nodes duplicate the parsing, we only want to do it once
		// grammar, goal, input
		try {
			Grammar g = parametersG();
			String goal = "S";
			String text = "a";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			ParseTreeBuilder b = this.builder(g, text, goal);;
			IBranch expected =
				b.branch("S",
						b.branch("fpfps",
							b.branch("fp",
									b.leaf("a")
							)
						)

				);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
}
