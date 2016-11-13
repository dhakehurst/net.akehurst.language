package net.akehurst.language.parse.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.akehurst.language.core.parser.INodeIdentity;
import net.akehurst.language.grammar.parse.tree.Leaf;
import net.akehurst.language.grammar.parser.forrest.Input2;
import net.akehurst.language.grammar.parser.forrest.Input3;
import net.akehurst.language.grammar.parser.forrest.NodeIdentifier;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSetBuilder;

public class ParseGraph implements IParseGraph {

	public ParseGraph(RuntimeRuleSetBuilder runtimeFactory, CharSequence text) {
		this.runtimeBuilder = runtimeFactory;
		this.input = new Input3(runtimeFactory, text);
		this.nodes = new HashMap<>();
		this.growable = new ArrayList<>();
	}

	RuntimeRuleSetBuilder runtimeBuilder;
	Input3 input;
	Map<NodeIdentifier, IGraphNode> nodes;
	List<IGraphNode> growable;

	@Override
	public IGraphNode peek(NodeIdentifier identifier) {
		return this.nodes.get(identifier);
	}

	@Override
	public List<IGraphNode> getGrowable() {
		return this.growable;
	}

	@Override
	public IParseGraph shallowClone() {
		ParseGraph ng = new ParseGraph(this.runtimeBuilder, this.input.text);
		ng.nodes = this.nodes;
		ng.growable = this.growable;
		return ng;
	}

	@Override
	public IGraphNode createLeaf(RuntimeRule terminalRule, int position) {
		Leaf l = this.input.fetchOrCreateBud(terminalRule, position);
		if (null == l) {
			return null;
		} else {
			IGraphNode gn = new GraphNodeLeaf(l);
			this.growable.add(gn);
			return gn;
		}
	}

	@Override
	public IGraphNode createBranch(RuntimeRule rr, int priority, IGraphNode firstChild, int nextItemIndex) {
		IGraphNode gn = new GraphNodeBranch(this, rr, priority, firstChild, nextItemIndex);
		this.growable.add(gn);
		return gn;
	}

}
