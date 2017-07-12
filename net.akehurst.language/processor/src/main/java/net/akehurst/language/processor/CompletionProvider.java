package net.akehurst.language.processor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.akehurst.language.core.ICompletionItem;
import net.akehurst.language.core.analyser.INonTerminal;
import net.akehurst.language.core.analyser.IRule;
import net.akehurst.language.core.analyser.IRuleItem;
import net.akehurst.language.core.analyser.ITerminal;
import net.akehurst.language.ogl.semanticStructure.TerminalPattern;
import net.akehurst.language.ogl.semanticStructure.Visitable;

public class CompletionProvider {

	public List<ICompletionItem> provideFor(final IRuleItem item) {
		if (item instanceof ITerminal) {
			final ITerminal terminal = (ITerminal) item;
			if (terminal.isPattern()) {
				return Arrays.asList(new CompletionItemPattern(terminal.getOwningRule().getName(), ((TerminalPattern) terminal).getPattern()));
			} else {
				return Arrays.asList(new CompletionItemText(((ITerminal) item).getValue()));
			}
		} else {
			return this.provideFor((INonTerminal) item);
		}
	}

	private List<ICompletionItem> provideFor(final INonTerminal item) {
		try {
			final IRule rule = item.getReferencedRule();
			final IRuleItem rhs = rule.getRhs();

			final SampleGeneratorVisitor v = new SampleGeneratorVisitor();
			final List<ICompletionItem> options = ((Visitable) rhs).accept(v, Arrays.asList(rule));

			return options;
		} catch (final Throwable t) {
			t.printStackTrace();
			return Collections.emptyList();
		}
	}
}
