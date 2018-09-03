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

import java.util.ArrayList;
import java.util.Arrays;

public class RuntimeRuleItem {

	private final RuntimeRuleItemKind kind;
	private RuntimeRule[] items;
	private final RuntimeRule[][] itemsByType;
	private int multiMin;
	private int multiMax;

	public RuntimeRuleItem(final RuntimeRuleItemKind kind, final int maxRuleNumber) {
		this.kind = kind;
		this.itemsByType = new RuntimeRule[maxRuleNumber][]; //FIXME: very bad for memory! do this properly
	}

	public RuntimeRuleItemKind getKind() {
		return this.kind;
	}

	public RuntimeRule[] getItems() {
		return this.items;
	}

	public void setItems(final RuntimeRule[] value) {
		this.items = value;
	}

	public RuntimeRule[] getItems(final int ruleNumber) {
		RuntimeRule[] res = this.itemsByType[ruleNumber];
		if (null == res) {
			final ArrayList<RuntimeRule> rrs = new ArrayList<>();
			for (final RuntimeRule r : this.getItems()) {
				if (ruleNumber == r.getRuleNumber()) {
					rrs.add(r);
				}
			}
			this.itemsByType[ruleNumber] = rrs.toArray(new RuntimeRule[rrs.size()]);
			res = this.itemsByType[ruleNumber];
		}
		return res;
	}

	public RuntimeRule[] getItemAt(final int n) {
		final ArrayList<RuntimeRule> result = new ArrayList<>();
		switch (this.getKind()) {
			case EMPTY:
			break;
			case PRIORITY_CHOICE: {
				if (0 == n) {
					result.addAll(Arrays.asList(this.getItems()));
				} else {
					// do nothing
					// throw new UnsupportedOperationException("this is not implemented yet!");
				}
			}
			break;
			case CHOICE: {
				if (0 == n) {
					result.addAll(Arrays.asList(this.getItems()));
				} else {
					// do nothing
					// throw new UnsupportedOperationException("Internal Error");
				}
			}
			break;
			case CONCATENATION: {
				if (this.getItems().length > n) {
					result.add(this.getItems()[n]);
				}
			}
			break;
			case MULTI: {
				if ((this.multiMax == -1 || n <= this.multiMax - 1) && n >= this.multiMin - 1) {
					result.add(this.getItems()[0]);
				}
			}
			break;
			case SEPARATED_LIST: {
				final int i = n % 2;
				result.add(this.getItems()[i]);
				if ((this.multiMax == -1 || n <= this.multiMax - 1) && n >= this.multiMin - 1) {
					result.add(this.getItems()[1]);
				}
			}
			break;
			default:
			break;

		}
		return result.toArray(new RuntimeRule[result.size()]);
	}

	/**
	 * if rule kind is a MULTI
	 *
	 * @return
	 */
	public int getMultiMin() {
		return this.multiMin;
	}

	public void setMultiMin(final int value) {
		this.multiMin = value;
	}

	/**
	 * if rule kind is a MULTI
	 *
	 * @return
	 */
	public int getMultiMax() {
		return this.multiMax;
	}

	public void setMultiMax(final int value) {
		this.multiMax = value;
	}

	@Override
	public String toString() {
		String s = "";
		for (final RuntimeRule r : this.getItems()) {
			s += r.getRuleNumber() + " (" + r.getName() + ") ";
		}
		return s;
	}
}
