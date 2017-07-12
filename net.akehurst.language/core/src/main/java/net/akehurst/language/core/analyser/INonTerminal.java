package net.akehurst.language.core.analyser;

import net.akehurst.language.core.parser.RuleNotFoundException;

public interface INonTerminal extends ITangibleItem {

	IRule getReferencedRule() throws RuleNotFoundException;

}
