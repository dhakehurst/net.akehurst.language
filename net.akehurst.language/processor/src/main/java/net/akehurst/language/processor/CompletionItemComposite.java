package net.akehurst.language.processor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.akehurst.language.core.ICompletionItem;

public class CompletionItemComposite implements ICompletionItem {
	public CompletionItemComposite() {
		this.content = new ArrayList<>();
	}

	private final List<ICompletionItem> content;

	List<ICompletionItem> getContent() {
		return this.content;
	}

	@Override
	public String getText() {
		final StringBuilder b = new StringBuilder();
		for (final ICompletionItem item : this.getContent()) {
			b.append(item.getText());
		}
		return b.toString();
	}

	// --- Object ---
	@Override
	public int hashCode() {
		return Objects.hash(this.content);
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof CompletionItemComposite) {
			final CompletionItemComposite other = (CompletionItemComposite) obj;
			return Objects.equals(this.content, other.content);
		}
		return false;
	}

	@Override
	public String toString() {
		final StringBuilder b = new StringBuilder();
		for (final ICompletionItem item : this.getContent()) {
			b.append(item.toString());
		}
		return b.toString();
	}
}
