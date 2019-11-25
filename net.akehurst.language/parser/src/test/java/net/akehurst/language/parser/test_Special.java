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
import net.akehurst.language.api.sppt.SharedPackedParseTree;
import net.akehurst.language.grammar.parser.forrest.ParseTreeBuilder;
import net.akehurst.language.ogl.semanticStructure.GrammarDefault;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilderDefault;
import net.akehurst.language.ogl.semanticStructure.NamespaceDefault;
import net.akehurst.language.ogl.semanticStructure.NonTerminalDefault;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteralDefault;
import net.akehurst.language.ogl.semanticStructure.TerminalPatternDefault;

public class test_Special extends AbstractParser_Test {
    /**
     * <code>
     * S : 'a' S B B | 'a' ;
     * B : 'b' ? ;
     * </code>
     */
    GrammarDefault S() {
        final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
        // b.rule("S").choice(new NonTerminal("S1"), new NonTerminal("S2"));
        b.rule("S_group1").concatenation(new TerminalLiteralDefault("a"), new NonTerminalDefault("S"), new NonTerminalDefault("B"), new NonTerminalDefault("B"));
        b.rule("S").choice(new NonTerminalDefault("S_group1"), new TerminalLiteralDefault("a"));
        // b.rule("B").choice(new NonTerminal("B1"), new NonTerminal("B2"));
        b.rule("B").multi(0, 1, new TerminalLiteralDefault("b"));
        return b.get();
    }

    @Test
    public void S_S_aab() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.S();
        final String goal = "S";
        final String text = "aab";

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        final ParseTreeBuilder b = this.builder(g, text, goal);
        b.define("S {");
        b.define("  S_group1 {");
        b.define("    'a'");
        b.define("    S { 'a' }");
        b.define("    B { 'b' }");
        b.define("    B { $empty }");
        b.define("  }");
        b.define("}");
        b.buildAndAdd();

        b.define("S {");
        b.define("  S_group1 {");
        b.define("    'a'");
        b.define("    S { 'a' }");
        b.define("    B { $empty }");
        b.define("    B { 'b' }");
        b.define("  }");
        b.define("}");
        final SharedPackedParseTree expected = b.buildAndAdd();

        Assert.assertEquals(expected, tree);

    }

    /**
     * <code>
     * S : S S | 'a' ;
     * </code>
     */
    GrammarDefault S2() {
        final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
        b.rule("S").choice(new NonTerminalDefault("S1"), new TerminalLiteralDefault("a"));
        b.rule("S1").concatenation(new NonTerminalDefault("S"), new NonTerminalDefault("S"));
        return b.get();
    }

    @Test
    public void S2_S_aaa() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.S2();
        final String goal = "S";
        final String text = "aaa";

        final SharedPackedParseTree actual = this.process(g, text, goal);

        final ParseTreeBuilder b = this.builder(g, text, goal);
        b.define("S {");
        b.define("  S1 {");
        b.define("    S {");
        b.define("      S1 {");
        b.define("        S {'a'}");
        b.define("        S {'a'}");
        b.define("      }");
        b.define("    }");
        b.define("    S {'a'}");
        b.define("  }");
        b.define("}");
        b.buildAndAdd();
        b.define("S {");
        b.define("  S1 {");
        b.define("    S {'a'}");
        b.define("    S {");
        b.define("      S1 {");
        b.define("        S {'a'}");
        b.define("        S {'a'}");
        b.define("      }");
        b.define("    }");
        b.define("  }");
        b.define("}");
        final SharedPackedParseTree expected = b.buildAndAdd();

        Assert.assertNotNull("Parse failed", actual);
        Assert.assertEquals(expected, actual);

    }

    /**
     * <code>
     * S : S S S | S S | 'a' ;
     * </code>
     */
    GrammarDefault S3() {
        final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
        b.rule("S").choice(new NonTerminalDefault("S1"), new NonTerminalDefault("S2"), new TerminalLiteralDefault("a"));
        b.rule("S1").concatenation(new NonTerminalDefault("S"), new NonTerminalDefault("S"), new NonTerminalDefault("S"));
        b.rule("S2").concatenation(new NonTerminalDefault("S"), new NonTerminalDefault("S"));
        return b.get();
    }

    @Test
    public void S3_S_aaa() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.S3();
        final String goal = "S";
        final String text = "aaa";

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        final ParseTreeBuilder b = this.builder(g, text, goal);
        b.define("S {");
        b.define("  S1 {");
        b.define("    S {'a'}");
        b.define("    S {'a'}");
        b.define("    S {'a'}");
        b.define("  }");
        b.define("}");
        b.buildAndAdd();

        b.define("S {");
        b.define("  S2 {");
        b.define("    S {'a'}");
        b.define("    S {");
        b.define("      S2 {");
        b.define("        S {'a'}");
        b.define("        S {'a'}");
        b.define("      }");
        b.define("    }");
        b.define("  }");
        b.define("}");
        b.buildAndAdd();

        b.define("S {");
        b.define("  S2 {");
        b.define("    S {");
        b.define("      S2 {");
        b.define("        S {'a'}");
        b.define("        S {'a'}");
        b.define("      }");
        b.define("    }");
        b.define("    S {'a'}");
        b.define("  }");
        b.define("}");

        final SharedPackedParseTree expected = b.buildAndAdd();

        Assert.assertTrue(tree.contains(expected));
        Assert.assertEquals(expected, tree);

    }

    GrammarDefault parametersG() {
        final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");

        b.rule("S").priorityChoice(new NonTerminalDefault("fpfps"), new NonTerminalDefault("rpfps"));
        b.rule("fpfps").concatenation(new NonTerminalDefault("fp"), new NonTerminalDefault("fpList1"));
        b.rule("rpfps").concatenation(new NonTerminalDefault("rp"), new NonTerminalDefault("fpList2"));
        b.rule("fpList1").multi(0, -1, new NonTerminalDefault("cmrFp1"));
        b.rule("cmrFp1").concatenation(new TerminalLiteralDefault(","), new NonTerminalDefault("fp"));
        b.rule("fpList2").multi(0, -1, new NonTerminalDefault("cmrFp2"));
        b.rule("cmrFp2").concatenation(new TerminalLiteralDefault(","), new NonTerminalDefault("fp"));
        b.rule("fp").concatenation(new NonTerminalDefault("vms"), new NonTerminalDefault("unannType"));
        b.rule("rp").concatenation(new NonTerminalDefault("anns"), new NonTerminalDefault("unannType"), new TerminalLiteralDefault("this"));
        b.rule("unannType").choice(new NonTerminalDefault("unannReferenceType"));
        b.rule("unannReferenceType").choice(new NonTerminalDefault("unannClassOrInterfaceType"));
        b.rule("unannClassOrInterfaceType").choice(new NonTerminalDefault("unannClassType_lfno_unannClassOrInterfaceType"));
        b.rule("unannClassType_lfno_unannClassOrInterfaceType").concatenation(new NonTerminalDefault("Id"), new NonTerminalDefault("typeArgs"));
        b.rule("vms").multi(0, -1, new NonTerminalDefault("vm"));
        b.rule("vm").choice(new TerminalLiteralDefault("final"), new NonTerminalDefault("ann"));
        b.rule("anns").multi(0, -1, new NonTerminalDefault("ann"));
        b.rule("ann").choice(new TerminalLiteralDefault("@"), new NonTerminalDefault("Id"));
        b.rule("typeArgs").multi(0, 1, new NonTerminalDefault("typeArgList"));
        b.rule("typeArgList").concatenation(new TerminalLiteralDefault("<"), new TerminalLiteralDefault(">"));
        b.rule("Id").choice(new TerminalLiteralDefault("a"));

        return b.get();
    }

    @Test
    public void parameters() throws ParseFailedException {
        // FIXME: This test repeats work.
        // the fp and rp nodes duplicate the parsing, we only want to do it once
        // grammar, goal, input

        final GrammarDefault g = this.parametersG();
        final String goal = "S";
        final String text = "a";

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        final ParseTreeBuilder b = this.builder(g, text, goal);
        b.define("S {");
        b.define("fpfps {");
        b.define("  fp {");
        b.define("    vms { $empty }");
        b.define("    unannType {");
        b.define("      unannReferenceType {");
        b.define("        unannClassOrInterfaceType {");
        b.define("          unannClassType_lfno_unannClassOrInterfaceType {");
        b.define("            Id { 'a' }");
        b.define("            typeArgs { $empty }");
        b.define("          }");
        b.define("        }");
        b.define("      }");
        b.define("    }");
        b.define("  }");
        b.define("  fpList1 { $empty }");
        b.define("}");
        b.define("}");
        final SharedPackedParseTree expected = b.buildAndAdd();
        // final IBranch expected = b.branch("S",
        // b.branch("fpfps",
        // b.branch("fp", b.branch("vms", b.emptyLeaf("vms")), b.branch("unannType",
        // b.branch("unannReferenceType",
        // b.branch("unannClassOrInterfaceType", b.branch("unannClassType_lfno_unannClassOrInterfaceType",
        // b.branch("Id", b.leaf("a")), b.branch("typeArgs", b.emptyLeaf("typeArgs")))))))),
        // b.branch("fpList1", b.emptyLeaf("fpList1")));
        Assert.assertEquals(expected, tree);
    }

    // S = NP VP | S PP ;
    // NP = n | det n | NP PP ;
    // PP = p NP ;
    // VP = v NP ;
    // n = 'I' | 'man' | 'telescope' | 'park' ;
    // v = 'saw' | ;
    // p = 'in' | 'with' ;
    // det = 'a' | 'the' ;
    GrammarDefault tomita() {
        final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
        b.skip("WS").concatenation(new TerminalPatternDefault("\\s+"));
        b.rule("S").choice(new NonTerminalDefault("S1"), new NonTerminalDefault("S2"));
        b.rule("S1").concatenation(new NonTerminalDefault("NP"), new NonTerminalDefault("VP"));
        b.rule("S2").concatenation(new NonTerminalDefault("S"), new NonTerminalDefault("PP"));
        b.rule("NP").choice(new NonTerminalDefault("NP1"), new NonTerminalDefault("NP2"), new NonTerminalDefault("NP3"));
        b.rule("NP1").choice(new NonTerminalDefault("n"));
        b.rule("NP2").concatenation(new NonTerminalDefault("det"), new NonTerminalDefault("n"));
        b.rule("NP3").concatenation(new NonTerminalDefault("NP"), new NonTerminalDefault("PP"));
        b.rule("PP").concatenation(new NonTerminalDefault("p"), new NonTerminalDefault("NP"));
        b.rule("VP").concatenation(new NonTerminalDefault("v"), new NonTerminalDefault("NP"));
        b.rule("n").choice(new TerminalLiteralDefault("I"), new TerminalLiteralDefault("man"), new TerminalLiteralDefault("telescope"), new TerminalLiteralDefault("park"));
        b.rule("v").choice(new TerminalLiteralDefault("saw"));
        b.rule("p").choice(new TerminalLiteralDefault("in"), new TerminalLiteralDefault("with"));
        b.rule("det").choice(new TerminalLiteralDefault("a"), new TerminalLiteralDefault("the"));
        return b.get();
    }

    @Test
    public void tomita_S_1() throws ParseFailedException {
        // grammar, goal, input
        final GrammarDefault g = this.tomita();
        final String goal = "S";
        final String text = "I saw a man";

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        final ParseTreeBuilder b = this.builder(g, text, goal);
        b.define("  S {");
        b.define("    S1 {");
        b.define("      NP { NP1{ n { 'I' WS { '\\s+':' ' } } } }");
        b.define("      VP {");
        b.define("        v { 'saw' WS { '\\s+':' ' } }");
        b.define("        NP {");
        b.define("          NP2 {");
        b.define("            det { 'a' WS { '\\s+':' ' } }");
        b.define("            n { 'man' } ");
        b.define("          }");
        b.define("        }");
        b.define("      }");
        b.define("    }");
        b.define("  }");
        final SharedPackedParseTree expected = b.buildAndAdd();
        Assert.assertEquals(expected.toString(), tree.toString());
    }

    @Test
    public void tomita_S_2() throws ParseFailedException {
        // grammar, goal, input
        final GrammarDefault g = this.tomita();
        final String goal = "S";
        final String text = "I saw a man in the park";

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        final ParseTreeBuilder b = this.builder(g, text, goal);
        b.define("  S {");
        b.define("    S1 {");
        b.define("      NP { NP1{ n { 'I' WS { '\\s+':' ' } } } }");
        b.define("      VP {");
        b.define("        v { 'saw' WS { '\\s+':' ' } }");
        b.define("        NP {");
        b.define("          NP3 {");
        b.define("            NP {");
        b.define("              NP2 {");
        b.define("                det { 'a' WS { '\\s+':' ' } }");
        b.define("                n { 'man' WS { '\\s+':' ' } } ");
        b.define("              }");
        b.define("            }");
        b.define("            PP {");
        b.define("              p { 'in' WS { '\\s+':' ' } }");
        b.define("              NP {");
        b.define("                NP2 {");
        b.define("                  det { 'the' WS { '\\s+':' ' } }");
        b.define("                  n { 'park' } ");
        b.define("                }");
        b.define("              }");
        b.define("            }");
        b.define("          }");
        b.define("        }");
        b.define("      }");
        b.define("    }");
        b.define("  }");

        b.buildAndAdd();
        b.define("S {");
        b.define("  S2 {");
        b.define("    S {");
        b.define("      S1 {");
        b.define("        NP { NP1{ n { 'I' WS { '\\s+':' ' } } } }");
        b.define("        VP {");
        b.define("          v { 'saw' WS { '\\s+':' ' } }");
        b.define("          NP {");
        b.define("            NP2 {");
        b.define("              det { 'a' WS { '\\s+':' ' } }");
        b.define("              n { 'man' WS { '\\s+':' ' } } ");
        b.define("            }");
        b.define("          }");
        b.define("        }");
        b.define("      }");
        b.define("    }");
        b.define("    PP{");
        b.define("      p { 'in' WS { '\\s+':' ' } }");
        b.define("      NP {");
        b.define("        NP2 {");
        b.define("          det { 'the' WS { '\\s+':' ' } }");
        b.define("          n { 'park' } ");
        b.define("        }");
        b.define("      }");
        b.define("    }");
        b.define("  }");
        b.define("}");
        final SharedPackedParseTree expected = b.buildAndAdd();
        Assert.assertEquals(expected.toStringAll(), tree.toStringAll());
    }

    @Test
    public void tomita_S_sentence() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.tomita();
        final String goal = "S";
        final String text = "I saw a man in the park with a telescope";

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        final ParseTreeBuilder b = this.builder(g, text, goal);
        b.define("S {");
        b.define("  S2 {");
        b.define("    S {");
        b.define("      NP { n { 'I' } }");
        b.define("      VP {");
        b.define("        v { 'saw' }");
        b.define("        NP {");
        b.define("          det { 'a' }");
        b.define("          n { 'man' } ");
        b.define("        }");
        b.define("      }");
        b.define("    }");
        b.define("    PP {");
        b.define("    }");
        b.define("  }");
        b.define("}");
        b.define("  p { 'in' }");
        b.define("  det { 'the' }");
        b.define("  n { 'park' } ");
        b.define("  p { 'with' }");
        b.define("  NP {");
        b.define("    NP2 {");
        b.define("      det { 'a' }");
        b.define("      n { 'telescope' } ");
        b.define("    }");
        b.define("  }");

        b.buildAndAdd();

        b.define("S {");
        b.define("  NP { n { 'I' } }");
        b.define("  v { 'saw' }");
        b.define("  det { 'a' }");
        b.define("  n { 'man' } ");
        b.define("  p { 'in' }");
        b.define("  det { 'the' }");
        b.define("  n { 'park' } ");
        b.define("  p { 'with' }");
        b.define("  det { 'a' }");
        b.define("  n { 'telescope' } ");
        b.define("}");

        final SharedPackedParseTree expected = b.buildAndAdd();
        Assert.assertEquals(expected.toStringAll(), tree.toStringAll());

    }

    // S = Ab | A2c
    // A2 = A2a | a
    // A = aA | Aa | a

    // S = T
    // T = Ab | TTT
    // A = T bAAA | TTb | <empty>
}
