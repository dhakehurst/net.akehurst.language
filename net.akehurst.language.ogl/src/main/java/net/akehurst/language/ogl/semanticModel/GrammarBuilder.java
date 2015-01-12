package net.akehurst.language.ogl.semanticModel;

public class GrammarBuilder {

	public GrammarBuilder(Namespace namespace, String name) {
		this.grammar = new Grammar(namespace, name);
	}

	Grammar grammar;

	public Grammar get() {
		return this.grammar;
	}

	public RuleBuilder rule(String name) {
		return new RuleBuilder(name, this.grammar);
	}

	public class RuleBuilder {

		public RuleBuilder(String name, Grammar grammar) {
			this.rule = new Rule(grammar, name);
			grammar.getRule().add(this.rule);
		}

		Rule rule;

		public void concatination(TangibleItem... sequence) {
			this.rule.setRhs(new Concatination(sequence));
		}

		public void choice(TangibleItem... alternative) {
			this.rule.setRhs(new Choice(alternative));
		}

		public void multi(int min, int max, TangibleItem item) {
			this.rule.setRhs(new Multi(min, max, item));
		}

		public void separatedList(int min, TerminalLiteral separator, TangibleItem item) {
			this.rule.setRhs(new SeparatedList(min, separator, item));
		}
	}

	public SkipRuleBuilder skip(String name) {
		return new SkipRuleBuilder(name, this.grammar);
	}

	public class SkipRuleBuilder {

		public SkipRuleBuilder(String name, Grammar grammar) {
			this.rule = new SkipRule(grammar, name);
			grammar.getRule().add(this.rule);
		}

		Rule rule;

		public void concatination(TangibleItem... sequence) {
			this.rule.setRhs(new Concatination(sequence));
		}

		public void choice(TangibleItem... alternative) {
			this.rule.setRhs(new Choice(alternative));
		}

		public void multi(int min, int max, TangibleItem item) {
			this.rule.setRhs(new Multi(min, max, item));
		}

		public void separatedList(int min, TerminalLiteral separator, TangibleItem item) {
			this.rule.setRhs(new SeparatedList(min, separator, item));
		}

	}
}
