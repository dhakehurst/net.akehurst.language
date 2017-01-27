package net.akehurst.language.core.analyser;

import java.util.Set;

import net.akehurst.language.core.parser.INodeType;

public interface IGrammar {

	String getName();

	Set<INodeType> findAllNodeType();

	Set<ITerminal> getAllTerminal();
}
