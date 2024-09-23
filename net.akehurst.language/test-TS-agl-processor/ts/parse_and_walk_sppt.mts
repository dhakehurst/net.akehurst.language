//import {Agl, KtList} from 'net.akehurst.language-agl-processor';
//import {SpptWalker, SpptDataNodeInfo, SpptDataNode} from 'net.akehurst.language-agl-processor';
import {Agl, KtList} from 'net.akehurst.language-agl-processor/net.akehurst.language-agl-processor.mjs';
import {SpptWalker, SpptDataNodeInfo, SpptDataNode} from 'net.akehurst.language-agl-processor/net.akehurst.language-agl-parser.mjs';

const grammarStr = `
namespace test
grammar Test {
    S = 'a';
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
    const res = Agl.getInstance().processorFromStringDefault(grammarStr);
    console.log(res.issues.toString());
    const proc = res.processor;

    console.log("Parse something")
    const pres = proc.parse('a');
    console.log(pres.issues.toString());
    const sppt = pres.sppt;
    console.log(sppt.toStringAll);

    const walker = new MyWalker();
    sppt.traverseTreeDepthFirst(walker);
}