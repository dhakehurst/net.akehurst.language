package net.akehurst.language.parser.runtime;

import org.junit.Test;

import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSet;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSetBuilder;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteral;

public class RuntimeRule_Test {

	@Test
	public void create_terminal() {
		RuntimeRuleSetBuilder factory = new RuntimeRuleSetBuilder();
		
		RuntimeRuleSet runtimeRuleSet = factory.createRuntimeRuleSet(2);
		RuntimeRule rr = factory.createRuntimeRule(new TerminalLiteral("a"));
//		runtimeRuleSet.setRuntimeRules(value);
	}
	
}
