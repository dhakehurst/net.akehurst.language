package net.akehurst.language.core.analyser;

public interface IRuleItem {
	public IRule getOwningRule();

	public IRuleItem getSubItem(int i);
}
