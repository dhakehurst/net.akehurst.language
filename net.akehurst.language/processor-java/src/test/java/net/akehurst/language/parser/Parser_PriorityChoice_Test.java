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

public class Parser_PriorityChoice_Test extends AbstractParser_Test {

    GrammarDefault abc() {
        final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
        b.rule("abc").priorityChoice(new NonTerminalDefault("a"), new NonTerminalDefault("b"), new NonTerminalDefault("c"));
        b.rule("a").concatenation(new TerminalLiteralDefault("a"));
        b.rule("b").concatenation(new TerminalLiteralDefault("b"));
        b.rule("c").concatenation(new TerminalLiteralDefault("c"));
        return b.get();
    }

    GrammarDefault aempty() {
        final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
        b.rule("a").priorityChoice();
        return b.get();
    }

    @Test
    public void aempty_a_empty() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.aempty();
        final String goal = "a";
        final String text = "";

        final SharedPackedParseTree actual = this.process(g, text, goal);

        final ParseTreeBuilder b = this.builder(g, text, goal);
        b.define("a {");
        b.define("  $empty");
        b.define("}");
        final SharedPackedParseTree expected = b.buildAndAdd();

        Assert.assertNotNull(actual);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void abc_abc_a() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.abc();
        final String goal = "abc";
        final String text = "a";

        final SharedPackedParseTree actual = this.process(g, text, goal);

        final ParseTreeBuilder b = this.builder(g, text, goal);
        b.define("abc {");
        b.define("  a {");
        b.define("    'a'");
        b.define("  }");
        b.define("}");
        final SharedPackedParseTree expected = b.buildAndAdd();

        Assert.assertNotNull(actual);
        Assert.assertEquals(expected, actual);

    }

    @Test
    public void abc_abc_b() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.abc();
        final String goal = "abc";
        final String text = "b";

        final SharedPackedParseTree actual = this.process(g, text, goal);

        final ParseTreeBuilder b = this.builder(g, text, goal);
        b.define("abc {");
        b.define("  b {");
        b.define("    'b'");
        b.define("  }");
        b.define("}");
        final SharedPackedParseTree expected = b.buildAndAdd();

        Assert.assertNotNull(actual);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void abc_abc_c() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.abc();
        final String goal = "abc";
        final String text = "c";

        final SharedPackedParseTree actual = this.process(g, text, goal);

        final ParseTreeBuilder b = this.builder(g, text, goal);
        b.define("abc {");
        b.define("  c {");
        b.define("    'c'");
        b.define("  }");
        b.define("}");
        final SharedPackedParseTree expected = b.buildAndAdd();

        Assert.assertNotNull(actual);
        Assert.assertEquals(expected, actual);
    }

    GrammarDefault kwOrId1() {
        final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
        b.rule("S").choice(new NonTerminalDefault("type"));
        b.rule("type").priorityChoice(new NonTerminalDefault("id"), new NonTerminalDefault("kw"));
        b.rule("kw").concatenation(new TerminalLiteralDefault("int"));
        b.rule("id").concatenation(new TerminalPatternDefault("[a-z]+"));
        return b.get();
    }

    @Test
    public void kwOrId_S_id() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.kwOrId1();
        final String goal = "S";
        final String text = "int";

        final SharedPackedParseTree actual = this.process(g, text, goal);

        final ParseTreeBuilder b = this.builder(g, text, goal);
        b.define("S {");
        b.define("  type {");
        b.define("    id { '[a-z]+' : 'int' }");
        b.define("  }");
        b.define("}");
        final SharedPackedParseTree expected = b.buildAndAdd();

        Assert.assertNotNull(actual);
        Assert.assertEquals(expected, actual);

    }

    GrammarDefault kwOrId2() {
        final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
        b.rule("S").choice(new NonTerminalDefault("type"));
        b.rule("type").priorityChoice(new NonTerminalDefault("kw"), new NonTerminalDefault("id"));
        b.rule("kw").concatenation(new TerminalLiteralDefault("int"));
        b.rule("id").concatenation(new TerminalPatternDefault("[a-z]+"));
        return b.get();
    }

    @Test
    public void kwOrId_S_int() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.kwOrId2();
        final String goal = "S";
        final String text = "int";

        final SharedPackedParseTree actual = this.process(g, text, goal);

        final ParseTreeBuilder b = this.builder(g, text, goal);
        b.define("S {");
        b.define("  type {");
        b.define("    kw { 'int' }");
        b.define("  }");
        b.define("}");
        final SharedPackedParseTree expected = b.buildAndAdd();

        Assert.assertNotNull(actual);
        Assert.assertEquals(expected, actual);

    }
    // more tests needed!!!!
}