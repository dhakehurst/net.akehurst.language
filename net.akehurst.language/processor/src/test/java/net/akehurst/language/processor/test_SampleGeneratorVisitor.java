package net.akehurst.language.processor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

		final SampleGeneratorVisitor visitor = new SampleGeneratorVisitor(5);

		final ITerminal t = grammar.findAllTerminal("grammar");
		final Set<ICompletionItem> items = ((Visitable) t).accept(visitor, 0);
		final List<ICompletionItem> list = new ArrayList<>(items);
		Assert.assertEquals(1, items.size());
		Assert.assertEquals("grammar", list.get(0).getText());
	}

	@Test
	public void pattern() throws Throwable {
		final OGLGrammar grammar = new OGLGrammar();

		final SampleGeneratorVisitor visitor = new SampleGeneratorVisitor(5);

		final ITerminal t = grammar.findAllTerminal("'(?:\\\\?.)*?'");
		final Set<ICompletionItem> items = ((Visitable) t).accept(visitor, 0);
		final List<ICompletionItem> list = new ArrayList<>(items);
		Assert.assertEquals(1, items.size());
		Assert.assertEquals("LITERAL", list.get(0).getText());
	}

	@Test
	public void simpleChoice() throws Throwable {
		final OGLGrammar grammar = new OGLGrammar();

		final SampleGeneratorVisitor visitor = new SampleGeneratorVisitor(5);

		final IRule t = grammar.findAllRule("multiplicity");
		final Set<ICompletionItem> items = ((Visitable) t.getRhs()).accept(visitor, 0);
		final List<ICompletionItem> list = new ArrayList<>(items);
		Assert.assertEquals(3, items.size());
		Assert.assertEquals("*", list.get(0).getText());
	}

	@Test
	public void simpleChoice2() throws Throwable {
		final OGLGrammar grammar = new OGLGrammar();

		final SampleGeneratorVisitor visitor = new SampleGeneratorVisitor(5);

		final IRule t = grammar.findAllRule("terminal");
		final Set<ICompletionItem> items = ((Visitable) t.getRhs()).accept(visitor, 0);
		final List<ICompletionItem> list = new ArrayList<>(items);
		Assert.assertEquals(2, items.size());
		Assert.assertEquals("LITERAL", list.get(0).getText());
	}

	@Test
	public void separatedList() throws Throwable {
		final OGLGrammar grammar = new OGLGrammar();

		final SampleGeneratorVisitor visitor = new SampleGeneratorVisitor(10);

		final IRule t = grammar.findAllRule("qualifiedName");
		final Set<ICompletionItem> items = ((Visitable) t.getRhs()).accept(visitor, 0);
		final List<ICompletionItem> list = new ArrayList<>(items);
		Assert.assertEquals(2, items.size());
		Assert.assertEquals("IDENTIFIER", list.get(0).getText());
		Assert.assertEquals("IDENTIFIER::IDENTIFIER", list.get(1).getText());
	}

	@Test
	public void concatination() throws Throwable {
		final OGLGrammar grammar = new OGLGrammar();

		final SampleGeneratorVisitor visitor = new SampleGeneratorVisitor(10);

		final IRule t = grammar.findAllRule("group");
		final Set<ICompletionItem> items = ((Visitable) t.getRhs()).accept(visitor, 0);
		final List<ICompletionItem> list = new ArrayList<>(items);
		Assert.assertEquals(5, items.size());
		Assert.assertEquals("()", list.get(0).getText());
	}

	@Test
	public void concatination2() throws Throwable {
		final OGLGrammar grammar = new OGLGrammar();

		final SampleGeneratorVisitor visitor = new SampleGeneratorVisitor(5);

		final IRule t = grammar.findAllRule("separatedList");
		final Set<ICompletionItem> items = ((Visitable) t.getRhs()).accept(visitor, 0);
		final List<ICompletionItem> list = new ArrayList<>(items);
		Assert.assertEquals(12, items.size());
		Assert.assertEquals("[LITERAL/LITERAL]*", list.get(0).getText());
	}
}
