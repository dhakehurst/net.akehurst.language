package net.akehurst.language.parser.runtime;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.ParseTreeException;

abstract
public class Node implements INode {

	public Node(final RuntimeRule runtimeRule) {
		this.runtimeRule = runtimeRule;
	}
	
	RuntimeRule runtimeRule;
	public RuntimeRule getRuntimeRule() {
		return this.runtimeRule;
	}

	@Override
	public INodeType getNodeType() throws ParseTreeException {
		return this.runtimeRule.getRuntimeRuleSet().getNodeType(this.runtimeRule.getRuleNumber());
	}
	
	public boolean getIsSkip() {
		return this.runtimeRule.isSkipRule;
	}
	
	@Override
	public int getNumberOfLines() {
		String str = this.getMatchedText();
		Pattern p = Pattern.compile(System.lineSeparator());
	    Matcher m = p.matcher(str);
	    int count = 0;
	    while (m.find()){
	    	count +=1;
	    }
	    return count;
	}
}
