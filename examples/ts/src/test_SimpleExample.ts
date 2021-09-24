import {net} from "net.akehurst.language-agl-processor";
import { SimpleExampleSyntaxAnalyser } from "./SimpleExampleSyntaxAnalyser";
import LanguageProcessor = net.akehurst.language.api.processor.LanguageProcessor;
import Agl = net.akehurst.language.agl.processor.Agl;
import AutomatonKind_api = net.akehurst.language.api.processor.AutomatonKind_api;
import {grammarStr} from "./SimpleExample";

export class Test_SimpleExample {
    analyser = new SimpleExampleSyntaxAnalyser();
    proc: LanguageProcessor  = Agl.processorFromString(grammarStr, this.analyser, null, null);

    doIt() {
        const sentence = `
class class {
  property : String
  method(p1: Integer, p2: String) {
  }
}
`;
        let sppt = this.proc.parse(sentence);
        console.info(sppt);

        let asm = this.proc.process(null, sentence, AutomatonKind_api.LOOKAHEAD_1);
        console.info(asm);
    }
}



