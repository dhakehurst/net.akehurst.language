package net.akehurst.language.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.akehurst.language.core.ICompletionItem;
import net.akehurst.language.core.analyser.IRuleItem;
import net.akehurst.language.core.analyser.ITerminal;
import net.akehurst.language.ogl.semanticStructure.TerminalPattern;
import net.akehurst.language.ogl.semanticStructure.Visitable;

public class CompletionProvider {

	public List<ICompletionItem> provideFor(final IRuleItem item, final int desiredDepth) {
		if (item instanceof ITerminal) {
			final ITerminal terminal = (ITerminal) item;
			if (terminal.isPattern()) {
				return Arrays.asList(new CompletionItemPattern(terminal.getOwningRule().getName(), ((TerminalPattern) terminal).getPattern()));
			} else {
				return Arrays.asList(new CompletionItemText(((ITerminal) item).getValue()));
			}
		} else {
			try {
				final SampleGeneratorVisitor v = new SampleGeneratorVisitor(desiredDepth);
				final Set<ICompletionItem> options = ((Visitable) item).accept(v, 0);
				return new ArrayList<>(options);
			} catch (final Throwable t) {
				t.printStackTrace();
				return Collections.emptyList();
			}
		}
	}

}
