package net.akehurst.language.parser.runtime;

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
}
