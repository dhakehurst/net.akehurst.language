
import { LanguageProcessor } from api;

export class LanguageProcessorDefault implement LanguageProcessor {

  private runtimeRuleSetBuilder: RuntimeRuleSetBuilder;
  private parser: Parser;

  constructor(grammar: Grammar, semanticAnalyser: SemanticAnalyser) {
     this.runtimeRuleSetBuilder = new RuntimeRuleSetBuilder();
     this.parser = new ScannerlessParser(this.runtimeRuleSetBuilder, grammar);
  }

}