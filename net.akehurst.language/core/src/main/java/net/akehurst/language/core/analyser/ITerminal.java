package net.akehurst.language.core.analyser;

public interface ITerminal extends ITangibleItem {
	boolean isPattern();

	String getValue();
}
