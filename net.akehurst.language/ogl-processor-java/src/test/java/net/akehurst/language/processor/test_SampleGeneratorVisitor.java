package net.akehurst.language.processor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import net.akehurst.language.api.grammar.Rule;
import net.akehurst.language.api.grammar.Terminal;
import net.akehurst.language.api.processor.CompletionItem;
import net.akehurst.language.ogl.grammar.OGLGrammar;
import net.akehurst.language.ogl.semanticStructure.GramarVisitable;

public class test_SampleGeneratorVisitor {

	@Test
	public void literal() throws Throwable {
		final OGLGrammar grammar = new OGLGrammar();

		final SampleGeneratorVisitor visitor = new SampleGeneratorVisitor(5);

		final Terminal t = grammar.findAllTerminal("grammar");
		final Set<CompletionItem> items = ((GramarVisitable) t).accept(visitor, 0);
		final List<CompletionItem> list = new ArrayList<>(items);
		Assert.assertEquals(1, items.size());
		Assert.assertEquals("grammar", list.get(0).getText());
	}

	@Test
	public void pattern() throws Throwable {
		final OGLGrammar grammar = new OGLGrammar();

		final SampleGeneratorVisitor visitor = new SampleGeneratorVisitor(5);

		final Terminal t = grammar.findAllTerminal("'(?:\\\\?.)*?'");
		final Set<CompletionItem> items = ((GramarVisitable) t).accept(visitor, 0);
		final List<CompletionItem> list = new ArrayList<>(items);
		Assert.assertEquals(1, items.size());
		Assert.assertEquals("LITERAL", list.get(0).getText());
	}

	@Test
	public void simpleChoice() throws Throwable {
		final OGLGrammar grammar = new OGLGrammar();

		final SampleGeneratorVisitor visitor = new SampleGeneratorVisitor(5);

		final Rule t = grammar.findAllRule("multiplicity");
		final Set<CompletionItem> items = ((GramarVisitable) t.getRhs()).accept(visitor, 0);
		final List<CompletionItem> list = new ArrayList<>(items);
		Assert.assertEquals(3, items.size());
		Assert.assertEquals("*", list.get(0).getText());
	}

	@Test
	public void simpleChoice2() throws Throwable {
		final OGLGrammar grammar = new OGLGrammar();

		final SampleGeneratorVisitor visitor = new SampleGeneratorVisitor(5);

		final Rule t = grammar.findAllRule("terminal");
		final Set<CompletionItem> items = ((GramarVisitable) t.getRhs()).accept(visitor, 0);
		final List<CompletionItem> list = new ArrayList<>(items);
		Assert.assertEquals(2, items.size());
		Assert.assertEquals("LITERAL", list.get(0).getText());
	}

	@Test
	public void separatedList() throws Throwable {
		final OGLGrammar grammar = new OGLGrammar();

		final SampleGeneratorVisitor visitor = new SampleGeneratorVisitor(10);

		final Rule t = grammar.findAllRule("qualifiedName");
		final Set<CompletionItem> items = ((GramarVisitable) t.getRhs()).accept(visitor, 0);
		final List<CompletionItem> list = new ArrayList<>(items);
		Assert.assertEquals(2, items.size());
		Assert.assertEquals("IDENTIFIER", list.get(0).getText());
		Assert.assertEquals("IDENTIFIER::IDENTIFIER", list.get(1).getText());
	}

	@Test
	public void concatination() throws Throwable {
		final OGLGrammar grammar = new OGLGrammar();

		final SampleGeneratorVisitor visitor = new SampleGeneratorVisitor(10);

		final Rule t = grammar.findAllRule("group");
		final Set<CompletionItem> items = ((GramarVisitable) t.getRhs()).accept(visitor, 0);
		final List<CompletionItem> list = new ArrayList<>(items);
		Assert.assertEquals(57, items.size());
		Assert.assertEquals("()", list.get(0).getText());
	}

	@Test
	public void concatination2() throws Throwable {
		final OGLGrammar grammar = new OGLGrammar();

		final SampleGeneratorVisitor visitor = new SampleGeneratorVisitor(5);

		final Rule t = grammar.findAllRule("separatedList");
		final Set<CompletionItem> items = ((GramarVisitable) t.getRhs()).accept(visitor, 0);
		final List<CompletionItem> list = new ArrayList<>(items);
		Assert.assertEquals(12, items.size());
		Assert.assertEquals("[LITERAL/LITERAL]*", list.get(0).getText());
	}
}
