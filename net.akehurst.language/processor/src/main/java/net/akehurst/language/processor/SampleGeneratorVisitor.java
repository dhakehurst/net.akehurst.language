package net.akehurst.language.processor;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import net.akehurst.language.api.grammar.NonTerminal;
import net.akehurst.language.api.grammar.RuleItem;
import net.akehurst.language.api.processor.CompletionItem;
import net.akehurst.language.ogl.semanticStructure.ChoicePriorityDefault;
import net.akehurst.language.ogl.semanticStructure.ChoiceSimpleDefault;
import net.akehurst.language.ogl.semanticStructure.ConcatenationDefault;
import net.akehurst.language.ogl.semanticStructure.ConcatenationItemAbstract;
import net.akehurst.language.ogl.semanticStructure.GroupDefault;
import net.akehurst.language.ogl.semanticStructure.MultiDefault;
import net.akehurst.language.ogl.semanticStructure.SeparatedListDefault;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteralDefault;
import net.akehurst.language.ogl.semanticStructure.TerminalPatternDefault;
import net.akehurst.language.ogl.semanticStructure.GramarVisitable;
import net.akehurst.language.ogl.semanticStructure.GrammarVisitor;

public class SampleGeneratorVisitor implements GrammarVisitor<Set<CompletionItem>, Throwable> {

	public SampleGeneratorVisitor(final int desiredDepth) {
		this.desiredDepth = desiredDepth;
		this.cache = new HashMap<>();
	}

	private final int desiredDepth;
	private final Map<RuleItem, Set<CompletionItem>> cache;

	int getArg(final Object[] arg) {
		if (arg[0] instanceof Integer) {
			final Integer cur = (Integer) arg[0];
			return cur;
		} else {
			throw new RuntimeException("visitor argument must be an Integer indicating current depth");
		}
	}

	boolean reachedDepth(final Object[] arg) {
		final int cur = this.getArg(arg);
		return cur >= this.desiredDepth;
	}

	@Override
	public Set<CompletionItem> visit(final ChoiceSimpleDefault target, final Object... arg) throws Throwable {
		Set<CompletionItem> result = this.cache.get(target);
		if (null == result) {
			result = new LinkedHashSet<>();
			for (final ConcatenationDefault item : target.getAlternative()) {
				final Set<CompletionItem> options = item.accept(this, arg);
				for (final CompletionItem option : options) {
					final CompletionItemComposite composite = new CompletionItemComposite();
					composite.getContent().add(option);

					result.add(composite);
				}
			}
			this.cache.put(target, result);
		}
		return result;
	}

	@Override
	public Set<CompletionItem> visit(final ChoicePriorityDefault target, final Object... arg) throws Throwable {
		Set<CompletionItem> result = this.cache.get(target);
		if (null == result) {
			result = new LinkedHashSet<>();
			for (final ConcatenationDefault item : target.getAlternative()) {
				final Set<CompletionItem> options = item.accept(this, arg);
				for (final CompletionItem option : options) {
					final CompletionItemComposite composite = new CompletionItemComposite();

					composite.getContent().add(option);

					result.add(composite);
				}
			}
			this.cache.put(target, result);
		}
		return result;
	}

	@Override
	public Set<CompletionItem> visit(final ConcatenationDefault target, final Object... arg) throws Throwable {
		Set<CompletionItemComposite> result = (Set<CompletionItemComposite>) (Object) this.cache.get(target);
		if (null == result) {
			result = new LinkedHashSet<>();

			for (final ConcatenationItemAbstract item : target.getItem()) {
				final Set<CompletionItem> options = item.accept(this, arg);
				if (result.isEmpty()) {
					for (final CompletionItem ci : options) {
						final CompletionItemComposite composite = new CompletionItemComposite();
						composite.getContent().add(ci);
						result.add(composite);
					}
				} else {
					final Set<CompletionItemComposite> result2 = new LinkedHashSet<>();
					for (final CompletionItemComposite cp : result) {
						if (options.isEmpty()) {
							result2.addAll(result);
						} else {
							for (final CompletionItem ci : options) {
								final CompletionItemComposite composite = new CompletionItemComposite();
								composite.getContent().addAll(cp.getContent());
								composite.getContent().add(ci);
								result2.add(composite);
							}
						}
					}
					result = result2;
				}
			}

			this.cache.put(target, (Set<CompletionItem>) (Object) result);
		}
		return (Set<CompletionItem>) (Object) result;

	}

	@Override
	public Set<CompletionItem> visit(final NonTerminal target, final Object... arg) throws Throwable {
		Set<CompletionItem> result = this.cache.get(target);
		if (null == result) {

			// if (arg[0] instanceof Set<?>) {
			// final Set<IRule> rules = (Set<IRule>) arg[0];
			// if (rules.contains(target.getReferencedRule())) {
			// // already visited this rule, don't do it again
			// result = Collections.emptySet();
			// } else {
			// final Set<IRule> visited = new LinkedHashSet<>();
			// visited.addAll(rules);
			// visited.add(target.getReferencedRule());
			// result = ((Visitable) target.getReferencedRule().getRhs()).accept(this, visited);
			// }
			// } else {
			// throw new RuntimeException("visitor argument must be a Set of IRule");
			// }
			final int cur = this.getArg(arg);
			final int next = cur + 1;
			result = ((GramarVisitable) target.getReferencedRule().getRhs()).accept(this, next);
			this.cache.put(target, result);
		}
		return result;

	}

	@Override
	public Set<CompletionItem> visit(final MultiDefault target, final Object... arg) throws Throwable {
		Set<CompletionItem> result = this.cache.get(target);
		if (null == result) {
			result = new LinkedHashSet<>();
			if (0 == target.getMin()) {
				// option for nothing
				result.add(new CompletionItemComposite());
			}
			if (!this.reachedDepth(arg)) {
				if (-1 == target.getMax() || 0 < target.getMax()) {
					// if max > 0 (weird if not) add min or 1 if min==0
					final Set<CompletionItem> options = target.getItem().accept(this, arg);
					for (final CompletionItem option : options) {
						final CompletionItemComposite composite = new CompletionItemComposite();
						for (int i = 0; i < Math.max(1, target.getMin()); ++i) {
							composite.getContent().add(option);
						}
						result.add(composite);
					}

				}
			}
			this.cache.put(target, result);
		}
		return result;
	}

	@Override
	public Set<CompletionItem> visit(final SeparatedListDefault target, final Object... arg) throws Throwable {
		Set<CompletionItem> result = this.cache.get(target);
		if (null == result) {
			result = new LinkedHashSet<>();
			if (0 == target.getMin()) {
				// option for nothing
				result.add(new CompletionItemComposite());
			}
			if (-1 == target.getMax() || 0 < target.getMax()) {
				if (!this.reachedDepth(arg)) {
					// if max > 0 (weird if not) add min or 2 if min==0
					final Set<CompletionItem> options = target.getItem().accept(this, arg);
					final Set<CompletionItem> sep = target.getSeparator().accept(this, arg);
					// add option for 1 item
					for (final CompletionItem option : options) {
						final CompletionItemComposite composite = new CompletionItemComposite();
						for (int i = 0; i < Math.max(1, target.getMin()); ++i) {
							composite.getContent().add(option);
							if (i < Math.max(1, target.getMin()) - 1) {
								composite.getContent().addAll(sep);
							}
						}
						result.add(composite);
					}
					// add option for 2 items
					if (-1 == target.getMax() || 1 < target.getMax()) {
						for (final CompletionItem option : options) {
							final CompletionItemComposite composite = new CompletionItemComposite();
							for (int i = 0; i < Math.max(0, target.getMin()); ++i) {
								composite.getContent().add(option);
								if (i < Math.max(0, target.getMin()) - 1) {
									composite.getContent().addAll(sep);
								}
							}
							result.add(composite);
						}
					}
				}
			}
			this.cache.put(target, result);
		}
		return result;
	}

	@Override
	public Set<CompletionItem> visit(final GroupDefault target, final Object... arg) throws Throwable {
		Set<CompletionItem> result = this.cache.get(target);
		if (null == result) {
			result = target.getChoice().accept(this, arg);
			this.cache.put(target, result);
		}
		return result;
	}

	@Override
	public Set<CompletionItem> visit(final TerminalPatternDefault target, final Object... arg) throws Throwable {
		Set<CompletionItem> result = this.cache.get(target);
		// if (null == result) {
		result = new LinkedHashSet<>();
		result.add(new CompletionItemPattern(target.getOwningRule().getName(), target.getPattern()));
		// this.cache.put(target, result);
		// }
		return result;
	}

	@Override
	public Set<CompletionItem> visit(final TerminalLiteralDefault target, final Object... arg) throws Throwable {
		Set<CompletionItem> result = this.cache.get(target);
		// if (null == result) {
		result = new LinkedHashSet<>();
		result.add(new CompletionItemText(target.getValue()));
		// this.cache.put(target, result);
		// }
		return result;
	}

}
