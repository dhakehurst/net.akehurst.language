package net.akehurst.language.processor;

import java.util.Objects;
import java.util.regex.Pattern;

import net.akehurst.language.core.parser.CompletionItem;

public class CompletionItemPattern implements CompletionItem {

	public CompletionItemPattern(final String name, final Pattern pattern) {
		this.name = name;
		this.pattern = pattern;
	}

	private final String name;
	private final Pattern pattern;

	@Override
	public String getText() {
		return this.name;
	}

	// --- Object ---
	@Override
	public int hashCode() {
		return Objects.hash(this.name, this.pattern);
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof CompletionItemPattern) {
			final CompletionItemPattern other = (CompletionItemPattern) obj;
			return Objects.equals(this.name, other.name) && Objects.equals(this.pattern, other.pattern);
		}
		return false;
	}

	@Override
	public String toString() {
		return this.name + "(" + this.pattern + ")";
	}
}
