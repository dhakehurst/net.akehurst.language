package net.akehurst.language.parse.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.IParseTreeVisitor;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;

abstract public class AbstractGraphNode implements IGraphNode {

	public AbstractGraphNode(ParseGraph graph, RuntimeRule runtimeRule, int startPosition, int matchedTextLength) {
		this.graph = graph;
		this.runtimeRule = runtimeRule;
		this.startPosition = startPosition;
		this.matchedTextLength = matchedTextLength;
		this.previous = new ArrayList<>();
		this.stackHash = 0;
	}

	protected ParseGraph graph;
	protected RuntimeRule runtimeRule;
	protected int startPosition;
	protected int matchedTextLength;
	private List<PreviousInfo> previous;
	int stackHash;
	
	@Override
	public RuntimeRule getRuntimeRule() {
		return this.runtimeRule;
	}

	public int getRuntimeRuleNumber() {
		return this.getRuntimeRule().getRuleNumber();
	}
	
	public String getName() {
		return this.getRuntimeRule().getName();
	}
	
	@Override
	public int getStartPosition() {
		return this.startPosition;
	}

	@Override
	public int getMatchedTextLength() {
		return this.matchedTextLength;
	}

	@Override
	public int getNextInputPosition() {
		return this.startPosition + this.matchedTextLength;
	}

	@Override
	public int getStackHash() {
//		//TODO: pre-cache this value when stack changes
//		if (0==this.stackHash && !this.getPrevious().isEmpty()) {
//			for(PreviousInfo prev: this.getPrevious()) {
//				this.stackHash = Objects.hash(prev.node.getRuntimeRule().getRuleNumber(), prev.node.getStackHash());
//			}
//		}
		return this.stackHash;
	}
	
	@Override
	public List<PreviousInfo> getPrevious() {
		//TODO: don't think this needs ot be a list
		return this.previous;
	}
	
	@Override
	public void addPrevious(IGraphNode prev, int atPosition) {
		PreviousInfo info = new PreviousInfo(prev, atPosition);
		this.previous.add(info);
		this.stackHash = Objects.hash(this.stackHash, prev.getRuntimeRule().getRuleNumber(), prev.getStackHash());
	}

	@Override
	public IGraphNode pushToStackOf(IGraphNode next, int atPosition) {
		next.addPrevious(this, atPosition);
		this.graph.tryAddGrowable(next);
		return next;
	}

	
	IBranch parent;
	public IBranch getParent() {
		return parent;
	}
	public void setParent(IBranch value) {
		this.parent = value;
	}
	
	public abstract String getMatchedText();
	
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
	
	@Override
	public int hashCode() {
//		throw new RuntimeException("GraphNodes are not comparible");
		return Objects.hash(this.getRuntimeRule().getRuleNumber(), this.getStartPosition(), this.getMatchedTextLength());
	}

	@Override
	public boolean equals(Object obj) {
//		throw new RuntimeException("GraphNodes are not comparible");
		if (obj instanceof IGraphNode) {
			IGraphNode other = (IGraphNode)obj;
			return this.getRuntimeRule().getRuleNumber() == other.getRuntimeRule().getRuleNumber()
					&& this.getStartPosition() == other.getStartPosition()
					&& this.getMatchedTextLength() == other.getMatchedTextLength()
					;
		}else{
			return false;
		}
	}


}
