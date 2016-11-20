package net.akehurst.language.parse.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import net.akehurst.language.core.parser.INodeIdentity;
import net.akehurst.language.grammar.parse.tree.Leaf;
import net.akehurst.language.grammar.parser.forrest.Input2;
import net.akehurst.language.grammar.parser.forrest.Input3;
import net.akehurst.language.grammar.parser.forrest.NodeIdentifier;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSetBuilder;
import net.akehurst.language.parse.graph.AbstractGraphNode.ParentsIndex;

public class ParseGraph implements IParseGraph {

	public ParseGraph(RuntimeRuleSetBuilder runtimeFactory, CharSequence text) {
		this.runtimeBuilder = runtimeFactory;
		this.input = new Input3(runtimeFactory, text);
		this.nodes = new HashMap<>();
		this.growable = new HashSet<>();
	}

	RuntimeRuleSetBuilder runtimeBuilder;
	Input3 input;
	Map<NodeIdentifier, IGraphNode> nodes;
	Set<IGraphNode> growable;

	@Override
	public IGraphNode peek(NodeIdentifier identifier) {
		return this.nodes.get(identifier);
	}

	@Override
	public void registerCompleteNode(IGraphNode node) {
		NodeIdentifier id = new NodeIdentifier(node.getRuntimeRule().getRuleNumber(), node.getStartPosition(), node.getMatchedTextLength());
		this.nodes.put(id, node);
	}
	
	@Override
	public Set<IGraphNode> getGrowable() {
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
	public List<IGraphNode> getChildren(IGraphNode parent) {
		Vector<IGraphNode> children = new Vector<>();
		for(IGraphNode pc: this.nodes.values()) {
			for(Map.Entry<ParentsIndex, IGraphNode> pp: pc.getParents().entrySet()) {
				if (pp.getValue().equals(parent)) {
					int i = pp.getKey().childIndex;
					if (i >= children.size()) {
						children.setSize(i+1);
					}
					children.set(i,  pc);
				}
			}
		}
		return children;
	}
	
	@Override
	public IGraphNode createLeaf(RuntimeRule terminalRule, int position) {
		Leaf l = this.input.fetchOrCreateBud(terminalRule, position);
		if (null == l) {
			return null;
		} else {
			NodeIdentifier id = new NodeIdentifier(terminalRule.getRuleNumber(), l.getStart(), l.getMatchedTextLength());//, l.getEnd(), -1);
			IGraphNode gn = this.nodes.get(id);
			if (null == gn) {
				gn = new GraphNodeLeaf(this,l);
//				this.nodes.put(id, gn);
//				this.growable.add(gn);
				return gn;
			} else {
				return gn;
			}
		}
	}

	@Override
	public IGraphNode createBranch(RuntimeRule rr, int priority, int startPosition, int length, int nextItemIndex) {
		//NodeIdentifier id = new NodeIdentifier(rr.getRuleNumber(), firstChild.getStartPosition());//, firstChild.getEndPosition(), nextItemIndex);
		IGraphNode gn = null;//this.nodes.get(id);
		if (null == gn) {
			gn = new GraphNodeBranch(this, rr, priority, startPosition, length, nextItemIndex);
//			this.growable.add(gn);
			return gn;
		} else {
//			this.growable.add(gn);
			return gn;
		}
	}

	@Override
	public String toString() {
		return ""+this.growable.size() + "-" +Arrays.toString(this.growable.toArray());
	}
	
}
