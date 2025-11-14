//import {Agl, KtList} from 'net.akehurst.language-agl-processor';
//import {SpptWalker, SpptDataNodeInfo, SpptDataNode} from 'net.akehurst.language-agl-processor';
import {Agl, GrammarString, KtList} from 'net.akehurst.language-agl-processor/net.akehurst.language-agl-processor.mjs';
import {SpptDataNode, SpptDataNodeInfo, SpptWalker} from 'net.akehurst.language-agl-processor/net.akehurst.language-agl-parser.mjs';

const grammarStr = `
namespace test
grammar Test {
    skip leaf WHITESPACE = "\\s+" ;
    skip leaf MULTI_LINE_COMMENT = "/\\*[^*]*\\*+(?:[^*/][^*]*\\*+)*/" ;
    skip leaf SINGLE_LINE_COMMENT = "//[\\n\\r]*?" ;

    // literal must be last choice to be highest priority
    // otherwise 'true' | 'false' get matched as predefined.
    value = object | predefined | literal ;

    predefined = IDENTIFIER ;
    object = '{' property* '}' ;
    property = IDENTIFIER ':' value ;

    literal = BOOLEAN | INTEGER | REAL | STRING ;
    
    leaf BOOLEAN = "true|false";
    leaf REAL = "[0-9]+[.][0-9]+";
    leaf STRING = "'([^'\\\\]|\\\\'|\\\\\\\\)*'";
    leaf INTEGER = "[0-9]+";
    leaf IDENTIFIER = "[a-zA-Z_][a-zA-Z_0-9-]*" ;
}
`;

class MyWalker implements SpptWalker {
    beginTree(): void {
        console.log("start of SPPT");
    }

    endTree(): void {
        console.log("end of SPPT");
    }

    skip(startPosition: number, nextInputPosition: number): void {
        console.log(`a skip node: ${startPosition}-${nextInputPosition}`);
    }

    leaf(nodeInfo: SpptDataNodeInfo): void {
        console.log(`leaf node: ${nodeInfo.node.rule.tag} ${nodeInfo.node.startPosition}-${nodeInfo.node.nextInputPosition}`);
    }

    beginBranch(nodeInfo: SpptDataNodeInfo): void {
        console.log(`start branch: ${nodeInfo.node.rule.tag}`);
    }

    endBranch(nodeInfo: SpptDataNodeInfo): void {
        console.log(`end branch: ${nodeInfo.node.rule.tag}`);
    }

    beginEmbedded(nodeInfo: SpptDataNodeInfo): void {
        console.log("start embedded");
    }

    endEmbedded(nodeInfo: SpptDataNodeInfo): void {
        console.log("end embedded");
    }

    error(msg: string, path: () => KtList<SpptDataNode>): void {
        console.log("error $msg");
    }
}

export function parse_and_walk_sppt() {

    console.log("Create Processor")
    const res = Agl.getInstance().processorFromStringSimple(new GrammarString(grammarStr));
    console.log(res.issues.toString());
    const proc = res.processor;

    const sentences = [
        "true", //BOOLEAN
        "1", //INTEGER
        "3.14", //REAL
        "'Hello World!'", // STRING
        "var1", // predefined
        "{}", // empty object
        "{ a:false b:1 c:3.141 d:'bob' e:var2 }", // object
        "{ f:{x:1 y:{a:3 b:7}} }" //nested objects
    ];

    for (const sentence of sentences) {
        console.log(`Parsing '${sentence}'`)
        const pres = proc.parse(sentence);
        console.log(pres.issues.toString());
        const sppt = pres.sppt;
        console.log(sppt.toStringAll);

        const walker = new MyWalker();
        sppt.traverseTreeDepthFirst(walker, false);
    }
}