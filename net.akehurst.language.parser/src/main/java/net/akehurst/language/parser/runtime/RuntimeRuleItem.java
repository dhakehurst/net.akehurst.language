package net.akehurst.language.parser.runtime;


public class RuntimeRuleItem {
	
	public RuntimeRuleItem(RuntimeRuleItemKind kind) {
		this.kind = kind;
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
