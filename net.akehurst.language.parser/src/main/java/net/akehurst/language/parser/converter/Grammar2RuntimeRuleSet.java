package net.akehurst.language.parser.converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import net.akehurst.language.ogl.semanticModel.Grammar;
import net.akehurst.language.ogl.semanticModel.Rule;
import net.akehurst.language.ogl.semanticModel.Terminal;
import net.akehurst.language.parser.runtime.RuntimeRule;
import net.akehurst.language.parser.runtime.RuntimeRuleSet;
import net.akehurst.transform.binary.Relation;
import net.akehurst.transform.binary.RelationNotFoundException;
import net.akehurst.transform.binary.Transformer;

public class Grammar2RuntimeRuleSet implements Relation<Grammar, RuntimeRuleSet> {

	@Override
	public boolean isValidForLeft2Right(Grammar arg0) {
		return true;
	}
	
	@Override
	public RuntimeRuleSet constructLeft2Right(Grammar left, Transformer transformer) {
		Converter converter = (Converter)transformer;
		RuntimeRuleSet right = converter.getFactory().createRuntimeRuleSet();
		return right;
	}
	
	@Override
	public void configureLeft2Right(Grammar left, RuntimeRuleSet right, Transformer transformer) {
		List<Rule> rules = left.getAllRule();
		List<Terminal> terminals = Arrays.asList(left.getAllTerminal().toArray(new Terminal[0]));
		
		try {

			List<? extends RuntimeRule> runtimeRules = transformer.transformAllLeft2Right(Rule2RuntimeRule.class, rules);
			List<? extends RuntimeRule> runtimeRules2 = transformer.transformAllLeft2Right(Terminal2RuntimeRule.class, terminals);

			List<RuntimeRule> rr = new ArrayList<>();
			rr.addAll(runtimeRules);
			rr.addAll(runtimeRules2);
			
			right.setRuntimeRules(rr);
			
		} catch (RelationNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void configureRight2Left(Grammar arg0, RuntimeRuleSet arg1, Transformer arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Grammar constructRight2Left(RuntimeRuleSet arg0, Transformer arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isValidForRight2Left(RuntimeRuleSet arg0) {
		// TODO Auto-generated method stub
		return false;
	}

}
