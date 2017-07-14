package net.akehurst.language.processor;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.akehurst.language.core.parser.ICompletionItem;

public class CompletionItemComposite implements ICompletionItem {
	public CompletionItemComposite() {
		this.content = new ArrayList<>();
	}

	private final List<ICompletionItem> content;

	public List<ICompletionItem> getContent() {
		final List<ICompletionItem> base = this.content;
		return new AbstractList<ICompletionItem>() {

			@Override
			public ICompletionItem get(final int index) {
				return base.get(index);
			}

			@Override
			public int size() {
				return base.size();
			}

			@Override
			public boolean add(final ICompletionItem e) {
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
			public void add(final int index, final ICompletionItem e) {
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
