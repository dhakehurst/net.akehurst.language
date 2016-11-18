package net.akehurst.language.parse.graph;

import java.util.ArrayList;
import java.util.List;

import net.akehurst.language.grammar.parser.runtime.RuntimeRule;

public class GraphNodeRoot {

	public GraphNodeRoot(RuntimeRule goalRule, ArrayList<IGraphNode> children) {
		this.goalRule = goalRule;
		this.children = children;
	}
	RuntimeRule goalRule;
	List<IGraphNode> children;
	
	
}
