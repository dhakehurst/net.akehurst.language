package net.akehurst.language.processor;

import java.util.Objects;

import net.akehurst.language.api.processor.CompletionItem;

public class CompletionItemText implements CompletionItem {
	public CompletionItemText(final String text) {
		this.text = text;
	}

	private final String text;

	public String getText() {
		return this.text;
	}

	// --- Object ---
	@Override
	public int hashCode() {
		return this.text.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof CompletionItemText) {
			final CompletionItemText other = (CompletionItemText) obj;
			return Objects.equals(this.text, other.text);
		}
		return false;
	}

	@Override
	public String toString() {
		return this.text;
	}
}
