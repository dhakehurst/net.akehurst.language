package net.akehurst.language.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.akehurst.language.api.grammar.RuleItem;
import net.akehurst.language.api.grammar.Terminal;
import net.akehurst.language.api.processor.CompletionItem;
import net.akehurst.language.ogl.semanticStructure.GramarVisitable;
import net.akehurst.language.ogl.semanticStructure.TerminalPatternDefault;

public class CompletionProvider {

    public List<CompletionItem> provideFor(final RuleItem item, final long desiredDepth) {
        if (item instanceof Terminal) {
            final Terminal terminal = (Terminal) item;
            if (terminal.isPattern()) {
                return Arrays.asList(new CompletionItemPattern(terminal.getOwningRule().getName(), ((TerminalPatternDefault) terminal).getPattern()));
            } else {
                return Arrays.asList(new CompletionItemText(((Terminal) item).getValue()));
            }
        } else {
            try {
                final SampleGeneratorVisitor v = new SampleGeneratorVisitor(desiredDepth);
                final Set<CompletionItem> options = ((GramarVisitable) item).accept(v, 0);
                return new ArrayList<>(options);
            } catch (final Throwable t) {
                t.printStackTrace();
                return Collections.emptyList();
            }
        }
    }

}
