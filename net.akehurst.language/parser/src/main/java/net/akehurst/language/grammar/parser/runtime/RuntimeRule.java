/**
 * Copyright (C) 2015 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.akehurst.language.grammar.parser.runtime;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class RuntimeRule {

	private final RuntimeRuleSet runtimeRuleSet;
	private final String name;
	private final int ruleNumber;
	private final RuntimeRuleKind kind;
	private final int patternFlags;
	private boolean isSkipRule;
	private RuntimeRuleItem rhs;
	private String toString_cache;

	RuntimeRule(final RuntimeRuleSet runtimeRuleSet, final String name, final int ruleNumber, final RuntimeRuleKind kind, final int patternFlags) {
		this.runtimeRuleSet = runtimeRuleSet;
		this.name = name;
		this.ruleNumber = ruleNumber;
		this.kind = kind;
		this.patternFlags = patternFlags;
	}

	public RuntimeRuleSet getRuntimeRuleSet() {
		return this.runtimeRuleSet;
	}

	public String getName() {
		return this.name;
	}

	public int getRuleNumber() {
		return this.ruleNumber;
	}

	public RuntimeRuleKind getKind() {
		return this.kind;
	}

	public int getPatternFlags() {
		return this.patternFlags;
	}

	// boolean isEmptyRule;

	public boolean getIsEmptyRule() {
		return null != this.getRhs() && this.getRhs().getKind() == RuntimeRuleItemKind.EMPTY;
	}

	public boolean getIsSkipRule() {
		return this.isSkipRule;
	}

	public void setIsSkipRule(final boolean value) {
		this.isSkipRule = value;
	}

	public void setRhs(final RuntimeRuleItem value) {
		this.rhs = value;
	}

	public RuntimeRuleItem getRhs() {
		return this.rhs;
	}

	public RuntimeRule getRhsItem(final int index) {
		if (this.getKind() == RuntimeRuleKind.TERMINAL) {
			return null;
		} else {
			return this.getRhs().getItems()[index];
		}
	}

	public int getRhsIndexOf(final RuntimeRule rule) {
		if (this.getKind() == RuntimeRuleKind.TERMINAL) {
			return -1;
		} else {
			return Arrays.asList(this.getRhs().getItems()).indexOf(rule);
		}
	}

	public RuntimeRule getSeparator() {
		return this.getRhsItem(1);
	}

	public RuntimeRule getRuleThatIsEmpty() {
		if (this.getIsEmptyRule()) {
			return this.getRhs().getItems()[0];
		} else {
			return null;
		}
	}

	Set<RuntimeRule> findAllNonTerminal() {
		final Set<RuntimeRule> result = new HashSet<>();
		if (RuntimeRuleKind.TERMINAL == this.getKind()) {
			return result;
		}
		for (final RuntimeRule item : this.getRhs().getItems()) {
			if (RuntimeRuleKind.NON_TERMINAL == item.getKind()) {
				result.add(item);
			}
		}
		return result;
	}

	Set<RuntimeRule> findAllNonTerminalAt(final int n) {
		final Set<RuntimeRule> result = new HashSet<>();
		if (RuntimeRuleKind.TERMINAL == this.getKind()) {
			return result;
		}

		final RuntimeRule[] items = this.getRhs().getItemAt(n);
		for (final RuntimeRule item : items) {
			if (RuntimeRuleKind.NON_TERMINAL == item.getKind()) {
				result.add(item);
			}
		}
		return result;
	}

	Set<RuntimeRule> findAllTerminal() {
		final Set<RuntimeRule> result = new HashSet<>();
		if (RuntimeRuleKind.TERMINAL == this.getKind()) {
			return result;
		}
		for (final RuntimeRule item : this.getRhs().getItems()) {
			if (RuntimeRuleKind.TERMINAL == item.getKind()) {
				result.add(item);
			}
		}
		switch (this.getRhs().getKind()) {
			case EMPTY:
			break;
			case CHOICE:
			break;
			case PRIORITY_CHOICE:
			break;
			case CONCATENATION:
			break;
			case MULTI: {
				if (0 == this.getRhs().getMultiMin()) {
					result.add(this.runtimeRuleSet.getEmptyRule(this));
				}
			}
			break;
			case SEPARATED_LIST: {
				if (0 == this.getRhs().getMultiMin()) {
					result.add(this.runtimeRuleSet.getEmptyRule(this));
				}
			}
			break;
			default:
			break;

		}
		return result;
	}

	/**
	 * depending on the kind of rule, determine if the given possibleChild rule could be a child at the given position
	 *
	 * @param position
	 * @return
	 */
	public boolean couldHaveChild(final RuntimeRule possibleChild, final int atPosition) {
		if (this.getKind() == RuntimeRuleKind.TERMINAL) {
			return false;
		} else {
			if (possibleChild.getIsSkipRule()) {
				return true;
			} else {
				switch (this.getRhs().getKind()) {
					case EMPTY:
						return false;
					case CHOICE:
						// TODO: cache this
						return 0 == atPosition && Arrays.asList(this.getRhs().getItems()).contains(possibleChild);
					case PRIORITY_CHOICE:
						return 0 == atPosition && Arrays.asList(this.getRhs().getItems()).contains(possibleChild);
					case CONCATENATION:
						return -1 == atPosition || atPosition >= this.getRhs().getItems().length ? false
								: this.getRhsItem(atPosition).getRuleNumber() == possibleChild.getRuleNumber();
					case MULTI:
						return this.getRhsItem(0).getRuleNumber() == possibleChild.getRuleNumber() || this.getRhs().getMultiMin() == 0 && atPosition == 0
								&& possibleChild.getIsEmptyRule() && possibleChild.getRuleThatIsEmpty().getRuleNumber() == this.getRuleNumber();
					case SEPARATED_LIST: {
						if (possibleChild.getIsEmptyRule()) {
							return this.getRhs().getMultiMin() == 0 && possibleChild.getRuleThatIsEmpty().getRuleNumber() == this.getRuleNumber();
						} else {
							if (atPosition % 2 == 0) {
								return this.getRhsItem(0).getRuleNumber() == possibleChild.getRuleNumber();
							} else {
								return this.getSeparator().getRuleNumber() == possibleChild.getRuleNumber();
							}
						}
					}
					default:
						throw new RuntimeException("Internal Error: rule kind not recognised " + this.getRhs().getKind());
				}
			}
		}
	}

	Set<RuntimeRule> findSubRules() {
		final Set<RuntimeRule> result = this.findAllNonTerminal();
		Set<RuntimeRule> oldResult = new HashSet<>();
		while (!oldResult.containsAll(result)) {
			oldResult = new HashSet<>();
			oldResult.addAll(result);
			for (final RuntimeRule nt : oldResult) {
				final Set<RuntimeRule> newNts = nt.findAllNonTerminal();
				newNts.removeAll(result);
				result.addAll(newNts);
			}
		}
		return result;
	}

	Set<RuntimeRule> findSubRulesAt(final int n) {
		final Set<RuntimeRule> result = this.findAllNonTerminalAt(n);
		Set<RuntimeRule> oldResult = new HashSet<>();
		while (!oldResult.containsAll(result)) {
			oldResult = new HashSet<>();
			oldResult.addAll(result);
			for (final RuntimeRule nt : oldResult) {
				final Set<RuntimeRule> newNts = nt.findAllNonTerminalAt(n);
				newNts.removeAll(result);
				result.addAll(newNts);
			}
		}
		return result;
	}

	public int incrementNextItemIndex(final int currentIndex) {
		switch (this.getRhs().getKind()) {
			case EMPTY:
				return -1;

			case CHOICE:
				return -1;

			case PRIORITY_CHOICE:
				return -1;

			case CONCATENATION:
				return this.getRhs().getItems().length == currentIndex + 1 ? -1 : currentIndex + 1;

			case MULTI:
				return currentIndex;

			case SEPARATED_LIST:
				return currentIndex == 0 ? 1 : 0;

			default:
				throw new RuntimeException("Internal Error: Unknown RuleKind " + this.getRhs().getKind());
		}
	}

	public Set<RuntimeRule> getNextExpectedItems(final int nextItemIndex, final int currentNumChildren) {
		return this.getRuntimeRuleSet().getNextExpectedItems(this, nextItemIndex, currentNumChildren);
	}

	public Set<RuntimeRule> findTerminalAt(final int n) {
		final Set<RuntimeRule> result = new HashSet<>();
		if (RuntimeRuleKind.TERMINAL == this.getKind()) {
			result.add(this);
			return result;
		}
		final RuntimeRule[] firstItems = this.getRhs().getItemAt(n);
		for (final RuntimeRule item : firstItems) {
			if (RuntimeRuleKind.TERMINAL == item.getKind()) {
				result.add(item);
			}
		}
		if (0 == n) {
			switch (this.getRhs().getKind()) {
				case EMPTY:
				break;
				case PRIORITY_CHOICE:
				break;
				case CHOICE:
				break;
				case CONCATENATION:
				break;
				case MULTI: {
					if (0 == n && 0 == this.getRhs().getMultiMin()) {
						result.add(this.runtimeRuleSet.getEmptyRule(this));
					}
				}
				break;
				case SEPARATED_LIST: {
					if (0 == n && 0 == this.getRhs().getMultiMin()) {
						result.add(this.runtimeRuleSet.getEmptyRule(this));
					}
				}
				break;
				default:
				break;

			}
		}
		return result;
	}

	public String getNodeTypeName() {
		return this.getName();
	}

	public String getTerminalPatternText() {
		return this.getName();
	}

	@Override
	public String toString() {
		if (null == this.toString_cache) {
			if (RuntimeRuleKind.NON_TERMINAL == this.getKind()) {
				this.toString_cache = "[" + this.getRuleNumber() + "](" + this.getNodeTypeName() + ") : " + this.getRhs();
			} else {
				this.toString_cache = "[" + this.getRuleNumber() + "]" + this.getTerminalPatternText();
			}
		}
		return this.toString_cache;
	}

	@Override
	public int hashCode() {
		return this.getRuleNumber();
	}

	@Override
	public boolean equals(final Object arg) {
		if (!(arg instanceof RuntimeRule)) {
			return false;
		}
		final RuntimeRule other = (RuntimeRule) arg;
		return this.getRuleNumber() == other.getRuleNumber();
	}

}
