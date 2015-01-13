package net.akehurst.language.ogl.semanticModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.akehurst.language.core.lexicalAnalyser.ITokenType;

public class Grammar {

	public Grammar(Namespace namespace, String name) {
		this.namespace = namespace;
		this.name = name;
		this.extends_ = new ArrayList<>();
		this.rule = new ArrayList<>();
	}
	
	Namespace namespace;
	public Namespace getNamespace() {
		return this.namespace;
	}
	
	String name;
	public String getName() {
		return this.name;
	}
	
	List<Grammar> extends_;
	public List<Grammar> getExtends() {
		return this.extends_;
	}
	public void setExtends(List<Grammar> value) {
		this.extends_ = value;
	}
	
	List<Rule> rule;
	public List<Rule> getRule() {
		return this.rule;
	}
	public void setRule(List<Rule> value) {
		this.rule = value;
	}
	
	public Rule findRule(String ruleName) throws RuleNotFoundException {
		for(Rule r : this.getRule()) {
			if (r.getName().equals(ruleName)) {
				return r;
			}
		}
		throw new RuleNotFoundException(ruleName + " in Grammar("+this.getName()+").findRule");
	}
	
	Set<Terminal> allTerminal;
	public Set<Terminal> getAllTerminal() {
		if (null==this.allTerminal) {
			this.allTerminal = this.findAllTerminal();
		}
		return this.allTerminal;
	}
	
	Set<Terminal> findAllTerminal() {
		Set<Terminal> result = new HashSet<>();
		for (Rule rule : this.getRule()) {
			RuleItem ri = rule.getRhs();
			result.addAll(this.findAllTerminal(0, rule, ri ) );
		}
		return result;
	}
	
	Set<Terminal> findAllTerminal(final int totalItems, final Rule rule, RuleItem item) {
		Set<Terminal> result = new HashSet<>();
		if (item instanceof Terminal) {
			Terminal t = (Terminal) item;
			result.add(t);
		} else if (item instanceof Multi) {
			result.addAll( this.findAllTerminal( totalItems, rule, ((Multi)item).getItem() ) );
		} else if (item instanceof Choice) {
			for(TangibleItem ti : ((Choice)item).getAlternative()) {
				result.addAll( this.findAllTerminal( totalItems, rule, ti ) );
			}
		} else if (item instanceof Concatination) {
			for(TangibleItem ti : ((Concatination)item).getItem()) {
				result.addAll( this.findAllTerminal( totalItems, rule, ti ) );
			}
		} else if (item instanceof SeparatedList) {
			result.addAll(this.findAllTerminal(totalItems, rule,((SeparatedList)item).getSeparator()));
			result.addAll( this.findAllTerminal(totalItems, rule, ((SeparatedList)item).getConcatination() ) );
		}
		return result;
	}

	public List<ITokenType> findTokenTypes() {
		List<ITokenType> result = new ArrayList<ITokenType>();
		for(Terminal t: this.getAllTerminal()){
			String pattern = t.getValue();
			String identity = t.getOwningRule().getName();
			TokenType tt = new TokenType(identity, pattern, (t instanceof TerminalPattern));
			if (!result.contains(tt)) {
				result.add(tt);
			}			
		}
		return result;
	}
	
	//--- Object ---
	@Override
	public String toString() {
		String r = this.getNamespace() + System.lineSeparator();
		r += "grammar "+this.getName() + "{" + System.lineSeparator();
		for(Rule i : this.getRule()) {
			r += i.toString() + System.lineSeparator();
		}
		r+="}";
		return r;
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof Grammar) {
			Grammar other = (Grammar)arg;
			return this.toString().equals(other.toString());
		} else {
			return false;
		}
	}

}
