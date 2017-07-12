package net.akehurst.language.core.analyser;

import java.util.Set;

import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.RuleNotFoundException;

public interface IGrammar {

	String getName();

	Set<INodeType> findAllNodeType();

	Set<ITerminal> getAllTerminal();

	IRule findAllRule(String name) throws RuleNotFoundException;

	ITerminal findAllTerminal(final String terminalPattern);
}
