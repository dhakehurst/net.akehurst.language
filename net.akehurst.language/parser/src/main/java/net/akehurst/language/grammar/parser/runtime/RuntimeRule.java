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

import java.util.HashSet;
import java.util.Set;

import net.akehurst.language.ogl.semanticStructure.Rule;
import net.akehurst.language.ogl.semanticStructure.Terminal;

public class RuntimeRule {

	RuntimeRule(RuntimeRuleSet runtimeRuleSet, String name, int ruleNumber, RuntimeRuleKind kind, int patternFlags) {
		this.runtimeRuleSet = runtimeRuleSet;
		this.name = name;
		this.ruleNumber = ruleNumber;
		this.kind = kind;
		this.patternFlags = patternFlags;
	}

	RuntimeRuleSet runtimeRuleSet;

	public RuntimeRuleSet getRuntimeRuleSet() {
		return this.runtimeRuleSet;
	}

	String name;

	public String getName() {
		return this.name;
	}

	int ruleNumber;

	public int getRuleNumber() {
		return this.ruleNumber;
	}

	RuntimeRuleKind kind;

	public RuntimeRuleKind getKind() {
		return this.kind;
	}

	int patternFlags;

	public int getPatternFlags() {
		return this.patternFlags;
	}

	boolean isEmptyRule;

	public boolean getIsEmptyRule() {
		return null != this.getRhs() && this.getRhs().getKind() == RuntimeRuleItemKind.EMPTY;
	}

	boolean isSkipRule;

	public boolean getIsSkipRule() {
		return this.isSkipRule;
	}

	public void setIsSkipRule(boolean value) {
		this.isSkipRule = value;
	}

	RuntimeRuleItem rhs;

	public void setRhs(RuntimeRuleItem value) {
		this.rhs = value;
	}

	public RuntimeRuleItem getRhs() {
		return rhs;
	}

	public RuntimeRule getRhsItem(int index) {
		return this.getRhs().getItems()[index];
	}

	public RuntimeRule getSeparator() {
		return this.getRhsItem(1);
	}

	public RuntimeRule getRuleThatIsEmpty() {
		return this.getRhsItem(0);
	}

	Set<RuntimeRule> findAllNonTerminal() {
		Set<RuntimeRule> result = new HashSet<>();
		if (RuntimeRuleKind.TERMINAL == this.getKind()) {
			return result;
		}
		for (RuntimeRule item : this.getRhs().getItems()) {
			if (RuntimeRuleKind.NON_TERMINAL == item.getKind()) {
				result.add(item);
			}
		}
		return result;
	}

	Set<RuntimeRule> findAllNonTerminalAt(int n) {
		Set<RuntimeRule> result = new HashSet<>();
		if (RuntimeRuleKind.TERMINAL == this.getKind()) {
			return result;
		}
		RuntimeRule[] items = this.getRhs().getItemAt(n);
		for (RuntimeRule item : items) {
			if (RuntimeRuleKind.NON_TERMINAL == item.getKind()) {
				result.add(item);
			}
		}
		return result;
	}

	Set<RuntimeRule> findAllTerminal() {
		Set<RuntimeRule> result = new HashSet<>();
		if (RuntimeRuleKind.TERMINAL == this.getKind()) {
			return result;
		}
		for (RuntimeRule item : this.getRhs().getItems()) {
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

	Set<RuntimeRule> findSubRules() {
		Set<RuntimeRule> result = this.findAllNonTerminal();
		Set<RuntimeRule> oldResult = new HashSet<>();
		while (!oldResult.containsAll(result)) {
			oldResult = new HashSet<>();
			oldResult.addAll(result);
			for (RuntimeRule nt : oldResult) {
				Set<RuntimeRule> newNts = nt.findAllNonTerminal();
				newNts.removeAll(result);
				result.addAll(newNts);
			}
		}
		return result;
	}

	Set<RuntimeRule> findSubRulesAt(int n) {
		Set<RuntimeRule> result = this.findAllNonTerminalAt(n);
		Set<RuntimeRule> oldResult = new HashSet<>();
		while (!oldResult.containsAll(result)) {
			oldResult = new HashSet<>();
			oldResult.addAll(result);
			for (RuntimeRule nt : oldResult) {
				Set<RuntimeRule> newNts = nt.findAllNonTerminalAt(n);
				newNts.removeAll(result);
				result.addAll(newNts);
			}
		}
		return result;
	}

	public Set<RuntimeRule> findAllPossibleTerminalAt(int n) {
		Set<RuntimeRule> result = new HashSet<>();
		switch (this.getRhs().getKind()) {
			case EMPTY:
			break;
			case CHOICE:
				return this.findTerminalAt(n);
			case PRIORITY_CHOICE:
				return this.findTerminalAt(n);
			case CONCATENATION:
				return this.findTerminalAt(n);
			case MULTI: {
				if (0 == this.getRhs().getMultiMin()) {
					Set<RuntimeRule> s = this.findAllPossibleTerminalAt(n + 1);
					result.addAll(s);
					// result.add(this.runtimeRuleSet.getEmptyRule());
				}
			}
			break;
			case SEPARATED_LIST: {
				if (0 == this.getRhs().getMultiMin()) {
					Set<RuntimeRule> s = this.findAllPossibleTerminalAt(n + 1);
					result.addAll(s);
					// result.add(this.runtimeRuleSet.getEmptyRule());
				}
			}
			break;
			default:
			break;
		}
		return result;
	}

	public Set<RuntimeRule> findTerminalAt(int n) {
		Set<RuntimeRule> result = new HashSet<>();
		if (RuntimeRuleKind.TERMINAL == this.getKind()) {
			result.add(this);
			return result;
		}
		RuntimeRule[] firstItems = this.getRhs().getItemAt(n);
		for (RuntimeRule item : firstItems) {
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
					if (0 == this.getRhs().getMultiMin()) {
						result.add(this.runtimeRuleSet.getEmptyRule(this));
					}
				} break;
				case SEPARATED_LIST: {
					if (0 == this.getRhs().getMultiMin()) {
						result.add(this.runtimeRuleSet.getEmptyRule(this));
					}
				} break;
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

	String toString_cache;

	@Override
	public String toString() {
		if (null == this.toString_cache) {
			if (RuntimeRuleKind.NON_TERMINAL == this.getKind()) {
				this.toString_cache = this.getRuleNumber() + "(" + this.getNodeTypeName() + ") : " + this.getRhs();
			} else {
				this.toString_cache = this.getTerminalPatternText();
			}
		}
		return this.toString_cache;
	}

	@Override
	public int hashCode() {
		return this.getRuleNumber();
	}

	@Override
	public boolean equals(Object arg) {
		if (!(arg instanceof RuntimeRule)) {
			return false;
		}
		RuntimeRule other = (RuntimeRule) arg;
		return this.getRuleNumber() == other.getRuleNumber();
	}

}
