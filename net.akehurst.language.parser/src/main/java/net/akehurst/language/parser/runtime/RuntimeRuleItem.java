package net.akehurst.language.parser.runtime;

import java.util.ArrayList;


public class RuntimeRuleItem {
	
	public RuntimeRuleItem(RuntimeRuleItemKind kind, int maxRuleNumber) {
		this.kind = kind;
		this.itemsByType = new RuntimeRule[maxRuleNumber][];
	}
	
	RuntimeRuleItemKind kind;
	public RuntimeRuleItemKind getKind() {
		return this.kind;
	}
	
	RuntimeRule[] items; 
	public RuntimeRule[] getItems() {
		return this.items;
	}
	public void setItems(RuntimeRule[] value) {
		this.items = value;
	}
	
	RuntimeRule[][] itemsByType; 
	public RuntimeRule[] getItems(int ruleNumber) {
		RuntimeRule[] res = this.itemsByType[ruleNumber];
		if (null==res) {
			ArrayList<RuntimeRule> rrs = new ArrayList<>();
			for(RuntimeRule r : this.getItems()) {
				if (ruleNumber == r.getRuleNumber()) {
					rrs.add(r);
				}
			}
			this.itemsByType[ruleNumber] = rrs.toArray(new RuntimeRule[rrs.size()]);
			res = this.itemsByType[ruleNumber];
		}
		return res;
	}
	
	/**
	 * if rule kind is a MULTI
	 * @return
	 */
	int multiMin;
	public int getMultiMin() {
		return this.multiMin;
	}
	public void setMultiMin(int value) {
		this.multiMin = value;
	}	
	
	/**
	 * if rule kind is a MULTI
	 * @return
	 */
	int multiMax;
	public int getMultiMax() {
		return this.multiMax;
	}
	public void setMultiMax(int value) {
		this.multiMax = value;
	}

}
