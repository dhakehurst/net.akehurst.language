package net.akehurst.language.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.akehurst.language.core.ICompletionItem;
import net.akehurst.language.core.analyser.IRule;
import net.akehurst.language.ogl.semanticStructure.ChoicePriority;
import net.akehurst.language.ogl.semanticStructure.ChoiceSimple;
import net.akehurst.language.ogl.semanticStructure.Concatenation;
import net.akehurst.language.ogl.semanticStructure.ConcatenationItem;
import net.akehurst.language.ogl.semanticStructure.Group;
import net.akehurst.language.ogl.semanticStructure.Multi;
import net.akehurst.language.ogl.semanticStructure.NonTerminal;
import net.akehurst.language.ogl.semanticStructure.SeparatedList;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteral;
import net.akehurst.language.ogl.semanticStructure.TerminalPattern;
import net.akehurst.language.ogl.semanticStructure.Visitor;

public class SampleGeneratorVisitor implements Visitor<List<ICompletionItem>, Throwable> {

	@Override
	public List<ICompletionItem> visit(final ChoiceSimple target, final Object... arg) throws Throwable {
		final List<ICompletionItem> result = new ArrayList<>();
		for (final Concatenation item : target.getAlternative()) {
			final List<ICompletionItem> options = item.accept(this, arg);
			for (final ICompletionItem option : options) {
				final CompletionItemComposite composite = new CompletionItemComposite();

				composite.getContent().add(option);

				result.add(composite);
			}
		}
		return result;
	}

	@Override
	public List<ICompletionItem> visit(final ChoicePriority target, final Object... arg) throws Throwable {
		final List<ICompletionItem> result = new ArrayList<>();
		for (final Concatenation item : target.getAlternative()) {
			final List<ICompletionItem> options = item.accept(this, arg);
			for (final ICompletionItem option : options) {
				final CompletionItemComposite composite = new CompletionItemComposite();

				composite.getContent().add(option);

				result.add(composite);
			}
		}
		return result;
	}

	@Override
	public List<ICompletionItem> visit(final Concatenation target, final Object... arg) throws Throwable {
		List<CompletionItemComposite> result = new ArrayList<>();

		for (final ConcatenationItem item : target.getItem()) {
			final List<ICompletionItem> options = item.accept(this, arg);
			if (result.isEmpty()) {
				for (final ICompletionItem ci : options) {
					final CompletionItemComposite composite = new CompletionItemComposite();
					composite.getContent().add(ci);
					result.add(composite);
				}
			} else {
				final List<CompletionItemComposite> result2 = new ArrayList<>();
				for (final CompletionItemComposite cp : result) {
					if (options.isEmpty()) {
						result2.addAll(result);
					} else {
						for (final ICompletionItem ci : options) {
							final CompletionItemComposite composite = new CompletionItemComposite();
							composite.getContent().addAll(cp.getContent());
							composite.getContent().add(ci);
							result2.add(composite);
						}
					}
				}
				result = result2;
			}
		}
		return (List<ICompletionItem>) (Object) result;
	}

	@Override
	public List<ICompletionItem> visit(final Multi target, final Object... arg) throws Throwable {
		final List<ICompletionItem> result = new ArrayList<>();
		final List<ICompletionItem> options = target.getItem().accept(this, arg);
		for (final ICompletionItem option : options) {
			final CompletionItemComposite composite = new CompletionItemComposite();
			for (int i = 0; i < target.getMin(); ++i) {
				composite.getContent().add(option);
			}
			result.add(composite);
		}
		return result;
	}

	@Override
	public List<ICompletionItem> visit(final NonTerminal target, final Object... arg) throws Throwable {
		if (arg[0] instanceof List<?>) {
			final List<IRule> rules = (List<IRule>) arg[0];
			if (rules.contains(target.getReferencedRule())) {
				// already visited this rule, don't do it again
				return Collections.emptyList();
			} else {
				final List<IRule> visited = new ArrayList<>();
				visited.addAll(rules);
				visited.add(target.getReferencedRule());
				return target.getReferencedRule().getRhs().accept(this, visited);
			}
		} else {
			throw new RuntimeException("visitor argument must be a list of IRule");
		}

	}

	@Override
	public List<ICompletionItem> visit(final SeparatedList target, final Object... arg) throws Throwable {
		final List<ICompletionItem> result = new ArrayList<>();
		if (0 == target.getMin()) {
			return Collections.emptyList();
		} else {
			final List<ICompletionItem> options = target.getItem().accept(this, arg);
			final List<ICompletionItem> sep = target.getSeparator().accept(this, arg);
			for (final ICompletionItem option : options) {
				final CompletionItemComposite composite = new CompletionItemComposite();
				for (int i = 0; i < target.getMin(); ++i) {
					composite.getContent().add(option);
					if (i < target.getMin() - 1) {
						composite.getContent().addAll(sep);
					}
				}
				result.add(composite);
			}
			return result;
		}
	}

	@Override
	public List<ICompletionItem> visit(final Group target, final Object... arg) throws Throwable {
		return target.getChoice().accept(this, arg);
	}

	@Override
	public List<ICompletionItem> visit(final TerminalPattern target, final Object... arg) throws Throwable {
		return Arrays.asList(new CompletionItemPattern(target.getOwningRule().getName(), target.getPattern()));
	}

	@Override
	public List<ICompletionItem> visit(final TerminalLiteral target, final Object... arg) throws Throwable {
		return Arrays.asList(new CompletionItemText(target.getValue()));
	}

}
