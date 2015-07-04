package net.akehurst.language.parser.runtime;

import java.util.ArrayList;
import java.util.Arrays;

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
		if (null == res) {
			ArrayList<RuntimeRule> rrs = new ArrayList<>();
			for (RuntimeRule r : this.getItems()) {
				if (ruleNumber == r.getRuleNumber()) {
					rrs.add(r);
				}
			}
			this.itemsByType[ruleNumber] = rrs.toArray(new RuntimeRule[rrs.size()]);
			res = this.itemsByType[ruleNumber];
		}
		return res;
	}

	public RuntimeRule[] getItemAt(int n) {
		ArrayList<RuntimeRule> result = new ArrayList<RuntimeRule>();
		switch(this.getKind())  {
			case CHOICE: {
//				for(RuntimeRule rr: this.getItems()) {
//					result.addAll( Arrays.asList(rr.getRhs().getItemAt(n)) );
//				}
				if (0==n) {
					result.addAll( Arrays.asList(this.getItems()) );
				} else {
					throw new UnsupportedOperationException("this is not implemented yet!");
				}
			} break;
			case CONCATENATION: {
				if (this.getItems().length > n) {
					result.add( this.getItems()[n] );
				}
			} break;
			case MULTI: {
				if ((this.multiMax==-1 || n <= (this.multiMax-1)) && n >= (this.multiMin-1) ) {
					result.add( this.getItems()[0] );
				}
			} break;
			case SEPARATED_LIST: {
				int i = n % 2;
				result.add( this.getItems()[i] );
//				if ((this.multiMax==-1 || n <= (this.multiMax-1)) && n >= (this.multiMin-1) ) {
//					result.add( this.getItems()[1] );
//				}
			} break;
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
	int multiMin;

	public int getMultiMin() {
		return this.multiMin;
	}

	public void setMultiMin(int value) {
		this.multiMin = value;
	}

	/**
	 * if rule kind is a MULTI
	 * 
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
