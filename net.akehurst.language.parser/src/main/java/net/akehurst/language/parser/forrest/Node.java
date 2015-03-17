package net.akehurst.language.parser.forrest;

import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.parser.runtime.Factory;
import net.akehurst.language.parser.runtime.RuntimeRule;

abstract
public class Node implements INode {

	public Node(Factory factory, RuntimeRule runtimeRule) {
		this.factory = factory;
		this.runtimeRule = runtimeRule;
	}
	
	Factory factory;
	
	RuntimeRule runtimeRule;
	public RuntimeRule getRuntimeRule() {
		return this.runtimeRule;
	}

	@Override
	public INodeType getNodeType() throws ParseTreeException {
		return this.runtimeRule.getRuntimeRuleSet().getNodeType(this.runtimeRule.getRuleNumber());
	}
}
