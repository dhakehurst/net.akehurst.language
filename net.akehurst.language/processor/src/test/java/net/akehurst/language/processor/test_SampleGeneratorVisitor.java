package net.akehurst.language.processor;

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import net.akehurst.language.core.ICompletionItem;
import net.akehurst.language.core.analyser.IRule;
import net.akehurst.language.core.analyser.ITerminal;
import net.akehurst.language.ogl.grammar.OGLGrammar;
import net.akehurst.language.ogl.semanticStructure.Visitable;

public class test_SampleGeneratorVisitor {

	@Test
	public void literal() throws Throwable {
		final OGLGrammar grammar = new OGLGrammar();

		final SampleGeneratorVisitor visitor = new SampleGeneratorVisitor();

		final ITerminal t = grammar.findAllTerminal("grammar");
		final List<ICompletionItem> items = ((Visitable) t).accept(visitor, Collections.EMPTY_LIST);

		Assert.assertEquals(1, items.size());
		Assert.assertEquals("grammar", items.get(0).getText());
	}

	@Test
	public void pattern() throws Throwable {
		final OGLGrammar grammar = new OGLGrammar();

		final SampleGeneratorVisitor visitor = new SampleGeneratorVisitor();

		final ITerminal t = grammar.findAllTerminal("'(?:\\\\?.)*?'");
		final List<ICompletionItem> items = ((Visitable) t).accept(visitor, Collections.EMPTY_LIST);

		Assert.assertEquals(1, items.size());
		Assert.assertEquals("LITERAL", items.get(0).getText());
	}

	@Test
	public void simpleChoice() throws Throwable {
		final OGLGrammar grammar = new OGLGrammar();

		final SampleGeneratorVisitor visitor = new SampleGeneratorVisitor();

		final IRule t = grammar.findAllRule("multiplicity");
		final List<ICompletionItem> items = ((Visitable) t.getRhs()).accept(visitor, Collections.EMPTY_LIST);

		Assert.assertEquals(3, items.size());
		Assert.assertEquals("*", items.get(0).getText());
	}

	@Test
	public void simpleChoice2() throws Throwable {
		final OGLGrammar grammar = new OGLGrammar();

		final SampleGeneratorVisitor visitor = new SampleGeneratorVisitor();

		final IRule t = grammar.findAllRule("terminal");
		final List<ICompletionItem> items = ((Visitable) t.getRhs()).accept(visitor, Collections.EMPTY_LIST);

		Assert.assertEquals(2, items.size());
		Assert.assertEquals("LITERAL", items.get(0).getText());
	}

	@Test
	public void separatedList() throws Throwable {
		final OGLGrammar grammar = new OGLGrammar();

		final SampleGeneratorVisitor visitor = new SampleGeneratorVisitor();

		final IRule t = grammar.findAllRule("qualifiedName");
		final List<ICompletionItem> items = ((Visitable) t.getRhs()).accept(visitor, Collections.EMPTY_LIST);

		Assert.assertEquals(1, items.size());
		Assert.assertEquals("IDENTIFIER", items.get(0).getText());
	}

	@Test
	public void concatination() throws Throwable {
		final OGLGrammar grammar = new OGLGrammar();

		final SampleGeneratorVisitor visitor = new SampleGeneratorVisitor();

		final IRule t = grammar.findAllRule("group");
		final List<ICompletionItem> items = ((Visitable) t.getRhs()).accept(visitor, Collections.EMPTY_LIST);

		Assert.assertEquals(1, items.size());
		Assert.assertEquals("()", items.get(0).getText());
	}

	@Test
	public void concatination2() throws Throwable {
		final OGLGrammar grammar = new OGLGrammar();

		final SampleGeneratorVisitor visitor = new SampleGeneratorVisitor();

		final IRule t = grammar.findAllRule("separatedList");
		final List<ICompletionItem> items = ((Visitable) t.getRhs()).accept(visitor, Collections.EMPTY_LIST);

		Assert.assertEquals(12, items.size());
		Assert.assertEquals("[LITERAL/LITERAL]*", items.get(0).getText());
	}
}
