package net.akehurst.language.parse.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.akehurst.language.grammar.parse.tree.Leaf;
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
//		this.growable = new HashSet<>();
	}

	RuntimeRuleSetBuilder runtimeBuilder;
	Input3 input;
	Map<NodeIdentifier, IGraphNode> nodes;
	@Override
	public List<IGraphNode> getNodes() {
		return new ArrayList<>(nodes.values());
	}
	
	Collection<IGraphNode> growable;

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
	public Collection<IGraphNode> getGrowable() {
		return this.growable;
	}

	@Override
	public void addGrowable(IGraphNode value) {
		if (this.getGrowable().contains(value)) {
//			System.out.println("merged "+value);
		} else {
			this.getGrowable().add(value);
		}
	}
	
	@Override
	public IParseGraph shallowClone() {
		ParseGraph ng = new ParseGraph(this.runtimeBuilder, this.input.text);
		ng.nodes = this.nodes;
		ng.growable = this.growable;
		return ng;
	}

//	@Override
//	public List<IGraphNode> getChildren(IGraphNode parent) {
//		Vector<IGraphNode> children = new Vector<>();
//		for(IGraphNode pc: this.nodes.values()) {
//			for(Map.Entry<ParentsIndex, IGraphNode> pp: pc.getParents().entrySet()) {
//				if (pp.getValue().equals(parent)) {
//					int i = pp.getKey().childIndex;
//					if (i >= children.size()) {
//						children.setSize(i+1);
//					}
//					children.set(i,  pc);
//				}
//			}
//		}
//		return children;
//	}
	
	@Override
	public IGraphNode createLeaf(IGraphNode parent, RuntimeRule terminalRule, int position) {
		Leaf l = this.input.fetchOrCreateBud(terminalRule, position);
		if (null == l) {
			return null;
		} else {
			NodeIdentifier id = new NodeIdentifier(terminalRule.getRuleNumber(), l.getStart(), l.getMatchedTextLength());//, l.getEnd(), -1);
			IGraphNode gn = this.nodes.get(id);
			if (null == gn) {
				gn = new GraphNodeLeaf(this,parent,l);
//				this.nodes.put(id, gn);
//				this.growable.add(gn);
				return gn;
			} else {
				return gn;
			}
		}
	}

	@Override
	public IGraphNode createBranch(IGraphNode parent, RuntimeRule rr, int priority, int startPosition, int length, int nextItemIndex) {
		//NodeIdentifier id = new NodeIdentifier(rr.getRuleNumber(), firstChild.getStartPosition());//, firstChild.getEndPosition(), nextItemIndex);
		IGraphNode gn = null;//this.nodes.get(id);
		if (null == gn) {
			gn = new GraphNodeBranch(this, parent,rr, priority, startPosition, length, nextItemIndex);
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
