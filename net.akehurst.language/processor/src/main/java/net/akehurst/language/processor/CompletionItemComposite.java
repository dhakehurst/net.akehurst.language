package net.akehurst.language.processor;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.akehurst.language.core.parser.CompletionItem;

public class CompletionItemComposite implements CompletionItem {
	public CompletionItemComposite() {
		this.content = new ArrayList<>();
	}

	private final List<CompletionItem> content;

	public List<CompletionItem> getContent() {
		final List<CompletionItem> base = this.content;
		return new AbstractList<CompletionItem>() {

			@Override
			public CompletionItem get(final int index) {
				return base.get(index);
			}

			@Override
			public int size() {
				return base.size();
			}

			@Override
			public boolean add(final CompletionItem e) {
				if (e.getText().isEmpty()) {
					return false;
				} else {
					if (e instanceof CompletionItemComposite) {
						final CompletionItemComposite cp = (CompletionItemComposite) e;
						return super.addAll(cp.getContent());
					} else {
						return super.add(e);
					}
				}
			}

			@Override
			public void add(final int index, final CompletionItem e) {
				if (e.getText().isEmpty()) {
					// do not add
				} else {
					if (e instanceof CompletionItemComposite) {
						final CompletionItemComposite cp = (CompletionItemComposite) e;
						// FIXME !
						super.addAll(cp.getContent());
					} else {
						base.add(index, e);
					}

				}
			}

		};
	}

	@Override
	public String getText() {
		final StringBuilder b = new StringBuilder();
		for (final CompletionItem item : this.getContent()) {
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
		for (final CompletionItem item : this.getContent()) {
			b.append(item.toString());
		}
		return b.toString();
	}
}
